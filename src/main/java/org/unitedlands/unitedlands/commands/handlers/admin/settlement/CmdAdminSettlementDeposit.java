package org.unitedlands.unitedlands.commands.handlers.admin.settlement;

import java.math.BigDecimal;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlement.class,
        name = "deposit",
        description = "Deposits money into the settlement bank",
        usage = "/ula settlement deposit <settlement_name> <amount>"
)
public class CmdAdminSettlementDeposit extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getSettlementNames();
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

        BigDecimal amount = new BigDecimal(0);
        try {
            amount = new BigDecimal(args[1]);
        } catch (Exception ex) {
            United.messenger().send(sender, "general-errors.wrong-number-format", args[1]);
            return;
        }

        UnitedLandsEconomyManager.instance().deposit(settlement.getUuid(), amount, sender.getName());
        United.messenger().send(sender, 
            "admin.settlement.deposit.success", 
            UnitedLandsEconomyManager.instance().format(amount), 
            settlement.getCleanName()
        );
        
    }



}
