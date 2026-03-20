package com.soup.game.ent.barn;

import com.soup.game.ent.Animal;
import com.soup.game.enums.AnimalType;
import com.soup.game.enums.Product;
import com.soup.game.enums.Sex;
import com.soup.game.intf.Entity;
import com.soup.game.service.Localization;

@Entity(type = "animal")
public class Chicken extends Animal {

    public Chicken(String name) {
        super(name, Sex.random(), AnimalType.CHICKEN);
    }

    @Override
    public String getLocalizedName() {
        return Localization.lang.t("animal.chicken");
    }

    @Override
    public void feed() {
        super.feed();
        happy((int) (Math.random() * 10f));
    }

    @Override
    public void sleep() {
        hungry((int) (Math.random() * 10f));
    }

    @Override
    public Product produce() {
        return Product.EGGS;
    }

    @Override
    public Animal breed(Animal partner) {
        return super.breed(partner);
    }
}
