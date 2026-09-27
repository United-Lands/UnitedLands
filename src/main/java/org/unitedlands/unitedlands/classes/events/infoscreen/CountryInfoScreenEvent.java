package org.unitedlands.unitedlands.classes.events.infoscreen;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.events.base.InfoScreenEvent;
import org.unitedlands.unitedlands.classes.infoscreen.InfoScreen;

public class CountryInfoScreenEvent extends InfoScreenEvent {

    private final Country country;

    public CountryInfoScreenEvent(InfoScreen infoScreen, Country country) {
        super(infoScreen);
        this.country = country;
    }

    public Country getCountry() {
        return country;
    }

}
