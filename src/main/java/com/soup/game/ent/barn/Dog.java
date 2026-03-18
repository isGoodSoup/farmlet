package com.soup.game.ent.barn;

import com.soup.game.ent.Animal;
import com.soup.game.enums.AnimalType;
import com.soup.game.enums.Product;
import com.soup.game.enums.Sex;
import com.soup.game.intf.Entity;
import com.soup.game.service.Localization;

@Entity(type = "animal")
public class Dog extends Animal {

    public Dog(String name) {
        super(name, Sex.random(), AnimalType.DOG);
    }

    @Override
    public String getLocalizedName() {
        return Localization.lang.t("animal.dog");
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
    public Animal breed(Animal partner) {
        if(!canBreedWith(partner)) { return null; }
        Dog mate = (Dog) partner;
        Enum<?> childTrait = Math.random() < 0.5 ? getTrait() : mate.getTrait();
        Dog child = new Dog(Animal.name());
        child.setTrait(childTrait);
        return child;
    }
}
