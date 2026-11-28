package org.unitedlands.unitedlands.commands.handlers.country.set;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.utils.TagUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountrySet.class,
        name = "tag",
        description = "Sets the country tag",
        usage = "/country set tag <tag|none>",
        playerOnly = true
)
public class CmdCountrySetTag extends CountryCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "country.settag");
        if (context == null)
            return;

        var country = context.country();

        if (args[0].equalsIgnoreCase("none")) {
            country.setTag(null);
            country.save();
            United.messenger().send(context.player(), "player.country.settag.cleared");
            return;
        }

        if (TagUtils.containsIllegalPlayerCharacters(args[0])) {
            United.messenger().send(sender, "general-errors.illegal-characters", TagUtils.escape(args[0]));
            return;
        }

        var tag = TagUtils.format(args[0]);
        var length = TagUtils.visibleLength(tag);
        if (length == 0) {
            United.messenger().send(sender, "general-errors.tag-empty");
            return;
        }

        var maxLength = UnitedLandsConfig.get().general().maxTagLength();
        if (length > maxLength) {
            United.messenger().send(sender, "general-errors.tag-too-long", String.valueOf(maxLength));
            return;
        }

        country.setTag(tag);
        country.save();

        United.messenger().send(context.player(), "player.country.settag.success", tag);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return List.of("none");
        return null;
    }

}
