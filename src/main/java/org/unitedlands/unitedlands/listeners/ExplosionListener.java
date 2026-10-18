package org.unitedlands.unitedlands.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;

public class ExplosionListener implements Listener {

    public ExplosionListener() {
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent event) {
        if (!(event.getSource().getType() == Material.FIRE))
            return;
        var location = event.getBlock().getLocation();
        if (!isFireAllowed(location)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        var location = event.getBlock().getLocation();
        if (!isExplosionAllowed(location)) {
            event.blockList().clear();
            return;
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        var location = event.getLocation();
        if (!isExplosionAllowed(location)) {
            event.blockList().clear();
            return;
        }
    }

    @EventHandler
    public void onEntityDamagedByExplosion(EntityDamageEvent event) {
        if (!(event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION))
            return;

        var location = event.getEntity().getLocation();
        if (!isExplosionAllowed(location)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onVehicleDamageByExplosion(VehicleDamageEvent event) {
        // Weg 1: Über das moderne DamageSource-System (Paper)
        if (event.getDamageSource().getDamageType() == DamageType.EXPLOSION ||
                event.getDamageSource().getDamageType() == DamageType.PLAYER_EXPLOSION) {
            if (!isExplosionAllowed(event.getVehicle().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
        Entity attacker = event.getAttacker();
        if (attacker == null ||
                attacker instanceof Creeper ||
                attacker instanceof TNTPrimed ||
                attacker instanceof Fireball ||
                attacker instanceof Wither) {
            if (!isExplosionAllowed(event.getVehicle().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }

    }

    private boolean isExplosionAllowed(Location location) {
        var chunkCoordinates = CoordinateUtils.locationToChunkCoordinates(location);
        var settlementchunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoordinates);
        if (settlementchunk != null) {
            if (!settlementchunk.allowExplosions()) {
                return false;
            }
        } else {
            var region = UnitedLandsDataManager.instance()
                    .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(location));
            if (region != null) {
                if (!region.allowExplosions()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isFireAllowed(Location location) {
        var chunkCoordinates = CoordinateUtils.locationToChunkCoordinates(location);
        var settlementchunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoordinates);
        if (settlementchunk != null) {
            if (!settlementchunk.allowFire()) {
                return false;
            }
        } else {
            var region = UnitedLandsDataManager.instance()
                    .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(location));
            if (region != null) {
                if (!region.allowFire()) {
                    return false;
                }
            }
        }
        return true;
    }

}
