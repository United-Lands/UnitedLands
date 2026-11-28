package org.unitedlands.unitedlands.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.ChatChannel;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsChatConfig;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsChatConfig.CustomChatChannelSettings;
import org.unitedlands.unitedlands.integrations.papi.PlaceholderAPIIntegration;
import org.unitedlands.utils.United;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatChannelManager {

    private static ChatChannelManager instance;

    public static ChatChannelManager instance() {
        return instance;
    }

    private final Set<Citizen> viewers = ConcurrentHashMap.newKeySet();

    private final Map<String, ChatChannel> channels = new ConcurrentHashMap<>();
    private final Map<Player, String> playerChannels = new ConcurrentHashMap<>();

    public ChatChannelManager() {
        instance = this;
        loadChannels();
    }

    private void loadChannels() {

        // Default channels
        for (var key : UnitedLandsChatConfig.get().defaultChannels().keys()) {
            var channelSettings = UnitedLandsChatConfig.get().defaultChannels().get(key);

            ChatChannel channel = new ChatChannel(
                channelSettings.key(), 
                channelSettings.tag(),
                channelSettings.color(), 
                channelSettings.prefix(), 
                channelSettings.notifyActivity(), 
                false);

            channels.put(channelSettings.key(), channel);
        }

        // Custom channels
        List<String> staleChannelKeys = new ArrayList<>();
        for (var key : UnitedLandsChatConfig.get().customChannels().keys()) {

            var channelSettings = UnitedLandsChatConfig.get().customChannels().get(key);
            if (isChannelStale(channelSettings)) {
                staleChannelKeys.add(key);
                continue;
            }

            ChatChannel channel = new ChatChannel(
                    channelSettings.key(),
                    channelSettings.tag(),
                    channelSettings.color(),
                    channelSettings.prefix(),
                    channelSettings.notifyActvity(),
                    true);

            channel.setLastActivity(channelSettings.lastActivity());

            if (channelSettings.password() != null && !channelSettings.password().isEmpty())
                channel.setPassword(channelSettings.password());

            if (channelSettings.creator() != null && !channelSettings.creator().isEmpty())
                channel.setCreator(UUID.fromString(channelSettings.creator()));

            if (channelSettings.blockedPlayers() != null) {
                for (var uuid : channelSettings.blockedPlayers())
                    channel.addBlockedPlayer(UUID.fromString(uuid));
            }
            channels.put(channelSettings.key(), channel);
        }
        removeChannels(staleChannelKeys);
    }

    public boolean isCustomChannel(String key) {
        return channels.containsKey(key) && channels.get(key).isCustom();
    }

    public void removeChannel(String key) {

        var playersInChannel = getPlayersInChannel(key);
        for (var player : playersInChannel)
            switchChannel(player, "GLOBAL", null);

        channels.remove(key);
        removeChannels(List.of(key));
    }

    private void removeChannels(List<String> keys) {

        File configFile = new File(UnitedLands.instance().getDataFolder(), "chat.yml");
        if (!configFile.exists()) {
            United.logger().error("Chat config file not found.");
            return;
        }

        var config = YamlConfiguration.loadConfiguration(configFile);
        var customChannels = config.getConfigurationSection("custom-channels");
        if (customChannels == null)
            return;

        for (var key : keys) {
            if (customChannels.contains(key)) {
                United.logger().info("Removing stale chat channel " + key);
                customChannels.set(key, null);
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            United.logger().error("Error while removing channels: " + e.getMessage());
        }
    }

    private boolean isChannelStale(CustomChatChannelSettings channelSettings) {
        var timeSinceLastActivity = System.currentTimeMillis() - channelSettings.lastActivity();
        var lifetime = UnitedLandsChatConfig.get().customChannelLifetime() * 1000;
        return timeSinceLastActivity > lifetime;
    }

    public void saveChannel(ChatChannel channel) {

        File configFile = new File(UnitedLands.instance().getDataFolder(), "chat.yml");
        if (!configFile.exists()) {
            United.logger().error("Chat config file not found.");
            return;
        }

        var config = YamlConfiguration.loadConfiguration(configFile);
        var customChannels = config.getConfigurationSection("custom-channels");
        if (customChannels == null)
            customChannels = config.createSection("custom-channels");

        ConfigurationSection channelConfig = null;
        if (customChannels.contains(channel.getKey())) {
            channelConfig = customChannels.getConfigurationSection(channel.getKey());
        } else {
            channelConfig = customChannels.createSection(channel.getKey());
        }

        channelConfig.set("key", channel.getKey());
        channelConfig.set("color", channel.getColor());
        channelConfig.set("tag", channel.getTag());
        channelConfig.set("prefix", channel.getPrefix());
        channelConfig.set("password", channel.getPassword());
        channelConfig.set("creator", channel.getCreator().toString());
        channelConfig.set("notify-activity", channel.notifyActvity());
        channelConfig.set("last-activity", channel.getLastActivity());
        channelConfig.set("blocked-players", channel.getBlockedPlayers().stream().map(UUID::toString).toList());

        try {
            config.save(configFile);
        } catch (IOException e) {
            United.logger().error("Error while saving channel " + channel.getKey() + ": " + e.getMessage());
        }

    }

    public void registerChannel(ChatChannel channel) {
        channels.put(channel.getKey(), channel);
        channel.save();
    }

    public List<String> getChannelNames() {
        return channels.keySet().stream().toList();
    }

    public boolean channelExists(String name) {
        return channels.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(name));
    }

    public long getChannelCountByCreator(UUID creator) {
        return channels.values().stream().filter(c -> creator.equals(c.getCreator())).count();
    }

    public List<String> getPublicChannelNames() {
        return channels.entrySet().stream().filter(c -> !c.getValue().hasPassword()).map(Entry::getKey).toList();
    }

    public ChatChannel getPlayerChannel(Player player) {
        if (player == null)
            return null;
        var key = playerChannels.get(player);
        return key != null ? channels.get(key) : null;
    }

    public List<Player> getPlayersInChannel(String channelKey) {
        return playerChannels.entrySet().stream().filter(e -> e.getValue().equals(channelKey)).map(Entry::getKey)
                .toList();
    }

    public boolean isInChannel(Player player, String key) {
        if (player == null)
            return false;
        return key.equals(playerChannels.get(player));
    }

    public void switchChannel(Player player, String key, String password) {

        var senderViewer = viewers.stream().filter(c -> c.getUuid().equals(player.getUniqueId())).findFirst()
                .orElse(null);
        if (senderViewer == null) {
            return;
        }

        if (!channels.containsKey(key)) {
            United.messenger().send(player, "chat.unknown-channel");
            return;
        }

        var oldChannel = getPlayerChannel(player);
        var newChannel = channels.get(key);

        switch (key) {
            case "COUNTRY":
                if (!senderViewer.hasCountry()) {
                    United.messenger().send(player, "general-errors.not-in-country");
                    return;
                }
                break;
            case "SETTLEMENT":
                if (!senderViewer.hasSettlement()) {
                    United.messenger().send(player, "general-errors.not-in-settlement");
                    return;
                }
                break;
            case "STAFF":
                if (!player.hasPermission("united.lands.admin")) {
                    United.messenger().send(player, "general-errors.no-permission");
                    return;
                }
                break;
            default:
                break;

        }

        if (newChannel.hasPassword()) {
            if (!newChannel.getPassword().equals(password)) {
                United.messenger().send(player, "chat.wrong-password");
                return;
            }
        }

        if (newChannel.getBlockedPlayers() != null) {
            if (newChannel.getBlockedPlayers().contains(player.getUniqueId())) {
                United.messenger().send(player, "chat.got-blocked", key);
                return;
            }
        }

        playerChannels.put(player, key);
        United.messenger().send(player, "chat.switch", newChannel.getColor(), newChannel.getKey());

        // Notify the audience in the old channel that someone left.
        if (oldChannel.notifyActvity()) {
            sendChannelNotification(oldChannel.getKey(),
                    United.messenger().get("chat.left", player.getName(), oldChannel.getKey()));
        }
        // Notify the audience in the new channel that someone joined.
        if (newChannel.notifyActvity()) {
            sendChannelNotification(key, United.messenger().get("chat.join", player.getName(), key));
        }

        // In custom channels, save the time when the a player joined or left a channel.
        // This will will be used to purge channels that have not been used for a 
        // prolonged period of time.
        if (oldChannel.isCustom()) {
            oldChannel.setLastActivity(System.currentTimeMillis());
            oldChannel.save();
        }
        if (newChannel.isCustom()) {
            newChannel.setLastActivity(System.currentTimeMillis());
            newChannel.save();
        }

    }


    public void registerPlayer(Player player) {

        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error(
                    "Could not add player " + player.getName() + " to chat because citizen data is missing.",
                    "UnitedLands");
            return;
        }
        viewers.add(citizen);

        playerChannels.put(player, "GLOBAL");
    }

    public void unregisterPlayer(Player player) {
        viewers.removeIf(v -> v.getUuid().equals(player.getUniqueId()));
        playerChannels.remove(player);
    }

    public void sendMessage(Player player, String channelKey, String message) {

        var channel = channels.get(channelKey);
        if (channel == null) {
            United.logger().error("Unknown chat channel: " + channelKey);
            return;
        }

        var senderViewer = viewers.stream().filter(c -> c.getUuid().equals(player.getUniqueId())).findFirst()
                .orElse(null);
        if (senderViewer == null) {
            return;
        }

        if (channelKey.equals("STAFF") && !player.hasPermission("united.lands.admin")) {
            United.messenger().send(player, "general-errors.no-permission");
            return;
        }

        List<Player> receivers = filterViewers(senderViewer, channelKey);

        if (receivers.size() <= 1 && UnitedLandsChatConfig.get().showEmptyChannelMessage()) {
            United.messenger().send(player, "chat.no-audience");
        }

        var color = channel.getColor();
        var formattedMessage = getFormattedMessage(channelKey, message, color, player);

        Audience.audience(receivers).sendMessage(formattedMessage);
    }

    private List<Player> filterViewers(Citizen citizen, String channelKey) {

        switch (channelKey) {
            case "GLOBAL":
                return viewers.stream().map(c -> c.getPlayer()).toList();
            case "LOCAL":
                var origin = citizen.getPlayer().getLocation();
                return viewers.stream()
                        .map(c -> c.getPlayer())
                        .filter(p -> p.getWorld().equals(origin.getWorld())
                                && p.getLocation().distanceSquared(origin) <= 100 * 100)
                        .toList();
            case "SETTLEMENT":
                return viewers.stream()
                        .filter(c -> c.hasSettlement() && c.getSettlement().equals(citizen.getSettlement()))
                        .map(c -> c.getPlayer())
                        .toList();
            case "COUNTRY":
                return viewers.stream().filter(c -> c.hasCountry() && c.getCountry().equals(citizen.getCountry()))
                        .map(c -> c.getPlayer()).toList();
            case "STAFF":
                return viewers.stream().filter(c -> c.getPlayer().hasPermission("united.lands.admin"))
                        .map(c -> c.getPlayer()).toList();
            default:
                return playerChannels.entrySet().stream().filter(pc -> pc.getValue().equals(channelKey))
                        .map(Entry::getKey).toList();
        }
    }

    public void handleMessage(AsyncChatEvent event) {

        var player = event.getPlayer();
        var text = PlainTextComponentSerializer.plainText().serialize(event.message());

        PreparedMessage prepared;
        try {
            prepared = callOnMainThread(() -> prepareMessage(player, text));
        } catch (Exception e) {
            United.logger().error("Error while processing chat message of player " + player.getName() + ": " + e);
            event.setCancelled(true);
            return;
        }

        if (prepared == null)
            return;

        event.viewers().clear();
        event.viewers().addAll(prepared.receivers());
        event.renderer((source, sourceDisplayName, message, viewer) -> prepared.message());
    }

    private PreparedMessage prepareMessage(Player player, String text) {

        var channel = getPlayerChannel(player);
        if (channel == null) {
            United.logger().error("Could not find chat channel of player " + player.getName());
            return null;
        }

        var senderViewer = viewers.stream().filter(c -> c.getUuid().equals(player.getUniqueId())).findFirst()
                .orElse(null);
        if (senderViewer == null) {
            return null;
        }

        var receivers = filterViewers(senderViewer, channel.getKey());

        if (receivers.size() <= 1 && UnitedLandsChatConfig.get().showEmptyChannelMessage()) {
            United.messenger().send(player, "chat.no-audience");
        }

        var message = getFormattedMessage(channel.getKey(), text, channel.getColor(), player);
        return new PreparedMessage(receivers, message);
    }

    private <T> T callOnMainThread(Callable<T> task) throws Exception {
        if (Bukkit.isPrimaryThread())
            return task.call();
        return Bukkit.getScheduler().callSyncMethod(UnitedLands.instance(), task).get(5, TimeUnit.SECONDS);
    }

    private record PreparedMessage(List<Player> receivers, Component message) {
    }

    public void sendChannelNotification(String channelKey, String message) {
        var receivers = getPlayersInChannel(channelKey);
        United.messenger().send(receivers, United.miniMessage().get().deserialize(message));
    }

    private @NotNull TextComponent getFormattedMessage(String channelKey, String text, String color, Player source) {
        var miniMessage = United.miniMessage().get();

        var channel = channels.get(channelKey);
        if (channel == null) {
            United.logger().error("Unknwon chat channel: " + channelKey);
            return Component.empty();
        }

        var tag = UnitedLandsChatConfig.get().tagOpen() +
                "<" + channel.getColor() + ">" +
                channel.getTag() +
                "</" + channel.getColor() + ">" +
                UnitedLandsChatConfig.get().tagClose();

        var prefix = channel.getPrefix();
        if (prefix != null && IntegrationManager.instance().usePAPI()) {
            prefix = PlaceholderAPIIntegration.instance().setPlaceholders(source, prefix);
        }
        var playerName = source.getName();
        if (UnitedLandsChatConfig.get().playerNamePlaceholder() != null && IntegrationManager.instance().usePAPI()) {
            playerName = PlaceholderAPIIntegration.instance().setPlaceholders(source,
                    UnitedLandsChatConfig.get().playerNamePlaceholder());
        }

        var header = tag + prefix + playerName + UnitedLandsChatConfig.get().delimiter();

        var message = Component.text()
                .append(miniMessage.deserialize(header))
                .append(miniMessage.deserialize("<" + color + ">" + text + "</" + color + ">"))
                .build();

        // Log all messages to console
        United.messenger().send(Bukkit.getConsoleSender(), message);

        return message;
    }

}
