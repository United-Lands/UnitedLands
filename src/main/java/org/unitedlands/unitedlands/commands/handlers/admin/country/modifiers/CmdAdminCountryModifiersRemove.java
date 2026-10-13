package org.unitedlands.unitedlands.commands.handlers.admin.country.modifiers;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.GeopolAttributeModifier;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountryModifiers.class,
        name = "remove",
        description = "Removes a country modifier",
        usage = "/ula country modifiers remove <country_name> <attr_key> <mod_key>",
        catchAll = true
)

public class CmdAdminCountryModifiersRemove extends CountryAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getCountryNames();
        } else if (args.length == 2) {
            var country = UnitedLandsDataManager.instance().getCountry(args[0]);
            if (country != null) {
                var mods = country.getAttributeModifiers();
                    return mods.stream().map(GeopolAttributeModifier::getAttributeKey).distinct().toList();
            } 
        } else if (args.length == 3) {
            var country = UnitedLandsDataManager.instance().getCountry(args[0]);
            if (country != null) {
                var mods = country.getAttributeModifiers();
                    return mods.stream().map(GeopolAttributeModifier::getModifierKey).distinct().toList();
            } 
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 3) {
            sendUsage(sender);
            return;
        }

        var country = getCountry(sender, args[0]);
        if (country == null) {
            return;
        }

        country.removeAttributeModifiers(args[1], args[2]);

        // TODO: Move strings to config
        United.messenger().sendRaw(sender, "<green>Removed all modifiers for attribute " + args[1] + " with key " + args[2]);

    }

}
