package org.unitedlands.unitedlands.commands.handlers.settlement.banish;

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
        parent = CmdSettlementBanish.class,
        name = "remove",
        description = "Removes settlement banishment from a player",
        usage = "/settlement banish remove <player>",
        playerOnly = true
)
public class CmdSettlementBanishRemove extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            var settlement = UnitedLandsDataManager.instance().getSettlement((Player) sender);
            if (settlement != null)
                return settlement.getBanishList().stream().map(Citizen::getName).toList();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.removebanish");
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

        if (!context.settlement().getBanishList().contains(targetCitizen)) {
            United.messenger().send(context.player(), "player.settlement.removebanish.not-banished", args[0]);
            return;
        }

        context.settlement().removeBanished(targetCitizen);
        context.settlement().save();

        if (targetPlayer.isOnline()) {
            United.messenger().send(targetPlayer, "player.settlement.removebanish.unbanished", context.settlement().getCleanName());
        }

        United.messenger().send(context.player(), "player.settlement.removebanish.success", targetPlayer.getName());
    }

}
