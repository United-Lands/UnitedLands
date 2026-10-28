package org.unitedlands.unitedlands.commands.handlers.country.set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdCountrySet.class, 
    name = "iconlocation", 
    description = "Sets the country icon location", 
    usage = "/country set iconlocation", 
    playerOnly = true
)
public class CmdCountrySetIconLocation extends CountryCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "country.seticon");
        if (context == null)
            return;

        if (context.country().getIconPosition() == null) {
            United.messenger().send(sender, "player.country.seticonlocation.no-icon");
            return;
        }

        var player = (Player) sender;
        var region = UnitedLandsDataManager.instance().getRegion(player.getLocation());
        if (region == null || !context.country().equals(region.getCountry())) {
            United.messenger().send(sender, "player.country.seticonlocation.not-in-region");
            return;           
        }

        context.country().setIconPosition(player.getLocation());
        context.country().saveAndRender();

        United.messenger().send(sender, "player.country.seticonlocation.success");
    }

}