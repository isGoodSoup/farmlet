package com.soup.game.enums;

import com.soup.game.intf.Item;
import com.soup.game.service.Localization;

public enum Product implements Item {
    MILK("animals.milk", 2),
    EGGS("animals.eggs", 3),
    WOOL("animals.wool", 4),
    MEAT("animals.meat", 10),
    NONE("animals.none", 0);

    private final String name;
    private final int value;

    Product(String name, int value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public String getName() {
        return Localization.lang.t(name);
    }
}
