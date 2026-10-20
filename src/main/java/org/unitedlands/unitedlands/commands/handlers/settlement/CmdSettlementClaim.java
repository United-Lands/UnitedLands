package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

@UnitedSubCommand(
    parent          = CmdSettlement.class,
    name            = "claim",
    description     = "Claims a chunk for the settlement",
    usage           = "/settlement claim",
    playerOnly      = true
)
public class CmdSettlementClaim extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "settlement.claim");
        if (context == null)
            return;

        context.settlement().tryClaimChunk(context.player(), context.player().getLocation());
        
    }

}
