package com.soup.game.enums;

import java.util.Random;

/**
 * Represents the different types of animals in the game along with
 * their default traits, size, and product.
 *
 * <p>
 * Some animal types support an optional trait enum (e.g., {@link Breed} for dogs).
 * The {@link #randomTrait()} method can be used to assign a random trait if applicable.
 * </p>
 */
public enum AnimalType {
    CHICKEN(Product.EGGS, 30, 3, null),
    COW(Product.MILK, 120, 500, null),
    DOG(Product.NONE, 50, 20, Breed.class),
    DUCK(Product.EGGS, 40, 5, null),
    GOAT(Product.MILK, 70, 50, null),
    HORSE(Product.NONE, 160, 450, null),
    PIG(Product.MEAT, 60, 150, null),
    SHEEP(Product.WOOL, 70, 60, null);

    private static final Random random = new Random();
    private final Product product;
    private final int height;
    private final int weight;
    private final Class<? extends Enum<?>> trait;

    AnimalType(Product product, int height, int weight, Class<? extends Enum<?>> trait) {
        this.product = product;
        this.height = height;
        this.weight = weight;
        this.trait = trait;
    }

    public Product getProduct() { return product; }
    public int getHeight() { return height; }
    public int getWeight() { return weight; }
    public Class<? extends Enum<?>> getTrait() { return trait; }

    public Enum<?> randomTrait() {
        if(trait == null) return null;
        Enum<?>[] values = trait.getEnumConstants();
        return values[random.nextInt(values.length)];
    }
}