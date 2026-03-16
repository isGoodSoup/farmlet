package com.soup.game.world;

import com.soup.game.enums.CropID;
import com.soup.game.enums.GrowthStage;
import com.soup.game.enums.Hydration;
import com.soup.game.intf.World;

/**
 * Represents a crop in the farm game.
 *
 * <p>A Crop tracks its type ({@link CropID}), growth stage,
 * hydration, and harvest status. It provides methods to grow,
 * wither, be watered, and be harvested.</p>
 *
 * <p>Each crop has a lifecycle from {@link GrowthStage#SEED} to
 * {@link GrowthStage#HARVESTABLE}, and may regrow depending on
 * the crop type.</p>
 */
@World(entity = "crop")
public final class Crop {
    private final CropID id;
    private GrowthStage stage;
    private Hydration hydration;
    private int daysToMature;
    private int days;
    private boolean wasHarvested;
    private boolean canHarvest;
    private boolean canRegrow;
    private boolean wasWatered;
    private boolean isWithered;

    /**
     * Constructs a new Crop with the specified {@link CropID}.
     * <p>The crop is initialized at the SEED stage, with mid-level hydration,
     * and tracks the number of days required to mature.</p>
     * @param id the type of crop
     */
    public Crop(CropID id) {
        this.id = id;
        this.stage = GrowthStage.SEED;
        this.days = 0;
        this.daysToMature = Math.max(1, id.getDays());
        this.canRegrow = id.regrows();
        this.hydration = Hydration.MID;
    }

    /**
     * Advances the crop's growth by one day.
     *
     * <p>If the crop is not yet harvestable, its growth stage may advance
     * depending on the number of days passed. Once the crop reaches maturity,
     * it becomes harvestable.</p>
     */
    public void grow() {
        if(!canHarvest) {
            days++;
            int stageLength = Math.max(1, daysToMature / GrowthStage.values().length);
            if(days % stageLength == 0 && stage != GrowthStage.HARVESTABLE) {
                stage = stage.next();
            }
            if(days == daysToMature) {
                canHarvest = true;
            }
        }
    }

    /**
     * Withers the crop, preventing it from being harvested.
     * <p>Sets the crop's harvest status to false and marks it as no longer
     * able to mature.</p>
     */
    public void wither() {
        canHarvest = false;
        daysToMature = -1;
        isWithered = true;
    }

    /**
     * Returns the sell value of the crop.
     * @return the crop's value in gold
     */
    public int getValue() {
        return id.value();
    }

    /**
     * Returns the name of the crop.
     * @return the crop's localized or default name
     */
    public String getName() {
        return id.name();
    }

    /**
     * Checks whether the crop is ready to be harvested.
     * @return true if the crop can be harvested, false otherwise
     */
    public boolean canHarvest() {
        return canHarvest;
    }

    /**
     * Checks whether the crop was harvested today.
     * @return true if harvested today, false otherwise
     */
    public boolean wasHarvestedToday() {
        return wasHarvested;
    }

    /**
     * Marks the crop as harvested for the current day.
     * @return true once the crop has been harvested
     */
    @SuppressWarnings({"ConstantValue", "UnusedReturnValue"})
    public boolean harvested() {
        wasHarvested = true;
        return wasHarvested;
    }

    /**
     * Resets the harvest status of the crop.
     *
     * <p>This is typically called at the start of a new day
     * to allow re-harvesting or tracking for regrowing crops.</p>
     */
    public void resetHarvest() {
        wasHarvested = false;
    }

    /**
     * Checks whether the crop can regrow after harvesting.
     * @return true if the crop regrows, false otherwise
     */
    public boolean canRegrow() {
        return canRegrow;
    }

    /**
     * Returns the type of this crop.
     * @return the {@link CropID} of the crop
     */
    public CropID getId() {
        return id;
    }

    /**
     * Returns the current growth stage of the crop.
     * @return the {@link GrowthStage} of the crop
     */
    public GrowthStage getStage() {
        return stage;
    }

    /**
     * Sets the current growth stage of the crop.
     * @param stage the new {@link GrowthStage} to set
     */
    public void setStage(GrowthStage stage) {
        this.stage = stage;
    }

    /**
     * Returns the current hydration level of the crop.
     * @return the {@link Hydration} level
     */
    public Hydration getHydration() {
        return hydration;
    }

    /**
     * Waters the crop, updating its hydration level.
     * @param hydration the new {@link Hydration} level
     */
    public void water(Hydration hydration) {
        this.hydration = hydration;
        wasWatered = true;
    }

    /**
     * As in real life farming, when the crop is neglected with
     * no hydration, it dries, to the point of withering.
     */
    public void decay() {
        if(!wasWatered) {
            hydration = hydration.decay();
            if(hydration == Hydration.NONE) {
                wither();
            }
        }
        wasWatered = false;
    }

    /**
     * A check if the crop itself is withered
     * @return returns true if the crop is withered (WOWW),
     * false otherwise
     */
    public boolean isWithered() {
        return isWithered;
    }

    /**
     * Returns a character representing the current display state of the crop.
     * <p>
     * The returned character encodes the crop’s status for rendering in the farm grid:
     * <ul>
     *     <li>{@code ' '} – The crop was harvested today.</li>
     *     <li>{@code 'H'} – The crop is ready to be harvested.</li>
     *     <li>{@code 'X'} – The crop has withered.</li>
     *     <li>Otherwise, the first letter of the crop's growth stage (e.g., 'S' for SEED, 'G' for GROWING).</li>
     * </ul>
     * </p>
     *
     * @return a {@code char} representing the crop’s current display state
     */
    public char getChar() {
        if(wasHarvested) { return ' '; }
        if(isWithered) { return 'X'; }
        return stage.name().charAt(0);
    }
}
