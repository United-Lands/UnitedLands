package org.unitedlands.unitedlands.commands.handlers.settlement.set;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.utils.TagUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdSettlementSet.class,
        name = "tag",
        description = "Sets the settlement tag",
        usage = "/settlement set tag <tag|none>",
        playerOnly = true
)
public class CmdSettlementSetTag extends SettlementCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var context = validate(sender, "settlement.settag");
        if (context == null)
            return;

        var settlement = context.settlement();

        if (args[0].equalsIgnoreCase("none")) {
            settlement.setTag(null);
            settlement.save();
            United.messenger().send(context.player(), "player.settlement.settag.cleared");
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

        settlement.setTag(tag);
        settlement.save();

        United.messenger().send(context.player(), "player.settlement.settag.success", tag);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return List.of("none");
        return null;
    }

}
