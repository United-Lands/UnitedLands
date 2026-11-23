package org.unitedlands.unitedlands.commands.handlers.admin.country;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountry.class,
        name = "unclaim",
        description = "Unclaims a region",
        usage = "/ula country unclaim <region_name>",
        catchAll = true
)
public class CmdAdminCountryUnclaim extends AdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var region = UnitedLandsDataManager.instance().getRegion(args[0]);
        if (region == null) {
            United.messenger().send(player, "admin.country.claim.no-region", args[0]);
            return;
        } else {
            if (region.getCountry() == null) {
                United.messenger().send(player,
                        "admin.country.unclaim.not-claimed");
                return;
            }
        }

        var country = region.getCountry();

        for (var settlement : region.getSettlements()) {
            for (var citizen : settlement.getCitizens()) {
                citizen.removeCountryRanks();
                citizen.save();
            }
            settlement.removeCountry();
            settlement.saveAndRender();
        }
        region.removeCountry();
        region.saveAndRender();

        country.removeRegion(region);
        country.saveAndRender();

        United.messenger().send(player, "admin.country.unclaim.success", region.getCleanName(), country.getCleanName());
    }

    @Override
    public List<String> handleTab(CommandSender arg0, String[] args) {
        return switch (args.length) {
            case 1 -> UnitedLandsDataManager.instance().getRegionNames();
            default -> null;
        };
    }

}
