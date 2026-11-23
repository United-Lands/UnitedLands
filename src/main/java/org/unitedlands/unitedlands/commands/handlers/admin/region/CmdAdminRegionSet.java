package org.unitedlands.unitedlands.commands.handlers.admin.region;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminRegion.class,
        name = "set",
        description = "Sets a region to a country",
        usage = "/ula region set <region> <countr>"
)

public class CmdAdminRegionSet extends AdminCommandHandler {

    
    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> UnitedLandsDataManager.instance().getRegionNames();
            case 2 -> UnitedLandsDataManager.instance().getCountryNames();
            default -> null;
        };
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
        
        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var region = getRegion(sender, args[0]);
        if (region == null) {
            return;
        }

        var country = getCountry(sender, args[1]);
        if (country == null) {
            return;
        }

        if (region.hasCountry()) {
            var previousCountry = region.getCountry();
            previousCountry.removeRegion(region);
            previousCountry.saveAndRender();
        }

        region.setCountry(country);
        region.saveAndRender();
        country.addRegion(region);
        country.saveAndRender();

        United.messenger().send(sender, "admin.region.set.success", region.getCleanName(), country.getCleanName());

    }

}
