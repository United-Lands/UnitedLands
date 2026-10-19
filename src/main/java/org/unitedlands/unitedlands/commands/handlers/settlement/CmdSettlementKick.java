package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementPlayerLeaveEvent;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(parent = CmdSettlement.class, name = "kick", description = "Kicks a player from the settlement", usage = "/settlement kick <player>", playerOnly = true)
public class CmdSettlementKick extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            var player = (Player) sender;
            var citizen = getCitizen(player);
            if (citizen == null)
                return null;
            var settlement = citizen.getSettlement();
            if (settlement == null)
                return null;
            return settlement.getCitizens().stream().map(Citizen::getName).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.kick");
        if (context == null)
            return;

        var targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null) {
            United.messenger().send(context.player(), "general-errors.player-not-found", args[0]);
            return;
        }

        if (context.player().equals(targetPlayer)) {
            United.messenger().send(context.player(), "player.settlement.kick.cannot-kick-yourself");
            return;
        }

        var targetCitizen = getCitizen(targetPlayer);
        if (targetCitizen == null)
            return;

        if (!context.settlement().getCitizens().contains(targetCitizen)) {
            United.messenger().send(context.player(), "player.settlement.kick.not-in-settlement");
            return;
        }

        if (targetCitizen.hasCountryRank("leader")) {
            United.messenger().send(context.player(), "player.settlement.kick.is-leader");
            return;
        }

        context.settlement().removeCitizen(targetCitizen);
        targetCitizen.removeSettlementRanks();
        targetCitizen.removeSettlement();
        targetCitizen.save();

        (new SettlementPlayerLeaveEvent(context.settlement(), targetPlayer)).callEvent();

        if (ChatChannelManager.instance().isInChannel(targetCitizen.getPlayer(), "SETTLEMENT")) {
            ChatChannelManager.instance().switchChannel(targetCitizen.getPlayer(), "GLOBAL", null);
        }

        if (targetPlayer.isOnline()) {
            United.messenger().send(targetPlayer, "player.settlement.kick.kicked", context.settlement().getCleanName());
        }

        United.messenger().send(context.player(), "player.settlement.kick.success", targetPlayer.getName());
    }

}
