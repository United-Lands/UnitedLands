package org.unitedlands.unitedlands.commands.handlers.channel;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.ChatChannelManager;

@UnitedSubCommand(
    parent = CmdChannel.class,
    name            = "remove",
    description     = "Removes a chat channel",
    usage           = "/channel remove <channel>",
    permission      = "united.lands.admin",
    playerOnly      = true
)
public class CmdChannelRemove implements UnitedCommandExecutor{

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return ChatChannelManager.instance().getChannelNames();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        
        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        if (!ChatChannelManager.instance().isCustomChannel(args[0])) {
            return;
        }

        ChatChannelManager.instance().removeChannel(args[0]);
    }

}
