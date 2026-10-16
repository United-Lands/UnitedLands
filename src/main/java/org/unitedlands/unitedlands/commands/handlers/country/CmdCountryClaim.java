package org.unitedlands.unitedlands.commands.handlers.country;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.events.region.RegionClaimStartEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionDoubleClaimEvent;

import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.unitedlands.utils.CostUtils;
import org.unitedlands.unitedlands.utils.PolygonUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountry.class,
        name = "claim",
        description = "Starts a region claim for a country",
        usage = "/country claim",
        playerOnly = true
)
public class CmdCountryClaim extends CountryCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "country.claim");
        if (context == null)
            return;

        var region = UnitedLandsDataManager.instance()
                .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(context.player().getLocation()));
        if (region == null) {
            United.messenger().send(context.player(), "player.country.claim.no-region");
            return;
        } else {
            if (region.getCountry() != null) {
                United.messenger().send(context.player(), "player.country.claim.already-claimed", region.getCountry().getCleanName());
                return;
            }
        }

        // Region adjacency

        boolean connected = false;
        for (var countryRegion : context.country().getRegions()) {
            if (PolygonUtils.sharesVertex(countryRegion.getPolygon(), region.getPolygon()))
                connected = true;
        }
        if (!connected) {
            United.messenger().send(context.player(), "player.country.claim.disconnected");
            return;
        }

        var claimCost = CostUtils.getRegionClaimCosts(context.country(), region);

        var countryBalance = UnitedLandsEconomyManager.instance().getBalance(context.country().getUuid());
        if (countryBalance.doubleValue() < claimCost) {
            United.messenger().send(context.player(), "general-errors.no-funds-country", 
                UnitedLandsEconomyManager.instance().format(claimCost));
            return;
        }

        var maxClaimAttribute = context.country().getModifiedAttribute("MAX_PARELLEL_CLAIMS");
        var currentClaims = UnitedLandsDataManager.instance().getRegionClaimsOngoing(context.country());
        if (currentClaims.size() >= maxClaimAttribute.getCurrentValue()) {
            United.messenger().send(context.player(), "player.country.claim.too-many-claiming", String.valueOf(maxClaimAttribute.getCurrentValue()));
            return;
        }

        var regionClaimTime = (long) context.country().getModifiedAttribute("REGION_CLAIM_TIME").getCurrentValue();

        var confirmationMessage = United.messenger().get("player.country.claim.confirm",
                region.getCleanName(),
                context.country().getCleanName(),
                UnitedLandsEconomyManager.instance().format(claimCost),
                United.formatter().formatDuration(regionClaimTime * 1000)
            );

        var doubleClaim = false;

        if (region.getClaimantCountry() != null) {

            // Notify other plugins (e.g. UnitedWars) of the double claim attempt. They can
            // decide to uncancel the double claim event to execute other logic on the
            // actual claim event. If they don't, prevent the double claiming by default.
            RegionDoubleClaimEvent doubleClaimEvent = new RegionDoubleClaimEvent(
                    (Player) sender,
                    region,
                    context.country(),
                    region.getClaimantCountry());

            doubleClaimEvent.setCancelled(true);
            doubleClaimEvent.callEvent();

            if (doubleClaimEvent.isCancelled()) {
                United.messenger().send(context.player(), "player.country.claim.already-being-claimed", region.getClaimantCountry().getCleanName());
                return;
            }

            // Get the other plugin's alternate confirmation message
            if (doubleClaimEvent.getConfirmationMessage() != null)
                confirmationMessage = doubleClaimEvent.getConfirmationMessage();
            doubleClaim = true;

        }

        var finalDoubleClaim = doubleClaim;
        Confirmation confirmation = new Confirmation("region-claim");
        confirmation.setRunnable(() -> {

            // Allow other plugins (like UnitedWars) cancel the event and execute other code
            // instead.
            RegionClaimStartEvent startClaimEvent = new RegionClaimStartEvent((Player) sender, region, context.country(), finalDoubleClaim);
            startClaimEvent.callEvent();
            if (startClaimEvent.isCancelled())
                return;

            region.setClaimantCountry(context.country());
            region.setClaimStartTime(System.currentTimeMillis());
            region.setClaimEndTime(System.currentTimeMillis() + (regionClaimTime * 1000));
            region.startClaimTask();
            region.saveAndRender();

            United.messenger().broadcast("player.country.claim.start-broadcast", context.country().getCleanName(), region.getCleanName(), United.formatter().formatDuration(region.getClaimEndTime() - region.getClaimStartTime()));


        })
                .setTitle(confirmationMessage)
                .setSender(context.player())
                .setReceiver(context.player())
                .send();

    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
