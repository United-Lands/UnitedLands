package org.unitedlands.unitedlands.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.unitedlands.classes.ChatChannel;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.integrations.papi.PlaceholderAPIIntegration;
import org.unitedlands.utils.United;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatChannelManager {

    private static ChatChannelManager instance;

    public static ChatChannelManager instance() {
        return instance;
    }

    private Set<Citizen> viewers = new HashSet<>();

    private Map<String, ChatChannel> channels = new HashMap<>();
    private Map<Player, String> playerChannels = new HashMap<>();

    public ChatChannelManager() {
        instance = this;
        loadChannels();
    }

    private void loadChannels() {

        for (var key : UnitedLandsConfig.get().chat().channels().keys()) {
            var channelSettings = UnitedLandsConfig.get().chat().channels().get(key);

            ChatChannel channel = new ChatChannel(channelSettings.key(), channelSettings.tag(), 
                channelSettings.color(), channelSettings.prefix(), channelSettings.notifyJoin(), false);
            channels.put(channelSettings.key(), channel);
        }

    }

    public void registerChannel(ChatChannel channel) {
            channels.put(channel.getKey(), channel);
    }

    public List<String> getChannelNames() {
        return  channels.keySet().stream().toList();
    }

    public List<String> getPublicChannelNames() {
        return  channels.entrySet().stream().filter(c -> !c.getValue().hasPassword()).map(Entry::getKey).toList();
    }

    public ChatChannel getPlayerChannel(Player player) {
        return channels.get(playerChannels.get(player));
    }

    public List<Player> getPlayersInChannel(String channelKey) {
        return playerChannels.entrySet().stream().filter(e -> e.getValue().equals(channelKey)).map(Entry::getKey).toList();
    }

    public boolean isInChannel(Player player, String key) {
        return playerChannels.get(player).equals(key);
    }

    public void switchChannel(Player player, String key, String password) {

        var senderViewer = viewers.stream().filter(c -> c.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
        if (senderViewer == null) {
            return;
        }

        if (!channels.containsKey(key)) {
            United.messenger().send(player, "chat.unknown-channel");
            return;
        }

        var oldChannel = getPlayerChannel(player);
        var channel = channels.get(key);

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
            case "STAFF":
                if (!player.hasPermission("united.lands.admin")) {
                    United.messenger().send(player, "general-errors.no-permission");
                    return;
                }
            default:
                break;

        }

        if (channel.hasPassword()) {
            if (!channel.getPassword().equals(password)) {
                United.messenger().send(player, "chat.wrong-password");
                return;
            }
        }

        playerChannels.put(player, key);
        United.messenger().send(player, "chat.switch", channel.getColor(), channel.getKey());

        // Notify the audience that in the old channel that someone left. We'll use the same settings
        // as join messages.
        if (oldChannel.notifyJoin()) {
            sendChannelNotification(oldChannel.getKey(), United.messenger().get("chat.left", player.getName(), oldChannel.getKey()));
        }

        // Notify the audience that someone joined a channel
        if (channel.notifyJoin()) {
            sendChannelNotification(key, United.messenger().get("chat.join", player.getName(), key));
        }

        cleanUpChannels();
    }

    private void cleanUpChannels() {
        var playerChannelKeys = playerChannels.values().stream().distinct().toList();
        var abandonedChannels = channels.values().stream().filter(c -> !playerChannelKeys.contains(c.getKey())).toList();
        for (var channel : abandonedChannels) {
            if (channel.removeEmpty()) {
                United.logger().info("Removing empty chat channel " + channel.getKey());
                channels.remove(channel.getKey());
            }
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
        
        cleanUpChannels();
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

        List<Player> receivers = filterViewers(senderViewer, channelKey);

        if (receivers.size() <= 1 && UnitedLandsConfig.get().chat().showEmptyChannelMessage()) {
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
                return viewers.stream()
                        .filter(c -> c.getPlayer().getLocation()
                                .distanceSquared(citizen.getPlayer().getLocation()) <= 100 * 100)
                        .map(c -> c.getPlayer()).toList();
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

        var channel = channels.get(playerChannels.get(event.getPlayer()));
        if (channel == null) {
            United.logger().error("Could not find chat channel of player " + event.getPlayer().getName());
            return;
        }

        var senderViewer = viewers.stream().filter(c -> c.getUuid().equals(event.getPlayer().getUniqueId())).findFirst()
                .orElse(null);
        if (senderViewer == null) {
            return;
        }

        var receivers = filterViewers(senderViewer, channel.getKey());
        event.viewers().clear();
        event.viewers().addAll(receivers);

        if (receivers.size() <= 1 && UnitedLandsConfig.get().chat().showEmptyChannelMessage()) {
            United.messenger().send(event.getPlayer(), "chat.no-audience");
        }

        var text = PlainTextComponentSerializer.plainText().serialize(event.message());
        var color = channel.getColor();

        event.renderer((source, sourceDisplayName, message, viewer) -> getFormattedMessage(channel.getKey(), text,
                color, source));
    }

    public void sendChannelNotification(String channelKey, String message) {
        var receivers = getPlayersInChannel(channelKey);
        United.messenger().send(receivers, MiniMessage.miniMessage().deserialize(message));
    }

    private @NotNull TextComponent getFormattedMessage(String channelKey, String text, String color, Player source) {
        var miniMessage = MiniMessage.miniMessage();

        var channel = channels.get(channelKey);
        if (channel == null) {
            United.logger().error("Unknwon chat channel: " + channelKey);
            return Component.empty();
        }

        var tag = 
            UnitedLandsConfig.get().chat().tagOpen() + 
            "<" + channel.getColor() + ">" +
            channel.getTag() + 
            "</" + channel.getColor() + ">" +
            UnitedLandsConfig.get().chat().tagClose();

        var prefix = channel.getPrefix();
        if (prefix != null && IntegrationManager.instance().usePAPI()) {
            prefix = PlaceholderAPIIntegration.instance().setPlaceholders(source, prefix);
        }
        var playerName = source.getName();
        if (UnitedLandsConfig.get().chat().playerNamePlaceholder() != null && IntegrationManager.instance().usePAPI()) {
            playerName = PlaceholderAPIIntegration.instance().setPlaceholders(source, UnitedLandsConfig.get().chat().playerNamePlaceholder());
        }

        var header = tag + prefix + playerName + UnitedLandsConfig.get().chat().delimiter();
        
        var message =  Component.text()
                .append(miniMessage.deserialize(header))
                .append(miniMessage.deserialize("<" + color + ">" + text + "</" + color + ">"))
                .build();

        // Log all messages to console
        United.messenger().send(Bukkit.getConsoleSender(), message);

        return message;
    }

}
