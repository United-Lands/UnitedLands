package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.events.settlement.SettlementPlayerLeaveEvent;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlement.class,
        name = "leave",
        description = "Makes the player leave the settlement",
        usage = "/settlement leave",
        playerOnly = true
)
public class CmdSettlementLeave extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, null);
        if (context == null)
            return;

        if (context.citizen().hasSettlementRank("mayor")) {
            United.messenger().send(context.player(), "player.settlement.leave.is_mayor");
            return;
        }

        if (context.citizen().hasCountryRank("leader")) {
            United.messenger().send(context.player(), "player.settlement.leave.is-leader");
            return;
        }

        Confirmation leave = new Confirmation("settlement-leave");
        leave.setRunnable(() -> {

            context.settlement().removeCitizen(context.citizen());
            context.settlement().save();
            
            context.citizen().removeSettlementRanks();
            context.citizen().removeSettlement();
            context.citizen().save();

            (new SettlementPlayerLeaveEvent(context.settlement(), context.player())).callEvent();

            if (ChatChannelManager.instance().isInChannel(context.citizen().getPlayer(), "SETTLEMENT")) {
                ChatChannelManager.instance().switchChannel(context.citizen().getPlayer(), "GLOBAL", null);
            }

            United.messenger().send(context.settlement().getOnlinePlayers(), "player.settlement.player-left", context.player().getName());
            United.messenger().send(context.player(), "player.settlement.leave.settlement-left", context.settlement().getCleanName());

        })
                .setTitle("player.settlement.leave.confirm")
                .setReplacements(Map.of("settlement", context.settlement().getCleanName()))
                .setSender(context.player())
                .setReceiver(context.player())
                .send();
    }

}
