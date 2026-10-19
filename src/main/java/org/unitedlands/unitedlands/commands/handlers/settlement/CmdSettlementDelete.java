package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdSettlement.class,
    name            = "delete",
    description     = "Deletes a settlement",
    usage           = "/settlement delete",
    playerOnly      = true
)
public class CmdSettlementDelete extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "settlement.delete");
        if (context == null)
            return;
        
        if (context.settlement().hasCountry() && context.settlement().getCountry().getCapital().equals(context.settlement())) {
            United.messenger().send(context.player(), "player.settlement.delete.is-capital");
            return;
        }

        Confirmation delete = new Confirmation("settlement-delete");
        delete.setRunnable(() -> {
            UnitedLandsDataManager.instance().removeSettlement(context.settlement());
            United.messenger().send(Bukkit.getServer(), "player.settlement.delete.broadcast-message", context.settlement().getCleanName());
        })
                .setTitle("player.settlement.delete.CONFIRM")
                .setReplacements(Map.of("settlement", context.settlement().getCleanName()))
                .setSender(context.player())
                .setReceiver(context.player())
                .send();
    }

}
