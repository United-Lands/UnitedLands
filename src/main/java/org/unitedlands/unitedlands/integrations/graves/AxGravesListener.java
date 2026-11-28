package org.unitedlands.unitedlands.integrations.graves;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.PlotTypeBehavior;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

import com.artillexstudios.axgraves.api.events.GravePreSpawnEvent;

@UnitedListener (
    requirePlugins = "AxGraves"
)
public class AxGravesListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onGraveCreate(GravePreSpawnEvent event) {
        
        var location = event.getLocation();
        var player = event.getPlayer();

        if (PermissionManager.instance().hasPlotBehavior(location, PlotTypeBehavior.KEEP_INVENTORY)) {
            event.setCancelled(true);
            return;
        }

        var settlement = UnitedLandsDataManager.instance().getSettlement(location);
        if (settlement == null)
            return;

        if (settlement.isBanished(player)) {
            United.messenger().send(player, "player.settlement.banish-punishment");
            event.setCancelled(true);
        }

    }

}
