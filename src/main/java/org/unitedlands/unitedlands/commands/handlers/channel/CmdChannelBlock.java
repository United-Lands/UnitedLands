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
    name            = "block",
    description     = "Blocks a player from joining a channel",
    usage           = "/channel block <player>",
    playerOnly      = true
)
public class CmdChannelBlock implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) { 
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
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

        if (!player.equals(channel.getCreator())) {
            United.messenger().send(player, "chat.not-creator");
            return;
        }

        var targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            United.messenger().send(player, "general-errors.player-not-found");
            return;
        }

        channel.addBlockedPlayer(targetPlayer);

        if (ChatChannelManager.instance().isInChannel(targetPlayer, channel.getKey())) {
            ChatChannelManager.instance().switchChannel(targetPlayer, "GLOBAL", null);
        }

        United.messenger().send(targetPlayer, "chat.got-blocked", channel.getKey());
        United.messenger().send(player, "chat.blocked", targetPlayer.getName());

    }

}
