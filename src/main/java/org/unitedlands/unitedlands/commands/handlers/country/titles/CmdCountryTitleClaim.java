package org.unitedlands.unitedlands.commands.handlers.country.titles;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;
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
            return UnitedLandsTitlesConfig.get().titles().keys().stream().toList();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var titleConfig = UnitedLandsTitlesConfig.get().titles().get(args[0]);
        if (titleConfig == null) {
            return;
        }

        var context = validate(sender, "country.title.claim");
        if (context == null)
            return;

        var countryRegionNames = context.country().getRegions().stream().map(Region::getDefaultName).toList();

        List<String> missingRegions = new ArrayList<>();
        for (var primaryRegion : titleConfig.primaryRegions()) {
            if (!countryRegionNames.contains(primaryRegion))
                missingRegions.add(primaryRegion);
        }

        if (missingRegions.size() > 0) {
            United.messenger().sendRaw(sender, "<yellow>Your country doesn't own the following required region(s): " + String.join(", ", missingRegions));
            return;
        }

    }

}
