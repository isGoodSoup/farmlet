package com.soup.game.core;

import com.soup.game.cmd.Executor;
import com.soup.game.enums.Hydration;
import com.soup.game.intf.CommandListener;
import com.soup.game.service.Colors;
import com.soup.game.service.Localization;
import com.soup.game.service.Pos;
import com.soup.game.service.Stats;
import com.soup.game.swing.SwingPanel;
import com.soup.game.world.*;
import com.soup.game.world.Choice;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
    private static final float HOURS_PER_DAY = 24f;
    private static final float TIME_INCREMENT = 0.2f;
    private static final float DAY_START_HOUR = 6f;

    private final Game game;
    private final SwingPanel panel;
    private final Farm farm;
    private final Barn barn;
    private final Environment env;
    private final Executor executor;
    private List<Choice> choices;
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
        this.choices = null;
        panel.setCommandListener(this);
    }

    /**
     * Displays a list of selectable choices to the player and activates choice input mode.
     * <p>
     * This method renders a localized prompt followed by a list of options,
     * each prefixed by its selection key e.g. "A)", "B)", etc.
     * </p>
     *
     * <p>
     * While choices are active, normal game command processing is suspended and
     * input is routed to {@link #choiceInput(String)} until a valid selection is made.
     * </p>
     *
     * @param prompt the localization key for the menu prompt/title
     * @param choices the list of {@link Choice} objects representing available options
     */
    public void choices(String prompt, List<Choice> choices) {
        panel.append(Localization.lang.t(prompt), Colors.BRIGHT_WHITE);
        for(Choice c : choices) {
            panel.append("\n" + c.key() + ") " + Localization.lang.t(c.text()),
                    Colors.BRIGHT_WHITE);
        }
        this.choices = choices;
    }

    /**
     * Handles user input while a choice menu is active.
     * <p>
     * The input is matched against the selection keys of the currently active choices.
     * If a match is found:
     * <ul>
     *     <li>The choice menu is cleared</li>
     *     <li>The corresponding action is executed</li>
     * </ul>
     * </p>
     *
     * <p>
     * If the input does not match any available choice, a localized error message
     * is displayed and the menu remains active.
     * </p>
     *
     * @param input the user input representing a choice selection
     */
    private void choiceInput(String input) {
        input = input.trim();
        for (Choice c : choices) {
            if (c.key().equalsIgnoreCase(input)) {
                c.execute();
                choices = null;
                return;
            }
        }
        panel.append(Localization.lang.t("game.error.selection"),
                Colors.BRIGHT_RED);
    }

    /**
     * Displays the main area interaction menu with contextual options.
     * <p>
     * This menu allows the player to choose between different locations or actions
     * within the current area, such as:
     * <ul>
     *     <li>Checking the farm</li>
     *     <li>Entering the barn</li>
     *     <li>Leaving the area</li>
     * </ul>
     * </p>
     *
     * <p>
     * Each option is represented as a {@link Choice} with a corresponding action
     * that updates the game state or renders relevant information.
     * </p>
     *
     * <p>
     * The menu is rendered using localized text and processed via the
     * choice interaction system.
     * </p>
     */
    public void showArea() {
        choices("game.choice.prompt-1", new ArrayList<>(List.of(
                new Choice("A", "game.menu.main1A", () -> {
                    panel.append(Localization.lang.t("game.menu.choice1A"),
                            Colors.BRIGHT_BLACK);
                    update();
                }),
                new Choice("B", "game.menu.main2A", () -> {
                    panel.append(Localization.lang.t("game.menu.choice2A"),
                            Colors.BRIGHT_BLACK);
                    barn.update();
                }),
                new Choice("C", "game.menu.main3A", () ->
                        panel.append(Localization.lang.t("game.menu.choice3A"),
                        Colors.BRIGHT_BLACK))
        )));
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
        showArea();
    }

    @Override
    public void onCommand(String command) {
        if(Stats.stat().isGameOver) {
            panel.append(Localization.lang.t("game.end.worst"), Colors.MAGENTA);
            return;
        }

        if(choices != null) {
            choiceInput(command);
            return;
        }
        game(command);
    }

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