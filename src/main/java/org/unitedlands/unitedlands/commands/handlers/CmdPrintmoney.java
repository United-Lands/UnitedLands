package org.unitedlands.unitedlands.commands.handlers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.BankRecordTransactionType;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;

@UnitedCommand(
    name            = "printmoney",
    aliases         = { },
    description     = "Givey money out of thin air",
    usage           = "/printmoney",
    playerOnly      = true
)
public class CmdPrintmoney implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) { 
        var player = (Player) sender;
        UnitedLandsEconomyManager.instance().deposit(player.getUniqueId(), 10000, BankRecordTransactionType.SYSTEM, "Conjured up by higher forces");
    }

}
