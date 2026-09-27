package org.unitedlands.unitedlands.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.classes.Settlement;

import org.unitedlands.libs.ormlite.dao.Dao;

public class SettlementService extends BaseDbService<Settlement> {

    public SettlementService(Dao<Settlement, UUID> dao) {
        super(dao);
    }

}
