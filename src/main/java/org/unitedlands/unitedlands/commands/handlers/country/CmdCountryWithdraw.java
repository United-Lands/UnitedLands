package org.unitedlands.unitedlands.commands.handlers.country;

import java.math.BigDecimal;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.BankRecordTransactionType;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdCountry.class,
    name            = "withdraw",
    description     = "Withdraws money from the country bank account",
    usage           = "/country withdraw <amount>",
    playerOnly      = true
)
public class CmdCountryWithdraw extends CountryCommandHandler {

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

        var context = validate(sender, "country.withdraw");
        if (context == null)
            return;

        BigDecimal amount = new BigDecimal(0);
        try {
            amount = new BigDecimal(args[0]);
        } catch (Exception ex) {
            United.messenger().send(context.player(), "general-errors.wrong-number-format", args[1]);
            return;
        }

        if (!UnitedLandsEconomyManager.instance().has(context.country().getUuid(), amount)) {
            United.messenger().send(context.player(), "general-errors.no-funds-country", UnitedLandsEconomyManager.instance().format(amount));
            return;
        }

        UnitedLandsEconomyManager.instance().withdraw(context.country().getUuid(), amount, BankRecordTransactionType.WITHDRAWAL, "Withdrawn by " + context.player().getName());
        UnitedLandsEconomyManager.instance().deposit(context.player().getUniqueId(), amount, BankRecordTransactionType.WITHDRAWAL, "Withdrawn from " + context.country().getName());

        United.messenger().send(context.player(), "player.country.withdraw.success", UnitedLandsEconomyManager.instance().format(amount));
    }

}
