package org.unitedlands.unitedlands.commands.handlers.admin.country.set;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.TagUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountrySet.class,
        name = "tag",
        description = "Sets a country tag",
        usage = "/ula country set tag <country> <tag|none>",
        catchAll = true
)
public class CmdAdminCountrySetTag extends AdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        var country = getCountry(sender, args[0]);
        if (country == null) {
            return;
        }

        var input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (input.equalsIgnoreCase("none")) {
            country.setTag(null);
            country.save();
            United.messenger().send(sender, "admin.country.settag.cleared", country.getCleanName());
            return;
        }

        var tag = TagUtils.format(input);
        if (TagUtils.visibleLength(tag) == 0) {
            United.messenger().send(sender, "general-errors.tag-empty");
            return;
        }

        country.setTag(tag);
        country.save();

        United.messenger().send(sender, "admin.country.settag.success", country.getCleanName(), tag);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getCountryNames();
        if (args.length == 2)
            return List.of("none");
        return null;
    }

}
