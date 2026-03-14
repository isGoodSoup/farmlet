package com.soup.game.enums;

public enum GrowthStage {
    SEED, SPROUT, GROWING, MATURE, HARVESTABLE;

    public GrowthStage next() {
        int next = this.ordinal() + 1;
        if (next >= values().length) {
            return this;
        }
        return values()[next];
    }
}