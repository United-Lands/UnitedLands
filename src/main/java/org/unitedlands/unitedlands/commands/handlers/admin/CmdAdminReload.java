package org.unitedlands.unitedlands.commands.handlers.admin;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.registrars.messages.UnitedMessagesRegistrar;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsChatConfig;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;
import org.unitedlands.unitedlands.managers.IntegrationManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.TitleManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdmin.class,
        name = "reload",
        description = "Admin reload command",
        usage = "/ula reload"
)
public class CmdAdminReload implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        UnitedLandsConfig.get().reload();
        UnitedLandsChatConfig.get().reload();
        UnitedLandsTitlesConfig.get().reload();

        UnitedMessagesRegistrar.reload(UnitedLands.instance());
        IntegrationManager.instance().getMapRenderer().loadMessages();

        TitleManager.instance().loadSettings();
        PermissionManager.instance().loadSettings();
        
        UnitedLands.instance().getWebServices().reloadConfig();

        United.messenger().send(sender, "reload");
    }


}
