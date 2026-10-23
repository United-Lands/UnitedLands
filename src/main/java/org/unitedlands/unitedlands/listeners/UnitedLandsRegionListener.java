package org.unitedlands.unitedlands.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.events.region.RegionClaimedEvent;

import org.unitedlands.utils.United;

@UnitedListener
public class UnitedLandsRegionListener implements Listener {

    @EventHandler
    public void onRegionClaimedEvent(RegionClaimedEvent event) {
        United.messenger().send(Bukkit.getServer(), "player.country.claim.claimed-broadcast", event.getCountry().getCleanName(), event.getRegion().getCleanName());
    }

}
