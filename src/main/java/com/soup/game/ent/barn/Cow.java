package com.soup.game.ent.barn;

import com.soup.game.ent.Animal;
import com.soup.game.enums.AnimalType;
import com.soup.game.enums.Product;
import com.soup.game.enums.Sex;
import com.soup.game.intf.Entity;
import com.soup.game.service.Localization;

@Entity(type = "animal")
public class Cow extends Animal {
    public Cow(String name) {
        super(name, Sex.random(), AnimalType.COW);
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
    public Animal breed(Animal partner) {
       return super.breed(partner);
    }
}
