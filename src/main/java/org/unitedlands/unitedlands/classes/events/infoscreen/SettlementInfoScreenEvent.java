package org.unitedlands.unitedlands.classes.events.infoscreen;

import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.events.base.InfoScreenEvent;
import org.unitedlands.unitedlands.classes.infoscreen.InfoScreen;

public class SettlementInfoScreenEvent extends InfoScreenEvent {

    private final Settlement settlement;

    public SettlementInfoScreenEvent(InfoScreen infoScreen, Settlement country) {
        super(infoScreen);
        this.settlement = country;
    }

    public Settlement getSettlement() {
        return settlement;
    }

}
