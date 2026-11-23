package org.unitedlands.unitedlands.classes.events.settlementchunk;

import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.events.base.SettlementChunkEvent;

public class SettlementChunkPrePurchaseEvent extends SettlementChunkEvent{

    private Citizen buyer;

    public SettlementChunkPrePurchaseEvent(Settlement settlement, SettlementChunk settlementChunk) {
        super(settlement, settlementChunk);
    }

    public SettlementChunkPrePurchaseEvent(Settlement settlement, SettlementChunk settlementChunk, Citizen buyer) {
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
