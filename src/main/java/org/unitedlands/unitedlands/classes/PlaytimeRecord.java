package org.unitedlands.unitedlands.classes;

import java.util.UUID;

import org.unitedlands.libs.ormlite.field.DatabaseField;
import org.unitedlands.unitedlands.classes.db.Identifiable;


public class PlaytimeRecord implements Identifiable {

    @DatabaseField(id = true, width = 36, canBeNull = false)
    private UUID uuid;

    @DatabaseField(width = 36, canBeNull = false, columnName = "player_uuid")
    private UUID playerUuid;

    @DatabaseField(canBeNull = false)
    private Long logon;

    @DatabaseField(canBeNull = false)
    private Long logoff;

    @DatabaseField(canBeNull = false)
    private Long playtime;
    
    public PlaytimeRecord() {
    }

    public PlaytimeRecord(Long logon, Long logoff, Long playtime) {
        this.logon = logon;
        this.logoff = logoff;
        this.playtime = playtime;
    }

    @Override 
    public UUID getUuid() {
        return uuid;
    }

    @Override 
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public Long getLogon() {
        return logon;
    }

    public void setLogon(Long logon) {
        this.logon = logon;
    }

    public Long getLogoff() {
        return logoff;
    }

    public void setLogoff(Long logoff) {
        this.logoff = logoff;
    }

    public Long getPlaytime() {
        return playtime;
    }

    public void setPlaytime(Long playtime) {
        this.playtime = playtime;
    }

    
    
}
