package com.soup.game.core;

import com.soup.game.ent.Crop;
import com.soup.game.ent.Water;
import com.soup.game.enums.CropID;
import com.soup.game.enums.GrowthStage;
import com.soup.game.enums.Weather;
import com.soup.game.intf.Item;
import com.soup.game.service.Inventory;
import com.soup.game.service.Localization;

import java.nio.file.Paths;
import java.util.*;

public class Farm {
    private final static int MAX_SIZE = 1024;
    private final Crop[][] crops;
    private final Inventory inventory;
    private final Map<String, Runnable> commands;
    private final Map<Integer, String> market;
    private final String user;
    private final String day;
    private final String title;
    private final Scanner scan;

    private int[][] indices;
    private int SIZE = 16;
    private int coin;
    private int days;
    private int dryDay;

    private Weather weather;
    private String cmd = "";
    private String previousCmd = "";

    public Farm() {
        this.crops = new Crop[MAX_SIZE][MAX_SIZE];
        this.indices = new int[SIZE * SIZE][2];
        Localization.lang.setLocale(Locale.forLanguageTag("en"));
        final String NAME = Localization.lang.t("game.farm");
        this.user = Paths.get(System.getProperty("user.home")).getFileName().toString();
        this.title = Localization.lang.t("game.farm.title", user, NAME);
        println(Localization.lang.t("game.welcome", title));

        this.inventory = new Inventory();
        this.commands = new LinkedHashMap<>();
        this.market = new LinkedHashMap<>();
        addCommands();

        this.scan = new Scanner(System.in);
        this.day = Localization.lang.t("game.day");
        this.weather = Weather.SUNNY;
        start();
    }

    private void start() {
        days = 0; coin = 0;
        plant(true);
        loop();
        showStats();
    }

    private void loop() {
        do {
            println(day + " " + days);
            season();
            weather();
            grow();
            update();
            do {
                cmd = run();
            } while (!equals(cmd, "skip") && !equals(cmd, "end"));
            resetHarvest();
        } while (!equals(cmd, "end"));
    }

    private String run() {
        cmd = reply(user);
        Runnable action = commands.get(cmd.toLowerCase());
        if(action != null) { action.run(); }
        if(!equals(cmd, ".") && !equals(cmd, "..")) {
            previousCmd = cmd;
        }
        return cmd;
    }

    private void addCommands() {
        commands.put(".", this::redo);
        commands.put("harvest", this::harvest);
        commands.put("replant", () -> this.plant(false));
        commands.put("show", this::update);
        commands.put("inv", this::showInventory);
        commands.put("sell", this::sellCrops);
        commands.put("buy", this::buy);
        commands.put("stats", this::showStats);
        commands.put("sleep", this::sleep);
        commands.put("skip", () -> days++);
        commands.put("end", () -> {});
    }

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

    private void grow() {
        if(!Objects.equals(weather, Weather.DRY)) {
            for(int[] pos : index()) {
                Crop crop = crops[pos[0]][pos[1]];
                if(crop != null) crop.grow();
            }
        }
    }

    private void harvest() {
        List<Item> todayHarvest = new ArrayList<>();
        for(int[] pos : index()) {
            Crop crop = crops[pos[0]][pos[1]];
            if(crop != null && crop.canHarvest()) {
                todayHarvest.add(crop.getId());
                crop.harvested();
                if(crop.getId().regrows()) {
                    crop.setStage(GrowthStage.SEED);
                } else {
                    crops[pos[0]][pos[1]] = null;
                }
            }
        }
        inventory.addAll(todayHarvest);

        if(!todayHarvest.isEmpty()) {
            Map<Item, Integer> countMap = new LinkedHashMap<>();
            for(Item item : todayHarvest) {
                countMap.merge(item, 1, Integer::sum);
            }
            for(Map.Entry<Item, Integer> e : countMap.entrySet()) {
                println(Localization.lang.t("game.yields", e.getKey().getName(), e.getValue()));
            }
        }
    }

    private void resetHarvest() {
        for(int[] pos : index()) {
            Crop crop = crops[pos[0]][pos[1]];
            if(crop != null) {
                crop.resetHarvest();
            }
        }
    }

    private void season() {
        // TODO seasons logic
    }

    private void weather() {
        weather = Weather.getRandomWeather();
        if(Objects.equals(weather, Weather.DRY)) {
            dryDay++;
        }
        println(weather.message());
    }

    @SuppressWarnings("SimplifiableBooleanExpression")
    private void plant(boolean isFirstTime) {
        for(int row = 0; row < SIZE; row++) {
            for(int col = 0; col < SIZE; col++) {
                if(crops[row][col] == null) {
                    if(((Math.random() < 0.5) && isFirstTime) || !isFirstTime) {
                        crops[row][col] = new Crop(CropID.random());
                    }
                }
            }
        }
    }

    private void sleep() {
        harvest();
        println(Localization.lang.t("game.sleep"));
        println(Localization.lang.t("game.coin", coin));
        days++;
        cmd = "skip";
    }

    private void sellCrops() {
        int totalCoin = 0;

        for(Map.Entry<Item, Integer> entry : new LinkedHashMap<>(inventory.getAll()).entrySet()) {
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
                        if(equals(entries.getValue(), Localization.lang.t("market.plot"))) {
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
                    SIZE += increase;
                    coin -= cost;
                    int newPlots = SIZE * SIZE - oldSize * oldSize;
                    resize();
                    println(Localization.lang.t("market.bought.plot",
                            newPlots, coin));
                }
                case 2 -> {}
                case 3 -> {
                    int cost = 0;
                    for(Map.Entry<Integer, String> entries : market.entrySet()) {
                        if(equals(entries.getValue(), Localization.lang.t("market.water"))) {
                            cost = entries.getKey();
                        }
                    }

                    if(coin < cost) {
                        println(Localization.lang.t("market.funds"));
                        return;
                    }

                    coin -= cost;
                    inventory.add(new Water(Localization.lang.t("market.water"), cost));
                    println(Localization.lang.t("market.bought", "market.water", coin));
                }
                case 4 -> isBuying = false;
            }
        } while(coin > 0 || isBuying);
    }

    private void showInventory() {
        if(inventory.isEmpty()) {
            println(Localization.lang.t("game.inventory.empty"));
            return;
        }

        for(Map.Entry<Item, Integer> entry : inventory.getAll().entrySet()) {
            println(entry.getKey().getName() + " x" + entry.getValue());
        }
    }

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

    private int[][] index() {
        List<int[]> positions = new ArrayList<>();
        for(int row = 0; row < SIZE; row++) {
            for(int col = 0; col < SIZE; col++) {
                positions.add(new int[]{row, col});
            }
        }
        return positions.toArray(new int[0][0]);
    }

    private void resize() {
        indices = new int[SIZE * SIZE][2];
    }

    private void redo() {
        if(previousCmd.isEmpty()) { return; }
        Runnable action = commands.get(previousCmd.toLowerCase());
        if(action != null) { action.run(); }
    }

    private String reply(String q) {
        print(q + "$ ");
        return scan.nextLine();
    }

    private int replyNum(String q) {
        print(q + "$ ");
        return scan.nextInt();
    }

    private void print(String str) {
        System.out.print(str);
    }

    private void println(String str) {
        System.out.println(str);
    }

    private void println() {
        System.out.println();
    }

    private boolean equals(String str1, String str2) {
        return str1.equalsIgnoreCase(str2);
    }
}
