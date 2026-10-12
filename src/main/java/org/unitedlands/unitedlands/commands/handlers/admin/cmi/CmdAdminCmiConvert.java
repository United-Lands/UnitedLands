package org.unitedlands.unitedlands.commands.handlers.admin.cmi;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent = CmdAdminCmi.class, 
    name = "convert", 
    usage = "/ula cmi convert"
)
public class CmdAdminCmiConvert implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var path = File.separator + "import" + File.separator + "cmi-bals.csv";
        var file = new File(UnitedLands.instance().getDataFolder(), path);

        if (!file.exists()) {
            United.logger().error("Import file not found, must be /import/cmi-bals.csv");
            return;
        }
        try {
            var lines = Files.readAllLines(file.toPath());

            var counter = 1;
            for (var line : lines) {

                var split = line.split(";");
                if (split.length != 3) {
                    United.logger().warning("Wrong number if columns in line " + counter);
                    continue;
                }

                var uuid = UUID.fromString(split[0]);
                var username = split[1];
                var bal = 0d;
                try {
                    bal = Double.parseDouble(split[2].replace(",", "."));
                } catch (Exception ex) {
                    United.logger().warning("Wrong number format for balance in line " + counter);
                    continue;
                }

                if (bal == 0d)
                    continue;

                UnitedLandsEconomyManager.instance().createAccount(uuid, username);
                UnitedLandsEconomyManager.instance().deposit(uuid, bal, "Import");

                counter++;
            }

        } catch (Exception ex) {
            United.logger().error(ex.getMessage());
            return;
        }
    }

}
