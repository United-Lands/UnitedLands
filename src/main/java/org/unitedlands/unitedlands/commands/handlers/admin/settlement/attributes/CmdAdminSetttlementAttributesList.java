package org.unitedlands.unitedlands.commands.handlers.admin.settlement.attributes;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementAttributes.class,
        name = "list",
        description = "Lists all settlement attribues",
        usage = "/ula settlement attributes list"
)

public class CmdAdminSetttlementAttributesList extends AdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var settlement = getSettlement(sender, args[0]);
        if (settlement == null) {
            return;
        }

        var attributes = settlement.getAttributes();
        for (var set : attributes.entrySet()) {
            var v = set.getValue();
            String entry = "<bold>" + set.getKey() + "</bold> - Current value: " + v.getCurrentValue() + " | Min value: " + v.getMinValue() + " | Max value: " + v.getMaxValue() + " | Daily change: "
                    + v.getDailyChange();
            United.messenger().sendRaw(sender, entry);
        }
    }

}
