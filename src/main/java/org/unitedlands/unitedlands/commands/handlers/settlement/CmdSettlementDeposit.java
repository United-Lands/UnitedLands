package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.math.BigDecimal;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(parent = CmdSettlement.class, name = "deposit", description = "Deposits money in a settlement's bank account", usage = "/settlement deposit <amount>", playerOnly = true)
public class CmdSettlementDeposit extends SettlementCommandHandler {

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

        var citizen = getCitizen((Player) sender);
        if (citizen == null)
            return;

        var settlement = getCitizenSettlement(citizen);
        if (settlement == null)
            return;

        BigDecimal amount = new BigDecimal(0);
        try {
            amount = new BigDecimal(args[0]);
        } catch (Exception ex) {
            United.messenger().send(player, "general-errors.wrong-number-format", args[1]);
            return;
        }

        if (!UnitedLandsEconomyManager.instance().has(player.getUniqueId(), amount)) {
            United.messenger().send(player, "general-errors.no-funds-player",
                    UnitedLandsEconomyManager.instance().format(amount));
            return;
        }

        UnitedLandsEconomyManager.instance().withdraw(player.getUniqueId(), amount,
                "Deposit to " + settlement.getCleanName());
        UnitedLandsEconomyManager.instance().deposit(settlement.getUuid(), amount, "Deposited by " + player.getName());
        
        United.messenger().send(player, "player.settlement.deposit.success",
                UnitedLandsEconomyManager.instance().format(amount));

        if (settlement.isBankrupt()) {
            var threshold = BigDecimal.valueOf(UnitedLandsConfig.get().economy().costs().settlement().bankruptcyStopThreshold());
            if (UnitedLandsEconomyManager.instance().getBalance(settlement.getUuid()).compareTo(threshold) >= 0) {
                settlement.setBankrupt(false);
                settlement.setBankruptcyTimestamp(null);
                settlement.save();
                United.messenger().send(player, "player.settlement.deposit.bankruptcy-stopped",
                        UnitedLandsEconomyManager.instance().format(amount));
            }
        }


    }

}
