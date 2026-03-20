package com.soup.game.service;

import com.soup.game.ent.Player;
import com.soup.game.intf.Item;
import com.soup.game.swing.SwingPanel;

import java.util.*;

/**
 * Represents a player's inventory in the farm game.
 *
 * <p>This class manages items, tracks quantities, allows adding
 * and removing items, and provides access to individual items
 * or a summary of all items in the inventory.</p>
 */
public class Inventory {
    private final SwingPanel panel;
    private final Map<Item, List<Item>> items = new LinkedHashMap<>();

    public Inventory(SwingPanel panel) {
        this.panel = panel;
    }

    /**
     * Adds a single item to the inventory.
     * <p>If the item type already exists, it adds it to the existing list.</p>
     * @param item the {@link Item} to add
     */
    public void add(Item item) {
        items.computeIfAbsent(item, k -> new ArrayList<>()).add(item);
    }

    /**
     * Returns the item at the given index across the inventory.
     * <p>The inventory is treated as a flattened list of items.
     * Throws an {@link IndexOutOfBoundsException} if the index is
     * negative or exceeds the total item count.</p>
     * @param i the index of the item
     * @return the {@link Item} at the given index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Item get(int i) {
        if(i < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative");
        }

        int count = 0;
        for(Map.Entry<Item, List<Item>> entry : items.entrySet()) {
            List<Item> list = entry.getValue();
            if(i < count + list.size()) {
                return list.get(i - count);
            }
            count += list.size();
        }
        throw new IndexOutOfBoundsException("Index out of inventory bounds");
    }

    /**
     * Adds all items from a collection to the inventory.
     * @param newItems collection of {@link Item}s to add
     */
    public void addAll(Collection<Item> newItems) {
        for (Item item : newItems) add(item);
    }

    /**
     * Removes a single instance of the given item from the inventory.
     * <p>If the last instance is removed, the item type is removed entirely.</p>
     * @param item the {@link Item} to remove
     * @return true if an item was removed, false otherwise
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean remove(Item item) {
        List<Item> list = items.get(item);
        if(list == null || list.isEmpty()) { return false; }
        list.removeFirst();
        if(list.isEmpty()) { items.remove(item); }
        return true;
    }

    /**
     * Returns the quantity of a specific item in the inventory.
     * @param item the {@link Item} to count
     * @return number of instances of this item
     */
    public int getQuantity(Item item) {
        List<Item> list = items.get(item);
        return list == null ? 0 : list.size();
    }

    /**
     * Returns a map of all items with their quantities.
     * @return a {@link Map} mapping each {@link Item} to its quantity
     */
    public Map<Item, Integer> getAll() {
        Map<Item, Integer> result = new LinkedHashMap<>();
        for(Map.Entry<Item, List<Item>> e : items.entrySet()) {
            result.put(e.getKey(), e.getValue().size());
        }
        return result;
    }

    /**
     * Checks whether the inventory is empty.
     * @return true if there are no items, false otherwise
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns the total number of items in the inventory,
     * counting all instances of each item type.
     * @return total item count
     */
    public int size() {
        int total = 0;
        for(List<Item> list : items.values()) {
            total += list.size();
        }
        return total;
    }

    /**
     * Shows all items and quantities in the player's inventory.
     */
    public void showInventory(Player player) {
        if(player.inventory().isEmpty()) {
            panel.append(Localization.lang.t("game.inventory.empty"),
                    Colors.BRIGHT_RED);
            return;
        }

        for(Map.Entry<Item, Integer> entry : player.inventory().getAll().entrySet()) {
            panel.append(entry.getKey().getName() + " x" + entry.getValue(),
                    Colors.PURPLE);
        }
    }
}