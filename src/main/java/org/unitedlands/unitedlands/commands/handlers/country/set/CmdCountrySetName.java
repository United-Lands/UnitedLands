package org.unitedlands.unitedlands.commands.handlers.country.set;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountrySet.class,
        name = "name",
        description = "Sets the country name",
        usage = "/country set name <new_name>",
        playerOnly = true
)
public class CmdCountrySetName extends CountryCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "country.setname");
        if (context == null)
            return;

        if (ColorUtils.containsColourCharacters(args[0])) {
            United.messenger().send(sender, "general-errors.illegal-characters", args[0]);
            return;
        }

        context.country().setName(args[0]);
        context.country().saveAndRender();

        United.messenger().send(context.player(), "player.country.setname.success", context.country().getCleanName());
    }

}
