package org.unitedlands.unitedlands.commands.handlers.country;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountry.class,
        name = "unclaim",
        description = "Unclaims a region",
        usage = "/country unclaim",
        playerOnly = true
)
public class CmdCountryUnclaim extends CountryCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "country.unclaim");
        if (context == null)
            return;

        var region = UnitedLandsDataManager.instance().getRegion(CoordinateUtils.locationToChunkCenterCoordinates(context.player().getLocation()));
        if (region == null) {
            United.messenger().send(context.player(), "player.country.unclaim.no-region");
            return;
        } else {

            if (context.country().getRegionCount() == 1) {
                United.messenger().send(context.player(), "player.country.unclaim.last-region");
                return;
            }

            // Not claied and not being claimed
            if (region.getCountry() == null && region.getClaimantCountry() == null) {
                United.messenger().send(context.player(), "player.country.unclaim.region-not-claimed");
                return;
            }

            // TODO: CAPITAL CHECK

            if (region.getCountry() != null) {
                if (!context.country().equals(region.getCountry())) {
                    // Claimed, but by someone else
                    United.messenger().send(context.player(), "player.country.unclaim.region-not-owned");
                    return;
                } else {
                    // Claimed by country

                    Player player = (Player) sender;

                    var confirmTitle = United.messenger().get("player.country.unclaim.confirm");
                    var countrySettlements = region.getSettlements(context.country());
                    if (countrySettlements.size() > 0) {
                        confirmTitle += " " + United.messenger().get("player.country.unclaim.confirm-warn-settlement");
                        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
                        if (citizen != null && countrySettlements.stream().anyMatch(s -> s.getCitizens().contains(citizen))) {
                            confirmTitle += " " + United.messenger().get("player.country.unclaim.confirm-warn-leader");
                        }
                    }

                    Confirmation confirmation = new Confirmation("region-unclaim");
                    confirmation.setRunnable(() -> {

                        for (var countrySettlement : countrySettlements) {
                            for (var citizen : countrySettlement.getCitizens()) {
                                citizen.removeCountryRanks();
                                citizen.save();
                            }
                            countrySettlement.removeCountry();
                            countrySettlement.saveAndRender();
                        }

                        region.removeCountry();
                        region.saveAndRender();

                        context.country().removeRegion(region);
                        context.country().saveAndRender();

                        United.messenger().send(context.player(), "player.country.unclaim.success");
                    })
                            .setTitle(confirmTitle)
                            .setSender(player)
                            .setReceiver(player)
                            .send();

                }
            } else {
                if (region.getClaimantCountry() != null) {
                    if (!context.country().equals(region.getClaimantCountry())) {
                        // Being claimed, but by someone else
                        United.messenger().send(context.player(), "player.country.unclaim.other-country-claiming");
                        return;
                    } else {
                        // Being claimed by country
                        region.removeClaimantCountry();
                        region.setClaimStartTime(null);
                        region.setClaimEndTime(null);
                        region.saveAndRender();

                        United.messenger().send(context.player(), "player.country.unclaim.success", region.getCleanName());
                    }
                }
            }
        }
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
