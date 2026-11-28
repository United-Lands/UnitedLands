package org.unitedlands.unitedlands.commands.handlers.channel;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdChannel.class,
    name            = "unblock",
    description     = "Unblocks a player from joining a channel",
    usage           = "/channel unblock <player>",
    playerOnly      = true
)
public class CmdChannelUnblock implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) { 
            var channel = ChatChannelManager.instance().getPlayerChannel((Player) sender);
            if (channel != null) {
                return channel.getBlockedPlayers().stream()
                        .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                        .filter(Objects::nonNull)
                        .toList();
            }
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        
        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var player = (Player) sender;

        var channel = ChatChannelManager.instance().getPlayerChannel(player);
        if (channel == null) {
            United.messenger().send(player, "chat.not-in-channel");
            return;
        }

        if (!player.getUniqueId().equals(channel.getCreator())) {
            United.messenger().send(player, "chat.not-creator");
            return;
        }

        var target = channel.getBlockedPlayers().stream()
                .map(Bukkit::getOfflinePlayer)
                .filter(p -> args[0].equalsIgnoreCase(p.getName()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            United.messenger().send(player, "chat.not-blocked");
            return;
        }

        channel.removeBlockedPlayer(target.getUniqueId());
        channel.save();

        var onlineTarget = target.getPlayer();
        if (onlineTarget != null)
            United.messenger().send(onlineTarget, "chat.got-unblocked", channel.getKey());
        United.messenger().send(player, "chat.unblocked", target.getName());

    }

}
