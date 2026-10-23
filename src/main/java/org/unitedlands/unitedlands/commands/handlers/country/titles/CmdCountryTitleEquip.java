package org.unitedlands.unitedlands.commands.handlers.country.titles;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;
import org.unitedlands.unitedlands.managers.TitleManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountryTitle.class,
        name = "equip",
        description = "Equips a country title",
        usage = "/country title equip <title>",
        playerOnly = true
)
public class CmdCountryTitleEquip extends CountryCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            var cititen = getCitizen((Player) sender);
            if (cititen != null) {
                var country = getCitizenCountry(cititen);
                if (country != null) {
                    return country.getClaimedTitles().stream().toList();
                }
            }
        }

        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "country.title.equip");
        if (context == null)
            return;

        var titleConfig = UnitedLandsTitlesConfig.get().uniqueTitles().get(args[0]);
        if (titleConfig == null) {
            United.messenger().send(sender, "player.country.titles.unknown-title", args[0]);
            return;
        }

        if (!context.country().hasClaimedTitle(args[0])) {
            United.messenger().send(context.player(), "player.country.titles.not-owned");
            return;
        }

        TitleManager.instance().unequipTitle(context.country(), titleConfig);
        TitleManager.instance().equipTitle(context.country(), args[0], titleConfig);

        United.messenger().send(context.player(), "player.country.titles.equipped", titleConfig.displayName());
    }

}
