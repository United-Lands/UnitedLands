package org.unitedlands.unitedlands.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.classes.Citizen;

import org.unitedlands.libs.ormlite.dao.Dao;

public class CitizenService extends BaseDbService<Citizen> {

    public CitizenService(Dao<Citizen, UUID> dao) {
        super(dao);
    }

}
