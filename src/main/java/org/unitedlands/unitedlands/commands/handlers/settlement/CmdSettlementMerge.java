package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.BankRecordTransactionType;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.unitedlands.managers.IntegrationManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
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

        var targetMayor = targetSettlement.getMayor();
        if (!targetMayor.getPlayer().isOnline()) {
            United.messenger().send(sender, "player.settlement.merge.mayor-not-online");
            return;
        }

        if (context.settlement().hasCountry()) {
            if (!context.settlement().getCountry().equals(targetSettlement.getCountry())) {
                United.messenger().send(sender, "player.settlement.merge.target-not-in-country", context.settlement().getCleanName());
                return;
            }
        } else {
            if (targetSettlement.hasCountry()) {
                United.messenger().send(sender, "player.settlement.merge.target-has-country", context.settlement().getCleanName());
                return;
            }
        }

        Confirmation confirmation = new Confirmation("merge-settlement");
        confirmation.setRunnable(() -> {
            for (var chunk : targetSettlement.getChunks()) {
                context.settlement().addChunk(chunk);
                chunk.setSettlement(context.settlement());
                chunk.save();
            }
            for (var citizen : targetSettlement.getCitizens()) {
                citizen.setSettlement(context.settlement());
                citizen.removeSettlementRanks();
                citizen.save();
            }

            if (targetSettlement.hasRegion()) {
                var region = targetSettlement.getRegion();
                region.removeSettlement(targetSettlement);
            }
            if (targetSettlement.hasCountry()) {
                var country = targetSettlement.getCountry();
                country.removeSettlement(targetSettlement);
            }

            UnitedLandsEconomyManager.instance().deposit(context.settlement().getUuid(),
                    UnitedLandsEconomyManager.instance().getBalance(targetSettlement.getUuid()),
                    BankRecordTransactionType.SYSTEM,
                    "Merging of settlement bank of " + targetSettlement.getName());
            UnitedLandsEconomyManager.instance().deleteAccount(targetSettlement.getUuid());

            var targetSettlementName = targetSettlement.getCleanName();
            UnitedLandsDataManager.instance().removeSettlementDbData(targetSettlement);

            IntegrationManager.instance().getMapRenderer().removeSettlement(targetSettlement);

            United.messenger().send(Bukkit.getServer(), "player.settlement.merge.broadcast", context.settlement().getCleanName(), targetSettlementName);
        })
                .setTitle("player.settlement.merge.CONFIRM")
                .setReplacements(Map.of("settlement", context.settlement().getCleanName()))
                .setSender((Player) sender)
                .setReceiver(targetMayor.getPlayer())
                .send();

    }
}
