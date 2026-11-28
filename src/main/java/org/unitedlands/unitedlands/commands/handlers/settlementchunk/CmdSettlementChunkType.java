package org.unitedlands.unitedlands.commands.handlers.settlementchunk;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.BankRecordTransactionType;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementChunkCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig.SettlementChunkSettings;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementChunk.class,
        name = "type",
        description = "Sets the type of settlement chunk",
        usage = "/settlementchunk type <type>",
        playerOnly = true
)
public class CmdSettlementChunkType extends SettlementChunkCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length != 1)
            return null;

        var types = UnitedLandsConfig.get().settlementChunks().toMap();
        if (!(sender instanceof Player player))
            return types.keySet().stream().toList();

        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null)
            return types.keySet().stream().toList();

        return types.entrySet().stream()
                .filter(e -> canSetType(citizen, e.getValue()))
                .map(e -> e.getKey())
                .toList();
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.plot.settype");
        if (context == null)
            return;

        var type = args[0];
        if (!UnitedLandsConfig.get().settlementChunks().has(type)) {
            United.messenger().send(context.player(), "player.settlementchunk.settype.unknown-type", type);
            return;
        }

        if (type.equals(context.settlementChunk().getChunkType())) {
            United.messenger().send(context.player(), "player.settlementchunk.settype.already-type", type);
            return;
        }

        var typeSettings = UnitedLandsConfig.get().settlementChunks().get(type);
        if (!canSetType(context.citizen(), typeSettings)) {
            United.messenger().send(context.player(), "general-errors.no-settlement-permission",
                    typeSettings.requiredPermission());
            return;
        }

        var settlement = context.settlementChunk().getSettlement();
        if (typeSettings.maxPerSettlement() != -1) {
            var chunksOfType = settlement.getChunksOfType(type);
            if (chunksOfType.size() >= typeSettings.maxPerSettlement()) {
                United.messenger().send(context.player(), "player.settlementchunk.settype.too-many-of-type",
                        String.valueOf(typeSettings.maxPerSettlement()), type);
                return;
            }
        }

        var cost = typeSettings.setCost();
        if (cost > 0) {
            if (!UnitedLandsEconomyManager.instance().has(settlement.getUuid(), cost)) {
                United.messenger().send(context.player(), "general-errors.no-funds-settlement",
                        UnitedLandsEconomyManager.instance().format(cost));
                return;
            }
            UnitedLandsEconomyManager.instance().withdraw(settlement.getUuid(), cost,
                    BankRecordTransactionType.CLAIM,
                    "Plot type " + type + " set by " + context.player().getName());
        }

        context.settlementChunk().resetOverrides();
        context.settlementChunk().setChunkType(type);
        context.settlementChunk().save();

        if (cost > 0) {
            United.messenger().send(context.player(), "player.settlementchunk.settype.success-cost", type,
                    UnitedLandsEconomyManager.instance().format(cost));
        } else {
            United.messenger().send(context.player(), "player.settlementchunk.settype.success", type);
        }

    }

    private boolean canSetType(Citizen citizen, SettlementChunkSettings typeSettings) {
        var permission = typeSettings.requiredPermission();
        if (permission == null || permission.isBlank())
            return true;
        return PermissionManager.instance().hasRankPermission(permission, citizen);
    }

}
