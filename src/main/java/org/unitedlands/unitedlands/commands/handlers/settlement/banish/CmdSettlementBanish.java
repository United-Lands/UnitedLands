package org.unitedlands.unitedlands.commands.handlers.settlement.banish;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.commands.handlers.settlement.CmdSettlement;

@UnitedSubCommand(
    parent          = CmdSettlement.class,
    name            = "banish",
    description     = "Manages settlement banishment",
    usage           = "/settlement banish <add|remove|list> [player]",
    playerOnly      = true
)
public class CmdSettlementBanish implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
    }

}
