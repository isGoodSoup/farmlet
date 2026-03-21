package com.soup.game.world;

import com.soup.game.ent.Animal;
import com.soup.game.ent.Player;
import com.soup.game.ent.barn.*;
import com.soup.game.enums.AnimalType;
import com.soup.game.enums.Gamerule;
import com.soup.game.intf.World;
import com.soup.game.service.Colors;
import com.soup.game.service.Localization;
import com.soup.game.swing.SwingPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
 *     <li>Each animal is updated via {@link Animal#update(Player)}</li>
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
 *     <li>No upper bound on capacity is enforced (maybe added later)</li>
 * </ul>
 *
 * @see Animal
 * @see #update()
 * @author isGoodSoup
 * @since 1.8
 */
@World(entity = "barn")
public class Barn {
    private static final Logger log = LoggerFactory.getLogger(Barn.class);
    private final SwingPanel panel;
    private final List<Animal> animals;
    private final Player player;

    /**
     * Constructs a new barn and populates it with an initial set of animals.
     *
     * <p>
     * The barn maintains a list of all animals currently present. Upon creation,
     * this constructor initializes the animal list and calls {@link #populate()}
     * to generate a randomized population.
     * </p>
     *
     * <h3>Population Rules</h3>
     * <ul>
     *     <li>At least one dog is guaranteed to exist in the barn</li>
     *     <li>The total number of animals is randomly chosen between 4 and 15</li>
     *     <li>Additional animals are randomly selected from all available {@link AnimalType} values</li>
     * </ul>
     *
     * <h3>Design Notes</h3>
     * <ul>
     *     <li>Population is randomized each time a barn is created to enhance replayability</li>
     *     <li>Animals are shuffled after creation to avoid predictable ordering</li>
     *     <li>Subclasses of {@link Animal} are instantiated according to their type</li>
     * </ul>
     *
     * @param panel render layer
     * @param player player who will save items to inventory
     * @see #populate()
     */
    public Barn(SwingPanel panel, Player player) {
        this.panel = panel;
        this.animals = new ArrayList<>();
        this.player = player;
        populate();
    }

    /**
     * Populates the barn with a randomized selection of animals.
     *
     * <p>
     * This method handles instantiation of concrete {@link Animal} subclasses
     * according to the available {@link AnimalType} values. It guarantees that
     * at least one dog is present and fills the rest of the barn with a random
     * number and variety of animals.
     * </p>
     *
     * <h3>Execution Details</h3>
     * <ul>
     *     <li>Adds at least one {@link Dog} to the barn</li>
     *     <li>Randomly selects additional animals until a total between 4 and 15 is reached</li>
     *     <li>Shuffles the animal list to randomize order</li>
     *     <li>Instantiates each animal subclass based on its {@link AnimalType}</li>
     * </ul>
     *
     * <h3>Design Notes</h3>
     * <ul>
     *     <li>Uses {@link java.util.Random} to control population size and selection</li>
     *     <li>Supports future extension by adding new {@link AnimalType} entries</li>
     * </ul>
     *
     * @see Animal
     * @see AnimalType
     */
    private void populate() {
        log.info("Populating barn with:");
        List<AnimalType> types = new ArrayList<>(Arrays.asList(AnimalType.values()));
        animals.add(new Dog(Animal.name()));
        animals.add(new Dog(Animal.name()));

        Random random = new Random();
        while(animals.size() < random.nextInt(4, 15)) {
            AnimalType type = types.get(random.nextInt(types.size()));
            switch(type) {
                case COW -> animals.add(new Cow(Animal.name()));
                case CHICKEN -> animals.add(new Chicken(Animal.name()));
                case DUCK -> animals.add(new Duck(Animal.name()));
                case GOAT -> animals.add(new Goat(Animal.name()));
                case HORSE -> animals.add(new Horse(Animal.name()));
                case PIG -> animals.add(new Pig(Animal.name()));
                case SHEEP -> animals.add(new Sheep(Animal.name()));
            }
        }
        Collections.shuffle(animals);

        for(Animal a : animals) {
            log.debug("{} the {}", a.getName(), a.getLocalizedName());
        }
    }

    /**
     * Updates all animals in the barn for a single game tick:
     * <ul>
     *     <li>Calls {@link Animal#update(Player)} for each animal</li>
     *     <li>Passed Player saves to inventory any product the animals produce</li>
     *     <li>Removes animals that are no longer alive</li>
     * </ul>
     */
    public void update() {
        for(Animal a : animals) {
            a.update(player);
        }
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
        if(Gamerule.isEnabled(Gamerule.ENABLE_BREEDING)) {
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

    /**
     * Feeds all animals currently in the barn.
     *
     * <p>
     * This method iterates through every {@link Animal} in the barn and invokes
     * {@link Animal#feed()} on each one. The actual effect of feeding depends on
     * the concrete implementation of the animal.
     * </p>
     *
     * <h3>Execution Model</h3>
     * <ul>
     *     <li>Iterates over all animals in the barn</li>
     *     <li>Calls {@link Animal#feed()} for each animal</li>
     * </ul>
     *
     * <h3>Design Notes</h3>
     * <ul>
     *     <li>Feeding behavior (e.g., hunger reduction, happiness increase) is defined per animal</li>
     *     <li>This method does not guarantee feeding success if {@code feed()} is probabilistic</li>
     * </ul>
     *
     * @see Animal#feed()
     */
    public void feedAll() {
        for(Animal a : animals) {
            a.feed();
        }
    }

    /**
     * Pets all dogs in the barn, increasing their happiness.
     *
     * <p>
     * This method iterates through all animals and applies a positive interaction
     * only to instances of {@link Dog}. Each dog receives a fixed happiness boost
     * and a localized message is printed to the console.
     * </p>
     *
     * <h3>Execution Model</h3>
     * <ul>
     *     <li>Filters animals to instances of {@link Dog}</li>
     *     <li>Applies a happiness increase via {@link Animal#happy(int)}</li>
     *     <li>Outputs a localized message for each affected dog</li>
     * </ul>
     *
     * <h3>Design Notes</h3>
     * <ul>
     *     <li>Only dogs can be petted; other animal types are ignored</li>
     *     <li>Happiness increase is fixed at +25</li>
     *     <li>Uses {@link Localization} for message output</li>
     * </ul>
     *
     * @see Dog
     * @see Animal#happy(int)
     */
    public void pet() {
        for(Animal a : animals) {
            if(a instanceof Dog dog) {
                dog.happy(25);
                panel.append(Localization.lang.t(
                        "animal.dog.pet", dog.getName()), Colors.BLUE);
            }
        }
    }
}
