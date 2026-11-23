package org.unitedlands.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementUpdatedEvent;
import org.unitedlands.unitedlands.classes.events.settlementchunk.SettlementChunkUpdatedEvent;
import org.unitedlands.unitedlands.managers.DisplayManager;
import org.unitedlands.unitedlands.managers.PlayerCacheManager;

@UnitedListener
public class UnitedLandsSettlementListener implements Listener {

    @EventHandler
    public void onSettlementUpdated(SettlementUpdatedEvent event) {
        PlayerCacheManager.instance().refreshMembershipsForSettlement(event.getSettlement());
        DisplayManager.instance().refreshSettlementDisplay(event.getSettlement());
    }

    @EventHandler
    public void onSettlementChunkUpdated(SettlementChunkUpdatedEvent event) {
        PlayerCacheManager.instance().refreshMembershipsForSettlementChunk(event.getSettlementChunk());
    }

}
