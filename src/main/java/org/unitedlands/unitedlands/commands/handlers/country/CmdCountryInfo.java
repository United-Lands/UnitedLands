package org.unitedlands.unitedlands.commands.handlers.country;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.events.infoscreen.CountryInfoScreenEvent;
import org.unitedlands.unitedlands.classes.infoscreen.CountryInfoScreen;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;

@UnitedSubCommand(
    parent          = CmdCountry.class,
    name            = "info",
    description     = "Shows information about a country",
    usage           = "/country info <country_name>",
    playerOnly      = true
)
public class CmdCountryInfo extends CountryCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getCountryNames();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;
        var citizen = getCitizen(player);
        if (citizen == null)
            return;

        Country country = null;
        if (args.length == 0) {
            country = getCitizenCountry(citizen);
            if (country == null)
                return;
        } else if (args.length >= 1) {
            country = UnitedLandsDataManager.instance().getCountry(args[0]);
            if (country == null) {
                return;
            }
        }

        var infoScreen = new CountryInfoScreen(country);

        // Give other plugins the opportunity to alter the info screen
        var infoScreenEvent = new CountryInfoScreenEvent(infoScreen, country);
        infoScreenEvent.callEvent();
        
        infoScreenEvent.getInfoScreen().send(player);
    }
}
