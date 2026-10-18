package org.unitedlands.unitedlands.commands.handlers.region.set;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.RegionCommandHandler;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdRegionSet.class,
    name            = "name",
    description     = "Set the name of a region",
    usage           = "/region set name <new_name>",
    playerOnly      = true
)
public class CmdRegionSetName extends RegionCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "region.setname");
        if (context == null)
            return;

        if (ColorUtils.containsColourCharacters(args[0])) {
            United.messenger().send(sender, "general-errors.illegal-characters", args[0]);
            return;
        }

        var oldname = context.region().getName();

        context.region().setName(args[0]);
        context.region().saveAndRender();

        United.messenger().send(context.player(), "player.region.setname.success", oldname, context.region().getName());

    }

}
