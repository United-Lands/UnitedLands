package org.unitedlands.unitedlands.classes.events.settlement;

import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.events.base.SettlementEvent;

public class SettlementUpdatedEvent extends SettlementEvent {

    private final boolean visualChange;

    public SettlementUpdatedEvent(Settlement settlement, boolean visualChange) {
        super(settlement);
        this.visualChange = visualChange;
    }

    public boolean isVisualChange() {
        return visualChange;
    }

}
