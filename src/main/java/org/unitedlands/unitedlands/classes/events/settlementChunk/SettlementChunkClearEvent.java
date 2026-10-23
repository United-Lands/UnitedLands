package org.unitedlands.unitedlands.classes.events.settlementChunk;

import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.events.base.SettlementChunkEvent;

public class SettlementChunkClearEvent extends SettlementChunkEvent {

    public SettlementChunkClearEvent(Settlement settlement, SettlementChunk settlementChunk) {
        super(settlement, settlementChunk);
    }

}
