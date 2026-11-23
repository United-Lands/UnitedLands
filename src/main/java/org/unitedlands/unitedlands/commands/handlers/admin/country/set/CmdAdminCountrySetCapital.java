package org.unitedlands.unitedlands.commands.handlers.admin.country.set;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.commandhandlers.AdminCommandHandler;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdAdminCountrySet.class,
        name = "capital",
        description = "Sets a country capital",
        usage = "/ula country set capital <country> <settlement_name>",
        catchAll = true
)
public class CmdAdminCountrySetCapital extends AdminCommandHandler {

    @Override
    public List<String> handleTab(CommandSender arg0, String[] args) {
        switch (args.length) {
            case 1:
                return UnitedLandsDataManager.instance().getCountryNames();
            case 2:
                var country = UnitedLandsDataManager.instance().getCountry(args[0]);
                if (country != null)
                    return country.getSettlements().stream().map(Settlement::getName).collect(Collectors.toList());
        }
        return null;
    }

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

        if (!settlement.hasCountry() || !country.equals(settlement.getCountry())) {
            United.messenger().send(sender,
                    "admin.country.setcapital.settlement-not-in-country", settlement.getCleanName(), country.getCleanName());
            return;
        }

        country.setCapital(settlement);
        country.saveAndRender();

        United.messenger().send(sender, "admin.country.setcapital.success", country.getName(),  settlement.getName());
    }

}
