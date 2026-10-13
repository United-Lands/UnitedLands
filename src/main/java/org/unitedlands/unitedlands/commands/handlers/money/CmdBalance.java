package org.unitedlands.unitedlands.commands.handlers.money;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedCommand (
    name = "balance",
    aliases = { "bal" },
    usage = "/balance [player]",
    description = "Displays a player's balance",
    playerOnly = true
)
public class CmdBalance implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        UUID targetPlayerUuid = player.getUniqueId();
        String targetPlayerName = player.getName();

        if (args.length > 0) {
            var targetOfflinePlayer = Bukkit.getOfflinePlayer(args[0]);
            if (targetOfflinePlayer.getLastLogin() == 0) {
                United.messenger().send(player, "general-errors.player-not-found", args[0]);
                return;
            }
            targetPlayerUuid = targetOfflinePlayer.getUniqueId();
            targetPlayerName = targetOfflinePlayer.getName();
        } 

        var balance = UnitedLandsEconomyManager.instance().format(UnitedLandsEconomyManager.instance().getBalance(targetPlayerUuid)); 

        United.messenger().send(sender, "player.money.balance", targetPlayerName, balance);
    }

}
