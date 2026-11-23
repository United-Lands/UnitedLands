package org.unitedlands.unitedlands.classes.db;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.unitedlands.unitedlands.classes.BankRecord;
import org.unitedlands.unitedlands.classes.BankRecordTransactionType;
import org.unitedlands.classes.database.BaseDbService;
import org.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.libs.ormlite.stmt.QueryBuilder;

public class BankRecordService extends BaseDbService<BankRecord> {

    public BankRecordService(Dao<BankRecord, UUID> dao) {
        super(dao);
    }

    public CompletableFuture<List<BankRecord>> getRecordsAsync(UUID id, long page, int pageSize) {

        return readAsync("Query of bank records for " + id, () -> {
            QueryBuilder<BankRecord, UUID> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("subject", id);
            queryBuilder.orderBy("timestamp", false);
            queryBuilder.orderBy("amount", true);
            queryBuilder.offset(page * pageSize);
            queryBuilder.limit((long) pageSize);
            return dao.query(queryBuilder.prepare());
        }, List.of());
    }

    public CompletableFuture<List<BankRecord>> getRecordsByTypeAsync(BankRecordTransactionType type, long from, long to) {

        return readAsync("Query of bank records of type " + type, () -> {
            QueryBuilder<BankRecord, UUID> queryBuilder = dao.queryBuilder();
            queryBuilder.where()
            .eq("transaction_type", type)
            .and()
            .ge("timestamp", from)
            .and()
            .lt("timestamp", to);
            return dao.query(queryBuilder.prepare());
        }, List.of());
    }


}
