package org.unitedlands.unitedlands.classes.db;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.unitedlands.classes.PlaytimeRecord;

public class PlaytimeRecordService extends BaseDbService<PlaytimeRecord> {

    public PlaytimeRecordService(Dao<PlaytimeRecord, UUID> dao) {
        super(dao);
    }

    public CompletableFuture<List<PlaytimeRecord>> getForLastDaysAsync(int days) {

        var cutoff = LocalDate.now().minusDays(days).atStartOfDay(ZoneId.systemDefault())
                                           .toInstant()
                                           .toEpochMilli();

        return readAsync("Query of playtime records", () -> dao.query(
                dao.queryBuilder().where()
                    .ge("logon", cutoff)
                    .prepare()
            ), Collections.emptyList());

    }

    public CompletableFuture<List<PlaytimeRecord>> getForLastDaysAsync(UUID playerId, int days) {

        var cutoff = LocalDate.now().minusDays(days).atStartOfDay(ZoneId.systemDefault())
                                           .toInstant()
                                           .toEpochMilli();

        return readAsync("Query of playtime records for " + playerId, () -> dao.query(
                dao.queryBuilder().where()
                    .ge("logon", cutoff)
                    .and()
                    .eq("player_uuid", playerId)
                    .prepare()
            ), Collections.emptyList());

    }

}
