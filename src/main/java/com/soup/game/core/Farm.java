package com.soup.game.core;

import com.soup.game.ent.Crop;
import com.soup.game.enums.CropID;

import java.nio.file.Paths;

public class Farm {
    private static final String NAME = "Farm";
    private static final int SIZE = 16;
    private final Crop[][] crops = new Crop[SIZE][SIZE];
    private final String user;
    private final String title;

    public Farm() {
        this.user = Paths.get(System.getProperty("user.home")).getFileName().toString();
        this.title = user + "'s " + NAME;
        System.out.println("Welcome to " + title);
        start();
    }

    private void start() {
        int days = 0;
        final int MAX_DAYS = 24;
        boolean isRunning = true;

        for(int row = 0; row < SIZE; row++) {
            for(int col = 0; col < SIZE; col++) {
                crops[row][col] = new Crop(CropID.random());
            }
        }

        while(isRunning) {
            System.out.println("Day = " + days);

            for(int row = 0; row < SIZE; row++) {
                for(int col = 0; col < SIZE; col++) {
                    Crop crop = crops[row][col];
                    if (crop == null) {
                        System.out.print("[ ] ");
                    } else {
                        System.out.print(crop.canHarvest() ? "[H] " :
                                "[" + crop.getId().getName().charAt(0) + "] ");
                    }
                }
                System.out.println();
            }

            for(int row = 0; row < SIZE; row++) {
                for(int col = 0; col < SIZE; col++) {
                    Crop crop = crops[row][col];
                    if (crop != null) crop.grow();
                }
            }

            for(int row = 0; row < SIZE; row++) {
                for(int col = 0; col < SIZE; col++) {
                    Crop crop = crops[row][col];
                    if (crop != null && crop.canHarvest()) {
                        System.out.println("Harvested " + crop.getId().getName() +
                                " for " + crop.getId().getYield() + " units!");
                        crops[row][col] = null;
                    }
                }
            }

            days++;
            if(days > MAX_DAYS) isRunning = false;
        }
    }

    private void harvest(int x, int y) {
        Crop crop = crops[x][y];
        if(crop.canHarvest()) {
            System.out.println("Harvested " + crop.getId().getName() +
                    " for " + crop.getId().getYield() + " units!");
            crops[x][y] = null;
        }
    }
}
