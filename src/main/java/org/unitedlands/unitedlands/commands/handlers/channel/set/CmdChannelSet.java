package org.unitedlands.unitedlands.commands.handlers.channel.set;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.commands.handlers.channel.CmdChannel;

@UnitedSubCommand(
    parent = CmdChannel.class,
    name            = "set",
    usage           = "/channel set <command> <arg>",
    playerOnly      = true
)
public class CmdChannelSet implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
    }

}
