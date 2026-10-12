package org.unitedlands.unitedlands.commands.handlers.admin.country;

import java.math.BigDecimal;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountry.class,
        name = "deposit",
        description = "Deposits money into a country bank",
        usage = "/ula country deposit <country_name> <amount>",
        catchAll = true
)
public class CmdAdminCountryDeposit extends CountryAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender arg0, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getCountryNames();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        var country = getCountry(sender, args[0]);
        if (country == null) {
            return;
        }

        BigDecimal amount = new BigDecimal(0);
        try {
            amount = new BigDecimal(args[1]);
        } catch (Exception ex) {
            United.messenger().send(sender, "general-errors.wrong-number-format", args[1]);
            return;
        }

        UnitedLandsEconomyManager.instance().deposit(country.getUuid(), amount, sender.getName());
        United.messenger().send(sender, 
            "admin.country.deposit.success", 
            UnitedLandsEconomyManager.instance().format(amount), 
            country.getCleanName()
        );

    }

}
