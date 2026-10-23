package org.unitedlands.unitedlands.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.PermissionType;
import org.unitedlands.unitedlands.classes.events.base.PermissablePlayerDamageEvent;
import org.unitedlands.unitedlands.classes.events.base.PermissablePlayerDamageEvent.DamageType;

import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedListener 
public class BukkitPlayerDamageListener implements Listener {

    @EventHandler
    private void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {

        if (event.getDamager() == null)
            return;

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        if (attacker == null)
            return;

        if ((event.getEntity() instanceof Player)) {
            handlePvpAttack(event, attacker);
        } else {
            if (event.getEntity() instanceof LivingEntity livingEntity) {
                handlePveAttack(event, livingEntity, attacker);
            }
        }

    }

    private void handlePvpAttack(EntityDamageByEntityEvent event, Player attacker) {

        var settlementChunk = UnitedLandsDataManager.instance().getSettlementChunk(CoordinateUtils.locationToChunkCoordinates(event.getEntity().getLocation()));
        if (settlementChunk != null) {
            if (!settlementChunk.allowPvp()) {
                cancelAttack(event, attacker, DamageType.PVP);
                return;
            }
        } else {
            var region = UnitedLandsDataManager.instance().getRegion(CoordinateUtils.locationToChunkCenterCoordinates(event.getEntity().getLocation()));
            if (region != null) {
                if (!region.allowPvp()) {
                    cancelAttack(event, attacker, DamageType.PVP);
                    return;
                }
            }
        }
    }

    private void handlePveAttack(EntityDamageByEntityEvent event, LivingEntity victim, Player attacker) {

        // Hostile mobs are never protected
        if (victim instanceof Monster)
            return;

        var coords = CoordinateUtils.locationToChunkCoordinates(event.getEntity().getLocation());
        var settlementChunk = UnitedLandsDataManager.instance().getSettlementChunk(coords);
        if (settlementChunk != null) {
            if (!PermissionManager.instance().checkLocationPermissions(attacker, victim.getLocation(),
                    PermissionType.INTERACT)) {
                cancelAttack(event, attacker, DamageType.PVE);
                return;
            }
        }

        var region = UnitedLandsDataManager.instance().getRegion(coords);
        if (region != null && !region.allowPvp()) {
            if (!PermissionManager.instance().checkLocationPermissions(attacker, victim.getLocation(),
                    PermissionType.INTERACT)) {
                cancelAttack(event, attacker, DamageType.PVE);
                return;
            }
        }
    }

    private void cancelAttack(EntityDamageByEntityEvent event, Player attacker, DamageType type) {

        // Call event. Other plugins might override the cancellation (e.g. UnitedWars in
        // war zones).
        PermissablePlayerDamageEvent damageEvent = new PermissablePlayerDamageEvent(attacker, event.getEntity().getLocation(), type);
        damageEvent.setCancelled(true);
        damageEvent.callEvent();

        if (damageEvent.isCancelled()) {
            United.messenger().send(attacker, "general-errors.damage-disabled");
            event.setCancelled(true);
        }

    }

}
