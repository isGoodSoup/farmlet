package com.soup.game.intf;

/**
 * Represents a generic item in the game, such as crops,
 * tools, or other inventory objects.
 *
 * <p>Every item must provide a value and a display name.</p>
 */
public interface Item {

    /**
     * Returns the value of the item, typically used
     * for selling or trading purposes.
     * @return the item value in gold
     */
    int value();

    /**
     * Returns the display name of the item, localized
     * if necessary.
     * @return the item's name
     */
    String getName();

    static Item random() {
        return this;
    }
}