package org.unitedlands.unitedlands.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.LocationMembership;
import org.unitedlands.unitedlands.classes.PermissionHolder;
import org.unitedlands.unitedlands.classes.PermissionType;
import org.unitedlands.unitedlands.classes.PlotType;
import org.unitedlands.unitedlands.classes.PlotTypeBehavior;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.Country;
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

    public PermissionManager() {
        instance = this;
        loadSettings();
    }

    private List<String> settlementRanks = new ArrayList<>();
    private List<String> countryRanks = new ArrayList<>();

    private List<Tag<Material>> protectedSwitchTags = new ArrayList<>();

    private Map<String, Set<String>> rankPermissions = new HashMap<>();

    private Map<String, PlotType> plotTypes = new HashMap<>();

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

    public void loadSettings() {

        rankPermissions = new HashMap<>();
        settlementRanks = new ArrayList<>();
        countryRanks = new ArrayList<>();
        var config = UnitedLands.instance().getPermissionConfig().get();

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

        protectedSwitchTags = new ArrayList<>();
        for (var strTag : UnitedLandsConfig.get().protection().switchTags()) {
            NamespacedKey key = NamespacedKey.minecraft(strTag);
            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
            if (tag != null) {
                protectedSwitchTags.add(tag);
            } else {
                United.logger().warning("Unknown block tag: " + strTag, "UnitedLands");
            }
        }

        var newPlotTypes = new HashMap<String, PlotType>();
        UnitedLandsConfig.get().settlementChunks().toMap()
                .forEach((name, settings) -> newPlotTypes.put(name, PlotType.fromSettings(name, settings)));
        plotTypes = newPlotTypes;

        United.logger().info("Permissions config loaded.", "UnitedLands");
    }

    public boolean hasAnyPermission(Player player, List<String> perms) {
        for (var perm : perms) {
            if (player.hasPermission(perm))
                return true;
        }

        return false;
    }

    public boolean hasRankPermission(String permission, Player player) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null)
            return false;
        return hasRankPermission(permission, citizen);
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
        return checkLocationPermissions(player, eventLocation, type, false, null, null);
    }

    public boolean checkLocationPermissions(Player player, Location eventLocation, PermissionType type,
            Material material) {
        return checkLocationPermissions(player, eventLocation, type, true, material, null);
    }

    public boolean checkLocationPermissions(Player player, Location eventLocation, PermissionType type,
            EntityType entityType) {
        return checkLocationPermissions(player, eventLocation, type, true, null, entityType);
    }

    private boolean checkLocationPermissions(Player player, Location eventLocation, PermissionType type,
            boolean applyRestrictions, Material material, EntityType entityType) {

        var chunkCoordinates = CoordinateUtils.locationToChunkCoordinates(eventLocation);
        var settlementChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoordinates);
        if (settlementChunk != null) {
            var playerCache = PlayerCacheManager.instance().getPlayerCache(player);
            int membership;
            if (settlementChunk.equals(playerCache.getCachedSettlementChunk())) {
                membership = playerCache.getChunkMembership();
            } else {
                membership = calculateChunkMembership(settlementChunk, player);
            }

            boolean hasSettlementChunkPermission = hasLocationPermissions(settlementChunk, membership, type);
            if (hasSettlementChunkPermission && applyRestrictions)
                hasSettlementChunkPermission = passesPlotTypeRestrictions(settlementChunk, membership, type, material,
                        entityType);

            PermissablePlayerActionEvent event = new PermissablePlayerActionEvent(player, eventLocation, type);
            event.setCancelled(!hasSettlementChunkPermission);
            event.callEvent();

            return !event.isCancelled();
        } else {
            // var regionCoords =
            // CoordinateUtils.locationToRegionCoordinates(eventLocation);
            var region = UnitedLandsDataManager.instance()
                    .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(eventLocation));

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

    private boolean passesPlotTypeRestrictions(SettlementChunk settlementChunk, int membership, PermissionType type,
            Material material, EntityType entityType) {
        var plotType = settlementChunk.getPlotType();
        if (plotType == null)
            return true;
        if (entityType != null)
            return plotType.allowsEntity(type, entityType, membership);
        return plotType.allowsMaterial(type, material, membership);
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

    // *************************************************************
    // Getters
    // *************************************************************

    public List<Tag<Material>> getProtectedSwitchTags() {
        return protectedSwitchTags;
    }

    public boolean hasPlotBehavior(Location location, PlotTypeBehavior behavior) {
        var chunk = UnitedLandsDataManager.instance()
                .getSettlementChunk(CoordinateUtils.locationToChunkCoordinates(location));
        return chunk != null && chunk.hasPlotBehavior(behavior);
    }

    public boolean isInBankPlot(Player player, Settlement settlement) {
        if (!UnitedLandsConfig.get().economy().requireBankPlot())
            return true;
        var chunk = UnitedLandsDataManager.instance()
                .getSettlementChunk(CoordinateUtils.locationToChunkCoordinates(player.getLocation()));
        return chunk != null && chunk.hasPlotBehavior(PlotTypeBehavior.BANK)
                && settlement.equals(chunk.getSettlement());
    }

    public boolean isInBankPlot(Player player, Country country) {
        if (!UnitedLandsConfig.get().economy().requireBankPlot())
            return true;
        var chunk = UnitedLandsDataManager.instance()
                .getSettlementChunk(CoordinateUtils.locationToChunkCoordinates(player.getLocation()));
        if (chunk == null || !chunk.hasPlotBehavior(PlotTypeBehavior.BANK))
            return false;
        var chunkSettlement = chunk.getSettlement();
        return chunkSettlement != null && chunkSettlement.hasCountry() && country.equals(chunkSettlement.getCountry());
    }

    public PlotType getPlotType(String name) {
        if (name == null)
            return null;
        return plotTypes.get(name);
    }

}
