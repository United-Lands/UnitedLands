package org.unitedlands.unitedlands.classes.events.country;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.events.base.CostEvent;

public class CountryUpkeepCalculatedEvent extends CostEvent {

    private final Country country;

    public CountryUpkeepCalculatedEvent(Country country, double originalCosts) {
        super(originalCosts);
        this.country = country;
    }

    public Country getCountry() {
        return country;
    }

}
