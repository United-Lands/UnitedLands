package org.unitedlands.unitedlands.commands.handlers.admin.settlement.banish;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.commands.handlers.admin.settlement.CmdAdminSettlement;

@UnitedSubCommand(
    parent          = CmdAdminSettlement.class,
    name            = "banish",
    description     = "Manages settlement banishment",
    usage           = "/ula settlement banish <add|remove|list> <settlement> [player]"
)
public class CmdAdminSettlementBanish implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
    }

}
