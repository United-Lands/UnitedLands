package org.unitedlands.unitedlands.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.classes.Region;

import org.unitedlands.libs.ormlite.dao.Dao;

public class RegionService extends BaseDbService<Region> {

    public RegionService(Dao<Region, UUID> dao) {
        super(dao);
    }

}
