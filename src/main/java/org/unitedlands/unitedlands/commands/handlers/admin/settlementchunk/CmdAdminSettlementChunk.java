package org.unitedlands.unitedlands.commands.handlers.admin.settlementchunk;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.commands.handlers.admin.CmdAdmin;

@UnitedSubCommand(
        parent = CmdAdmin.class,
        name = "settlementchunk",
        aliases = { "sc", "plot" },
        description = "Admin settlement chunk commands",
        usage = "/ula settlementchunk <command>"
)
public class CmdAdminSettlementChunk implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
    }

}
