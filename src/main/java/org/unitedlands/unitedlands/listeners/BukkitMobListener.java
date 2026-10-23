package org.unitedlands.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;

@UnitedListener 
public class BukkitMobListener implements Listener {

    public BukkitMobListener() {
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {

        if (event.getSpawnReason() == SpawnReason.CUSTOM || event.getSpawnReason() == SpawnReason.COMMAND)
            return;

        var creatureType = event.getEntityType().toString();

        var isMonster = UnitedLandsConfig.get().protection().monsters().contains(creatureType);
        var isAnimal = UnitedLandsConfig.get().protection().animals().contains(creatureType);

        if (!isMonster && !isAnimal)
            return;

        var location = event.getLocation();
        var chunkCoordinates = CoordinateUtils.locationToChunkCoordinates(location);

        var settlementChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoordinates);
        if (settlementChunk != null) {
            if (!settlementChunk.allowMonsters() && isMonster) {
                event.setCancelled(true);
                return;
            }
            if (!settlementChunk.allowAnimals() && isAnimal) {
                event.setCancelled(true);
                return;
            }
        } else {
            var region = UnitedLandsDataManager.instance()
                    .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(location));
            if (region != null) {
                if (!region.allowMonsters() && isMonster) {
                    event.setCancelled(true);
                    return;
                }
                if (!region.allowAnimals() && isAnimal) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

    }

}
