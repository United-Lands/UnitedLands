package org.unitedlands.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.PlotTypeBehavior;
import org.unitedlands.unitedlands.managers.PermissionManager;

@UnitedListener
public class BukkitPlayerDeathListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!PermissionManager.instance().hasPlotBehavior(event.getEntity().getLocation(),
                PlotTypeBehavior.KEEP_INVENTORY))
            return;

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
    }

}
