package org.unitedlands.unitedlands.classes.events.settlement;

import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.events.base.SettlementPlayerEvent;

public class SettlmentExhaustionPreventedEvent extends SettlementPlayerEvent {

    public SettlmentExhaustionPreventedEvent(Settlement settlement, Player player) {
        super(settlement, player);
    }

}
