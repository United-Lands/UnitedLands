package org.unitedlands.unitedlands.commands.handlers.admin.settlement.modifiers;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.commands.handlers.admin.settlement.CmdAdminSettlement;

@UnitedSubCommand(
        parent = CmdAdminSettlement.class,
        name = "modifiers",
        description = "Admin settlement modifier commands",
        usage = "/ula settlement modifiers <command> <argument>"
)
public class CmdAdminSettlementModifiers implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) { }

}
