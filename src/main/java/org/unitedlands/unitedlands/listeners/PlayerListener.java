package org.unitedlands.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.unitedlands.unitedlands.classes.events.base.PlayerChangeChunkEvent;
import org.unitedlands.unitedlands.classes.events.player.PlayerEnterRegionEvent;
import org.unitedlands.unitedlands.classes.events.player.PlayerEnterSettlementEvent;
import org.unitedlands.unitedlands.classes.events.player.PlayerExitSettlementEvent;
import org.unitedlands.unitedlands.managers.DisplayManager;
import org.unitedlands.utils.United;

public class PlayerListener implements Listener {

    public PlayerListener() {
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnterSettlement(PlayerEnterSettlementEvent event) {
        DisplayManager.instance().showSettlementNameDisplay(event.getSettlement(), event.getPlayer());

        // Test for banishment upon entering settlement
        if (event.getSettlement().isBanished(event.getPlayer())) {
            United.messenger().send(event.getPlayer(), "player.settlement.banish-notice");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onExitSettlement(PlayerExitSettlementEvent event) {
        DisplayManager.instance().hideSettlementNameDisplay(event.getSettlement(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnterRegion(PlayerEnterRegionEvent event) {
        DisplayManager.instance().showRegionName(event.getRegion(), event.getPlayer(), event.getAdditionalDisplay());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChangeChunk(PlayerChangeChunkEvent event) {
        if (DisplayManager.instance().isPlayerViewingMap(event.getPlayer()))
            DisplayManager.instance().showMap(event.getPlayer(), event.getToLocation());
    }
}
