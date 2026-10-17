package org.unitedlands.unitedlands.commands.handlers.admin.settlementchunk;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.LocationMembership;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementChunkCommandHandler;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdAdminSettlementChunk.class, 
    name = "permission", 
    description = "Sets settlement chunk permissions",
    usage = "/ula settlementchunk permission <permission> <group> <on|off>", 
    playerOnly = true, 
    catchAll = true
)
public class CmdAdminSettlementChunkPermission extends SettlementChunkCommandHandler {

    List<String> permissions = List.of("break", "place", "containers", "switch", "block_use", "interact");
    List<String> memberships = List.of("settlement_residents", "region_residents", "country_residents", "foreigners");
    List<String> switches = List.of("on", "off");

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return permissions;
            case 2:
                return memberships;
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

        var context = validate(sender, null);
        if (context == null)
            return;

        int membership = 0;
        switch (args[1]) {
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
                United.messenger().send(context.player(), "player.settlement.permission.unknown-membership", args[1]);
                return;
        }

        boolean add = args[2].equalsIgnoreCase("on");

        int p = 0;
        switch (args[0]) {
            case "break":
                p = context.settlementChunk().getBreakPermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                context.settlementChunk().setBreakPermissions(p);
                break;
            case "place":
                p = context.settlementChunk().getPlacePermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                context.settlementChunk().setPlacePermissions(p);
                break;
            case "containers":
                p = context.settlementChunk().getContainerPermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                context.settlementChunk().setContainerPermissions(p);
                break;
            case "switch":
                p = context.settlementChunk().getSwitchPermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                context.settlementChunk().setSwitchPermissions(p);
                break;
            case "block_use":
                p = context.settlementChunk().getBlockUsePermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                context.settlementChunk().setBlockUsePermissions(p);
                break;
            case "interact":
                p = context.settlementChunk().getInteractPermissions();
                if (add)
                    p |= membership;
                else
                    p &= ~membership;
                context.settlementChunk().setInteractPermissions(p);
                break;
            default:
                United.messenger().send(context.player(),
                        "player.settlement.permission.unknown-permission", args[0]);
                return;
        }

        context.settlementChunk().save();

        United.messenger().send(context.player(), "player.settlementchunk.permission.success", args[0], args[1],
                add ? "<green>on</green>" : "<red>off</red>");
    }

}
