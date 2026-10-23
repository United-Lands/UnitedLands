package org.unitedlands.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.schedulers.NewDayScheduler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedListener 
public class BukkitServerEventListener implements Listener {
    
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {

        Pl3xMapRenderer.instance().initialize();
        UnitedLandsDataManager.instance().loadDataFromDatabase();
        NewDayScheduler.instance().scheduleNewDay();

        UnitedLandsEconomyManager.instance().loadEconomy();
        if (!UnitedLandsEconomyManager.instance().hasEconomy()) {
            United.logger().warning(
                    "No valid economy provider detected, falling back to mockup economy. All economic transactions will be executed as if all economic actors had unlimited funds.", "UnitedLands");
        }
    }

}
