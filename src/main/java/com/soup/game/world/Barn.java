package com.soup.game.world;

import com.soup.game.ent.Animal;
import com.soup.game.ent.barn.Cow;
import com.soup.game.intf.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a barn containing a collection of animals.
 * <p>
 * The barn can populate itself with animals, update all contained animals
 * every game tick, and remove dead animals automatically.
 */
@World(entity = "barn")
public class Barn {
    private final List<Animal> animals;

    /**
     * Constructs a new barn and populates it with initial animals.
     */
    public Barn() {
        this.animals = new ArrayList<>();
        populate();
    }

    /**
     * Populates the barn with initial animals.
     * <p>
     * This method should instantiate concrete {@link Animal} subclasses
     * and add them to the animals list.
     */
    private void populate() {
        animals.add(new Cow(Animal.name()));
        animals.add(new Cow(Animal.name()));
        animals.add(new Cow(Animal.name()));
        animals.add(new Cow(Animal.name()));
    }

    /**
     * Updates all animals in the barn for a single game tick:
     * <ul>
     *     <li>Calls {@link Animal#update()} for each animal</li>
     *     <li>Removes animals that are no longer alive</li>
     * </ul>
     */
    public void update() {
        animals.forEach(Animal::update);
        animals.removeIf(a -> !a.isAlive());
    }
}
