package org.unitedlands.unitedlands.commands.handlers.settlement.set;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementSet.class,
        name = "name",
        description = "Sets the settlement name",
        usage = "/settlement set name <new_name>",
        playerOnly = true
)
public class CmdSettlementSetName extends SettlementCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.setname");
        if (context == null)
            return;

        if (ColorUtils.containsColourCharacters(args[0])) {
            United.messenger().send(sender, "general-errors.illegal-characters", args[0]);
            return;
        }

        var oldname = context.settlement().getName();

        context.settlement().setName(args[0]);
        context.settlement().saveAndRender();

        United.messenger().send(context.player(), "player.settlement.setname.success", oldname, context.settlement().getName());
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
