package org.unitedlands.unitedlands.classes;

import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.managers.PermissionManager;

public class PlayerCache {

    private final Player player;

    private Coordinates cachedChunkCoordinates;

    private Region cachedRegion;
    private Settlement cachedSettlement;
    private SettlementChunk cachedSettlementChunk;

    private int chunkMembership;
    private int regionMembership;

    private boolean autoclaimActive = false;
    private Settlement autoclaimSettlement;

    public PlayerCache(Player player) {
        this.player = player;
    }

    public Coordinates getCachedChunkCoordinates() {
        return cachedChunkCoordinates;
    }

    public Region getCachedRegion() {
        return cachedRegion;
    }

    public Settlement getCachedSettlement() {
        return cachedSettlement;
    }

    public SettlementChunk getCachedSettlementChunk() {
        return cachedSettlementChunk;
    }

    public void updateSettlementChunkCache(Coordinates chunkCoordinates, SettlementChunk settlementChunk) {
        this.cachedChunkCoordinates = chunkCoordinates;
        this.cachedSettlementChunk = settlementChunk;
        this.cachedSettlement = settlementChunk.getSettlement();
    }

    public void clearSettlementChunkCache() {
        this.cachedChunkCoordinates = null;
        this.cachedSettlementChunk = null;
        this.cachedSettlement = null;
    }

    public void updateRegionCache(Region region) {
        this.cachedRegion = region;
    }

    public void clearRegionCache() {
        this.cachedRegion = null;
    }

    public void calculateMemberships() {

        chunkMembership = LocationMembership.FOREIGNER;
        regionMembership = LocationMembership.FOREIGNER;

        if (cachedSettlementChunk != null) {
            chunkMembership = PermissionManager.instance().calculateChunkMembership(cachedSettlementChunk, player);
        }
        if (cachedRegion != null) {
            regionMembership = PermissionManager.instance().calculateRegionMembership(cachedRegion, player);
        }
    }

    public int getChunkMembership() {
        return chunkMembership;
    }

    public int getRegionMembership() {
        return regionMembership;
    }

    public boolean isAutoclaimActive() {
        return autoclaimActive;
    }

    public void setAutoclaimActive(boolean autoclaimActive) {
        this.autoclaimActive = autoclaimActive;
    }

    public Settlement getAutoclaimSettlement() {
        return autoclaimSettlement;
    }

    public void setAutoclaimSettlement(Settlement autoclaimSettlement) {
        this.autoclaimSettlement = autoclaimSettlement;
    }

}
