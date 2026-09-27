package org.unitedlands.unitedlands.classes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.events.region.RegionClaimedEvent;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.PolygonUtils;
import org.unitedlands.unitedlands.utils.SerializationUtils;
import org.unitedlands.libs.ormlite.field.DataType;
import org.unitedlands.libs.ormlite.field.DatabaseField;

public class Region extends GeopolObject implements PermissionHolder {

    @DatabaseField(columnName = "default_name")
    private String defaultName;

    @DatabaseField(canBeNull = true, columnName = "home_chunk_x")
    private int homeChunkCoordinatesX;
    @DatabaseField(canBeNull = true, columnName = "home_chunk_z")
    private int homeChunkCoordinatesZ;
    @DatabaseField(canBeNull = true, columnName = "polygon_serialized", dataType = DataType.LONG_STRING)
    private String polygonSerialized;
    @DatabaseField(canBeNull = false, columnName = "area")
    private double area;
    @DatabaseField(canBeNull = true, columnName = "spawn_serialized")
    private String spawnSerialized;

    @DatabaseField(width = 36, columnName = "claimant_country_uuid")
    private UUID claimantCountryUuid;
    private transient Country claimantCountry;
    @DatabaseField(columnName = "claim_start_time", canBeNull = true)
    private Long claimStartTime;
    @DatabaseField(columnName = "claim_end_time", canBeNull = true)
    private Long claimEndTime;
    @DatabaseField(columnName = "claimed_time", canBeNull = true)
    private Long claimedTime;

    @DatabaseField(width = 36, columnName = "country_uuid")
    private UUID countryUuid;

    @DatabaseField(width = 36, columnName = "administrator_uuid")
    private UUID administratorUuid;

    @DatabaseField(canBeNull = false, columnName = "break_permissions")
    private int breakPermissions = LocationMembership.OWNER | LocationMembership.TRUSTED;
    @DatabaseField(canBeNull = false, columnName = "place_permissions")
    private int placePermissions = LocationMembership.OWNER | LocationMembership.TRUSTED;
    @DatabaseField(canBeNull = false, columnName = "container_permissions")
    private int containerPermissions = LocationMembership.OWNER | LocationMembership.TRUSTED;
    @DatabaseField(canBeNull = false, columnName = "switch_permissions")
    private int switchPermissions = LocationMembership.OWNER | LocationMembership.TRUSTED;
    @DatabaseField(canBeNull = false, columnName = "block_use_permissions")
    private int blockUsePermissions = LocationMembership.OWNER | LocationMembership.TRUSTED;
    @DatabaseField(canBeNull = false, columnName = "interact_permissions")
    private int interactPermissions = LocationMembership.OWNER | LocationMembership.TRUSTED;

    @DatabaseField(columnName = "allow_pvp", canBeNull = false)
    private boolean allowPvp = true;
    @DatabaseField(columnName = "allow_monsters", canBeNull = false)
    private boolean allowMonsters = true;
    @DatabaseField(columnName = "allow_animals", canBeNull = false)
    private boolean allowAnimals = true;
    @DatabaseField(columnName = "allow_fire", canBeNull = false)
    private boolean allowFire = true;
    @DatabaseField(columnName = "allow_explosions", canBeNull = false)
    private boolean allowExplosions = true;

    @DatabaseField(columnName = "debug_stroke_color", canBeNull = true)
    private int debugStrokeColor;
    @DatabaseField(columnName = "debug_fill_color", canBeNull = true)
    private int debugFillColor;

    private transient Coordinates homeChunkCoordinates;
    private transient Location spawn;
    private transient Country country;
    private transient Citizen administrator;

    private transient double minX, minZ, maxX, maxZ;

    private transient Set<Settlement> settlements = new HashSet<>();
    // private transient Set<RegionChunk> chunks = new HashSet<>();

    private BukkitTask claimTask;

    public Region() {

    }

    public String getDefaultName() {
        return defaultName;
    }

    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }

    public int getHomeChunkCoordinatesX() {
        return homeChunkCoordinatesX;
    }

    public void setHomeChunkCoordinatesX(int homeChunkCoordinatedX) {
        this.homeChunkCoordinatesX = homeChunkCoordinatedX;
    }

    public int getHomeChunkCoordinatesZ() {
        return homeChunkCoordinatesZ;
    }

    public void setHomeChunkCoordinatesZ(int homeChunkCoordinatesZ) {
        this.homeChunkCoordinatesZ = homeChunkCoordinatesZ;
    }

    public String getSpawnSerialized() {
        return spawnSerialized;
    }

    public void setSpawnSerialized(String spawnSerialized) {
        this.spawnSerialized = spawnSerialized;
    }

    public void setSpawn(Location location) {
        this.spawn = location;
        this.spawnSerialized = SerializationUtils.serializeLocation(location);
    }

    public Location getSpawn() {
        if (spawn == null && spawnSerialized != null)
            this.spawn = SerializationUtils.deserializeLocation(spawnSerialized);
        return spawn;
    }

    public String getPolygonSerialized() {
        return polygonSerialized;
    }

    public void setPolygonSerialized(String polygonSerialized) {
        this.polygonSerialized = polygonSerialized;
    }

    public void setPolygon(double[] points) {
        this.polygonSerialized = Arrays.stream(points).mapToObj(String::valueOf).collect(Collectors.joining(";"));
        calculateBounds(points);
    }

    public double[] getPolygon() {
        if (this.polygonSerialized == null)
            return null;
        return Arrays.stream(this.polygonSerialized.split(";")).mapToDouble(Double::parseDouble).toArray();
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public void calculateBounds() {
        calculateBounds(getPolygon());
    }

    public void calculateBounds(double[] points) {

        this.minX = Double.POSITIVE_INFINITY;
        this.minZ = Double.POSITIVE_INFINITY;
        this.maxX = Double.NEGATIVE_INFINITY;
        this.maxZ = Double.NEGATIVE_INFINITY;

        for (int x = 0; x <= points.length - 2; x += 2) {
            if (points[x] < this.minX)
                this.minX = points[x];
            if (points[x] > this.maxX)
                this.maxX = points[x];
        }
        for (int z = 1; z <= points.length - 1; z += 2) {
            if (points[z] < this.minZ)
                this.minZ = points[z];
            if (points[z] > this.maxZ)
                this.maxZ = points[z];
        }
    }

    public void setHomeChunkCoordinates(Coordinates coordinates) {
        this.homeChunkCoordinates = coordinates;
        this.homeChunkCoordinatesX = coordinates.getX();
        this.homeChunkCoordinatesZ = coordinates.getZ();
    }

    public Coordinates getHomeChunkCoordinates() {
        if (this.homeChunkCoordinates == null)
            homeChunkCoordinates = new Coordinates(this.homeChunkCoordinatesX, this.homeChunkCoordinatesZ, this.worldName);
        return homeChunkCoordinates;
    }

    public UUID getClaimantCountryId() {
        return claimantCountryUuid;
    }

    public void setClaimantCountry(Country country) {
        this.claimantCountry = country;
        this.claimantCountryUuid = country.getUuid();
    }

    public Country getClaimantCountry() {
        if (this.claimantCountry == null && this.claimantCountryUuid != null)
            claimantCountry = UnitedLandsDataManager.instance().getCountry(claimantCountryUuid);
        return claimantCountry;
    }

    public void removeClaimantCountry() {
        this.claimantCountry = null;
        this.claimantCountryUuid = null;
    }

    public Long getClaimStartTime() {
        return claimStartTime;
    }

    public void setClaimStartTime(Long claimStartTime) {
        this.claimStartTime = claimStartTime;
    }

    public Long getClaimEndTime() {
        return claimEndTime;
    }

    public void setClaimEndTime(Long claimEndTime) {
        this.claimEndTime = claimEndTime;
    }

    public Long getClaimedTime() {
        return claimedTime;
    }

    public void setClaimedTime(Long claimedTime) {
        this.claimedTime = claimedTime;
    }

    public void setCountry(Country country) {
        this.country = country;
        this.countryUuid = country.getUuid();
        this.claimedTime = System.currentTimeMillis();
    }

    public Country getCountry() {
        if (this.country == null && this.countryUuid != null)
            country = UnitedLandsDataManager.instance().getCountry(countryUuid);
        return country;
    }

    public void removeCountry() {
        this.country = null;
        this.countryUuid = null;
        this.claimedTime = null;
        this.name = this.defaultName;
        removeAdministrator();
    }

    public void setAdministrator(Citizen citizen) {
        if (citizen == null)
            return;
        this.administrator = citizen;
        this.administratorUuid = citizen.getUuid();
    }

    public Citizen getAdministrator() {
        if (this.administrator == null && this.administratorUuid != null)
            administrator = UnitedLandsDataManager.instance().getCitizen(administratorUuid);
        return administrator;
    }

    public void removeAdministrator() {
        this.administrator = null;
        this.administratorUuid = null;
    }

    public boolean hasCountry() {
        return getCountry() != null;
    }

    public Set<Settlement> getSettlements() {
        return this.settlements;
    }

    public Set<Settlement> getSettlements(Country country) {
        return this.settlements.stream().filter(s -> country.equals(s.getCountry())).collect(Collectors.toSet());
    }

    public void addSettlement(Settlement settlement) {
        settlements.add(settlement);
    }

    public void removeSettlement(Settlement settlement) {
        settlements.remove(settlement);
    }

    @Override
    public int getBreakPermissions() {
        return breakPermissions;
    }

    @Override
    public int getPlacePermissions() {
        return placePermissions;
    }

    @Override
    public int getContainerPermissions() {
        return containerPermissions;
    }

    @Override
    public int getSwitchPermissions() {
        return switchPermissions;
    }

    @Override
    public int getBlockUsePermissions() {
        return blockUsePermissions;
    }

    @Override
    public int getInteractPermissions() {
        return interactPermissions;
    }

    public void setBreakPermissions(int breakPermissions) {
        this.breakPermissions = breakPermissions;
    }

    public void setPlacePermissions(int placePermissions) {
        this.placePermissions = placePermissions;
    }

    public void setContainerPermissions(int containerPermissions) {
        this.containerPermissions = containerPermissions;
    }

    public void setSwitchPermissions(int switchPermissions) {
        this.switchPermissions = switchPermissions;
    }

    public void setBlockUsePermissions(int blockUsePermissions) {
        this.blockUsePermissions = blockUsePermissions;
    }

    public void setInteractPermissions(int interactPermissions) {
        this.interactPermissions = interactPermissions;
    }

    public boolean allowPvp() {
        return allowPvp;
    }

    public void setAllowPvp(boolean allowPvp) {
        this.allowPvp = allowPvp;
    }

    public boolean allowMonsters() {
        return allowMonsters;
    }

    public void setAllowMonsters(boolean allowMonsters) {
        this.allowMonsters = allowMonsters;
    }

    public boolean allowAnimals() {
        return allowAnimals;
    }

    public void setAllowAnimals(boolean allowAnimals) {
        this.allowAnimals = allowAnimals;
    }

    public boolean allowFire() {
        return allowFire;
    }

    public void setAllowFire(boolean allowFire) {
        this.allowFire = allowFire;
    }

    public boolean allowExplosions() {
        return allowExplosions;
    }

    public void setAllowExplosions(boolean allowExplosions) {
        this.allowExplosions = allowExplosions;
    }

    public Integer getFillColor() {
        if (hasCountry())
            return country.getFillColor();
        return Settings.defaultRegionFillColour;
    }

    public Integer getStrokeColor() {
        if (hasCountry())
            return country.getStrokeColor();
        return Settings.defaultRegionStrokeColour;
    }

    public int getDebugStrokeColor() {
        return debugStrokeColor;
    }

    public void setDebugStrokeColor(int debugStrokeColor) {
        this.debugStrokeColor = debugStrokeColor;
    }

    public int getDebugFillColor() {
        return debugFillColor;
    }

    public void setDebugFillColor(int debugFillColor) {
        this.debugFillColor = debugFillColor;
    }

    public double getMaxZ() {
        return maxZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMinX() {
        return minX;
    }

    public boolean isPointInRegion(double px, double py) {
        var regionPolygon = getPolygon();
        if (regionPolygon == null)
            return false;

        return PolygonUtils.isPointInPolygon(regionPolygon, px, py);
    }

    public void save() {
        UnitedLandsDataManager.instance().updateRegionDbData(this, false);
    }

    public void saveAndRender() {
        UnitedLandsDataManager.instance().updateRegionDbData(this, true);
    }

    public void startClaimTask() {
        cancelClaimTask();

        // Minimum delay of 5 seconds in case claimEndTime would have been during a
        // server downtime (i.e. give some time to let other things finish loading
        // before executing the region claim)

        var milliDelay = claimEndTime - System.currentTimeMillis();
        var delay = Math.max(100, milliDelay / 1000 * 20);

        claimTask = Bukkit.getScheduler().runTaskLater(UnitedLands.instance(), () -> {

            setCountry(getClaimantCountry());
            setAdministrator(getClaimantCountry().getLeader());
            getClaimantCountry().addRegion(this);

            removeClaimantCountry();
            setClaimStartTime(null);
            setClaimEndTime(null);

            this.saveAndRender();
            
            getCountry().saveAndRender();

            RegionClaimedEvent claimedEvent = new RegionClaimedEvent(this, country);
            claimedEvent.callEvent();
        }, delay);
    }

    public void cancelClaimTask() {
        if (claimTask != null) {
            claimTask.cancel();
            claimTask = null;
        }
    }

}
