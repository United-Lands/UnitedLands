package org.unitedlands.unitedlands.commands.handlers.region.set;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.RegionCommandHandler;

import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdRegionSet.class,
        name = "admintitle",
        description = "Sets the administratator title of a region",
        usage = "/region set admintitle <title>",
        playerOnly = true
)
public class CmdRegionSetAdministratorTitle extends RegionCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "region.setadministrator");
        if (context == null)
            return;

        if (args[0].equalsIgnoreCase("clear")) {
            context.region().setAdministratorTitle(null);
            context.region().saveAndRender();
        } else {
            context.region().setAdministratorTitle(ColorUtils.stripAllColors(args[0]));
            context.region().saveAndRender();
        }

        United.messenger().send(context.player(), "player.region.setadministratortitle.success", context.region().getAdministratorTitle());
    }

}
