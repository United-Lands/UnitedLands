package org.unitedlands.unitedlands.commands.handlers.channel;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdChannel.class,
    name            = "list",
    description     = "Gets the chat channel list",
    usage           = "/channel list",
    playerOnly      = true
)
public class CmdChannelList implements UnitedCommandExecutor{

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        United.messenger().sendRaw(sender, String.join(", ", ChatChannelManager.instance().getPublicChannelNames()));
    }

}
