package org.unitedlands.unitedlands.commands.handlers.web;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

@UnitedSubCommand(
        parent = CmdWeb.class,
        name = "login",
        description = "Confirms an external web UI login",
        usage = "/ulweb login <code>",
        playerOnly = true
)
public class CmdWebLogin implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        var code = args[0];

        var loginChallenge = UnitedLandsDataManager.instance().getDatabaseManager().getLoginChallengeService()
                .getByCode(code);
        if (loginChallenge == null) {
            United.messenger().send(sender, "web.login.invalid-code");
            return;
        }

        if (System.currentTimeMillis() > loginChallenge.getExpiresAt()) {
            United.messenger().send(sender, "web.login.code-expired");
            return;
        }

        Player player = (Player) sender;

        loginChallenge.setMcUUID(player.getUniqueId());
        loginChallenge.setMcUsername(player.getName());
        loginChallenge.setStatus("completed");

        UnitedLandsDataManager.instance().getDatabaseManager().getLoginChallengeService()
                .updateAsync(loginChallenge);
    }

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

}
