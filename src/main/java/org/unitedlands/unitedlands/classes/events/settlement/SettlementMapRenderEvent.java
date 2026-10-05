package org.unitedlands.unitedlands.classes.events.settlement;

import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.events.base.SettlementEvent;

public class SettlementMapRenderEvent extends SettlementEvent {

    public SettlementMapRenderEvent(Settlement settlement) {
        super(settlement);
    }

}
