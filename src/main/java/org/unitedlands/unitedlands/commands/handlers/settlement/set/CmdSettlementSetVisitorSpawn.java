package org.unitedlands.unitedlands.commands.handlers.settlement.set;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementSet.class,
        name = "visitorspawn",
        description = "Sets the settlement spawn",
        usage = "/settlement set spawn",
        playerOnly = true
)
public class CmdSettlementSetVisitorSpawn extends SettlementCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "settlement.setspawn");
        if (context == null)
            return;

        var chunkCoordinates = CoordinateUtils.locationToChunkCoordinates(context.player().getLocation());
        if (!context.settlement().hasChunkAtCoordinates(chunkCoordinates)) {
            United.messenger().send(context.player(), "player.settlement.setspawn.not-in-claims");
            return;
        }

        context.settlement().setVisitorSpawn(context.player().getLocation());
        context.settlement().save();

        United.messenger().send(context.player(), "player.settlement.setspawn.success");
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
