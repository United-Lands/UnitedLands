package org.unitedlands.unitedlands.commands.handlers.country;

import java.math.BigDecimal;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountry.class,
        name = "create",
        description = "Creates a new country",
        usage = "/country create <country_name>",
        playerOnly = true
)
public class CmdCountryCreate implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        var player = (Player) sender;
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null || citizen.getSettlement() == null) {
            United.messenger().send(player, "general-errors.not-in-settlement");
            return;
        }

        if (ColorUtils.containsColourCharacters(args[0])) {
            United.messenger().send(player, "general-errors.illegal-characters", args[0]);
            return;
        }

        var settlement = citizen.getSettlement();
        var region = settlement.getRegion();

        if (region == null) {
            United.messenger().send(player, "general-errors.region-not-found");
            return;
        }

        if (region.getCountry() != null) {
            United.messenger().send(player, "player.country.create.region-occupied", region.getCountry().getCleanName());
            return;
        }

        if (!UnitedLandsEconomyManager.instance().has(citizen.getUuid(), new BigDecimal(UnitedLandsConfig.get().economy().costs().country().createCosts()))) {
            United.messenger().send(player, "general-errors.no-funds-player", 
            UnitedLandsEconomyManager.instance().format(UnitedLandsConfig.get().economy().costs().country().createCosts()));
            return;
        }

        var confirmation = new Confirmation("country");
        confirmation.setRunnable(() -> {

            var country = UnitedLandsDataManager.instance().createCountry(args[0], region, settlement, citizen);
            
            United.messenger().send(player, "player.country.create.player-message", country.getCleanName());
            United.messenger().send(Bukkit.getServer(), "player.country.create.broadcast-message", player.getName(), country.getCleanName(),
                    region.getCleanName());
        })
                .setTitle(
                    United.messenger().get("player.country.create.confirm", 
                    args[0].replace("_", " "), 
                    settlement.getCleanName(),
                    UnitedLandsEconomyManager.instance().format(UnitedLandsConfig.get().economy().costs().country().createCosts())))
                .setSender(player)
                .setReceiver(player)
                .send();
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
