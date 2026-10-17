package org.unitedlands.unitedlands.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitTask;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.PlayerCache;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.utils.United;

@UnitedListener
public class FlightManager {
    private static FlightManager instance;

    public static FlightManager instance() {
        return instance;
    }

    private Set<Player> protectedPlayers = new HashSet<>();
    private Map<Player, BukkitTask> removalTasks = new HashMap<>();

    public FlightManager() {
        instance = this;
    }

    public void activateFlight(Player player) {
        if (!player.getAllowFlight()) {
            United.logger().info("Activating flight permissions for player " + player);
            player.setAllowFlight(true);
        }
    }

    public boolean isAllowedToFly(Player player, PlayerCache cache) {

        if (PermissionManager.instance().hasGlobalOverrides(player)) {
            return true;
        }

        // See is player is in UL-managed world
        if (!UnitedLandsConfig.get().general().worlds().contains(player.getLocation().getWorld().getName())) {
            // Return check for dimension override
            return PermissionManager.instance().hasAnyPermission(player,
                    UnitedLandsConfig.get().general().flight().dimensionOverridePermissions());
        }

        // Check general flight permissions
        if (!PermissionManager.instance().hasAnyPermission(player,
                UnitedLandsConfig.get().general().flight().permissions())) {
            return false;
        }

        // Check if player is in town or wilderness
        if (cache.getCachedSettlement() != null) {

            // Player might be a moderator with overrides
            if (PermissionManager.instance().hasAnyPermission(player,
                    UnitedLandsConfig.get().general().flight().anySettlementOverridePermissions()))
                return true;

            // Check if player is citizen or has trust
            if (cache.getCachedSettlement().hasCitizen(player)) {
                return true;
            }
            if (cache.getCachedSettlement().hasTrusted(player)) {
                return true;
            }
        }

        // Return hceck for wilderness override
        return PermissionManager.instance().hasAnyPermission(player,
                UnitedLandsConfig.get().general().flight().wildernessOverridePermissions());
    }

    public void removeFlight(Player player, PlayerCache cache) {

        // Player already not allowed to fly
        if (!player.getAllowFlight() && !player.isFlying())
            return;

        // Don't schedule twice
        if (removalTasks.containsKey(player))
            return;

        if (player.isFlying()) {
            // Remove flight 5 seconds after leaving allowed flight zone
            var removalTask = Bukkit.getScheduler().runTaskLater(UnitedLands.instance(), () -> {
                deactivateFlight(player, cache);
            }, 5 * 20L);
            removalTasks.put(player, removalTask);
            United.messenger().send(player, "player.flight.removing");
        } else {
            player.setAllowFlight(false);
        }
    }

    public void deactivateFlight(Player player, PlayerCache cache) {

        removalTasks.remove(player);

        // Player may have returned to an allowed flight zone
        if (isAllowedToFly(player, cache))
            return;

        if (player.getAllowFlight()) {
            United.logger().info("Deactivating flight permissions for player " + player);
            player.setAllowFlight(false);
            if (player.isFlying()) {
                protectPlayerFromFallDamage(player);
            }
            United.messenger().send(player, "player.flight.removed");
        }

    }

    private void protectPlayerFromFallDamage(Player player) {
        protectedPlayers.add(player);
        Bukkit.getScheduler().runTaskLater(UnitedLands.instance(), () -> removeFallProtection(player), 100);
    }

    private void removeFallProtection(Player player) {
        protectedPlayers.remove(player);
    }

    @EventHandler
    private void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != DamageCause.FALL)
            return;
        if (event.getEntity() instanceof Player player) {
            if (protectedPlayers.contains(player))
                event.setCancelled(true);
        }
    }

    // Optional auto-triggering, but might be unreliable and interfere with PVP, so deactivate for now

    // @EventHandler
    // private void onFlightToggle(PlayerToggleFlightEvent event) {
        // if (!event.isFlying()) {
            // var player = event.getPlayer();
            // if (!FlightManager.instance().isAllowedToFly(player,
                // PlayerCacheManager.instance().getPlayerCache(player))) {
                // United.messenger().send(player, "player.flight.no-permission");
                // event.setCancelled(true);
            // }
        // }
    // }

}
