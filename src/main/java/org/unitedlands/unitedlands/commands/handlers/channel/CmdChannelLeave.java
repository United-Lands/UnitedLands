package org.unitedlands.unitedlands.commands.handlers.channel;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;

@UnitedSubCommand(
    parent = CmdChannel.class,
    name            = "leave",
    aliases         = { "j" },
    description     = "Leaves the current chat channel",
    usage           = "/channel leave",
    playerOnly      = true
)
public class CmdChannelLeave implements UnitedCommandExecutor{

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        ChatChannelManager.instance().switchChannel((Player) sender, "GLOBAL", null);
    }

}
