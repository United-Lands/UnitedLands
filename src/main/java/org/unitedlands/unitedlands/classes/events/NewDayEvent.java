package org.unitedlands.unitedlands.classes.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NewDayEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final long dayStart;

    public NewDayEvent(long dayStart) {
        this.dayStart = dayStart;
    }

    public long getDayStart() {
        return dayStart;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
