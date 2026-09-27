package org.unitedlands.unitedlands.classes.webservices;

import java.util.UUID;

import org.unitedlands.unitedlands.classes.db.Identifiable;

import org.unitedlands.libs.ormlite.field.DatabaseField;

public class LoginChallenge implements Identifiable {

    @DatabaseField(id = true, width = 36, canBeNull = false)
    protected UUID uuid;
    @DatabaseField(width = 20, canBeNull = false)
    protected String code;
    @DatabaseField(width = 36, canBeNull = true, columnName = "mc_uuid")
    protected UUID mcUUID;
    @DatabaseField(width = 64, canBeNull = true, columnName = "mc_username")
    protected String mcUsername;
    @DatabaseField(canBeNull = false)
    protected String status;
    @DatabaseField(canBeNull = false, columnName = "created_at")
    protected long createdAt;
    @DatabaseField(canBeNull = false, columnName = "expires_at")
    protected long expiresAt;

    public LoginChallenge() {
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UUID getMcUUID() {
        return mcUUID;
    }

    public void setMcUUID(UUID mcUUID) {
        this.mcUUID = mcUUID;
    }

    public String getMcUsername() {
        return mcUsername;
    }

    public void setMcUsername(String mcUsername) {
        this.mcUsername = mcUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    
}
