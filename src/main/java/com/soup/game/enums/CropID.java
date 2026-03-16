package com.soup.game.enums;

import com.soup.game.intf.Data;
import com.soup.game.intf.Item;
import com.soup.game.service.Localization;

import java.util.Arrays;
import java.util.Random;

/**
 * Represents the different types of crops available in the game.
 *
 * <p>Each crop has properties including its growing season, display name,
 * yield, number of days to grow, sell value, and whether it can regrow after harvest.</p>
 *
 * <p>This enum also provides utility methods to get random crops, either from
 * all crops or filtered by a specific season.</p>
 */
@Data
public enum CropID implements Item {
    WHEAT(Seasons.SPRING, "crop.wheat", 8, 5, 4, 2, true),
    CABBAGE(Seasons.AUTUMN, "crop.cabbage", 8, 10, 6, 4, false),
    CORN(Seasons.WINTER, "crop.corn", 16, 15, 6, 6, false),
    CARROT(Seasons.SPRING, "crop.carrot", 8, 20, 10, 8, false),
    POTATO(Seasons.WINTER, "crop.potato", 16, 15, 12, 10, true),
    TOMATO(Seasons.SUMMER, "crop.tomato", 16, 15, 12, 12, true),
    STRAWBERRY(Seasons.SUMMER, "crop.strawberry", 10, 16, 16, 14, true),
    APPLE(Seasons.SPRING, "crop.apple", 32, 20, 16, 16, false),
    GRAPE(Seasons.WINTER, "crop.grape", 16, 25, 24, 18, false),
    PUMPKIN(Seasons.AUTUMN, "crop.pumpkin", 32, 25, 32, 20, true);

    public static final CropID id = CropID.WHEAT;
    private static final Random random = new Random();
    private final Seasons season;
    private final String name;
    private final int yield;
    private final int days;
    private final int value;
    private final int xp;
    private final boolean canRegrow;

    /**
     * Constructor for CropID enum.
     *
     * @param season    the season this crop grows in
     * @param name      localization key for the crop name
     * @param yield     units produced per harvest
     * @param days      number of days required to grow
     * @param value     gold value per unit when sold
     * @param xp        experience added from harvest
     * @param canRegrow true if the crop regrows after harvest, false otherwise
     */
    CropID(Seasons season, String name, int yield, int days, int value,
           int xp, boolean canRegrow) {
        this.season = season;
        this.name = name;
        this.yield = yield;
        this.days = days;
        this.value = value;
        this.xp = xp;
        this.canRegrow = canRegrow;
    }

    /**
     * Returns the localized name of the crop.
     * @return the display name of the crop
     */
    @Override
    public String getName() {
        return Localization.lang.t(name);
    }

    /**
     * Returns the value of the crop when sold.
     * @return the gold value of the crop
     */
    @Override
    public int value() {
        return value;
    }

    /**
     * Returns a random crop that matches the given season.
     * @param currentSeason the season to filter crops by
     * @return a random CropID valid for the season
     */
    public CropID random(Seasons currentSeason) {
        CropID[] seasonalCrops = Arrays.stream(CropID.values())
                .filter(c -> c.getSeason() == currentSeason)
                .toArray(CropID[]::new);

        if (seasonalCrops.length == 0) {
            return CropID.values()[random.nextInt(CropID.values().length)];
        }

        return seasonalCrops[random.nextInt(seasonalCrops.length)];
    }

    /**
     * Returns true if the crop can regrow after being harvested.
     * @return true if regrows, false otherwise
     */
    public boolean regrows() {
        return canRegrow;
    }

    /**
     * Returns the number of units produced per harvest.
     * @return yield per harvest
     */
    public int getYield() {
        return yield;
    }

    /**
     * Returns the experience earned by each crop, varying per crop type
     * @return the xp per harvested crop
     */
    public int getXp() {
        return xp;
    }

    /**
     * Returns the number of days it takes for the crop to fully grow.
     * @return growth duration in days
     */
    public int getDays() {
        return days;
    }

    /**
     * Returns the season in which this crop can be planted and grown.
     * @return the crop's season
     */
    public Seasons getSeason() {
        return season;
    }
}