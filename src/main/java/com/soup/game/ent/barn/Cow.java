package com.soup.game.ent.barn;

import com.soup.game.ent.Animal;
import com.soup.game.enums.Product;
import com.soup.game.intf.Entity;
import com.soup.game.service.Localization;

@Entity(type = "animal")
public class Cow extends Animal {
    public Cow(String name) {
        super(name, Product.MILK, 1.85f, (float) (Math.random() * 100f));
    }

    @Override
    public String getLocalizedName() {
        return Localization.lang.t("animal.cow");
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
        return Product.MILK;
    }

    @Override
    public Animal breed() {
        return new Cow(name());
    }
}
