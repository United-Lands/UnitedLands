package org.unitedlands.unitedlands.commands.handlers.settlementchunk;

import java.math.BigDecimal;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.BankRecordTransactionType;
import org.unitedlands.unitedlands.classes.PlotTypeBehavior;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementChunkCommandHandler;
import org.unitedlands.unitedlands.classes.events.settlementchunk.SettlementChunkPrePurchaseEvent;
import org.unitedlands.unitedlands.classes.events.settlementchunk.SettlementChunkPurchaseEvent;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementChunk.class,
        name = "buy",
        description = "Buys a settlement chunk that is for sale",
        usage = "/settlementchunk buy",
        playerOnly = true
)
public class CmdSettlementChunkBuy extends SettlementChunkCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, null);
        if (context == null)
            return;

        if (!context.settlementChunk().isForSale()) {
            United.messenger().send(context.player(), "player.settlementchunk.buy.not-for-sale");
            return;
        }

        var settlement = context.settlementChunk().getSettlement();
        if (settlement.isBanished(context.citizen())) {
            United.messenger().send(context.player(), "player.settlementchunk.buy.banished");
            return;
        }

        if (!context.settlementChunk().hasPlotBehavior(PlotTypeBehavior.FREEHOLD)
                && !settlement.equals(context.citizen().getSettlement())) {
            United.messenger().send(context.player(), "player.settlementchunk.buy.not-resident");
            return;
        }

        if (!UnitedLandsEconomyManager.instance().has(context.citizen().getUuid(),
                new BigDecimal(context.settlementChunk().getSalePrice()))) {
            United.messenger().send(context.player(), "general-errors.no-funds-player", UnitedLandsEconomyManager.instance().format(context.settlementChunk().getSalePrice()));
            return;
        }

        var preEvent = new SettlementChunkPrePurchaseEvent(context.settlementChunk().getSettlement(), context.settlementChunk(), context.citizen());
        preEvent.callEvent();
        if (preEvent.isCancelled())
            return;

        UnitedLandsEconomyManager.instance().withdraw(context.citizen().getUuid(),
                context.settlementChunk().getSalePrice(),
                BankRecordTransactionType.PURCHASE,
                "Purchased plot in " + context.settlementChunk().getSettlement().getName());
        UnitedLandsEconomyManager.instance().deposit(context.settlementChunk().getSettlement().getUuid(),
                context.settlementChunk().getSalePrice(),
                BankRecordTransactionType.PURCHASE,
                "Plot bought by " + context.player().getName());

        United.messenger().send(context.player(), "player.settlementchunk.buy.success", UnitedLandsEconomyManager.instance().format(context.settlementChunk().getSalePrice()));

        context.settlementChunk().setSalePrice(null);
        context.settlementChunk().setOwner(context.citizen());

        (new SettlementChunkPurchaseEvent(context.settlementChunk().getSettlement(), context.settlementChunk(), context.citizen())).callEvent();

        context.settlementChunk().save();
    }

}
