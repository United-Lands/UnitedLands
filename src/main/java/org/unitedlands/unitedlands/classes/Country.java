package org.unitedlands.unitedlands.classes;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.unitedlands.utils.CostUtils;
import org.unitedlands.unitedlands.utils.SerializationUtils;
import org.unitedlands.libs.ormlite.field.DataType;
import org.unitedlands.libs.ormlite.field.DatabaseField;

public class Country extends GeopolObject {

    @DatabaseField(canBeNull = true, columnName = "stroke_color")
    private Integer strokeColor;
    @DatabaseField(canBeNull = true, columnName = "fill_color")
    private Integer fillColor;

    @DatabaseField(canBeNull = true, columnName = "bonus_regions")
    private int bonusRegions = 0;

    @DatabaseField(canBeNull = true, columnName = "spawn_serialized")
    private String spawnSerialized;

    @DatabaseField(canBeNull = false, width = 36, columnName = "capital_uuid")
    private UUID capitalUuid;

    @DatabaseField(canBeNull = true, width = 36, columnName = "overlord_uuid")
    private UUID overlordUuid;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "settlement_claim_whitelist")
    private String settlementClaimWhitelistSerialized;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "allies_serialized")
    private String alliesSerialized;

    private transient Location spawn;
    private transient Settlement capital;
    private transient Country overlord;
    private transient Set<Region> regions = new HashSet<>();
    private transient Set<Settlement> settlements = new HashSet<>();
    private transient Set<Settlement> settlementClaimWhitelist = new HashSet<>();
    private transient Set<Country> allies = new HashSet<>();

    public Integer getStrokeColor() {
        return strokeColor;
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
        return fillColor;
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

    public int getBonusRegions() {
        return bonusRegions;
    }

    public void setBonusRegions(int bonusRegions) {
        this.bonusRegions = bonusRegions;
    }

    public double getArea() {
        return getRegions().stream().mapToDouble(Region::getArea).sum();
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

    public boolean hasCapital() {
        return capitalUuid != null;
    }

    public Settlement getCapital() {
        if (capital == null && capitalUuid != null) {
            capital = UnitedLandsDataManager.instance().getSettlement(capitalUuid);
        }
        return capital;
    }

    public void setCapital(Settlement settlement) {
        if (settlement == null) {
            throw new IllegalArgumentException("Capital cannot be null");
        }
        this.capital = settlement;
        this.capitalUuid = settlement.getUuid();
    }

    public Country getOverlord() {
        if (overlord == null && overlordUuid != null) {
            overlord = UnitedLandsDataManager.instance().getCountry(overlordUuid);
        }
        return overlord;
    }

    public void setOverlord(Country country) {
        if (country == null) {
            this.overlord = null;
            this.overlordUuid = null;
        }
        this.overlord = country;
        this.overlordUuid = country.getUuid();
    }

    public void removeOverlord() {
        this.overlord = null;
        this.overlordUuid = null;
    }

    public Set<Region> getRegions() {
        return this.regions;
    }

    public void addRegion(Region region) {
        regions.add(region);
    }

    public void removeRegion(Region region) {
        regions.remove(region);
    }

    public Set<Settlement> getSettlements() {
        return this.settlements;
    }

    public void addSettlement(Settlement settlement) {
        settlements.add(settlement);
    }

    public void removeSettlement(Settlement settlement) {
        settlements.remove(settlement);
    }

    public Set<Citizen> getCitizens() {
        CompletableFuture<Set<Citizen>> future = CompletableFuture.supplyAsync(() -> {
            return settlements.stream().map(Settlement::getCitizens).flatMap(Set::stream).collect(Collectors.toSet());
        });
        return future.join();
    }

    public Citizen getLeader() {
        return getCitizens().stream().filter(c -> c.hasCountryRank("leader")).findFirst().orElse(null);
    }

    public int getRegionCount() {
        return getRegions().size();
    }

    public int getSettlementCount() {
        return getSettlements().size();
    }

    public int getCitizenCount() {
        return getCitizens().size();
    }

    public Set<Settlement> getSettlementClaimWhitelist() {
        if (settlementClaimWhitelist == null) {
            this.settlementClaimWhitelist = SerializationUtils.deserializeUuidListToSet(settlementClaimWhitelistSerialized,
                    UnitedLandsDataManager.instance()::getSettlement);
        }
        return settlementClaimWhitelist;
    }

    public void setSettlementClaimWhitelist(Set<Settlement> settlementClaimWhitelist) {
        this.settlementClaimWhitelist = settlementClaimWhitelist;
        this.settlementClaimWhitelistSerialized = SerializationUtils.serializeIdentifiableList(settlementClaimWhitelist);
    }

    public Set<Country> getAllies() {
        if (allies == null) {
            this.allies = SerializationUtils.deserializeUuidListToSet(alliesSerialized, UnitedLandsDataManager.instance()::getCountry);
        }
        return allies;
    }

    public void setAllies(Set<Country> allies) {
        this.allies = allies;
        this.alliesSerialized = SerializationUtils.serializeIdentifiableList(allies);
    }

    // TODO: Neutrality
    public boolean isNeutral() {
        return false;
    }

    public BigDecimal getBalance() {
        return UnitedLandsEconomyManager.instance().getBalance(uuid);
    }

    public double getUpkeep() {
        return CostUtils.getCountryUpkeep(this);
    }

    public void save() {
        UnitedLandsDataManager.instance().updateCountryDbData(this, false);
    }

    public void saveAndRender() {
        UnitedLandsDataManager.instance().updateCountryDbData(this, true);
    }


}
