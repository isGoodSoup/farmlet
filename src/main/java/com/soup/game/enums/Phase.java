package com.soup.game.enums;

public enum Phase {
    NEWBORN, TODDLER, TEENAGEHOOD, ADULTHOOD;

    public Phase next() {
        int next = this.ordinal() + 1;
        if(next >= values().length) {
            return this;
        }
        return values()[next];
    }
}
