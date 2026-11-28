package org.unitedlands.unitedlands.commands.handlers.channel;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.ChatChannel;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsChatConfig;
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

        var player = (Player) sender;
        var config = UnitedLandsChatConfig.get();

        var maxLength = config.customChannelMaxNameLength();
        if (!args[0].matches("[A-Za-z0-9_-]{1," + maxLength + "}")) {
            United.messenger().send(sender, "chat.create.invalid-name", String.valueOf(maxLength));
            return;
        }

        if (ChatChannelManager.instance().channelExists(args[0])) {
            United.messenger().send(sender, "chat.create.channel-exists");
            return;
        }

        var maxChannels = config.customChannelMaxPerPlayer();
        if (!player.hasPermission("united.lands.admin")
                && ChatChannelManager.instance().getChannelCountByCreator(player.getUniqueId()) >= maxChannels) {
            United.messenger().send(sender, "chat.create.limit-reached", String.valueOf(maxChannels));
            return;
        }

        var customChannelColor = UnitedLandsChatConfig.get().customChannelColor();

        var chatChannel = new ChatChannel(
                args[0], 
                args[0], 
                customChannelColor, 
                null, 
                true,
                true);

        chatChannel.setCreator(((Player) sender).getUniqueId());
        chatChannel.setLastActivity(System.currentTimeMillis());

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
