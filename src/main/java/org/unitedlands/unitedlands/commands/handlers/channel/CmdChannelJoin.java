package org.unitedlands.unitedlands.commands.handlers.channel;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;

@UnitedSubCommand(
    parent = CmdChannel.class,
    name            = "join",
    aliases         = { "j" },
    description     = "Switches the chat channel",
    usage           = "/channel join <channel>",
    playerOnly      = true
)
public class CmdChannelJoin implements UnitedCommandExecutor{

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return ChatChannelManager.instance().getPublicChannelNames();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        
        if (args.length < 1)
            return;

        ChatChannelManager.instance().switchChannel((Player) sender, args[0], args.length > 1 ? args[1] : null);
    }

}
