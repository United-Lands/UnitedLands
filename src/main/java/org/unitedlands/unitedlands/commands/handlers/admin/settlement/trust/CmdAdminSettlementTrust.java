package org.unitedlands.unitedlands.commands.handlers.admin.settlement.trust;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.commands.handlers.admin.settlement.CmdAdminSettlement;

@UnitedSubCommand(
    parent          = CmdAdminSettlement.class,
    name            = "trust",
    description     = "Manages settlement trust",
    usage           = "/ula settlement trust <add|remove|list> <settlement> [player]",
    playerOnly      = true
)
public class CmdAdminSettlementTrust implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
    }

}
