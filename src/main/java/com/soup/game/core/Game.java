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

    private int SIZE = 12;
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
        String input = console().reply(player.name()).trim();
        if(input.isEmpty()) { return; }
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();
        Consumer<String[]> action = console().cmd().get(command);
        if(action != null) {
            action.accept(parts);
            if(!command.equals(".")) {
                lastCommand = command;
                previousArgs = parts.clone();
            }
        } else {
            console().error("Unknown command");
        }
    }

    /**
     * Registers all available commands
     * and their corresponding actions.
     */
    private void addCommands() {
        console().cmd().put("?", this::showHelp);
        console().cmd().put(".", this::redo);
        console().cmd().put("harvest", this::harvest);
        console().cmd().put("rip", this::rip);
        console().cmd().put("water", this::irrigate);
        console().cmd().put("plant", this::plant);
        console().cmd().put("get", this::get);
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
     * Displays the current farm grid and crop status.
     * Shows [X] for withered crops, [ ] for empty, [H] for harvestable, or
     * the first letter of the growth stage otherwise.
     */
    private void update() {
        console().print("    ");
        for (int col = 0; col < SIZE; col++) {
            console().print(String.format("%-4d", col));
        }
        console().println();

        for(int row = 0; row < SIZE; row++) {
            console().print(String.format("%-3d", row));
            for(int col = 0; col < SIZE; col++) {
                Tile tile = tiles[row][col];
                if(tile == null || tile.crop() == null) {
                    console().print("[ ] ");
                } else if(tile.crop().getHydration() == Hydration.NONE) {
                    tile.crop().wither();
                    console().print("[X] ", Console.BRIGHT_RED);
                } else {
                    console().print("[" + tile.crop().getChar() + "] ",
                            Console.BRIGHT_GREEN);
                }
            }

            console().println();
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
     * Harvests a crop or multiple crops depending on arguments and upgrades.
     * @param args command arguments (row and column indices optional)
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
     * Waters all crops if the player has water available.
     * @param args optional command arguments
     */
    private void irrigate(String[] args) {
        if(water > 0) {
            for(Pos pos : index()) {
                if(water <= 0) { break; }
                Tile tile = tiles[pos.row()][pos.col()];
                if(tile != null && tile.crop() != null) {
                    tile.crop().water(Hydration.HIGH);
                }
                water -= 0.1f;
            }
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
     * Plants a crop at a specific location or across all plots if player has upgrade.
     * @param args command arguments (row and column indices optional)
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
        market.put(8_192, Localization.lang.t("market.plot"));
        market.put(16_384, Localization.lang.t("market.upgrades"));
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
                case 3 -> {
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
                case 4 -> isBuying = false;
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
