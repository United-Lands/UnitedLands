package org.unitedlands.unitedlands.classes.events.settlementchunk;

import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.events.base.SettlementChunkEvent;

public class SettlementChunkCreatedEvent extends SettlementChunkEvent {

    public SettlementChunkCreatedEvent(Settlement settlement, SettlementChunk settlementChunk) {
        super(settlement, settlementChunk);
    }

}
