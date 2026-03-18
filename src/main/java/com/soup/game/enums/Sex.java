package com.soup.game.enums;

/**
 * <h1>Sex</h1>
 * Represents the biological sex of an {@link com.soup.game.ent.Animal},
 * used to determine breeding compatibility.
 *
 * <p>
 * {@code Sex} is a binary classification used in the reproduction model.
 * Animals must have opposing sexes in order to breed successfully.
 * </p>
 *
 * <h2>Values</h2>
 * <ul>
 *     <li>{@link #MALE}</li>
 *     <li>{@link #FEMALE}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>
 * Sex is assigned at creation time, typically via {@link #random()}, and
 * remains immutable throughout the animal's lifecycle.
 * </p>
 *
 * <h2>Randomization</h2>
 * <p>
 * {@link #random()} returns a uniformly distributed random sex with a 50/50 split.
 * This method is used during animal instantiation and breeding.
 * </p>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *     <li>This model assumes a simplified binary reproduction system</li>
 *     <li>Can be extended to support more complex biological systems if needed</li>
 *     <li>Currently not influenced by genetics or environmental factors</li>
 * </ul>
 *
 * @see com.soup.game.ent.Animal
 */
public enum Sex {
    MALE,
    FEMALE;

    /**
     * Returns a randomly selected {@link Sex} with equal probability.
     * @return {@link #MALE} or {@link #FEMALE} with ~50% chance each
     */
    public static Sex random() {
        return Math.random() < 0.5 ? MALE : FEMALE;
    }
}