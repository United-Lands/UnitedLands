package org.unitedlands.unitedlands.classes.events.settlement;

import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.events.base.SettlementEvent;

public class SettlementRemovedEvent extends SettlementEvent {

    public SettlementRemovedEvent(Settlement settlement) {
        super(settlement);
    }

}
