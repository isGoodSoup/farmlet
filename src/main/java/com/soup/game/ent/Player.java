package com.soup.game.ent;

import com.soup.game.enums.Gamerule;
import com.soup.game.enums.Upgrades;
import com.soup.game.intf.Entity;
import com.soup.game.service.Colors;
import com.soup.game.service.Inventory;
import com.soup.game.service.Localization;
import com.soup.game.swing.SwingPanel;
import com.soup.game.world.QuestLog;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the player in the game, including their level, experience, and inventory.
 * <p>
 * The Player class tracks the player's progression through experience points and levels.
 * It also maintains an Inventory object that holds all items the player has acquired.
 * </p>
 */
@Entity(type = "player")
public class Player {
    private final SwingPanel panel;
    private final String title;
    private final String name;
    private final Inventory inventory;
    private final QuestLog questLog;
    private final List<Upgrades> upgrades;
    private int level;
    private int experience;
    private int nextLevel;
    private int coin;
    private float water = 100f;

    /**
     * Constructs a new Player with default values.
     * Initializes the inventory, sets the level to 1, and experience to 0.
     */
    public Player(SwingPanel panel) {
        this.panel = panel;
        this.name = Paths.get(System.getProperty("user.home"))
                .getFileName().toString();
        title = Localization.lang.t("game.farm.title", name,
                Localization.lang.t("game.farm"));
        this.inventory = new Inventory(panel);
        this.questLog = new QuestLog(panel);
        this.upgrades = new ArrayList<>();
        this.level = 1;
        this.experience = 0;
        this.nextLevel = 16;
        this.coin = 0;
    }

    /**
     * Returns the {@link QuestLog} associated with this player.
     * <p>
     * The {@code QuestLog} contains all quests that the player has accepted,
     * including active and completed quests. This allows tracking quest
     * progress, adding new quests, or displaying them in the game's TUI.
     * </p>
     *
     * @return the {@link QuestLog} of this player
     */
    public QuestLog quests() {
        return questLog;
    }

    /**
     * Updates the current stats of the player, that being either
     * experience, level or next level experience, gold and inventory quantity
     * @param experience is the amount of updated experience
     */
    public void update(int experience) {
        add(experience);
        while(this.experience >= nextLevel) {
            this.experience -= nextLevel;
            levelUp();
            nextLevel = level * 10;

            panel.append(Localization.lang.t("player.levelup", level),
                    Colors.BRIGHT_GREEN);
        }
        panel.append(Localization.lang.t("player.gainxp", experience,
                        nextLevel - this.experience), Colors.GREEN);
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

    public void add(Upgrades e) {
        upgrades.add(e);
    }

    public boolean has(Upgrades e) {
        return upgrades.contains(e);
    }

    public void remove(Upgrades e) {
        upgrades.remove(e);
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
        if(Gamerule.isEnabled(Gamerule.ENABLE_LEVEL_UP)) {
            this.level += 1;
        }
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
        if(Gamerule.isEnabled(Gamerule.ENABLE_EXPERIENCE)) {
            this.experience += experience;
        }
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

    public float can() {
        return water;
    }

    public float water(float i) {
        return water += i;
    }
}