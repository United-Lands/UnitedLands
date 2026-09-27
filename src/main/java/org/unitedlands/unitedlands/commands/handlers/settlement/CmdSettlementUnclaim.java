package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementUnclaimEvent;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(parent = CmdSettlement.class, name = "unclaim", description = "Unclaims a settlement chunk", usage = "/settlement unclaim", playerOnly = true)
public class CmdSettlementUnclaim extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "settlement.unclaim");
        if (context == null)
            return;

        var chunkCoords = CoordinateUtils.locationToChunkCoordinates(context.player().getLocation());

        var existingChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoords);
        if (existingChunk == null) {
            United.messenger().send(context.player(), "player.settlement.unclaim.not-claimed");
            return;
        }

        if (!existingChunk.getSettlement().equals(context.settlement())) {
            United.messenger().send(context.player(), "player.settlement.unclaim.not-in-settlement");
            return;
        }

        var spawnChunkCoords = CoordinateUtils.locationToChunkCoordinates(context.settlement().getSpawn());
        if (spawnChunkCoords.equals(chunkCoords)) {
            United.messenger().send(context.player(), "player.settlement.unclaim.has-spawn");
            return;
        }

        var visitorSpawnChunkCoords = CoordinateUtils.locationToChunkCoordinates(context.settlement().getVisitorSpawn());
        if (visitorSpawnChunkCoords.equals(chunkCoords)) {
            United.messenger().send(context.player(), "player.settlement.unclaim.has-visitor-spawn");
            return;
        }

        if (context.settlement().getHomeChunkCoordinates().equals(chunkCoords)) {
            United.messenger().send(context.player(), "player.settlement.unclaim.is-home-chunk");
            return;
        }

        context.settlement().removeChunk(existingChunk);

        (new SettlementUnclaimEvent(context.settlement(), chunkCoords)).callEvent();

        UnitedLandsDataManager.instance().removeSettlementChunkDbData(existingChunk);
        context.settlement().saveAndRender();

        United.messenger().send(context.player(), "player.settlement.unclaim.success",
                context.settlement().getCleanName(), chunkCoords.toString());
    }

}
