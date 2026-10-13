package org.unitedlands.unitedlands.commands.handlers.admin.country.modifiers;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountryModifiers.class,
        name = "list",
        description = "Lists all country modifiers",
        usage = "/ula country modifiers list <country_name>"
)

public class CmdAdminCountryModifiersList extends CountryAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender semder, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getCountryNames();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        var country = getCountry(sender, args[0]);
        if (country == null) {
            return;
        }

        United.messenger().sendRaw(sender, "<aqua><bold>" + country.getCleanName() + " Modifiers</bold></aqua>");

        var modifiers = country.getAttributeModifiers();
        for (var mod : modifiers) {
            String entry = "<bold>" + mod.getAttributeKey() + "</bold> - Modifier key: " + mod.getModifierKey() + " | Mode: " + mod.getMode() +
                    " | Value modifier: " + mod.getValueModifier() + " | Max value modifier: " + mod.getMaxValueModifier() + " | Min value modifier: "
                    + mod.getMinValueModifier() + " | Daily change modifier: " + mod.getDailyChangeModifier();
            United.messenger().sendRaw(sender, entry);
        }
    }

}
