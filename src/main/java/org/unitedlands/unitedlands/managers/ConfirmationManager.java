package org.unitedlands.unitedlands.managers;

import java.util.HashSet;
import java.util.Set;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.utils.United;

public class ConfirmationManager {

    private static ConfirmationManager instance;

    public static ConfirmationManager instance() {
        return instance;
    }

    private Set<Confirmation> confirmations = new HashSet<>();

    public ConfirmationManager() {
        instance = this;
    }

    public void queueConfirmation(Confirmation confirmation) {

        var key = confirmation.getKey();
        if (key == null || key.isBlank() || key.isEmpty()) {
            United.logger().error("Cannot process confirmation without key.", "UnitedLands");
            return;
        }

        confirmations.add(confirmation);

        // TODO: move strings to config
        if (!confirmation.getSender().equals(confirmation.getReceiver()))
            United.messenger().sendRaw(confirmation.getSender(), "Your request has been sent.");

    }

    public void executeConfirmation(Confirmation confirmation) {
        confirmation.getRunnable().run();
        confirmations.remove(confirmation);
    }

    public void rejectConfirmation(Confirmation confirmation) {
        if (confirmation == null)
            return;

        // TODO: Move string to config
        if (!confirmation.getSender().equals(confirmation.getReceiver()))
            United.messenger().sendRaw(confirmation.getSender(), "<red>Your request was rejected.</red>");

        confirmations.remove(confirmation);
    }

}
