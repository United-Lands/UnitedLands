package org.unitedlands.unitedlands.commands.handlers.admin.cmi;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.commands.handlers.admin.CmdAdmin;

@UnitedSubCommand(
        parent = CmdAdmin.class,
        name = "cmi",
        description = "Admin CMI commands",
        usage = "/ula cmi <command>"
)
public class CmdAdminCmi implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

    }

}
