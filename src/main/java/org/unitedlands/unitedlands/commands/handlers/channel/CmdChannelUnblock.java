package org.unitedlands.unitedlands.commands.handlers.channel;

import java.util.List;

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
                return channel.getBlockedPlayers().stream().map(uuid -> Bukkit.getPlayer(uuid).getName()).toList();
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

        var targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            United.messenger().send(player, "general-errors.player-not-found");
            return;
        }

        if (!channel.isBlocked(targetPlayer.getUniqueId())) {
            United.messenger().send(player, "chat.not-blocked");
            return;
        }

        channel.removeBlockedPlayer(targetPlayer.getUniqueId());
        channel.save();

        United.messenger().send(targetPlayer, "chat.got-unblocked", channel.getKey());
        United.messenger().send(player, "chat.unblocked", targetPlayer.getName());

    }

}
