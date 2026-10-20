package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.managers.PlayerCacheManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdSettlement.class,
    name            = "autoclaim",
    description     = "Toggles autoclaim for the settlement",
    usage           = "/settlement autoclaim <on|off>",
    playerOnly      = true
)
public class CmdSettlementAutoclaim extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off");
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return ;
        }

        var context = validate(sender, "settlement.claim");
        if (context == null)
            return;

        var player = (Player) sender;

        var chunkCoords = CoordinateUtils.locationToChunkCoordinates(player.getLocation());

        var existingChunk = UnitedLandsDataManager.instance().getSettlementChunk(chunkCoords);
        if (existingChunk == null) {
            United.messenger().send(player, "player.settlement.autoclaim.not-in-chunk");
            return;
        }

        var cache = PlayerCacheManager.instance().getPlayerCache(player);

        if (args[0].equalsIgnoreCase("on")) {
            cache.setAutoclaimActive(true);
            cache.setAutoclaimSettlement(context.settlement());
            United.messenger().send(player, "player.settlement.autoclaim.switched-on");
        } else {
            cache.setAutoclaimActive(false);
            cache.setAutoclaimSettlement(null);
            United.messenger().send(player, "player.settlement.autoclaim.switched-off");
        }

    }

}
