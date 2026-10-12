package org.unitedlands.unitedlands.classes.events.country;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.events.base.CountryEvent;

public class CountryCreatedEvent extends CountryEvent {

    public CountryCreatedEvent(Country country) {
        super(country);
    }

}
