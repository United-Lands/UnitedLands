package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlement.class,
        name = "invite",
        description = "Invites a player to the settlement",
        usage = "/settlement invite <player>",
        playerOnly = true
)
public class CmdSettlementInvite extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.invite");
        if (context == null)
            return;

        var targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            United.messenger().send(context.player(), "general-errors.player-not-found", args[0]);
            return;
        }

        var targetCitizen = getCitizen(targetPlayer);
        if (targetCitizen == null)
            return;

        if (targetCitizen.hasSettlement()) {
            United.messenger().send(context.player(), "player.settlement.invite.already-in-settlement");
            return;
        }

        Confirmation invite = new Confirmation("settlement-invite");
        invite.setRunnable(() -> {

            context.settlement().addCitizen(targetCitizen);
            context.settlement().saveAndRender();

            targetCitizen.setSettlement(context.settlement());
            targetCitizen.save();

            United.messenger().send(context.settlement().getOnlinePlayers(), "player.settlement.invite.player-joined", targetPlayer.getName());
            United.messenger().send(targetPlayer, "player.settlement.invite.player-message", context.settlement().getCleanName());
        })
                .setTitle(United.messenger().get("player.settlement.invite.invite-message", context.settlement().getCleanName()))
                .setSender(context.player())
                .setReceiver(targetPlayer)
                .send();
    }

}
