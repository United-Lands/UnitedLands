package org.unitedlands.unitedlands.commands.handlers.settlement.trust;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementTrust.class,
        name = "remove",
        description = "Removes settlement trust from a player",
        usage = "/settlement trust remove <player>",
        playerOnly = true
)
public class CmdSettlementTrustRemove extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            var settlement = UnitedLandsDataManager.instance().getSettlement((Player) sender);
            if (settlement != null)
                return settlement.getTrustList().stream().map(Citizen::getName).toList();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.removetrust");
        if (context == null)
            return;

        var targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null) {
            United.messenger().send(context.player(), "general-errors.player-not-found", args[0]);
            return;
        }
        var targetCitizen = getCitizen(targetPlayer);
        if (targetCitizen == null)
            return;

        if (!context.settlement().getTrustList().contains(targetCitizen)) {
            United.messenger().send(context.player(), "player.settlement.removetrust.not-trusted", args[0]);
            return;
        }

        context.settlement().removeTrusted(targetCitizen);
        context.settlement().save();

        if (targetPlayer.isOnline()) {
            United.messenger().send(targetPlayer, "player.settlement.removetrust.untrusted", context.settlement().getCleanName());
        }

        United.messenger().send(context.player(), "player.settlement.removetrust.success", targetPlayer.getName());
    }

}
