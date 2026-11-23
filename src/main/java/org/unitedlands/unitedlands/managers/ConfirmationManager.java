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

        if (!confirmation.getSender().equals(confirmation.getReceiver()))
            United.messenger().send(confirmation.getSender(), "confirmation.request-sent");

    }

    public void executeConfirmation(Confirmation confirmation) {
        confirmation.getRunnable().run();
        confirmations.remove(confirmation);
    }

    public void rejectConfirmation(Confirmation confirmation) {
        if (confirmation == null)
            return;

        if (!confirmation.getSender().equals(confirmation.getReceiver()))
            United.messenger().send(confirmation.getSender(), "confirmation.request-rejected");

        confirmations.remove(confirmation);
    }

}
