package org.unitedlands.unitedlands.commands.handlers.country;

import java.text.SimpleDateFormat;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.unitedlands.classes.commandhandlers.CountryCommandHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdCountry.class,
        name = "bankhistory",
        description = "Shows the citizen's bank history",
        usage = "/citizen bankhistory [page]",
        playerOnly = true
)
public class CmdCountryBankHistory extends CountryCommandHandler {

    private final int PAGE_SIZE = 5;

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var context = validate(sender, "country.bankhistory");
        if (context == null)
            return;

        var startIndex = 0;
        if (args.length == 1) {
            try {
                startIndex = (Integer.parseInt(args[0]) - 1);
            } catch (Exception ex) {
                United.messenger().send(context.player(), "general-errors.wrong-number-format",
                        Map.of("input", args[0]));
                return;
            }
        }

        var records = UnitedLandsEconomyManager.instance().getBankRecords(context.country().getUuid(), startIndex, PAGE_SIZE);
        United.messenger().sendRaw(sender, "<bold>" + context.country().getName() + " Bank History:</bold>");
        for (var record : records) {

            var timeStamp = "<gray>[" + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(record.getTimestamp()) + "]</gray>";
            var amount = record.getAmount() > 0 ?          
                         "<green>+"  + UnitedLandsEconomyManager.instance().format(record.getAmount()) + "<green>" :
                         "<red>"  + UnitedLandsEconomyManager.instance().format(record.getAmount()) + "<red>";

            United.messenger().sendRaw(sender, 
                timeStamp + " " + amount + " <gray>(" + record.getDetails() + ")</gray>");
        }
    }

}
