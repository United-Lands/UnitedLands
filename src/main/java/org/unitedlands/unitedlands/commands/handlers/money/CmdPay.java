package org.unitedlands.unitedlands.commands.handlers.money;

import java.math.BigDecimal;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedCommand (
    name = "paymoney",
    aliases = { "pay" },
    usage = "/pay <player> <amount>",
    description = "Pays money to a player",
    playerOnly = true
)
public class CmdPay implements UnitedCommandExecutor {

    
    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        var targetPlayer = Bukkit.getOfflinePlayer(args[0]);
        if (targetPlayer.getLastLogin() == 0) {
            United.messenger().send(player, "general-errors.player-not-found", args[0]);
            return;
        }

        BigDecimal amount = new BigDecimal(0);
        try {
            amount = new BigDecimal(args[1]);
        } catch (Exception ex) {
            United.messenger().send(player, "general-errors.wrong-number-format", args[1]);
            return;
        }

        if (!UnitedLandsEconomyManager.instance().has(player.getUniqueId(), amount)) {
            United.messenger().send(player, "general-errors.no-funds-player", UnitedLandsEconomyManager.instance().format(amount));
            return;
        }

        UnitedLandsEconomyManager.instance().withdraw(player.getUniqueId(), amount, "Payment to " + targetPlayer.getName());
        UnitedLandsEconomyManager.instance().deposit(targetPlayer.getUniqueId(), amount, "Payment from " + player.getName());

        United.messenger().send(player, "player.money.pay.success", UnitedLandsEconomyManager.instance().format(amount), targetPlayer.getName());
        if (targetPlayer.isOnline()) {
            United.messenger().send(player, "player.money.pay.received", UnitedLandsEconomyManager.instance().format(amount), player.getName());
        }

    }

}
