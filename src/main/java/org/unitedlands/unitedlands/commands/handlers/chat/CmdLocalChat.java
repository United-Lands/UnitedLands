package org.unitedlands.unitedlands.commands.handlers.chat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;

@UnitedCommand(
    name = "localchat", 
    aliases = { "lc" }, 
    playerOnly = true)
public class CmdLocalChat implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            ChatChannelManager.instance().switchChannel((Player) sender, "LOCAL", null);
        } else {
            ChatChannelManager.instance().sendMessage((Player) sender, "LOCAL", String.join(" ", args));
        }

    }

}
