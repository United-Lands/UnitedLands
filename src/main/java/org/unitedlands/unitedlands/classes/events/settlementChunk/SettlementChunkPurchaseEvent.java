package org.unitedlands.unitedlands.classes.events.settlementchunk;

import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.events.base.SettlementChunkEvent;

public class SettlementChunkPurchaseEvent extends SettlementChunkEvent {

    private Citizen buyer;

    public SettlementChunkPurchaseEvent(Settlement settlement, SettlementChunk settlementChunk) {
        super(settlement, settlementChunk);
    }

    public SettlementChunkPurchaseEvent(Settlement settlement, SettlementChunk settlementChunk, Citizen buyer) {
        super(settlement, settlementChunk);
        this.buyer = buyer;
    }

    public Citizen getBuyer() {
        return buyer;
    }

    public void setBuyer(Citizen buyer) {
        this.buyer = buyer;
    }
}
