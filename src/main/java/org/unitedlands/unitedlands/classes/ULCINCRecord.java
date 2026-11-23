package org.unitedlands.unitedlands.classes;

import java.util.UUID;

import org.unitedlands.libs.ormlite.field.DatabaseField;
import org.unitedlands.classes.database.Identifiable;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;

public class ULCINCRecord implements Identifiable {

    @DatabaseField(id = true, width = 36, canBeNull = false)
    private UUID uuid;

    @DatabaseField(width = 36, canBeNull = false)
    private UUID countryId;

    @DatabaseField(columnName = "timestamp")
    private long timestamp;

    @DatabaseField(columnName = "activity_index")
    private double activityIndex;

    @DatabaseField(columnName = "economy_index")
    private double economyIndex;

    @DatabaseField(columnName = "military_index")
    private double militaryIndex;

    @DatabaseField(columnName = "geographic_index")
    private double geographicIndex;

    @DatabaseField(columnName = "total_index")
    private double totalIndex;

    @DatabaseField(columnName = "smoothed_total_index")
    private double smoothedTotalIndex;

    private transient Country country;

    public ULCINCRecord() {
    }

    public ULCINCRecord(Country country, double activityIndex, double economyIndex, double militaryIndex, double geographicIndex,
            double totalIndex, double smoothedTotalIndex) {

        this.uuid = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.country = country;
        this.countryId = country.getUuid();
        this.activityIndex = activityIndex;
        this.economyIndex = economyIndex;
        this.militaryIndex = militaryIndex;
        this.geographicIndex = geographicIndex;
        this.totalIndex = totalIndex;
        this.smoothedTotalIndex = smoothedTotalIndex;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Country getCountry() {
        if (country == null)
            country = UnitedLandsDataManager.instance().getCountry(countryId);
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
        this.countryId = country.getUuid();
    }

    public UUID getCountryId() {
        return countryId;
    }

    public double getActivityIndex() {
        return activityIndex;
    }

    public void setActivityIndex(double activityIndex) {
        this.activityIndex = activityIndex;
    }

    public double getEconomyIndex() {
        return economyIndex;
    }

    public void setEconomyIndex(double economyIndex) {
        this.economyIndex = economyIndex;
    }

    public double getMilitaryIndex() {
        return militaryIndex;
    }

    public void setMilitaryIndex(double militaryIndex) {
        this.militaryIndex = militaryIndex;
    }

    public double getGeographicIndex() {
        return geographicIndex;
    }

    public void setGeographicIndex(double geographicIndex) {
        this.geographicIndex = geographicIndex;
    }

    public double getTotalIndex() {
        return totalIndex;
    }

    public void setTotalIndex(double totalIndex) {
        this.totalIndex = totalIndex;
    }

    public double getSmoothedTotalIndex() {
        return smoothedTotalIndex;
    }

    public void setSmoothedTotalIndex(double smoothedTotalIndex) {
        this.smoothedTotalIndex = smoothedTotalIndex;
    }
    

}
