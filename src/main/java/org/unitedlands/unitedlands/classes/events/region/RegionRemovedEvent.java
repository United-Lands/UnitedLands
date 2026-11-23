package org.unitedlands.unitedlands.classes.events.region;

import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.events.base.RegionEvent;

public class RegionRemovedEvent extends RegionEvent {

    public RegionRemovedEvent(Region region) {
        super(region);
    }

}
