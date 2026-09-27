package org.unitedlands.unitedlands.classes;

import java.util.UUID;

import org.unitedlands.unitedlands.classes.db.Identifiable;

import org.unitedlands.libs.ormlite.field.DatabaseField;

public class BankRecord implements Identifiable {

    @DatabaseField(id = true, width = 36, canBeNull = false)
    protected UUID uuid;

    @DatabaseField(canBeNull = false)
    protected long timestamp;

    @DatabaseField(width = 36, canBeNull = false)
    protected UUID subject;

    @DatabaseField(canBeNull = false)
    protected double amount;

    @DatabaseField(width = 255)
    protected String details;

    public BankRecord() { }

    public BankRecord(UUID subject, double amount, String details) {
        this.uuid = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.subject = subject;
        this.amount = amount;
        this.details = details;
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

    public UUID getSubject() {
        return subject;
    }

    public void setSubject(UUID subject) {
        this.subject = subject;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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
        BankRecord other = (BankRecord) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }


}
