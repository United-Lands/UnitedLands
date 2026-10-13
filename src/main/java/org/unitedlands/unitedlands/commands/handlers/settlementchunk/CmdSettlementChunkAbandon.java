package org.unitedlands.unitedlands.commands.handlers.settlementchunk;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementChunkCommandHandler;

import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementChunk.class,
        name = "abandon",
        description = "Abandons an owned settlement chunk",
        usage = "/settlementchunk abandon",
        playerOnly = true
)
public class CmdSettlementChunkAbandon extends SettlementChunkCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, null);
        if (context == null)
            return;

        if (!isOwner(context.settlementChunk(), context.citizen())) {
            United.messenger().send(context.player(), "player.settlementchunk.abandon.not_owner");
            return;
        }

        context.settlementChunk().removeOwner();
        context.settlementChunk().save();

        United.messenger().send(context.player(), "player.settlementchunk.abandon.success");

    }

}
