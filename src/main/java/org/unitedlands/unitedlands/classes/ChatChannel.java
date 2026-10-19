package org.unitedlands.unitedlands.classes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.unitedlands.unitedlands.managers.ChatChannelManager;

public class ChatChannel {

    private final String key;
    private final String prefix;
    private final boolean custom;
    private final boolean notifyActvity;

    private String tag;
    private UUID creator;
    private String color;
    private String password;
    private long lastActivity;

    private Set<UUID> blockedPlayers = new HashSet<>();

    public ChatChannel(String key, String tag, String color, String prefix, boolean notifyJoin, boolean custom) {
        this.key = key;
        this.tag = tag;
        this.color = color;
        this.prefix = prefix;
        this.notifyActvity = notifyJoin;
        this.custom = custom;
    }

    public String getKey() {
        return key;
    }

    public String getPrefix() {
        return prefix != null ? prefix : "";
    }

    public boolean isCustom() {
        return custom;
    }

    public boolean notifyActvity() {
        return notifyActvity;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public UUID getCreator() {
        return creator;
    }

    public void setCreator(UUID creator) {
        this.creator = creator;
    }

    public String getColor() {
        return color != null ? color : "white";
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasPassword() {
        return password != null;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastLeave) {
        this.lastActivity = lastLeave;
    }

    public void addBlockedPlayer(UUID playerId) {
        blockedPlayers.add(playerId);
    }

    public void removeBlockedPlayer(UUID playerId) {
        blockedPlayers.remove(playerId);
    }

    public boolean isBlocked(UUID playerId) {
        return blockedPlayers.contains(playerId);
    }

    public Set<UUID> getBlockedPlayers() {
        return blockedPlayers;
    }

    public void setBlockedPlayers(Set<UUID> blockedPlayers) {
        this.blockedPlayers = blockedPlayers;
    }

    public void save() {
        ChatChannelManager.instance().saveChannel(this);
    }

}