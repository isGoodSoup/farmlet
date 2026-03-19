package com.soup.game.core;

import com.soup.game.cmd.Executor;
import com.soup.game.enums.Gamerule;
import com.soup.game.enums.Hydration;
import com.soup.game.service.Console;
import com.soup.game.service.Localization;
import com.soup.game.service.Pos;
import com.soup.game.service.Stats;
import com.soup.game.world.Barn;
import com.soup.game.world.Environment;
import com.soup.game.world.Farm;
import com.soup.game.world.Tile;

/**
 * <h1>Game Loop</h1>
 * The {@code GameLoop} class manages the core progression and time-based updates
 * of the farming simulation.
 * <p>
 * This class is responsible for the main game loop, day/night cycles, weather and season updates,
 * entity updates (crops, animals), and determining when the game should end. It acts as the
 * orchestrator between the player's actions (via commands) and the simulation systems.
 * </p>
 *
 * <h2>Loop Structure</h2>
 * <p>
 * The game runs in a continuous loop where each iteration represents a fraction of a day:
 * <ul>
 *     <li>Display current day and time</li>
 *     <li>Update season and weather</li>
 *     <li>Show farm view via {@link #update()} or {@link #update(String[])}</li>
 *     <li>Process player commands</li>
 *     <li>Update animals and environment</li>
 *     <li>Advance time</li>
 *     <li>Check for day end or sleep command</li>
 * </ul>
 * </p>
 *
 * @see Game
 * @see Farm
 * @see Environment
 * @see Barn
 * @author isGoodSoup
 * @version 1.0
 * @since 2.0
 */
public class GameLoop {
    private static final float HOURS_PER_DAY = 24f;
    private static final float TIME_INCREMENT = 0.2f;
    private static final float DAY_START_HOUR = 6f;

    private final Farm farm;
    private final Barn barn;
    private final Environment env;
    private final Executor executor;
    private final String day;
    private String lastCommand;

    /**
     * Creates a new game loop that orchestrates the simulation.
     * @param farm the farm managing crops and tiles
     * @param barn the barn managing animals
     * @param env the environment managing weather and seasons
     * @param executor the command executor for player input
     */
    public GameLoop(Farm farm, Barn barn, Environment env, Executor executor) {
        this.farm = farm;
        this.barn = barn;
        this.env = env;
        this.executor = executor;
        this.day = Localization.lang.t("game.day");
    }

    /**
     * Starts the main game loop and runs until termination.
     * <p>
     * The loop continues until either:
     * <ul>
     *     <li>The player enters the "end" command</li>
     *     <li>The game over flag is set</li>
     * </ul>
     * </p>
     */
    public void start() {
        while(!Console.cli.equals(executor.getLastCommand(), "end") && !Stats.stat.isGameOver) {
            Console.cli.println(day + " " + Stats.stat.days, Console.GREEN);
            env.season();
            env.weather();
            update();
            env.hours(DAY_START_HOUR);
            do {
                executor.run();
                barn.update();
                env.advanceTime(TIME_INCREMENT);

                if(env.hours() >= HOURS_PER_DAY || executor.doSleep()) {
                    env.hours(0f);
                    Stats.stat.days++;
                    farm.grow();
                }
            } while(!executor.doSleep()
                    && !Console.cli.equals(executor.getLastCommand(), "end")
                    && !Stats.stat.isGameOver);
            farm.reset();
        }
    }

    /**
     * Displays the entire farm grid to the console.
     * <p>
     * This method shows each tile as follows:
     * <ul>
     *     <li>[ ] for empty plots</li>
     *     <li>[X] for withered crops</li>
     *     <li>[S], [G], [M], [H] etc. for crops in various growth stages</li>
     * </ul>
     * </p>
     */
    public void update() {
        update(new String[]{"show", "0", "0", String.valueOf(farm.tiles().size()),
                String.valueOf(farm.tiles().size())});
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
     * Colors are applied according to crop growth stage:
     * <ul>
     *     <li>SEED, BUD: purple</li>
     *     <li>GROWING: blue</li>
     *     <li>MATURE: cyan</li>
     *     <li>HARVESTABLE: bright green</li>
     * </ul>
     * </p>
     *
     * @param args an array of command arguments defining the viewport
     */
    public void update(String[] args) {
        if(args.length < 5) {
            Console.cli.println(Localization.lang.t("game.view.usage"),
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
            Console.cli.error(Localization.lang.t("game.coordinates.invalid"));
            return;
        }

        if(startRow < 0 || startRow >= farm.tiles().size()
                || startCol < 0 || startCol >= farm.tiles().size()) {
            Console.cli.error(Localization.lang.t("game.coordinates.out_of_bounds"));
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
                Tile tile = farm.tiles().get(row).get(col);
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
        Console.cli.print(sb.toString());
    }

    /**
     * Updates hydration levels of all crops based on previous water state
     * and withers crops with no water.
     */
    public void updateHydration() {
        int totalHydration = 0;
        int cropCount = 0;

        for(Pos pos : farm.letter()) {
            Tile tile = farm.tiles().get(pos.col()).get(pos.row());
            if(tile != null && tile.crop() != null) {
                tile.crop().decay();
                totalHydration += tile.crop().getHydration().ordinal();
                cropCount++;
            }
        }

        float average = cropCount > 0 ? (float) totalHydration/cropCount : 0f;
        Console.cli.println(Localization.lang.t("game.irrigate_crops", average),
                Console.CYAN);
    }

    /**
     * Skips to the next day, updates hydration, and shows coin status.
     *
     * @param playerCoins the player's current coin balance
     */
    public void sleep(int playerCoins) {
        env.hours(HOURS_PER_DAY);
        Console.cli.println(Localization.lang.t("game.sleep"), Console.CYAN);
        Console.cli.println(Localization.lang.t("game.coin", playerCoins),
                Console.YELLOW);
        updateHydration();
    }

    /**
     * Immediately ends the game and displays the apocalypse ending.
     * <p>
     * This method sets the internal game over flag to true and prints
     * a message indicating that the player's farm has failed due to
     * an in-game catastrophe.
     * </p>
     *
     * @param playerName the name of the player for the ending message
     */
    public void forceEnd(String playerName) {
        if(Gamerule.isEnabled(Gamerule.ENABLE_PUNISHMENT)) {
            Stats.stat.isGameOver = true;
            Console.cli.println(Localization.lang.t("game.end.worst", playerName),
                    Console.BRIGHT_RED);
        }
    }
}