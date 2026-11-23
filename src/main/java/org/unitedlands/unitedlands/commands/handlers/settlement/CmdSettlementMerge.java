package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlement.class,
        name = "merge",
        description = "Merges the target settlement into this settlement",
        usage = "/settlement merge <target_settlement>",
        playerOnly = true
)
public class CmdSettlementMerge extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getSettlementNames();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.merge");
        if (context == null)
            return;

        var targetSettlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
        if (targetSettlement == null) {
            United.messenger().send(sender, "general-errors.settlement-not-found", args[0]);
            return;
        }

        if (targetSettlement.isCapital()) {
            United.messenger().send(sender, "player.settlement.merge.is-capital", targetSettlement.getCleanName());
            return;
        }

        var targetMayor = targetSettlement.getMayor();
        if (targetMayor == null) {
            United.messenger().send(sender, "player.settlement.merge.target-is-npc", targetSettlement.getCleanName());
            return;
        }

        var targetMayorPlayer = targetMayor.getPlayer();
        if (targetMayorPlayer == null || !targetMayorPlayer.isOnline()) {
            United.messenger().send(sender, "player.settlement.merge.mayor-not-online", targetSettlement.getCleanName());
            return;
        }

        if (context.settlement().hasCountry()) {
            if (!context.settlement().getCountry().equals(targetSettlement.getCountry())) {
                United.messenger().send(sender, "player.settlement.merge.target-not-in-country", targetSettlement.getCleanName());
                return;
            }
        } else {
            if (targetSettlement.hasCountry()) {
                United.messenger().send(sender, "player.settlement.merge.target-has-country", targetSettlement.getCleanName());
                return;
            }
        }

        Confirmation confirmation = new Confirmation("merge-settlement");
        confirmation.setRunnable(() -> {
            var targetSettlementName = targetSettlement.getCleanName();
            UnitedLandsDataManager.instance().mergeSettlements(context.settlement(), targetSettlement);

            United.messenger().send(Bukkit.getServer(), "player.settlement.merge.broadcast", context.settlement().getCleanName(), targetSettlementName);
        })
                .setTitle(United.messenger().get("player.settlement.merge.confirm",
                        context.settlement().getCleanName(), targetSettlement.getCleanName()))
                .setSender((Player) sender)
                .setReceiver(targetMayorPlayer)
                .send();

    }
}
