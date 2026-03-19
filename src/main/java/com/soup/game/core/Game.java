package com.soup.game.core;

import com.soup.game.cmd.Executor;
import com.soup.game.cmd.Parser;
import com.soup.game.cmd.Registry;
import com.soup.game.ent.Player;
import com.soup.game.enums.Gamerule;
import com.soup.game.intf.World;
import com.soup.game.service.Console;
import com.soup.game.service.Localization;
import com.soup.game.service.Stats;
import com.soup.game.world.Barn;
import com.soup.game.world.Environment;
import com.soup.game.world.Farm;
import com.soup.game.world.Market;

import java.util.List;
import java.util.Locale;

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
 * @author isGoodSoup
 * @version 2.0
 * @since 1.0
 */
@World
@SuppressWarnings("all")
public final class Game {
    private static final float HOURS = 24f;
    private final GameLoop gameLoop;
    private final Player player;
    private final Farm farm;
    private final Barn barn;
    private final Environment env;
    private final Market market;
    private final String day;

    private final Parser parser;
    private final Registry registry;
    private final Executor executor;

    /**
     * Initializes a new Farm game.
     * Sets up the farm grid, inventory, commands, market,
     * weather and upgrades. It starts the main game loop.
     */
    public Game() {
        Localization.lang.setLocale(Locale.forLanguageTag("en"));
        this.player = new Player();
        this.env = new Environment();
        this.farm = new Farm(player, env);
        this.barn = new Barn(player);
        this.market = new Market(player);

        this.parser = new Parser();
        this.registry = new Registry();
        this.executor = new Executor(player, parser, registry);
        this.gameLoop = new GameLoop(farm, barn, env, executor);
        addCommands();

        this.day = Localization.lang.t("game.day");
        Console.cli.println("Farmlet, a terminal farm", Console.PURPLE);
        Console.cli.println(Localization.lang.t("game.welcome", player.title()),
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
     * Starts the main game sequence.
     * <p>
     * This method performs the following steps in order:
     * </p>
     * <ol>
     *     <li>Begins the main game loop by calling {@link GameLoop#start()}.</li>
     *     <li>Displays the game ending message based on the number of days
     *         the player survived using {@link #showEnding()}.</li>
     *     <li>Shows the final game statistics for the player via
     *         {@link com.soup.game.service.Stats#showStats(com.soup.game.ent.Player)}.</li>
     * </ol>
     * <p>
     * It is called once when the game is initialized to run the simulation
     * until completion, handling day progression, player actions, and ending logic.
     * </p>
     * <p><b>Notes:</b></p>
     * <ul>
     *     <li>All game events, such as farming, market transactions, and inventory updates,
     *         occur inside {@link GameLoop#start()}.</li>
     *     <li>Once this method completes, the game has effectively ended for the session.</li>
     * </ul>
     */
    private void start() {
        gameLoop.start();
        showEnding();
        Stats.stat.showStats(player);
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
        if(Stats.stat.days > 60) {
            Console.cli.println(Localization.lang.t("game.end.best", Stats.stat.days),
                    Console.BRIGHT_GREEN);
        } else if(Stats.stat.days >= 15 && Stats.stat.days < 60) {
            Console.cli.println(Localization.lang.t("game.end.good", Stats.stat.days),
                    Console.BRIGHT_YELLOW);
        } else if(Stats.stat.days < 15) {
            Console.cli.println(Localization.lang.t("game.end.bad", Stats.stat.days),
                    Console.PURPLE);
        }
    }

    /**
     * Registers all in-game commands with the {@code registry}.
     * <p>
     * This method maps string commands to their respective actions using lambda expressions or method references.
     * Commands cover player interactions, farm management, market operations, and game information.
     * </p>
     *
     * <p><b>Available Commands:</b></p>
     * <ul>
     *     <li><b>?</b> – Shows the list of available commands by calling {@link Stats#showHelp}.</li>
     *     <li><b>.</b> – Repeats the last executed command using {@code executor.redo()}.</li>
     *     <li><b>give</b> – Adds items to the player using {@code executor.give()}.</li>
     *     <li><b>gamerule</b> – Changes or queries game rules via {@link #gamerule(String[])}.</li>
     *     <li><b>var</b> – Executes variable-related commands through {@code executor.var()}.</li>
     *     <li><b>harvest</b> – Harvests crops from the farm using {@code farm.harvest()}.</li>
     *     <li><b>rip</b> – Removes or destroys crops from the farm via {@code farm.rip()}.</li>
     *     <li><b>water</b> – Irrigates crops by calling {@code farm.irrigate()}.</li>
     *     <li><b>plant</b> – Plants seeds using {@code farm.plant()}.</li>
     *     <li><b>fertilize</b> – Applies fertilizer to crops via {@code farm.fertilize()}.</li>
     *     <li><b>get</b> – Retrieves crop data from the farm using {@code farm.get()}.</li>
     *     <li><b>feed</b> – Feeds all animals in the barn via {@code barn.feedAll()}.</li>
     *     <li><b>pet</b> – Interacts with animals by petting them through {@code barn.pet()}.</li>
     *     <li><b>view</b> – Updates the game display using {@code gameLoop.update()}.</li>
     *     <li><b>show</b> – Alias for <b>view</b>; refreshes the game screen.</li>
     *     <li><b>inv</b> – Displays the player's inventory via {@code player.inventory().showInventory(player)}.</li>
     *     <li><b>time</b> – Shows the current day and time using {@link Stats#showTime}.</li>
     *     <li><b>sell</b> – Sells all crops in the player's inventory using {@link Market#sellCrops()}.</li>
     *     <li><b>buy</b> – Opens the market for purchases via {@link Market#buy(Farm)}.</li>
     *     <li><b>stats</b> – Displays current player stats with {@link Stats#showStats}.</li>
     *     <li><b>sleep</b> – Advances the day and consumes resources as needed using {@code gameLoop.sleep(player.purse())}.</li>
     *     <li><b>end</b> – Placeholder command; currently does nothing.</li>
     * </ul>
     *
     * <p>
     * All commands are registered via {@code registry.register(String, Consumer<String[]>)}.
     * Commands that require arguments pass the {@code args} array, while others ignore it.
     * </p>
     */
    public void addCommands() {
        registry.register("?", args -> Stats.stat.showHelp(registry));
        registry.register(".", args -> executor.redo());
        registry.register("give", executor::give);
        registry.register("gamerule", this::gamerule);
        registry.register("var", executor::var);
        registry.register("harvest", farm::harvest);
        registry.register("rip", farm::rip);
        registry.register("water", farm::irrigate);
        registry.register("plant", farm::plant);
        registry.register("fertilize", farm::fertilize);
        registry.register("get", farm::get);
        registry.register("feed", args -> barn.feedAll());
        registry.register("pet", args -> barn.pet());
        registry.register("view", gameLoop::update);
        registry.register("show", args -> gameLoop.update());
        registry.register("inv", args -> player.inventory().showInventory(player));
        registry.register("time", args -> Stats.stat.showTime(env, day));
        registry.register("sell", args -> market.sellCrops());
        registry.register("buy", args -> market.buy(farm));
        registry.register("stats", args -> Stats.stat.showStats(player));
        registry.register("sleep", args -> gameLoop.sleep(player.purse()));
        registry.register("end", args -> {});
    }

    /**
     * Returns the list of all game rules currently registered in the game engine.
     * <p>
     * Each {@link Gamerule} in the returned list represents a configurable gameplay
     * mechanic that can be enabled or disabled at runtime. Modifying the values of
     * these rules affects core game behavior such as cheat availability, market access,
     * experience gain, and weather cycles.
     * </p>
     * <p>
     * The returned list is mutable; changes to the {@link Gamerule} values persist
     * across the game session. However, adding or removing elements from the list
     * is not recommended as it may break internal assumptions of the game engine.
     * </p>
     *
     * <h2>Usage Example:</h2>
     * <pre>{@code
     * // Retrieve all game rules
     * List<Gamerule> rules = game.gamerules();
     *
     * // Disable breeding and stop time
     * rules.stream()
     *      .filter(r -> r == Gamerule.ENABLE_BREEDING || r == Gamerule.ENABLE_STOP_TIME)
     *      .forEach(r -> r.setValue(false));
     * }</pre>
     *
     * @return a mutable {@link List} of {@link Gamerule} objects
     * @since 1.0
     */
    private void gamerule(String[] args) {
        if(args.length < 3) {
            Console.cli.println(Localization.lang.t("game.gamerule.usage"), Console.PURPLE);
            return;
        }

        String key = args[1];
        boolean value = Boolean.parseBoolean(args[2]);

        Gamerule rule = Gamerule.fromKey(key);
        if(rule == null) {
            Console.cli.error(Localization.lang.t("game.gamerule.notfound"));
            return;
        }
        rule.setValue(value);
        Console.cli.println(Localization.lang.t("game.gamerule.success",
                        rule.key(), rule.value()), Console.BRIGHT_GREEN);
    }
}
