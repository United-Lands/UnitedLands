package org.unitedlands.unitedlands.commands.handlers.country.titles;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;
import org.unitedlands.unitedlands.managers.TitleManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountryTitle.class,
        name = "check",
        description = "Checks the requirements of a country title",
        usage = "/country title check <title>",
        playerOnly = true
)
public class CmdCountryTitleCheck extends CountryCommandHandler {

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
            return;
        }

        var context = validate(sender, null);
        if (context == null)
            return;

        var titleValidation = TitleManager.instance().validateUniqueTitle(context.country(), args[0]);

        List<String> primaryRegions = new ArrayList<>();

        for (var region : titleValidation.ownedPrimaryRegions()) {
            primaryRegions.add("<green>" + region + "</green>");
        }
        for (var region : titleValidation.missingPrimaryRegions()) {
            primaryRegions.add("<red>" + region + "</red>");
        }

        List<String> secondaryRegions = new ArrayList<>();
        for (var region : titleValidation.ownedSecondaryRegions()) {
            secondaryRegions.add("<green>" + region + "</green>");
        }
        for (var region : titleValidation.missingecondaryRegions()) {
            secondaryRegions.add("<red>" + region + "</red>");
        }

        String validationResult = titleValidation.claimable() ? "<green>You can claim this title</green>"
                : "<red>You can't claim this title at this point.</red>";

        United.messenger().sendRaw(sender, "<gray>Title check: <bold>" + titleConfig.displayName() + "</bold></gray>");
        United.messenger().sendRaw(sender,
                "<gray><bold>Primary regions</bold>: " + String.join(", ", primaryRegions) + " ("
                        + titleValidation.ownedPrimaryRegions().size() + "/"
                        + titleValidation.requiredPrimaryRegionCount() + ")</gray>");
        United.messenger().sendRaw(sender,
                "<gray><bold>Secondary regions</bold>: " + String.join(", ", secondaryRegions) + " ("
                        + titleValidation.ownedSecondaryRegions().size() + "/"
                        + titleValidation.requiredSecondaryRegionCount() + ")</gray>");

        United.messenger().sendRaw(sender, validationResult);
    }

}
