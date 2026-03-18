package com.soup.game.ent;

import com.soup.game.enums.Phase;
import com.soup.game.enums.Product;
import com.soup.game.intf.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class representing a farm animal.
 * <p>
 * Each animal has a name, a primary product it produces (e.g., milk, eggs, wool),
 * a list of all products it has produced, a list of its offspring, and attributes
 * such as height, weight, happiness, hunger, and life phase.
 * <p>
 * Concrete subclasses must implement feeding, sleeping, production, breeding,
 * and localized naming behavior.
 */
@Entity
@SuppressWarnings("all")
public abstract class Animal {
    private final String name;
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
     * @param product the main product the animal produces
     * @param height  the animal's height
     * @param weight  the animal's weight
     */
    public Animal(String name, Product product,
                  float height, float weight) {
        this.name = name;
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
     * Breeds a new animal of the same type.
     * @return the newly created {@link Animal}, or {@code null} if breeding fails
     */
    public abstract Animal breed();

    /**
     * Returns the localized display name of this animal.
     * @return the localized name as a string
     */
    public abstract String getLocalizedName();

    /**
     * Updates the animal's state for a single game tick:
     * feeds it, potentially produces a product, potentially breeds,
     * and makes it sleep.
     */
    public void update() {
        if(!isAlive) {
            return;
        }

        feed();
        if(Math.random() > 0.8) {
            Product product = produce();
            if(product.equals(Product.NONE)) {
                return;
            }
            if(product != null) { products.add(product); }
        }

        if(Math.random() > 0.9f) {
            Animal child = breed();
            if(child != null) { children.add(child); }
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
