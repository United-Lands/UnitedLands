package org.unitedlands.unitedlands.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.classes.PermissionType;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.utils.United;

import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;

@UnitedListener 
public class BukkitBlockListener implements Listener {

    public BukkitBlockListener() {
    }

    // Break listeners

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleBreak(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemFrameRemove(PlayerItemFrameChangeEvent event) {
        if (event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE) {
            handleBreak(event.getPlayer(), event.getItemFrame().getLocation(), event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        handleBreak(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            handleBreak(player, event.getEntity().getLocation(), event);
        } else {
            if (event.getRemover() instanceof Projectile) {
                if (((Projectile) event.getRemover()).getShooter() instanceof Player player) {
                    handleBreak(player, event.getEntity().getLocation(), event);
                }
            }
        }
    }

    private void handleBreak(Player player, Location location, Cancellable event) {

        if (PermissionManager.instance().hasGlobalOverrides(player))
            return;

        if (!PermissionManager.instance().checkLocationPermissions(player, location,
                PermissionType.BREAK)) {
            event.setCancelled(true);
            United.messenger().send(player, "general-errors.not-allowed");
        }
    }

    // Placement listeners

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        handlePlace(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemFramePlace(PlayerItemFrameChangeEvent event) {
        if (event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.PLACE) {
            handlePlace(event.getPlayer(), event.getItemFrame().getLocation(), event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        handlePlace(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        handlePlace(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        handlePlace(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    private void handlePlace(Player player, Location location, Cancellable event) {

        if (PermissionManager.instance().hasGlobalOverrides(player))
            return;

        if (!PermissionManager.instance().checkLocationPermissions(player, location,
                PermissionType.PLACE)) {

            event.setCancelled(true);
            United.messenger().send(player, "general-errors.not-allowed");
            return;
        }
    }

    // Open, interact and switch listeners

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (PermissionManager.instance().hasGlobalOverrides(player))
            return;

        var block = event.getClickedBlock();
        if (block == null || block.isEmpty() || block.getType() == Material.AIR)
            return;

        var blockType = block.getType().toString();
        if (UnitedLandsConfig.get().protection().containers().contains(blockType)) {
            if (!PermissionManager.instance().checkLocationPermissions(player, block.getLocation(),
                    PermissionType.CONTAINER)) {
                event.setCancelled(true);
                United.messenger().send(player, "general-errors.not-allowed");
                return;
            }
        }

        if (UnitedLandsConfig.get().protection().useBlocks().contains(blockType)) {
            if (!PermissionManager.instance().checkLocationPermissions(player, block.getLocation(),
                    PermissionType.BLOCK_USE)) {
                event.setCancelled(true);
                United.messenger().send(player, "general-errors.not-allowed");
                return;
            }
        }

        if (UnitedLandsConfig.get().protection().switchBlocks().contains(blockType)
                || isSwitchMaterialTagProtected(block.getType())) {
            if (!PermissionManager.instance().checkLocationPermissions(player, block.getLocation(),
                    PermissionType.SWITCH)) {
                event.setCancelled(true);
                United.messenger().send(player, "general-errors.not-allowed");
                return;
            }
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        var player = event.getPlayer();
        if (PermissionManager.instance().hasGlobalOverrides(player))
            return;

        var entity = event.getRightClicked();
        var entityType = entity.getType().toString();
        if (UnitedLandsConfig.get().protection().interact().contains(entityType)) {
            handleInteract(player, entity.getLocation(), event);
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onGrappleEntity(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            Player player = event.getPlayer();
            Entity caughtEntity = event.getCaught();

            if (caughtEntity != null) {
                handleInteract(player, caughtEntity.getLocation(), event);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBonemeal(BlockFertilizeEvent event) {
        var player = event.getPlayer();
        if (PermissionManager.instance().hasGlobalOverrides(player))
            return;

        handleInteract(player, event.getBlock().getLocation(), event);
    }

    private void handleInteract(Player player, Location location, Cancellable event) {

        if (PermissionManager.instance().hasGlobalOverrides(player))
            return;

        if (!PermissionManager.instance().checkLocationPermissions(player, location,
                PermissionType.INTERACT)) {
            event.setCancelled(true);
            United.messenger().send(player, "general-errors.not-allowed");
        }
    }

    // Helpers

    private boolean isSwitchMaterialTagProtected(Material type) {

        for (var tag : PermissionManager.instance().getProtectedSwitchTags()) {
            if (tag.isTagged(type))
                return true;
        }
        return false;
    }

}
