package com.soup.game.world;

import com.soup.game.ent.Animal;
import com.soup.game.ent.barn.Cow;
import com.soup.game.intf.World;

import java.util.ArrayList;
import java.util.List;

@World(entity = "barn")
public class Barn {
    private final List<Animal> animals;

    public Barn() {
        this.animals = new ArrayList<>();
        populate();
    }

    private void populate() {
        animals.add(new Cow(Animal.name()));
        animals.add(new Cow(Animal.name()));
        animals.add(new Cow(Animal.name()));
        animals.add(new Cow(Animal.name()));
    }

    public void update() {
        animals.forEach(Animal::update);
        animals.removeIf(a -> !a.isAlive());
    }
}
