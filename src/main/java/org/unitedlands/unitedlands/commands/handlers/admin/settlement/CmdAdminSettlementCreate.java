package org.unitedlands.unitedlands.commands.handlers.admin.settlement;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementAdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlement.class,
        name = "create",
        description = "Creates a settlement",
        usage = "/ula settlement create <settlement_name>",
        playerOnly = true
)
public class CmdAdminSettlementCreate extends SettlementAdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var chunkCoords = CoordinateUtils.locationToChunkCoordinates(player.getLocation());
        var existingChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoords);
        if (existingChunk != null) {
            United.messenger().send(player, "admin.settlement.create.already-claimed");
            return;
        }

        var region = UnitedLandsDataManager.instance()
                .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(player.getLocation()));
        

        var settlement = UnitedLandsDataManager.instance().createSettlement(args[0], chunkCoords, player.getLocation(), region, null);

        United.messenger().send(player, "admin.settlement.create.success", settlement.getCleanName());
    }

}
