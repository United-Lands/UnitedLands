package org.unitedlands.unitedlands.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.integrations.floodgate.FloodgateAPIIntegration;
import org.unitedlands.unitedlands.managers.ConfirmationManager;
import org.unitedlands.unitedlands.managers.IntegrationManager;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;


public class Confirmation {

    private Runnable runnable;
    private String title;
    private Map<String, String> replacements;
    private String key;
    private Player sender;
    private Player receiver;

    public Confirmation(String key) {
        this.key = key;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public Confirmation setRunnable(Runnable runnable) {
        this.runnable = runnable;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public String getFilledTitle() {
        if (title != null && replacements != null) {
            var filled = title;
            for (var entry : replacements.entrySet()) {
                filled = filled.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return filled;
        }
        return title;
    }

    public Confirmation setTitle(String title) {
        this.title = title;
        return this;
    }

    public Map<String, String> getReplacements() {
        return replacements;
    }

    public Confirmation setReplacements(Map<String, String> replacements) {
        this.replacements = replacements;
        return this;
    }

    public String getKey() {
        return key;
    }

    public Player getSender() {
        return sender;
    }

    public Confirmation setSender(Player sender) {
        this.sender = sender;
        return this;
    }

    public Player getReceiver() {
        return receiver;
    }

    public Confirmation setReceiver(Player receiver) {
        this.receiver = receiver;
        return this;
    }

    public void send() {
        if (sender == null || receiver == null)
            return;

        // United.messenger().send(receiver, "<aqua>" + getTitle() + " [<yellow>" +
        // getAcceptCommand() + "</yellow>]</aqua>", replacements);

        ConfirmationManager.instance().queueConfirmation(this);

        // Show different UIs to Java and Bedrock players if floodgate is present
        if (!IntegrationManager.instance().useFloodgate()) {
            sendJavaDialog(receiver);
        } else {
            var floodgate = new FloodgateAPIIntegration();
            if (!floodgate.isBedrockPlayer(receiver))
                sendJavaDialog(receiver);
            else {
                floodgate.sendConfirmationPanel(receiver, this);
            }
        }

    }

    public void sendJavaDialog(Player receiver) {

        List<DialogBody> dialogBody = new ArrayList<>();
        var miniMessage = MiniMessage.miniMessage();

        dialogBody.add(DialogBody.plainMessage(miniMessage.deserialize(getFilledTitle())));

        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Confirmation")).body(dialogBody).build())
                .type(DialogType.confirmation(ActionButton.builder(Component.text("Approve")).action(DialogAction.customClick((view, audience) -> {
                    ConfirmationManager.instance().executeConfirmation(this);
                }, ClickCallback.Options.builder().build())).build(),
                        ActionButton.builder(Component.text("Cancel")).action(DialogAction.customClick((view, audience) -> {
                            ConfirmationManager.instance().rejectConfirmation(this);
                        }, ClickCallback.Options.builder().build())).build())));

        receiver.showDialog(dialog);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((sender == null) ? 0 : sender.hashCode());
        result = prime * result + ((receiver == null) ? 0 : receiver.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Confirmation other = (Confirmation) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (sender == null) {
            if (other.sender != null)
                return false;
        } else if (!sender.equals(other.sender))
            return false;
        if (receiver == null) {
            if (other.receiver != null)
                return false;
        } else if (!receiver.equals(other.receiver))
            return false;
        return true;
    }

}
