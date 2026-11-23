package org.unitedlands.unitedlands.classes.events.country;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.events.base.CountryEvent;

public class CountryUpdatedEvent extends CountryEvent {

    private final boolean visualChange;

    public CountryUpdatedEvent(Country country, boolean visualChange) {
        super(country);
        this.visualChange = visualChange;
    }

    public boolean isVisualChange() {
        return visualChange;
    }

}
