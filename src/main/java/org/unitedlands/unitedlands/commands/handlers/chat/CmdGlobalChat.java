package org.unitedlands.unitedlands.commands.handlers.chat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;

@UnitedCommand(
    name = "globalchat", 
    aliases = { "gc" }, 
    playerOnly = true)
public class CmdGlobalChat implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            ChatChannelManager.instance().switchChannel((Player) sender, "GLOBAL", null);
        } else {
            ChatChannelManager.instance().sendMessage((Player) sender, "GLOBAL", String.join(" ", args));
        }

    }

}
