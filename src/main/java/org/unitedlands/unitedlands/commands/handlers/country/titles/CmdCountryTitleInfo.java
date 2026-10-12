package org.unitedlands.unitedlands.commands.handlers.country.titles;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountryTitle.class,
        name = "info",
        description = "Shows country title information",
        usage = "/country title info <title>",
        playerOnly = true
)
public class CmdCountryTitleInfo implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsTitlesConfig.get().titles().keys().stream().toList();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var titleConfig = UnitedLandsTitlesConfig.get().titles().get(args[0]);
        if (titleConfig == null) {
            return;
        }

        United.messenger().sendRaw(sender, "<gold><bold>" + titleConfig.displayName() + "</bold></gold>");
        //United.messenger().sendRaw(sender, titleConfig.description());
        United.messenger().sendRaw(sender, "<bold>Effects when claimed:</bold>");
        United.messenger().sendRaw(sender, titleConfig.effectsDescription());
    }

}
