package org.unitedlands.unitedlands.commands.handlers.admin;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.registrars.messages.UnitedMessagesRegistrar;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;
import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.managers.PermissionManager;
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
        UnitedLandsTitlesConfig.get().reload();

        UnitedMessagesRegistrar.reload(UnitedLands.instance());
        Pl3xMapRenderer.instance().loadMessages();
        
        PermissionManager.instance().reloadRankPermissions();

        UnitedLands.instance().getWebServices().reloadConfig();

        United.messenger().send(sender, "reload");
    }

    @Override
    public List<String> handleTab(CommandSender arg0, String[] arg1) {
        return null;
    }

}
