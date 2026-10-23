package org.unitedlands.unitedlands.commands.handlers.country.titles;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;
import org.unitedlands.unitedlands.managers.TitleManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountryTitle.class,
        name = "claim",
        description = "Claims a country title",
        usage = "/country title claim <title>",
        playerOnly = true
)
public class CmdCountryTitleClaim extends CountryCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsTitlesConfig.get().uniqueTitles().keys().stream().toList();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var titleConfig = UnitedLandsTitlesConfig.get().uniqueTitles().get(args[0]);
        if (titleConfig == null) {
            United.messenger().send(sender, "player.country.titles.unknown-title", args[0]);
            return;
        }

        var context = validate(sender, "country.title.claim");
        if (context == null)
            return;

        if (TitleManager.instance().isUniqueTitleClaimed(args[0])) {
            var holder = TitleManager.instance().getUniqueTitleHolder(args[0]);
            United.messenger().send(sender, "player.country.titles.already-claimed", holder.getCleanName());
            return;
        }

        var countryRegionNames = context.country().getRegions().stream().map(Region::getDefaultName).toList();

        List<String> missingRegions = new ArrayList<>();
        for (var primaryRegion : titleConfig.primaryRegions()) {
            if (!countryRegionNames.contains(primaryRegion))
                missingRegions.add(primaryRegion);
        }

        if (missingRegions.size() > 0) {
            United.messenger().send(sender, "player.country.titles.missing-regions", String.join(", ", missingRegions));
            return;
        }

        context.country().addClaimedTitle(args[0]);
        context.country().save();

        United.messenger().broadcast("player.country.titles.success-broadcast", context.country().getCleanName(), titleConfig.displayName());
    }

}
