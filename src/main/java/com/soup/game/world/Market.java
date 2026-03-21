package com.soup.game.world;

import com.soup.game.core.GameLoop;
import com.soup.game.ent.Player;
import com.soup.game.enums.CropID;
import com.soup.game.enums.Fertilizer;
import com.soup.game.enums.Gamerule;
import com.soup.game.enums.Upgrades;
import com.soup.game.intf.Item;
import com.soup.game.intf.World;
import com.soup.game.service.Colors;
import com.soup.game.service.Localization;
import com.soup.game.swing.SwingPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
    private static final Logger log = LoggerFactory.getLogger(Market.class);
    private final SwingPanel panel;
    private final GameLoop gameLoop;
    private final Map<Integer, String> market;
    private final Player player;

    /**
     * Constructs a new Market instance for the specified player.
     *
     * @param panel  render layer
     * @param player the player who will interact with this market
     */
    public Market(SwingPanel panel, GameLoop gameLoop, Player player) {
        this.panel = panel;
        this.gameLoop = gameLoop;
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
        log.info("Selling all crops for {} gold", totalCoin);
        panel.append(Localization.lang.t("game.sold", totalCoin), Colors.YELLOW);
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
    public void buy(Farm farm, Consumer<Integer> onComplete) {
        if(!Gamerule.isEnabled(Gamerule.ENABLE_MARKET)) {
            return;
        }
        market.clear();
        market.put(100, Localization.lang.t("market.water"));
        market.put(200, Localization.lang.t("market.fertilizer"));
        market.put(500, Localization.lang.t("market.for"));
        market.put(8192, Localization.lang.t("market.plot"));
        market.put(12288, Localization.lang.t("market.upgrades"));

        int maxPriceWidth = market.keySet().stream()
                .map(k -> k.toString().length())
                .max(Integer::compare).orElse(0);

        List<Map.Entry<Integer, String>> items = new ArrayList<>(market.entrySet());
        for(int i = 0; i < items.size(); i++) {
            Map.Entry<Integer, String> entry = items.get(i);
            String spaces = " ".repeat(maxPriceWidth - entry.getKey().toString().length() + 2);
            panel.append((i + 1) + ". " + entry.getKey() + " gold" + spaces + entry.getValue(), Colors.MAGENTA);
        }

        panel.setCommandListener(line -> {
            if(line.equalsIgnoreCase("exit")) {
                panel.setCommandListener(gameLoop);
                return;
            }
            try {
                int choice = Integer.parseInt(line.trim());
                if(choice < 1 || choice > items.size()) {
                    panel.append(Localization.lang.t("game.error.selection"), Colors.BRIGHT_RED);
                    return;
                }
                purchase(farm, choice);
                if(onComplete != null) {
                    onComplete.accept(choice);
                }
            } catch (NumberFormatException e) {
                panel.append(Localization.lang.t("game.error.number"), Colors.BRIGHT_RED);
            }
        });
    }

    private void purchase(Farm farm, int r) {
        Map.Entry<Integer, String> selected = new ArrayList<>(market.entrySet()).get(r - 1);
        int cost = selected.getKey();
        String item = selected.getValue();

        if(player.purse() < cost) {
            panel.append(Localization.lang.t("market.funds"), Colors.BRIGHT_RED);
            return;
        }

        switch (r) {
            case 1 -> {
                player.take(cost);
                player.water(1f);
                panel.append(Localization.lang.t("market.bought", item, player.purse()), Colors.BRIGHT_GREEN);
            }
            case 2 -> {
                player.take(cost);
                for (int i = 0; i < 16; i++) {
                    player.inventory().add(Fertilizer.SPEED);
                    player.inventory().add(Fertilizer.YIELD);
                }
                panel.append(Localization.lang.t("market.bought", item, player.purse()), Colors.BRIGHT_GREEN);
            }
            case 3 -> {
                player.take(cost);
                player.add(Upgrades.FOR_LOOP);
                panel.append(Localization.lang.t("market.bought", item, player.purse()), Colors.BRIGHT_GREEN);
            }
            case 4 -> {
                player.take(cost);
                farm.populate(farm.tiles().size() + 2);
                panel.append(Localization.lang.t("market.bought.plot", farm.tiles().size(), player.purse()), Colors.BRIGHT_GREEN);
            }
            case 5 -> {
                player.take(cost);
                player.add(Upgrades.HARVEST_UPGRADE);
                player.add(Upgrades.PLANT_UPGRADE);
                panel.append(Localization.lang.t("market.bought", item, player.purse()), Colors.BRIGHT_GREEN);
            }
        }
        log.debug("Bought= {}, {}", item,cost);
    }
}
