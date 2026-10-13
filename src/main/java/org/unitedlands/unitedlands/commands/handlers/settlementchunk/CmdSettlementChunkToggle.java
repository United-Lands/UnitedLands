package org.unitedlands.unitedlands.commands.handlers.settlementchunk;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementChunkCommandHandler;

import org.unitedlands.utils.United;

@UnitedSubCommand(parent = CmdSettlementChunk.class, name = "toggle", description = "Sets settlement chunk toggles", usage = "/settlementchunk toggle <toggle> <on|off>", playerOnly = true, catchAll = true)
public class CmdSettlementChunkToggle extends SettlementChunkCommandHandler {

    List<String> fields = List.of("pvp", "monsters", "animals", "fire", "explosions");
    List<String> switches = List.of("on", "off", "unset");

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

        var context = validate(sender, null);
        if (context == null)
            return;

        if (!(isOwner(context.settlementChunk(), context.citizen())
                || hasPermission("settlement.plot.permissions", context.citizen(), context.settlementChunk()))) {
            return;
        }

        Boolean enable = null;
        if (args[1].equalsIgnoreCase("on")) {
            enable = true;
        } else if (args[1].equalsIgnoreCase("off")) {
            enable = false;
        }

        switch (args[0]) {
            case "pvp":
                context.settlementChunk().setAllowPvp(enable);
                break;
            case "monsters":
                context.settlementChunk().setAllowMonsters(enable);
                break;
            case "animals":
                context.settlementChunk().setAllowAnimals(enable);
                break;
            case "fire":
                context.settlementChunk().setAllowFire(enable);
                break;
            case "explosions":
                context.settlementChunk().setAllowExplosions(enable);
                break;
            default:
                United.messenger().send(context.player(), "player.settlement.toggle.unknown-toggle", args[0]);
                return;
        }

        context.settlementChunk().save();

        United.messenger().send(context.player(), "player.settlementchunk.toggle.success", args[0],
                enable != null ? (enable == true ? "<green>on</green>" : "<red>off</red>") : "<yellow>unset</yellow>");
    }

}
