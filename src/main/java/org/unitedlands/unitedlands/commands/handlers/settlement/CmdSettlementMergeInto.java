package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.PolygonUtils;
import org.unitedlands.utils.United;


@UnitedSubCommand(
    parent          = CmdSettlement.class,
    name            = "mergeinto",
    description     = "Merges a settlement into the target settlement",
    usage           = "/settlement mergeinto <target_settlememt>",
    playerOnly      = true
)
public class CmdSettlementMergeInto extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getSettlementNames();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
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

        if (targetSettlement.equals(context.settlement())) {
            United.messenger().send(sender, "player.merge.cannot-merge-self");
            return;  
        }

        if (targetSettlement.getMayor() == null) {
            United.messenger().send(sender, "player.merge.target-is-npc");
            return;  
        }

        if (!targetSettlement.getMayor().getPlayer().isOnline()) {
            United.messenger().send(sender, "player.merge.target-mayor-offline");
            return;  
        }

        if (context.settlement().hasRegion()) {
            if (targetSettlement.hasRegion()) {
                // Check same region and region adjecency
                if (!targetSettlement.getRegion().equals(context.settlement().getRegion())) {
                    if (!PolygonUtils.sharesVertex(targetSettlement.getRegion().getPolygon(), context.settlement().getRegion().getPolygon())) {
                        United.messenger().send(sender, "player.merge.target-not-adjacent");
                        return;  
                    }
                }
            } else {
                // A region settlement can't merge witha regionless settlement
                United.messenger().send(sender, "player.merge.target-not-in-region");
                return; 
            }
        } else {
            if (targetSettlement.hasRegion()) {
                United.messenger().send(sender, "player.merge.target-in-region");
                return; 
            } 
        }

        if (context.settlement().hasCountry()) {
            if (targetSettlement.hasCountry()) { 
                // Country settlements can only merge with settlements in their own country
                if (!targetSettlement.getCountry().equals(context.settlement().getCountry())) {
                    United.messenger().send(sender, "player.merge.cannot-merge-foreign-settlement", args[0]);
                    return;  
                }
            } 
        }

        var confirmStr = United.messenger().get("player.merge.confirm", targetSettlement.getCleanName());

        var mergeConfirm = new Confirmation("merge");
        mergeConfirm.setRunnable(() -> {

            var requestStr = United.messenger().get("player.merge.request", context.settlement().getCleanName());
            var targetMergeConfirm = new Confirmation("merge-request");
            targetMergeConfirm.setRunnable(() -> {

                UnitedLandsDataManager.instance().mergeSettlements(targetSettlement, context.settlement());

                United.messenger().send(Bukkit.getServer(), "player.settlement.merge.broadcast", 
                    context.settlement().getCleanName(), 
                    targetSettlement.getCleanName());

            })
            .setTitle(requestStr)
            .setSender(context.player())
            .setReceiver(targetSettlement.getMayor().getPlayer())
            .send();

        })
        .setTitle(confirmStr)
        .setSender(context.player())
        .setReceiver(context.player())
        .send();
        
    }

}
