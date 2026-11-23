package org.unitedlands.unitedlands.commands.handlers.admin.country.set;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountrySet.class,
        name = "color",
        description = "Sets a country color",
        usage = "/ula country set color <country> <#hexcolor>",
        catchAll = true
)
public class CmdAdminCountrySetColor extends AdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var country = getCountry(sender, args[0]);
        if (country == null) {
            return;
        }

        if (!(args[1].length() == 7) || !ColorUtils.isValidHexColor(args[1])) {
            United.messenger().send(sender, "general-errors.wrong-color-format");
            return;
        }

        country.setFillColor(args[1] + "10");
        country.setStrokeColor(args[1]);
        country.saveAndRender();

        United.messenger().send(sender, "admin.country.setcolor.success", country.getCleanName(), args[1]);
    }

    @Override
    public List<String> handleTab(CommandSender arg0, String[] args) {
        if (args.length == 1) {
            return UnitedLandsDataManager.instance().getCountryNames();
        }
        return null;
    }

}
