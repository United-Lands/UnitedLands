package org.unitedlands.unitedlands.commands.handlers.channel;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;

@UnitedCommand(
    name            = "channel",
    aliases         = { "ch", "chan" },
    description     = "Chat channel commands",
    usage           = "/channel <command>",
    playerOnly      = true
)
public class CmdChannel implements UnitedCommandExecutor{

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

    }


}
