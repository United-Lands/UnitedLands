package org.unitedlands.unitedlands.classes.events.country;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.events.base.CountryEvent;

public class CountryRemovedEvent extends CountryEvent {

    public CountryRemovedEvent(Country country) {
        super(country);
    }

}
