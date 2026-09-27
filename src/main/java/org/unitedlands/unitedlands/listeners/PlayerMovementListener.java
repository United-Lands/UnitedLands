package org.unitedlands.unitedlands.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.PlaytimeRecord;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig;
import org.unitedlands.unitedlands.classes.events.base.PlayerChangeChunkEvent;
import org.unitedlands.unitedlands.classes.events.cititen.CitizenCreatedEvent;
import org.unitedlands.unitedlands.classes.events.player.PlayerEnterRegionEvent;
import org.unitedlands.unitedlands.classes.events.player.PlayerEnterSettlementEvent;
import org.unitedlands.unitedlands.classes.events.player.PlayerExitRegionEvent;
import org.unitedlands.unitedlands.classes.events.player.PlayerExitSettlementEvent;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.unitedlands.managers.PlayerCacheManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

public class PlayerMovementListener implements Listener {

    private Map<UUID, Long> logons = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateCitizenRecord(player);
        updatePlayerLocation(player, new Location(player.getLocation().getWorld(), 0, 0, 0), player.getLocation());
        ChatChannelManager.instance().registerPlayer(player);
        logons.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        updatePlayerLocation(player, player.getLocation(), new Location(player.getLocation().getWorld(), 0, 0, 0));
        ChatChannelManager.instance().unregisterPlayer(player);
        updatePlaytime(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!updatePlayerLocation(player, event.getFrom(), event.getTo())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock())
            return;
        if (!updatePlayerLocation(event.getPlayer(), event.getFrom(), event.getTo()))
            event.setCancelled(true);
    }

    private void updatePlaytime(Player player) {
        var logon = logons.get(player.getUniqueId());
        if (logon == null) {
            United.logger().error("Could not retrieve logon time of player " + player.getUniqueId());
            return;
        }

        var playtime = System.currentTimeMillis() - logon;
        if (playtime < GeneralConfig.get().general().playtimeRecordThreshold() * 1000) {
            United.logger().debug("Playtime of player " + player.getUniqueId() + " below threshold, skiping.");
            return;
        }

        PlaytimeRecord record = new PlaytimeRecord();
        record.setUuid(UUID.randomUUID());
        record.setPlayerUuid(player.getUniqueId());
        record.setLogon(logon);
        record.setLogoff(System.currentTimeMillis());
        record.setPlaytime(playtime);
        UnitedLandsDataManager.instance().createPlaytimeRecordDbData(record);

        Citizen citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error("Could not retrieve citizen data of player " + player.getUniqueId());
            return;
        }
        citizen.setTotalPlaytime(citizen.getTotalPlaytime() + playtime);
        citizen.save();

        logons.remove(player.getUniqueId());
    }

    private void updateCitizenRecord(Player player) {
        Citizen citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            citizen = new Citizen(player);
            citizen.setJoined(System.currentTimeMillis());
            citizen.setName(player.getName());
            citizen.setPlayer(player);
            citizen.setLastLogon(System.currentTimeMillis());

            (new CitizenCreatedEvent(citizen)).callEvent();

            UnitedLandsDataManager.instance().createCitizenDbData(citizen);
        } else {
            citizen.setName(player.getName());
            citizen.setPlayer(player);
            citizen.setLastLogon(System.currentTimeMillis());
            citizen.save();
        }

    }

    private boolean updatePlayerLocation(Player player, Location from, Location to) {

        var fromChunkCoords = CoordinateUtils.locationToChunkCoordinates(from);
        var toChunkCoords = CoordinateUtils.locationToChunkCoordinates(to);

        if (!fromChunkCoords.equals(toChunkCoords)) {

            var playerCache = PlayerCacheManager.instance().getPlayerCache(player);

            // Settlement handling

            boolean enteredSettlement = false;
            boolean leftSettlement = false;
            Settlement lastSettlement = null;
            SettlementChunk settlementChunk = UnitedLandsDataManager.instance().getSettlementChunk(toChunkCoords);

            if (settlementChunk != null) {

                // The new chunk is in a settlement. See if the player was already in this
                // settlement before, or if he just entered.

                if (playerCache.getCachedSettlement() != null) {
                    // The player was in a settlement before. See if he entered a new one (if the
                    // settlements are bordering each other)
                    if (!settlementChunk.getSettlement().equals(playerCache.getCachedSettlement())) {
                        // The player left one settlement and entered a new one.
                        enteredSettlement = true;
                        leftSettlement = true;
                        lastSettlement = playerCache.getCachedSettlement();
                    }
                } else {
                    // The player entered from the wilderness.
                    enteredSettlement = true;
                }
                playerCache.updateChunkCache(toChunkCoords, settlementChunk);
            } else {
                // Entered the wilderness
                if (playerCache.getCachedSettlementChunk() != null) {
                    // Entered wilderness from a settlement
                    leftSettlement = true;
                    lastSettlement = playerCache.getCachedSettlement();
                }
                playerCache.clearChunkCache();
            }

            if (enteredSettlement) {
                var enterSettlementEvent = new PlayerEnterSettlementEvent(to, settlementChunk.getSettlement(), player);
                enterSettlementEvent.callEvent();
                if (enterSettlementEvent.isCancelled())
                    return false;
            }
            if (leftSettlement) {
                var exitSettlementEvent = new PlayerExitSettlementEvent(lastSettlement, player);
                exitSettlementEvent.callEvent();
                if (exitSettlementEvent.isCancelled())
                    return false;
            }

            // Region handling

            boolean enteredRegion = false;
            boolean leftRegion = false;
            Region lastRegion = null;

            var region = UnitedLandsDataManager.instance()
                    .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(to));
            if (region != null) {
                // Entered a valid region
                if (!region.equals(playerCache.getCachedRegion())) {
                    // New region is different from cached region
                    enteredRegion = true;
                    if (playerCache.getCachedRegion() != null) {
                        // Entered from a different region
                        leftRegion = true;
                        lastRegion = playerCache.getCachedRegion();
                    }
                    playerCache.updateRegionCache(region);
                }
            } else {
                // Entered a regionless zone
                if (playerCache.getCachedRegion() != null) {
                    // Entered regionless zone from a region
                    leftRegion = true;
                    lastRegion = playerCache.getCachedRegion();
                }
                playerCache.clearRegionCache();
            }

            if (enteredRegion) {

                var enterRegionEvent = new PlayerEnterRegionEvent(region, player);
                if (region.hasCountry())
                    enterRegionEvent.addAdditionalDisplay("<green>" + region.getCountry().getCleanName() + "</green>");
                enterRegionEvent.callEvent();
                if (enterRegionEvent.isCancelled())
                    return false;
            }
            if (leftRegion) {
                var exitRegionEvent = new PlayerExitRegionEvent(lastRegion, player);
                exitRegionEvent.callEvent();
                if (exitRegionEvent.isCancelled())
                    return false;
            }

            // General event

            var chunkChangeEvent = new PlayerChangeChunkEvent(player, from, to, fromChunkCoords, toChunkCoords);
            chunkChangeEvent.callEvent();
            if (chunkChangeEvent.isCancelled())
                return false;

            playerCache.calculateMemberships();
            return true;

        }

        return true;
    }

}
