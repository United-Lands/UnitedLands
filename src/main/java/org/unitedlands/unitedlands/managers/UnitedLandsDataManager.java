package org.unitedlands.unitedlands.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.BankRecord;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.PlaytimeRecord;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.RegionIndex;
import org.unitedlands.unitedlands.classes.Settings;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig;
import org.unitedlands.unitedlands.classes.events.country.CountryPreRemoveEvent;
import org.unitedlands.unitedlands.classes.events.country.CountryRemovedEvent;
import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

public class UnitedLandsDataManager {

    private static UnitedLandsDataManager instance;

    public static UnitedLandsDataManager instance() {
        return instance;
    }

    private final DatabaseManager databaseManager;

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    private Map<UUID, Citizen> citizens = new HashMap<>();
    private Map<UUID, Settlement> settlements = new HashMap<>();
    private Map<Coordinates, SettlementChunk> settlementChunks = new HashMap<>();
    private Map<UUID, Region> regions = new HashMap<>();
    private Map<UUID, Country> countries = new HashMap<>();

    private RegionIndex regionIndex;

    private LRUCache<Coordinates, Region> coordinateRegionCache = new LRUCache<>(5000);

    public UnitedLandsDataManager(UnitedLands plugin, Pl3xMapRenderer mapRenderer) {
        instance = this;

        databaseManager = new DatabaseManager();
        databaseManager.initialize();
    }

    public void loadDataFromDatabase() {

        CompletableFuture<List<Country>> countryFuture = databaseManager.getCountryService().getAllAsync();
        CompletableFuture<List<Region>> regionFuture = databaseManager.getRegionService().getAllAsync();
        CompletableFuture<List<Settlement>> settlementFuture = databaseManager.getSettlementService().getAllAsync();
        CompletableFuture<List<SettlementChunk>> settlementChunkFuture = databaseManager.getSettlementChunkService()
                .getAllAsync();
        CompletableFuture<List<Citizen>> citizenFuture = databaseManager.getCitizenService().getAllAsync();

        try {
            CompletableFuture.allOf(countryFuture, settlementFuture, settlementChunkFuture, regionFuture, citizenFuture)
                    .thenRun(() -> {

                        try {
                            buildCountries(countryFuture.get());
                            buildRegions(regionFuture.get());
                            buildSettlements(settlementFuture.get(), settlementChunkFuture.get());
                            buildCitizens(citizenFuture.get());
                        } catch (Exception ex) {
                            United.logger().error("GeopolObject building failed: " + ex.getMessage(), "UnitedLands");
                            throw new RuntimeException("App init failed", ex);
                        }

                    }).get();

            validateGeopolAttributes();

            Pl3xMapRenderer.instance().setDebugMode(false);
            Pl3xMapRenderer.instance().addSettlementsToRenderQueue(getSettlements());
            Pl3xMapRenderer.instance().addRegionsToRenderQueue(getRegions());
            Pl3xMapRenderer.instance().addCountriesToRenderQueue(getCountries());

        } catch (Exception ex) {
            United.logger().error("Initialization failed: " + ex.getMessage(), "UnitedLands");
            throw new RuntimeException("App init failed", ex);
        }

    }

    public void buildCitizens(List<Citizen> loadedCitizens) {
        for (Citizen citizen : loadedCitizens) {
            citizens.put(citizen.getUuid(), citizen);
        }
        United.logger().info("Loaded " + loadedCitizens.size() + " citizens to memory.", "UnitedLands");
    }

    public void buildSettlements(List<Settlement> loadedSettlements, List<SettlementChunk> loadedSettlementChunks) {
        for (var settlement : loadedSettlements) {
            settlements.put(settlement.getUuid(), settlement);

            if (settlement.hasRegion()) {
                settlement.getRegion().addSettlement(settlement);
            }
            if (settlement.hasCountry()) {
                settlement.getCountry().addSettlement(settlement);
            }
        }
        United.logger().info("Loaded " + loadedSettlements.size() + " settlements to memory.", "UnitedLands");

        for (var settlementChunk : loadedSettlementChunks) {
            settlementChunks.put(settlementChunk.getCoordinates(), settlementChunk);
            settlements.get(settlementChunk.getSettlementUuid()).addChunk(settlementChunk);
        }
        United.logger().info("Loaded " + loadedSettlementChunks.size() + " settlement chunks to memory.",
                "UnitedLands");
    }

    public void buildRegions(List<Region> loadedRegions) {
        for (var region : loadedRegions) {
            region.calculateBounds();
            regions.put(region.getUuid(), region);
            if (region.hasCountry()) {
                region.getCountry().addRegion(region);
            }
            if (region.getClaimEndTime() != null) {
                region.startClaimTask();
            }
        }

        buildRegionIndex();

        United.logger().info("Loaded " + regions.size() + " regions to memory.", "UnitedLands");
    }

    public void buildRegionIndex() {
        regionIndex = new RegionIndex();
        // TODO: Add to world bounds to settings

        var worldXMin = Settings.importOffsetX * -1d;
        var worldXMax = Settings.importOffsetX;
        var worldZMin = Settings.importOffsetY * -1d;
        var worldZMax = Settings.importOffsetY;

        United.logger().info(
                "Using world bounds " + worldXMin + " | " + worldZMin + " - " + worldXMax + " | " + worldZMax,
                "UnitedLands");

        regionIndex.build(regions.values(), worldXMin, worldZMin, worldXMax, worldZMax);
    }

    public void buildCountries(List<Country> loadedCountries) {
        for (var country : loadedCountries) {
            countries.put(country.getUuid(), country);
        }
        United.logger().info("Loaded " + countries.size() + " countries to memory.", "UnitedLands");
    }

    private void validateGeopolAttributes() {

        // Make sure all geopol objects have the correct attributes

        for (var country : countries.values()) {
            boolean changed = false;
            // Use this for attribute validation, e.g.:
            // if (country.getAttribute("DIPLOMACY") == null) {
            //     changed = true;
            //     country.addAttribute("DIPLOMACY", new GeopolAttribute(100, 0, 100, 0));
            // }
            if (changed)
                country.saveAttributes();
        }

    }

    public void clearData() {
        settlements = new HashMap<>();
        settlementChunks = new HashMap<>();
        regions = new HashMap<>();
        countries = new HashMap<>();
    }

    // **************************************************
    // Citizens
    // **************************************************

    // Database operations

    public void createCitizenDbData(Citizen citizen) {
        databaseManager.getCitizenService().createAsync(citizen);
        registerCitizen(citizen);
    }

    public void updateCitizenDbData(Citizen citizen) {
        databaseManager.getCitizenService().updateAsync(citizen);
    }

    public void removeCitizenDbData(Citizen citizen) {
        databaseManager.getCitizenService().deleteAsync(citizen);
        unregisterCitizen(citizen);
    }

    // Cache operations

    public Citizen getCitizen(Player player) {
        return citizens.get(player.getUniqueId());
    }

    public Citizen getCitizen(UUID uuid) {
        return citizens.get(uuid);
    }

    public Citizen getCitizen(String name) {
        CompletableFuture<Citizen> future = CompletableFuture.supplyAsync(() -> {
            return citizens.values().stream().filter(c -> name.equalsIgnoreCase(c.getName())).findFirst().orElse(null);
        });
        return future.join();
    }

    public List<String> getCitizenNames() {
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            return citizens.values().stream().map(Citizen::getName).collect(Collectors.toList());
        });
        return future.join();
    }

    public void registerCitizen(Citizen citizen) {
        citizens.put(citizen.getUuid(), citizen);
    }

    public void unregisterCitizen(Citizen citizen) {
        citizens.remove(citizen.getUuid(), citizen);
    }

    // **************************************************
    // Settlements
    // **************************************************

    // Database operations

    public void createSettlementDbData(Settlement settlement) {
        databaseManager.getSettlementChunkService().createAllAsync(settlement.getChunks());
        databaseManager.getSettlementService().createAsync(settlement);
        registerSettlement(settlement);

        Pl3xMapRenderer.instance().addToRenderQueue(settlement);
    }

    public void updateSettlementDbData(Settlement settlement, boolean render) {
        databaseManager.getSettlementService().updateAsync(settlement);

        if (render)
            Pl3xMapRenderer.instance().addToRenderQueue(settlement);
    }

    public void removeSettlementDbData(Settlement settlement) {
        databaseManager.getSettlementChunkService().deleteAllAsync(settlement.getChunks());
        databaseManager.getSettlementService().deleteAsync(settlement);

        unregisterSettlement(settlement);

        Pl3xMapRenderer.instance().removeSettlement(settlement);
    }

    // Cache operations

    public void registerSettlement(Settlement settlement) {
        for (var chunk : settlement.getChunks())
            registerSettlementChunk(chunk);
        settlements.put(settlement.getUuid(), settlement);
    }

    public void unregisterSettlement(Settlement settlement) {
        for (var chunk : settlement.getChunks())
            unregisterSettlementChunk(chunk);
        settlements.remove(settlement.getUuid());
    }

    public Collection<Settlement> getSettlements() {
        return settlements.values();
    }

    public Settlement getSettlement(Location location) {
        var settlementChunk = settlementChunks.get(CoordinateUtils.locationToChunkCoordinates(location));
        if (settlementChunk != null)
            return settlementChunk.getSettlement();

        return null;
    }

    public Settlement getSettlement(Coordinates settlementCoordinates) {
        var settlementChunk = settlementChunks.get(settlementCoordinates);
        if (settlementChunk != null)
            return settlementChunk.getSettlement();

        return null;
    }

    public Settlement getSettlement(String name) {
        return settlements.values().stream().filter(r -> r.getName().equals(name)).findFirst().orElse(null);
    }

    public Settlement getSettlement(UUID settlementId) {
        return settlements.get(settlementId);
    }

    public List<String> getSettlementNames() {
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            return settlements.values().stream().map(Settlement::getName).collect(Collectors.toList());
        });
        return future.join();
    }

    // **************************************************
    // Settlement Chunks
    // **************************************************

    // Database operations

    public void createSettlementChunkDbData(SettlementChunk settlementChunk) {
        databaseManager.getSettlementChunkService().createAsync(settlementChunk);
        registerSettlementChunk(settlementChunk);

        Pl3xMapRenderer.instance().addToRenderQueue(settlementChunk.getSettlement());
    }

    public void updateSettlementChunkDbData(SettlementChunk settlementChunk) {
        databaseManager.getSettlementChunkService().updateAsync(settlementChunk);
    }

    public void removeSettlementChunkDbData(SettlementChunk settlementChunk) {
        databaseManager.getSettlementChunkService().deleteAsync(settlementChunk);
        unregisterSettlementChunk(settlementChunk);

        Pl3xMapRenderer.instance().addToRenderQueue(settlementChunk.getSettlement());
    }

    // Cache operations

    public void registerSettlementChunk(SettlementChunk settlementChunk) {
        settlementChunks.put(settlementChunk.getCoordinates(), settlementChunk);
    }

    public void unregisterSettlementChunk(SettlementChunk settlementChunk) {
        settlementChunks.remove(settlementChunk.getCoordinates());
    }

    public SettlementChunk getSettlementChunk(Coordinates regionCoordinates) {
        return settlementChunks.get(regionCoordinates);
    }

    // **************************************************
    // Regions
    // **************************************************

    // Database operations

    public void createRegionDbData(Region region) {
        databaseManager.getRegionService().createAsync(region);
        registerRegion(region);
        queueRegionRender(region);
    }

    public void updateRegionDbData(Region region, boolean render) {
        databaseManager.getRegionService().updateAsync(region);
        if (render)
            queueRegionRender(region);
    }

    public void removeRegionDbData(Region region) {
        databaseManager.getRegionService().deleteAsync(region);
        unregisterRegion(region);
        Pl3xMapRenderer.instance().removeRegion(region);
    }

    private void queueRegionRender(Region region) {
        for (var settlement : region.getSettlements()) {
            Pl3xMapRenderer.instance().addToRenderQueue(settlement);
        }
        Pl3xMapRenderer.instance().addToRenderQueue(region);
    }

    // Cache operations

    public void registerRegion(Region region) {
        regions.put(region.getUuid(), region);
    }

    public void unregisterRegion(Region region) {
        regions.remove(region.getUuid());
    }

    public Collection<Region> getRegions() {
        return regions.values();
    }

    public Region getRegion(Coordinates coordinates) {
        if (coordinateRegionCache.containsKey(coordinates)) {
            return coordinateRegionCache.get(coordinates);
        }

        if (!GeneralConfig.get().general().worlds().contains(coordinates.getWorldName()))
            return null;

        var region = regionIndex.findRegion(coordinates.getX(), coordinates.getZ());
        coordinateRegionCache.put(coordinates, region);
        return region;
    }

    public Region getRegion(String name) {
        return regions.values().stream().filter(r -> r.getName().equals(name)).findFirst().orElse(null);
    }

    public Region getRegion(UUID regionId) {
        return regions.get(regionId);
    }

    public List<String> getRegionNames() {
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            return regions.values().stream().map(Region::getName).collect(Collectors.toList());
        });
        return future.join();
    }

    public Set<Region> getRegionClaimsOngoing(Country country) {
        return regions.values().stream().filter(r -> country.equals(r.getClaimantCountry()))
                .collect(Collectors.toSet());
    }

    // **************************************************
    // Countries
    // **************************************************

    public void removeCountry(Country country) {

        (new CountryPreRemoveEvent(country)).callEvent();

        for (var settlement : country.getSettlements()) {
            for (var settlementCitizen : settlement.getCitizens()) {
                settlementCitizen.removeCountryRanks();
                settlementCitizen.save();
            }
            settlement.removeCountry();
            settlement.saveAndRender();
        }
        for (var region : country.getRegions()) {

            region.removeCountry();
            region.saveAndRender();
        }

        UnitedLandsEconomyManager.instance().deleteAccount(country.getUuid());
        UnitedLandsDataManager.instance().removeCountryDbData(country);

        United.messenger().broadcast("general-broadcasts.country-removed", country.getCleanName());

        (new CountryRemovedEvent(country)).callEvent();
    }

    // Database operations

    public void createCountryDbData(Country country) {
        databaseManager.getCountryService().createAsync(country);
        registerCountry(country);
        queueCountryRender(country);
    }

    public void updateCountryDbData(Country country, boolean render) {
        databaseManager.getCountryService().updateAsync(country);
        if (render)
            queueCountryRender(country);
    }

    private void queueCountryRender(Country country) {
        for (var region : country.getRegions()) {
            for (var settlement : region.getSettlements()) {
                Pl3xMapRenderer.instance().addToRenderQueue(settlement);
            }
            Pl3xMapRenderer.instance().addToRenderQueue(region);
        }
        Pl3xMapRenderer.instance().addToRenderQueue(country);
        if (country.getOverlord() != null) {
            queueCountryRender(country.getOverlord());
        }
    }

    public void removeCountryDbData(Country country) {
        databaseManager.getCountryService().deleteAsync(country);
        unregisterCountry(country);
        Pl3xMapRenderer.instance().removeCountry(country);
    }

    // Cache operations

    public void registerCountry(Country country) {
        countries.put(country.getUuid(), country);
    }

    public void unregisterCountry(Country country) {
        countries.remove(country.getUuid());
    }

    public Collection<Country> getCountries() {
        return countries.values();
    }

    public Collection<Country> getCountryVassals(Country country) {
        return countries.values().stream().filter(r -> country.equals(r.getOverlord())).collect(Collectors.toList());
    }

    public Country getCountry(String name) {
        return countries.values().stream().filter(r -> r.getName().equals(name)).findFirst().orElse(null);
    }

    public Country getCountry(UUID countryId) {
        return countries.get(countryId);
    }

    public List<String> getCountryNames() {
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            return countries.values().stream().map(Country::getName).collect(Collectors.toList());
        });
        return future.join();
    }

    public Set<Citizen> getCountryCitizens(Country country) {
        CompletableFuture<Set<Citizen>> future = CompletableFuture
                .supplyAsync(() -> settlements.values().stream().filter(s -> country.equals(s.getCountry()))
                        .flatMap(s -> s.getCitizens().stream()).collect(Collectors.toSet()));
        return future.join();
    }

    // **************************************************
    // Bank Records
    // **************************************************

    public void createBankRecordDbData(BankRecord record) {
        databaseManager.getBankRecordService().createAsync(record);
    }

    public List<BankRecord> getBankRecords(UUID objectId, long page, int pageSize) {
        try {
            return databaseManager.getBankRecordService().getRecordsAsync(objectId, page, pageSize).get();
        } catch (Exception ex) {
            United.logger().error("Could not retrieve bank records for object " + objectId, "UnitedLands");
            return new ArrayList<>();
        }
    }

    // **************************************************
    // Playtime Records
    // **************************************************

    public void createPlaytimeRecordDbData(PlaytimeRecord record) {
        databaseManager.getPlaytimeRecordService().createAsync(record);
    }

    public List<PlaytimeRecord> getRecentPlaytimeRecords(UUID playerId, int days) {
        try {
            return databaseManager.getPlaytimeRecordService().getForLastDaysAsync(playerId, days).get();
        } catch (Exception ex) {
            United.logger().error("Could not retrieve playtime records for player " + playerId, "UnitedLands");
            return new ArrayList<>();
        }
    }

    // **************************************************
    // Helper classes
    // **************************************************

    public static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public LRUCache(int capacity) {
            super(capacity, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

}
