package org.unitedlands.unitedlands.classes.db;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.unitedlands.classes.ULCINCRecord;

public class ULCINCService extends BaseDbService<ULCINCRecord> {

    public ULCINCService(Dao<ULCINCRecord, UUID> dao) {
        super(dao);
    }

    public CompletableFuture<List<ULCINCRecord>> getForLastDaysAsync(int days) {

        var cutoff = LocalDate.now().minusDays(days).atStartOfDay(ZoneId.systemDefault())
                                           .toInstant()
                                           .toEpochMilli();

        return readAsync("Query of ULCINC records", () -> dao.query(
                dao.queryBuilder().where()
                    .ge("timestamp", cutoff)
                    .prepare()
            ), Collections.emptyList());

    }

    public CompletableFuture<Optional<Long>> getLatestTimestampAsync() {
        return readAsync("Query of latest ULCINC timestamp", () -> {
            var latest = dao.queryBuilder().orderBy("timestamp", false).limit(1L).queryForFirst();
            return Optional.ofNullable(latest).map(ULCINCRecord::getTimestamp);
        }, Optional.empty());
    }

}
