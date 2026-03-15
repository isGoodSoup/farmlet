package com.soup.game.core;

import com.soup.game.ent.Crop;
import com.soup.game.enums.*;
import com.soup.game.intf.Item;
import com.soup.game.service.Inventory;
import com.soup.game.service.Localization;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

/**
 * Represents a console-based farm game where the player can plant,
 * water, harvest crops, buy plots/upgrades, and manage resources
 * like coins and water.
 * <p>The farm maintains a grid of crops,
 * an inventory, a market, weather and upgrades.</p>
 */

public class Farm {
    private final static int MAX_SIZE = 1024;
    private final Crop[][] crops;
    private final Inventory inventory;
    private final Map<String, Consumer<String[]>> commands;
    private final Map<Integer, String> market;
    private final String user;
    private final String day;
    private final String title;
    private final Scanner scan;
    private final List<Upgrades> upgrades;

    private int[][] indices;
    private int SIZE = 4;
    private int coin;
    private int water;
    private int days;
    private int dryDay;

    private Weather weather;
    private Seasons season;
    private String[] previousArgs;
    private String lastCommand = "";

    /**
     * Initializes a new Farm game.
     * Sets up the farm grid, inventory, commands, market,
     * weather and upgrades. It starts the main game loop.
     */
    public Farm() {
        this.crops = new Crop[MAX_SIZE][MAX_SIZE];
        this.indices = new int[SIZE * SIZE][2];

        Localization.lang.setLocale(Locale.forLanguageTag("en"));
        final String NAME = Localization.lang.t("game.farm");
        this.user = Paths.get(System.getProperty("user.home")).getFileName().toString();
        this.title = Localization.lang.t("game.farm.title", user, NAME);

        this.inventory = new Inventory();
        this.commands = new LinkedHashMap<>();
        this.market = new LinkedHashMap<>();
        addCommands();

        this.scan = new Scanner(System.in);
        this.day = Localization.lang.t("game.day");
        this.weather = Weather.SUNNY;
        this.season = Seasons.SPRING;
        this.upgrades = new ArrayList<>();
        upgrades.add(Upgrades.NULL);

        ASCIILogo.print();
        println(Localization.lang.t("game.welcome", title));
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
        days = 0; coin = 0;
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
        do {
            println(day + " " + days);
            season();
            weather();
            grow();
            update();
            do {
                run();
            } while (!equals(lastCommand, "skip") && !equals(lastCommand, "end"));
            resetHarvest();
        } while (!equals(lastCommand, "end"));
    }

    /**
     * Processes a single command from the player.
     * prints an error if the command is unknown
     */
    private void run() {
        String input = reply(user).trim();
        if(input.isEmpty()) { return; }
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();
        Consumer<String[]> action = commands.get(command);
        if(action != null) {
            action.accept(parts);
            lastCommand = command;
            previousArgs = parts.clone();
        } else {
            error("Unknown command");
        }
    }

    /**
     * Registers all available commands
     * and their corresponding actions.
     */
    private void addCommands() {
        commands.put("?", this::showHelp);
        commands.put(".", this::redo);
        commands.put("harvest", this::harvest);
        commands.put("water", this::irrigate);
        commands.put("plant", this::plant);
        commands.put("show", args -> update());
        commands.put("inv", args -> showInventory());
        commands.put("sell", args -> sellCrops());
        commands.put("buy", args -> buy());
        commands.put("stats", args -> showStats());
        commands.put("sleep", this::sleep);
        commands.put("skip", args -> days++);
        commands.put("end", args -> {});
    }

    /**
     * Displays the current farm grid and crop status.
     * Shows [X] for withered crops, [ ] for empty, [H] for harvestable, or
     * the first letter of the growth stage otherwise.
     */
    private void update() {
        for(int row = 0; row < SIZE; row++) {
            for(int col = 0; col < SIZE; col++) {
                Crop crop = crops[row][col];
                if(dryDay > 4) {
                    print("[X] ");
                    if(crop != null) { crop.wither(); }
                } else if(crop == null) {
                    print("[ ] ");
                } else {
                    print(crop.wasHarvestedToday() ? "[ ] "
                            : crop.canHarvest() ? "[H] "
                            : "[" + crop.getStage().name().charAt(0) + "] ");
                }
            }
            println();
        }
    }

    /**
     * Advances the growth of all crops on the farm,
     * except during dry weather.
     */
    private void grow() {
        if(!Objects.equals(weather, Weather.DRY)) {
            for(int[] pos : index()) {
                Crop crop = crops[pos[0]][pos[1]];
                if(crop != null) crop.grow();
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
                Crop crop = crops[row][col];
                inventory.add(crop.getId());
                crop.harvested();
                if(crop.getId().regrows()) {
                    crop.setStage(GrowthStage.SEED);
                } else {
                    crops[row][col] = null;
                }
                println(Localization.lang.t("game.yields",
                        inventory.getQuantity(crop.getId())));
            }
            return;
        }

        if(args.length < 3) {
            println(Localization.lang.t("game.harvest.usage"));
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            error(Localization.lang.t("game.coordinates.invalid"));
            return;
        }

        if(row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            println(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        Crop crop = crops[row][col];
        if(crop == null) {
            println(Localization.lang.t("game.harvest.nothing"));
            return;
        }

        if(!crop.canHarvest()) {
            println(Localization.lang.t("game.harvest.not_ready"));
            return;
        }

        inventory.add(crop.getId());
        crop.harvested();
        if(crop.getId().regrows()) {
            crop.setStage(GrowthStage.SEED);
        } else {
            crops[row][col] = null;
        }

        println(Localization.lang.t("game.harvest.success",
                crop.getId().getName(), row, col));
    }

    /**
     * Resets the harvest state of all crops
     * at the end of the day.
     */
    private void resetHarvest() {
        for(int[] pos : index()) {
            Crop crop = crops[pos[0]][pos[1]];
            if(crop != null) {
                crop.resetHarvest();
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
            Crop crop = crops[pos[0]][pos[1]];
            Hydration hydration = crop.getHydration();
            switch (hydration) {
                case NONE -> {
                    noneCount++;
                    crop.wither();
                }
                case LOW -> {
                    lowCount++;
                    crop.water(Hydration.NONE);
                }
                case MID -> {
                    midCount++;
                    crop.water(Hydration.LOW);
                }
                case HIGH -> {
                    highCount++;
                    crop.water(Hydration.MID);
                }
                case MAX -> {
                    maxCount++;
                    crop.water(Hydration.HIGH);
                }
            }
        }

        int[] counts = {noneCount, lowCount, midCount, highCount, maxCount};
        int average = Arrays.stream(counts).sum()/counts.length;
        println(Localization.lang.t("game.irrigate_crops", average));
    }

    /**
     * Waters all crops if the player has water available.
     * @param args optional command arguments
     */
    private void irrigate(String[] args) {
        if(water > 0) {
            for(int[] pos : index()) {
                Crop crop = crops[pos[0]][pos[1]];
                crop.water(Hydration.HIGH);
                water -= 1;
            }
            println(Localization.lang.t("game.irrigate.success", water));
        } else {
            error(Localization.lang.t("game.irrigate.fail"));
        }
    }

    /**
     * Updates the season every 30 days, and yes, it affects
     * which and when it grows
     */
    private void season() {
        if(days % 30 == 0) {
            season = season.next();
            println(Localization.lang.t("game.season.new",
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
        println(weather.message());
    }

    /**
     * Plants a crop at a specific location or across all plots if player has upgrade.
     * @param args command arguments (row and column indices optional)
     */
    private void plant(String[] args) {
        if(args.length < 3 && upgrades.contains(Upgrades.PLANT)) {
            for(int[] pos : index()) {
                crops[pos[1]][pos[2]] = new Crop(CropID.random(season));
            }
            return;
        }

        if(args.length < 3) {
            println(Localization.lang.t("game.plant.usage"));
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            error(Localization.lang.t("game.coordinates.invalid"));
            return;
        }

        if(row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            println(Localization.lang.t("game.coordinates.out_of_bounds"));
            return;
        }

        if(crops[row][col] != null) {
            println(Localization.lang.t("game.plant.occupied"));
            return;
        }

        crops[row][col] = new Crop(CropID.random(season));
        println(Localization.lang.t("game.plant.success", row, col));
    }

    /**
     * Skips to the next day, updates hydration, and shows coin status.
     * @param args optional command arguments
     */
    private void sleep(String[] args) {
        println(Localization.lang.t("game.sleep"));
        println(Localization.lang.t("game.coin", coin));
        days++;
        updateHydration();
        lastCommand = "skip";
    }

    /**
     * Sells all crops in the inventory and adds coins to the player.
     */
    private void sellCrops() {
        int totalCoin = 0;

        for(Map.Entry<Item, Integer> entry : new LinkedHashMap<>(inventory
                .getAll()).entrySet()) {
            Item item = entry.getKey();
            if(item instanceof CropID c) {
                int quantity = entry.getValue();
                totalCoin += c.value() * quantity;
                for(int i = 0; i < quantity; i++) {
                    inventory.remove(c);
                }
            }
        }
        coin += totalCoin;
        println(Localization.lang.t("game.sold", totalCoin));
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
                println(price + spaces + name + " gold");
            }

            while(!(r > market.size() + 1)) {
                r = replyNum(Localization.lang.t("market.query"));
            }

            switch(r) {
                case 1 -> {
                    int cost = 0;
                    int increase = 2;

                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(equals(entries.getValue(), Localization.lang.t(
                                "market.plot"))) {
                            cost = entries.getKey();
                        }
                    }

                    if(coin < cost) {
                        println(Localization.lang.t("game.plot.fail"));
                        return;
                    }

                    if(SIZE + increase > MAX_SIZE) {
                        println(Localization.lang.t("game.plot.size"));
                        return;
                    }

                    int oldSize = SIZE;
                    coin -= cost;
                    SIZE += increase;
                    int newPlots = SIZE * SIZE - oldSize * oldSize;
                    resize();
                    println(Localization.lang.t("market.bought.plot",
                            newPlots, coin));
                }
                case 2 -> {
                    int cost = 0;
                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(equals(entries.getValue(), Localization.lang.t(
                                "market.upgrades"))) {
                            cost = entries.getKey();
                        }
                    }
                    coin -= cost;
                    upgrades.add(Upgrades.HARVEST);
                    upgrades.add(Upgrades.PLANT);
                }
                case 3 -> {
                    int cost = 0;
                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(equals(entries.getValue(), Localization.lang.t(
                                "market.water"))) {
                            cost = entries.getKey();
                        }
                    }

                    if(coin < cost) {
                        println(Localization.lang.t("market.funds"));
                        return;
                    }

                    coin -= cost;
                    water += 4;
                    println(Localization.lang.t("market.bought",
                            "market.water", coin));
                }
                case 4 -> isBuying = false;
            }
        } while(coin > 0 || isBuying);
    }

    /**
     * Shows all items and quantities in the player's inventory.
     */
    private void showInventory() {
        if(inventory.isEmpty()) {
            println(Localization.lang.t("game.inventory.empty"));
            return;
        }

        for(Map.Entry<Item, Integer> entry : inventory.getAll().entrySet()) {
            println(entry.getKey().getName() + " x" + entry.getValue());
        }
    }

    /**
     * Displays current game statistics: total crops, days passed, and coins.
     */
    private void showStats() {
        println(Localization.lang.t("game.stats"));
        int totalCrops = 0;
        for(Map.Entry<Item, Integer> entries : inventory.getAll().entrySet()) {
            totalCrops += entries.getValue();
        }
        println(Localization.lang.t("game.stats.crops", totalCrops));
        println(Localization.lang.t("game.stats.days",days));
        println(Localization.lang.t("game.stats.coin", coin));
    }

    /**
     * Shows available commands to the player.
     * @param args optional command arguments
     */
    private void showHelp(String[] args) {
        println("Available commands:");
        for (String cmd : commands.keySet()) {
            println(" - " + cmd);
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
            println("No previous command.");
            return;
        }

        Consumer<String[]> action = commands.get(previousArgs[0]);
        if(action != null) {
            action.accept(previousArgs.clone());
        }
    }

    /**
     * Prints a prompt and reads a line from the console.
     * @param q prompt to display
     * @return user input string
     */
    private String reply(String q) {
        print(q + "$ ");
        return scan.nextLine();
    }

    /**
     * Prints a prompt and reads an integer from the console.
     * @param q prompt to display
     * @return user input integer
     */
    private int replyNum(String q) {
        print(q + "$ ");
        return scan.nextInt();
    }

    /**
     * Prints an error message to standard error.
     * @param str message to print
     */
    private void error(String str) {
        System.err.println(str);
    }

    /**
     * Prints text to standard output without newline.
     * @param str text to print
     */
    private void print(String str) {
        System.out.print(str);
    }

    /**
     * Prints text to standard output with newline.
     * @param str text to print
     */
    private void println(String str) {
        System.out.println(str);
    }

    /**
     * Prints a blank line to standard output.
     */
    private void println() {
        System.out.println();
    }

    /**
     * Compares two strings ignoring case.
     * @param str1 first string
     * @param str2 second string
     * @return true if strings are equal ignoring case
     */
    private boolean equals(String str1, String str2) {
        return str1.equalsIgnoreCase(str2);
    }
}
