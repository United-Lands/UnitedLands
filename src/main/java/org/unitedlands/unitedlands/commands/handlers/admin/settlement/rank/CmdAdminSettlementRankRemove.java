package org.unitedlands.unitedlands.commands.handlers.admin.settlement.rank;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementRank.class,
        name = "remove",
        description = "Removes a rank from a citizen",
        usage = "/ula settlement rank remove <settlement_name> <player> <rank>",
        catchAll = true
)
public class CmdAdminSettlementRankRemove extends AdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        switch (args.length) {
            case 1:
                return UnitedLandsDataManager.instance().getSettlementNames();
            case 2:
                var settlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
                if (settlement != null)
                    return settlement.getCitizens().stream().map(Citizen::getName).collect(Collectors.toList());
            case 3:
                return PermissionManager.instance().getCountryRanks();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 3) {
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

        if (!citizen.hasSettlement() || !settlement.equals(citizen.getSettlement())) {
            United.messenger().send(sender, "admin.settlement.citizen-not-in-settlement", citizen.getName(), settlement.getCleanName());
            return;
        }

        if (!PermissionManager.instance().getSettlementRanks().contains(args[2])) {
            United.messenger().send(sender, "admin.settlement.unknown-rank", args[2]);
            return;
        }

        citizen.removeSettlementRank(args[2]);
        citizen.save();

        United.messenger().send(sender, "admin.settlement.removerank.success", args[2], citizen.getName(), settlement.getCleanName());

    }

}
