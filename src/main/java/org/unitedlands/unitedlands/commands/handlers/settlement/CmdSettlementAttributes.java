package org.unitedlands.unitedlands.commands.handlers.settlement;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.commandhandlers.SettlementCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.FontUtils;
import org.unitedlands.unitedlands.utils.FontUtils.Align;
import org.unitedlands.utils.United;

import net.kyori.adventure.text.minimessage.MiniMessage;

@UnitedSubCommand(
        parent = CmdSettlement.class,
        name = "attributes",
        description = "Shows attributes of a settlement",
        usage = "/settlement attributes [settlement_name]",
        playerOnly = true
)
public class CmdSettlementAttributes extends SettlementCommandHandler {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getSettlementNames();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;
        var citizen = getCitizen(player);
        if (citizen == null)
            return;

        Settlement settlement = null;
        if (args.length == 0) {
            settlement = getCitizenSettlement(citizen);
            if (settlement == null)
                return;
        } else if (args.length >= 1) {
            settlement = UnitedLandsDataManager.instance().getSettlement(args[0]);
            if (settlement == null) {
                return;
            }
        }

        var firstColWith = 150;
        var colWith = 40;

        var head1 = FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gray>Attribute</gray>"), firstColWith, Align.LEFT);
        var head2 = FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gray>Current</gray>"), colWith, Align.LEFT);
        var head3 = FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gray>Min</gray>"), colWith, Align.LEFT);
        var head4 = FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gray>Max</gray>"), colWith, Align.LEFT);
        var head5 = FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gray>Daily</gray>"), colWith, Align.LEFT);

        var header = head1.append(head2).append(head3).append(head4).append(head5);
        United.messenger().send(sender, header);


        var keys = settlement.getAttributeKeys();
        for (var key : keys) {

            var attribute = settlement.getModifiedAttribute(key);

            var paddedComponent = FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gold><b>" + key + "</b>:</gold>"), firstColWith, Align.LEFT)
                .append(FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gray>" + String.format("%.2f", attribute.getCurrentValue()) + "</gray>"), colWith, Align.LEFT))
                .append(FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gray>" + String.format("%.2f", attribute.getMinValue()) + "</gray>"), colWith, Align.LEFT))
                .append(FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gray>" + String.format("%.2f", attribute.getMaxValue()) + "</gray>"), colWith, Align.LEFT))
                .append(FontUtils.padTo(MiniMessage.miniMessage().deserialize("<gray>" + String.format("%.2f", attribute.getDailyChange()) + "</gray>"), colWith, Align.LEFT));


            United.messenger().send(sender, paddedComponent);
        }

    }
}
