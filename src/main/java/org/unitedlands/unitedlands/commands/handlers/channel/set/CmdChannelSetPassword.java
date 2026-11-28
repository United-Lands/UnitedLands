package org.unitedlands.unitedlands.commands.handlers.channel.set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdChannelSet.class,
    name            = "password",
    description     = "Sets or removes a channel password",
    usage           = "/channel set password [password]",
    playerOnly      = true
)
public class CmdChannelSetPassword implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        
        if (args.length > 1) {
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

        if (args.length == 0) {
            if (!channel.hasPassword()) {
                United.messenger().send(player, "chat.no-password");
                return;
            }
            channel.removePassword();
            channel.save();
            United.messenger().send(player, "chat.password-removed");
            return;
        }

        channel.setPassword(args[0]);
        channel.save();

        United.messenger().send(player, "chat.password-set");

    }

}
