package org.unitedlands.unitedlands.classes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.db.Identifiable;
import org.unitedlands.unitedlands.classes.interfaces.MetadataHolder;
import org.unitedlands.unitedlands.classes.metadata.MetaDataField;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.JsonUtils;
import com.google.gson.reflect.TypeToken;
import org.unitedlands.libs.ormlite.field.DataType;
import org.unitedlands.libs.ormlite.field.DatabaseField;

public class Citizen implements Identifiable, MetadataHolder {

    @DatabaseField(id = true, width = 36, canBeNull = false)
    private UUID uuid;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private long joined;

    @DatabaseField(canBeNull = false, columnName = "last_logon")
    private long lastLogon;

    @DatabaseField(canBeNull = false, columnName = "total_playtime")
    private long totalPlaytime;

    @DatabaseField(width = 36, columnName = "settlement_uuid")
    private UUID settlementUuid;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "settlement_ranks_serialized")
    private String settlementRanksSerialized;
    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "country_ranks_serialized")
    private String countryRanksSerialized;

    @DatabaseField(canBeNull = true, dataType = DataType.LONG_STRING, columnName = "metadata_serialized", columnDefinition = "MEDIUMTEXT")
    private String metadataSerialized;

    private OfflinePlayer offlinePlayer;
    private Player player;
    private Settlement settlement;
    private transient Set<String> settlementRanks;
    private transient Set<String> countryRanks;

    protected transient Map<String, MetaDataField<?>> metadata;

    public Citizen() {

    }

    public Citizen(OfflinePlayer player) {
        this.uuid = player.getUniqueId();
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getJoined() {
        return joined;
    }

    public void setJoined(long joined) {
        this.joined = joined;
    }

    public long getLastLogon() {
        return lastLogon;
    }

    public void setLastLogon(long lastLogon) {
        this.lastLogon = lastLogon;
    }

    public long getTotalPlaytime() {
        return totalPlaytime;
    }

    public void setTotalPlaytime(long totalPlaytime) {
        this.totalPlaytime = totalPlaytime;
    }

    public OfflinePlayer getOfflinePlayer() {
        if (this.offlinePlayer == null)
            this.offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return this.offlinePlayer;
    }

    public void setOfflinePlayer(OfflinePlayer player) {
        this.offlinePlayer = player;
    }

    public Player getPlayer() {
        if (this.player == null)
            this.player = Bukkit.getPlayer(uuid);
        return this.player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Settlement getSettlement() {
        if (settlement == null && settlementUuid != null)
            settlement = UnitedLandsDataManager.instance().getSettlement(settlementUuid);
        return settlement;
    }

    public Boolean hasSettlement() {
        return getSettlement() != null;
    }

    public boolean isMayor() {
        return hasSettlementRank("mayor");
    }

    public boolean isLeader() {
        return getCountry() != null && hasCountryRank("country-leader");
    }

    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
        this.settlementUuid = settlement.getUuid();
    }

    public void removeSettlement() {
        this.settlement = null;
        this.settlementUuid = null;
    }

    public void addSettlementRank(String rank) {
        var r = new HashSet<>(getSettlementRanks());
        r.add(rank);
        setSettlementRanks(r);
    }

    public void removeSettlementRank(String rank) {
        var r = new HashSet<>(getSettlementRanks());
        r.remove(rank);
        setSettlementRanks(r);
    }

    public void removeSettlementRanks() {
        var r = new HashSet<String>();
        setSettlementRanks(r);
    }

    public boolean hasSettlementRank(String rank) {
        return getSettlementRanks() != null ? getSettlementRanks().contains(rank) : false;
    }

    public Set<String> getSettlementRanks() {
        if (settlementRanks == null) {
            if (settlementRanksSerialized != null) {
                settlementRanks = Arrays.stream(settlementRanksSerialized.split(";"))
                        .collect(Collectors.toSet());
            } else {
                settlementRanks = new HashSet<>();
            }
        }
        return settlementRanks;
    }

    public void setSettlementRanks(Set<String> ranks) {
        this.settlementRanks = ranks;
        if (ranks != null && !ranks.isEmpty()) {
            this.settlementRanksSerialized = ranks.stream()
                    .collect(Collectors.joining(";"));
        } else {
            this.settlementRanksSerialized = null;
        }
    }

    public void addCountryRank(String rank) {
        var r = new HashSet<>(getCountryRanks());
        r.add(rank);
        setCountryRanks(r);
    }

    public void removeCountryRank(String rank) {
        var r = new HashSet<>(getCountryRanks());
        r.remove(rank);
        setCountryRanks(r);
    }

    public void removeCountryRanks() {
        var r = new HashSet<String>();
        setCountryRanks(r);
    }

    public boolean hasCountry() {
        if (getSettlement() == null)
            return false;
        return getSettlement().hasCountry();
    }

    public Country getCountry() {
        if (getSettlement() == null)
            return null;
        return getSettlement().getCountry();
    }

    public boolean hasCountryRank(String rank) {
        return getCountryRanks() != null ? getCountryRanks().contains(rank) : false;
    }

    public Set<String> getCountryRanks() {
        if (countryRanks == null) {
            if (countryRanksSerialized != null) {
                countryRanks = Arrays.stream(countryRanksSerialized.split(";"))
                        .collect(Collectors.toSet());
            } else {
                countryRanks = new HashSet<>();
            }
        }
        return countryRanks;
    }

    public void setCountryRanks(Set<String> ranks) {
        this.countryRanks = ranks;
        if (ranks != null && !ranks.isEmpty()) {
            this.countryRanksSerialized = ranks.stream()
                    .collect(Collectors.joining(";"));
        } else {
            this.countryRanksSerialized = null;
        }
    }

    @Override
    public boolean hasMetadata(String key) {
        return getMetadata().containsKey(key);
    }

    @Override
    public Map<String, MetaDataField<?>> getMetadata() {
        if (metadata == null)
            if (metadataSerialized != null && !metadataSerialized.isEmpty()) {
                var t = new TypeToken<Collection<MetaDataField<?>>>() {
                };
                Collection<MetaDataField<?>> parsedData = JsonUtils.deserialize(metadataSerialized, t);
                metadata = new HashMap<>();
                for (var m : parsedData)
                    metadata.put(m.getKey(), m);
            } else {
                metadata = new HashMap<>();
            }
        return metadata;
    }

    @Override
    public MetaDataField<?> getMetadata(String key) {
        if (getMetadata() == null)
            return null;
        return getMetadata().get(key);
    }

    @Override
    public void addMetadata(MetaDataField<?> data) {
        if (getMetadata() == null)
            metadata = new HashMap<>();
        metadata.put(data.getKey(), data);
        metadataSerialized = JsonUtils.serialize(metadata.values());
    }

    @Override
    public void removeMetadata(String key) {
        if (getMetadata() == null)
            return;
        metadata.remove(key);
        metadataSerialized = JsonUtils.serialize(metadata.values());
    }

    public void save() {
        UnitedLandsDataManager.instance().updateCitizenDbData(this);
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
        Citizen other = (Citizen) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
