package org.unitedlands.unitedlands.classes.events.region;

import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.events.base.RegionEvent;

public class RegionCreatedEvent extends RegionEvent {

    public RegionCreatedEvent(Region region) {
        super(region);
    }

}
