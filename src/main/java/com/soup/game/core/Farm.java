package com.soup.game.core;

import com.soup.game.ent.Crop;
import com.soup.game.ent.Player;
import com.soup.game.world.Tile;
import com.soup.game.enums.*;
import com.soup.game.intf.Item;
import com.soup.game.intf.World;
import com.soup.game.service.Console;
import com.soup.game.service.Inventory;
import com.soup.game.service.Localization;

import java.util.*;
import java.util.function.Consumer;

/**
 * Represents a console-based farm game where the player can plant,
 * water, harvest crops, buy plots/upgrades, and manage resources
 * like coins and water.
 * <p>The farm maintains a grid of crops,
 * an inventory, a market, weather and upgrades.</p>
 */

@World
public class Farm {
    private static final int MAX_SIZE = 1024;
    private static final float HOURS = 24f;
    private final Player player;
    private final Tile[][] tiles;
    private final Map<Integer, String> market;
    private final List<Upgrades> upgrades;
    private final String day;

    private int[][] indices;
    private int SIZE = 2;
    private int water;
    private int days;
    private int dryDay;
    private float hours;

    private Weather weather;
    private Seasons season;
    private String[] previousArgs;
    private String lastCommand = "foo";

    /**
     * Initializes a new Farm game.
     * Sets up the farm grid, inventory, commands, market,
     * weather and upgrades. It starts the main game loop.
     */
    public Farm() {
        Localization.lang.setLocale(Locale.forLanguageTag("en"));
        this.tiles = new Tile[MAX_SIZE][MAX_SIZE];
        this.indices = new int[SIZE * SIZE][2];
        this.player = new Player();
        this.market = new LinkedHashMap<>();
        this.addCommands();

        this.day = Localization.lang.t("game.day");
        this.weather = Weather.SUNNY;
        this.season = Seasons.SPRING;
        this.upgrades = new ArrayList<>();
        upgrades.add(Upgrades.NULL);

        ASCIILogo.print();
        console().println(Localization.lang.t("game.welcome", player.title()));
        start();
    }

    /**
     * The main method, to start the game
     * @param args
     */
    public static void main(String[] args) {
        new Farm();
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
            console().println(day + " " + days);
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
            lastCommand = command;
            previousArgs = parts.clone();
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
        console().cmd().put("water", this::irrigate);
        console().cmd().put("plant", this::plant);
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
        for(int row = 0; row < SIZE; row++) {
            for(int col = 0; col < SIZE; col++) {
                Tile tile = tiles[row][col];
                if(dryDay > 4) {
                    console().print("[X] ");
                    if(tile != null) { tile.crop().wither(); }
                } else if(tile == null) {
                    console().print("[ ] ");
                } else {
                    console().print(tile.crop().wasHarvestedToday() ? "[ ] "
                            : tile.crop().canHarvest() ? "[H] "
                            : "[" + tile.crop().getStage().name().charAt(0) + "] ");
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
            for(int[] pos : index()) {
                Tile tile = tiles[pos[0]][pos[1]];
                if(tile.crop() != null) tile.crop().grow();
            }
        }
    }

    /**
     * Harvests a crop or multiple crops depending on arguments and upgrades.
     * @param args command arguments (row and column indices optional)
     */
    private void harvest(String[] args) {
        if(args.length < 3 && upgrades.contains(Upgrades.HARVEST)) {
            for(int[] pos : index()) {
                int row = pos[1];
                int col = pos[2];
                Tile tile = tiles[row][col];
                inventory().add(tile.crop().getId());
                tile.crop().harvested();
                if(tile.crop().getId().regrows()) {
                    tile.crop().setStage(GrowthStage.SEED);
                } else {
                    tiles[row][col] = null;
                }
                console().println(Localization.lang.t("game.yields",
                        inventory().getQuantity(tile.crop().getId())));
                player.update(tile.crop().getId().getXp());
            }
            return;
        }

        if(args.length < 3) {
            console().println(Localization.lang.t("game.harvest.usage"));
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
            console().println(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        Tile tile = tiles[row][col];
        if(tile.crop() == null) {
            console().println(Localization.lang.t("game.harvest.nothing"));
            return;
        }

        if(!tile.crop().canHarvest()) {
            console().println(Localization.lang.t("game.harvest.not_ready"));
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
                tile.crop().getId().getName(), row, col));
        player.update(tile.crop().getId().getXp());
    }

    /**
     * Resets the harvest state of all crops
     * at the end of the day.
     */
    private void resetHarvest() {
        for(int[] pos : index()) {
            Tile tile = tiles[pos[0]][pos[1]];
            if(tile.crop() != null) {
                tile.crop().resetHarvest();
            }
        }
    }

    /**
     * Updates hydration levels of all crops based on previous water state
     * and withers crops with no water.
     */
    private void updateHydration() {
        int noneCount = 0, lowCount = 0, midCount = 0, highCount = 0, maxCount = 0;

        for (int[] pos : index()) {
            Tile tile = tiles[pos[0]][pos[1]];
            Hydration hydration = tile.crop().getHydration();
            switch (hydration) {
                case NONE -> {
                    noneCount++;
                    tile.crop().wither();
                }
                case LOW -> {
                    lowCount++;
                    tile.crop().water(Hydration.NONE);
                }
                case MID -> {
                    midCount++;
                    tile.crop().water(Hydration.LOW);
                }
                case HIGH -> {
                    highCount++;
                    tile.crop().water(Hydration.MID);
                }
                case MAX -> {
                    maxCount++;
                    tile.crop().water(Hydration.HIGH);
                }
            }
        }

        int[] counts = {noneCount, lowCount, midCount, highCount, maxCount};
        int average = Arrays.stream(counts).sum()/counts.length;
        console().println(Localization.lang.t("game.irrigate_crops", average));
    }

    /**
     * Waters all crops if the player has water available.
     * @param args optional command arguments
     */
    private void irrigate(String[] args) {
        if(water > 0) {
            for(int[] pos : index()) {
                Tile tile = tiles[pos[0]][pos[1]];
                tile.crop().water(Hydration.HIGH);
                water -= 1;
            }
            console().println(Localization.lang.t("game.irrigate.success", water));
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
                    season.getKey()));
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
        }
        console().println(weather.message());
    }

    /**
     * Plants a crop at a specific location or across all plots if player has upgrade.
     * @param args command arguments (row and column indices optional)
     */
    private void plant(String[] args) {
        if(args.length < 3 && upgrades.contains(Upgrades.PLANT)) {
            for(int[] pos : index()) {
                tiles[pos[1]][pos[2]] = new Tile(new Crop(CropID.random(season)),
                        Soil.SILT, Fertilizer.NONE);
            }
            return;
        }

        if(args.length < 3) {
            console().println(Localization.lang.t("game.plant.usage"));
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
            console().println(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        if(tiles[row][col] != null) {
            console().println(Localization.lang.t("game.plant.occupied"));
            return;
        }

        tiles[row][col] = new Tile(new Crop(CropID.random(season)),
                Soil.SILT, Fertilizer.NONE);
        console().println(Localization.lang.t("game.plant.success", row, col));
    }

    /**
     * Skips to the next day, updates hydration, and shows coin status.
     * @param args optional command arguments
     */
    private void sleep(String[] args) {
        hours = HOURS;
        console().println(Localization.lang.t("game.sleep"));
        console().println(Localization.lang.t("game.coin", player.purse()));
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
        console().println(Localization.lang.t("game.sold", totalCoin));
    }

    /**
     * Opens the market for the player to buy plots,
     * water, or upgrades. More to be added.
     */
    private void buy() {
        market.clear();
        market.put(131_072, Localization.lang.t("market.plot"));
        market.put(65_536, Localization.lang.t("market.upgrades"));
        market.put(4096, Localization.lang.t("market.water"));
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
                console().println(price + spaces + name + " gold");
            }

            while(!(r > market.size() + 1)) {
                r = console().replyNum(Localization.lang.t("market.query"));
            }

            switch(r) {
                case 1 -> {
                    int cost = 0;
                    int increase = 2;

                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(console().equals(entries.getValue(), Localization.lang.t(
                                "market.plot"))) {
                            cost = entries.getKey();
                        }
                    }

                    if(player.purse() < cost) {
                        console().println(Localization.lang.t("game.plot.fail"));
                        return;
                    }

                    if(SIZE + increase > MAX_SIZE) {
                        console().println(Localization.lang.t("game.plot.size"));
                        return;
                    }

                    int oldSize = SIZE;
                    player.take(cost);
                    SIZE += increase;
                    int newPlots = SIZE * SIZE - oldSize * oldSize;
                    resize();
                    console().println(Localization.lang.t("market.bought.plot",
                            newPlots, player.purse()));
                }
                case 2 -> {
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
                case 3 -> {
                    int cost = 0;
                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(console().equals(entries.getValue(), Localization.lang.t(
                                "market.water"))) {
                            cost = entries.getKey();
                        }
                    }

                    if(player.purse() < cost) {
                        console().println(Localization.lang.t("market.funds"));
                        return;
                    }

                    player.take(cost);
                    water += 4;
                    console().println(Localization.lang.t("market.bought",
                            "market.water", player.purse()));
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
            console().println(Localization.lang.t("game.inventory.empty"));
            return;
        }

        for(Map.Entry<Item, Integer> entry : inventory().getAll().entrySet()) {
            console().println(entry.getKey().getName() + " x" + entry.getValue());
        }
    }

    /**
     * Shows the current time and/of the day
     */
    private void showTime() {
        int hour = (int) hours;
        int minute = (int) ((hours - hour) * 60);
        console().println(day + " " + days + " - " + String.format("%02d:%02d", hour, minute));
    }

    /**
     * Displays current game statistics: total crops, days passed, and coins.
     */
    private void showStats() {
        console().println(Localization.lang.t("game.stats"));
        int totalCrops = 0;
        for(Map.Entry<Item, Integer> entries : inventory().getAll().entrySet()) {
            totalCrops += entries.getValue();
        }
        console().println(Localization.lang.t("game.stats.crops", totalCrops));
        console().println(Localization.lang.t("game.stats.days",days));
        console().println(Localization.lang.t("game.stats.coin", player.purse()));
    }

    /**
     * Shows available commands to the player.
     * @param args optional command arguments
     */
    private void showHelp(String[] args) {
        console().println("Available commands:");
        for (String cmd : console().cmd().keySet()) {
            console().println(" - " + cmd);
        }
    }

    /**
     * Returns a list of all positions on the farm grid.
     * @return 2D array of row-column indices
     */
    private int[][] index() {
        List<int[]> positions = new ArrayList<>();
        for(int row = 0; row < SIZE; row++) {
            for(int col = 0; col < SIZE; col++) {
                positions.add(new int[]{row, col});
            }
        }
        return positions.toArray(new int[0][0]);
    }

    /**
     * Resizes the farm grid and updates indices array
     * after buying new plots.
     */
    private void resize() {
        indices = new int[SIZE * SIZE][2];
    }

    /**
     * Repeats the previous command entered by the player.
     * @param args optional command arguments
     */
    private void redo(String[] args) {
        if(previousArgs == null) {
            console().println("No previous command.");
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
