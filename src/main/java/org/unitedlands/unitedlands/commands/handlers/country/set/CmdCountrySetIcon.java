package org.unitedlands.unitedlands.commands.handlers.country.set;

import java.net.URI;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.managers.IntegrationManager;
import org.unitedlands.unitedlands.utils.IconUtils;
import org.unitedlands.unitedlands.utils.IconUtils.IconException;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdCountrySet.class,
    name = "icon",
    description = "Sets the country icon URL",
    usage = "/country set icon <url|none>",
    playerOnly = true
)
public class CmdCountrySetIcon extends CountryCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        if (!IntegrationManager.instance().getMapRenderer().isAvailable()) {
            United.messenger().send(sender, "general-errors.map-not-available");
            return;
        }

        var context = validate(sender, "country.seticon");
        if (context == null)
            return;

        if (args[0].equalsIgnoreCase("none")) {
            IntegrationManager.instance().getMapRenderer().removeCountryIcon(context.country());
            context.country().clearIconPosition();
            context.country().save();

            United.messenger().send(sender, "player.country.seticon.removed");
            return;
        }

        final URI uri;
        try {
            uri = IconUtils.parseUri(args[0]);
        } catch (IconException e) {
            United.messenger().sendRaw(sender, e.getMessage());
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(UnitedLands.instance(), () -> {
            try {
                String fileName = context.country().getUuid().toString() + ".png";
                IconUtils.downloadAndSave(uri, fileName);

                Bukkit.getScheduler().runTask(UnitedLands.instance(), () -> {
                    IntegrationManager.instance().getMapRenderer().registerIcon(context.country().getUuid().toString(), fileName);

                    context.country().setIconPosition(context.country().getSpawn());
                    context.country().saveAndRender();

                    United.messenger().send(sender, "player.country.seticon.success");
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

}
