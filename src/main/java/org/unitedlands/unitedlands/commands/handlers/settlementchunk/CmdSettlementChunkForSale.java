package org.unitedlands.unitedlands.commands.handlers.settlementchunk;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementChunkCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementChunk.class,
        name = "forsale",
        aliases = { "fs" },
        description = "Puts a settlement chunk on sale",
        usage = "/settlementchunk forsale <price>",
        playerOnly = true
)
public class CmdSettlementChunkForSale extends SettlementChunkCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.plot.sell");
        if (context == null)
            return;

        if (context.settlementChunk().hasOwner()) {
            United.messenger().send(context.player(), "player.settlementchunk.forsale.has-owner", context.settlementChunk().getOwner().getName());
            return;
        }

        Integer price = null;
        if (args[0].equalsIgnoreCase("clear")) {
            context.settlementChunk().setSalePrice(null);
            context.settlementChunk().save();

            United.messenger().send(context.player(), "player.settlementchunk.forsale.cleared");
        } else {
            try {
                price = Integer.parseInt(args[0]);
            } catch (Exception ex) {
                United.messenger().send(context.player(), "general-errors.wrong-number-format", args[0]);
                return;
            }
            context.settlementChunk().setSalePrice(price);
            context.settlementChunk().save();

            United.messenger().send(context.player(), "player.settlementchunk.forsale.success", UnitedLandsEconomyManager.instance().format(price));
        }
    }

}
