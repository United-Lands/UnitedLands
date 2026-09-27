package org.unitedlands.unitedlands.commands.handlers.citizen;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.PlaytimeRecord;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.ActivityScoreCalculator;
import org.unitedlands.utils.United;

@UnitedSubCommand(
    parent          = CmdCitizen.class,
    name            = "playtime",
    description     = "Shows information about a citizen's playtime",
    usage           = "/citizen playtime <citizen_name>",
    playerOnly      = true
)
public class CmdCitizenPlaytime implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getCitizenNames();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        Citizen citizen = null;
        if (args.length == 0) {
            citizen = UnitedLandsDataManager.instance().getCitizen((Player) sender);
            if (citizen == null)
                return;
        } else if (args.length >= 1) {
            citizen = UnitedLandsDataManager.instance().getCitizen(args[0]);
            if (citizen == null) {
                return;
            }
        }

        var sessionPlaytime =  System.currentTimeMillis() - citizen.getLastLogon();

        var playtimeRecords = UnitedLandsDataManager.instance().getRecentPlaytimeRecords(citizen.getUuid(), 60);
        // Add a virtual playtime record for the current session
        playtimeRecords.add(
            new PlaytimeRecord(citizen.getLastLogon(), System.currentTimeMillis(), sessionPlaytime)
        );

        var activityScore = ActivityScoreCalculator.calculateActivityScore(playtimeRecords);

        United.messenger().sendRaw(sender, "<white><b>" + citizen.getName() + "'s Playtime</b>: " +
                                United.formatter().formatDuration(citizen.getTotalPlaytime() + sessionPlaytime) + 
                                " (Activity Score: " + String.format("%.2f", activityScore) + ")"
        );

    }
}
