package org.unitedlands.unitedlands.commands.handlers.admin.settlement.citizen;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementCitizen.class,
        name = "add",
        description = "Adds a citizen to a settlement",
        usage = "/ula settlement citizen add <settlement_name> <player>",
        catchAll = true
)
public class CmdAdminSettlementCitizenAdd extends AdminCommandHandler {

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

        if (citizen.getSettlement() != null) {
            United.messenger().send(sender, "admin.settlement.addcitizen.already-in-settlement", citizen.getName(), citizen.getSettlement().getCleanName());
            return;
        }

        settlement.addCitizen(citizen);
        settlement.saveAndRender();

        citizen.setSettlement(settlement);
        citizen.save();
        
        United.messenger().send(sender, "admin.settlement.addcitizen.success", citizen.getName(), settlement.getCleanName());
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return UnitedLandsDataManager.instance().getSettlementNames();
            case 2:
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return null;
    }

}
