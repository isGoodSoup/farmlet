package com.soup.game.cmd;

import com.soup.game.ent.Player;
import com.soup.game.enums.CropID;
import com.soup.game.enums.Fertilizer;
import com.soup.game.enums.Gamerule;
import com.soup.game.enums.Upgrades;
import com.soup.game.intf.Command;
import com.soup.game.service.Console;
import com.soup.game.service.Localization;

import java.util.*;
import java.util.function.Consumer;

/**
 * <h1>Command Executor</h1>
 * The {@code Executor} class is the runtime engine for the F+ command language.
 * It processes tokenized commands, handles control structures, manages loop indices,
 * and dispatches execution to registered command handlers.
 * </p>
 *
 * <h2>F+ Execution Model</h2>
 * <p>
 * The Executor implements a recursive descent parser that processes F+ commands
 * with support for:
 * <ul>
 *     <li>Sequential execution (commands separated by newlines or {@code &&})</li>
 *     <li>Nested {@code for} loops with automatic index variables ({@code +i}, {@code +j})</li>
 *     <li>Conditional {@code if} statements with comparison operators</li>
 *     <li>Variable substitution and loop index replacement</li>
 *     <li>Command chaining and script blocks terminated by {@code ;}</li>
 * </ul>
 * </p>
 *
 * <h2>Control Structures</h2>
 * <h3>For Loops</h3>
 * <pre>
 * for 4 plant +i 0        // plants at rows 0-3, column 0
 * for 4 for 4 plant +i +j // plants entire 4x4 grid
 * </pre>
 *
 * <h3>If Statements</h3>
 * <pre>
 * if water < 10 then buy water
 * if days == 30 then season next
 * </pre>
 *
 * <h2>Execution Flow</h2>
 * <ol>
 *     <li>Input is collected until a semicolon ({@code ;}) is entered</li>
 *     <li>Script is split into lines and command chains</li>
 *     <li>Each command is tokenized by the {@link Parser}</li>
 *     <li>Tokens are processed recursively for control structures</li>
 *     <li>Variables and loop indices are resolved</li>
 *     <li>Commands are dispatched to registered handlers</li>
 * </ol>
 *
 * @see Parser
 * @see Registry
 * @author isGoodSoup
 * @version 1.0
 * @since 1.9
 */
public class Executor implements Command {
    private final Player player;
    private final Parser parser;
    private final Registry registry;
    private String lastCommand;
    private String[] previousArgs;
    private int totalCmd;

    public Executor(Player player, Parser parser, Registry registry) {
        this.player = player;
        this.parser = parser;
        this.registry = registry;
    }

    /**
     * Reads user input from the console, parses and executes commands until a
     * terminating semicolon (";") is entered or a command triggers a sleep condition.
     * Commands can be chained using "&&"; each command is looked up in the console's
     * command map and executed if found. Unknown commands are reported in red. Updates
     * {@link #totalCmd} and {@link #lastCommand} for each executed command.
     *
     * @see #doSleep()
     */
    public void run() {
        StringBuilder script = new StringBuilder();
        String line;
        do {
            line = Console.cli.reply("").trim();
            if(!Console.cli.equals(line, ";")) {
                script.append(line).append("\n");
            }
        } while(!Console.cli.equals(line, ";"));

        String[] lines = script.toString().split("\\R");
        for(String l : lines) {
            String[] chain = l.split("\\s*&&\\s*");
            for(String cmd : chain) {
                cmd = cmd.trim();
                if(cmd.isEmpty()) continue;

                String[] tokens = parser.tokenize(cmd);
                execute(tokens, 0, new LinkedHashMap<>(), 0);
            }
        }
    }

    /**
     * Executes a loop a fixed number of times over a tokenized command stream.
     * <p>
     * This method is the core driver for the custom {@code for} construct. It binds
     * a loop index (e.g. {@code +i}, {@code +j}, ...) based on the current recursion
     * depth, then repeatedly invokes {@link #execute(String[], int, Map, int)} on the
     * provided token sequence.
     * </p>
     *
     * <p>
     * Loop indices are stored in the {@code indices} map and are scoped per iteration.
     * Each iteration assigns the current index value, executes the nested command,
     * and then removes the index binding.
     * </p>
     *
     * @param times   the number of iterations to execute; must be {@code > 0}
     * @param indices a map of active loop indices (e.g. {@code i -> 0, j -> 2})
     * @param depth   the current nesting depth, used to determine index variable names
     *
     * @see #execute(String[], int, Map, int)
     * @see #letter(int)
     */
    private void runFor(int times, String[] bodyTokens, Map<String, Integer> indices, int depth) {
        for(int i = 0; i < times; i++) {
            indices.put(letter(depth), i);
            execute(bodyTokens, 0, indices, depth + 1);
            indices.remove(letter(depth));
        }
    }

    /**
     * Executes a command from a token stream starting at a given position.
     * <p>
     * This method performs a recursive descent over the token array. It supports:
     * </p>
     * <ul>
     *     <li>Nested {@code for} loops (e.g. {@code for 4 for 4 plant +i +j})</li>
     *     <li>{@code while} loops (e.g. {@code while true water 0 0}</li>
     *     <li>Standard command execution via the console command registry</li>
     * </ul>
     *
     * <p>
     * If the current token is {@code "for"}, the method parses the iteration count
     * from the next token and delegates execution to {@link #runFor(int, String[], Map, int)}.
     * Otherwise, the token is treated as a command name and executed with all remaining
     * tokens as arguments.
     * </p>
     *
     * <p>
     * Before execution, all arguments are processed through
     * {@link #replace(String[], Map)} to resolve loop indices (e.g. {@code +i})
     * and variable references.
     * </p>
     *
     * @param tokens  the full tokenized command sequence
     * @param pos     the current position in the token array
     * @param indices a map of active loop indices
     * @param depth   the current nesting depth for loop index resolution
     *
     * @see #runFor(int, String[], Map, int)
     * @see #replace(String[], Map)
     */
    private void execute(String[] tokens, int pos, Map<String, Integer> indices, int depth) {
        if(pos >= tokens.length) return;

        String token = tokens[pos];

        if(token != null && token.equals("for")) {
            if(pos + 1 >= tokens.length) {
                Console.cli.println("Usage: for <times> <command>", Console.PURPLE);
                return;
            }

            Object rawTimes = getVar(tokens[pos + 1]);
            int nestedTimes;

            if(rawTimes instanceof Number) {
                nestedTimes = ((Number) rawTimes).intValue();
            } else {
                try {
                    nestedTimes = Integer.parseInt(rawTimes.toString());
                } catch(Exception e) {
                    Console.cli.error("Invalid number of times: " + tokens[pos + 1]);
                    return;
                }
            }

            int bodyEnd = tokens.length;
            String[] subTokens = Arrays.copyOfRange(tokens, pos + 2, bodyEnd);
            runFor(nestedTimes, subTokens, indices, depth);
            execute(tokens, bodyEnd + 1, indices, depth);
            return;
        }

        if(token != null && token.equals("if")) {
            if(pos + 4 >= tokens.length) {
                Console.cli.println("Usage: if <left> <op> <right> then <command>", Console.PURPLE);
                return;
            }

            String left = tokens[pos + 1];
            String op = tokens[pos + 2];
            String right = tokens[pos + 3];

            Object result = evaluate(left, op, right);
            if(!(result instanceof Boolean)) {
                Console.cli.error("Condition must evaluate to boolean");
                return;
            }

            if(!tokens[pos + 4].equalsIgnoreCase("then")) {
                Console.cli.println("Usage: if <left> <op> <right> then <command>", Console.PURPLE);
                return;
            }

            String[] body = Arrays.copyOfRange(tokens, pos + 5, tokens.length);
            if((Boolean) result) {
                execute(body, 0, indices, depth);
            }
            return;
        }

        assert token != null;
        Consumer<String[]> action = registry.get(token);
        if(action == null) {
            return;
        }

        String[] rawArgs = Arrays.copyOfRange(tokens, pos + 1, tokens.length);
        String[] finalArgs = replace(rawArgs, indices);
        String[] fullArgs = new String[finalArgs.length + 1];
        fullArgs[0] = token;
        System.arraycopy(finalArgs, 0, fullArgs, 1, finalArgs.length);

        previousArgs = fullArgs.clone();
        lastCommand = token;

        action.accept(fullArgs);
    }

    /**
     * Replaces loop index placeholders and variable references in command arguments.
     * <p>
     * This method processes each argument by:
     * </p>
     * <ol>
     *     <li>Replacing loop index placeholders (e.g. {@code +i}, {@code +j}) with
     *     their corresponding values from {@code indices}</li>
     *     <li>Resolving variables via {@link #getVar(String)} if applicable</li>
     * </ol>
     *
     * <p>
     * The replacement is performed sequentially for each active index, allowing
     * nested loops to correctly substitute multiple placeholders.
     * </p>
     *
     * @param args    the raw argument array
     * @param indices the active loop index bindings
     * @return a new array with all placeholders and variables resolved
     *
     * @see #getVar(String)
     */
    private String[] replace(String[] args, Map<String, Integer> indices) {
        String[] result = args.clone();
        for(int k = 0; k < result.length; k++) {
            String arg = result[k];
            for(Map.Entry<String, Integer> entry : indices.entrySet()) {
                arg = arg.replace("+" + entry.getKey(), entry.getValue().toString());
            }
            Object varValue = getVar(arg);
            if(varValue != null) arg = varValue.toString();
            result[k] = arg;
        }
        return result;
    }

    /**
     * Retrieves the value of a named variable from the console's variable store.
     * <p>
     * If the variable exists, returns its stored value. If the variable does not
     * exist, returns the input name itself as a fallback.
     * <p>
     * This method is used to dynamically resolve variables in commands such as loops
     * or conditional operations.
     *
     * @param name the name of the variable to retrieve
     * @return the stored value of the variable, or the input name if not found
     */
    private Object getVar(String name) {
        return registry.getVariable(name);
    }

    /**
     * Returns the loop index variable name for a given nesting depth.
     * <p>
     * Index variables start at {@code 'i'} for depth {@code 0}, then increment
     * alphabetically ({@code j}, {@code k}, ...). This allows nested loops to use
     * distinct placeholders such as {@code +i}, {@code +j}, etc.
     * </p>
     *
     * @param depth the current nesting depth (0-based)
     * @return the corresponding index variable name as a string
     * @throws IllegalStateException if the nesting depth exceeds supported range
     */
    private String letter(int depth) {
        char c = (char) ('i' + depth);
        if(c > 'z') throw new IllegalStateException("Too many nested loops (>18)");
        return String.valueOf(c);
    }

    /**
     * Evaluates an expression between two values and returns the result
     * <p>
     * That result may either be numeric or boolean depending on the operation.
     * Used in both if and while since they require a condition.
     * </p>
     * @param leftVar value A
     * @param op binary operator
     * @param rightVar value B
     * @return the boolean/numeric type {@link Object}
     *
     * @see #execute(String[], int, Map, int)
     */
    public Object evaluate(String leftVar, String op, String rightVar) {
        Object leftValue = getVar(leftVar);
        Object rightValue = getVar(rightVar);
        if(leftValue == null) leftValue = leftVar;
        if(rightValue == null) rightValue = rightVar;

        double a, b;
        try {
            a = Double.parseDouble(leftValue.toString());
            b = Double.parseDouble(rightValue.toString());
        } catch(NumberFormatException e) {
            Console.cli.error("Invalid number in expression");
            return 0;
        }

        return switch(op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;
            case "<" -> a < b;
            case ">" -> a > b;
            case "==" -> a == b;
            default -> 0;
        };
    }

    /**
     * Grants the player a specified quantity of an item, crop, upgrade,
     * water, or gold.
     * <p>
     * The method first checks if the arguments are valid. Then it:
     * <ul>
     *     <li>Adds the specified crop to the player's inventory if it exists.</li>
     *     <li>Adds the specified upgrade to the player's upgrades if it exists.</li>
     *     <li>Adds the specified fertilizer to the player's inventory if it exists</li>
     *     <li>Adds water or gold directly to the player if specified.</li>
     * </ul>
     * After applying the grant, the game is forcibly ended to reflect
     * the immediate effect.
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] – the command name "give"</li>
     *                 <li>args[1] – the item/upgrade/water/gold name</li>
     *                 <li>args[2] – the quantity to give</li>
     *             </ul>
     */
    public void give(String[] args) {
        if(Gamerule.isEnabled(Gamerule.ENABLE_CHEATS)) {
            if(args.length < 3) {
                Console.cli.println(Localization.lang.t("game.give.usage"), Console.PURPLE);
                return;
            }

            String item = args[1];
            int quantity = Integer.parseInt(args[2]);

            for(int i = 0; i < quantity; i++) {
                CropID itemCrop;
                for(CropID c : CropID.values()) {
                    if(Console.cli.equals(c.getName(), item)) {
                        itemCrop = c;
                        player.inventory().add(itemCrop);
                        break;
                    }
                }
            }

            for(int i = 0; i < quantity; i++) {
                Upgrades upgrade;
                for(Upgrades u : Upgrades.values()) {
                    if(Console.cli.equals(u.name().toLowerCase(), item)) {
                        upgrade = u;
                        player.add(upgrade);
                        break;
                    }
                }
            }

            for(int i = 0; i < quantity; i++) {
                Fertilizer fertilizer;
                for(Fertilizer f : Fertilizer.values()) {
                    if(Console.cli.equals("f." + f.name().toLowerCase(), item)) {
                        fertilizer = f;
                        player.inventory().add(fertilizer);
                        item = "fertilizer." + f.name().toLowerCase();
                        break;
                    }
                }
            }

            if(Console.cli.equals(item, "water")) {
                player.water(quantity);
            }

            if(Console.cli.equals(item, "gold")) {
                player.earn(quantity);
            }

            Console.cli.println(Localization.lang.t("game.give.success",
                    item, quantity), Console.BRIGHT_GREEN);
//            forceEnd();
        }
    }

    /**
     * Assigns a value to a named variable in the console's variable store.
     * <p>
     * The expected command format is: <code>var &lt;name&gt; = &lt;value&gt;</code>.
     * Supports numeric values (integer or decimal) and strings. Numeric strings
     * are automatically parsed into {@link Integer} or {@link Double}.
     * <p>
     * Example usage:
     * <pre>
     * var times = 6
     * var playerName = John
     * </pre>
     *
     * @param args the command arguments, where
     *             <ul>
     *                 <li>args[1] is the variable name</li>
     *                 <li>args[2] must be "="</li>
     *                 <li>args[3] is the value to assign</li>
     *             </ul>
     * @see Console
     */
    public void var(String[] args) {
        if(args.length < 4 || !args[2].equalsIgnoreCase("=")) {
            Console.cli.println(Localization.lang.t("game.var.usage"), Console.PURPLE);
            return;
        }

        String name = args[1];
        String valueStr = args[3];
        Object value;

        if(args.length == 6 && args[4].matches("[+\\-*/]")) {
            value = evaluate(args[3], args[4], args[5]
            );
        } else {
            try {
                if(valueStr.contains(".")) {
                    value = Double.parseDouble(valueStr);
                } else {
                    value = Integer.parseInt(valueStr);
                }
            } catch (NumberFormatException e) {
                value = valueStr;
            }
        }
        registry.setVariable(name, value);
    }

    /**
     * Repeats the previously executed command.
     * <p>
     * If there was a previous command, it is retrieved from storage and
     * executed again with the same arguments. This is triggered by the
     * {@code .} (redo) command.
     * </p>
     */
    public void redo() {
        if(previousArgs == null) {
            Console.cli.error("No previous command.");
            return;
        }

        Consumer<String[]> action = registry.get(previousArgs[0]);
        if(action != null) {
            action.accept(previousArgs.clone());
        }
    }

    /**
     * With the parsed last command it returns a boolean check of
     * if the last command is sleep
     * @return boolean if true command was sleep, false otherwise
     */
    public boolean doSleep() {
        return Console.cli.equals(lastCommand, "sleep");
    }

    /**
     * Obtain the last executed command
     * @return a {@link String} of the last command executed in
     * the pipeline
     */
    public String getLastCommand() {
        return lastCommand;
    }
}