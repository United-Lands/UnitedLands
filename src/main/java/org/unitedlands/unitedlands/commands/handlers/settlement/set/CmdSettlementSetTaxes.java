package org.unitedlands.unitedlands.commands.handlers.settlement.set;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementSet.class,
        name = "taxes",
        description = "Sets the settlement taxes",
        usage = "/settlement set taxes <percentage>",
        playerOnly = true
)
public class CmdSettlementSetTaxes extends SettlementCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.settaxes");
        if (context == null)
            return;

        double value = 0.0f;
        try {
            value = Float.parseFloat(args[0]);
        } catch (NumberFormatException ex) {
            United.messenger().send(context.player(), "general-errors.wrong-number-format", args[0]);
            return;
        }

        // If players enter percentage as full numbers (e.g. 25), divide by 100
        if (value > 1.0) {
            value /= 100.0;
        }

        if (value < UnitedLandsConfig.get().economy().taxes().get("settlement").minPercent()) {
            United.messenger().send(context.player(), "player.settlement.settax.below-min", 
                String.format("%.2f%%", UnitedLandsConfig.get().economy().taxes().get("settlement").minPercent() * 100));
            value = UnitedLandsConfig.get().economy().taxes().get("settlement").minPercent();
        } else if (value > UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent()) {
            United.messenger().send(context.player(), "player.settlement.settax.above-max", 
                String.format("%.2f%%", UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent() * 100));
            value = UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent();
        }

        context.settlement().setTax((float) value);
        context.settlement().save();
        
        United.messenger().send(context.player(), "player.settlement.settax.success", context.settlement().getCleanName(), String.format("%.2f%%", value * 100));
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
