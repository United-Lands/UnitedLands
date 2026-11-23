package org.unitedlands.unitedlands.commands.handlers.admin.country.rank;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountryRank.class,
        name = "add",
        description = "Adds a country rank to a player",
        usage = "/ula country rank add <player> <rank>",
        catchAll = true
)
public class CmdAdminCountryRankAdd extends AdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        switch (args.length) {
            case 1:
                return UnitedLandsDataManager.instance().getCountryNames();
            case 2:
                var country = UnitedLandsDataManager.instance().getCountry(args[0]);
                if (country != null)
                    return country.getCitizens().stream().map(Citizen::getName).collect(Collectors.toList());
            case 3:
                return PermissionManager.instance().getCountryRanks();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 3) {
            sendUsage(sender);
            return;
        }

        var country = getCountry(sender, args[0]);
        if (country == null) {
            return;
        }

        var citizen = getCitizen(sender, args[1]);
        if (citizen == null) {
            return;
        }

        if (!citizen.hasCountry() || !country.equals(citizen.getCountry())) {
            United.messenger().send(sender, "admin.country.citizen-not-in-country", citizen.getName(), country.getCleanName());
            return;
        }

        if (!PermissionManager.instance().getCountryRanks().contains(args[2])) {
            United.messenger().send(sender, "admin.country.unknown-rank", args[2]);
            return;
        }

        citizen.addCountryRank(args[2]);
        citizen.save();

        United.messenger().send(sender, "admin.country.addrank.success",
                args[2], citizen.getName(), country.getName());

    }

}
