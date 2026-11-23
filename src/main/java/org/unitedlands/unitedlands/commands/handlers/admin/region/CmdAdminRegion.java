package org.unitedlands.unitedlands.commands.handlers.admin.region;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.commands.handlers.admin.CmdAdmin;

@UnitedSubCommand(
        parent = CmdAdmin.class,
        name = "region",
        aliases = { "r" },
        description = "Admin region commands",
        usage = "/ula region <command>"
)
public class CmdAdminRegion implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

    }

}
