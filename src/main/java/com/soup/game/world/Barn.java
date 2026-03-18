package com.soup.game.world;

import com.soup.game.ent.Animal;
import com.soup.game.ent.barn.Cow;
import com.soup.game.intf.World;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>Barn</h1>
 * Represents a container and lifecycle manager for {@link Animal} entities
 * within the farm simulation.
 *
 * <p>
 * A {@code Barn} is responsible for maintaining a dynamic collection of animals,
 * initializing starting livestock, and advancing their state on each game tick.
 * It acts as a localized subsystem of the world, delegating behavior to individual
 * animals while enforcing high-level constraints such as automatic cleanup.
 * </p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *     <li>Instantiate and register initial animals via {@link #populate()}</li>
 *     <li>Maintain an internal list of active {@link Animal} instances</li>
 *     <li>Propagate update cycles to all animals</li>
 *     <li>Remove animals that are no longer alive</li>
 * </ul>
 *
 * <h2>Lifecycle Model</h2>
 * <p>
 * On each game tick, {@link #update()} performs a full simulation step:
 * </p>
 * <ol>
 *     <li>Each animal is updated via {@link Animal#update()}</li>
 *     <li>Animals marked as dead are removed from the barn</li>
 * </ol>
 *
 * <h2>Population Strategy</h2>
 * <p>
 * The initial population is defined in {@link #populate()} and may be extended
 * to support different animal types, randomized distributions, or progression-based scaling.
 * </p>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *     <li>The barn does not directly control animal behavior — it delegates to {@link Animal}</li>
 *     <li>Dead animals are removed eagerly each tick (no persistence)</li>
 *     <li>No upper bound on capacity is enforced (may be added later)</li>
 * </ul>
 *
 * @see Animal
 * @see #update()
 * @author isGoodSoup
 * @since 1.8
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
        breeding();
        animals.removeIf(a -> !a.isAlive());
    }

    /**
     * <h2>Breeding System</h2>
     * Performs pairwise breeding between compatible animals in the barn.
     *
     * <p>
     * This method models population-level reproduction by evaluating all unique
     * pairs of animals and attempting to produce offspring based on compatibility
     * and probability. Newborn animals are collected and added to the barn after
     * iteration to avoid concurrent modification.
     * </p>
     *
     * <h3>Execution Model</h3>
     * <ul>
     *     <li>Iterates over all unique animal pairs ({@code i < j})</li>
     *     <li>Checks compatibility via {@link Animal#canBreedWith(Animal)}</li>
     *     <li>Applies a probabilistic breeding chance (~5%)</li>
     *     <li>Delegates offspring creation to {@link Animal#breed(Animal)}</li>
     *     <li>Registers offspring in both parents' lineage</li>
     *     <li>Adds all newborns to the barn after iteration</li>
     * </ul>
     *
     * <h3>Breeding Rules</h3>
     * <ul>
     *     <li>Animals must be of the same type</li>
     *     <li>Animals must be of opposite {@link com.soup.game.enums.Sex}</li>
     *     <li>Both animals must be alive</li>
     * </ul>
     *
     * <h3>Design Notes</h3>
     * <ul>
     *     <li>Breeding is handled externally from {@link Animal} to maintain separation of concerns</li>
     *     <li>Pairwise iteration is O(n²) and may require optimization for large populations</li>
     *     <li>Newborns are buffered to prevent modification during iteration</li>
     *     <li>Lineage tracking is optional but useful for genealogy or gameplay features</li>
     * </ul>
     *
     * @see Animal#canBreedWith(Animal)
     * @see Animal#breed(Animal)
     */
    private void breeding() {
        List<Animal> newborns = new ArrayList<>();
        for(int i = 0; i < animals.size(); i++) {
            for(int j = i + 1; j < animals.size(); j++) {
                Animal a = animals.get(i);
                Animal b = animals.get(j);
                if(!a.canBreedWith(b)) { continue; }
                if(Math.random() > 0.95) {
                    Animal child = a.breed(b);
                    if(child != null) {
                        newborns.add(child);
                        a.getChildren().add(child);
                        b.getChildren().add(child);
                    }
                }
            }
        }
        animals.addAll(newborns);
    }
}
