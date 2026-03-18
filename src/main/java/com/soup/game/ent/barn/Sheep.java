package com.soup.game.ent.barn;

import com.soup.game.ent.Animal;
import com.soup.game.enums.AnimalType;
import com.soup.game.enums.Product;
import com.soup.game.enums.Sex;
import com.soup.game.intf.Entity;
import com.soup.game.service.Localization;

@Entity(type = "animal")
public class Sheep extends Animal {
    public Sheep(String name) {
        super(name, Sex.random(), AnimalType.SHEEP);
    }

    @Override
    public String getLocalizedName() {
        return Localization.lang.t("animal.sheep");
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
        return Product.WOOL;
    }

    @Override
    public Animal breed(Animal partner) {
        return super.breed(partner);
    }
}
