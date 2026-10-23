package org.unitedlands.unitedlands.commands.handlers.region;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.RegionCommandHandler;

import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdRegion.class,
    name            = "toggle",
    usage           = "/region toggle <toggle> <on|off>",
    playerOnly      = true,
    catchAll        = true
)
public class CmdRegionToggle extends RegionCommandHandler {

    List<String> fields = List.of("pvp", "monsters", "animals", "fire", "explosions");
    List<String> switches = List.of("on", "off");

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
        case 1:
            return fields;
        case 2:
            return switches;
        default:
            return null;
        }
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "region.manage.toggle");
        if (context == null)
            return;

        Boolean enable = null;
        if (args[1].equalsIgnoreCase("on")) {
            enable = true;
        } else {
            enable = false;
        }

        switch (args[0]) {
        case "pvp":
            context.region().setAllowPvp(enable);
            break;
        case "monsters":
            context.region().setAllowMonsters(enable);
            break;
        case "animals":
            context.region().setAllowAnimals(enable);
            break;
        case "fire":
            context.region().setAllowFire(enable);
            break;
        case "explosions":
            context.region().setAllowExplosions(enable);
            break;
        default:
            United.messenger().send(context.player(), "player.region.toggle.unknown-toggle", args[0]);
            return;
        }

        context.region().saveAndRender();

        United.messenger().send(context.player(), "player.region.toggle.success", 
            args[0], 
            enable != null ? (enable == true ? "<green>on</green>" : "<red>off</red>") : "<yellow>unset</yellow>",
            context.region().getCleanName());
    }

}
