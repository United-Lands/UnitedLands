package org.unitedlands.unitedlands.classes.events.country;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.events.base.CountryEvent;

public class CountryPreRemoveEvent extends CountryEvent {

    public CountryPreRemoveEvent(Country country) {
        super(country);
    }

}
