package org.unitedlands.unitedlands.classes.db;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.unitedlands.unitedlands.classes.BankRecord;
import org.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.libs.ormlite.stmt.QueryBuilder;

public class BankRecordService extends BaseDbService<BankRecord> {

    public BankRecordService(Dao<BankRecord, UUID> dao) {
        super(dao);
    }

    public CompletableFuture<List<BankRecord>> getRecordsAsync(UUID id, long page, int pageSize) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                QueryBuilder<BankRecord, UUID> queryBuilder = dao.queryBuilder();
                queryBuilder.where().eq("subject", id);
                queryBuilder.orderBy("timestamp", false);
                queryBuilder.offset(page * pageSize);
                queryBuilder.limit((long) pageSize);
                return dao.query(queryBuilder.prepare());
            } catch (SQLException e) {
                e.printStackTrace();
                return List.of();
            }
        });
    }

}
