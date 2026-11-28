package org.unitedlands.unitedlands.commands.handlers.admin.country.set;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountrySet.class,
        name = "spawn",
        description = "Sets a country spawn to your current position",
        usage = "/ula country set spawn <country>",
        playerOnly = true,
        catchAll = true
)
public class CmdAdminCountrySetSpawn extends AdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var country = getCountry(sender, args[0]);
        if (country == null) {
            return;
        }

        var player = (Player) sender;
        var location = player.getLocation();
        var region = UnitedLandsDataManager.instance().getRegion(location);
        if (region == null || !country.equals(region.getCountry())) {
            United.messenger().send(sender, "admin.country.setspawn.not-in-region", country.getCleanName());
            return;
        }

        country.setSpawn(location);
        country.save();

        United.messenger().send(sender, "admin.country.setspawn.success", country.getCleanName());
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getCountryNames();
        }
        return null;
    }

}
