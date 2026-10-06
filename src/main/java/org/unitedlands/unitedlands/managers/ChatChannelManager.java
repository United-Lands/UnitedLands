package org.unitedlands.unitedlands.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.ChatChannel;
import org.unitedlands.unitedlands.classes.Citizen;

import org.unitedlands.unitedlands.integrations.papi.PlaceholderAPIIntegration;
import org.unitedlands.utils.United;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
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

    private final UnitedLands plugin;

    private Set<Citizen> viewers = new HashSet<>();
    private Map<UUID, ChatChannel> playerChannels = new HashMap<>();

    public ChatChannelManager(UnitedLands plugin) {
        instance = this;
        this.plugin = plugin;

        registerCommands();
    }

    private void registerCommands() {

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            ArgumentCommandNode<CommandSourceStack, String> messageArgument = Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> {

                        Player sender = (Player) context.getSource().getSender();
                        String usedAlias = context.getNodes().get(0).getNode().getName();
                        String message = StringArgumentType.getString(context, "message");

                        switch (usedAlias) {
                        case "lc", "local", "localchat":
                            sendMessage(sender, ChatChannel.LOCAL, message);
                            break;
                        case "gc", "global", "globalchat":
                            sendMessage(sender, ChatChannel.GLOBAL, message);
                            break;
                        case "stc", "staff", "staffchat":
                            sendMessage(sender, ChatChannel.STAFF, message);
                            break;
                        case "sc", "settlementchat":
                            sendMessage(sender, ChatChannel.SETTLEMENT, message);
                            break;
                        case "cc", "countrychat":
                            sendMessage(sender, ChatChannel.COUNTRY, message);
                            break;
                        default:
                            break;
                        }

                        return Command.SINGLE_SUCCESS;
                    }).build();

            LiteralCommandNode<CommandSourceStack> localChatCommand = Commands.literal("localchat").then(messageArgument).executes(context -> {
                ChatChannelManager.instance().switchChannel((Player) context.getSource().getSender(), ChatChannel.LOCAL);
                return 0;
            }).build();

            LiteralCommandNode<CommandSourceStack> globalChatCommand = Commands.literal("globachat").then(messageArgument).executes(context -> {
                ChatChannelManager.instance().switchChannel((Player) context.getSource().getSender(), ChatChannel.GLOBAL);
                return 0;
            }).build();

            LiteralCommandNode<CommandSourceStack> staffChatCommand = Commands.literal("staffchat").then(messageArgument).executes(context -> {
                ChatChannelManager.instance().switchChannel((Player) context.getSource().getSender(), ChatChannel.STAFF);
                return 0;
            }).build();

            LiteralCommandNode<CommandSourceStack> settlementChatCommand = Commands.literal("settlementchat").then(messageArgument).executes(context -> {
                ChatChannelManager.instance().switchChannel((Player) context.getSource().getSender(), ChatChannel.SETTLEMENT);
                return 0;
            }).build();

            LiteralCommandNode<CommandSourceStack> countryChatCommand = Commands.literal("countrychat").then(messageArgument).executes(context -> {
                ChatChannelManager.instance().switchChannel((Player) context.getSource().getSender(), ChatChannel.COUNTRY);
                return 0;
            }).build();

            commands.register(localChatCommand, "Local chat", List.of("lc", "local"));
            commands.register(globalChatCommand, "Global chat", List.of("gc", "global"));
            commands.register(staffChatCommand, "Staff chat", List.of("stc", "staff"));
            commands.register(settlementChatCommand, "Settlement chat", List.of("sc"));
            commands.register(countryChatCommand, "Country chat", List.of("cc"));

        });

    }

    public void switchChannel(Player player, ChatChannel channel) {

        var senderViewer = viewers.stream().filter(c -> c.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
        if (senderViewer == null) {
            return;
        }

        switch (channel) {
        case COUNTRY:
            if (!senderViewer.hasCountry()) {
                United.messenger().send(player, "general-errors.not-in-country");
                return;
            }
            break;
        case SETTLEMENT:
            if (!senderViewer.hasSettlement()) {
                United.messenger().send(player, "general-errors.not-in-settlement");
                return;
            }
        case STAFF:
            if (!player.hasPermission("united.lands.admin")) {
                United.messenger().send(player, "general-errors.no-permission");
                return;
            }
        default:
            break;

        }

        playerChannels.put(player.getUniqueId(), channel);
        United.messenger().sendRaw(player, "Now talking in <" + channel.getColor() + ">" + channel + "</" + channel.getColor() + ">");
    }

    public ChatChannel getPlayerChannel(Player player) {
        return playerChannels.get(player.getUniqueId());
    }

    public void registerPlayer(Player player) {

        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error("Could not add player " + player.getName() + " to chat because citizen data is missing.", "UnitedLands");
            return;
        }
        viewers.add(citizen);

        playerChannels.put(player.getUniqueId(), ChatChannel.GLOBAL);
    }

    public void unregisterPlayer(Player player) {
        viewers.removeIf(v -> v.getUuid().equals(player.getUniqueId()));
        playerChannels.remove(player.getUniqueId());
    }

    public void sendMessage(Player player, ChatChannel channel, String message) {

        var senderViewer = viewers.stream().filter(c -> c.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
        if (senderViewer == null) {
            return;
        }

        List<Player> receivers = filterViewers(senderViewer, channel);

        if (receivers.size() <= 1) {
            United.messenger().sendRaw(player, "<dark_gray>No one can hear you.</dark_gray>");
        }

        var color = channel.getColor();
        var formattedMessage = getFormattedMessage(channel, message, color, player);

        Audience.audience(receivers).sendMessage(formattedMessage);
    }

    private List<Player> filterViewers(Citizen citizen, ChatChannel channel) {

        switch (channel) {
        case GLOBAL:
            return viewers.stream().map(c -> c.getPlayer()).toList();
        case LOCAL:
            return viewers.stream()
                    .filter(c -> c.getPlayer().getLocation().distanceSquared(citizen.getPlayer().getLocation()) <= 100 * 100)
                    .map(c -> c.getPlayer()).toList();
        case SETTLEMENT:
            return viewers.stream().filter(c -> c.hasSettlement() && c.getSettlement().equals(citizen.getSettlement())).map(c -> c.getPlayer())
                    .toList();
        case COUNTRY:
            return viewers.stream().filter(c -> c.hasCountry() && c.getCountry().equals(citizen.getCountry())).map(c -> c.getPlayer()).toList();
        case STAFF:
            return viewers.stream().filter(c -> c.getPlayer().hasPermission("united.lands.admin")).map(c -> c.getPlayer()).toList();
        default:
            break;
        }
        return new ArrayList<>();
    }

    public void handleMessage(AsyncChatEvent event) {

        var channel = getPlayerChannel(event.getPlayer());
        var senderViewer = viewers.stream().filter(c -> c.getUuid().equals(event.getPlayer().getUniqueId())).findFirst().orElse(null);
        if (senderViewer == null) {
            return;
        }

        var receivers = filterViewers(senderViewer, channel);
        event.viewers().clear();
        event.viewers().addAll(receivers);

        if (receivers.size() <= 1) {
            United.messenger().sendRaw(event.getPlayer(), "<dark_gray>No one can hear you.</dark_gray>");
        }

        var text = PlainTextComponentSerializer.plainText().serialize(event.message());
        var color = channel.getColor();

        event.renderer((source, sourceDisplayName, message, viewer) -> getFormattedMessage(channel, text, color, source));
    }

    private @NotNull TextComponent getFormattedMessage(ChatChannel channel, String text, String color, Player source) {
        var miniMessage = MiniMessage.miniMessage();

        var prefix = channel.getPrefix();
        if (IntegrationManager.instance().usePAPI())
            prefix = PlaceholderAPIIntegration.instance().setPlaceholders(source, prefix);

        return Component.text().append(miniMessage.deserialize(prefix)).append(Component.text(source.getName()))
                .append(miniMessage.deserialize("<dark_gray>: </dark_gray>")).append(miniMessage.deserialize("<" + color + ">" + text + "</" + color + ">"))
                .build();
    }

}
