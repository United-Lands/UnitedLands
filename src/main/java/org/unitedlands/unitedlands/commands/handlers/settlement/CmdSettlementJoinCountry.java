package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;

import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlement.class,
        name = "joincountry",
        description = "Joins the region's country with a settlement",
        usage = "/settlement joincountry",
        playerOnly = true
)
public class CmdSettlementJoinCountry extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "settlement.joincountry");
        if (context == null)
            return;

        if (context.settlement().hasCountry()) {
            United.messenger().send(context.player(), "player.settlement.joincountry.already-in-country");
            return;
        }
        if (!context.settlement().hasRegion()) {
            United.messenger().send(context.player(), "player.settlement.joincountry.no-region");
            return;
        }

        var region = context.settlement().getRegion();
        if (!region.hasCountry()) {
            United.messenger().send(context.player(), "player.settlement.joincountry.no-country");
            return;
        }

        var country = region.getCountry();

        Confirmation join = new Confirmation("country-join");
        join.setRunnable(() -> {

            context.settlement().setCountry(country);
            context.settlement().saveAndRender();

            country.addSettlement(context.settlement());

            United.messenger().send(Bukkit.getServer(), "player.settlement.joincountry.broadcast-message", context.settlement().getCleanName(),
                    country.getCleanName());
        })
                .setTitle(United.messenger().get("player.settlement.joincountry.confirm", country.getCleanName()))
                .setSender(context.player())
                .setReceiver(context.player())
                .send();

    }

}
