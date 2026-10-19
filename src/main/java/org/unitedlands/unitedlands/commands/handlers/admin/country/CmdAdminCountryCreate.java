package org.unitedlands.unitedlands.commands.handlers.admin.country;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountry.class,
        name = "create",
        description = "Created a country with a capital",
        usage = "/ula country create <country_name> <capital>",
        playerOnly = true
)
public class CmdAdminCountryCreate extends CountryAdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var player = (Player) sender;

        if (!hasPermission(player)) {
            return;
        }

        var settlement = getSettlement(player, args[1]);
        if (settlement == null) {
            return;
        }

        var region = settlement.getRegion();

        if (region == null) {
            United.messenger().send(player, "admin.country.create.no-region", settlement.getCleanName());
            return;
        }

        if (settlement.hasCountry()) {
            United.messenger().send(player, "admin.country.create.already-in-country", settlement.getCleanName());
            return;
        }

        if (region.hasCountry()) {
            United.messenger().send(player, "admin.country.create.region-occupied", settlement.getCleanName());
            return;
        }

        var country = UnitedLandsDataManager.instance().createCountry(args[0], region, settlement, null);

        United.messenger().send(player, "admin.country.create.success", country.getCleanName(), settlement.getName(), region.getName());

    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return UnitedLandsDataManager.instance().getSettlementNames();
        }
        return null;
    }

}
