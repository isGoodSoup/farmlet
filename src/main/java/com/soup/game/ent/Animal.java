package com.soup.game.ent;

import com.soup.game.enums.Phase;
import com.soup.game.enums.Product;
import com.soup.game.intf.Entity;

import java.util.ArrayList;
import java.util.List;

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

    public abstract void feed();
    public abstract void sleep();
    public abstract Product produce();
    public abstract Animal breed();
    public abstract String getLocalizedName();

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


    public void happy(int i) {
        this.happiness += i;
        this.hunger -= i;
    }

    public void hungry(int i) {
        this.happiness -= i;
        this.hunger += i;
    }

    public void age() {
        phase = phase.next();
    }

    public boolean isAlive() {
        return isAlive;
    }
}
