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
        name = "taxpercent",
        description = "Toggles the settlement tax percent use",
        usage = "/settlement set taxpercent <true|false>",
        playerOnly = true
)
public class CmdSettlementSetTaxPercent extends SettlementCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.usetaxpercent");
        if (context == null)
            return;

        boolean value = Boolean.parseBoolean(args[0]);

        context.settlement().setUseTaxPercent(value);

        var currTax = context.settlement().getTax();
        if (context.settlement().useTaxPercent()) {
            if (currTax < UnitedLandsConfig.get().economy().taxes().get("settlement").minPercent()) {
                United.messenger().send(context.player(), "player.settlement.settax.below-min", String.format("%.2f%%", 
                    UnitedLandsConfig.get().economy().taxes().get("settlement").minPercent() * 100));
                context.settlement().setTax((float)UnitedLandsConfig.get().economy().taxes().get("settlement").minPercent());
            } else if (currTax > UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent()) {
                United.messenger().send(context.player(), "player.settlement.settax.above-max", String.format("%.2f%%", 
                    UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent() * 100));
                context.settlement().setTax((float)UnitedLandsConfig.get().economy().taxes().get("settlement").maxPercent());

            }
        } else {
            if (currTax < UnitedLandsConfig.get().economy().taxes().get("settlement").minAmount()) {
                United.messenger().send(context.player(), "player.settlement.settax.below-min", 
                    UnitedLandsEconomyManager.instance().format(UnitedLandsConfig.get().economy().taxes().get("settlement").minAmount()));
                context.settlement().setTax((float) UnitedLandsConfig.get().economy().taxes().get("settlement").minAmount());

            } else if (currTax > UnitedLandsConfig.get().economy().taxes().get("settlement").maxAmount()) {
                United.messenger().send(context.player(), "player.settlement.settax.above-max", 
                    UnitedLandsEconomyManager.instance().format(UnitedLandsConfig.get().economy().taxes().get("settlement").maxAmount()));
                context.settlement().setTax((float) UnitedLandsConfig.get().economy().taxes().get("settlement").maxAmount());
            }
        }

        context.settlement().save();

        United.messenger().send(context.player(), "player.settlement.usetaxpercent.success", context.settlement().getCleanName(), value == true ? "<green>on</green>" : "<red>off</red>");
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return List.of("true", "false");
        return null;
    }

}
