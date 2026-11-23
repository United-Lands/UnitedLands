package org.unitedlands.unitedlands.classes.events.region;

import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.events.base.RegionEvent;

public class RegionUpdatedEvent extends RegionEvent {

    private final boolean visualChange;

    public RegionUpdatedEvent(Region region, boolean visualChange) {
        super(region);
        this.visualChange = visualChange;
    }

    public boolean isVisualChange() {
        return visualChange;
    }

}
