package org.unitedlands.unitedlands.commands.handlers.channel;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdChannel.class,
    name            = "remove",
    description     = "Removes a chat channel",
    usage           = "/channel remove <channel>",
    playerOnly      = true
)
public class CmdChannelRemove implements UnitedCommandExecutor{

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            var player = (Player) sender;
            var isAdmin = player.hasPermission("united.lands.admin");
            return ChatChannelManager.instance().getCustomChannels().stream()
                    .filter(c -> isAdmin || player.getUniqueId().equals(c.getCreator()))
                    .map(c -> c.getKey())
                    .toList();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        var player = (Player) sender;

        if (!ChatChannelManager.instance().isCustomChannel(args[0])) {
            United.messenger().send(player, "chat.unknown-channel");
            return;
        }

        var channel = ChatChannelManager.instance().getChannel(args[0]);
        if (!player.hasPermission("united.lands.admin") && !player.getUniqueId().equals(channel.getCreator())) {
            United.messenger().send(player, "chat.not-creator");
            return;
        }

        ChatChannelManager.instance().removeChannel(channel.getKey());
        United.messenger().send(player, "chat.removed", channel.getKey());
    }

}
