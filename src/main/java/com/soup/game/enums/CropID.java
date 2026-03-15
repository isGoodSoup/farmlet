package com.soup.game.enums;

import com.soup.game.intf.Data;
import com.soup.game.intf.Item;
import com.soup.game.service.Localization;

import java.util.Random;

@Data
public enum CropID implements Item {
    WHEAT("crop.wheat", 8, 5, 4, true),
    CABBAGE("crop.cabbage", 8, 10, 6, false),
    CORN("crop.corn", 16, 15, 6, false),
    CARROT("crop.carrot", 8, 20, 10, false),
    POTATO("crop.potato", 16, 15, 12, true),
    TOMATO("crop.tomato", 16, 15, 12, true),
    STRAWBERRY("crop.strawberry", 10, 16, 16, true),
    APPLE("crop.apple", 32, 20, 16, false),
    GRAPE("crop.grape", 16, 25, 24, false),
    PUMPKIN("crop.pumpkin", 32, 25, 32, true);

    private static final Random random = new Random();
    private final String name;
    private final int yield;
    private final int days;
    private final int value;
    private final boolean canRegrow;

    CropID(String name, int yield, int days, int value, boolean canRegrow) {
        this.name = name;
        this.yield = yield;
        this.days = days;
        this.value = value;
        this.canRegrow = canRegrow;
    }

    @Override
    public String getName() {
        return Localization.lang.t(name);
    }

    @Override
    public int value() {
        return value;
    }

    public static CropID random() {
        return CropID.values()[random.nextInt(0, CropID.values().length - 1)];
    }
    public boolean regrows() {
        return canRegrow;
    }
    public int getYield() {
        return yield;
    }
    public int getDays() {
        return days;
    }
}