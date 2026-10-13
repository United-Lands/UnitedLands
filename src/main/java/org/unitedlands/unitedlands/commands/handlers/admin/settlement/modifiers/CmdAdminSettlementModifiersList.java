package org.unitedlands.unitedlands.commands.handlers.admin.settlement.modifiers;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementModifiers.class,
        name = "list",
        description = "Lists all settlement modifiers",
        usage = "/ula settlement modifiers list <settlement_name>"
)

public class CmdAdminSettlementModifiersList extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender semder, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getSettlementNames();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        var settlement = getSettlement(sender, args[0]);
        if (settlement == null) {
            return;
        }

        United.messenger().sendRaw(sender, "<aqua><bold>" + settlement.getCleanName() + " Modifiers</bold></aqua>");

        var modifiers = settlement.getAttributeModifiers();
        for (var mod : modifiers) {
            String entry = "<bold>" + mod.getAttributeKey() + "</bold> - Modifier key: " + mod.getModifierKey() + " | Mode: " + mod.getMode() +
                    " | Value modifier: " + mod.getValueModifier() + " | Max value modifier: " + mod.getMaxValueModifier() + " | Min value modifier: "
                    + mod.getMinValueModifier() + " | Daily change modifier: " + mod.getDailyChangeModifier();
            United.messenger().sendRaw(sender, entry);
        }
    }

}
