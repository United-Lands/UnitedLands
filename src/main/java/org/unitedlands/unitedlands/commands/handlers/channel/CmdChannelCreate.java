package org.unitedlands.unitedlands.commands.handlers.channel;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.ChatChannel;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdChannel.class,
    name            = "create",
    aliases         = { "c" },
    description     = "Creates a chat channel",
    usage           = "/channel create <name> [password]",
    playerOnly      = true
)
public class CmdChannelCreate implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return ;
        }

        if (ChatChannelManager.instance().getChannelNames().contains(args[0])) {
            United.messenger().send(sender, "chat.create.channel-exists");
            return;
        }

        var customChannelColor = UnitedLandsConfig.get().chat().customChannelColor();

        var chatChannel = new ChatChannel(
                args[0], 
                args[0], 
                customChannelColor, 
                null, 
                true,
                true);

        chatChannel.setCreator((Player) sender);

        if (args.length == 2)
            chatChannel.setPassword(args[1]);

        ChatChannelManager.instance().registerChannel(chatChannel);

        if (!chatChannel.hasPassword()) {
            United.messenger().send(sender, "chat.create.success", chatChannel.getKey());
        }
        else {
            United.messenger().send(sender, "chat.create.success-private", chatChannel.getKey(), chatChannel.getPassword());
        }

        ChatChannelManager.instance().switchChannel((Player) sender, args[0], chatChannel.getPassword());
    }

}
