package org.unitedlands.unitedlands.commands.handlers.settlement.banish;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementBanish.class,
        name = "add",
        description = "Adds settlement banishment to a player",
        usage = "/settlement banish add <player>",
        playerOnly = true
)
public class CmdSettlementBanishAdd extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.addbanish");
        if (context == null)
            return;

        var targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null) {
            United.messenger().send(context.player(), "general-errors.player-not-found", args[0]);
            return;
        }

        if (context.player().equals(targetPlayer)) {
            United.messenger().send(context.player(), "player.settlement.addbanish.cannot-banish-yourself");
            return;
        }

        var targetCitizen = getCitizen(targetPlayer);
        if (targetCitizen == null)
            return;

        context.settlement().addBanished(targetCitizen);
        context.settlement().save();

        if (targetPlayer.isOnline()) {
            United.messenger().send(targetPlayer, "player.settlement.addbanish.banished", context.settlement().getCleanName());
        }

        United.messenger().send(context.player(), "player.settlement.addbanish.success", targetPlayer.getName());
    }

}
