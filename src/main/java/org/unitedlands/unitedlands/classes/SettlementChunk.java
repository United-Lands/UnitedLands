package org.unitedlands.unitedlands.classes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.unitedlands.classes.database.Identifiable;
import org.unitedlands.unitedlands.classes.interfaces.CoordinateHolder;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.SerializationUtils;
import org.unitedlands.libs.ormlite.field.DataType;
import org.unitedlands.libs.ormlite.field.DatabaseField;

import io.smallrye.common.constraint.Nullable;

public class SettlementChunk implements Identifiable, PermissionHolder, CoordinateHolder {

    @DatabaseField(id = true, width = 36, canBeNull = false)
    private UUID uuid;
    @DatabaseField(width = 255)
    private String name;
    @DatabaseField(width = 255, columnName = "world_name")
    private String worldName;
    @DatabaseField(width = 255, columnName = "settlement_uuid")
    private UUID settlementUuid;

    @DatabaseField()
    private int x;
    @DatabaseField()
    private int z;

    @DatabaseField(width = 32, columnName = "chunk_type")
    private String chunkType = "none";

    @DatabaseField(width = 36, columnName = "owner_uuid")
    private UUID ownerUuid;
    @DatabaseField(canBeNull = false, columnName = "claim_timestamp")
    protected long claimTimestamp;

    @DatabaseField(canBeNull = true, columnName = "sale_price")
    private Integer salePrice = null;

    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING, columnName = "trust_list_serialized")
    private String trustListSerialized;
    private transient Set<Citizen> trustList;

    @DatabaseField(canBeNull = false, columnName = "break_permissions")
    private int breakPermissions = LocationMembership.UNSET;
    @DatabaseField(canBeNull = false, columnName = "place_permissions")
    private int placePermissions = LocationMembership.UNSET;
    @DatabaseField(canBeNull = false, columnName = "container_permissions")
    private int containerPermissions = LocationMembership.UNSET;
    @DatabaseField(canBeNull = false, columnName = "switch_permissions")
    private int switchPermissions = LocationMembership.UNSET;
    @DatabaseField(canBeNull = false, columnName = "block_use_permissions")
    private int blockUsePermissions = LocationMembership.UNSET;
    @DatabaseField(canBeNull = false, columnName = "interact_permissions")
    private int interactPermissions = LocationMembership.UNSET;

    @DatabaseField(columnName = "allow_pvp", canBeNull = true)
    private @Nullable Boolean allowPvp;
    @DatabaseField(columnName = "allow_monsters", canBeNull = true)
    private @Nullable Boolean allowMonsters;
    @DatabaseField(columnName = "allow_animals", canBeNull = true)
    private @Nullable Boolean allowAnimals;
    @DatabaseField(columnName = "allow_fire", canBeNull = true)
    private @Nullable Boolean allowFire;
    @DatabaseField(columnName = "allow_explosions", canBeNull = true)
    private @Nullable Boolean allowExplosions;

    private transient World world;
    private transient Settlement settlement;
    private transient Coordinates coordinates;
    private transient Citizen owner;

    private final int size = 16;

    public SettlementChunk() {

    }

    public SettlementChunk(Coordinates coords) {
        setCoordinates(coords);
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public Coordinates getCoordinates() {
        if (coordinates == null)
            coordinates = new Coordinates(x, z, worldName);
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
        this.x = coordinates.getX();
        this.z = coordinates.getZ();
        this.worldName = coordinates.getWorldName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public World getWorld() {
        if (world == null && worldName != null)
            world = Bukkit.getWorld(worldName);
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
        this.worldName = world.getName();
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Settlement getSettlement() {
        if (settlement == null)
            settlement = UnitedLandsDataManager.instance().getSettlement(settlementUuid);
        return settlement;
    }

    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
        this.settlementUuid = settlement.getUuid();
    }

    public UUID getSettlementUuid() {
        return settlementUuid;
    }

    public String getChunkType() {
        return chunkType;
    }

    public void setChunkType(String chunkType) {
        this.chunkType = chunkType;
    }

    public void setOwner(Citizen owner) {
        this.ownerUuid = owner.getUuid();
        this.owner = owner;
    }

    public void removeOwner() {
        this.ownerUuid = null;
        this.owner = null;
    }

    public Citizen getOwner() {
        if (owner == null && ownerUuid != null)
            owner = UnitedLandsDataManager.instance().getCitizen(ownerUuid);
        return owner;
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public boolean hasOwner() {
        return ownerUuid != null;
    }

    public long getClaimTimestamp() {
        return claimTimestamp;
    }

    public void setClaimTimestamp(long claimTimestamp) {
        this.claimTimestamp = claimTimestamp;
    }

    public Integer getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Integer salePrice) {
        this.salePrice = salePrice;
    }

    public boolean isForSale() {
        return salePrice != null;
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

    public int getSize() {
        return size;
    }

    @Override
    public Coordinates getWorldCoords() {
        return new Coordinates(this.coordinates.getX() * size, this.coordinates.getZ() * size, this.worldName);
    }

    @Override
    public Coordinates getCenter() {
        var worldCoords = getWorldCoords();
        return new Coordinates(worldCoords.getX() + (size / 2), worldCoords.getZ() - (size / 2), this.worldName);
    }

    @Override
    public Coordinates getLowerLeft() {
        var worldCoords = getWorldCoords();
        return new Coordinates(worldCoords.getX(), worldCoords.getZ() + size, this.worldName);
    }

    @Override
    public Coordinates getLowerRight() {
        var worldCoords = getWorldCoords();
        return new Coordinates(worldCoords.getX() + size, worldCoords.getZ() + size, this.worldName);
    }

    @Override
    public Coordinates getUpperLeft() {
        var worldCoords = getWorldCoords();
        return new Coordinates(worldCoords.getX(), worldCoords.getZ(), this.worldName);
    }

    @Override
    public Coordinates getUpperRight() {
        var worldCoords = getWorldCoords();
        return new Coordinates(worldCoords.getX() + size, worldCoords.getZ(), this.worldName);
    }

    // Special settlement chunk permissions: if UNSET, inherit permissions
    // from the owning settlement.

    public PlotType getPlotType() {
        return PermissionManager.instance().getPlotType(chunkType);
    }

    public boolean hasPlotBehavior(PlotTypeBehavior behavior) {
        var plotType = getPlotType();
        return plotType != null && plotType.hasBehavior(behavior);
    }

    private int resolvePermission(int chunkValue, ToIntFunction<PlotType> typeValue,
            ToIntFunction<Settlement> settlementValue) {
        if (chunkValue != LocationMembership.UNSET)
            return chunkValue;
        var plotType = getPlotType();
        if (plotType != null) {
            var typeDefault = typeValue.applyAsInt(plotType);
            if (typeDefault != LocationMembership.UNSET)
                return typeDefault;
        }
        var parent = getSettlement();
        return parent != null ? settlementValue.applyAsInt(parent) : LocationMembership.UNSET;
    }

    private Boolean resolveFlag(Boolean chunkValue, Function<PlotType, PlotTypeFlag> typeValue,
            Function<Settlement, Boolean> settlementValue) {
        var plotType = getPlotType();
        var typeFlag = plotType != null ? typeValue.apply(plotType) : PlotTypeFlag.INHERIT;
        if (typeFlag == PlotTypeFlag.FORCE_ON)
            return true;
        if (typeFlag == PlotTypeFlag.FORCE_OFF)
            return false;
        if (chunkValue != null)
            return chunkValue;
        if (typeFlag == PlotTypeFlag.ON)
            return true;
        if (typeFlag == PlotTypeFlag.OFF)
            return false;
        var parent = getSettlement();
        return parent != null ? settlementValue.apply(parent) : false;
    }

    @Override
    public int getBreakPermissions() {
        return resolvePermission(breakPermissions, PlotType::breakPermissions, Settlement::getBreakPermissions);
    }

    @Override
    public int getPlacePermissions() {
        return resolvePermission(placePermissions, PlotType::placePermissions, Settlement::getPlacePermissions);
    }

    @Override
    public int getContainerPermissions() {
        return resolvePermission(containerPermissions, PlotType::containerPermissions, Settlement::getContainerPermissions);
    }

    @Override
    public int getSwitchPermissions() {
        return resolvePermission(switchPermissions, PlotType::switchPermissions, Settlement::getSwitchPermissions);
    }

    @Override
    public int getBlockUsePermissions() {
        return resolvePermission(blockUsePermissions, PlotType::blockUsePermissions, Settlement::getBlockUsePermissions);
    }

    @Override
    public int getInteractPermissions() {
        return resolvePermission(interactPermissions, PlotType::interactPermissions, Settlement::getInteractPermissions);
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
        return resolveFlag(allowPvp, PlotType::pvp, Settlement::allowPvp);
    }

    public void setAllowPvp(Boolean allowPvp) {
        this.allowPvp = allowPvp;
    }

    public Boolean allowMonsters() {
        return resolveFlag(allowMonsters, PlotType::monsters, Settlement::allowMonsters);
    }

    public void setAllowMonsters(Boolean allowMonsters) {
        this.allowMonsters = allowMonsters;
    }

    public Boolean allowAnimals() {
        return resolveFlag(allowAnimals, PlotType::animals, Settlement::allowAnimals);
    }

    public void setAllowAnimals(Boolean allowAnimals) {
        this.allowAnimals = allowAnimals;
    }

    public Boolean allowFire() {
        return resolveFlag(allowFire, PlotType::fire, Settlement::allowFire);
    }

    public void setAllowFire(Boolean allowFire) {
        this.allowFire = allowFire;
    }

    public Boolean allowExplosions() {
        return resolveFlag(allowExplosions, PlotType::explosions, Settlement::allowExplosions);
    }

    public void setAllowExplosions(Boolean allowExplosions) {
        this.allowExplosions = allowExplosions;
    }

    public void resetOverrides() {
        breakPermissions = LocationMembership.UNSET;
        placePermissions = LocationMembership.UNSET;
        containerPermissions = LocationMembership.UNSET;
        switchPermissions = LocationMembership.UNSET;
        blockUsePermissions = LocationMembership.UNSET;
        interactPermissions = LocationMembership.UNSET;
        allowPvp = null;
        allowMonsters = null;
        allowAnimals = null;
        allowFire = null;
        allowExplosions = null;
    }

    public void save() {
        UnitedLandsDataManager.instance().saveSettlementChunk(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SettlementChunk other = (SettlementChunk) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
