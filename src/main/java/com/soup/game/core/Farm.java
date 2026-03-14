package com.soup.game.core;

import com.soup.game.ent.Crop;
import com.soup.game.enums.CropID;
import com.soup.game.enums.GrowthStage;
import com.soup.game.enums.Weather;
import com.soup.game.service.Localization;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class Farm {
    private final static int MAX_SIZE = 1024;
    private final Crop[][] crops;
    private final int[][] indices;
    private final Map<CropID, Integer> harvest;
    private final Map<String, Runnable> commands;
    private final String user;
    private final String day;
    private final String title;
    private final Scanner scan;

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

        this.harvest = new LinkedHashMap<>();
        this.commands = new LinkedHashMap<>();
        populate();

        this.scan = new Scanner(System.in);
        this.day = Localization.lang.t("game.day");
        this.weather = Weather.SUNNY;
        start();
    }

    private void start() {
        days = 0; coin = 0;
        plant();
        loop();
        showStats();
    }

    private void loop() {
        do {
            println(day + " " + days);
            weather();
            grow();
            update();
            if(!equals(cmd, "skip")) { harvest(); }
            do {
                cmd = run();
            } while (!equals(cmd, "skip") && !equals(cmd, "end"));
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

    private void populate() {
        commands.put(".", this::redo);
        commands.put("harvest", this::harvest);
        commands.put("replant", this::plant);
        commands.put("show", this::update);
        commands.put("inv", this::showInventory);
        commands.put("sell", this::sellCrops);
        commands.put("buy", this::buyPlot);
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
                    dryDay = 0;
                    continue;
                }
                if(crop == null) {
                    print("[ ] ");
                } else {
                    print(crop.canHarvest() ? "[H] " :
                            "[" + crop.getStage().name().charAt(0) + "] ");
                }
            }
            println();
        }

        dryDay = 0;
    }

    private void grow() {
        if(!equals(weather, Weather.DRY)) {
            for(int[] pos : index()) {
                Crop crop = crops[pos[0]][pos[1]];
                if(crop != null) crop.grow();
            }
        }
    }

    private void harvest() {
        Map<CropID, Integer> todayHarvest = new LinkedHashMap<>();

        for(int[] pos : index()) {
            Crop crop = crops[pos[0]][pos[1]];
            if(crop != null && crop.canHarvest()) {
                todayHarvest.merge(crop.getId(), crop.getId().getYield(), Integer::sum);
                harvest.merge(crop.getId(), crop.getId().getYield(), Integer::sum);
                if(crop.getId().regrows()) {
                    crop.setStage(GrowthStage.SEED);
                } else {
                    crops[pos[0]][pos[1]] = null;
                }
            }
        }

        if(!todayHarvest.isEmpty()) {
            for(Map.Entry<CropID, Integer> entry : todayHarvest.entrySet()) {
                println(Localization.lang.t("game.yields",
                        entry.getKey().getName(), entry.getValue()));
            }
        }
    }

    private void weather() {
        weather = Weather.getRandomWeather();
        if(equals(weather, Weather.DRY)) {
            dryDay++;
        }
        println(weather.message());
    }

    private void plant() {
        for(int[] pos : index()) {
            crops[pos[0]][pos[1]] = new Crop(CropID.random());
        }
    }

    private void sleep() {
        println(Localization.lang.t("game.sleep"));
        harvest();
        days++;
        cmd = "skip";
    }

    private void sellCrops() {
        for(Map.Entry<CropID, Integer> entries : harvest.entrySet()) {
            coin += entries.getKey().getValue() * entries.getValue();
        }
        harvest.clear();
        println(Localization.lang.t("game.sold", coin));
    }

    private void buyPlot() {
        int plotCost = 131_072;
        int increase = 2;

        if(coin < plotCost) {
            return;
        }

        if(SIZE + increase > MAX_SIZE) {
            println(Localization.lang.t("game.plot.size"));
            return;
        }

        int oldSize = SIZE;
        SIZE += increase;
        coin -= plotCost;
        int newPlots = SIZE * SIZE - oldSize * oldSize;
        println(Localization.lang.t("game.plot", coin, newPlots));
    }

    private void showInventory() {
        for(Map.Entry<CropID, Integer> entries : harvest.entrySet()) {
            println(entries.getKey().getName() + " x" + entries.getValue());
        }
    }

    private void showStats() {
        println(Localization.lang.t("game.stats"));
        int totalCrops = 0;
        for(Map.Entry<CropID, Integer> entries : harvest.entrySet()) {
            totalCrops += entries.getValue();
        }
        println(Localization.lang.t("game.stats.crops", totalCrops));
        println(Localization.lang.t("game.stats.days",days));
    }

    private int[][] index() {
        int k = 0;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                indices[k][0] = row;
                indices[k][1] = col;
                k++;
            }
        }
        return indices;
    }

    private void redo() {
        Runnable action = commands.get(previousCmd.toLowerCase());
        if(action != null) { action.run(); }
    }

    private String reply(String q) {
        print(q + "$ ");
        return scan.nextLine();
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

    private boolean equals(Object str1, Object str2) {
        return str1.equals(str2);
    }

    private boolean equals(String str1, String str2) {
        return str1.equalsIgnoreCase(str2);
    }
}
