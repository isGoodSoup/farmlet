package com.soup.game.core;

import com.soup.game.ent.Player;
import com.soup.game.enums.*;
import com.soup.game.intf.Item;
import com.soup.game.intf.World;
import com.soup.game.service.Console;
import com.soup.game.service.Inventory;
import com.soup.game.service.Localization;
import com.soup.game.service.Pos;
import com.soup.game.world.Barn;
import com.soup.game.world.Crop;
import com.soup.game.world.Tile;

import java.util.*;
import java.util.function.Consumer;

/**
 * <h1>farmlet</h1>
 * The {@code Game} class represents the core logic for the
 * roguelike terminal-based farming simulation game.
 * <p>
 * This class manages the player's farm, including tiles, crops, resources, upgrades, inventory, weather,
 * seasons, and the main game loop. It handles all player commands and updates the game state accordingly.
 * </p>
 *
 * <p><b>Game Features:</b></p>
 * <ul>
 *     <li>Dynamic farm grid of size {@code SIZE} × {@code SIZE}, up to {@value MAX_SIZE}</li>
 *     <li>Player resource management (coins, water, inventory, XP)</li>
 *     <li>Seasonal and daily weather effects</li>
 *     <li>Crop planting, watering, harvesting, and regrowth</li>
 *     <li>Command-driven gameplay with loops and conditionals</li>
 *     <li>Market system for buying water, plots, and upgrades</li>
 *     <li>Statistics and game ending messages based on performance</li>
 *  </ul>
 * <h2>F+ Command Language</h2>
 *
 * <p>
 * F+ is a lightweight command-based scripting language used to control
 * in-game actions through a console interface. Commands are executed line-by-line
 * and may be chained, nested, and parameterized.
 * </p>
 *
 * <h2>Basic Syntax</h2>
 * <ul>
 *     <li>Commands are separated by newlines or {@code &&}</li>
 *     <li>Execution ends when a semicolon ({@code ;}) is entered</li>
 *     <li>Arguments are separated by whitespace</li>
 *     <li>Quoted strings are preserved as single arguments</li>
 * </ul>
 *
 * <pre>
 * plant 1 2
 * plant 1 2 && harvest 1 2
 * </pre>
 *
 * <h2>Loops</h2>
 * <p>
 * The {@code for} command allows repeated and nested execution:
 * </p>
 *
 * <pre>
 * for 4 plant 0 +i
 * for 4 for 4 plant +i +j
 * </pre>
 *
 * <p>
 * Loop indices are exposed as {@code +i}, {@code +j}, {@code +k}, ... depending
 * on nesting depth.
 * </p>
 *
 * <h2>Variables</h2>
 * <p>
 * Arguments may reference variables, which are resolved at runtime.
 * </p>
 *
 * <h2>Execution Model</h2>
 * <ul>
 *     <li>Input is tokenized into arguments</li>
 *     <li>Commands are resolved via a registry</li>
 *     <li>Nested structures are executed recursively</li>
 * </ul>
 *
 * @see #run()
 * @author isGoodSoup
 * @version 1.8
 * @since 1.0
 */
@World
public final class Game {
    private static final int MAX_SIZE = 512;
    private static final float HOURS = 24f;
    private final Player player;
    private final Tile[][] tiles;
    private final Barn barn;
    private final Map<Integer, String> market;
    private List<Pos> positions;
    private final List<Upgrades> upgrades;
    private final String day;

    private int SIZE = 4;
    private float water = 100f;
    private int days;
    private int dryDay;
    private float hours;

    private Weather weather = Weather.SUNNY;
    private Seasons season = Seasons.WINTER;
    private String[] previousArgs;
    private String lastCommand = "foo";

    private int totalCmd;
    private int totalCrops;
    private float waterUsed;
    private boolean isGameOver;

    /**
     * Initializes a new Farm game.
     * Sets up the farm grid, inventory, commands, market,
     * weather and upgrades. It starts the main game loop.
     */
    public Game() {
        Localization.lang.setLocale(Locale.forLanguageTag("en"));
        this.tiles = new Tile[MAX_SIZE][MAX_SIZE];
        this.player = new Player();
        this.barn = new Barn(player);
        this.market = new LinkedHashMap<>();
        this.addCommands();

        this.day = Localization.lang.t("game.day");
        this.upgrades = new ArrayList<>();
        upgrades.add(Upgrades.NULL);

        console().println("Farmlet, a terminal farm", Console.PURPLE);
        console().println(Localization.lang.t("game.welcome", player.title()),
                Console.BRIGHT_GREEN);
        start();
    }

    /**
     * The main method, to start the game
     * @param args
     */
    public static void main(String[] args) {
        new Game();
    }

    /**
     * Starts the game by initializing coins and days,
     * then entering the main loop.
     */
    private void start() {
        days = 0;
        totalCrops = 0;
        totalCmd = 0;
        loop();
        showEnding();
        showStats();
    }

    /**
     * Displays the game ending message based on the number of days the player has survived.
     * <p>
     * The ending is determined as follows:
     * <ul>
     *     <li>Best Ending – if days &gt; 60</li>
     *     <li>Good Ending – if days are between 15 (inclusive) and 60 (exclusive)</li>
     *     <li>Bad Ending – if days &lt; 15</li>
     * </ul>
     * </p>
     */
    private void showEnding() {
        if(days > 60) {
            console().println(Localization.lang.t("game.end.best", days),
                    Console.BRIGHT_GREEN);
        } else if(days >= 15 && days < 60) {
            console().println(Localization.lang.t("game.end.good", days),
                    Console.BRIGHT_YELLOW);
        } else if(days < 15) {
            console().println(Localization.lang.t("game.end.bad", days),
                    Console.PURPLE);
        }
    }

    /**
     * Main game loop for the roguelike farming simulation.
     *
     * <p>
     * This method drives the core progression of the game. Each iteration of the
     * loop represents a fraction of a day and includes:
     * </p>
     *
     * <ul>
     *     <li>Displaying the current day number and total days</li>
     *     <li>Updating the season and weather</li>
     *     <li>Updating all game entities, including crops and animals via {@link #barn}</li>
     *     <li>Advancing in-game time in increments until the player sleeps, the day ends, or the game ends</li>
     *     <li>Processing player commands through the F+ console interface</li>
     *     <li>Resetting harvested crops at the end of the day</li>
     * </ul>
     *
     * <p><b>Animal Integration:</b></p>
     * <ul>
     *     <li>{@link Barn.#update()} is called once per game loop iteration to update all
     *         animals, handling feeding, sleeping, production, and aging</li>
     *     <li>Animals that are dead are automatically removed, and breeding logic can be triggered separately</li>
     * </ul>
     *
     * <p><b>Time Progression:</b></p>
     * <ul>
     *     <li>The in-game clock starts at {@code hours = 6f} each loop</li>
     *     <li>Time advances in increments of {@code 0.2f} per inner loop iteration</li>
     *     <li>If {@code hours >= HOURS} (end of day) or the player chooses to sleep,
     *         the day counter is incremented and crops are grown via {@link #grow()}</li>
     * </ul>
     *
     * <p><b>Loop Termination:</b></p>
     * <ul>
     *     <li>The loop ends if the player enters the "end" command</li>
     *     <li>The loop also ends if {@code isGameOver} becomes true</li>
     * </ul>
     *
     * @see #barn
     * @see #update()
     * @see #grow()
     * @see #resetHarvest()
     * @see #console()
     */
    private void loop() {
        while(!console().equals(lastCommand, "end") && !isGameOver) {
            console().println(day + " " + days, Console.GREEN);
            season();
            weather();
            update();
            hours = 6f;
            do {
                run();
                barn.update();
                hours += 0.2f;
                if(hours >= HOURS || doSleep(lastCommand)) {
                    hours = 0f;
                    days++;
                    grow();
                }
            } while(!doSleep(lastCommand)
                    && !console().equals(lastCommand, "end")
                    && !isGameOver);
            resetHarvest();
        }
    }

    /**
     * Reads user input from the console, parses and executes commands until a
     * terminating semicolon (";") is entered or a command triggers a sleep condition.
     * Commands can be chained using "&&"; each command is looked up in the console's
     * command map and executed if found. Unknown commands are reported in red. Updates
     * {@link #totalCmd} and {@link #lastCommand} for each executed command.
     *
     * @see #doSleep(String)
     * @see Console#cmd()
     */
    private void run() {
        StringBuilder script = new StringBuilder();
        String line;
        do {
            line = console().reply("").trim();
            if(!console().equals(line, ";")) {
                script.append(line).append("\n");
            }
        } while(!console().equals(line, ";"));

        String[] lines = script.toString().split("\\R");
        for(String l : lines) {
            String[] chain = l.split("\\s*&&\\s*");
            for(String cmd : chain) {
                cmd = cmd.trim();
                if(cmd.isEmpty()) { continue; }
                String[] tokens = tokenize(cmd);
                totalCmd++;
                execute(tokens, 0, new LinkedHashMap<>(), 0);
            }
        }
    }

    /**
     * Tokenizes a command line string into arguments, preserving quoted segments.
     * <p>
     * Splits the input on whitespace, except when inside double quotes. Quoted
     * substrings are treated as a single token and returned without the quote characters.
     * </p>
     *
     * <p>
     * Example:
     * </p>
     * <pre>
     * input:  plant 1 "hello world"
     * output: ["plant", "1", "hello world"]
     * </pre>
     *
     * @param commandLine the raw command line input
     * @return an array of tokens representing the parsed arguments
     */
    private String[] tokenize(String commandLine) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;

        for(int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);
            if(c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if(Character.isWhitespace(c) && !inQuote) {
                if(!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if(!current.isEmpty()) tokens.add(current.toString());
        return tokens.toArray(new String[0]);
    }

    /**
     * Registers all available commands
     * and their corresponding actions.
     */
    private void addCommands() {
        console().cmd().put("?", this::showHelp);
        console().cmd().put(".", this::redo);
        console().cmd().put("var", this::var);
        console().cmd().put("harvest", this::harvest);
        console().cmd().put("rip", this::rip);
        console().cmd().put("water", this::irrigate);
        console().cmd().put("plant", this::plant);
        console().cmd().put("fertilize", this::fertilize);
        console().cmd().put("feed", args -> barn.feedAll());
        console().cmd().put("pet", args -> barn.pet());
        console().cmd().put("get", this::get);
        console().cmd().put("view", this::update);
        console().cmd().put("show", args -> update());
        console().cmd().put("inv", args -> showInventory());
        console().cmd().put("time", args -> showTime());
        console().cmd().put("sell", args -> sellCrops());
        console().cmd().put("give", this::give);
        console().cmd().put("buy", args -> buy());
        console().cmd().put("stats", args -> showStats());
        console().cmd().put("sleep", this::sleep);
        console().cmd().put("end", args -> {});
    }

    /**
     * Registers all available gamerules
     */
    private void addGamerules() {
        gamerules.addAll(Arrays.asList(Gamerule.values()));
    }

    private void gamerule(String[] args) {
        if(args.length < 3) {
            console().println(Localization.lang.t("game.gamerule.usage"), Console.PURPLE);
            return;
        }

        String str = args[1];
        boolean value = Boolean.parseBoolean(args[2]);
        int index = gamerules.indexOf(Gamerule.rule.keyOf(str));

        Gamerule gamerule = gamerules.get(index);
        gamerule.setValue(value);
        console().println(Localization.lang.t("game.gamerule.success"),
                Console.BRIGHT_GREEN);
    }

    /**
     * Displays the entire farm grid to the console.
     * <p>
     * This method is equivalent to calling {@link #update(String[])}
     * with coordinates covering the whole farm, from (0,0) to (SIZE,SIZE).
     * It shows each tile as follows:
     * <ul>
     *     <li>[ ] for empty plots</li>
     *     <li>[X] for withered crops</li>
     *     <li>[S], [G], [M], [H] etc. for crops in various growth stages</li>
     * </ul>
     * The output includes row and column indices for reference.
     * </p>
     * <p>
     * This method is used internally as the default "show" command and
     * can also be called directly to display the entire grid.
     * </p>
     */
    private void update() {
        update(new String[]{"show", "0", "0", String.valueOf(SIZE),
                String.valueOf(SIZE)});
    }

    /**
     * Displays a portion of the farm grid to the console.
     * <p>
     * The portion is defined by the start and end coordinates:
     * <ul>
     *     <li>args[1]: start row (inclusive)</li>
     *     <li>args[2]: start column (inclusive)</li>
     *     <li>args[3]: end row (exclusive)</li>
     *     <li>args[4]: end column (exclusive)</li>
     * </ul>
     * Each tile is displayed as follows:
     * <ul>
     *     <li>[ ] for empty plots</li>
     *     <li>[X] for withered crops</li>
     *     <li>[S], [G], [M], [H] etc. for crops in various growth stages</li>
     * </ul>
     * Colors are applied according to crop growth stage:
     * <ul>
     *     <li>SEED, BUD: purple</li>
     *     <li>GROWING: blue</li>
     *     <li>MATURE: cyan</li>
     *     <li>HARVESTABLE: bright green</li>
     * </ul>
     * <p>
     * This method is used by both the "show" command (full farm)
     * and the "view" command (subset of the farm). It performs
     * validation of coordinates and prints an error message if
     * coordinates are invalid or out of bounds.
     * </p>
     *
     * @param args an array of command arguments, where:
     *             <ul>
     *                 <li>args[0] is the command name ("show" or "view")</li>
     *                 <li>args[1] is the start row (inclusive)</li>
     *                 <li>args[2] is the start column (inclusive)</li>
     *                 <li>args[3] is the end row (exclusive)</li>
     *                 <li>args[4] is the end column (exclusive)</li>
     *             </ul>
     */
    private void update(String[] args) {
        if(args.length < 5) {
            console().println(Localization.lang.t("game.view.usage"),
                    Console.PURPLE);
            return;
        }

        int startRow, startCol;
        int endRow, endCol;
        try {
            startRow = Integer.parseInt(args[1]);
            startCol = Integer.parseInt(args[2]);
            endRow = Integer.parseInt(args[3]);
            endCol = Integer.parseInt(args[4]);
        } catch(NumberFormatException e) {
            console().error(Localization.lang.t("game.coordinates.invalid"));
            return;
        }

        if(startRow < 0 || startRow >= SIZE || startCol < 0 || startCol >= SIZE) {
            console().error(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("    ");
        for(int col = startCol; col < endCol; col++) {
            sb.append(String.format("%-3d", col));
        }
        sb.append("\n");
        for(int row = startRow; row < endRow; row++) {
            sb.append(String.format("%-3d", row));
            for(int col = startCol; col < endCol; col++) {
                Tile tile = tiles[row][col];
                if(tile == null || tile.crop() == null) {
                    sb.append("[ ]");
                } else if(tile.crop().getHydration() == Hydration.NONE) {
                    tile.crop().wither();
                    sb.append(Console.RED).append("[X]").append(Console.RESET);
                } else {
                    String stageColor = "foo2";
                    switch(tile.crop().getStage()) {
                        case SEED, BUD -> stageColor = Console.PURPLE;
                        case GROWING -> stageColor = Console.BLUE;
                        case MATURE -> stageColor = Console.CYAN;
                        case HARVESTABLE -> stageColor = Console.BRIGHT_GREEN;
                    }
                    sb.append(stageColor).append("[")
                            .append(tile.crop().getChar()).append("]")
                            .append(Console.RESET);
                }
            }
            sb.append("\n");
        }
        console().print(sb.toString());
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
        for (int i = 0; i < times; i++) {
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
    private void execute(String[] tokens, int pos,
                         Map<String, Integer> indices, int depth) {
        if(pos >= tokens.length) { return; }
        String token = tokens[pos];
        if(token != null && token.equals("for")) {
            if(!upgrades.contains(Upgrades.FOR_LOOP)) {
                console().error(Localization.lang.t("game.upgrade.locked"));
                return;
            }

            if(pos + 1 >= tokens.length) {
                console().println(Localization.lang.t("game.for.usage"), Console.PURPLE);
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
                    console().error("Invalid number of times: " + tokens[pos + 1]);
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
                console().println(Localization.lang.t("game.if.usage"), Console.PURPLE);
                return;
            }

            String left = tokens[pos + 1];
            String op = tokens[pos + 2];
            String right = tokens[pos + 3];

            Object result = evaluate(left, op, right, indices);
            if(!(result instanceof Boolean)) {
                console().error(Localization.lang.t("game.error.condition"));
                return;
            }

            if(!tokens[pos + 4].equalsIgnoreCase("then")) {
                console().println(Localization.lang.t("game.if.usage"), Console.PURPLE);
                return;
            }

            String[] body = Arrays.copyOfRange(tokens, pos + 5, tokens.length);
            if((Boolean) result) {
                execute(body, 0, indices, depth);
            }
            return;
        }

        Consumer<String[]> action = console().cmd().get(token);
        if(action == null) {
            return;
        }

        String[] rawArgs = Arrays.copyOfRange(tokens, pos + 1, tokens.length);
        String[] finalArgs = replace(rawArgs, indices);
        String[] awc = new String[finalArgs.length + 1];
        awc[0] = token;
        System.arraycopy(finalArgs, 0, awc, 1, finalArgs.length);
        action.accept(awc);
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
     * @param indices {@link Map} of indices from the {@link #execute(String[], int, Map, int)} method
     * @return the boolean/numeric type {@link Object}
     *
     * @see #execute(String[], int, Map, int)
     */
    private Object evaluate(String leftVar, String op, String rightVar,
                            Map<String, Integer> indices) {
        Object leftValue = getVar(leftVar);
        Object rightValue = getVar(rightVar);
        if(leftValue == null) { leftValue = leftVar; }
        if(rightValue == null) { rightValue = rightVar; }

        double a, b;
        try {
            a = Double.parseDouble(leftValue.toString());
            b = Double.parseDouble(rightValue.toString());
        } catch(NumberFormatException e) {
            console().error(Localization.lang.t("game.error.number"));
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
     * @see #console()
     */
    private void var(String[] args) {
        if(args.length < 4 || !args[2].equalsIgnoreCase("=")) {
            console().println(Localization.lang.t("game.var.usage"), Console.PURPLE);
            return;
        }

        String name = args[1];
        String valueStr = args[3];
        Object value;

        if(args.length == 6 && args[4].matches("[+\\-*/]")) {
            value = evaluate(args[3], args[4], args[5],
                    new LinkedHashMap<>());
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
        console().var().put(name, value);
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
        return console().var().getOrDefault(name, name);
    }

    /**
     * Advances the growth of all crops on the farm,
     * except during dry weather.
     */
    private void grow() {
        if(!Objects.equals(weather, Weather.DRY)) {
            for(Pos pos : letter()) {
                Tile tile = tiles[pos.row()][pos.col()];
                if(tile != null && tile.crop() != null) {
                    tile.crop().grow(tile.soil(), tile.fertilizer(),
                            weather.equals(Weather.STORM) ? 2 : 1);
                }
            }
        }
    }

    /**
     * Harvests crops from the farm at a specified location or, if the player has
     * the {@link Upgrades#HARVEST_UPGRADE} upgrade, all harvestable crops on the farm.
     * <p>
     * If the "all" keyword is used with the appropriate upgrade, this method
     * iterates over all farm tiles, adds harvested crops to the player's
     * inventory, and either resets the crop to the {@link GrowthStage#SEED} stage
     * (if it regrows) or clears the tile.
     * </p>
     * <p>
     * If a specific tile is specified using row and column arguments, the method
     * validates the coordinates and ensures the crop is ready for harvest.
     * If the crop cannot be harvested yet, or the tile is empty, an error
     * message is printed.
     * </p>
     * <p>
     * Upon successful harvest:
     * <ul>
     *     <li>The crop is added to the player's inventory.</li>
     *     <li>The crop's harvested state is updated.</li>
     *     <li>If the crop regrows, its stage is reset to {@link GrowthStage#SEED}.</li>
     *     <li>If the crop does not regrow, the tile is cleared.</li>
     *     <li>The player gains XP associated with the crop.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Usage examples:
     * <pre>
     *   harvest(new String[]{"harvest", "2", "0"}); // harvests crop at row 2, column 0
     *   harvest(new String[]{"harvest", "all"});   // harvests all crops if HARVEST upgrade unlocked
     * </pre>
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] – the command name "harvest"</li>
     *                 <li>args[1] – either the row index or the keyword "all" (if upgrade unlocked)</li>
     *                 <li>args[2] – the column index (required if harvesting a specific tile)</li>
     *             </ul>
     */
    private void harvest(String[] args) {
        if(args.length < 3 && upgrades.contains(Upgrades.HARVEST_UPGRADE)
                && console().equals(args[1], "all")) {
            for(Pos pos : letter()) {
                int row = pos.row();
                int col = pos.col();
                Tile tile = tiles[row][col];
                int totalYield = Math.round(tile.crop().getId().getYield() *
                        (tile.soil().getYieldModifier() +
                                tile.crop().getYieldBonus()));
                for(int i = 0; i < totalYield; i++) {
                    inventory().add(tile.crop().getId());
                }
                tile.crop().harvested();
                if(tile.crop().getId().regrows()) {
                    tile.crop().setStage(GrowthStage.SEED);
                } else {
                    tiles[row][col] = null;
                }
                console().println(Localization.lang.t("game.yields",
                        inventory().getQuantity(tile.crop().getId())), Console.PURPLE);
                player.update(tile.crop().getId().getXp());
                tile.crop().resetYieldBonus();
            }
            console().println(Localization.lang.t("game.harvest.success.all"),
                    Console.BRIGHT_GREEN);
            return;
        }

        if(args.length < 3) {
            console().println(Localization.lang.t("game.harvest.usage"), Console.PURPLE);
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            console().error(Localization.lang.t("game.coordinates.invalid"));
            return;
        }

        if(row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            console().error(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        Tile tile = tiles[row][col];
        if(tile.crop() == null) {
            console().error(Localization.lang.t("game.harvest.nothing"));
            return;
        }

        if(!tile.crop().canHarvest()) {
            console().error(Localization.lang.t("game.harvest.not_ready"));
            return;
        }

        int totalYield = Math.round(tile.crop().getId().getYield() *
                (tile.soil().getYieldModifier() +
                        tile.crop().getYieldBonus()));
        for(int i = 0; i < totalYield; i++) {
            inventory().add(tile.crop().getId());
        }

        tile.crop().harvested();
        if(tile.crop().getId().regrows()) {
            tile.crop().setStage(GrowthStage.SEED);
        } else {
            tiles[row][col] = null;
        }

        tile.crop().resetYieldBonus();
        console().println(Localization.lang.t("game.harvest.success",
                tile.crop().getId().getName(), row, col), Console.BRIGHT_GREEN);
        player.update(tile.crop().getId().getXp());
    }

    /**
     * Resets the harvest state of all crops
     * at the end of the day.
     */
    private void resetHarvest() {
        for(Pos pos : letter()) {
            Tile tile = tiles[pos.row()][pos.col()];
            if(tile != null && tile.crop() != null) {
                tile.crop().resetHarvest();
            }
        }
    }

    /**
     * Updates hydration levels of all crops based on previous water state
     * and withers crops with no water.
     */
    private void updateHydration() {
        int totalHydration = 0;
        int cropCount = 0;

        for(Pos pos : letter()) {
            Tile tile = tiles[pos.row()][pos.col()];
            if(tile != null && tile.crop() != null) {
                tile.crop().decay();
                totalHydration += tile.crop().getHydration().ordinal();
                cropCount++;
            }
        }

        float average = cropCount > 0 ? (float) totalHydration/cropCount : 0f;
        console().println(Localization.lang.t("game.irrigate_crops", average),
                Console.CYAN);
    }

    /**
     * Waters a specific crop on the farm if the player has available water.
     * <p>
     * This method requires the player to specify the row and column of the crop
     * they wish to water. If the specified tile contains a crop, its hydration
     * level is set to {@link Hydration#HIGH}, and the player's water resource
     * is decremented by 0.1. A success message is printed showing the remaining water.
     * </p>
     * <p>
     * If the player does not have sufficient water, or if the specified tile
     * is empty or out of bounds, an error message is printed and no changes
     * are made to the farm.
     * </p>
     *
     * <p>
     * Usage example:
     * <pre>
     *   irrigate(new String[]{"water", "2", "0"}); // waters the crop at row 2, column 0
     * </pre>
     * </p>
     *
     * <p>
     * Effects:
     * <ul>
     *     <li>Sets the hydration of the targeted crop to {@link Hydration#HIGH}.</li>
     *     <li>Decrements the player's water resource by 0.1.</li>
     *     <li>Prints a success message with the remaining water, or an error if watering fails.</li>
     * </ul>
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] – the command name "water"</li>
     *                 <li>args[1] – the row index of the crop to water</li>
     *                 <li>args[2] – the column index of the crop to water</li>
     *             </ul>
     */
    private void irrigate(String[] args) {
        if(args.length < 3) {
            console().println(Localization.lang.t("game.irrigate.usage"), Console.PURPLE);
            return;
        }

        if(water > 0) {
            Tile tile = tiles[Integer.parseInt(args[1])][Integer.parseInt(args[2])];
            if(tile != null && tile.crop() != null) {
                tile.crop().water(Hydration.HIGH);
            }
            water -= 0.1f;
            waterUsed += 0.1f;
            console().println(Localization.lang.t("game.irrigate.success", water),
                    Console.BRIGHT_GREEN);
        } else {
            console().error(Localization.lang.t("game.irrigate.fail"));
        }
    }

    /**
     * Updates the season every 30 days, and yes, it affects
     * which and when it grows
     */
    private void season() {
        if(days % 30 == 0) {
            season = season.next();
            console().println(Localization.lang.t("game.season.new",
                    season.getKey()), Console.PURPLE);
        }
    }

    /**
     * Randomly sets the weather for the day and increments
     * dry day counter if necessary.
     */
    private void weather() {
        weather = Weather.getRandomWeather();
        if(Objects.equals(weather, Weather.DRY)) {
            dryDay++;
        } else {
            dryDay = 0;
        }
        console().println(weather.message(), Console.CYAN);
    }

    /**
     * Plants a crop on the farm at a specified location or across all tiles if
     * the player has the {@link Upgrades#PLANT_UPGRADE} upgrade and uses the "all" keyword.
     * <p>
     * If the "all" keyword is used with the appropriate upgrade, a new random
     * crop is planted on every farm tile.
     * </p>
     * <p>
     * When planting at a specific tile, the method validates the row and column
     * indices, ensures the tile is within bounds, and that it is unoccupied.
     * If the tile is already occupied, an error message is printed.
     * </p>
     * <p>
     * Upon successful planting:
     * <ul>
     *     <li>A new {@link Crop} instance is created with a random ID based on
     *         the current {@link Seasons}.</li>
     *     <li>The crop is placed in a new {@link Tile} at the specified location.</li>
     *     <li>A confirmation message is printed to the console.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Usage examples:
     * <pre>
     *   plant(new String[]{"plant", "2", "0"}); // plants a crop at row 2, column 0
     *   plant(new String[]{"plant", "all"});   // plants crops on all tiles if PLANT upgrade unlocked
     * </pre>
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] – the command name "plant"</li>
     *                 <li>args[1] – either the row index or the keyword "all" (if upgrade unlocked)</li>
     *                 <li>args[2] – the column index (required if planting a specific tile)</li>
     *             </ul>
     */
    private void plant(String[] args) {
        if(args.length >= 2 && upgrades.contains(Upgrades.PLANT_UPGRADE)
                && console().equals(args[1], "all")) {
            for(Pos pos : letter()) {
                int row = pos.row();
                int col = pos.col();
                if(tiles[row][col] == null) {
                    tiles[row][col] = new Tile(new Crop(CropID.id.random(season)),
                            Soil.SILT, Fertilizer.NONE);
                }
            }
            console().println(Localization.lang.t("game.plant.success.all"),
                    Console.BRIGHT_GREEN);
            return;
        }

        if(args.length < 3) {
            console().println(Localization.lang.t("game.plant.usage"), Console.PURPLE);
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            console().error(Localization.lang.t("game.coordinates.invalid"));
            return;
        }

        if(row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            console().error(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        if(tiles[row][col] != null) {
            console().error(Localization.lang.t("game.plant.occupied"));
            return;
        }

        tiles[row][col] = new Tile(new Crop(CropID.id.random(season)),
                Soil.SILT, Fertilizer.NONE);
        console().println(Localization.lang.t("game.plant.success", row, col),
                Console.BRIGHT_GREEN);
    }

    /**
     * Applies a specified {@link Fertilizer} to a farm tile at the given coordinates,
     * or to multiple tiles if the appropriate upgrade and arguments are provided.
     * <p>
     * The method parses the fertilizer type and target coordinates from the input
     * arguments, validates them, and updates the corresponding {@link Tile} by
     * attaching the selected fertilizer.
     * </p>
     *
     * <p><b>Usage:</b></p>
     * <ul>
     *     <li>{@code fertilize <type> <row> <col>} – applies the given fertilizer
     *         to a single tile.</li>
     *     <li>{@code fertilize <type> all} – applies the fertilizer using special
     *         logic if the {@link Upgrades#FERTILIZER_UPGRADE} upgrade is unlocked.</li>
     * </ul>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *     <li>Validates that coordinates are numeric and within farm bounds.</li>
     *     <li>Resolves the fertilizer type from the provided argument.</li>
     *     <li>Updates the tile by replacing it with a new instance containing the
     *         selected fertilizer.</li>
     *     <li>Prints a success message upon successful application.</li>
     * </ul>
     *
     * <p><b>Errors:</b></p>
     * <ul>
     *     <li>If fertilizer is invalid, an error is printed and operation aborted</li>
     *     <li>If fertilizer is already applied, an error is printed and operation aborted</li>
     *     <li>If coordinates are invalid or not numeric, an error is printed.</li>
     *     <li>If arguments are insufficient, usage instructions are displayed.</li>
     *     <li>If coordinates are out of bounds, the operation is aborted.</li>
     * </ul>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *     <li>Fertilizer effects are applied at the tile level and influence crop
     *         growth, yield, or water retention depending on type.</li>
     *     <li>This method does not validate whether the tile already contains a crop.</li>
     *     <li>Fertilizer matching is based on enum name equality.</li>
     * </ul>
     *
     * @param args command arguments where:
     *             <ul>
     *                 <li>{@code args[0]} – command name ("fertilize")</li>
     *                 <li>{@code args[1]} – fertilizer type (must match {@link Fertilizer} enum)</li>
     *                 <li>{@code args[2]} – row index or keyword "all"</li>
     *                 <li>{@code args[3]} – column index (required for single-tile application)</li>
     *             </ul>
     */
    private void fertilize(String[] args) {
        int row, col;
        try {
            row = Integer.parseInt(args[2]);
            col = Integer.parseInt(args[3]);
        } catch(NumberFormatException e) {
            console().error(Localization.lang.t("game.coordinates.invalid"));
            return;
        }

        if(args.length >= 3 && console().equals(args[2], "all")
                && upgrades.contains(Upgrades.FERTILIZER_UPGRADE)) {

            Fertilizer fertilizer = Arrays.stream(Fertilizer.values())
                    .filter(f -> f.name().equalsIgnoreCase(args[1]))
                    .findFirst()
                    .orElse(null);

            if(inventory().getQuantity(fertilizer) < SIZE) {
                console().error(Localization.lang.t("game.fertilize.fail"));
                return;
            }

            while(inventory().getQuantity(fertilizer) > SIZE) {
                for(int r = 0; r < SIZE; r++) {
                    for(int c = 0; c < SIZE; c++) {
                        tiles[r][c] = tiles[r][c].withFertilizer(fertilizer);
                        inventory().remove(fertilizer);
                    }
                }
            }

            console().println(Localization.lang.t("game.fertilize.success.all"));
            return;
        }

        if(args.length < 4) {
            console().println(Localization.lang.t("game.fertilize.usage"), Console.PURPLE);
            return;
        }

        if(row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            console().error(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        Fertilizer fertilizer = null;
        for(Fertilizer f : Fertilizer.values()) {
            if(f.name().equalsIgnoreCase(args[1])) {
                fertilizer = f;
            }
        }

        if(fertilizer == null) {
            console().println(Localization.lang.t("game.fertilize.invalid"),
                    Console.RED);
            return;
        }

        if(tiles[row][col].fertilizer() != Fertilizer.NONE) {
            console().println(Localization.lang.t("game.fertilize.done"),
                    Console.RED);
            return;
        }

        Tile tile = tiles[row][col].withFertilizer(fertilizer);
        tiles[row][col] = tile;
        inventory().remove(fertilizer);
        console().println(Localization.lang.t("game.fertilize.success", row, col),
                Console.BRIGHT_GREEN);
    }

    /**
     * Displays information about the crop at a specified location on the farm.
     * <p>
     * This method prints the crop's ID and its coordinates without modifying the farm.
     * It validates that the row and column indices are provided, numeric, and within bounds.
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] is the command name ("get")</li>
     *                 <li>args[1] is the row index of the crop</li>
     *                 <li>args[2] is the column index of the crop</li>
     *             </ul>
     */
    private void get(String[] args) {
        if(args.length < 3) {
            console().println(Localization.lang.t("game.get_crop.usage"),
                    Console.PURPLE);
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            console().error(Localization.lang.t("game.coordinates.invalid"));
            return;
        }

        if(row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            console().error(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        Tile tile = tiles[row][col];
        if(tile != null && tile.crop() != null) {
            String id = tile.crop().getId().getName();
            console().println(Localization.lang.t("game.get_crop", id, row, col),
                    Console.PURPLE);
        }
    }

    /**
     * Removes (rips) the crop at a specified location on the farm.
     * <p>
     * This method deletes the crop at the given coordinates, making the plot empty.
     * It validates that the row and column indices are provided, numeric, and within bounds.
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] is the command name ("rip")</li>
     *                 <li>args[1] is the row index of the crop to remove</li>
     *                 <li>args[2] is the column index of the crop to remove</li>
     *             </ul>
     */
    private void rip(String[] args) {
        if(args.length < 3) {
            console().println(Localization.lang.t("game.rip.usage"), Console.PURPLE);
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            console().error(Localization.lang.t("game.coordinates.invalid"));
            return;
        }

        if(row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            console().error(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        tiles[row][col] = null;
        console().println(Localization.lang.t("game.rip.success", row, col),
                Console.BRIGHT_GREEN);
    }

    /**
     * Skips to the next day, updates hydration, and shows coin status.
     * @param args optional command arguments
     */
    private void sleep(String[] args) {
        hours = HOURS;
        console().println(Localization.lang.t("game.sleep"), Console.CYAN);
        console().println(Localization.lang.t("game.coin", player.purse()),
                Console.YELLOW);
        updateHydration();
    }

    /**
     * With the parsed last command it returns a boolean check of
     * if the last command is sleep
     * @param lastCommand the latest command from the map
     * @return boolean if true command was sleep, false otherwise
     */
    private boolean doSleep(String lastCommand) {
        return console().equals(lastCommand, "sleep");
    }

    /**
     * Sells all crops in the inventory and adds coins to the player.
     */
    private void sellCrops() {
        int totalCoin = 0;

        for(Map.Entry<Item, Integer> entry : new LinkedHashMap<>(inventory()
                .getAll()).entrySet()) {
            Item item = entry.getKey();
            if(item instanceof CropID c) {
                int quantity = entry.getValue();
                totalCoin += c.value() * quantity;
                for(int i = 0; i < quantity; i++) {
                    inventory().remove(c);
                }
            }
        }
        player.earn(totalCoin);
        console().println(Localization.lang.t("game.sold", totalCoin), Console.YELLOW);
    }

    /**
     * Opens the in-game market and allows the player to purchase resources,
     * upgrades, and farm expansions.
     * <p>
     * The market presents a list of purchasable items, each with a fixed cost.
     * The player selects an option by entering its corresponding number.
     * The loop continues while the player has sufficient funds or chooses
     * to keep buying.
     * </p>
     *
     * <p><b>Available purchases:</b></p>
     * <ul>
     *     <li><b>Water</b> – increases the player's available water supply.</li>
     *     <li><b>Fertilizer</b> – adds a bundle of {@link Fertilizer#SPEED} and
     *         {@link Fertilizer#YIELD} items to the player's inventory.</li>
     *     <li><b>For-loop Upgrade</b> – unlocks the {@link Upgrades#FOR_LOOP}
     *         command for repeated execution.</li>
     *     <li><b>Plot Expansion</b> – increases the farm size by a fixed amount,
     *         up to {@code MAX_SIZE}, and initializes new tiles.</li>
     *     <li><b>Upgrades Bundle</b> – unlocks {@link Upgrades#HARVEST_UPGRADE} and
     *         {@link Upgrades#PLANT_UPGRADE} commands.</li>
     * </ul>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *     <li>Displays all available items with aligned pricing.</li>
     *     <li>Validates player input and ensures sufficient funds before purchase.</li>
     *     <li>Deducts coins from the player's purse upon successful purchase.</li>
     *     <li>Applies the corresponding effect immediately.</li>
     *     <li>Terminates early if the player cannot afford a selected item.</li>
     * </ul>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *     <li>The market is rebuilt on each invocation.</li>
     *     <li>Prices are dynamically resolved from the internal {@code market} map.</li>
     *     <li>Farm expansion recalculates tile indices via {@link #resize()}.</li>
     *     <li>Fertilizer is granted in bulk rather than applied directly to tiles.</li>
     * </ul>
     */
    private void buy() {
        market.clear();
        market.put(100, Localization.lang.t("market.water"));
        market.put(200, Localization.lang.t("market.fertilizer"));
        market.put(500, Localization.lang.t("market.for"));
        market.put(8_192, Localization.lang.t("market.plot"));
        market.put(12_288, Localization.lang.t("market.upgrades"));
        boolean isBuying = true;
        do {
            int r = Integer.MAX_VALUE;
            int maxPriceWidth = market.keySet()
                    .stream()
                    .map(k -> k.toString().length())
                    .max(Integer::compare)
                    .orElse(0);

            for(Map.Entry<Integer, String> entry : market.entrySet()) {
                String price = entry.getKey().toString();
                String name = entry.getValue();
                String spaces = " ".repeat(maxPriceWidth - price.length() + 2);
                console().println(price + " gold" + spaces + name, Console.PURPLE);
            }

            while(r > market.size()) {
                r = console().replyNum(Localization.lang.t("market.query") + " ");
            }

            List<Map.Entry<Integer, String>> items = new ArrayList<>(market.entrySet());
            Map.Entry<Integer, String> selected = items.get(r - 1);

            int cost = selected.getKey();
            String item = selected.getValue();

            switch(r) {
                case 1 -> {
                    if(player.purse() < cost) {
                        console().error(Localization.lang.t("market.funds"));
                        return;
                    }

                    player.take(cost);
                    water += 1f;
                    console().println(Localization.lang.t("market.bought",
                            "market.water", player.purse()), Console.BRIGHT_GREEN);
                }
                case 2 -> {
                    if(player.purse() < cost) {
                        console().error(Localization.lang.t("market.funds"));
                        return;
                    }

                    player.take(cost);
                    for(int i = 0; i < 16; i++) {
                        inventory().add(Fertilizer.SPEED);
                        inventory().add(Fertilizer.YIELD);
                    }
                    console().println(Localization.lang.t("market.bought",
                            "market.fertilizer", player.purse()), Console.BRIGHT_GREEN);
                }
                case 3 -> {
                    if(player.purse() < cost) {
                        console().error(Localization.lang.t("market.funds"));
                        return;
                    }

                    player.take(cost);
                    upgrades.add(Upgrades.FOR_LOOP);
                    console().println(Localization.lang.t("market.bought",
                            "market.for", player.purse()), Console.BRIGHT_GREEN);
                }
                case 4 -> {
                    int increase = 2;
                    if(player.purse() < cost) {
                        console().error(Localization.lang.t("game.plot.fail"));
                        return;
                    }

                    if(SIZE + increase > MAX_SIZE) {
                        console().error(Localization.lang.t("game.plot.size"));
                        return;
                    }

                    int oldSize = SIZE;
                    player.take(cost);
                    SIZE += increase;
                    int newPlots = SIZE * SIZE - oldSize * oldSize;
                    resize();
                    console().println(Localization.lang.t("market.bought.plot",
                            newPlots, player.purse()), Console.BRIGHT_GREEN);
                }
                case 5 -> {
                    player.take(cost);
                    upgrades.add(Upgrades.HARVEST_UPGRADE);
                    upgrades.add(Upgrades.PLANT_UPGRADE);
                }
                default -> isBuying = false;
            }
        } while(player.purse() > 0 || isBuying);
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
    private void give(String[] args) {
        if(args.length < 3) {
            console().println(Localization.lang.t("game.give.usage"), Console.PURPLE);
            return;
        }

        String item = args[1];
        int quantity = Integer.parseInt(args[2]);

        for(int i = 0; i < quantity; i++) {
            CropID itemCrop;
            for(CropID c : CropID.values()) {
                if(console().equals(c.getName(), item)) {
                    itemCrop = c;
                    inventory().add(itemCrop);
                    break;
                }
            }
        }

        for(int i = 0; i < quantity; i++) {
            Upgrades upgrade;
            for(Upgrades u : Upgrades.values()) {
                if(console().equals(u.name().toLowerCase(), item)) {
                    upgrade = u;
                    upgrades.add(upgrade);
                    break;
                }
            }
        }

        for(int i = 0; i < quantity; i++) {
            Fertilizer fertilizer;
            for(Fertilizer f : Fertilizer.values()) {
                if(console().equals("f." + f.name().toLowerCase(), item)) {
                    fertilizer = f;
                    inventory().add(fertilizer);
                    item = "fertilizer." + f.name().toLowerCase();
                    break;
                }
            }
        }

        if(console().equals(item, "water")) {
            water += quantity;
        }

        if(console().equals(item, "gold")) {
            player.earn(quantity);
        }

        console().println(Localization.lang.t("game.give.success",
                item, quantity), Console.BRIGHT_GREEN);
        forceEnd();
    }

    /**
     * Shows all items and quantities in the player's inventory.
     */
    private void showInventory() {
        if(inventory().isEmpty()) {
            console().error(Localization.lang.t("game.inventory.empty"));
            return;
        }

        for(Map.Entry<Item, Integer> entry : inventory().getAll().entrySet()) {
            console().println(entry.getKey().getName() + " x" + entry.getValue(),
                    Console.PURPLE);
        }
    }

    /**
     * Shows the current time and/of the day
     */
    private void showTime() {
        int hour = (int) hours;
        int minute = (int) ((hours - hour) * 60);
        console().println(day + " " + days + " - " + String.format("%02d:%02d", hour, minute),
                Console.CYAN);
    }

    /**
     * Displays the current game statistics to the console.
     * <p>
     * The statistics include:
     * <ul>
     *     <li>The overall ending type based on the number of days passed
     *     or if the game is over:
     *         <ul>
     *             <li>Worst Ending – if the game is over</li>
     *             <li>Best Ending – if days &gt; 60</li>
     *             <li>Good Ending – if days are between 15 and 60</li>
     *             <li>Bad Ending – if days &lt; 15</li>
     *         </ul>
     *     </li>
     *     <li>Total commands executed by the player</li>
     *     <li>Total crops in the player's inventory</li>
     *     <li>Number of days passed</li>
     *     <li>Total water used</li>
     *     <li>Player's current level</li>
     *     <li>Player's coin balance</li>
     * </ul>
     * </p>
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private void showStats() {
        console().println();
        StringBuilder sb = new StringBuilder(Localization.lang.t("game.stats"));
        if(isGameOver) {
            sb.append(", Worst Ending");
        } else if(days > 60) {
            sb.append(", Best Ending");
        } else if(days >= 15 && days <= 60) {
            sb.append(", Good Ending");
        } else if(days < 15) {
            sb.append(", Bad Ending");
        } else {}

        console().println(sb.toString(), Console.PURPLE);
        for(Map.Entry<Item, Integer> entries : inventory().getAll().entrySet()) {
            totalCrops += entries.getValue();
        }

        console().println(Localization.lang.t("game.stats.cmd_ran", totalCmd), Console.PURPLE);
        console().println(Localization.lang.t("game.stats.crops", totalCrops), Console.PURPLE);
        console().println(Localization.lang.t("game.stats.days", days), Console.PURPLE);
        console().println(Localization.lang.t("game.stats.waterUsed", waterUsed), Console.PURPLE);
        console().println(Localization.lang.t("game.stats.level", player.level()), Console.PURPLE);
        console().println(Localization.lang.t("game.stats.coin", player.purse()), Console.PURPLE);
    }

    /**
     * Shows available commands to the player.
     * @param args optional command arguments
     */
    private void showHelp(String[] args) {
        console().println("Available commands:", Console.PURPLE);
        for(String cmd : console().cmd().keySet()) {
            console().println(" - " + cmd, Console.CYAN);
        }
    }

    /**
     * Returns a list of all positions on the farm grid.
     * @return 2D array of row-column indices
     */
    private List<Pos> letter() {
        positions = new ArrayList<>(SIZE * SIZE);
        for(int row = 0; row < SIZE; row++) {
            for(int col = 0; col < SIZE; col++) {
                positions.add(new Pos(row, col));
            }
        }
        return positions;
    }

    /**
     * Resizes the farm grid and updates indices array
     * after buying new plots. It's a wrapper for index
     */
    private void resize() {
        for(int row = 0; row < SIZE; row++) {
            for(int col = 0; col < SIZE; col++) {
                if(tiles[row][col] == null) {
                    tiles[row][col] = new Tile(null, Soil.LOAM, Fertilizer.NONE);
                }
            }
        }
        letter();
    }

    /**
     * Repeats the previous command entered by the player.
     * @param args optional command arguments
     */
    private void redo(String[] args) {
        if(previousArgs == null) {
            console().error("No previous command.");
            return;
        }

        Consumer<String[]> action = console().cmd().get(previousArgs[0]);
        if(action != null) {
            action.accept(previousArgs.clone());
        }
    }

    /**
     * Immediately ends the game and displays the apocalypse ending.
     * <p>
     * This method sets the internal game over flag to true and prints
     * a message indicating that the player’s farm has failed due to
     * an in-game catastrophe. The last command is also set to "end"
     * to terminate any ongoing loops.
     * </p>
     */
    private void forceEnd() {
        isGameOver = true;
        console().println(Localization.lang.t("game.end.worst",
                player.name()),Console.BRIGHT_RED);
        lastCommand = "end";
    }

    /**
     * Returns the inventory associated with the current player.
     * <p>
     * This method provides convenient access to the player's
     * {@link Inventory} instance for managing items such as crops,
     * resources, or other collectibles.
     * </p>
     * @return the player's inventory
     */
    private Inventory inventory() {
        return player.inventory();
    }

    /**
     * Returns the console service used for input/output operations.
     * <p>
     * This method provides access to the shared singleton instance
     * of {@link Console} used throughout the application for printing
     * messages and interacting with the command-line interface.
     * </p>
     * @return the global console service instance
     */
    private Console console() {
        return Console.cli;
    }
}
