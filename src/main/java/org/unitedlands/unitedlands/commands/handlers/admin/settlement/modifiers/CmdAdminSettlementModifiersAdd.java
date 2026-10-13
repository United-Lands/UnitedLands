package org.unitedlands.unitedlands.commands.handlers.admin.settlement.modifiers;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.GeopolAttributeModifier;
import org.unitedlands.unitedlands.classes.GeopolAttributeModifier.Mode;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementModifiers.class,
        name = "add",
        description = "Adds a settlement modifier",
        usage = "/ula settlement modifiers add <settlement_name> <attr_key> <mod_key> <mode> <value_mod> <max_value_mod> <min_value_mod> <daily_change_mod>",
        catchAll = true
)

public class CmdAdminSettlementModifiersAdd extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender semder, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getSettlementNames();
        } else if (args.length == 2) {
            return UnitedLandsConfig.get().geopolAttributeDefaults().settlement().keys().stream().toList();
        } else if (args.length == 4) {
            return Arrays.stream(Mode.values()).map(Enum::toString).toList();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 8) {
            sendUsage(sender);
            return;
        }

        var settlement = getSettlement(sender, args[0]);
        if (settlement == null) {
            return;
        }

        var mode = Mode.ADD;
        try {
            mode = Mode.valueOf(args[3]);
        } catch (Exception ex) {
            United.logger().warning("Not a valid attribute modifier mode: " + args[3]);
            return;
        }

        var valueMod = 0d;
        try {
            valueMod = Double.parseDouble(args[4]);
        } catch (Exception ex) {
            United.logger().warning("Invalid number: " + args[4]);
            return;
        }

        var maxValueMod = 0d;
        try {
            maxValueMod = Double.parseDouble(args[5]);
        } catch (Exception ex) {
            United.logger().warning("Invalid number: " + args[5]);
            return;
        }

        var minValueMod = 0d;
        try {
            minValueMod = Double.parseDouble(args[6]);
        } catch (Exception ex) {
            United.logger().warning("Invalid number: " + args[6]);
            return;
        }

        var dailyChangeMod = 0d;
        try {
            dailyChangeMod = Double.parseDouble(args[7]);
        } catch (Exception ex) {
            United.logger().warning("Invalid number: " + args[7]);
            return;
        }

        GeopolAttributeModifier attrModifier = new GeopolAttributeModifier(args[1], args[2], mode, valueMod, maxValueMod, minValueMod, dailyChangeMod);
        settlement.addAttributeModifier(attrModifier);
        settlement.save();

        // TODO: Move strings to config
        United.messenger().sendRaw(sender, "<green>Attribute modifier added to " + settlement.getCleanName());

    }

}
