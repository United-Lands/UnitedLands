package org.unitedlands.unitedlands.commands.handlers.admin.settlement.set;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.commands.handlers.admin.settlement.CmdAdminSettlement;

@UnitedSubCommand(
    parent          = CmdAdminSettlement.class,
    name            = "set",
    description     = "Admin settlement set commands",
    usage           = "/ula settlement set <command> <argument>"
)
public class CmdAdminSettlementSet implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) { }

}
