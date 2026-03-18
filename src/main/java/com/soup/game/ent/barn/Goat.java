package com.soup.game.ent.barn;

import com.soup.game.ent.Animal;
import com.soup.game.enums.Product;
import com.soup.game.enums.Sex;
import com.soup.game.intf.Entity;
import com.soup.game.service.Localization;

@Entity(type = "animal")
public class Goat extends Animal {
    public Goat(String name) {
        super(name, Sex.random(), Product.MILK, 1.5f, (float) (Math.random() * 10f));
    }

    @Override
    public String getLocalizedName() {
        return Localization.lang.t("animal.goat");
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
    public Animal breed(Animal partner) {
        if(!canBreedWith(partner)) { return null; }
        return new Goat(name());
    }
}
