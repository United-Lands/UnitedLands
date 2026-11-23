package org.unitedlands.unitedlands.classes.commandhandlers;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.utils.United;

public class AdminCommandHandler implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender arg0, String[] arg1) {

    }

    @Override
    public List<String> handleTab(CommandSender arg0, String[] arg1) {
        return null;
    }

    protected Country getCountry(CommandSender sender, String name) {
        var country = UnitedLandsDataManager.instance().getCountry(name);
        if (country == null) {
            United.messenger().send(sender, "general-errors.country-not-found", name);
            return null;
        }
        return country;
    }

    protected Region getRegion(CommandSender sender, String name) {
        var region = UnitedLandsDataManager.instance().getRegion(name);
        if (region == null) {
            United.messenger().send(sender, "general-errors.region-not-found", name);
            return null;
        }
        return region;
    }

    protected Settlement getSettlement(CommandSender sender, String name) {
        var settlement = UnitedLandsDataManager.instance().getSettlement(name);
        if (settlement == null) {
            United.messenger().send(sender, "general-errors.settlement-not-found", name);
            return null;
        }
        return settlement;
    }

    protected Citizen getCitizen(CommandSender player, String name) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(name);
        if (citizen == null) {
            United.messenger().send(player, "general-errors.citizen-not-found", name);
            return null;
        }
        return citizen;
    }

    protected boolean hasPermission(Player player) {
        if (!PermissionManager.instance().hasGlobalOverrides(player)) {
            United.messenger().send((Player) player, "general-errors.no-permission");
            return false;
        }
        return true;
    }

}
