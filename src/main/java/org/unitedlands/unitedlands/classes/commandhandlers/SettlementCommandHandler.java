package org.unitedlands.unitedlands.classes.commandhandlers;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

public class SettlementCommandHandler implements UnitedCommandExecutor {

    public record SettlementCommandHandlerContext(Player player, Citizen citizen,
            Settlement settlement) {
    }

    @Override
    public void handleCommand(CommandSender arg0, String[] arg1) {

    }

    @Override
    public List<String> handleTab(CommandSender arg0, String[] arg1) {
        return null;
    }

    protected SettlementCommandHandlerContext validate(CommandSender sender, String permission) {

        var player = (Player) sender;
        
        if (!GeneralConfig.get().general().worlds().contains(player.getLocation().getWorld().getName())) {
            United.messenger().send(player, "general-errors.wrong-world");
            return null;
        }
        
        var citizen = getCitizen(player);
        if (citizen == null)
            return null;
        var settlement = getCitizenSettlement(citizen);
        if (settlement == null)
            return null;

        if (permission != null)
            if (!hasPermission(permission, citizen))
                return null;

        return new SettlementCommandHandlerContext(player, citizen, settlement);
    }

    protected Citizen getCitizen(Player player) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.messenger().send(player, "general-errors.no-citizen-data");
            return null;
        }
        return citizen;
    }

    protected Settlement getCitizenSettlement(Citizen citizen) {
        if (citizen.getSettlement() == null) {
            United.messenger().send((Player) citizen.getPlayer(), "general-errors.not-in-settlement");
            return null;
        }
        return citizen.getSettlement();
    }

    protected boolean hasPermission(String permission, Citizen citizen) {
        if (!PermissionManager.instance().hasRankPermission(permission, citizen)) {
            United.messenger().send((Player) citizen.getPlayer(), "general-errors.no-settlement-permission", permission);
            return false;
        }
        return true;
    }

}
