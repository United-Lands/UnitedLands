package org.unitedlands.unitedlands.commands.handlers.settlement.banish;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementBanish.class,
        name = "list",
        description = "Shows the settlement banish list",
        usage = "/settlement banish list",
        playerOnly = true
)
public class CmdSettlementBanishList extends SettlementCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, null);
        if (context == null)
            return;

        var trustList = context.settlement().getBanishList();
        var trustListStr = String.join(", ", trustList.stream().map(Citizen::getName).toList());

        United.messenger().send(context.player(), "player.settlement.banishlist", context.settlement().getCleanName(), trustListStr);
    }

}
