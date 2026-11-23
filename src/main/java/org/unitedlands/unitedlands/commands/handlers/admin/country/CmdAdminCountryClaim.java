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
        name = "claim",
        description = "Claims a region for a country ",
        usage = "/ula country claim <country_name> <region_name> [claim_seconds]",
        catchAll = true
)
public class CmdAdminCountryClaim extends AdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        var country = getCountry(player, args[0]);
        if (country == null) {
            return;
        }

        var region = UnitedLandsDataManager.instance().getRegion(args[1]);
        if (region == null) {
            United.messenger().send(player, "admin.country.claim.no-region", args[1]);
            return;
        } else {
            if (region.getCountry() != null) {
                United.messenger().send(player, "admin.country.claim.already-claimed", region.getCleanName(), region.getCountry().getCleanName());
                return;
            }
        }

        long claimDuration = 0;
        if (args.length > 2) {
            try {
                claimDuration = Long.parseLong(args[2]);
            } catch (Exception ex) {
                United.messenger().send(player, "general-errors.wrong-number-format", args[2]);
            }
        }

        region.setClaimantCountry(country);
        region.setClaimStartTime(System.currentTimeMillis());
        region.setClaimEndTime(System.currentTimeMillis() + (claimDuration * 1000));
        region.startClaimTask();
        region.saveAndRender();
        
        United.messenger().send(player, "admin.country.claim.success", region.getCleanName(), country.getCleanName());
    }

    @Override
    public List<String> handleTab(CommandSender arg0, String[] args) {
        return switch (args.length) {
            case 1 -> UnitedLandsDataManager.instance().getCountryNames();
            case 2 -> UnitedLandsDataManager.instance().getRegionNames();
            default -> null;
        };
    }

}
