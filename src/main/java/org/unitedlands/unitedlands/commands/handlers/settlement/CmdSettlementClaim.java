package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementPreClaimEvent;

import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.unitedlands.utils.CostUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdSettlement.class,
    name            = "claim",
    description     = "Claims a chunk for the settlement",
    usage           = "/settlement claim",
    playerOnly      = true
)
public class CmdSettlementClaim extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "settlement.claim");
        if (context == null)
            return;

        var chunkCoords = CoordinateUtils.locationToChunkCoordinates(context.player().getLocation());

        var existingChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoords);
        if (existingChunk != null) {
            United.messenger().send(context.player(), "player.settlement.claim.already-claimed");
            return;
        }

        var region = UnitedLandsDataManager.instance()
                .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(context.player().getLocation()));
        if (region != null) {
            if (!region.equals(context.settlement().getRegion()) && !UnitedLandsConfig.get().general().allowSettlementClaimsOutsideHomeRegion()) {
                United.messenger().send(context.player(), "player.settlement.claim.outside-of-region");
                return;
            }

            if (region.hasCountry()) {
                // If trying to claim in another country's region, check the claim whitelist
                if (!region.getCountry().equals(context.settlement().getCountry())) {
                    if (!region.getCountry().getSettlementClaimWhitelist().contains(context.settlement())) {
                        United.messenger().send(context.player(), "player.settlement.claim.has-country", region.getCountry().getCleanName());
                        return;
                    }
                }
            }
        }

        var claimCosts = CostUtils.getSettlementClaimCosts(context.settlement());
        if (!UnitedLandsEconomyManager.instance().has(context.settlement().getUuid(), claimCosts)) {
            United.messenger().send(context.player(), "general-errors.no-funds-settlement", UnitedLandsEconomyManager.instance().format(claimCosts));
            return;
        }

        var preEvent = new SettlementPreClaimEvent(context.settlement(), chunkCoords);
        preEvent.callEvent();
        if (preEvent.isCancelled())
            return;

        var chunk = new SettlementChunk();

        chunk.setUuid(UUID.randomUUID());
        chunk.setCoordinates(chunkCoords);
        chunk.setWorld(context.player().getLocation().getWorld());
        chunk.setClaimTimestamp(System.currentTimeMillis());
        chunk.setSettlement(context.settlement());

        context.settlement().addChunk(chunk);

        UnitedLandsDataManager.instance().createSettlementChunkDbData(chunk);

        UnitedLandsEconomyManager.instance().withdraw(context.settlement().getUuid(), claimCosts, "Chunk claiming");

        var coordNorth = new Coordinates(chunkCoords.getX(), chunkCoords.getZ() - 1, chunkCoords.getWorldName());
        var coordEast = new Coordinates(chunkCoords.getX() + 1, chunkCoords.getZ(), chunkCoords.getWorldName());
        var coordSouth = new Coordinates(chunkCoords.getX(), chunkCoords.getZ() + 1, chunkCoords.getWorldName());
        var coordWest = new Coordinates(chunkCoords.getX() - 1, chunkCoords.getZ(), chunkCoords.getWorldName());

        boolean connected = context.settlement().hasChunkAtCoordinates(coordNorth) || 
                            context.settlement().hasChunkAtCoordinates(coordEast) ||
                            context.settlement().hasChunkAtCoordinates(coordWest)  ||
                            context.settlement().hasChunkAtCoordinates(coordSouth);
        if (!connected)
            United.messenger().send(context.player(), "player.settlement.claim.disconnected-warning");
        United.messenger().send(context.player(), "player.settlement.claim.success", context.settlement().getCleanName(), chunkCoords.toCleanShortString(), UnitedLandsEconomyManager.instance().format(claimCosts));
    }

}
