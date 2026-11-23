package org.unitedlands.unitedlands.classes.events.settlementchunk;

import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.events.base.SettlementChunkEvent;

public class SettlementChunkUpdatedEvent extends SettlementChunkEvent {

    public SettlementChunkUpdatedEvent(Settlement settlement, SettlementChunk settlementChunk) {
        super(settlement, settlementChunk);
    }

}
