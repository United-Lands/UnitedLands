package org.unitedlands.unitedlands.commands.handlers.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.tasks.NewDayTask;

@UnitedSubCommand(
        parent = CmdAdmin.class,
        name = "forcenewday",
        description = "Admin force new day command",
        usage = "/ula forcenewday"
)


public class CmdAdminForceNewDay implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        Bukkit.getScheduler().runTask(UnitedLands.instance(), new NewDayTask());
    }

}
