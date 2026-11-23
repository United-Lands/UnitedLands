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
        name = "iconlocation",
        description = "Sets a country icon location to your current position",
        usage = "/ula country set iconlocation <country>",
        playerOnly = true,
        catchAll = true
)
public class CmdAdminCountrySetIconLocation extends AdminCommandHandler {

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

        if (country.getIconPosition() == null) {
            United.messenger().send(sender, "admin.country.seticonlocation.no-icon", country.getCleanName());
            return;
        }

        var player = (Player) sender;
        country.setIconPosition(player.getLocation());
        country.saveAndRender();

        United.messenger().send(sender, "admin.country.seticonlocation.success", country.getCleanName());
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getCountryNames();
        }
        return null;
    }

}
