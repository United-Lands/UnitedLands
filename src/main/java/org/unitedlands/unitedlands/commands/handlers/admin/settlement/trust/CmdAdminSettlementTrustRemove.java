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
        name = "remove",
        description = "Removes settlement trust from a player",
        usage = "/ula settlement trust remove <settlement> <player>",
        playerOnly = true
)
public class CmdAdminSettlementTrustRemove extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getSettlementNames(); 
        } else if (args.length == 2) {
            var settlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
            if (settlement != null)
                return settlement.getTrustList().stream().map(Citizen::getName).toList();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var settlement = getSettlement(sender, args[0]);
        if (settlement == null) {
            return;
        }

        var citizen = getCitizen(sender, args[1]);
        if (citizen == null) {
            return;
        }

        if (!settlement.getTrustList().contains(citizen)) {
            United.messenger().send(sender, "admin.settlement.removetrust.not-trusted", args[1]);
            return;
        }

        settlement.removeTrusted(citizen);
        settlement.save();

        if (citizen.getPlayer().isOnline()) {
            United.messenger().send(citizen.getPlayer(), "player.settlement.removetrust.untrusted", settlement.getCleanName());
        }

        United.messenger().send(sender, "admin.settlement.removetrust.success", citizen.getName(), settlement.getCleanName());
    }

}
