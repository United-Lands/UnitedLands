package org.unitedlands.unitedlands.commands.handlers.settlement.rank;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementRank.class,
        name = "remove",
        description = "Removed a settlement rank from a citizen",
        usage = "/settlement rank remove <player> <rank>",
        playerOnly = true,
        catchAll = true
)
public class CmdSettlementRankRemove extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        var citizen = getCitizen((Player) sender);
        if (citizen == null)
            return null;
        var settlement = getCitizenSettlement(citizen);
        if (settlement == null)
            return null;

        switch (args.length) {
            case 1:
                return settlement.getCitizens().stream().map(Citizen::getName).collect(Collectors.toList());
            case 2:
                var targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer == null)
                    return null;
                var targetCitizen = UnitedLandsDataManager.instance().getCitizen(targetPlayer);
                if (targetCitizen == null)
                    return null;
                return targetCitizen.getSettlementRanks().stream().collect(Collectors.toList());
            default:
                break;
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        // Concrete permission checks are further down
        var context = validate(sender, null);

        var targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            United.messenger().send(context.player(), "general-errors.player-not-found", args[0]);
            return;
        }

        var targetCitizen = getCitizen(targetPlayer);
        if (targetCitizen == null)
            return;

        if (!context.settlement().equals(targetCitizen.getSettlement())) {
            United.messenger().send(context.player(), "player.settlement.not-in-settlement", args[0]);
            return;
        }

        if (!PermissionManager.instance().getSettlementRanks().contains(args[1])) {
            United.messenger().send(context.player(), "admin.settlement.unknown-rank",  args[1]);
            return;
        }

        if (!targetCitizen.getSettlementRanks().contains(args[1])) {
            United.messenger().send(context.player(), "player.settlement.removerank.rank-not-owned", args[1], targetCitizen.getName());
            return;
        }

        if (args[1].equals("mayor")) {

            if (!hasPermission("settlement.manage.ranks.mayor", context.citizen()))
                return;

            United.messenger().send(context.player(), "player.settlement.removerank.cannot-remove-mayor");
            return;

        } else {

            if (!hasPermission("settlement.manage.ranks.other", context.citizen()))
                return;

            targetCitizen.removeSettlementRank(args[1]);
            targetCitizen.save();

            if (targetPlayer.isOnline()) {
                United.messenger().send(targetPlayer, "player.settlement.removerank.lost", args[1]);
            }

        }

        United.messenger().send(context.player(), "player.settlement.removerank.success", args[1], targetCitizen.getName());

    }

}
