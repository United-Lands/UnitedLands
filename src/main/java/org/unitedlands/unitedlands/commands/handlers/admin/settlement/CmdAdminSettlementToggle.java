package org.unitedlands.unitedlands.commands.handlers.admin.settlement;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlement.class,
        name = "toggle",
        description = "Sets settlement toggles",
        usage = "/ula settlement toggle <settlement> <toggle> <on|off>",
        catchAll = true,
        playerOnly = true
)
public class CmdAdminSettlementToggle extends SettlementAdminCommandHandler {

    List<String> fields = List.of("pvp", "monsters", "animals", "fire", "explosions", "public");
    List<String> switches = List.of("on", "off", "unset");

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return UnitedLandsDataManager.instance().getSettlementNames();
            case 2:
                return fields;
            case 3:
                return switches;
            default:
                return null;
        }
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 3) {
            sendUsage(sender);
            return;
        }

        var player = (Player) sender;

        var settlement = getSettlement(player, args[0]);
        if (settlement == null) {
            return;
        }

        Boolean enable = null;
        if (args[2].equalsIgnoreCase("on")) {
            enable = true;
        } else if (args[1].equalsIgnoreCase("off")) {
            enable = false;
        }

        switch (args[1]) {
            case "public":
                // public can't be inherited, enforce value
                if (enable == null)
                    enable = false;
                settlement.setPublic(enable);
                break;
            case "pvp":
                settlement.setAllowPvp(enable);
                break;
            case "monsters":
                settlement.setAllowMonsters(enable);
                break;
            case "animals":
                settlement.setAllowAnimals(enable);
                break;
            case "fire":
                settlement.setAllowFire(enable);
                break;
            case "explosions":
                settlement.setAllowExplosions(enable);
                break;
            default:
                United.messenger().send(player, "admin.settlement.toggle.unknown-toggle", args[1]);
                return;
        }

        settlement.save();

        United.messenger().send(player, "admin.settlement.toggle.success", 
            settlement.getCleanName(),
            args[1], 
            enable != null ? (enable == true ? "<green>on</green>" : "<red>off</red>") : "<yellow>unset</yellow>");
    }

}
