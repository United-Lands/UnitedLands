package org.unitedlands.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.events.country.CountryUpdatedEvent;
import org.unitedlands.unitedlands.managers.DisplayManager;

@UnitedListener
public class UnitedLandsCountryListener implements Listener {

    @EventHandler
    public void onCountryUpdated(CountryUpdatedEvent event) {
        for (var settlement : event.getCountry().getSettlements())
            DisplayManager.instance().refreshSettlementDisplay(settlement);
    }

}
