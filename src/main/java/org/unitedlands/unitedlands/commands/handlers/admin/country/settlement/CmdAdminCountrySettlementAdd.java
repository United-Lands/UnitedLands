package org.unitedlands.unitedlands.commands.handlers.admin.country.settlement;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountrySettlement.class,
        name = "add",
        description = "Adds a settlement to a player",
        usage = "/ula country settlement add <settlement_name>",
        catchAll = true
)
public class CmdAdminCountrySettlementAdd extends AdminCommandHandler {

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

        var settlement = getSettlement(sender, args[1]);
        if (settlement == null) {
            return;
        }

        if (settlement.hasCountry()) {
            United.messenger().send(sender, "admin.country.addsettlement.settlement-has-country", settlement.getName());
            return;
        }

        if (!settlement.hasRegion() || !settlement.getRegion().getCountry().equals(country)) {
            United.messenger().send(sender, "admin.country.addsettlement.not-country-region", settlement.getName());
            return;
        }

        settlement.setCountry(country);
        settlement.saveAndRender();

        country.addSettlement(settlement);

        United.messenger().send(sender, "admin.country.addsettlement.success", country.getCleanName(), settlement.getCleanName());
    }

    @Override
    public List<String> handleTab(CommandSender arg0, String[] args) {
        switch (args.length) {
            case 1:
                return UnitedLandsDataManager.instance().getCountryNames();
            case 2:
                return UnitedLandsDataManager.instance().getSettlementNames();
        }
        return null;
    }

}
