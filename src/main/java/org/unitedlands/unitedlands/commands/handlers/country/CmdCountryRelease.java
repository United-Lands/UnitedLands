package org.unitedlands.unitedlands.commands.handlers.country;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountry.class,
        name = "release",
        description = "Releases a settlement from the country",
        usage = "/country release <settlement>",
        playerOnly = true
)
public class CmdCountryRelease extends CountryCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            var citizen = UnitedLandsDataManager.instance().getCitizen((Player) sender);
            if (citizen == null || citizen.getCountry() == null)
                return null;
            return citizen.getCountry().getSettlements().stream()
                    .filter(s -> !s.isCapital())
                    .map(Settlement::getName)
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "country.releasesettlement");
        if (context == null)
            return;

        var country = context.country();

        var settlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
        if (settlement == null) {
            United.messenger().send(context.player(), "general-errors.settlement-not-found", args[0]);
            return;
        }

        if (!country.equals(settlement.getCountry())) {
            United.messenger().send(context.player(), "player.country.release.not-in-country",
                    settlement.getCleanName(), country.getCleanName());
            return;
        }

        if (settlement.isCapital()) {
            United.messenger().send(context.player(), "player.country.release.is-capital");
            return;
        }

        if (settlement.getCitizens().stream().anyMatch(c -> c.hasCountryRank("leader"))) {
            United.messenger().send(context.player(), "player.country.release.is-leader");
            return;
        }

        Confirmation release = new Confirmation("country-release");
        release.setRunnable(() -> {

            if (!country.equals(settlement.getCountry()))
                return;

            for (var citizen : settlement.getCitizens()) {
                citizen.removeCountryRanks();
                citizen.save();

                var player = citizen.getPlayer();
                if (player != null && player.isOnline()) {
                    if (ChatChannelManager.instance().isInChannel(player, "COUNTRY"))
                        ChatChannelManager.instance().switchChannel(player, "GLOBAL", null);
                    United.messenger().send(player,"player.country.release.released",
                            country.getCleanName());
                }
            }

            settlement.removeCountry();
            settlement.saveAndRender();

            country.removeSettlement(settlement);
            country.saveAndRender();

            United.messenger().send(Bukkit.getServer(), "player.country.release.broadcast-message",
                    settlement.getCleanName(), country.getCleanName());
        })
                .setTitle(United.messenger().get("player.country.release.confirm", settlement.getCleanName(),
                        country.getCleanName()))
                .setSender(context.player())
                .setReceiver(context.player())
                .send();
    }

}
