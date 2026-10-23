package org.unitedlands.unitedlands.commands.handlers.country.titles;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;
import org.unitedlands.unitedlands.managers.TitleManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountryTitle.class,
        name = "unequip",
        description = "Unequips the current country title",
        usage = "/country title unequip",
        playerOnly = true
)
public class CmdCountryTitleUnequip extends CountryCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "country.title.unequip");
        if (context == null)
            return;

        if (context.country().getEquippedTitle() == null) {
            United.messenger().send(context.player(), "player.country.titles.no-title-equipped");
            return;
        }

        var titleConfig = UnitedLandsTitlesConfig.get().uniqueTitles().get(context.country().getEquippedTitle());
        if (titleConfig == null) {
            United.messenger().send(sender, "player.country.titles.unknown-title", context.country().getEquippedTitle());
            return;
        }

        TitleManager.instance().unequipTitle(context.country(), titleConfig);

        United.messenger().send(context.player(), "player.country.titles.unequipped");
    }

}
