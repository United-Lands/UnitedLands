package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.managers.FlightManager;
import org.unitedlands.unitedlands.managers.PlayerCacheManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(parent = CmdSettlement.class, name = "fly", description = "Activates flight", usage = "/settlement fly", playerOnly = true)
public class CmdSettlementFly extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        if (!FlightManager.instance().isAllowedToFly(player, PlayerCacheManager.instance().getPlayerCache(player))) {
            United.messenger().send(player, "player.flight.no-permission");
            return;
        }

        FlightManager.instance().activateFlight(player);
        United.messenger().send(player, "player.flight.activated");
    }

}
