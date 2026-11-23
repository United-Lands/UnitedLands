package org.unitedlands.unitedlands.commands.handlers.country.rank;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;

import org.unitedlands.unitedlands.managers.ConfirmationManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountryRank.class,
        name = "add",
        description = "Adds a country rank to a citizen",
        usage = "/country rank add <player> <rank>",
        playerOnly = true,
        catchAll = true
)
public class CmdCountryRankAdd extends CountryCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        var citizen = getCitizen((Player) sender);
        if (citizen == null)
            return null;
        var country = getCitizenCountry(citizen);
        if (country == null)
            return null;

        switch (args.length) {
            case 1:
                return country.getCitizens().stream().map(Citizen::getName).collect(Collectors.toList());
            case 2:
                return PermissionManager.instance().getCountryRanks();
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

        if (!context.country().equals(targetCitizen.getCountry())) {
            United.messenger().send(context.player(), "player.country.addrank.target-player-not-in-country", targetCitizen.getName(), context.country().getName());
            return;
        }

        if (!PermissionManager.instance().getCountryRanks().contains(args[1])) {
            United.messenger().send(context.player(), "admin.country.unknown-rank", args[1]);
            return;
        }

        if (targetCitizen.getCountryRanks().contains(args[1])) {
            United.messenger().send(context.player(), "player.country.addrank.rank-already-owned", targetCitizen.getName(), args[1]);
            return;
        }

        if (args[1].equals("leader")) {

            if (!hasPermission("country.manage.ranks.leader", context.citizen()))
                return;

            var confirmation = new Confirmation("new-leader");
            confirmation.setRunnable(() -> {

                var currentLeader = context.country().getLeader();
                currentLeader.removeCountryRank("leader");
                currentLeader.save();

                targetCitizen.addCountryRank("leader");
                targetCitizen.save();

                if (currentLeader.getPlayer().isOnline()) {
                    United.messenger().send(currentLeader.getPlayer(),
                            "player.country.removerank.rank-lost","leader");
                }
                if (targetPlayer.isOnline()) {
                    United.messenger().send(targetPlayer, "player.country.addrank.rank-received", "leader");
                }
            })
                    .setTitle(United.messenger().get("player.country.addrank.confirm-new-leader", targetCitizen.getName()))
                    .setSender(context.player())
                    .setReceiver(context.player())
                    .send();

            ConfirmationManager.instance().queueConfirmation(confirmation);

        } else {

            if (!hasPermission("country.manage.ranks.other", context.citizen()))
                return;

            targetCitizen.addCountryRank(args[1]);
            targetCitizen.save();

            if (targetPlayer.isOnline()) {
                United.messenger().send(targetPlayer, "player.country.addrank.rank-received", args[1]);
            }

        }

        United.messenger().send(context.player(), "player.country.addrank.success", args[1], targetCitizen.getName());

    }

}
