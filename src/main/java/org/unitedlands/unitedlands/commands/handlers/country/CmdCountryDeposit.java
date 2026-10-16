package org.unitedlands.unitedlands.commands.handlers.country;

import java.math.BigDecimal;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountry.class,
        name = "deposit",
        description = "Deposits money in the country bank account",
        usage = "/country deposit <amount>",
        playerOnly = true
)
public class CmdCountryDeposit extends CountryCommandHandler {

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

        // Don't use the context for this command to avoid bankruptcy checks

        var player = (Player) sender;

        var citizen = getCitizen(player);
        if (citizen == null)
            return;

        var country = getCitizenCountry(citizen);
        if (country == null)
            return;

        BigDecimal amount = new BigDecimal(0);
        try {
            amount = new BigDecimal(args[0]);
        } catch (Exception ex) {
            United.messenger().send(player, "general-errors.wrong-number-format", args[0]);
            return;
        }

        if (!UnitedLandsEconomyManager.instance().has(player.getUniqueId(), amount)) {
            United.messenger().send(player, "general-errors.no-funds-player", UnitedLandsEconomyManager.instance().format(amount));
            return;
        }

        UnitedLandsEconomyManager.instance().withdraw(player.getUniqueId(), amount, "Deposit to " + country.getName());
        UnitedLandsEconomyManager.instance().deposit(country.getUuid(), amount, "Deposit by " + player.getName());
        
        United.messenger().send(player, "player.country.deposit.success", UnitedLandsEconomyManager.instance().format(amount));

        if (country.isBankrupt()) {
            var threshold = BigDecimal.valueOf(UnitedLandsConfig.get().economy().costs().settlement().bankruptcyStopThreshold());
            if (UnitedLandsEconomyManager.instance().getBalance(country.getUuid()).compareTo(threshold) >= 0) {
                country.setBankrupt(false);
                country.setBankruptcyTimestamp(null);
                country.save();
                United.messenger().send(player, "player.country.deposit.bankruptcy-stopped",
                        UnitedLandsEconomyManager.instance().format(amount));
            }
        }


    }

}
