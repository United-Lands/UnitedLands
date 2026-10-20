package org.unitedlands.unitedlands.commands.handlers.admin.settlement.trust;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementTrust.class,
        name = "list",
        description = "Shows the settlement trust list",
        usage = "/ula settlement trust list <settlement>",
        playerOnly = true
)
public class CmdSettlementTrustList extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getSettlementNames(); 
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var settlement = getSettlement(sender, args[0]);
        if (settlement == null) {
            return;
        }

        var trustList = settlement.getTrustList();
        var trustListStr = String.join(", ", trustList.stream().map(Citizen::getName).toList());

        United.messenger().send(sender, "player.settlement.trustlist", settlement.getCleanName(), trustListStr);
    }

}
