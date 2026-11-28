package org.unitedlands.unitedlands.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.BankRecord;
import org.unitedlands.unitedlands.classes.BankRecordTransactionType;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolAttribute;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.PlaytimeRecord;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.RegionIndex;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.ULCINCRecord;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.events.DataLoadedEvent;
import org.unitedlands.unitedlands.classes.events.citizen.CitizenUpdatedEvent;
import org.unitedlands.unitedlands.classes.events.country.CountryCreatedEvent;
import org.unitedlands.unitedlands.classes.events.country.CountryPreRemoveEvent;
import org.unitedlands.unitedlands.classes.events.country.CountryRemovedEvent;
import org.unitedlands.unitedlands.classes.events.country.CountryUpdatedEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionCreatedEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionRemovedEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionUpdatedEvent;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementCreatedEvent;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementRemovedEvent;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementUpdatedEvent;
import org.unitedlands.unitedlands.classes.events.settlementchunk.SettlementChunkCreatedEvent;
import org.unitedlands.unitedlands.classes.events.settlementchunk.SettlementChunkRemovedEvent;
import org.unitedlands.unitedlands.classes.events.settlementchunk.SettlementChunkUpdatedEvent;
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

    public UnitedLandsDataManager() {
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

            validateGeopolProperties();

            new DataLoadedEvent().callEvent();

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

        var worldXMin = UnitedLandsConfig.get().importSetting().offsetX() * -1d;
        var worldXMax = UnitedLandsConfig.get().importSetting().offsetX();
        var worldZMin = UnitedLandsConfig.get().importSetting().offsetY() * -1d;
        var worldZMax = UnitedLandsConfig.get().importSetting().offsetY();

        United.logger().info(
                "Using world bounds " + worldXMin + " | " + worldZMin + " - " + worldXMax + " | " + worldZMax,
                "UnitedLands");

        regionIndex.build(regions.values(), worldXMin, worldZMin, worldXMax, worldZMax);
        coordinateRegionCache.clear();
    }

    public void buildCountries(List<Country> loadedCountries) {
        for (var country : loadedCountries) {
            countries.put(country.getUuid(), country);
        }
        United.logger().info("Loaded " + countries.size() + " countries to memory.", "UnitedLands");
    }

    private void validateGeopolProperties() {

        // Make sure all geopol objects have the correct attributes and properties

        for (var settlement : settlements.values()) {
            boolean changed = false;

            for (var key : UnitedLandsConfig.get().geopolAttributeDefaults().settlement().keys()) {
                if (!settlement.hasAttribute(key)) {
                    United.logger().info(
                            "Settlement " + settlement.getName() + " is missing attribute " + key + ", adding...");
                    changed = true;
                    settlement.addAttribute(key, new GeopolAttribute(
                            UnitedLandsConfig.get().geopolAttributeDefaults().settlement().get(key)));
                }
            }
            if (changed)
                settlement.save();
        }

        for (var country : countries.values()) {
            boolean changed = false;
            for (var key : UnitedLandsConfig.get().geopolAttributeDefaults().country().keys()) {
                if (!country.hasAttribute(key)) {
                    United.logger()
                            .info("Country " + country.getName() + " is missing attribute " + key + ", adding...");
                    changed = true;
                    country.addAttribute(key,
                            new GeopolAttribute(UnitedLandsConfig.get().geopolAttributeDefaults().country().get(key)));
                }
            }
            if (country.getSpawn() == null) {
                var capital = country.getCapital();
                if (capital != null && capital.getSpawn() != null) {
                    United.logger().info("Country " + country.getName() + " has no spawn, using capital spawn...");
                    country.setSpawn(capital.getSpawn());
                    changed = true;
                } else {
                    United.logger().warning("Country " + country.getName()
                            + " has no spawn and no capital spawn to fall back to.");
                }
            }
            if (changed)
                country.save();
        }

    }

    public void clearData() {
        settlements = new HashMap<>();
        settlementChunks = new HashMap<>();
        regions = new HashMap<>();
        countries = new HashMap<>();
    }

    //#region Generic

    // **************************************************
    // Generic
    // **************************************************

    public GeopolObject getGeopolObject(UUID uuid) {
        if (countries.containsKey(uuid))
            return getCountry(uuid);
        if (regions.containsKey(uuid))
            return getRegion(uuid);
        if (settlements.containsKey(uuid))
            return getSettlement(uuid);
        return null;
    }

    
    public GeopolObject getGeopolObject(String name) {
        var country = countries.values().stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
        if (country != null)
            return country;
        var region = regions.values().stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
        if (region != null)
            return region;
        var settlement = settlements.values().stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
        if (settlement != null)
            return settlement;
        return null;
    }



    //#endregion

    // #region Citizens

    // **************************************************
    // Citizens
    // **************************************************

    // Database operations

    public void createCitizenDbData(Citizen citizen) {
        databaseManager.getCitizenService().createAsync(citizen);
        registerCitizen(citizen);
    }

    public void saveCitizen(Citizen citizen) {
        databaseManager.getCitizenService().updateAsync(citizen);
        new CitizenUpdatedEvent(citizen).callEvent();
    }

    public void updateCitizenDbData(Collection<Citizen> citizens) {
        databaseManager.getCitizenService().updateAllAsync(citizens);
    }

    public void removeCitizenDbData(Citizen citizen) {
        databaseManager.getCitizenService().deleteAsync(citizen);
        unregisterCitizen(citizen);
    }

    // Cache operations

    public Collection<Citizen> getCitizens() {
        return citizens.values();
    }

    public Citizen getCitizen(Player player) {
        return citizens.get(player.getUniqueId());
    }

    public Citizen getCitizen(UUID uuid) {
        return citizens.get(uuid);
    }

    public Citizen getCitizen(String name) {
        return citizens.values().stream().filter(c -> name.equalsIgnoreCase(c.getName())).findFirst().orElse(null);
    }

    public List<String> getCitizenNames() {
        return citizens.values().stream().map(Citizen::getName).collect(Collectors.toList());
    }

    public void registerCitizen(Citizen citizen) {
        citizens.put(citizen.getUuid(), citizen);
    }

    public void unregisterCitizen(Citizen citizen) {
        citizens.remove(citizen.getUuid(), citizen);
    }

    // #endregion

    // #region Settlements

    // **************************************************
    // Settlements
    // **************************************************

    public Settlement createSettlement(String name, Coordinates coords, Location spawn, Region region, Citizen mayor) {

        Settlement settlement = new Settlement();
        settlement.setUuid(UUID.randomUUID());
        settlement.setName(name);
        settlement.setFoundingTimestamp(System.currentTimeMillis());
        settlement.setWorld(coords.getWorld());
        settlement.setHomeChunkCoordinates(coords);
        settlement.setSpawn(spawn);
        settlement.setVisitorSpawn(spawn);

        if (region != null) {

            settlement.setRegion(region);
            region.addSettlement(settlement);

            if (region.hasCountry()) {
                var country = region.getCountry();
                settlement.setCountry(country);
                country.addSettlement(settlement);
            }
        }

        var chunk = new SettlementChunk();
        chunk.setUuid(UUID.randomUUID());
        chunk.setCoordinates(coords);
        chunk.setWorld(coords.getWorld());
        chunk.setClaimTimestamp(System.currentTimeMillis());
        chunk.setSettlement(settlement);

        settlement.addChunk(chunk);

        // Mayor may be null if settlement is created via Admin command

        if (mayor != null) {

            settlement.addCitizen(mayor);
            settlement.setFounder(mayor.getOfflinePlayer());

            mayor.setSettlement(settlement);
            mayor.addSettlementRank("mayor");
            mayor.save();

            UnitedLandsEconomyManager.instance().withdraw(mayor.getUuid(),
                    UnitedLandsConfig.get().economy().costs().settlement().createCosts(),
                    BankRecordTransactionType.CREATION,
                    "Settlement creation payment");
        }

        // Add default attributes
        for (var key : UnitedLandsConfig.get().geopolAttributeDefaults().settlement().keys()) {
            settlement.addAttribute(key,
                    new GeopolAttribute(UnitedLandsConfig.get().geopolAttributeDefaults().settlement().get(key)));
        }

        UnitedLandsEconomyManager.instance().createAccount(settlement.getUuid(), settlement.getName());

        UnitedLandsDataManager.instance().createSettlementDbData(settlement);

        return settlement;
    }

    public void mergeSettlements(Settlement prevailing, Settlement merging) {

        if (prevailing.equals(merging))
            return;

        PlayerCacheManager.instance().clearPlayerCachesForSettlement(merging);

        for (var chunk : new HashSet<>(merging.getChunks())) {
            merging.removeChunk(chunk);
            chunk.setSettlement(prevailing);
            prevailing.addChunk(chunk);
            chunk.save();
        }

        boolean sameCountry = Objects.equals(prevailing.getCountryUuid(), merging.getCountryUuid());
        for (var citizen : new HashSet<>(merging.getCitizens())) {
            citizen.setSettlement(prevailing);
            citizen.removeSettlementRanks();
            if (!sameCountry)
                citizen.removeCountryRanks();
            citizen.save();
            prevailing.addCitizen(citizen);
        }
        merging.setCitizens(new HashSet<>());

        UnitedLandsEconomyManager.instance().deposit(prevailing.getUuid(),
                UnitedLandsEconomyManager.instance().getBalance(merging.getUuid()),
                BankRecordTransactionType.SYSTEM,
                "Merging of settlement bank of " + merging.getName());

        removeSettlement(merging);

        prevailing.saveAndRender();
    }

    public void removeSettlement(Settlement settlement) {

        if (settlement.hasRegion()) {
            var region = settlement.getRegion();
            region.removeSettlement(settlement);
        }

        if (settlement.hasCountry()) {
            var country = settlement.getCountry();
            country.removeSettlement(settlement);
        }

        for (var settlementCitizen : settlement.getCitizens()) {
            settlementCitizen.removeSettlement();
            settlementCitizen.removeSettlementRanks();
            settlementCitizen.removeCountryRanks();
            settlementCitizen.save();

            if (ChatChannelManager.instance().isInChannel(settlementCitizen.getPlayer(), "SETTLEMENT"))
                ChatChannelManager.instance().switchChannel(settlementCitizen.getPlayer(), "GLOBAL", null);
        }

        UnitedLandsEconomyManager.instance().deleteAccount(settlement.getUuid());
        UnitedLandsDataManager.instance().removeSettlementDbData(settlement);
    }

    // Database operations

    public void createSettlementDbData(Settlement settlement) {
        databaseManager.getSettlementService().createAsync(settlement);
        databaseManager.getSettlementChunkService().createAllAsync(settlement.getChunks());
        registerSettlement(settlement);
        new SettlementCreatedEvent(settlement).callEvent();
    }

    public CompletableFuture<Void> createSettlementDbDataAsync(Settlement settlement) {
        var settlementFuture = databaseManager.getSettlementService().createAsync(settlement);
        var settlementChunkFuture = databaseManager.getSettlementChunkService().createAllAsync(settlement.getChunks());
        return CompletableFuture.allOf(settlementFuture, settlementChunkFuture);
    }

    public void saveSettlement(Settlement settlement, boolean visualChange) {
        databaseManager.getSettlementService().updateAsync(settlement);
        new SettlementUpdatedEvent(settlement, visualChange).callEvent();
    }

    public void removeSettlementDbData(Settlement settlement) {
        databaseManager.getSettlementChunkService().deleteAllAsync(settlement.getChunks());
        databaseManager.getSettlementService().deleteAsync(settlement);

        unregisterSettlement(settlement);

        new SettlementRemovedEvent(settlement).callEvent();
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

    public Settlement getSettlement(Player player) {
        var citizen = getCitizen(player);
        if (citizen != null)
            return citizen.getSettlement();

        return null;
    }

    public List<String> getSettlementNames() {
        return settlements.values().stream().map(Settlement::getName).collect(Collectors.toList());
    }

    // #endregion

    // #region Settlement Chunks

    // **************************************************
    // Settlement Chunks
    // **************************************************

    // Database operations

    public void createSettlementChunkDbData(SettlementChunk settlementChunk) {
        databaseManager.getSettlementChunkService().createAsync(settlementChunk);
        registerSettlementChunk(settlementChunk);

        new SettlementChunkCreatedEvent(settlementChunk.getSettlement(), settlementChunk).callEvent();
    }

    public void saveSettlementChunk(SettlementChunk settlementChunk) {
        databaseManager.getSettlementChunkService().updateAsync(settlementChunk);
        new SettlementChunkUpdatedEvent(settlementChunk.getSettlement(), settlementChunk).callEvent();
    }

    public void removeSettlementChunkDbData(SettlementChunk settlementChunk) {
        databaseManager.getSettlementChunkService().deleteAsync(settlementChunk);
        unregisterSettlementChunk(settlementChunk);

        new SettlementChunkRemovedEvent(settlementChunk.getSettlement(), settlementChunk).callEvent();
    }

    // Cache operations

    public void registerSettlementChunk(SettlementChunk settlementChunk) {
        settlementChunks.put(settlementChunk.getCoordinates(), settlementChunk);
    }

    public void unregisterSettlementChunk(SettlementChunk settlementChunk) {
        settlementChunks.remove(settlementChunk.getCoordinates());
    }

    public SettlementChunk getSettlementChunk(Coordinates coords) {
        return settlementChunks.get(coords);
    }

    public SettlementChunk getSettlementChunk(Location location) {
        return settlementChunks.get(CoordinateUtils.locationToChunkCoordinates(location));
    }

    // #endregion

    // #region Regions

    // **************************************************
    // Regions
    // **************************************************

    // Database operations

    public void createRegionDbData(Region region) {
        databaseManager.getRegionService().createAsync(region);
        registerRegion(region);
        new RegionCreatedEvent(region).callEvent();
    }

    public void saveRegion(Region region, boolean visualChange) {
        databaseManager.getRegionService().updateAsync(region);
        new RegionUpdatedEvent(region, visualChange).callEvent();
    }

    public void removeRegionDbData(Region region) {
        databaseManager.getRegionService().deleteAsync(region);
        unregisterRegion(region);
        buildRegionIndex();
        new RegionRemovedEvent(region).callEvent();
    }

    // Cache operations

    public void registerRegion(Region region) {
        regions.put(region.getUuid(), region);
        coordinateRegionCache.clear();
    }

    public void unregisterRegion(Region region) {
        regions.remove(region.getUuid());
        coordinateRegionCache.clear();
    }

    public Collection<Region> getRegions() {
        return regions.values();
    }

    public Region getRegion(Location location) {
        return getRegion(CoordinateUtils.locationToChunkCenterCoordinates(location));
    }

    public Region getRegion(Coordinates coordinates) {
        if (coordinateRegionCache.containsKey(coordinates)) {
            return coordinateRegionCache.get(coordinates);
        }

        if (!UnitedLandsConfig.get().general().worlds().contains(coordinates.getWorldName()))
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
        return regions.values().stream().map(Region::getName).collect(Collectors.toList());
    }

    public Set<Region> getRegionClaimsOngoing(Country country) {
        return regions.values().stream().filter(r -> country.equals(r.getClaimantCountry()))
                .collect(Collectors.toSet());
    }

    // #endregion

    // #region Countries

    // **************************************************
    // Countries
    // **************************************************

    public Country createCountry(String name, Region region, Settlement capital, Citizen leader) {

        Country country = new Country();
        country.setUuid(UUID.randomUUID());
        country.setName(name);
        country.setWorld(region.getWorld());
        country.setCapital(capital);
        country.setFoundingTimestamp(System.currentTimeMillis());
        country.setSpawn(capital.getSpawn());
        country.setStrokeColor(UnitedLandsConfig.get().mapRendering().get("country-default").stroke());
        country.setFillColor(UnitedLandsConfig.get().mapRendering().get("country-default").fill());

        // Leader may be null if country is created via Admin command
        if (leader != null) {

            country.setFounder(leader.getOfflinePlayer());
            region.setAdministrator(leader);

            leader.addCountryRank("leader");
            leader.save();

            UnitedLandsEconomyManager.instance().withdraw(leader.getUuid(),
                    UnitedLandsConfig.get().economy().costs().country().createCosts(),
                    BankRecordTransactionType.CREATION,
                    "Country creation costs");

        }

        country.addSettlement(capital);
        country.addRegion(region);

        region.setCountry(country);
        region.saveAndRender();

        capital.setCountry(country);
        capital.saveAndRender();

        // Add default attributes
        for (var key : UnitedLandsConfig.get().geopolAttributeDefaults().country().keys()) {
            country.addAttribute(key,
                    new GeopolAttribute(UnitedLandsConfig.get().geopolAttributeDefaults().country().get(key)));
        }

        UnitedLandsEconomyManager.instance().createAccount(country.getUuid(), country.getName());

        UnitedLandsDataManager.instance().createCountryDbData(country);

        return country;
    }

    public void removeCountry(Country country) {

        (new CountryPreRemoveEvent(country)).callEvent();

        for (var settlement : country.getSettlements()) {
            for (var settlementCitizen : settlement.getCitizens()) {
                settlementCitizen.removeCountryRanks();
                settlementCitizen.save();

                if (ChatChannelManager.instance().isInChannel(settlementCitizen.getPlayer(), "COUNTRY"))
                    ChatChannelManager.instance().switchChannel(settlementCitizen.getPlayer(), "GLOBAL", null);
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
    }

    // Database operations

    public CompletableFuture<Boolean> createCountryDbDataAsync(Country country) {
        return databaseManager.getCountryService().createAsync(country);
    }


    public void createCountryDbData(Country country) {
        databaseManager.getCountryService().createAsync(country);
        registerCountry(country);
        new CountryCreatedEvent(country).callEvent();
    }

    public void saveCountry(Country country, boolean visualChange) {
        databaseManager.getCountryService().updateAsync(country);
        new CountryUpdatedEvent(country, visualChange).callEvent();
    }

    public void removeCountryDbData(Country country) {
        databaseManager.getCountryService().deleteAsync(country);
        unregisterCountry(country);
        new CountryRemovedEvent(country).callEvent();
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

    public Country getCountry(Citizen citizen) {
        return citizen.getCountry();
    }

    public Country getCountry(Player player) {
        var citizen = getCitizen(player);
        if (citizen == null)
            return null;
        return citizen.getCountry();
    }

    public List<String> getCountryNames() {
        return countries.values().stream().map(Country::getName).collect(Collectors.toList());
    }

    public Set<Citizen> getCountryCitizens(Country country) {
        return settlements.values().stream().filter(s -> country.equals(s.getCountry()))
                .flatMap(s -> s.getCitizens().stream()).collect(Collectors.toSet());
    }

    // #endregion

    // #region Records

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

    public List<BankRecord> getBankRecords(BankRecordTransactionType type, long from, long to) {
        try {
            return databaseManager.getBankRecordService().getRecordsByTypeAsync(type, from, to).get();
        } catch (Exception ex) {
            United.logger().error("Could not retrieve bank records for type " + type, "UnitedLands");
            return new ArrayList<>();
        }
    }

    // **************************************************
    // Playtime Records
    // **************************************************

    public void createPlaytimeRecordDbData(PlaytimeRecord record) {
        databaseManager.getPlaytimeRecordService().createAsync(record);
    }

    public List<PlaytimeRecord> getRecentPlaytimeRecords(int days) {
        try {
            return databaseManager.getPlaytimeRecordService().getForLastDaysAsync(days).get();
        } catch (Exception ex) {
            United.logger().error("Could not retrieve playtime records", "UnitedLands");
            return new ArrayList<>();
        }
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
    // ULCINC Records
    // **************************************************

    public void createUlcincRecordDbData(ULCINCRecord record) {
        databaseManager.getUlcincService().createAsync(record);
    }

    public void createUlcincRecordDbData(Collection<ULCINCRecord> records) {
        databaseManager.getUlcincService().createAllAsync(records);
    }

    public List<ULCINCRecord> getRecentUlcincRecords(int days) {
        try {
            return databaseManager.getUlcincService().getForLastDaysAsync(days).get();
        } catch (Exception ex) {
            United.logger().error("Could not retrieve ULCINC records", "UnitedLands");
            return new ArrayList<>();
        }
    }

    public Long getLatestUlcincTimestamp() {
        try {
            return databaseManager.getUlcincService().getLatestTimestampAsync().get().orElse(null);
        } catch (Exception ex) {
            United.logger().error("Could not retrieve latest ULCINC timestamp", "UnitedLands");
            return null;
        }
    }

    // #endregion

    // #region Helpers

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

    // #endregion

}
