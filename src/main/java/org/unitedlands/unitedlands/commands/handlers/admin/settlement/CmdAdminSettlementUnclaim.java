package org.unitedlands.unitedlands.commands.handlers.admin.settlement;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementUnclaimEvent;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlement.class,
        name = "unclaim",
        description = "Unclaims a chunk",
        usage = "/ula settlement unclaim",
        playerOnly = true
)
public class CmdAdminSettlementUnclaim extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        if (args.length != 0) {
            sendUsage(sender);
            return;
        }

        var chunkCoords = CoordinateUtils.locationToChunkCoordinates(player.getLocation());

        var existingChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoords);
        if (existingChunk == null) {
            United.messenger().send(player, "admin.settlement.unclaim.not-claimed");
            return;
        }

        var settlement = existingChunk.getSettlement();

        var spawnChunkCoords = CoordinateUtils.locationToChunkCoordinates(settlement.getSpawn());
        if (spawnChunkCoords.equals(chunkCoords)) {
            United.messenger().send(player, "admin.settlement.unclaim.has-spawn");
            return;
        }

        var visitorSpawnChunkCoords = CoordinateUtils.locationToChunkCoordinates(settlement.getVisitorSpawn());
        if (visitorSpawnChunkCoords.equals(chunkCoords)) {
            United.messenger().send(player, "admin.settlement.unclaim.has-visitor-spawn");
            return;
        }

        if (settlement.getHomeChunkCoordinates().equals(chunkCoords)) {
            United.messenger().send(player, "admin.settlement.unclaim.is-home-chunk");
            return;
        }

        settlement.removeChunk(existingChunk);
        settlement.saveAndRender();

        (new SettlementUnclaimEvent(settlement, chunkCoords)).callEvent();

        UnitedLandsDataManager.instance().removeSettlementChunkDbData(existingChunk);

        United.messenger().send(player, "admin.settlement.unclaim.success", chunkCoords.toString(), settlement.getCleanName());
    }

}
