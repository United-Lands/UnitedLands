package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.math.BigDecimal;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdSettlement.class,
    name            = "create",
    description     = "Creates a new settlement",
    usage           = "/settlement create <settlement_name>",
    playerOnly      = true
)
public class CmdSettlementCreate extends SettlementCommandHandler {

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

        var player = (Player) sender;
        var citizen = getCitizen(player);
        if (citizen == null)
            return;

        if (citizen.hasSettlement()) {
            United.messenger().send(player, "player.settlement.create.already-in-settlement", citizen.getSettlement().getCleanName());
            return;
        }

        var chunkCoords = CoordinateUtils.locationToChunkCoordinates(player.getLocation());
        var existingChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoords);
        if (existingChunk != null) {
            United.messenger().send(player, "player.settlement.create.already-claimed");
            return;
        }

        if (!UnitedLandsEconomyManager.instance().has(citizen.getUuid(),
                new BigDecimal(UnitedLandsConfig.get().economy().costs().settlement().createCosts()))) {
            United.messenger().send(player, "general-errors.no-funds-player", 
                UnitedLandsEconomyManager.instance().format(UnitedLandsConfig.get().economy().costs().settlement().createCosts()));
            return;
        }

        var confirmTitle = United.messenger().get("player.settlement.create.confirm",
                        args[0].replace("_", " "),
                        UnitedLandsEconomyManager.instance().format(UnitedLandsConfig.get().economy().costs().settlement().createCosts()));

        var region = UnitedLandsDataManager.instance().getRegion(CoordinateUtils.locationToChunkCenterCoordinates(player.getLocation()));
        if (region != null && region.hasCountry()) {
            var countryWarning = United.messenger().get("player.settlement.create.country-warning", region.getCountry().getCleanName());
            confirmTitle += "<br><br>" + countryWarning;
        }

        var confirmation = new Confirmation("settlement");
        confirmation.setRunnable(() -> {

            var settlement = UnitedLandsDataManager.instance().createSettlement(args[0], chunkCoords, player.getLocation(), region, citizen);

            United.messenger().send(player, "player.settlement.create.player-message", settlement.getCleanName());
            United.messenger().broadcast("player.settlement.create.broadcast-message", 
                player.getName(), 
                settlement.getCleanName(), 
                settlement.hasRegion() ? settlement.getRegion().getCleanName() : "-", 
                settlement.hasCountry() ? settlement.getCountry().getCleanName() : "-"
            );
        })
                .setTitle(confirmTitle)
                .setSender(player)
                .setReceiver(player)
                .send();
    }

}
