package org.unitedlands.unitedlands.commands.handlers.admin.settlement;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.LocationMembership;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlement.class,
        name = "permission",
        description = "Sets settlement permissions",
        usage = "/ula settlement permission <settlement> <permission> <group> <on|off>",
        catchAll = true,
        playerOnly = true
)
public class CmdAdminSettlementPermission extends SettlementAdminCommandHandler {

    List<String> permissions = List.of("break", "place", "containers", "switch", "block_use", "interact");
    List<String> memberships = List.of("settlement_residents", "region_residents", "country_residents", "foreigners");
    List<String> switches = List.of("on", "off");

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return UnitedLandsDataManager.instance().getSettlementNames();
            case 2:
                return permissions;
            case 3:
                return memberships;
            case 4:
                return switches;
            default:
                return null;
        }
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 4) {
            sendUsage(sender);
            return;
        }

        var player = (Player) sender;

        var settlement = getSettlement(player, args[0]);
        if (settlement == null) {
            return;
        }

        int membership = 0;
        switch (args[2]) {
            case "settlement_residents":
                membership = LocationMembership.SETTLEMENT_RESIDENT;
                break;
            case "region_residents":
                membership = LocationMembership.REGION_RESIDENT;
                break;
            case "country_residents":
                membership = LocationMembership.COUNTRY_RESIDENT;
                break;
            case "foreigners":
                membership = LocationMembership.FOREIGNER;
                break;
            default:
                United.messenger().send(player, "admin.settlement.permission.unknown-membership", args[1]);
                return;
        }

        boolean add = args[3].equalsIgnoreCase("on");

        int p = 0;
        switch (args[1]) {
            case "break":
                p = settlement.getBreakPermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                settlement.setBreakPermissions(p);
                break;
            case "place":
                p = settlement.getPlacePermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                settlement.setPlacePermissions(p);
                break;
            case "containers":
                p = settlement.getContainerPermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                settlement.setContainerPermissions(p);
                break;
            case "switch":
                p = settlement.getSwitchPermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                settlement.setSwitchPermissions(p);
                break;
            case "block_use":
                p = settlement.getBlockUsePermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                settlement.setBlockUsePermissions(p);
                break;
            case "interact":
                p = settlement.getInteractPermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                settlement.setInteractPermissions(p);
                break;
            default:
                United.messenger().send(player, "admin.settlement.permission.unknown-permission", args[1]);
                return;
        }

        settlement.save();

        United.messenger().send(player, "admin.settlement.permission.success", settlement.getCleanName(), args[1], args[2], add ? "<green>on</green>" : "<red>off</red>");
    }

}
