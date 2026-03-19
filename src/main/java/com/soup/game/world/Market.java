package com.soup.game.world;

import com.soup.game.ent.Player;
import com.soup.game.enums.CropID;
import com.soup.game.enums.Fertilizer;
import com.soup.game.enums.Gamerule;
import com.soup.game.enums.Upgrades;
import com.soup.game.intf.Item;
import com.soup.game.intf.World;
import com.soup.game.service.Console;
import com.soup.game.service.Localization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>Market</h1>
 * Represents the in-game market where the player can sell crops and purchase resources, upgrades, or farm expansions.
 * <p>
 * The Market interacts directly with the {@link Player} inventory and purse. It handles sales and purchases, applies upgrades,
 * and expands the farm grid when requested.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *     <li>Sell all crops in inventory for coins.</li>
 *     <li>Purchase water, fertilizer, upgrades, and farm plot expansions.</li>
 *     <li>Validates funds and input before purchases.</li>
 *     <li>Immediately applies effects to the player and farm.</li>
 * </ul>
 *
 * <p>
 * This class depends on {@link Gamerule} settings to determine if the market is accessible.
 * Prices and items are managed internally via a {@link LinkedHashMap}.
 * </p>
 */
@World
public class Market {
    private final Map<Integer, String> market;
    private final Player player;

    /**
     * Constructs a new Market instance for the specified player.
     * @param player the player who will interact with this market
     */
    public Market(Player player) {
        this.market = new LinkedHashMap<>();
        this.player = player;
    }

    /**
     * Sells all crops in the player's inventory.
     * <p>
     * Each crop's value (from {@link CropID}) is multiplied by the quantity owned
     * to calculate the total coins earned. The coins are added to the player's purse,
     * and the sold crops are removed from inventory.
     * </p>
     * <p>
     * A message is printed to the console showing the total coins earned.
     * </p>
     */
    public void sellCrops() {
        int totalCoin = 0;

        for(Map.Entry<Item, Integer> entry : new LinkedHashMap<>(player.inventory()
                .getAll()).entrySet()) {
            Item item = entry.getKey();
            if(item instanceof CropID c) {
                int quantity = entry.getValue();
                totalCoin += c.value() * quantity;
                for(int i = 0; i < quantity; i++) {
                    player.inventory().remove(c);
                }
            }
        }
        player.earn(totalCoin);
        Console.cli.println(Localization.lang.t("game.sold", totalCoin), Console.YELLOW);
    }

    /**
     * Opens the in-game market interface, allowing the player to purchase resources,
     * upgrades, and farm expansions.
     * <p>
     * The market presents a numbered list of items and their prices. The player selects an item by number, and
     * if sufficient funds are available, the purchase is applied immediately.
     * </p>
     *
     * <p><b>Available Purchases:</b></p>
     * <ul>
     *     <li><b>Water</b> – increases the player's water supply by 1 unit.</li>
     *     <li><b>Fertilizer</b> – adds 16 units each of {@link Fertilizer#SPEED} and {@link Fertilizer#YIELD} to inventory.</li>
     *     <li><b>For-loop Upgrade</b> – unlocks the {@link Upgrades#FOR_LOOP} command.</li>
     *     <li><b>Plot Expansion</b> – increases the farm size by 2 tiles and initializes new tiles via {@link Farm#populate(int)}.</li>
     *     <li><b>Upgrades Bundle</b> – unlocks {@link Upgrades#HARVEST_UPGRADE} and {@link Upgrades#PLANT_UPGRADE}.</li>
     * </ul>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *     <li>Validates that the player has enough coins for each purchase.</li>
     *     <li>Deducts coins immediately upon purchase.</li>
     *     <li>Applies upgrades, inventory changes, or farm expansion instantly.</li>
     *     <li>Loops until the player has no coins left or chooses to exit.</li>
     * </ul>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *     <li>The market is only accessible if {@link Gamerule#ENABLE_MARKET} is enabled.</li>
     *     <li>Prices and items are rebuilt each time {@code buy()} is called.</li>
     *     <li>Fertilizer is added in bulk to the inventory, not applied directly to tiles.</li>
     * </ul>
     *
     * @param farm the {@link Farm} instance that may be expanded when purchasing plots
     */
    public void buy(Farm farm) {
        if(!Gamerule.isEnabled(Gamerule.ENABLE_MARKET)) { return; }
        market.clear();
        market.put(100, Localization.lang.t("market.water"));
        market.put(200, Localization.lang.t("market.fertilizer"));
        market.put(500, Localization.lang.t("market.for"));
        market.put(8_192, Localization.lang.t("market.plot"));
        market.put(12_288, Localization.lang.t("market.upgrades"));
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
                Console.cli.println(price + " gold" + spaces + name, Console.PURPLE);
            }

            while(r > market.size()) {
                r = Console.cli.replyNum(Localization.lang.t("market.query") + " ");
            }

            List<Map.Entry<Integer, String>> items = new ArrayList<>(market.entrySet());
            Map.Entry<Integer, String> selected = items.get(r - 1);

            int cost = selected.getKey();
            String item = selected.getValue();

            switch(r) {
                case 1 -> {
                    if(player.purse() < cost) {
                        Console.cli.error(Localization.lang.t("market.funds"));
                        return;
                    }

                    player.take(cost);
                    player.water(1f);
                    Console.cli.println(Localization.lang.t("market.bought",
                            item, player.purse()), Console.BRIGHT_GREEN);
                }
                case 2 -> {
                    if(player.purse() < cost) {
                        Console.cli.error(Localization.lang.t("market.funds"));
                        return;
                    }

                    player.take(cost);
                    for(int i = 0; i < 16; i++) {
                        player.inventory().add(Fertilizer.SPEED);
                        player.inventory().add(Fertilizer.YIELD);
                    }
                    Console.cli.println(Localization.lang.t("market.bought",
                            item, player.purse()), Console.BRIGHT_GREEN);
                }
                case 3 -> {
                    if(player.purse() < cost) {
                        Console.cli.error(Localization.lang.t("market.funds"));
                        return;
                    }

                    player.take(cost);
                    player.add(Upgrades.FOR_LOOP);
                    Console.cli.println(Localization.lang.t("market.bought",
                            item, player.purse()), Console.BRIGHT_GREEN);
                }
                case 4 -> {
                    int increase = 2;
                    if(player.purse() < cost) {
                        Console.cli.error(Localization.lang.t("game.plot.fail"));
                        return;
                    }

                    int oldSize = farm.tiles().size();
                    player.take(cost);
                    farm.populate(farm.tiles().size() + increase);
                    Console.cli.println(Localization.lang.t("market.bought.plot",
                            farm.tiles().size(), player.purse()), Console.BRIGHT_GREEN);
                }
                case 5 -> {
                    player.take(cost);
                    player.add(Upgrades.HARVEST_UPGRADE);
                    player.add(Upgrades.PLANT_UPGRADE);
                    Console.cli.println(Localization.lang.t("market.bought",
                            item, player.purse()), Console.BRIGHT_GREEN);
                }
                default -> isBuying = false;
            }
        } while(player.purse() > 0 || isBuying);
    }
}
