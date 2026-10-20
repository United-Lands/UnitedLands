package org.unitedlands.unitedlands.commands.handlers.admin.settlement.banish;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent          = CmdAdminSettlementBanish.class,
        name            = "add",
        description     = "Adds settlement banishment to a player",
        usage           = "/ula settlement banish add <settlement> <player>",
        catchAll        = true
)
public class CmdAdminSettlementBanishAdd extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getSettlementNames(); 
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var settlement = getSettlement(sender, args[0]);
        if (settlement == null) {
            return;
        }

        var citizen = getCitizen(sender, args[1]);
        if (citizen == null) {
            return;
        }

        settlement.addBanished(citizen);
        settlement.save();

        if (citizen.getPlayer().isOnline()) {
            United.messenger().send(citizen.getPlayer(), "player.settlement.addbanish.banished", settlement.getCleanName());
        }

        United.messenger().send(sender, "admin.settlement.addbanish.success", citizen.getName(), settlement.getCleanName());
    }

}
