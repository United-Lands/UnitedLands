package org.unitedlands.unitedlands.managers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.ChatChannel;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsChatConfig;
import org.unitedlands.unitedlands.integrations.papi.PlaceholderAPIIntegration;
import org.unitedlands.unitedlands.utils.PasswordUtils;
import org.unitedlands.utils.United;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatChannelManager {

    private static ChatChannelManager instance;

    public static ChatChannelManager instance() {
        return instance;
    }

    private final Set<Citizen> viewers = ConcurrentHashMap.newKeySet();

    private final Map<String, ChatChannel> channels = new ConcurrentHashMap<>();
    private final Map<Player, String> playerChannels = new ConcurrentHashMap<>();

    private static final long MAINTENANCE_INTERVAL_SECONDS = 600;
    private static final String CHANNEL_DATA_FILE = "chat-channels.yml";

    private BukkitTask maintenanceTask;
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable, "UnitedLands-ChatChannelSaver");
        thread.setDaemon(true);
        return thread;
    });

    public ChatChannelManager() {
        instance = this;
        loadDefaultChannels();
        loadCustomChannels();
        startMaintenanceTask();
    }

    private void loadDefaultChannels() {

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
    }

    public void reloadDefaultChannels() {

        var defaultKeys = channels.values().stream().filter(c -> !c.isCustom()).map(ChatChannel::getKey).toList();
        defaultKeys.forEach(channels::remove);
        loadDefaultChannels();

        for (var entry : playerChannels.entrySet()) {
            if (!channels.containsKey(entry.getValue()))
                switchChannel(entry.getKey(), "GLOBAL", null);
        }
    }

    private void loadCustomChannels() {

        var dataFile = getDataFile();
        var data = dataFile.exists() ? YamlConfiguration.loadConfiguration(dataFile) : new YamlConfiguration();
        var changed = migrateLegacyChannels(data);

        // Custom channels
        var section = data.getConfigurationSection("channels");
        if (section != null) {
            for (var key : section.getKeys(false)) {
                var channelConfig = section.getConfigurationSection(key);
                if (channelConfig == null)
                    continue;

                if (isStale(channelConfig.getLong("last-activity"))) {
                    United.logger().info("Removing stale chat channel " + key);
                    changed = true;
                    continue;
                }

                var storedPassword = channelConfig.getString("password");
                if (storedPassword != null && !storedPassword.isEmpty() && !PasswordUtils.isHash(storedPassword))
                    changed = true;

                try {
                    var channel = readChannel(key, channelConfig);
                    channels.put(channel.getKey(), channel);
                } catch (IllegalArgumentException e) {
                    United.logger().error("Skipping invalid chat channel " + key + ": " + e.getMessage());
                }
            }
        }

        if (changed)
            persistSync();
    }

    private boolean migrateLegacyChannels(YamlConfiguration data) {

        File configFile = new File(UnitedLands.instance().getDataFolder(), "chat.yml");
        if (!configFile.exists())
            return false;

        var config = YamlConfiguration.loadConfiguration(configFile);
        var legacyChannels = config.getConfigurationSection("custom-channels");
        if (legacyChannels == null)
            return false;

        var target = data.getConfigurationSection("channels");
        if (target == null)
            target = data.createSection("channels");

        for (var key : legacyChannels.getKeys(false)) {
            var legacyChannel = legacyChannels.getConfigurationSection(key);
            if (legacyChannel != null && !target.contains(key))
                target.createSection(key, legacyChannel.getValues(false));
        }

        config.set("custom-channels", null);
        try {
            config.save(configFile);
            United.logger().info("Moved custom chat channels from chat.yml to " + CHANNEL_DATA_FILE + ".");
        } catch (IOException e) {
            United.logger().error("Error while removing custom channels from chat.yml: " + e.getMessage());
        }
        return true;
    }

    private ChatChannel readChannel(String sectionKey, ConfigurationSection channelConfig) {

        var key = channelConfig.getString("key", sectionKey);
        var channel = new ChatChannel(
                key,
                channelConfig.getString("tag", key),
                channelConfig.getString("color", UnitedLandsChatConfig.get().customChannelColor()),
                channelConfig.getString("prefix"),
                channelConfig.getBoolean("notify-activity"),
                true);

        channel.setLastActivity(channelConfig.getLong("last-activity"));

        var password = channelConfig.getString("password");
        if (password != null && !password.isEmpty()) {
            if (PasswordUtils.isHash(password))
                channel.setPasswordHash(password);
            else
                channel.setPassword(password);
        }

        var creator = channelConfig.getString("creator");
        if (creator != null && !creator.isEmpty())
            channel.setCreator(UUID.fromString(creator));

        for (var uuid : channelConfig.getStringList("blocked-players"))
            channel.addBlockedPlayer(UUID.fromString(uuid));

        return channel;
    }

    public boolean isCustomChannel(String key) {
        return channels.containsKey(key) && channels.get(key).isCustom();
    }

    public void removeChannel(String key) {

        var playersInChannel = getPlayersInChannel(key);
        for (var player : playersInChannel)
            switchChannel(player, "GLOBAL", null);

        channels.remove(key);
        persist();
    }

    private boolean isStale(long lastActivity) {
        var timeSinceLastActivity = System.currentTimeMillis() - lastActivity;
        var lifetime = UnitedLandsChatConfig.get().customChannelLifetime() * 1000;
        return timeSinceLastActivity > lifetime;
    }

    private void startMaintenanceTask() {
        var interval = MAINTENANCE_INTERVAL_SECONDS * 20L;
        maintenanceTask = Bukkit.getScheduler().runTaskTimer(UnitedLands.instance(), this::runMaintenance,
                interval, interval);
    }

    private void runMaintenance() {
        refreshOccupiedChannels();

        var staleKeys = getCustomChannels().stream()
                .filter(c -> isStale(c.getLastActivity()))
                .map(ChatChannel::getKey)
                .toList();

        for (var key : staleKeys) {
            United.logger().info("Removing stale chat channel " + key);
            channels.remove(key);
        }

        persist();
    }

    private void refreshOccupiedChannels() {
        var now = System.currentTimeMillis();
        for (var channel : getCustomChannels()) {
            if (!getPlayersInChannel(channel.getKey()).isEmpty())
                channel.setLastActivity(now);
        }
    }

    private void markActivity(ChatChannel channel) {
        if (channel.isCustom())
            channel.setLastActivity(System.currentTimeMillis());
    }

    public void shutdown() {
        if (maintenanceTask != null)
            maintenanceTask.cancel();
        refreshOccupiedChannels();

        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(5, TimeUnit.SECONDS))
                United.logger().error("Timed out while waiting for pending chat channel saves.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        persistSync();
    }

    public void saveChannel(ChatChannel channel) {
        persist();
    }

    private void persist() {
        var content = serializeChannels();
        if (saveExecutor.isShutdown()) {
            writeDataFile(content);
            return;
        }
        saveExecutor.execute(() -> writeDataFile(content));
    }

    private void persistSync() {
        writeDataFile(serializeChannels());
    }

    private String serializeChannels() {

        var data = new YamlConfiguration();
        var section = data.createSection("channels");

        for (var channel : getCustomChannels()) {
            var channelConfig = section.createSection(channel.getKey());
            channelConfig.set("key", channel.getKey());
            channelConfig.set("color", channel.getColor());
            channelConfig.set("tag", channel.getTag());
            channelConfig.set("prefix", channel.getPrefix());
            channelConfig.set("password", channel.getPasswordHash());
            channelConfig.set("creator", channel.getCreator() != null ? channel.getCreator().toString() : null);
            channelConfig.set("notify-activity", channel.notifyActvity());
            channelConfig.set("last-activity", channel.getLastActivity());
            channelConfig.set("blocked-players", channel.getBlockedPlayers().stream().map(UUID::toString).toList());
        }

        return data.saveToString();
    }

    private void writeDataFile(String content) {

        var target = getDataFile().toPath();
        var temp = target.resolveSibling(CHANNEL_DATA_FILE + ".tmp");

        try {
            Files.createDirectories(target.getParent());
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            United.logger().error("Error while saving chat channels: " + e.getMessage());
        }
    }

    private File getDataFile() {
        return new File(UnitedLands.instance().getDataFolder(), CHANNEL_DATA_FILE);
    }

    public void registerChannel(ChatChannel channel) {
        channels.put(channel.getKey(), channel);
        channel.save();
    }

    public List<String> getChannelNames() {
        return channels.keySet().stream().toList();
    }

    public ChatChannel getChannel(String key) {
        return channels.get(key);
    }

    public List<ChatChannel> getCustomChannels() {
        return channels.values().stream().filter(ChatChannel::isCustom).toList();
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

        if (!newChannel.checkPassword(password)) {
            United.messenger().send(player, "chat.wrong-password");
            return;
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
        if (oldChannel != null && oldChannel.notifyActvity()) {
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
        if (oldChannel != null && oldChannel.isCustom()) {
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

        var senderViewer = getOrRegisterViewer(player);
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

        markActivity(channel);

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

        if (prepared == null) {
            event.setCancelled(true);
            return;
        }

        event.viewers().clear();
        event.viewers().addAll(prepared.receivers());
        event.renderer((source, sourceDisplayName, message, viewer) -> prepared.message());
    }

    private Citizen getOrRegisterViewer(Player player) {
        var viewer = findViewer(player);
        if (viewer == null) {
            registerPlayer(player);
            viewer = findViewer(player);
        }
        if (viewer == null) {
            United.messenger().send(player, "general-errors.no-citizen-data", player.getName());
            return null;
        }
        if (getPlayerChannel(player) == null) {
            United.logger().error("Could not find chat channel of player " + player.getName() + ", resetting to GLOBAL.");
            playerChannels.put(player, "GLOBAL");
        }
        return viewer;
    }

    private Citizen findViewer(Player player) {
        return viewers.stream().filter(c -> c.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    private PreparedMessage prepareMessage(Player player, String text) {

        var senderViewer = getOrRegisterViewer(player);
        if (senderViewer == null) {
            return null;
        }

        var channel = getPlayerChannel(player);

        var receivers = filterViewers(senderViewer, channel.getKey());

        if (receivers.size() <= 1 && UnitedLandsChatConfig.get().showEmptyChannelMessage()) {
            United.messenger().send(player, "chat.no-audience");
        }

        markActivity(channel);

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
        var allowFormatting = source.hasPermission("united.lands.admin");

        var playerName = source.getName();
        if (UnitedLandsChatConfig.get().playerNamePlaceholder() != null && IntegrationManager.instance().usePAPI()) {
            playerName = PlaceholderAPIIntegration.instance().setPlaceholders(source,
                    UnitedLandsChatConfig.get().playerNamePlaceholder());
            if (!allowFormatting)
                playerName = miniMessage.escapeTags(playerName);
        }

        var header = tag + prefix + playerName + UnitedLandsChatConfig.get().delimiter();

        var content = allowFormatting
                ? Placeholder.parsed("message", text)
                : Placeholder.unparsed("message", text);

        var message = Component.text()
                .append(miniMessage.deserialize(header))
                .append(miniMessage.deserialize("<" + color + "><message></" + color + ">", content))
                .build();

        // Log all messages to console
        United.messenger().send(Bukkit.getConsoleSender(), message);

        return message;
    }

}
