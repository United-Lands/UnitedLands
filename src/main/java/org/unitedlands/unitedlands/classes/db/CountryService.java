package org.unitedlands.unitedlands.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.classes.Country;

import org.unitedlands.libs.ormlite.dao.Dao;

public class CountryService extends BaseDbService<Country> {

    public CountryService(Dao<Country, UUID> dao) {
        super(dao);
    }

}
