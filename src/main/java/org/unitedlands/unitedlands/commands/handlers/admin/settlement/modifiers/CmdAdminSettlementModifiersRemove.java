package org.unitedlands.unitedlands.commands.handlers.admin.settlement.modifiers;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.GeopolAttributeModifier;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementModifiers.class,
        name = "remove",
        description = "Removes a settlement modifier",
        usage = "/ula settlement modifiers remove <settlement_name> <attr_key> <mod_key>",
        catchAll = true
)

public class CmdAdminSettlementModifiersRemove extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getSettlementNames();
        } else if (args.length == 2) {
            var settlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
            if (settlement != null) {
                var mods = settlement.getAttributeModifiers();
                    return mods.stream().map(GeopolAttributeModifier::getAttributeKey).distinct().toList();
            } 
        } else if (args.length == 3) {
            var settlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
            if (settlement != null) {
                var mods = settlement.getAttributeModifiers();
                    return mods.stream().map(GeopolAttributeModifier::getModifierKey).distinct().toList();
            } 
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 3) {
            sendUsage(sender);
            return;
        }

        var settlement = getSettlement(sender, args[0]);
        if (settlement == null) {
            return;
        }

        settlement.removeAttributeModifiers(args[1], args[2]);

        // TODO: Move strings to config
        United.messenger().sendRaw(sender, "<green>Removed all modifiers for attribute " + args[1] + " with key " + args[2]);

    }

}
