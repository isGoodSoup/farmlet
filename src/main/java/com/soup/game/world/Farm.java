package com.soup.game.world;

import com.soup.game.ent.Player;
import com.soup.game.enums.*;
import com.soup.game.intf.World;
import com.soup.game.service.*;
import com.soup.game.swing.SwingPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <h1>Farm</h1>
 * The {@code Farm} class represents a dynamic farm grid in the game.
 * <p>
 * It manages farm tiles, crop planting, harvesting, irrigation, fertilization,
 * and crop growth. The class enforces game rules based on player upgrades,
 * seasonal effects, and environmental conditions.
 * </p>
 *
 * <p><b>Farm Features:</b></p>
 * <ul>
 *     <li>Dynamic square grid of farm {@link Tile} objects (default size 4×4).</li>
 *     <li>Supports planting, harvesting, and ripping crops.</li>
 *     <li>Handles watering and fertilization of crops, respecting player resources and upgrades.</li>
 *     <li>Integrates with {@link Environment} to apply weather effects.</li>
 *     <li>Tracks crop growth and resets harvest state at the end of each day.</li>
 *     <li>Automatically manages crop regrowth for regrowable crops.</li>
 * </ul>
 *
 * <p><b>Upgrade Integration:</b></p>
 * <ul>
 *     <li>{@link Upgrades#HARVEST_UPGRADE} allows harvesting all tiles simultaneously.</li>
 *     <li>{@link Upgrades#PLANT_UPGRADE} allows planting across all tiles.</li>
 *     <li>{@link Upgrades#FERTILIZER_UPGRADE} allows mass application of fertilizer.</li>
 * </ul>
 *
 * <p><b>Grid Indexing:</b></p>
 * <ul>
 *     <li>Rows and columns are 0-based.</li>
 *     <li>Bounds are validated before performing any action on tiles.</li>
 * </ul>
 *
 * <p><b>Command Interface:</b></p>
 * <p>
 * Methods in this class are designed to be invoked via the in-game console
 * using F+ command syntax. Commands are parameterized arrays of {@code String}
 * arguments specifying actions and target coordinates.
 * </p>
 *
 * <pre>
 *   plant 2 1       // plants a crop at row 2, column 1
 *   harvest all     // harvests all crops if HARVEST_UPGRADE unlocked
 *   water 0 3       // irrigates the crop at row 0, column 3
 * </pre>
 *
 * <p><b>Integration:</b></p>
 * <p>
 * Works in conjunction with {@link Player}, {@link Environment}, and {@link Game}
 * to update inventory, XP, and statistics during gameplay.
 * </p>
 *
 * @author isGoodSoup
 * @version 2.0
 * @since 1.0
 */
@SuppressWarnings("all")
@World(entity = "farm")
public class Farm {
    private static final Logger log = LoggerFactory.getLogger(Farm.class);
    private final SwingPanel panel;
    private final List<List<Tile>> tiles;
    private final Player player;
    private final Environment env;
    private final int SIZE = 2;
    private List<Pos> positions;

    /**
     * Constructs a new {@code Farm} for the specified {@link Player} and {@link Environment}.
     * Initializes the farm grid with empty tiles and applies the NULL upgrade to the player.
     *
     * @param panel
     * @param player the player who owns the farm
     * @param env    the environment affecting the farm (weather, seasons)
     */
    public Farm(SwingPanel panel, Player player, Environment env) {
        this.panel = panel;
        this.tiles = new ArrayList<>();
        this.player = player;
        this.env = env;
        populate(SIZE);
        player.add(Upgrades.NULL);
    }

    /**
     * Returns the 2D list representing the farm tiles.
     * @return a {@link List} of {@link List} of {@link Tile} objects
     */
    public List<List<Tile>> tiles() {
        return tiles;
    }

    /**
     * Retrieves the tile at the specified row and column.
     * <p>
     * This is a low-level accessor used internally to simplify tile manipulation.
     * It does not perform bounds checking; callers must ensure valid indices.
     * </p>
     *
     * @param row the row index (0-based)
     * @param col the column index (0-based)
     * @return the {@link Tile} at the specified coordinates, or null if empty
     */
    private Tile get(int row, int col) {
        return tiles.get(row).get(col);
    }

    /**
     * Sets a {@link Tile} at the specified row and column.
     * <p>
     * This replaces the existing tile at the location. It does not perform bounds checking.
     * Use this for internal updates when the grid size is known to be valid.
     * </p>
     *
     * @param row the row index (0-based)
     * @param col the column index (0-based)
     * @param tile the {@link Tile} to place (can be null to clear)
     */
    private void set(int row, int col, Tile tile) {
        tiles.get(row).set(col, tile);
    }

    /**
     * Populates or resizes the farm grid to the specified square size.
     * <p>
     * - If the farm is empty, a new grid of {@code size × size} is created with
     * all tiles set to {@code null}.
     * - If the grid exists:
     *   - Rows and columns are added or removed to match the target size.
     *   - Existing tiles are preserved where possible.
     * </p>
     * <p>This allows dynamic resizing of the farm during gameplay.</p>
     * @param size the target number of rows and columns (square grid)
     */
    public void populate(int size) {
        int rows = size;
        int cols = size;

        if(tiles.isEmpty()) {
            for(int i = 0; i < rows; i++) {
                List<Tile> row = new ArrayList<>();
                for(int j = 0; j < cols; j++) {
                    row.add(null);
                }
                tiles.add(row);
            }
            return;
        }

        log.info("Repopulating grid");
        for(List<Tile> row : tiles) {
            int currentCols = row.size();
            if(currentCols < cols) {
                for(int j = currentCols; j < cols; j++) {
                    row.add(null);
                }
            } else if(currentCols > cols) {
                for(int j = currentCols - 1; j >= cols; j--) {
                    row.remove(j);
                }
            }
        }

        int currentRows = tiles.size();
        if(currentRows < rows) {
            for(int i = currentRows; i < rows; i++) {
                List<Tile> row = new ArrayList<>();
                for(int j = 0; j < cols; j++) {
                    row.add(null);
                }
                tiles.add(row);
            }
        } else if(currentRows > rows) {
            for(int i = currentRows - 1; i >= rows; i--) {
                tiles.remove(i);
            }
        }
    }

    /**
     * Harvests crops from the farm at a specified location or, if the player has
     * the {@link Upgrades#HARVEST_UPGRADE} upgrade, all harvestable crops on the farm.
     * <p>
     * If the "all" keyword is used with the appropriate upgrade, this method
     * iterates over all farm tiles, adds harvested crops to the player's
     * inventory, and either resets the crop to the {@link GrowthStage#SEED} stage
     * (if it regrows) or clears the tile.
     * </p>
     * <p>
     * If a specific tile is specified using row and column arguments, the method
     * validates the coordinates and ensures the crop is ready for harvest.
     * If the crop cannot be harvested yet, or the tile is empty, an error
     * message is printed.
     * </p>
     * <p>
     * Upon successful harvest:
     * <ul>
     *     <li>The crop is added to the player's inventory.</li>
     *     <li>The crop's harvested state is updated.</li>
     *     <li>If the crop regrows, its stage is reset to {@link GrowthStage#SEED}.</li>
     *     <li>If the crop does not regrow, the tile is cleared.</li>
     *     <li>The player gains XP associated with the crop.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Usage examples:
     * <pre>
     *   harvest(new String[]{"harvest", "2", "0"}); // harvests crop at row 2, column 0
     *   harvest(new String[]{"harvest", "all"});   // harvests all crops if HARVEST upgrade unlocked
     * </pre>
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] – the command name "harvest"</li>
     *                 <li>args[1] – either the row index or the keyword "all" (if upgrade unlocked)</li>
     *                 <li>args[2] – the column index (required if harvesting a specific tile)</li>
     *             </ul>
     */
    public void harvest(String[] args) {
        if(args.length < 3 && player.has(Upgrades.HARVEST_UPGRADE)
                && args[1].equalsIgnoreCase("all")) {
            for(Pos pos : letter()) {
                int row = pos.row();
                int col = pos.col();
                Tile tile = get(row, col);
                int totalYield = Math.round(tile.crop().getId().getYield() *
                        (tile.soil().getYieldModifier() +
                                tile.crop().getYieldBonus()));
                for(int i = 0; i < totalYield; i++) {
                    player.inventory().add(tile.crop().getId());
                }
                tile.crop().harvested();
                if(tile.crop().getId().regrows()) {
                    tile.crop().setStage(GrowthStage.SEED);
                } else {
                    set(row, col, null);
                }
                panel.append(Localization.lang.t("game.yields",
                        player.inventory().getQuantity(tile.crop().getId())),
                        Colors.PURPLE);
                player.update(tile.crop().getId().getXp());
                tile.crop().resetYieldBonus();
            }
            panel.append(Localization.lang.t("game.harvest.success.all"),
                    Colors.BRIGHT_GREEN);
            return;
        }

        if(args.length < 3) {
            panel.append(Localization.lang.t("game.harvest.usage"),
                    Colors.PURPLE);
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            panel.append(Localization.lang.t("game.coordinates.invalid"),
                    Colors.BRIGHT_RED);
            return;
        }

        if(row < 0 || row >= tiles.size() || col < 0 || col >= tiles.getFirst().size()) {
            panel.append(Localization.lang.t("game.coordinates.out_of_bounds"),
                    Colors.BRIGHT_RED);
            return;
        }

        Tile tile = get(row, col);
        if(tile.crop() == null) {
            panel.append(Localization.lang.t("game.harvest.nothing"),
                    Colors.BRIGHT_RED);
            return;
        }

        if(!tile.crop().canHarvest()) {
            panel.append(Localization.lang.t("game.harvest.not_ready"),
                    Colors.BRIGHT_RED);
            return;
        }

        int totalYield = Math.round(tile.crop().getId().getYield() *
                (tile.soil().getYieldModifier() +
                        tile.crop().getYieldBonus()));
        for(int i = 0; i < totalYield; i++) {
            player.inventory().add(tile.crop().getId());
        }

        tile.crop().harvested();
        if(tile.crop().getId().regrows()) {
            tile.crop().setStage(GrowthStage.SEED);
        } else {
            set(row, col, null);
        }

        tile.crop().resetYieldBonus();
        panel.append(Localization.lang.t("game.harvest.success",
                tile.crop().getId().getName(), row, col), Colors.BRIGHT_GREEN);
        log.debug("Harvest={}, {}", row, col);
        player.update(tile.crop().getId().getXp());
    }

    /**
     * Resets the harvest state of all crops
     * at the end of the day.
     */
    public void reset() {
        for(Pos pos : letter()) {
            Tile tile = tiles.get(pos.row()).get(pos.col());
            if(tile != null && tile.crop() != null) {
                tile.crop().resetHarvest();
            }
        }
    }

    /**
     * Advances the growth of all crops on the farm,
     * except during dry weather.
     */
    public void grow() {
        if(!Objects.equals(env.getWeather(), Weather.DRY)) {
            for(Pos pos : letter()) {
                Tile tile = tiles.get(pos.row()).get(pos.col());
                if(tile != null && tile.crop() != null) {
                    tile.crop().grow(tile.soil(), tile.fertilizer(),
                            env.getWeather().equals(Weather.STORM) ? 2 : 1);
                }
            }
        }
    }


    /**
     * Plants a crop on the farm at a specified location or across all tiles if
     * the player has the {@link Upgrades#PLANT_UPGRADE} upgrade and uses the "all" keyword.
     * <p>
     * If the "all" keyword is used with the appropriate upgrade, a new random
     * crop is planted on every farm tile.
     * </p>
     * <p>
     * When planting at a specific tile, the method validates the row and column
     * indices, ensures the tile is within bounds, and that it is unoccupied.
     * If the tile is already occupied, an error message is printed.
     * </p>
     * <p>
     * Upon successful planting:
     * <ul>
     *     <li>A new {@link Crop} instance is created with a random ID based on
     *         the current {@link Seasons}.</li>
     *     <li>The crop is placed in a new {@link Tile} at the specified location.</li>
     *     <li>A confirmation message is printed to the console.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Usage examples:
     * <pre>
     *   plant(new String[]{"plant", "2", "0"}); // plants a crop at row 2, column 0
     *   plant(new String[]{"plant", "all"});   // plants crops on all tiles if PLANT upgrade unlocked
     * </pre>
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] – the command name "plant"</li>
     *                 <li>args[1] – either the row index or the keyword "all" (if upgrade unlocked)</li>
     *                 <li>args[2] – the column index (required if planting a specific tile)</li>
     *             </ul>
     */
    public void plant(String[] args) {
        if(args.length >= 2 && player.has(Upgrades.PLANT_UPGRADE)
                && args[1].equalsIgnoreCase("all")) {
            for(Pos pos : letter()) {
                int row = pos.row();
                int col = pos.col();
                if(get(row, col) == null) {
                    set(row, col, new Tile(new Crop(CropID.id.random(env.getSeason())),
                            Soil.SILT, Fertilizer.NONE));
                }
            }
            panel.append(Localization.lang.t("game.plant.success.all"),
                    Colors.BRIGHT_GREEN);
            return;
        }

        if(args.length < 3) {
            panel.append(Localization.lang.t("game.plant.usage"), Colors.PURPLE);
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            panel.append(Localization.lang.t("game.coordinates.invalid"), Colors.BRIGHT_RED);
            return;
        }

        if(row < 0 || row >= tiles.size() || col < 0 || col >= tiles.getFirst().size()) {
            panel.append(Localization.lang.t("game.coordinates.out_of_bounds"), Colors.BRIGHT_RED);
            return;
        }

        if(get(row, col) != null) {
            panel.append(Localization.lang.t("game.plant.occupied"), Colors.BRIGHT_RED);
            return;
        }

        set(row, col, new Tile(new Crop(CropID.id.random(env.getSeason())),
                Soil.SILT, Fertilizer.NONE));
        log.debug("Plant={}, {}", row, col);
        panel.append(Localization.lang.t("game.plant.success", row, col),
                Colors.BRIGHT_GREEN);
    }

    /**
     * Applies a specified {@link Fertilizer} to a farm tile at the given coordinates,
     * or to multiple tiles if the appropriate upgrade and arguments are provided.
     * <p>
     * The method parses the fertilizer type and target coordinates from the input
     * arguments, validates them, and updates the corresponding {@link Tile} by
     * attaching the selected fertilizer.
     * </p>
     *
     * <p><b>Usage:</b></p>
     * <ul>
     *     <li>{@code fertilize <type> <row> <col>} – applies the given fertilizer
     *         to a single tile.</li>
     *     <li>{@code fertilize <type> all} – applies the fertilizer using special
     *         logic if the {@link Upgrades#FERTILIZER_UPGRADE} upgrade is unlocked.</li>
     * </ul>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *     <li>Validates that coordinates are numeric and within farm bounds.</li>
     *     <li>Resolves the fertilizer type from the provided argument.</li>
     *     <li>Updates the tile by replacing it with a new instance containing the
     *         selected fertilizer.</li>
     *     <li>Prints a success message upon successful application.</li>
     * </ul>
     *
     * <p><b>Errors:</b></p>
     * <ul>
     *     <li>If fertilizer is invalid, an error is printed and operation aborted</li>
     *     <li>If fertilizer is already applied, an error is printed and operation aborted</li>
     *     <li>If coordinates are invalid or not numeric, an error is printed.</li>
     *     <li>If arguments are insufficient, usage instructions are displayed.</li>
     *     <li>If coordinates are out of bounds, the operation is aborted.</li>
     * </ul>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *     <li>Fertilizer effects are applied at the tile level and influence crop
     *         growth, yield, or water retention depending on type.</li>
     *     <li>This method does not validate whether the tile already contains a crop.</li>
     *     <li>Fertilizer matching is based on enum name equality.</li>
     * </ul>
     *
     * @param args command arguments where:
     *             <ul>
     *                 <li>{@code args[0]} – command name ("fertilize")</li>
     *                 <li>{@code args[1]} – fertilizer type (must match {@link Fertilizer} enum)</li>
     *                 <li>{@code args[2]} – row index or keyword "all"</li>
     *                 <li>{@code args[3]} – column index (required for single-tile application)</li>
     *             </ul>
     */
    public void fertilize(String[] args) {
        int row, col;
        try {
            row = Integer.parseInt(args[2]);
            col = Integer.parseInt(args[3]);
        } catch(NumberFormatException e) {
            panel.append(Localization.lang.t("game.coordinates.invalid"),
                    Colors.BRIGHT_RED);
            return;
        }

        if(args.length >= 3 && args[2].equalsIgnoreCase("all")
                && player.has(Upgrades.FERTILIZER_UPGRADE)) {

            Fertilizer fertilizer = Arrays.stream(Fertilizer.values())
                    .filter(f -> f.name().equalsIgnoreCase(args[1]))
                    .findFirst()
                    .orElse(null);

            if(player.inventory().getQuantity(fertilizer) < tiles.size()) {
                panel.append(Localization.lang.t("game.fertilize.fail"),
                        Colors.BRIGHT_RED);
                return;
            }

            while(player.inventory().getQuantity(fertilizer) > tiles.size()) {
                for(int r = 0; r < tiles.size(); r++) {
                    for(int c = 0; c < tiles.getFirst().size(); c++) {
                        tiles.get(r).set(c, tiles.get(r).get(c).withFertilizer(fertilizer));
                        player.inventory().remove(fertilizer);
                    }
                }
            }

            panel.append(Localization.lang.t("game.fertilize.success.all"),
                    Colors.BRIGHT_GREEN);
            return;
        }

        if(args.length < 4) {
            panel.append(Localization.lang.t("game.fertilize.usage"), Colors.PURPLE);
            return;
        }

        if(row < 0 || row >= tiles.size() || col < 0 || col >= tiles.getFirst().size()) {
            panel.append(Localization.lang.t("game.coordinates.out_of_bounds"),
                    Colors.BRIGHT_RED);
            return;
        }

        Fertilizer fertilizer = null;
        for(Fertilizer f : Fertilizer.values()) {
            if(f.name().equalsIgnoreCase(args[1])) {
                fertilizer = f;
            }
        }

        if(fertilizer == null) {
            panel.append(Localization.lang.t("game.fertilize.invalid"),
                    Colors.RED);
            return;
        }

        if(get(row, col).fertilizer() != Fertilizer.NONE) {
            panel.append(Localization.lang.t("game.fertilize.done"),
                    Colors.RED);
            return;
        }

        Tile tile = get(row, col).withFertilizer(fertilizer);
        set(row, col, tile);
        player.inventory().remove(fertilizer);
        log.debug("Fertilized={}, {}", row, col);
        panel.append(Localization.lang.t("game.fertilize.success", row, col),
                Colors.BRIGHT_GREEN);
    }

    /**
     * Displays information about the crop at a specified location on the farm.
     * <p>
     * This method prints the crop's ID and its coordinates without modifying the farm.
     * It validates that the row and column indices are provided, numeric, and within bounds.
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] is the command name ("get")</li>
     *                 <li>args[1] is the row index of the crop</li>
     *                 <li>args[2] is the column index of the crop</li>
     *             </ul>
     */
    public void get(String[] args) {
        if(args.length < 3) {
            panel.append(Localization.lang.t("game.get_crop.usage"),
                    Colors.PURPLE);
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            panel.append(Localization.lang.t("game.coordinates.invalid"),
                    Colors.BRIGHT_RED);
            return;
        }

        if(row < 0 || row >= tiles.size() || col < 0 || col >= tiles.getFirst().size()) {
            panel.append(Localization.lang.t("game.coordinates.out_of_bounds"),
                    Colors.BRIGHT_RED);
            return;
        }

        Tile tile = get(row, col);
        if(tile != null && tile.crop() != null) {
            String id = tile.crop().getId().getName();
            log.debug("Crop={}, {} is {}", row, col, id);
            panel.append(Localization.lang.t("game.get_crop", id, row, col),
                    Colors.MAGENTA);
        }
    }
    
    /**
     * Removes (rips) the crop at a specified location on the farm.
     * <p>
     * This method deletes the crop at the given coordinates, making the plot empty.
     * It validates that the row and column indices are provided, numeric, and within bounds.
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] is the command name ("rip")</li>
     *                 <li>args[1] is the row index of the crop to remove</li>
     *                 <li>args[2] is the column index of the crop to remove</li>
     *             </ul>
     */
    public void rip(String[] args) {
        if(args.length < 3) {
            panel.append(Localization.lang.t("game.rip.usage"), Colors.PURPLE);
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(args[1]);
            col = Integer.parseInt(args[2]);
        } catch(NumberFormatException e) {
            panel.append(Localization.lang.t("game.coordinates.invalid"),
                    Colors.BRIGHT_RED);
            return;
        }

        if(row < 0 || row >= tiles.size() || col < 0 || col >= tiles.getFirst().size()) {
            panel.append(Localization.lang.t("game.coordinates.out_of_bounds"),
                    Colors.BRIGHT_RED);
            return;
        }

        set(row, col, null);
        log.debug("Ripped={}, {}", row, col);
        panel.append(Localization.lang.t("game.rip.success", row, col),
                Colors.BRIGHT_GREEN);
    }

    /**
     * Waters a specific crop on the farm if the player has available water.
     * <p>
     * This method requires the player to specify the row and column of the crop
     * they wish to water. If the specified tile contains a crop, its hydration
     * level is set to {@link Hydration#HIGH}, and the player's water resource
     * is decremented by 0.1. A success message is printed showing the remaining water.
     * </p>
     * <p>
     * If the player does not have sufficient water, or if the specified tile
     * is empty or out of bounds, an error message is printed and no changes
     * are made to the farm.
     * </p>
     *
     * <p>
     * Usage example:
     * <pre>
     *   irrigate(new String[]{"water", "2", "0"}); // waters the crop at row 2, column 0
     * </pre>
     * </p>
     *
     * <p>
     * Effects:
     * <ul>
     *     <li>Sets the hydration of the targeted crop to {@link Hydration#HIGH}.</li>
     *     <li>Decrements the player's water resource by 0.1.</li>
     *     <li>Prints a success message with the remaining water, or an error if watering fails.</li>
     * </ul>
     * </p>
     *
     * @param args an array of command arguments where:
     *             <ul>
     *                 <li>args[0] – the command name "water"</li>
     *                 <li>args[1] – the row index of the crop to water</li>
     *                 <li>args[2] – the column index of the crop to water</li>
     *             </ul>
     */
    public void irrigate(String[] args) {
        if(args.length < 3) {
            panel.append(Localization.lang.t("game.irrigate.usage"), Colors.PURPLE);
            return;
        }

        if(player.can() > 0) {
            Tile tile = get(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            if(tile != null && tile.crop() != null) {
                tile.crop().water(Hydration.HIGH);
            }
            player.water(-0.1f);
            Stats.stat().totalWater += 0.1f;
            log.debug("Water={}, {} on {}", Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                    tile.crop().getId().name());
            panel.append(Localization.lang.t("game.irrigate.success", player.can()),
                    Colors.BRIGHT_GREEN);
        } else {
            panel.append(Localization.lang.t("game.irrigate.fail"),
                    Colors.BRIGHT_RED);
        }
    }

    public List<Pos> letter() {
        positions = new ArrayList<>(tiles.getFirst().size()
                * tiles.getFirst().size());
        for(int row = 0; row < tiles.size(); row++) {
            for(int col = 0; col < tiles.getFirst().size(); col++) {
                positions.add(new Pos(row, col));
            }
        }
        return positions;
    }
}
