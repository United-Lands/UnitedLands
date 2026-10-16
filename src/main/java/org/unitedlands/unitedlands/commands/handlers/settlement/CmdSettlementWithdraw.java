package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.math.BigDecimal;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlement.class,
        name = "withdraw",
        description = "Withdraws money from a settlement's bank account",
        usage = "/settlement withdraw <amount>",
        playerOnly = true
)
public class CmdSettlementWithdraw extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.withdraw");
        if (context == null)
            return;

        BigDecimal amount = new BigDecimal(0);
        try {
            amount = new BigDecimal(args[0]);
        } catch (Exception ex) {
            United.messenger().send(context.player(), "general-errors.wrong-number-format", args[1]);
            return;
        }

        if (!UnitedLandsEconomyManager.instance().has(context.settlement().getUuid(), amount)) {
            United.messenger().send(context.player(), "general-errors.no-funds-settlement", UnitedLandsEconomyManager.instance().format(amount));
            return;
        }

        UnitedLandsEconomyManager.instance().withdraw(context.settlement().getUuid(), amount, "Withdrawn by " + context.player().getName());
        UnitedLandsEconomyManager.instance().deposit(context.player().getUniqueId(), amount, "Withdrawn from " + context.settlement().getCleanName());

        United.messenger().send(context.player(), "player.settlement.withdraw.success", UnitedLandsEconomyManager.instance().format(amount));
    }

}
