package org.unitedlands.unitedlands.commands.handlers.admin.settlement.set;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdAdminSettlementSet.class,
    name            = "name",
    description     = "Changes a settlement's name",
    usage           = "/ula settlement set name <settlement_name> <new_name>"
)
public class CmdAdminSettlementSetName extends SettlementAdminCommandHandler {

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

        var oldname = settlement.getName();

        settlement.setName(args[1]);
        settlement.saveAndRender();

        United.messenger().send(sender, "admin.settlement.setname.success", oldname, settlement.getCleanName());
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getSettlementNames();
        return null;
    }

}
