package org.unitedlands.unitedlands.commands.handlers.admin.country.attributes;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.GeopolAttribute;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountryAttributes.class,
        name = "list",
        description = "Lists all country attribues",
        usage = "/ula country attributes list <country_name> [--base]"
)

public class CmdAdminCountryAttributesList extends CountryAdminCommandHandler {

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

        var showBase = args.length == 2 && args[1].equalsIgnoreCase("--base");

        if (showBase) {
            United.messenger().sendRaw(sender, "<aqua><bold>" + country.getCleanName() + " Attributes (base values)</bold></aqua>");
        } else {
            United.messenger().sendRaw(sender, "<aqua><bold>" + country.getCleanName() + " Attributes (final values)</bold></aqua>");
        }

        var attributeKeys = country.getAttributeKeys();
        for (var key : attributeKeys) {

            GeopolAttribute v;
            if (showBase) {
                v = country.getAttribute(key);
            } else {
                v = country.getModifiedAttribute(key);
            }

            String entry = "<bold>" + key + "</bold> - Current value: " + v.getCurrentValue() + " | Min value: " + v.getMinValue() + " | Max value: "
                    + v.getMaxValue() + " | Daily change: "
                    + v.getDailyChange();
            United.messenger().sendRaw(sender, entry);
        }
    }

}
