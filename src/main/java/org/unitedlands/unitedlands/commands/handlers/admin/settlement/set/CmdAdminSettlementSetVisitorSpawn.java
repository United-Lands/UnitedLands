package org.unitedlands.unitedlands.commands.handlers.admin.settlement.set;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementSet.class,
        name = "visitorspawn",
        description = "Changes a settlement's spawn",
        usage = "/ula settlement set spawn <settlement_name>",
        playerOnly = true
)
public class CmdAdminSettlementSetVisitorSpawn extends SettlementAdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var settlement = getSettlement(player, args[0]);
        if (settlement == null) {
            return;
        }

        var chunkCoordinates = CoordinateUtils.locationToChunkCoordinates(player.getLocation());
        if (!settlement.hasChunkAtCoordinates(chunkCoordinates)) {
            United.messenger().send(player, "admin.settlement.setspawn.not-in-claims");
            return;
        }

        settlement.setVisitorSpawn(player.getLocation());
        settlement.save();

        United.messenger().send(player, "admin.settlement.setspawn.success", settlement.getCleanName());
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getSettlementNames();
        return null;
    }

}
