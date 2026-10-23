package org.unitedlands.unitedlands.commands.handlers.admin.settlement;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlement.class,
        name = "merge",
        description = "Merges the target settlement into the prevailing settlement",
        usage = "/settlement merge <prevailing_settlement> <dissolving_settlement>",
        catchAll = true
)
public class CmdAdminSettlementMerge implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var sourceSettlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
        if (sourceSettlement == null) {
            United.messenger().send(sender, "general-errors.settlement-not-found", args[0]);
            return;
        }

        var targetSettlement = UnitedLandsDataManager.instance().getSettlement(args[1]);
        if (targetSettlement == null) {
            United.messenger().send(sender, "general-errors.settlement-not-found", args[1]);
            return;
        }

        UnitedLandsDataManager.instance().mergeSettlements(sourceSettlement, targetSettlement);

        // TODO: Message
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length <= 2) {
            return UnitedLandsDataManager.instance().getSettlementNames();
        }
        return null;
    }
}