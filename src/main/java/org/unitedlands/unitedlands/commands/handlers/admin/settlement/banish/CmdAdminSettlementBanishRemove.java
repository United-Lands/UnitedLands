package org.unitedlands.unitedlands.commands.handlers.admin.settlement.banish;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdAdminSettlementBanish.class,
    name            = "remove",
    description     = "Removes settlement banishment from a player",
    usage           = "/ula settlement banish remove <settlement> <player>",
    catchAll        = true
)
public class CmdAdminSettlementBanishRemove extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getSettlementNames(); 
        } else if (args.length == 2) {
            var settlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
            if (settlement != null)
                return settlement.getBanishList().stream().map(Citizen::getName).toList();
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

        if (!settlement.getBanishList().contains(citizen)) {
            United.messenger().send(sender, "admin.settlement.removebanish.not-banished", args[1]);
            return;
        }

        settlement.removeBanished(citizen);
        settlement.save();

        if (citizen.getPlayer().isOnline()) {
            United.messenger().send(citizen.getPlayer(), "player.settlement.removebanish.unbanished", settlement.getCleanName());
        }

        United.messenger().send(sender, "admin.settlement.removebanish.success", citizen.getName(), settlement.getCleanName());
    }

}
