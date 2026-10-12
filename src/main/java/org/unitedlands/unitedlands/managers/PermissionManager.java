package org.unitedlands.unitedlands.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.LocationMembership;
import org.unitedlands.unitedlands.classes.PermissionHolder;
import org.unitedlands.unitedlands.classes.PermissionType;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.events.base.PermissablePlayerActionEvent;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

public class PermissionManager {

    private static PermissionManager instance;

    public static PermissionManager instance() {
        return instance;
    }

    private final UnitedLands plugin;

    public PermissionManager(UnitedLands plugin) {
        this.plugin = plugin;
        instance = this;

        reloadRankPermissions();
    }

    List<String> settlementRanks = new ArrayList<>();
    List<String> countryRanks = new ArrayList<>();

    Map<String, Set<String>> rankPermissions = new HashMap<>();

    // *************************************************************
    // Global admin overrides
    // *************************************************************

    public boolean hasGlobalOverrides(Player player) {
        if (player.hasPermission("united.lands.admin"))
            return true;
        return false;
    }

    // *************************************************************
    // Permissions related to settlement, region and country ranks
    // *************************************************************

    public void reloadRankPermissions() {

        rankPermissions = new HashMap<>();

        var config = plugin.getPermissionConfig().get();

        var settlementRankSection = config.getConfigurationSection("settlement");
        for (var key : settlementRankSection.getKeys(false)) {
            settlementRanks.add(key);
            var perms = settlementRankSection.getStringList(key);
            for (String perm : perms) {
                var rankList = rankPermissions.computeIfAbsent(perm, k -> new HashSet<>());
                rankList.add(key);
            }
        }

        var countryRankSection = config.getConfigurationSection("country");
        for (var key : countryRankSection.getKeys(false)) {
            countryRanks.add(key);
            var perms = countryRankSection.getStringList(key);
            for (String perm : perms) {
                var rankList = rankPermissions.computeIfAbsent(perm, k -> new HashSet<>());
                rankList.add(key);
            }
        }

        United.logger().info("Permissions loaded.", "UnitedLands");
    }

    public boolean hasRankPermission(String permission, Citizen citizen) {
        var requiredRanks = new HashSet<>(rankPermissions.computeIfAbsent(permission, k -> new HashSet<>()));
        var citizenRanks = new HashSet<>();
        citizenRanks.addAll(citizen.getSettlementRanks());
        citizenRanks.addAll(citizen.getCountryRanks());
        citizenRanks.retainAll(requiredRanks);
        return citizenRanks.size() > 0;
    }

    public List<String> getSettlementRanks() {
        return settlementRanks;
    }

    public List<String> getCountryRanks() {
        return countryRanks;
    }

    // *************************************************************
    // Permissions related to a specific location (settlement chunk or region)
    // *************************************************************

    public boolean checkLocationPermissions(Player player, Location eventLocation, PermissionType type) {

        var chunkCoordinates = CoordinateUtils.locationToChunkCoordinates(eventLocation);
        var settlementChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoordinates);
        if (settlementChunk != null) {
            var playerCache = PlayerCacheManager.instance().getPlayerCache(player);
            boolean hasSettlementChunkPermission = false;
            if (settlementChunk.equals(playerCache.getCachedSettlementChunk())) {
                hasSettlementChunkPermission = hasLocationPermissions(settlementChunk, playerCache.getChunkMembership(), type);
            } else {
                var eventLocationMembership = calculateChunkMembership(settlementChunk, player);
                hasSettlementChunkPermission = hasLocationPermissions(settlementChunk, eventLocationMembership, type);
            }

            PermissablePlayerActionEvent event = new PermissablePlayerActionEvent(player, eventLocation, type);
            event.setCancelled(!hasSettlementChunkPermission);
            event.callEvent();

            return !event.isCancelled();
        } else {
            // var regionCoords =
            // CoordinateUtils.locationToRegionCoordinates(eventLocation);
            var region = UnitedLandsDataManager.instance().getRegion(CoordinateUtils.locationToChunkCenterCoordinates(eventLocation));

            if (region != null) {
                var playerCache = PlayerCacheManager.instance().getPlayerCache(player);
                if (region.equals(playerCache.getCachedRegion())) {
                    return hasLocationPermissions(region, playerCache.getRegionMembership(), type);
                } else {
                    var eventLocationMembership = calculateRegionMembership(region, player);
                    return hasLocationPermissions(region, eventLocationMembership, type);
                }
            }
        }
        return UnitedLandsConfig.get().general().protectUnclaimedLand();
    }

    public boolean hasLocationPermissions(PermissionHolder holder, int membership, PermissionType type) {
        return (getLocationPermissions(holder, type) & membership) != 0;
    }

    public int getLocationPermissions(PermissionHolder holder, PermissionType type) {
        switch (type) {
        case BREAK:
            return holder.getBreakPermissions();
        case PLACE:
            return holder.getPlacePermissions();
        case CONTAINER:
            return holder.getContainerPermissions();
        case SWITCH:
            return holder.getSwitchPermissions();
        case BLOCK_USE:
            return holder.getBlockUsePermissions();
        case INTERACT:
            return holder.getInteractPermissions();
        default:
            return 0;
        }
    }

    public int calculateChunkMembership(SettlementChunk settlementChunk, Player player) {

        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error("CRITICAL: Could not retrieve citizen data of player " + player.getName());
            return 0;
        }

        if (settlementChunk.hasOwner() && settlementChunk.getOwner().equals(citizen)) {
            return LocationMembership.OWNER;
        }

        if (settlementChunk.getTrustList().contains(citizen)) {
            return LocationMembership.TRUSTED;
        }

        var settlement = settlementChunk.getSettlement();

        // Settlement mayor
        if (settlement.getMayor() != null && settlement.getMayor().equals(citizen)) {
            return LocationMembership.OWNER;
        }

        // Settlement Trusted
        if (settlement.getTrustList().contains(citizen)) {
            return LocationMembership.TRUSTED;
        }

        // Settlement Resident
        if (settlement.getCitizens().contains(citizen)) {
            return LocationMembership.SETTLEMENT_RESIDENT;
        }

        // Region Resident
        if (settlement.hasRegion() && citizen.hasSettlement() && citizen.getSettlement().hasRegion()) {
            if (settlement.getRegion().equals(citizen.getSettlement().getRegion())) {
                return LocationMembership.REGION_RESIDENT;
            }
        }

        if (settlement.hasCountry() && citizen.hasSettlement() && citizen.getSettlement().hasCountry()) {
            if (settlement.getCountry().equals(citizen.getSettlement().getCountry())) {
                return LocationMembership.COUNTRY_RESIDENT;
            }
        }

        // TODO: Outlaw

        return LocationMembership.FOREIGNER;
    }

    public int calculateRegionMembership(Region region, Player player) {

        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error("CRITICAL: Could not retrieve citizen data of player " + player.getName());
            return 0;
        }

        if (citizen.equals(region.getAdministrator())) {
            return LocationMembership.OWNER;
        }

        // TODO: Trusted

        if (citizen.hasSettlement() && citizen.getSettlement().hasRegion()) {
            if (region.equals(citizen.getSettlement().getRegion())) {
                return LocationMembership.REGION_RESIDENT;
            }
        }

        if (region.hasCountry() && citizen.hasSettlement() && citizen.getSettlement().hasCountry()) {
            if (region.getCountry().equals(citizen.getSettlement().getCountry())) {
                return LocationMembership.COUNTRY_RESIDENT;
            }
        }

        // TODO: Outlaw

        return LocationMembership.FOREIGNER;
    }

}
