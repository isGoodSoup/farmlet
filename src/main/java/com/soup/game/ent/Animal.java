package com.soup.game.ent;

import com.soup.game.enums.AnimalType;
import com.soup.game.enums.Phase;
import com.soup.game.enums.Product;
import com.soup.game.enums.Sex;
import com.soup.game.intf.Entity;
import com.soup.game.service.Colors;
import com.soup.game.service.Localization;
import com.soup.game.swing.SwingPanel;

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
    private final AnimalType animalType;
    private final List<Product> products;
    private final List<Animal> children;
    private static SwingPanel panel;
    private int weight;
    private int height;
    private Enum<?> trait;
    private Phase phase;
    private int happiness;
    private int hunger;
    private int meals;
    private boolean isAlive;
    private boolean wasFed;

    private boolean wasHungryMessageSent = false;
    private boolean wasUnhappyMessageSent = false;

    public Animal(String name, Sex sex, AnimalType animalType) {
        this.name = name;
        this.animalType = animalType;
        this.sex = sex;
        this.products = new ArrayList<>();
        this.children = new ArrayList<>();
        this.phase = Phase.NEWBORN;
        this.happiness = 100;
        this.hunger = 0;
        this.meals = 3;
        this.isAlive = true;
        this.wasFed = false;
    }

    public static void setPanel(SwingPanel panel) {
        Animal.panel = panel;
    }

    public static SwingPanel panel() {
        if(panel == null) { throw new IllegalStateException("Panel not set"); }
        return panel;
    }

    /**
     * Attempts to feed the animal based on a probabilistic check.
     *
     * <p>
     * Feeding does not always occur. This method applies a random chance
     * to determine whether feeding succeeds. If successful, {@link #food()}
     * is invoked to apply the actual feeding effects.
     * </p>
     *
     * <h3>Probability</h3>
     * <ul>
     *     <li>Feeding succeeds when {@code Math.random() > 0.2} (~80% chance)</li>
     *     <li>Otherwise, no action is performed</li>
     * </ul>
     *
     * <h3>Design Notes</h3>
     * <ul>
     *     <li>Introduces variability in animal care behavior</li>
     *     <li>Actual effects (e.g., hunger reduction, happiness increase)
     *         are handled by {@link #food()}</li>
     * </ul>
     *
     * @see #food()
     */
    public void feed() {
        if(!(Math.random() > 0.2f)) {
            return;
        }
        food();
    }

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
     * registration; the caller (e.g. Barn) is
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
    public Animal breed(Animal partner) {
        if(!canBreedWith(partner)) { return null; }
        Class<? extends Animal> cls = (Class<? extends Animal>) this.getClass();

        Animal child;
        try {
            child = cls.getConstructor(String.class)
                    .newInstance(Animal.name());
        } catch (Exception e) {
            panel().append(e.getMessage(), Colors.BRIGHT_RED);
            return null;
        }

        int childHeight = (this.getHeight() + partner.getHeight()) / 2
                + (int)((Math.random() - 0.5) * 10);
        int childWeight = (this.getWeight() + partner.getWeight()) / 2
                + (int)((Math.random() - 0.5) * 20);

        child.setHeight(childHeight);
        child.setWeight(childWeight);

        if(this.getTrait() != null && partner.getTrait() != null) {
            child.setTrait(Math.random() < 0.5 ? this.getTrait() : partner.getTrait());
        }
        return child;
    }

    /**
     * Returns the localized display name of this animal.
     * @return the localized name as a string
     */
    public abstract String getLocalizedName();

    /**
     * Updates the animal's internal state for a single game tick and
     * saves the product to {@link Player}'s inventory.
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
     *
     * @param player the {@link Player}, one and only
     */
    public void update(Player player) {
        if(!isAlive) {
            return;
        }

        if(Math.random() > 0.8) {
            Product product = produce();
            if(product != null && product != Product.NONE) {
                products.add(product);
                player.inventory().add(product);
            }
        }
        if(happiness < 20) {
            if(!wasUnhappyMessageSent) {
                panel().append("\n" + Localization.lang.t("animal.unhappy", getName(),
                        getLocalizedName()), Colors.BRIGHT_PURPLE);
                wasUnhappyMessageSent = true;
            }
        } else {
            wasUnhappyMessageSent = false;
        }

        if(hunger > 60 || !wasFed || meals >= 2) {
            if(!wasHungryMessageSent) {
                panel().append("\n" + Localization.lang.t("animal.hungry", getName(),
                        getLocalizedName()), Colors.BRIGHT_RED);
                wasHungryMessageSent = true;
            }
        } else {
            wasHungryMessageSent = false;
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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
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

    public int getHappiness() {
        return happiness;
    }

    public int getHunger() {
        return hunger;
    }

    public void setTrait(Enum<?> trait) {
        this.trait = trait;
    }

    public Enum<?> getTrait() {
        return trait;
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
     *     <li>Animals must be of opposite {@link Sex}</li>
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

    /**
     * Represents the total meals an animal has (3) and how per time fed
     * this method gets called and {@link #meals} is reduced, therefore
     * tracking what the {@link Animal} has per day. To track if the animal
     * was fed at all during the day, a {@link #fed()} method is used.
     * <p>
     * When is < 0, it gets adjusted to said number.
     * </p>
     *
     * @see #fed()
     */
    public void food() {
        if(meals < 0) {
            meals = 0;
        } else {
            meals -= 1;
            fed();
        }
    }

    /**
     * Method to track if the {@link Animal} was fed at all during the day,
     * gets called by {@link #food()} along with the meal tracking.
     *
     * @see #food()
     */
    public void fed() {
        wasFed = true;
    }

    /**
     * Is the Animal alive?
     * @return true if yes, false otherwise
     */
    public boolean isAlive() {
        return isAlive;
    }

    /**
     * Was the {@link Animal} fed today? This boolean is referenced once a day,
     * once per tick, through {@link #fed}.
     * @return if the Animal was fed today, then it returns true, false otherwise
     * @see #fed
     */
    public boolean wasFedToday() {
        return wasFed;
    }
}
