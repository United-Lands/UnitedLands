package org.unitedlands.unitedlands.commands.handlers.country.set;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountrySet.class,
        name = "taxes",
        description = "Sets the country taxes",
        usage = "/country set taxes <percentage>",
        playerOnly = true
)
public class CmdCountrySetTaxes extends CountryCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "country.settaxes");
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

        if (value < UnitedLandsConfig.get().economy().taxes().get("country").minPercent()) {
            United.messenger().send(context.player(), "player.country.settax.below-min", 
                String.format("%.2f%%", UnitedLandsConfig.get().economy().taxes().get("country").minPercent() * 100));
            value = UnitedLandsConfig.get().economy().taxes().get("country").minPercent();
        } else if (value > UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent()) {
            United.messenger().send(context.player(), "player.country.settax.above-max", 
                String.format("%.2f%%", UnitedLandsConfig.get().economy().taxes().get("country").maxPercent() * 100));
            value = UnitedLandsConfig.get().economy().taxes().get("country").maxPercent();
        }

        context.country().setTax((float) value);
        context.country().save();
        
        United.messenger().send(context.player(), "player.country.settax.success", context.country().getCleanName(), String.format("%.2f%%", value * 100));
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
