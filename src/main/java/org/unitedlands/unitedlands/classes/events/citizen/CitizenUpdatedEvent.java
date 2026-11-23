package org.unitedlands.unitedlands.classes.events.citizen;

import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.events.base.CitizenEvent;

public class CitizenUpdatedEvent extends CitizenEvent {

    public CitizenUpdatedEvent(Citizen citizen) {
        super(citizen);
    }

}
