package org.unitedlands.unitedlands.commands.handlers.settlement.rank;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.unitedlands.managers.ConfirmationManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementRank.class,
        name = "add",
        description = "Add a settlement rank to a citizen",
        usage = "/settlement rank add <player> <rank>",
        playerOnly = true,
        catchAll = true
)
public class CmdSettlementRankAdd extends SettlementCommandHandler {

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
                return PermissionManager.instance().getSettlementRanks();
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
        if (context == null)
            return;

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
            United.messenger().send(context.player(), "admin.settlement.unknown-rank", args[1]);
            return;
        }

        if (targetCitizen.getSettlementRanks().contains(args[1])) {
            United.messenger().send(context.player(), "player.settlement.addrank.rank-already-owned", targetCitizen.getName(), args[1]);
            return;
        }

        if (args[1].equals("mayor")) {

            if (!hasPermission("settlement.manage.ranks.mayor", context.citizen()))
                return;

            var confirmation = new Confirmation("new-mayor");
            confirmation.setRunnable(() -> {

                var currentMayor = context.settlement().getMayor();
                currentMayor.removeSettlementRank("mayor");
                currentMayor.save();

                targetCitizen.addSettlementRank("mayor");
                targetCitizen.save();

                if (currentMayor.getPlayer().isOnline()) {
                    United.messenger().send(currentMayor.getPlayer(),
                            "player.settlement.removerank.lost", "mayor");
                }
                if (targetPlayer.isOnline()) {
                    United.messenger().send(targetPlayer, "player.settlement.addrank.rank-received", "mayor");
                }

            })
                    .setTitle(United.messenger().get("player.settlement.addrank.confirm-new-mayor", targetCitizen.getName()))
                    .setSender(context.player())
                    .setReceiver(context.player())
                    .send();

            ConfirmationManager.instance().queueConfirmation(confirmation);

        } else {

            if (!hasPermission("settlement.manage.ranks.other", context.citizen()))
                return;

            targetCitizen.addSettlementRank(args[1]);
            targetCitizen.save();

            if (targetPlayer.isOnline()) {
                United.messenger().send(targetPlayer, "player.settlement.addrank.rank-received", args[1]);
            }

        }

        United.messenger().send(context.player(), "player.settlement.addrank.success", args[1], targetCitizen.getName());

    }

}
