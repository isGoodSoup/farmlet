package com.soup.game.ent.barn;

import com.soup.game.ent.Animal;
import com.soup.game.enums.Product;
import com.soup.game.intf.Entity;
import com.soup.game.service.Localization;

@Entity(type = "animal")
public class Horse extends Animal {
    public Horse(String name) {
        super(name, Product.NONE, 2.0f, (float) (Math.random() * 100f));
    }

    @Override
    public String getLocalizedName() {
        return Localization.lang.t("animal.horse");
    }

    @Override
    public void feed() {
        happy((int) (Math.random() * 10f));
    }

    @Override
    public void sleep() {
        hungry((int) (Math.random() * 10f));
    }

    @Override
    public Product produce() {
        return Product.NONE;
    }

    @Override
    public Animal breed() {
        return new Horse(name());
    }
}
