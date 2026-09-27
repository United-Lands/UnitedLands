package org.unitedlands.unitedlands.classes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.awt.Color;
import java.math.BigDecimal;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.unitedlands.utils.CostUtils;
import org.unitedlands.unitedlands.utils.SerializationUtils;
import org.unitedlands.libs.ormlite.field.DataType;
import org.unitedlands.libs.ormlite.field.DatabaseField;

public class Settlement extends GeopolObject implements PermissionHolder {

    @DatabaseField(canBeNull = true, columnName = "stroke_color")
    private Integer strokeColor;
    @DatabaseField(canBeNull = true, columnName = "fill_color")
    private Integer fillColor;

    @DatabaseField(canBeNull = true, columnName = "home_chunk_x")
    private int homeChunkCoordinatesX;
    @DatabaseField(canBeNull = true, columnName = "home_chunk_z")
    private int homeChunkCoordinatesZ;
    @DatabaseField(canBeNull = true, columnName = "spawn_serialized")
    private String spawnSerialized;
    @DatabaseField(canBeNull = true, columnName = "visitor_spawn_serialized")
    private String visitorSpawnSerialized;

    @DatabaseField(width = 36, columnName = "region_uuid")
    private UUID regionUuid;
    @DatabaseField(width = 36, columnName = "country_uuid")
    private UUID countryUuid;

    @DatabaseField(canBeNull = true, columnName = "town_board")
    private String townBoard;

    @DatabaseField(canBeNull = false, columnName = "bonus_claims")
    private int bonusClaims = 0;

    @DatabaseField(canBeNull = false, columnName = "tax")
    private float tax = 0.0f;
    @DatabaseField(canBeNull = false, columnName = "use_tax_percent")
    private boolean useTaxPercent = true;

    @DatabaseField(canBeNull = false, columnName = "public")
    private boolean isPublic = true;

    @DatabaseField(canBeNull = false, columnName = "ruined")
    private boolean ruined = true;
    @DatabaseField(canBeNull = true, columnName = "ruin_time")
    private Long ruinedTime;

    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING, columnName = "citizens_serialized")
    private String citizensSerialized;
    private transient Set<Citizen> citizens;

    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING, columnName = "trust_list_serialized")
    private String trustListSerialized;
    private transient Set<Citizen> trustList;

    @DatabaseField(canBeNull = false, columnName = "break_permissions")
    private int breakPermissions = LocationMembership.OWNER | LocationMembership.TRUSTED
            | LocationMembership.SETTLEMENT_RESIDENT;
    @DatabaseField(canBeNull = false, columnName = "place_permissions")
    private int placePermissions = LocationMembership.OWNER | LocationMembership.TRUSTED
            | LocationMembership.SETTLEMENT_RESIDENT;
    @DatabaseField(canBeNull = false, columnName = "container_permissions")
    private int containerPermissions = LocationMembership.OWNER | LocationMembership.TRUSTED
            | LocationMembership.SETTLEMENT_RESIDENT;
    @DatabaseField(canBeNull = false, columnName = "switch_permissions")
    private int switchPermissions = LocationMembership.OWNER | LocationMembership.TRUSTED
            | LocationMembership.SETTLEMENT_RESIDENT;
    @DatabaseField(canBeNull = false, columnName = "block_use_permissions")
    private int blockUsePermissions = LocationMembership.OWNER | LocationMembership.TRUSTED
            | LocationMembership.SETTLEMENT_RESIDENT;
    @DatabaseField(canBeNull = false, columnName = "interact_permissions")
    private int interactPermissions = LocationMembership.OWNER | LocationMembership.TRUSTED
            | LocationMembership.SETTLEMENT_RESIDENT;

    @DatabaseField(columnName = "allow_pvp", canBeNull = true)
    private Boolean allowPvp = false;
    @DatabaseField(columnName = "allow_monsters", canBeNull = true)
    private Boolean allowMonsters = false;
    @DatabaseField(columnName = "allow_animals", canBeNull = true)
    private Boolean allowAnimals = false;
    @DatabaseField(columnName = "allow_fire", canBeNull = true)
    private Boolean allowFire = false;
    @DatabaseField(columnName = "allow_explosions", canBeNull = true)
    private Boolean allowExplosions = false;

    private transient Coordinates homeChunkCoordinates;
    private transient Location spawn;
    private transient Location visitorSpawn;
    private transient Region region;
    private transient Country country;
    private transient Set<SettlementChunk> chunks = new HashSet<>();

    public Settlement() {

    }

    public Integer getStrokeColor() {
        if (hasCountry())
            return getCountry().getStrokeColor();
        return strokeColor != null ? strokeColor : Settings.defaultSettlementStrokeColour;
    }

    public void setStrokeColor(String hexColor) {
        this.strokeColor = ColorUtils.hexToColor(hexColor).getRGB();
    }

    public void setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor.getRGB();
    }

    public Integer getFillColor() {
        if (hasCountry())
            return getCountry().getFillColor();
        return fillColor != null ? fillColor : Settings.defaultSettlementFillColour;
    }

    public void setFillColor(String hexColor) {
        this.fillColor = ColorUtils.hexToColor(hexColor).getRGB();
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor.getRGB();
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

    public String getVisitorSpawnSerialized() {
        return visitorSpawnSerialized;
    }

    public void setVisitorSpawnSerialized(String visitorSpawnSerialized) {
        this.visitorSpawnSerialized = visitorSpawnSerialized;
    }

    public void setVisitorSpawn(Location location) {
        this.visitorSpawn = location;
        this.visitorSpawnSerialized = SerializationUtils.serializeLocation(location);
    }

    public Location getVisitorSpawn() {
        if (visitorSpawn == null && visitorSpawnSerialized != null)
            this.visitorSpawn = SerializationUtils.deserializeLocation(spawnSerialized);
        return visitorSpawn;
    }

    public void setHomeChunkCoordinates(Coordinates coordinates) {
        this.homeChunkCoordinates = coordinates;
        this.homeChunkCoordinatesX = coordinates.getX();
        this.homeChunkCoordinatesZ = coordinates.getZ();
    }

    public Coordinates getHomeChunkCoordinates() {
        if (this.homeChunkCoordinates == null)
            homeChunkCoordinates = new Coordinates(this.homeChunkCoordinatesX, this.homeChunkCoordinatesZ,
                    this.worldName);
        return homeChunkCoordinates;
    }

    public Set<SettlementChunk> getChunks() {
        return chunks;
    }

    public Set<SettlementChunk> getChunksOfType(String type) {
        CompletableFuture<Set<SettlementChunk>> chunkTypeFuture = CompletableFuture.supplyAsync(() -> {
            return getChunks().stream().filter(c -> type.equals(c.getChunkType())).collect(Collectors.toSet());
        });
        return chunkTypeFuture.join();
    }

    public void setChunks(Set<SettlementChunk> chunks) {
        this.chunks = chunks;
    }

    public void addChunk(SettlementChunk chunk) {
        this.chunks.add(chunk);
    }

    public void removeChunk(PermissionHolder chunk) {
        this.chunks.remove(chunk);
    }

    public boolean hasChunkAtCoordinates(Coordinates coords) {
        return chunks.stream().anyMatch(c -> c.getCoordinates().equals(coords));
    }

    public SettlementChunk getChunkAtCoordinates(Coordinates coords) {
        return chunks.stream().filter(c -> c.getCoordinates().equals(coords)).findFirst().orElse(null);
    }

    public void setRegion(Region region) {
        this.region = region;
        this.regionUuid = region.getUuid();
    }

    public Region getRegion() {
        if (this.region == null && this.regionUuid != null)
            this.region = UnitedLandsDataManager.instance().getRegion(regionUuid);
        return this.region;
    }

    public UUID getRegionUuid() {
        return this.regionUuid;
    }

    public Boolean hasCountry() {
        return getCountryUuid() != null;
    }

    public void setCountry(Country country) {
        this.country = country;
        this.countryUuid = country.getUuid();
    }

    public Country getCountry() {
        if (this.country == null && this.countryUuid != null)
            this.country = UnitedLandsDataManager.instance().getCountry(countryUuid);
        return this.country;
    }

    public void removeCountry() {
        this.country = null;
        this.countryUuid = null;
    }

    public boolean isCapital() {
        if (getCountry() == null)
            return false;
        return getCountry().getCapital().equals(this);
    }

    public UUID getCountryUuid() {
        return this.countryUuid;
    }

    public Boolean hasRegion() {
        return getRegion() != null;
    }

    public String getTownBoard() {
        return townBoard;
    }

    public void setTownBoard(String townBoard) {
        this.townBoard = townBoard;
    }

    public int getBonusClaims() {
        return bonusClaims;
    }

    public void setBonusClaims(int bonusClaims) {
        this.bonusClaims = bonusClaims;
    }

    public float getTax() {
        return tax;
    }

    public void setTax(float tax) {
        this.tax = tax;
    }

    public boolean useTaxPercent() {
        return useTaxPercent;
    }

    public void setUseTaxPercent(boolean useTaxPercent) {
        this.useTaxPercent = useTaxPercent;
    }

    public boolean isPublic() {
        return isPublic;
    }

    // TODO: Neutrality
    public boolean isNeutral() {
        return false;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean hasCitizen(Citizen citizen) {
        return getCitizens().contains(citizen);
    }

    public void addCitizen(Citizen citizen) {
        var c = new HashSet<>(getCitizens());
        c.add(citizen);
        setCitizens(c);
    }

    public void removeCitizen(Citizen citizen) {
        var c = new HashSet<>(getCitizens());
        c.remove(citizen);
        setCitizens(c);
    }

    public Set<Citizen> getCitizens() {
        if (citizens == null) {
            citizens = SerializationUtils.deserializeUuidListToSet(citizensSerialized,
                    UnitedLandsDataManager.instance()::getCitizen);
        }
        return citizens;
    }

    public void setCitizens(Set<Citizen> citizens) {
        this.citizens = citizens;
        this.citizensSerialized = SerializationUtils.serializeIdentifiableList(citizens);
    }

    public Set<Player> getOnlinePlayers() {
        return getCitizens().stream().map(c -> c.getPlayer()).filter(p -> p != null && p.isOnline())
                .map(p -> p.getPlayer())
                .collect(Collectors.toSet());
    }

    public void addTrusted(Citizen citizen) {
        var t = new HashSet<>(getTrustList());
        t.add(citizen);
        setTrustList(t);
    }

    public void removeTrusted(Citizen citizen) {
        var t = new HashSet<>(getTrustList());
        t.remove(citizen);
        setTrustList(t);
    }

    public Set<Citizen> getTrustList() {
        if (trustList == null) {
            trustList = SerializationUtils.deserializeUuidListToSet(trustListSerialized,
                    UnitedLandsDataManager.instance()::getCitizen);
        }
        return trustList;
    }

    public void setTrustList(Set<Citizen> trustList) {
        this.trustList = trustList;
        this.trustListSerialized = SerializationUtils.serializeIdentifiableList(trustList);
    }

    public Citizen getMayor() {
        if (getCitizens() == null)
            return null;
        return getCitizens().stream().filter(c -> c.hasSettlementRank("mayor")).findFirst().orElse(null);
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

    public Boolean allowPvp() {
        if (allowPvp == null) {
            if (region != null) {
                return region.allowPvp();
            } else {
                return false;
            }
        }
        return allowPvp;
    }

    public void setAllowPvp(Boolean allowPvp) {
        this.allowPvp = allowPvp;
    }

    public Boolean allowMonsters() {
        if (allowMonsters == null) {
            if (region != null) {
                return region.allowMonsters();
            } else {
                return false;
            }
        }
        return allowMonsters;
    }

    public void setAllowMonsters(Boolean allowMonsters) {
        this.allowMonsters = allowMonsters;
    }

    public Boolean allowAnimals() {
        if (allowAnimals == null) {
            if (region != null) {
                return region.allowAnimals();
            } else {
                return false;
            }
        }
        return allowAnimals;
    }

    public void setAllowAnimals(Boolean allowAnimals) {
        this.allowAnimals = allowAnimals;
    }

    public Boolean allowFire() {
        if (allowFire == null) {
            if (region != null) {
                return region.allowFire();
            } else {
                return false;
            }
        }
        return allowFire;
    }

    public void setAllowFire(Boolean allowFire) {
        this.allowFire = allowFire;
    }

    public Boolean allowExplosions() {
        if (allowExplosions == null) {
            if (region != null) {
                return region.allowExplosions();
            } else {
                return false;
            }
        }
        return allowExplosions;
    }

    public void setAllowExplosions(Boolean allowExplosions) {
        this.allowExplosions = allowExplosions;
    }

    public int getSize() {
        return getChunks().size();
    }

    public int getCitizenCount() {
        return getCitizens().size();
    }

    public BigDecimal getBalance() {
        return UnitedLandsEconomyManager.instance().getBalance(uuid);
    }

    public double getUpkeep() {
        return CostUtils.getSettlementUpkeep(this);
    }

    public void save() {
        UnitedLandsDataManager.instance().updateSettlementDbData(this, false);
    }

    public void saveAndRender() {
        UnitedLandsDataManager.instance().updateSettlementDbData(this, true);
    }

}
