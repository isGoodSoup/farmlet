package com.soup.game.ent;

import com.soup.game.intf.Entity;
import com.soup.game.service.Console;
import com.soup.game.service.Inventory;
import com.soup.game.service.Localization;

import java.nio.file.Paths;

/**
 * Represents the player in the game, including their level, experience, and inventory.
 * <p>
 * The Player class tracks the player's progression through experience points and levels.
 * It also maintains an Inventory object that holds all items the player has acquired.
 * </p>
 */
@Entity(type = "player")
public class Player {
    private final String title;
    private final String name;
    private final Inventory inventory;
    private int level;
    private int experience;
    private int nextLevel;
    private int coin;

    /**
     * Constructs a new Player with default values.
     * Initializes the inventory, sets the level to 1, and experience to 0.
     */
    public Player() {
        this.name = Paths.get(System.getProperty("user.home"))
                .getFileName().toString();
        title = Localization.lang.t("game.farm.title", name,
                Localization.lang.t("game.farm"));
        this.inventory = new Inventory();
        this.level = 1;
        this.experience = 0;
        this.nextLevel = 0;
        this.coin = 0;
    }

    /**
     * Updates the current stats of the player, that being either
     * experience, level or next level experience, gold and inventory quantity
     * @param experience is the amount of updated experience
     */
    public void update(int experience) {
        if(this.experience > nextLevel) {
            levelUp();
            Console.cli.println(Localization.lang.t("player.levelup", level));
        }
        Console.cli.println(Localization.lang.t("player.gainxp", experience));
        add(experience);
    }

    /**
     * Returns a String "name" based on the PC's home username
     * @return the player
     */
    public String name() {
        return name;
    }

    /**
     * Returns the built-in title customized with the player's name
     * @return title of the farm
     */
    public String title() {
        return title;
    }

    /**
     * Returns the player's inventory.
     * @return the Inventory object associated with the player
     */
    public Inventory inventory() {
        return inventory;
    }

    /**
     * Returns the player's current level.
     * @return the level of the player
     */
    public int level() {
        return level;
    }

    /**
     * Increases the player's level by one.
     * <p>
     * Typically called when the player's experience reaches or exceeds
     * the threshold for the next level.
     * </p>
     */
    public void levelUp() {
        this.level += 1;
    }

    /**
     * Returns the player's current experience points.
     * @return the total experience of the player
     */
    public int experience() {
        return experience;
    }

    /**
     * Adds a specified amount of experience points to the player.
     * @param experience the number of experience points to add
     */
    public void add(int experience) {
        this.experience += experience;
    }

    /**
     * Calculates and returns the experience required for the next level.
     * @return the experience points needed to reach the next level
     */
    public int levelNext() {
        return nextLevel = (level * experience) / 2;
    }

    /**
     * Adds the specified amount of coins to the player's total.
     * <p>This method increases the current coin balance by the
     * provided amount.</p>
     * @param coin the number of coins to add
     */
    public void earn(int coin) {
        this.coin += coin;
    }

    /**
     * Removes the specified amount of coins from the player's total.
     * <p>This method decreases the current coin balance by the
     * provided amount. If the value is greater than the current
     * balance, the resulting coin total may become negative.</p>
     * @param coin the number of coins to subtract
     */
    public void take(int coin) {
        this.coin -= coin;
    }

    /**
     * Returns the current number of coins the player possesses.
     * <p>This represents the player's available currency that can be
     * used for purchases, upgrades, or other in-game transactions.</p>
     * @return the current coin balance
     */
    public int purse() {
        return coin;
    }

}