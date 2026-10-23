package org.unitedlands.unitedlands.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.events.settlement.SettlmentExhaustionPreventedEvent;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;

@UnitedListener
public class BukkitPlayerMiscListener implements Listener {

    @EventHandler
	public void onEntityExhaustion(EntityExhaustionEvent event) {
        
        if (!UnitedLandsConfig.get().general().preventHungerInSettlement())
            return;

        if (event.getEntity() instanceof Player player) {
            
            var citizen = UnitedLandsDataManager.instance().getCitizen(player);
            if (citizen == null)
                return;

            if (!citizen.hasSettlement())
                return;

            var sc = UnitedLandsDataManager.instance().getSettlementChunk(player.getLocation());
            if (sc != null && sc.getSettlement().equals(citizen.getSettlement())) {

                // Other plugins like UnitedWars may want to override hunger prevention
                var exhaustionPreventedEvent = new SettlmentExhaustionPreventedEvent(sc.getSettlement(), player);
                exhaustionPreventedEvent.callEvent();

                if (!exhaustionPreventedEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }

        }

    }
}
