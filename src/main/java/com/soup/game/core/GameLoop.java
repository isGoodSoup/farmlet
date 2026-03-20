package com.soup.game.core;

import com.soup.game.cmd.Executor;
import com.soup.game.enums.Hydration;
import com.soup.game.intf.CommandListener;
import com.soup.game.service.*;
import com.soup.game.swing.SwingPanel;
import com.soup.game.world.*;

import java.awt.*;
import java.util.Random;

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
public class GameLoop implements CommandListener {
    private static final Random random = new Random();
    private static final float HOURS_PER_DAY = 24f;
    private static final float TIME_INCREMENT = 0.2f;
    private static final float DAY_START_HOUR = 6f;

    private final Game game;
    private final SwingPanel panel;
    private final Farm farm;
    private final Barn barn;
    private final Environment env;
    private final Executor executor;
    private final QuestLog questLog;
    private String lastCommand;

    /**
     * Creates a new game loop that orchestrates the simulation.
     * @param panel the rendering layer
     * @param farm the farm managing crops and tiles
     * @param barn the barn managing animals
     * @param env the environment managing weather and seasons
     * @param executor the command executor for player input
     */
    public GameLoop(Game game, SwingPanel panel, Farm farm, Barn barn,
                    Environment env, Executor executor) {
        this.game = game;
        this.panel = panel;
        this.farm = farm;
        this.barn = barn;
        this.env = env;
        this.executor = executor;
        this.questLog = new QuestLog(panel);
        panel.setCommandListener(this);
        NPCFactory.factory.build();
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
        panel.append("\n" + Localization.lang.t("game.day") + " " +
                Stats.stat().days + "\n", Colors.GREEN);
        env.season();
        env.weather();
        update();
    }

    /**
     * Callback triggered when a player enters a command in the UI.
     * <p>
     * If the game is over, displays a message and ignores further input.
     * Otherwise, forwards the command to {@link #game(String)} for processing.
     * </p>
     *
     * @param command the command string entered by the player
     */
    @Override
    public void onCommand(String command) {
        if(Stats.stat().isGameOver) {
            panel.append(Localization.lang.t("game.end.worst"), Colors.MAGENTA);
            return;
        }
        game(command);
    }

    /**
     * Processes a single player command and advances the game state.
     * <p>
     * This method runs the given command via the {@link Executor}, advances
     * the game time by {@link #TIME_INCREMENT}, updates the environment,
     * grows crops at day end, updates animals, and refreshes the farm display.
     * If the command is "end", the game is terminated and the ending is shown.
     * </p>
     *
     * @param command the command string entered by the player
     * @see Executor
     * @see Environment
     * @see Barn
     * @see Farm
     */
    private void game(String command) {
        executor.run(command);
        env.advanceTime(TIME_INCREMENT);
        if (env.hours() >= HOURS_PER_DAY) {
            env.hours(0f);
            Stats.stat().days++;
            farm.grow();
            panel.append(Localization.lang.t("game.day") + " "
                    + Stats.stat().days + "\n", Colors.GREEN);
            env.season();
            env.weather();
        }
        barn.update();
        update();
        if (command.equalsIgnoreCase("end")) {
            Stats.stat().isGameOver = true;
            game.showEnding();
            return;
        }
        farm.reset();
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
            panel.append(Localization.lang.t("game.view.usage"),
                    Colors.PURPLE);
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
            panel.append(Localization.lang.t("game.coordinates.invalid"), Colors.BRIGHT_RED);
            return;
        }

        if(startRow < 0 || startRow >= farm.tiles().size()
                || startCol < 0 || startCol >= farm.tiles().size()) {
            panel.append(Localization.lang.t("game.coordinates.out_of_bounds"), Colors.BRIGHT_RED);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("    ");
        for(int col = startCol; col < endCol; col++) {
            sb.append(String.format("%-3d", col));
        }
        Color color = null;
        sb.append("\n");
        for(int row = startRow; row < endRow; row++) {
            sb.append(String.format("%-3d", row));
            for(int col = startCol; col < endCol; col++) {
                Tile tile = farm.tiles().get(row).get(col);
                if(tile == null || tile.crop() == null) {
                    sb.append("[ ]");
                } else if(tile.crop().getHydration() == Hydration.NONE) {
                    tile.crop().wither();
                    sb.append("[X]");
                } else {
                    switch(tile.crop().getStage()) {
                        case SEED -> color = Colors.BRIGHT_BLUE;
                        case BUD -> color = Colors.YELLOW;
                        case GROWING -> color = Colors.BRIGHT_YELLOW;
                        case MATURE -> color = Colors.GREEN;
                        case HARVESTABLE -> color = Colors.BRIGHT_GREEN;
                    }
                    sb.append("[")
                            .append(tile.crop().getChar()).append("]");
                }
            }
            sb.append("\n");
        }
        panel.append(sb.toString(), color);
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
        panel.append(Localization.lang.t("game.irrigate_crops", average),
                Colors.CYAN);
    }

    /**
     * Skips to the next day, updates hydration, and shows coin status.
     *
     * @param playerCoins the player's current coin balance
     */
    public void sleep(int playerCoins) {
        env.hours(HOURS_PER_DAY);
        panel.append(Localization.lang.t("game.sleep"), Colors.CYAN);
        panel.append(Localization.lang.t("game.coin", playerCoins),
                Colors.YELLOW);
        updateHydration();
    }
}