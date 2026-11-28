package org.unitedlands.unitedlands.commands.handlers.channel.set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdChannelSet.class,
    name            = "tag",
    description     = "Sets a channel tag",
    usage           = "/channel set tag <tag>",
    playerOnly      = true
)
public class CmdChannelSetTag implements UnitedCommandExecutor {

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

        var tag = player.hasPermission("united.lands.admin")
                ? args[0]
                : United.miniMessage().get().escapeTags(args[0]);

        channel.setTag(tag);
        channel.save();

        United.messenger().send(player, "chat.tag-set", tag);

    }

}
