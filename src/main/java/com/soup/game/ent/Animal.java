package com.soup.game.ent;

import com.soup.game.enums.Phase;
import com.soup.game.enums.Product;
import com.soup.game.enums.Sex;
import com.soup.game.intf.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>Animal</h1>
 * Abstract base class representing a simulated farm animal within the game world.
 *
 * <p>
 * An {@code Animal} models a semi-autonomous entity with internal state,
 * behavioral routines, and probabilistic events such as production and breeding.
 * Each instance progresses through life phases, consumes resources, and may
 * generate products or offspring over time.
 * </p>
 *
 * <h2>Core Attributes</h2>
 * <ul>
 *     <li><b>Biology:</b> {@link Sex} and breeding compatibility</li>
 *     <li><b>Identity:</b> {@code name}, {@link #getLocalizedName()}</li>
 *     <li><b>Production:</b> primary {@link Product} and produced history</li>
 *     <li><b>Lifecycle:</b> {@link Phase}, alive/dead state</li>
 *     <li><b>Biometrics:</b> height, weight</li>
 *     <li><b>Needs:</b> hunger and happiness</li>
 *     <li><b>Reproduction:</b> offspring tracking</li>
 * </ul>
 *
 * <h2>Behavioral Model</h2>
 * <p>
 * Animals operate on a tick-based simulation driven by {@link #update()}.
 * Each update executes a sequence of actions:
 * </p>
 *
 * <ol>
 *     <li>{@link #feed()}</li>
 *     <li>Chance to {@link #produce()} (~20%)</li>
 *     <li>Chance to {@link #breed()} (~5%)</li>
 *     <li>{@link #sleep()}</li>
 * </ol>
 * <h2>Reproduction Model</h2>
 * <p>
 * Animals reproduce sexually and require a compatible partner of the same type
 * and opposite {@link Sex}. Breeding success may be probabilistic and is typically
 * orchestrated externally (e.g., by a {@code Barn}).
 * </p>
 * <p>
 * These behaviors are intentionally abstract and must be defined by subclasses
 * to reflect species-specific logic.
 * </p>
 *
 * <h2>Probabilistic Events</h2>
 * <ul>
 *     <li>Production occurs when {@code Math.random() > 0.8}</li>
 *     <li>Breeding occurs when {@code Math.random() > 0.95}</li>
 * </ul>
 *
 * <h2>State Invariants</h2>
 * <ul>
 *     <li>{@code isAlive == false} prevents further updates</li>
 *     <li>Happiness and hunger are inversely related but not bounded</li>
 *     <li>Produced items are accumulated in {@code products}</li>
 *     <li>Children are tracked but not automatically managed by a {@code Barn}</li>
 * </ul>
 *
 * <h2>Subclass Contract</h2>
 * <p>
 * Implementations must define:
 * </p>
 * <ul>
 *     <li>{@link #feed()} — how hunger/happiness change</li>
 *     <li>{@link #sleep()} — end-of-cycle adjustments</li>
 *     <li>{@link #produce()} — product generation logic</li>
 *     <li>{@link #breed()} — reproduction logic and offspring creation</li>
 *     <li>{@link #getLocalizedName()} — display name</li>
 * </ul>
 *
 * <p>
 * Implementations should ensure:
 * </p>
 * <ul>
 *     <li>No invalid state transitions (e.g., breeding when dead)</li>
 *     <li>Returned products are non-null or explicitly {@link Product#NONE}</li>
 *     <li>Child instances are properly initialized</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *     <li>This class mixes simulation state and behavior intentionally</li>
 *     <li>Randomness is uniform and not externally seeded</li>
 *     <li>No hard constraints are enforced on hunger/happiness ranges</li>
 * </ul>
 *
 * @see Product
 * @see Phase
 * @see #update()
 * @author isGoodSoup
 * @since 1.8
 */
@Entity
@SuppressWarnings("all")
public abstract class Animal {
    private final String name;
    private final Sex sex;
    private final Product product;
    private final List<Product> products;
    private final List<Animal> children;
    private final float height;
    private final float weight;
    private Phase phase;
    private int happiness;
    private int hunger;
    private boolean isAlive;

    /**
     * Constructs a new animal with the specified name, primary product,
     * height, and weight. Initializes happiness, hunger, life phase, and status.
     *
     * @param name    the animal's name
     * @param sex     the animal's sex
     * @param product the main product the animal produces
     * @param height  the animal's height
     * @param weight  the animal's weight
     */
    public Animal(String name, Sex sex, Product product,
                  float height, float weight) {
        this.name = name;
        this.sex = sex;
        this.product = product;
        this.products = new ArrayList<>();
        this.children = new ArrayList<>();
        this.height = height;
        this.weight = weight;
        this.phase = Phase.NEWBORN;
        this.happiness = 100;
        this.hunger = 0;
        this.isAlive = true;
    }

    /**
     * Feeds the animal, affecting its hunger and happiness.
     * <p>
     * Concrete subclasses define specific feeding behavior.
     */
    public abstract void feed();

    /**
     * Makes the animal sleep, affecting its state.
     * <p>
     * Concrete subclasses define specific sleep behavior.
     */
    public abstract void sleep();

    /**
     * Produces the animal's primary product.
     * @return the produced {@link Product}, or {@code null}/{@link Product#NONE} if none
     */
    public abstract Product produce();

    /**
     * Attempts to breed a new animal with a specified partner.
     *
     * <p>
     * This method models sexual reproduction between two compatible animals.
     * Offspring are initialized with randomized attributes where appropriate and
     * inherit the type of their parents. The method does not handle population
     * registration; the caller (e.g., {@link Barn}) is
     * responsible for adding newborns to the simulation.
     * </p>
     *
     * <h3>Preconditions</h3>
     * <ul>
     *     <li>{@code partner} must be non-null</li>
     *     <li>Both animals must satisfy {@link #canBreedWith(Animal)}</li>
     *     <li>Both animals must be alive</li>
     * </ul>
     *
     * <h3>Postconditions</h3>
     * <ul>
     *     <li>Returns a new {@link Animal} instance representing the offspring if breeding succeeds</li>
     *     <li>Returns {@code null} if breeding fails or preconditions are not met</li>
     *     <li>Caller is responsible for adding the child to parent lineage and simulation containers</li>
     * </ul>
     *
     * @param partner the other {@link Animal} to breed with
     * @return the newly created {@link Animal}, or {@code null} if breeding fails
     *
     * @see #canBreedWith(Animal)
     */
    public abstract Animal breed(Animal partner);

    /**
     * Returns the localized display name of this animal.
     * @return the localized name as a string
     */
    public abstract String getLocalizedName();

    /**
     * Updates the animal's internal state for a single game tick.
     *
     * <p>
     * This method models autonomous behavior that does not depend on other animals.
     * Reproduction is intentionally excluded and handled externally (e.g., by a Barn).
     * </p>
     *
     * <h2>Execution Order</h2>
     * <ol>
     *     <li>{@link #feed()}</li>
     *     <li>Chance to {@link #produce()} (~20%)</li>
     *     <li>{@link #sleep()}</li>
     * </ol>
     */
    public void update() {
        if(!isAlive) {
            return;
        }
        feed();
        if(Math.random() > 0.8) {
            Product product = produce();
            if(product != null && product != Product.NONE) {
                products.add(product);
            }
        }
        sleep();
    }

    /**
     * Generates a random name for animals.
     * @return a random animal name
     */
    public static String name() {
        String[] names = {
                "Bessie", "Daisy", "Buttercup", "Chucky", "Henrietta",
                "MooMoo", "Nugget", "Peaches", "Coco", "Ginger",
                "Fluffy", "Snowball", "Pip", "Chirpy", "Eggbert",
                "Wooly", "Oreo", "Pumpkin", "Sunny", "Twinkle"
        };
        int index = (int) (Math.random() * names.length);
        return names[index];
    }

    public String getName() {
        return name;
    }

    public Sex getSex() {
        return sex;
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<Animal> getChildren() {
        return children;
    }

    public Phase getPhase() {
        return phase;
    }

    public Product getProduct() {
        return product;
    }

    public int getHappiness() {
        return happiness;
    }

    public int getHunger() {
        return hunger;
    }

    /**
     * Determines whether this animal can breed with another.
     *
     * <p>
     * Breeding compatibility is defined by a set of constraints that must all be satisfied.
     * This method performs validation only and does not initiate reproduction.
     * </p>
     *
     * <h3>Compatibility Rules</h3>
     * <ul>
     *     <li>Both animals must be non-null</li>
     *     <li>Both animals must be alive</li>
     *     <li>Both animals must be of the same concrete type</li>
     *     <li>Animals must be of opposite {@link com.soup.game.enums.Sex}</li>
     * </ul>
     *
     * @param other the potential breeding partner
     * @return {@code true} if the animals can breed, {@code false} otherwise
     *
     * @see #breed(Animal)
     */
    public boolean canBreedWith(Animal other) {
        return other != null &&
                other.getClass() == this.getClass() &&
                this.sex != other.sex &&
                this.isAlive &&
                other.isAlive;
    }

    /**
     * Increases happiness and reduces hunger.
     * @param i amount to increase happiness and reduce hunger
     */
    public void happy(int i) {
        this.happiness += i;
        this.hunger -= i;
    }

    /**
     * Increases hunger and reduces happiness.
     * @param i amount to increase hunger and reduce happiness
     */
    public void hungry(int i) {
        this.happiness -= i;
        this.hunger += i;
    }

    /**
     * Advances the animal's life phase to the next stage.
     */
    public void age() {
        phase = phase.next();
    }

    public boolean isAlive() {
        return isAlive;
    }
}
