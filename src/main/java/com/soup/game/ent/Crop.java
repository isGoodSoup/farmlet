package com.soup.game.ent;

import com.soup.game.enums.CropID;
import com.soup.game.enums.GrowthStage;
import com.soup.game.enums.Hydration;
import com.soup.game.intf.Entity;

@Entity(type = "crop")
public final class Crop {
    private final CropID id;
    private GrowthStage stage;
    private Hydration hydration;
    private int daysToMature;
    private int days;
    private boolean wasHarvested;
    private boolean canHarvest;
    private boolean canRegrow;

    public Crop(CropID id) {
        this.id = id;
        this.stage = GrowthStage.SEED;
        this.days = 0;
        this.daysToMature = id.getDays();
        this.canRegrow = id.regrows();
        this.hydration = Hydration.MID;
    }

    public void grow() {
        if(!canHarvest) {
            days++;
            int stageLength = daysToMature/GrowthStage.values().length;
            if(days % stageLength == 0 && stage != GrowthStage.HARVESTABLE) {
                stage = stage.next();
            }
            if(days == daysToMature) {
                canHarvest = true;
            }
        }
    }

    public void wither() {
        canHarvest = false;
        daysToMature = -1;
    }

    public int getValue() {
        return id.getValue();
    }

    public String getName() {
        return id.getName();
    }

    public boolean canHarvest() {
        return canHarvest;
    }
    public boolean wasHarvestedToday() {
        return wasHarvested;
    }

    @SuppressWarnings({"ConstantValue", "UnusedReturnValue"})
    public boolean harvested() {
        wasHarvested = true;
        return wasHarvested;
    }

    public void resetHarvest() {
        wasHarvested = false;
    }
    public boolean canRegrow() {
        return canRegrow;
    }
    public CropID getId() {
        return id;
    }
    public GrowthStage getStage() {
        return stage;
    }
    public void setStage(GrowthStage stage) {
        this.stage = stage;
    }

    public Hydration getHydration() {
        return hydration;
    }
    public void setHydration(Hydration hydration) {
        this.hydration = hydration;
    }
}
