package org.unitedlands.unitedlands.commands.handlers.country;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountry.class,
        name = "delete",
        description = "Deletes a country",
        usage = "/country delete",
        playerOnly = true
)
public class CmdCountryDelete extends CountryCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "country.delete");
        if (context == null)
            return;

        Confirmation leave = new Confirmation("country-delete");
        leave.setRunnable(() -> {

            UnitedLandsDataManager.instance().removeCountry(context.country());

            United.messenger().send(Bukkit.getServer(), "player.country.delete.broadcast-message", context.country().getCleanName());

        })
                .setTitle(United.messenger().get("player.country.delete.delete-confirm", context.country().getCleanName()))
                .setSender(context.player())
                .setReceiver(context.player())
                .send();
    }

}
