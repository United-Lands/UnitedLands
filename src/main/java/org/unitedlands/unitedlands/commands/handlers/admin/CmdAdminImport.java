package org.unitedlands.unitedlands.commands.handlers.admin;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.unitedlands.utils.PolygonUtils;
import org.unitedlands.unitedlands.utils.SvgUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent      = CmdAdmin.class,
        name        = "import",
        description = "Admin regions import",
        usage       = "/ula import <world> <filename>",
        catchAll    = true
)
public class CmdAdminImport implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender arg0, String[] arg1) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var worldName = "world_earth";

        String file = "earth.png";

        if (args.length >= 1) {
            worldName = args[0];
        }

        if (args.length >= 2) {
            file = args[1];
        }

        File importFolder = new File(UnitedLands.instance().getDataFolder(), "import");
        File imageFile = new File(importFolder, file + ".svg");
        if (!imageFile.exists()) {
            United.logger().error("Image not found: " + file, "UnitedLands");
            return;
        }

        try {
            var parsed = SvgUtils.loadFile(imageFile);

            World world = Bukkit.getWorld(worldName);
            var counter = 1;

            for (var entry : parsed.entrySet()) {

                boolean create = false;

                var region = UnitedLandsDataManager.instance().getRegion(entry.getKey());
                if (region == null) {
                    United.logger().info("Region " + entry.getKey() + " not found, creating...");
                    create = true;
                    region = new Region();
                    region.setUuid(UUID.randomUUID());
                    region.setName(entry.getKey());
                    region.setDefaultName(entry.getKey());
                } else {
                    United.logger().info("Region " + entry.getKey() + " found, updating default name and polygon...");
                    region.setDefaultName(entry.getKey());
                }

                var val = entry.getValue();
                region.setPolygon(val.vertices);
                region.setArea(PolygonUtils.area(val.vertices));

                var center = PolygonUtils.calculatePolygonCenter(val.vertices);
                region.setHomeChunkCoordinates(new Coordinates((int) center[0] >> 4, (int) center[1] >> 4, worldName));

                if (val.color != null) {
                    region.setDebugStrokeColor(ColorUtils.hexToColor(val.color).getRGB());
                    region.setDebugFillColor(ColorUtils.hexToColor(val.color + "10").getRGB());
                }
                region.setWorld(world);

                if (create) {
                    UnitedLandsDataManager.instance().createRegionDbData(region);
                    United.logger().info("Registered new region " + region.getName() + " (" + counter + "/" +
                            parsed.size() + ")");
                } else {
                    region.saveAndRender();
                    United.logger().info("Updated region " + region.getName() + " (" + counter + "/" +
                            parsed.size() + ")");
                }

                counter++;
            }

            UnitedLandsDataManager.instance().buildRegionIndex();

        } catch (Exception ex) {
            United.logger().warning("Error parsing " + file + ": " + ex.getMessage(), "UnitedLands");
            ex.printStackTrace();
            return;
        }

    }

}
