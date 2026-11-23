package org.unitedlands.unitedlands.commands.handlers.admin.settlement;

import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlement.class,
        name = "claim",
        description = "Claims a chunk for a settlement",
        usage = "/ula settlement claim <settlement_name>",
        playerOnly = true
)
public class CmdAdminSettlementClaim extends AdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getSettlementNames();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var settlement = getSettlement(player, args[0]);
        if (settlement == null) {
            return;
        }

        var chunkCoords = CoordinateUtils.locationToChunkCoordinates(player.getLocation());

        var existingChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoords);
        if (existingChunk != null) {
            United.messenger().send(player, "admin.settlement.claim.already-claimed");
            return;
        }

        var chunk = new SettlementChunk();

        chunk.setUuid(UUID.randomUUID());
        chunk.setCoordinates(chunkCoords);
        chunk.setWorld(player.getLocation().getWorld());
        chunk.setClaimTimestamp(System.currentTimeMillis());
        chunk.setSettlement(settlement);

        settlement.addChunk(chunk);

        UnitedLandsDataManager.instance().createSettlementChunkDbData(chunk);

        United.messenger().send(player, "admin.settlement.claim.success", chunkCoords.toString(), settlement.getCleanName());
    }

}
