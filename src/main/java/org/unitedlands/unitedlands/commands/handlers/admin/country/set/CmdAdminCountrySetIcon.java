package org.unitedlands.unitedlands.commands.handlers.admin.country.set;

import java.net.URI;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;
import org.unitedlands.unitedlands.managers.IntegrationManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.IconUtils;
import org.unitedlands.unitedlands.utils.IconUtils.IconException;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountrySet.class,
        name = "icon",
        description = "Sets a country icon URL",
        usage = "/ula country set icon <country> <url|none>",
        catchAll = true
)
public class CmdAdminCountrySetIcon extends AdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        if (!IntegrationManager.instance().getMapRenderer().isAvailable()) {
            United.messenger().send(sender, "general-errors.map-not-available");
            return;
        }

        var country = getCountry(sender, args[0]);
        if (country == null) {
            return;
        }

        if (args[1].equalsIgnoreCase("none")) {
            IntegrationManager.instance().getMapRenderer().removeCountryIcon(country);
            country.clearIconPosition();
            country.save();

            United.messenger().send(sender, "admin.country.seticon.removed", country.getCleanName());
            return;
        }

        final URI uri;
        try {
            uri = IconUtils.parseUri(args[1]);
        } catch (IconException e) {
            United.messenger().sendRaw(sender, e.getMessage());
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(UnitedLands.instance(), () -> {
            try {
                String fileName = country.getUuid().toString() + ".png";
                IconUtils.downloadAndSave(uri, fileName);

                Bukkit.getScheduler().runTask(UnitedLands.instance(), () -> {
                    IntegrationManager.instance().getMapRenderer().registerIcon(country.getUuid().toString(), fileName);

                    country.setIconPosition(country.getSpawn());
                    country.saveAndRender();

                    United.messenger().send(sender, "admin.country.seticon.success", country.getCleanName());
                });
            } catch (IconException e) {
                United.messenger().sendRaw(sender, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                United.logger().warning("Icon download failed for " + uri + ": " + e);
                e.printStackTrace();
                United.messenger().sendRaw(sender, United.messenger().get("player.country.seticon.download-error"));
            }
        });
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return UnitedLandsDataManager.instance().getCountryNames();
            case 2:
                return List.of("none");
        }
        return null;
    }

}
