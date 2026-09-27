package org.unitedlands.unitedlands.classes.commandhandlers;

import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

public class CountryCommandHandler implements UnitedCommandExecutor {

    public record CountryCommandHandlerContext(Player player, Citizen citizen,
            Country country) {
    }

    @Override
    public void handleCommand(CommandSender arg0, String[] arg1) {

    }

    @Override
    public List<String> handleTab(CommandSender arg0, String[] arg1) {
        return null;
    }

    protected CountryCommandHandlerContext validate(CommandSender sender, String permission) {

        var player = (Player) sender;

        if (!GeneralConfig.get().general().worlds().contains(player.getLocation().getWorld().getName())) {
            United.messenger().send(player, "general-errors.wrong-world");
            return null;
        }

        var citizen = getCitizen(player);
        if (citizen == null)
            return null;
        var country = getCitizenCountry(citizen);
        if (country == null)
            return null;

        if (permission != null)
            if (!hasPermission(permission, citizen))
                return null;

        return new CountryCommandHandlerContext(player, citizen, country);
    }

    protected Citizen getCitizen(Player player) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.messenger().send(player, "general-errors.no-citizen-data");
            return null;
        }
        return citizen;
    }

    protected Country getCitizenCountry(Citizen citizen) {
        if (citizen.getCountry() == null) {
            United.messenger().send((Player) citizen.getPlayer(), "general-errors.player-not-in-country");
            return null;
        }
        return citizen.getCountry();
    }

    protected boolean hasPermission(String permission, Citizen citizen) {
        if (!UnitedLands.instance().getPermissionManager().hasRankPermission(permission, citizen)) {
            United.messenger().send((Player) citizen.getPlayer(), "general-errors.no-country-permission",
                    Map.of("perm", permission));
            return false;
        }
        return true;
    }

}
