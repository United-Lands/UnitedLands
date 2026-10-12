package org.unitedlands.unitedlands.commands.handlers.settlement.set;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementSet.class,
        name = "taxes",
        description = "Sets the settlement taxes",
        usage = "/settlement set taxes <amount>",
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

        if (context.settlement().useTaxPercent()) {
            if (value < UnitedLandsConfig.get().economy().taxes().get("settlement").minPercent()) {
                United.messenger().send(context.player(), "player.settlement.settax.below-min", 
                    String.format("%.2f%%", UnitedLandsConfig.get().economy().taxes().get("settlement").minPercent() * 100));
                value = UnitedLandsConfig.get().economy().taxes().get("settlement").minPercent();
            } else if (value > UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent()) {
                United.messenger().send(context.player(), "player.settlement.settax.above-max", 
                    String.format("%.2f%%", UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent() * 100));
                value = UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent();
            }
        } else {
            if (value < UnitedLandsConfig.get().economy().taxes().get("settlement").minAmount()) {
                United.messenger().send(context.player(), "player.settlement.settax.below-min", 
                    UnitedLandsEconomyManager.instance().format(UnitedLandsConfig.get().economy().taxes().get("settlement").minAmount()));
                value = UnitedLandsConfig.get().economy().taxes().get("settlement").minAmount();
            } else if (value > UnitedLandsConfig.get().economy().taxes().get("settlement").maxAmount()) {
                United.messenger().send(context.player(), "player.settlement.settax.above-max", 
                    UnitedLandsEconomyManager.instance().format(UnitedLandsConfig.get().economy().taxes().get("settlement").maxAmount()));
                value = UnitedLandsConfig.get().economy().taxes().get("settlement").maxAmount();
            }
        }

        context.settlement().setTax((float) value);

        var valueString = context.settlement().useTaxPercent() ? String.format("%.2f%%", value * 100)
                : UnitedLandsEconomyManager.instance().format((double) value);

        context.settlement().save();
        
        United.messenger().send(context.player(), "player.settlement.settax.success", context.settlement().getCleanName(), valueString);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
