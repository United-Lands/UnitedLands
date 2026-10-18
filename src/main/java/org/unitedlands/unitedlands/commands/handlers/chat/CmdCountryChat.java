package org.unitedlands.unitedlands.commands.handlers.chat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;

@UnitedCommand(
    name = "coutrychat", 
    aliases = { "cc" }, 
    playerOnly = true)
public class CmdCountryChat implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            ChatChannelManager.instance().switchChannel((Player) sender, "COUNTRY", null);
        } else {
            ChatChannelManager.instance().sendMessage((Player) sender, "COUNTRY", String.join(" ", args));
        }

    }

}
