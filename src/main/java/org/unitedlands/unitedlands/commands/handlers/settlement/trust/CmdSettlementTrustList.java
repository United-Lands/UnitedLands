package org.unitedlands.unitedlands.commands.handlers.settlement.trust;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementTrust.class,
        name = "list",
        description = "Shows the settlement trust list",
        usage = "/settlement trust list",
        playerOnly = true
)
public class CmdSettlementTrustList extends SettlementCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, null);
        if (context == null)
            return;

        var trustList = context.settlement().getTrustList();
        var trustListStr = String.join(", ", trustList.stream().map(Citizen::getName).toList());

        United.messenger().send(context.player(), "player.settlement.trustlist", context.settlement().getCleanName(), trustListStr);
    }

}
