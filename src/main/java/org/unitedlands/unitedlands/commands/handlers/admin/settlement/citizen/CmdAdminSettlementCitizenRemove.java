package org.unitedlands.unitedlands.commands.handlers.admin.settlement.citizen;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementPlayerLeaveEvent;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementCitizen.class,
        name = "remove",
        description = "Removes a citizen from a settlement",
        usage = "/ula settlement citizen remove <settlement_name> <player>",
        catchAll = true
)
public class CmdAdminSettlementCitizenRemove extends AdminCommandHandler {

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

        if (citizen.getSettlement() == null || !citizen.getSettlement().equals(settlement)) {
            United.messenger().send(sender, "admin.settlement.citizen-not-in-settlement", citizen.getName(), settlement.getCleanName());
            return;
        }

        settlement.removeCitizen(citizen);
        settlement.saveAndRender();

        citizen.removeCountryRanks();
        citizen.removeSettlementRanks();
        citizen.removeSettlement();
        citizen.save();

        (new SettlementPlayerLeaveEvent(settlement, citizen.getPlayer().getPlayer())).callEvent();

        United.messenger().send(sender, "admin.settlement.removecitizen.success", citizen.getName(), settlement.getCleanName());
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return UnitedLandsDataManager.instance().getSettlementNames();
            case 2:
                var settlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
                return settlement.getCitizens().stream().map(Citizen::getName).collect(Collectors.toList());
        }
        return null;
    }

}
