package com.soup.game.world;

import com.soup.game.ent.Crop;
import com.soup.game.enums.Fertilizer;
import com.soup.game.enums.Soil;
import com.soup.game.intf.Data;
import com.soup.game.intf.World;

/**
 * Represents a single tile on the farm grid.
 * <p>
 * A tile is the fundamental unit of the farming world and may contain:
 * </p>
 *
 * <ul>
 *   <li>A {@link Crop} currently planted on the tile</li>
 *   <li>A {@link Soil} type that influences crop growth and hydration</li>
 *   <li>A {@link Fertilizer} that modifies farming behavior</li>
 * </ul>
 *
 * <p>
 * Tiles are typically arranged in a 2D grid within the farm world and
 * are used to simulate planting, watering, harvesting, and soil
 * management mechanics.
 * </p>
 *
 * <p>
 * Because this class is implemented as a {@code record}, it is immutable
 * and primarily used as a lightweight data container within the game
 * world.
 * </p>
 *
 * @param crop the crop planted on the tile, or {@code null} if empty
 * @param soil the soil type affecting crop behavior
 * @param fertilizer the fertilizer applied to the tile
 */
@Data
@World
public record Tile(Crop crop, Soil soil, Fertilizer fertilizer) {}