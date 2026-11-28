package org.unitedlands.unitedlands.commands.handlers.admin.settlement.set;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.TagUtils;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminSettlementSet.class,
        name = "tag",
        description = "Sets a settlement's tag",
        usage = "/ula settlement set tag <settlement_name> <tag|none>",
        catchAll = true
)
public class CmdAdminSettlementSetTag extends AdminCommandHandler {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        var settlement = getSettlement(sender, args[0]);
        if (settlement == null) {
            return;
        }

        var input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (input.equalsIgnoreCase("none")) {
            settlement.setTag(null);
            settlement.save();
            United.messenger().send(sender, "admin.settlement.settag.cleared", settlement.getCleanName());
            return;
        }

        var tag = TagUtils.format(input);
        if (TagUtils.visibleLength(tag) == 0) {
            United.messenger().send(sender, "general-errors.tag-empty");
            return;
        }

        settlement.setTag(tag);
        settlement.save();

        United.messenger().send(sender, "admin.settlement.settag.success", settlement.getCleanName(), tag);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getSettlementNames();
        if (args.length == 2)
            return List.of("none");
        return null;
    }

}
