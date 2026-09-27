package org.unitedlands.unitedlands.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.classes.SettlementChunk;

import org.unitedlands.libs.ormlite.dao.Dao;

public class SettlementChunkService extends BaseDbService<SettlementChunk> {

    public SettlementChunkService(Dao<SettlementChunk, UUID> dao) {
        super(dao);
    }

}
