package com.soup.game.core;

import com.soup.game.ent.Player;
import com.soup.game.enums.*;
import com.soup.game.intf.Item;
import com.soup.game.intf.World;
import com.soup.game.service.Console;
import com.soup.game.service.Inventory;
import com.soup.game.service.Localization;
import com.soup.game.service.Pos;
import com.soup.game.world.Crop;
import com.soup.game.world.Tile;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a console-based farm game where the player can plant,
 * water, harvest crops, buy plots/upgrades, and manage resources
 * like coins and water.
 * <p>
 * The farm maintains a grid of crops,
 * an inventory, a market, weather and upgrades.
 * </p>
 */
@World
public final class Game {
    private static final int MAX_SIZE = 512;
    private static final float HOURS = 24f;
    private final Player player;
    private final Tile[][] tiles;
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

    /**
     * Initializes a new Farm game.
     * Sets up the farm grid, inventory, commands, market,
     * weather and upgrades. It starts the main game loop.
     */
    public Game() {
        Localization.lang.setLocale(Locale.forLanguageTag("en"));
        this.tiles = new Tile[MAX_SIZE][MAX_SIZE];
        this.player = new Player();
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
        loop();
        showStats();
    }

    /**
     * Main game loop.
     * For each day, it displays the day number, updates season and weather,
     * grows crops, updates the farm display, processes player commands, and
     * resets harvested crops.
     */
    private void loop() {
        while(!console().equals(lastCommand, "end")) {
            console().println(day + " " + days, Console.GREEN);
            season();
            weather();
            update();
            hours = 6f;
            do {
                run();
                hours += 0.2f;
                if(hours >= HOURS || doSleep(lastCommand)) {
                    hours = 0f;
                    days++;
                    grow();
                }
            } while (!doSleep(lastCommand)
                    && !console().equals(lastCommand, "end"));
            resetHarvest();
        }
    }

    /**
     * Processes a single command from the player.
     * prints an error if the command is unknown
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
                String[] parts = cmd.trim().split("\\s+");
                if(parts.length == 0) { continue; }
                String command = parts[0].toLowerCase();
                Consumer<String[]> action = console().cmd().get(command);
                if(action != null) {
                    action.accept(parts);
                    lastCommand = command;
                }
                else console().error("Unknown command: " + parts[0]);
            }

            if(doSleep(lastCommand)) {
                return;
            }
        }
    }

    /**
     * Registers all available commands
     * and their corresponding actions.
     */
    private void addCommands() {
        console().cmd().put("?", this::showHelp);
        console().cmd().put(".", this::redo);
        console().cmd().put("for", this::forLoop);
        console().cmd().put("while", this::whileLoop);
        console().cmd().put("harvest", this::harvest);
        console().cmd().put("rip", this::rip);
        console().cmd().put("water", this::irrigate);
        console().cmd().put("plant", this::plant);
        console().cmd().put("get", this::get);
        console().cmd().put("view", this::update);
        console().cmd().put("show", args -> update());
        console().cmd().put("inv", args -> showInventory());
        console().cmd().put("time", args -> showTime());
        console().cmd().put("sell", args -> sellCrops());
        console().cmd().put("buy", args -> buy());
        console().cmd().put("stats", args -> showStats());
        console().cmd().put("sleep", this::sleep);
        console().cmd().put("end", args -> {});
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
            console().println(Localization.lang.t("game.coordinates.out_of_bounds"),
                    Console.BRIGHT_RED);
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
        console().println(sb.toString());
    }

    /**
     * Executes a command multiple times using a for-like syntax.
     * Usage:
     *  for <times> <command> [args...]
     * Example:
     *  for 4 plant 2 0
     *  → runs "plant 2 0" four times
     * @param args the arguments passed from the console
     */
    private void forLoop(String[] args) {
        if(!upgrades.contains(Upgrades.FOR_LOOP)) {
            return;
        }
        if(args.length < 3) {
            console().println(Localization.lang.t("game.for.usage"),
                    Console.PURPLE);
            return;
        }

        int times;
        try {
            times = Integer.parseInt(args[1]);
            if(times <= 0) throw new NumberFormatException();
        } catch(NumberFormatException e) {
            console().error("Invalid number of times: " + args[1]);
            return;
        }

        String command = args[2];
        String[] commandArgs = Arrays.copyOfRange(args, 2, args.length);
        Consumer<String[]> action = console().cmd().get(command);
        if(action == null) {
            console().error("Unknown command: " + command);
            return;
        }

        for (int i = 0; i < times; i++) {
            String[] internalArgs = commandArgs.clone();
            for (int k = 1; k < internalArgs.length; k++) {
                if(internalArgs[k].startsWith("+i")) {
                    internalArgs[k] = String.valueOf(i);
                }
                if(internalArgs[k].startsWith("+j")) {
                    internalArgs[k] = String.valueOf(i);
                }
            }
            action.accept(internalArgs);
        }
    }

    /**
     * Executes a specified command repeatedly while a given condition evaluates to true.
     * <p>
     * This method allows players to run in-game commands in a loop, based on certain
     * game state conditions, such as available water or coins. The condition is
     * re-evaluated before each iteration, so changes in game state can stop the loop.
     * </p>
     * <p>
     * Usage example:
     * <pre>
     *   whileLoop(new String[]{"while", "water>0", "water"});
     * </pre>
     * This will repeatedly execute the "water" command as long as {@code water > 0}.
     * </p>
     * <p>
     * Supported conditions (as of now):
     * <ul>
     *     <li>{@code water>0} – loops while the player has water remaining.</li>
     *     <li>{@code coins>0} – loops while the player has more than 100 coins.</li>
     * </ul>
     * </p>
     * <p>
     * Notes:
     * <ul>
     *     <li>If an unknown condition is provided, the loop will not execute and an error is printed.</li>
     *     <li>If the command is unknown or invalid, an error is printed and the loop is skipped.</li>
     * </ul>
     * </p>
     *
     * @param args an array of strings representing the loop arguments:
     *             <ul>
     *                 <li>args[0] – the keyword "while"</li>
     *                 <li>args[1] – the condition name (e.g., "water>0")</li>
     *                 <li>args[2] – the command to execute repeatedly</li>
     *                 <li>args[3...] – optional arguments for the command</li>
     *             </ul>
     */
    private void whileLoop(String[] args) {
        if(args.length < 3) {
            console().println(Localization.lang.t("game.while.usage"), Console.PURPLE);
            return;
        }

        String conditionName = args[1];
        String command = args[2];
        String[] commandArgs = Arrays.copyOfRange(args, 2, args.length);
        Consumer<String[]> action = console().cmd().get(command);

        if(action == null) {
            console().error("Unknown command: " + command);
            return;
        }

        Supplier<Boolean> condition = () -> switch(conditionName) {
            case "water>0" -> water > 0;
            case "coins>0" -> player.purse() > 100;
            default -> {
                console().error("Unknown condition: " + conditionName);
                yield false;
            }
        };

        while(condition.get()) {
            action.accept(commandArgs);
        }
    }

    /**
     * Advances the growth of all crops on the farm,
     * except during dry weather.
     */
    private void grow() {
        if(!Objects.equals(weather, Weather.DRY)) {
            for(Pos pos : index()) {
                Tile tile = tiles[pos.row()][pos.col()];
                if(tile != null && tile.crop() != null) {
                    tile.crop().grow();
                }
            }
        }
    }

    /**
     * Harvests crops from the farm at a specified location or, if the player has
     * the {@link Upgrades#HARVEST} upgrade, all harvestable crops on the farm.
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
        if(args.length < 3 && upgrades.contains(Upgrades.HARVEST)
                && console().equals(args[1], "all")) {
            for(Pos pos : index()) {
                int row = pos.row();
                int col = pos.col();
                Tile tile = tiles[row][col];
                inventory().add(tile.crop().getId());
                tile.crop().harvested();
                if(tile.crop().getId().regrows()) {
                    tile.crop().setStage(GrowthStage.SEED);
                } else {
                    tiles[row][col] = null;
                }
                console().println(Localization.lang.t("game.yields",
                        inventory().getQuantity(tile.crop().getId())), Console.PURPLE);
                player.update(tile.crop().getId().getXp());
            }
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
            console().println(Localization.lang.t("game.coordinates.out_of_bounds"),
                    Console.BRIGHT_RED);
            return;
        }

        Tile tile = tiles[row][col];
        if(tile.crop() == null) {
            console().println(Localization.lang.t("game.harvest.nothing"),
                    Console.BRIGHT_RED);
            return;
        }

        if(!tile.crop().canHarvest()) {
            console().println(Localization.lang.t("game.harvest.not_ready"),
                    Console.BRIGHT_RED);
            return;
        }

        inventory().add(tile.crop().getId());
        tile.crop().harvested();
        if(tile.crop().getId().regrows()) {
            tile.crop().setStage(GrowthStage.SEED);
        } else {
            tiles[row][col] = null;
        }

        console().println(Localization.lang.t("game.harvest.success",
                tile.crop().getId().getName(), row, col), Console.BRIGHT_GREEN);
        player.update(tile.crop().getId().getXp());
    }

    /**
     * Resets the harvest state of all crops
     * at the end of the day.
     */
    private void resetHarvest() {
        for(Pos pos : index()) {
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

        for(Pos pos : index()) {
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
     * the player has the {@link Upgrades#PLANT} upgrade and uses the "all" keyword.
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
        if(args.length < 3 && upgrades.contains(Upgrades.PLANT) && console().equals(args[1], "all")) {
            for(Pos pos : index()) {
                tiles[pos.row()][pos.col()] = new Tile(new Crop(CropID.id.random(season)),
                        Soil.SILT, Fertilizer.NONE);
            }
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
            console().println(Localization.lang.t("game.coordinates.out_of_bounds"),
                    Console.BRIGHT_RED);
            return;
        }

        if(tiles[row][col] != null) {
            console().println(Localization.lang.t("game.plant.occupied"),
                    Console.BRIGHT_RED);
            return;
        }

        tiles[row][col] = new Tile(new Crop(CropID.id.random(season)),
                Soil.SILT, Fertilizer.NONE);
        console().println(Localization.lang.t("game.plant.success", row, col),
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
            console().println(Localization.lang.t("game.coordinates.out_of_bounds"),
                    Console.BRIGHT_RED);
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
            console().println(Localization.lang.t("game.coordinates.out_of_bounds"),
                    Console.BRIGHT_RED);
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
     * Opens the market for the player to buy plots,
     * water, or upgrades. More to be added.
     */
    private void buy() {
        market.clear();
        market.put(100, Localization.lang.t("market.water"));
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
                console().println(price + spaces + name + " gold", Console.PURPLE);
            }

            while(r > market.size() + 1) {
                r = console().replyNum(Localization.lang.t("market.query"));
            }

            switch(r) {
                case 1 -> {
                    int cost = 0;
                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(console().equals(entries.getValue(), Localization.lang.t(
                                "market.water"))) {
                            cost = entries.getKey();
                        }
                    }

                    if(player.purse() < cost) {
                        console().println(Localization.lang.t("market.funds"),
                                Console.BRIGHT_RED);
                        return;
                    }

                    player.take(cost);
                    water += 0.5f;
                    console().println(Localization.lang.t("market.bought",
                            "market.water", player.purse()), Console.BRIGHT_GREEN);
                }
                case 2 -> {
                    int cost = 0;
                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(console().equals(entries.getValue(), Localization.lang.t(
                                "market.for"))) {
                            cost = entries.getKey();
                        }
                    }

                    if(player.purse() < cost) {
                        console().println(Localization.lang.t("market.funds"),
                                Console.BRIGHT_RED);
                        return;
                    }

                    player.take(cost);
                    upgrades.add(Upgrades.FOR_LOOP);
                    console().println(Localization.lang.t("market.bought",
                            "market.for", player.purse()), Console.BRIGHT_GREEN);
                }
                case 3 -> {
                    int cost = 0;
                    int increase = 2;

                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(console().equals(entries.getValue(), Localization.lang.t(
                                "market.plot"))) {
                            cost = entries.getKey();
                        }
                    }

                    if(player.purse() < cost) {
                        console().println(Localization.lang.t("game.plot.fail"),
                                Console.BRIGHT_RED);
                        return;
                    }

                    if(SIZE + increase > MAX_SIZE) {
                        console().println(Localization.lang.t("game.plot.size"),
                                Console.BRIGHT_RED);
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
                case 4 -> {
                    int cost = 0;
                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(console().equals(entries.getValue(), Localization.lang.t(
                                "market.upgrades"))) {
                            cost = entries.getKey();
                        }
                    }
                    player.take(cost);
                    upgrades.add(Upgrades.HARVEST);
                    upgrades.add(Upgrades.PLANT);
                }
                default -> isBuying = false;
            }
        } while(player.purse() > 0 || isBuying);
    }

    /**
     * Shows all items and quantities in the player's inventory.
     */
    private void showInventory() {
        if(inventory().isEmpty()) {
            console().println(Localization.lang.t("game.inventory.empty"), Console.BRIGHT_RED);
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
     * Displays current game statistics: total crops, days passed, and coins.
     */
    private void showStats() {
        console().println(Localization.lang.t("game.stats"), Console.PURPLE);
        int totalCrops = 0;
        for(Map.Entry<Item, Integer> entries : inventory().getAll().entrySet()) {
            totalCrops += entries.getValue();
        }
        console().println(Localization.lang.t("game.stats.crops", totalCrops), Console.PURPLE);
        console().println(Localization.lang.t("game.stats.days",days), Console.PURPLE);
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
    private List<Pos> index() {
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
                if (tiles[row][col] == null) {
                    tiles[row][col] = new Tile(null, Soil.LOAM, Fertilizer.NONE);
                }
            }
        }
        index();
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
     * Returns the inventory associated with the current player.
     * <p>This method provides convenient access to the player's
     * {@link Inventory} instance for managing items such as crops,
     * resources, or other collectibles.</p>
     * @return the player's inventory
     */
    private Inventory inventory() {
        return player.inventory();
    }

    /**
     * Returns the console service used for input/output operations.
     * <p>This method provides access to the shared singleton instance
     * of {@link Console} used throughout the application for printing
     * messages and interacting with the command-line interface.</p>
     * @return the global console service instance
     */
    private Console console() {
        return Console.cli;
    }
}
