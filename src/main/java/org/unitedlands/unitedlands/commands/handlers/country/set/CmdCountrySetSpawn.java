package org.unitedlands.unitedlands.commands.handlers.country.set;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdCountrySet.class,
    name = "spawn",
    description = "Sets the country spawn",
    usage = "/country set spawn",
    playerOnly = true
)
public class CmdCountrySetSpawn extends CountryCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "country.setspawn");
        if (context == null)
            return;

        var location = context.player().getLocation();
        var region = UnitedLandsDataManager.instance().getRegion(location);
        if (region == null || !context.country().equals(region.getCountry())) {
            United.messenger().send(sender, "player.country.setspawn.not-in-region");
            return;
        }

        context.country().setSpawn(location);
        context.country().save();

        United.messenger().send(sender, "player.country.setspawn.success");
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
