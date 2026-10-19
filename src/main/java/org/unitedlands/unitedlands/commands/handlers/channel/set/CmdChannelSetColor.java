package org.unitedlands.unitedlands.commands.handlers.channel.set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdChannelSet.class,
    name            = "color",
    description     = "Sets a channel color",
    usage           = "/channel set color <hexcolor>",
    playerOnly      = true
)
public class CmdChannelSetColor implements UnitedCommandExecutor {

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

        if (!ColorUtils.isValidHexColor(args[0])) {
            United.messenger().send(player, "general-errors.wrong-color-format");
            return;
        }

        channel.setColor(args[0]);
        channel.save();
        
        United.messenger().send(player, "chat.color-set", args[0]);

    }

}
