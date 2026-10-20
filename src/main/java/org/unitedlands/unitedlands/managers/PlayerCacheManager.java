package org.unitedlands.unitedlands.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.PlayerCache;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.utils.CoordinateUtils;

public class PlayerCacheManager {

    private static PlayerCacheManager instance;

    public static PlayerCacheManager instance() {
        return instance;
    }

    private Map<UUID, PlayerCache> playerCache = new HashMap<>();

    public PlayerCacheManager() {
        instance = this;
    }

    public PlayerCache getPlayerCache(Player player) {
        return getPlayerCache(player.getUniqueId());
    }

    public PlayerCache getPlayerCache(Citizen citizen) {
        return getPlayerCache(citizen.getUuid());
    }

    public PlayerCache getPlayerCache(UUID playerUuid) {
        return playerCache.computeIfAbsent(playerUuid, k -> new PlayerCache(Bukkit.getPlayer(playerUuid)));
    }

    // Cache clearing (e.g. when a settlement is removed)

    public void clearOnlinePlayerCaches() {
        for (var player : Bukkit.getOnlinePlayers())
            clearCache(player);
    }

    public void clearPlayerCachesForSettlementChunk(SettlementChunk chunk) {
        var caches = new HashSet<>(
                playerCache.values().stream().filter(c -> chunk.equals(c.getCachedSettlementChunk())).toList());
        clearCaches(caches);
    }

    public void clearPlayerCachesForSettlement(Settlement settlement) {
        var caches = new HashSet<>(
                playerCache.values().stream().filter(c -> settlement.equals(c.getCachedSettlement())).toList());
        clearCaches(caches);
    }

    public void clearPlayerCachesForRegion(Region region) {
        var caches = new HashSet<>(
                playerCache.values().stream().filter(c -> region.equals(c.getCachedRegion())).toList());
        clearCaches(caches);
    }

    public void clearCaches(Set<PlayerCache> caches) {
        for (var cache : caches) {
            cache.clearSettlementChunkCache();
            cache.clearRegionCache();
        }
    }

    public void clearCache(Player player) {
        var playerCache = getPlayerCache(player.getUniqueId());
        playerCache.clearSettlementChunkCache();
        playerCache.clearRegionCache();
    }

    // Cache refreshing (e.g. when permissions change)

    public void refreshMembershipsForSettlementChunk(SettlementChunk chunk) {
        var caches = new HashSet<>(
                playerCache.values().stream().filter(c -> chunk.equals(c.getCachedSettlementChunk())).toList());
        refreshMemberships(caches);
    }

    public void refreshMembershipsForSettlement(Settlement settlement) {
        var caches = new HashSet<>(
                playerCache.values().stream().filter(c -> settlement.equals(c.getCachedSettlement())).toList());
        refreshMemberships(caches);
    }

    public void refreshMembershipsForRegion(Region region) {
        var caches = new HashSet<>(
                playerCache.values().stream().filter(c -> region.equals(c.getCachedRegion())).toList());
        refreshMemberships(caches);
    }

    public void refreshMemberships(Set<PlayerCache> caches) {
        for (var cache : caches) {
            cache.calculateMemberships();
        }
    }

    // Cache rebuilding
    
    public void rebuildCache(Player player) {
        var playerCache = getPlayerCache(player.getUniqueId());

        var coords = CoordinateUtils.locationToChunkCoordinates(player.getLocation());
        var settlementChunk = UnitedLandsDataManager.instance().getSettlementChunk(coords);
        if (settlementChunk != null) {
            playerCache.updateSettlementChunkCache(coords, settlementChunk);
        }
        
        var region = UnitedLandsDataManager.instance().getRegion(coords);
        if (region != null) {
            playerCache.updateRegionCache(region);
        }
        
        playerCache.calculateMemberships();
    }

}
