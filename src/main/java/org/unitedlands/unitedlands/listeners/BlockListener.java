package org.unitedlands.unitedlands.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.unitedlands.unitedlands.classes.PermissionType;
import org.unitedlands.unitedlands.classes.Settings;
import org.unitedlands.unitedlands.managers.PermissionManager;
import net.kyori.adventure.text.Component;

public class BlockListener implements Listener {

    public BlockListener() {
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        if (PermissionManager.instance().hasGlobalOverrides(player))
            return;

        if (!PermissionManager.instance().checkLocationPermissions(player, event.getBlock().getLocation(),
                PermissionType.BREAK)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cYou cannot break blocks here."));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (PermissionManager.instance().hasGlobalOverrides(player))
            return;

        if (!PermissionManager.instance().checkLocationPermissions(player, event.getBlock().getLocation(),
                PermissionType.PLACE)) {

            event.setCancelled(true);
            player.sendMessage(Component.text("§cYou cannot place blocks here."));
            return;
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (PermissionManager.instance().hasGlobalOverrides(player))
            return;

        var block = event.getClickedBlock();
        if (block == null || block.isEmpty() || block.getType() == Material.AIR)
            return;

        var blockType = block.getType().toString();
        if (Settings.protectedContainers.contains(blockType)) {
            if (!PermissionManager.instance().checkLocationPermissions(player, block.getLocation(),
                    PermissionType.CONTAINER)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cYou cannot open containers here."));
                return;
            }
        }

        if (Settings.protectedUseBlocks.contains(blockType)) {
            if (!PermissionManager.instance().checkLocationPermissions(player, block.getLocation(),
                    PermissionType.BLOCK_USE)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cYou cannot use blocks here."));
                return;
            }
        }

        if (Settings.protectedSwitchBlocks.contains(blockType) || isSwitchMaterialTagProtected(block.getType())) {
            if (!PermissionManager.instance().checkLocationPermissions(player, block.getLocation(),
                    PermissionType.SWITCH)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cYou cannot switch here."));
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
        if (Settings.protectedInteractEntities.contains(entityType)) {
            if (!PermissionManager.instance().checkLocationPermissions(player, entity.getLocation(),
                    PermissionType.INTERACT)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cYou cannot interact here."));
                return;
            }
        }

    }

    private boolean isSwitchMaterialTagProtected(Material type) {
        for (var tag : Settings.protectedSwitchTags) {
            if (tag.isTagged(type))
                return true;
        }
        return false;
    }

}
