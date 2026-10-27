package org.unitedlands.unitedlands.commands.handlers.country.lists;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.commands.handlers.country.CmdCountry;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdCountry.class,
    name            = "settlementlist",
    aliases         = { "settlements" }, 
    description     = "Shows a country's owned settlements",
    usage           = "/country settlementlist [country_name]",
    playerOnly      = true
)
public class CmdCountrySettlementList extends CountryCommandHandler {

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

        var regionNames = String.join(", ", country.getSettlements().stream().map(Settlement::getCleanName).toList());
        United.messenger().send(sender, "player.country.list.settlements", country.getDisplayName(), regionNames);
    }

}
