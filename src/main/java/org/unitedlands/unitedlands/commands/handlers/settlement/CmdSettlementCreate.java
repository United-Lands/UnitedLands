package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.Settings;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementCreatedEvent;

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
                new BigDecimal(Settings.settlementCreateCosts))) {
            United.messenger().send(player, "general-errors.no-funds-player", UnitedLandsEconomyManager.instance().format(Settings.settlementCreateCosts));
            return;
        }

        var region = UnitedLandsDataManager.instance()
                .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(player.getLocation()));
        if (region != null && region.hasCountry()) {
            United.messenger().send(player, "player.settlement.create.COUNTRY_WARNING", region.getCountry().getCleanName());
        }

        var confirmation = new Confirmation("settlement");
        confirmation.setRunnable(() -> {

            var world = player.getLocation().getWorld();

            Settlement settlement = new Settlement();
            settlement.setUuid(UUID.randomUUID());
            settlement.setName(args[0]);
            settlement.setFounder(player);
            settlement.setFoundingTimestamp(System.currentTimeMillis());
            settlement.setWorld(world);
            settlement.setHomeChunkCoordinates(chunkCoords);
            settlement.setSpawn(player.getLocation());
            settlement.setVisitorSpawn(player.getLocation());

            var regionInfo = "no region";
            var countryInfo = "no country";
            if (region != null) {
                settlement.setRegion(region);
                region.addSettlement(settlement);
                regionInfo = region.getCleanName();

                if (region.hasCountry()) {
                    var country = region.getCountry();
                    settlement.setCountry(country);
                    country.addSettlement(settlement);
                    countryInfo = country.getCleanName();
                }
            }

            var chunk = new SettlementChunk();
            chunk.setUuid(UUID.randomUUID());
            chunk.setCoordinates(CoordinateUtils.locationToChunkCoordinates(player.getLocation()));
            chunk.setWorld(world);
            chunk.setClaimTimestamp(System.currentTimeMillis());
            chunk.setSettlement(settlement);

            settlement.addChunk(chunk);
            settlement.addCitizen(citizen);
            UnitedLandsDataManager.instance().createSettlementDbData(settlement);

            UnitedLandsEconomyManager.instance().createAccount(settlement.getUuid(), settlement.getName());
            UnitedLandsEconomyManager.instance().withdraw(citizen.getUuid(), Settings.settlementCreateCosts, "Settlement creation payment");

            citizen.setSettlement(settlement);
            citizen.addSettlementRank("mayor");
            citizen.save();

            United.messenger().send(player, "player.settlement.create.player-message", settlement.getCleanName());
            United.messenger().send(Bukkit.getServer(), "player.settlement.create.broadcast-message", player.getName(), settlement.getCleanName(), regionInfo, countryInfo);

            (new SettlementCreatedEvent(settlement)).callEvent();

        })
                .setTitle("player.settlement.create.CONFIRM")
                .setReplacements(Map.of("settlement", args[0], "cost",
                        UnitedLandsEconomyManager.instance().format(Settings.settlementCreateCosts)))
                .setSender(player)
                .setReceiver(player)
                .send();
    }

}
