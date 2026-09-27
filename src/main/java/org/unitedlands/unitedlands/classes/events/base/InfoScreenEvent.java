package org.unitedlands.unitedlands.classes.events.base;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.unitedlands.unitedlands.classes.infoscreen.InfoScreen;

public abstract class InfoScreenEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private final InfoScreen infoScreen;

    public InfoScreenEvent(InfoScreen infoScreen) {
        this.infoScreen = infoScreen;
    }

    public InfoScreen getInfoScreen() {
        return infoScreen;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
