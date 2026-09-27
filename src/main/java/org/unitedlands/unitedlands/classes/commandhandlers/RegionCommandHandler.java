package org.unitedlands.unitedlands.classes.commandhandlers;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.PlayerCacheManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

public class RegionCommandHandler implements UnitedCommandExecutor{

    public record RegionCommandHandlerContext(Player player, Citizen citizen,
            Region region) {
    }

    @Override
    public void handleCommand(CommandSender arg0, String[] arg1) {

    }

    @Override
    public List<String> handleTab(CommandSender arg0, String[] arg1) {
        return null;
    }

    protected RegionCommandHandlerContext validate(CommandSender sender, String permission) {

        var player = (Player) sender;

        if (!GeneralConfig.get().general().worlds().contains(player.getLocation().getWorld().getName())) {
            United.messenger().send(player, "general-errors.wrong-world");
            return null;
        }
                
        var citizen = getCitizen(player);
        if (citizen == null)
            return null;

        var playerCache = PlayerCacheManager.instance().getPlayerCache(player);
        if (playerCache.getCachedRegion() == null)
            return null;

        var region = playerCache.getCachedRegion();
        if (!region.hasCountry() || !region.getCountry().equals(citizen.getCountry())) {
            United.messenger().send(player, "general-errors.no-region-permission");
            return null;
        }

        if (permission != null) {
            if (!hasPermission(permission, citizen)) {
                // Special region check: Region administrators have access to all region commands
                if (!citizen.equals(region.getAdministrator())) {
                    return null;
                }
            }
        }

        return new RegionCommandHandlerContext(player, citizen, playerCache.getCachedRegion());
    }

    protected Citizen getCitizen(Player player) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.messenger().send(player, "general-errors.no-citizen-data");
            return null;
        }
        return citizen;
    }

    protected boolean hasPermission(String permission, Citizen citizen) {
        if (!PermissionManager.instance().hasRankPermission(permission, citizen)) {
            United.messenger().send((Player) citizen.getPlayer(), "general-errors.no-country-permission", permission);
            return false;
        }
        return true;
    }

}
