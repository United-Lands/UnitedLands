package org.unitedlands.unitedlands.classes.db;

import org.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.libs.ormlite.field.FieldType;
import org.unitedlands.libs.ormlite.misc.TransactionManager;
import org.unitedlands.utils.United;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class BaseDbService<T extends Identifiable> {

    protected final Dao<T, UUID> dao;

    private static final int BATCH_SIZE = 500;

    // Dedicated pool; keep it <= your Hikari maximumPoolSize
    private static final ExecutorService WRITE_EXECUTOR = Executors
            .newSingleThreadExecutor(threadFactory("unitedlands-db-write"));
    private static final ExecutorService READ_EXECUTOR = Executors.newFixedThreadPool(4,
            threadFactory("unitedlands-db-read"));

    @FunctionalInterface
    protected interface SqlSupplier<R> {
        R get() throws SQLException;
    }

    public BaseDbService(Dao<T, UUID> dao) {
        this.dao = dao;
    }

    private static ThreadFactory threadFactory(String name) {
        var counter = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, name + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    public static void shutdownExecutors(long timeoutSeconds) {
        READ_EXECUTOR.shutdownNow();
        WRITE_EXECUTOR.shutdown();
        try {
            if (!WRITE_EXECUTOR.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                var dropped = WRITE_EXECUTOR.shutdownNow();
                United.logger().error("[DB] Timed out waiting for pending writes, " + dropped.size()
                        + " write(s) were not saved.", "UnitedLands");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            WRITE_EXECUTOR.shutdownNow();
        }
    }

    private static <R> CompletableFuture<R> submit(ExecutorService executor, Supplier<R> work) {
        try {
            return CompletableFuture.supplyAsync(work, executor);
        } catch (RejectedExecutionException ex) {
            try {
                return CompletableFuture.completedFuture(work.get());
            } catch (Exception inner) {
                return CompletableFuture.failedFuture(inner);
            }
        }
    }

    private <R> Supplier<R> logged(String operation, SqlSupplier<R> work, R fallback) {
        return () -> {
            try {
                return work.get();
            } catch (Exception e) {
                United.logger().error("[DB] " + operation + " failed: " + e.getMessage(), "UnitedLands");
                return fallback;
            }
        };
    }

    protected <R> CompletableFuture<R> readAsync(String operation, SqlSupplier<R> query, R fallback) {
        return submit(READ_EXECUTOR, logged(operation, query, fallback));
    }

    protected <R> CompletableFuture<R> writeAsync(String operation, SqlSupplier<R> write, R fallback) {
        return submit(WRITE_EXECUTOR, logged(operation, write, fallback));
    }

    private String describe(T entity) {
        return dao.getDataClass().getSimpleName() + " " + (entity != null ? entity.getUuid() : "null");
    }

    private String describe(String operation, Collection<T> entities) {
        return operation + " of " + entities.size() + " " + dao.getDataClass().getSimpleName() + " entities";
    }

    protected T snapshot(T entity) {
        try {
            var constructor = dao.getDataClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            T copy = constructor.newInstance();
            for (FieldType field : dao.getTableInfo().getFieldTypes()) {
                field.assignField(dao.getConnectionSource(), copy, field.extractJavaFieldValue(entity), false,
                        null);
            }
            return copy;
        } catch (Exception ex) {
            United.logger().warning("[DB] Could not snapshot " + describe(entity) + ", writing live object: "
                    + ex.getMessage(), "UnitedLands");
            return entity;
        }
    }

    private List<T> snapshot(Collection<T> entities) {
        List<T> copies = new ArrayList<>(entities.size());
        for (T entity : entities)
            copies.add(snapshot(entity));
        return copies;
    }

    public CompletableFuture<Optional<T>> getAsync(UUID id) {
        return readAsync("Query of " + dao.getDataClass().getSimpleName() + " " + id,
                () -> Optional.ofNullable(dao.queryForId(id)), Optional.empty());
    }

    public CompletableFuture<List<T>> getAllAsync() {
        return readAsync("Query of all " + dao.getDataClass().getSimpleName() + " entities",
                () -> dao.queryForAll(), Collections.emptyList());
    }

    public CompletableFuture<Boolean> createAsync(T entity) {
        var copy = snapshot(entity);
        return writeAsync("Create of " + describe(entity), () -> dao.create(copy) > 0, false);
    }

    public CompletableFuture<Integer> createAllAsync(Collection<T> entities) {
        return createAllAsync(entities, null);
    }

    public CompletableFuture<Integer> createAllAsync(Collection<T> entities,
            BiConsumer<Integer, Integer> onProgress) {

        List<T> list = snapshot(entities);
        String operation = describe("Create", entities);

        return submit(WRITE_EXECUTOR, () -> {
            int total = list.size();
            int batches = (total + BATCH_SIZE - 1) / BATCH_SIZE;
            int created = 0;
            long start = System.currentTimeMillis();

            United.logger().info("[DB] Starting import of " + total + " entities in " + batches + " batch(es).");

            for (int i = 0, batchNo = 1; i < total; i += BATCH_SIZE, batchNo++) {
                List<T> batch = list.subList(i, Math.min(i + BATCH_SIZE, total));
                long batchStart = System.currentTimeMillis();
                try {
                    created += TransactionManager.callInTransaction(
                            dao.getConnectionSource(), () -> dao.create(batch));
                } catch (SQLException e) {
                    United.logger().error("[DB] " + operation + " failed at batch " + batchNo + "/" + batches
                            + " (" + created + "/" + total + " already committed): " + e.getMessage(),
                            "UnitedLands");
                    throw new CompletionException(e);
                }

                int processed = Math.min(i + BATCH_SIZE, total);
                United.logger().info(String.format("[DB] Batch %d/%d done (%d/%d, %d%%) in %d ms",
                        batchNo, batches, processed, total,
                        processed * 100 / total,
                        System.currentTimeMillis() - batchStart));

                if (onProgress != null) {
                    try {
                        onProgress.accept(processed, total);
                    } catch (Exception ignored) {
                        // a faulty callback shouldn't abort the import
                    }
                }
            }

            United.logger().info("[DB] Import finished: " + created + "/" + total + " entities in "
                    + (System.currentTimeMillis() - start) + " ms.");
            return created;
        });
    }

    public CompletableFuture<Boolean> updateAsync(T entity) {
        var copy = snapshot(entity);
        return writeAsync("Update of " + describe(entity), () -> dao.update(copy) > 0, false);
    }

    public CompletableFuture<Integer> updateAllAsync(Collection<T> entities) {
        return updateAllAsync(entities, null);
    }

    public CompletableFuture<Integer> updateAllAsync(Collection<T> entities,
            BiConsumer<Integer, Integer> onProgress) {

        List<T> list = snapshot(entities);
        String operation = describe("Update", entities);

        return submit(WRITE_EXECUTOR, () -> {
            int total = list.size();
            int batches = (total + BATCH_SIZE - 1) / BATCH_SIZE;
            int updated = 0;
            long start = System.currentTimeMillis();

            United.logger().info("[DB] Starting update of " + total + " entities in " + batches + " batch(es).");

            for (int i = 0, batchNo = 1; i < total; i += BATCH_SIZE, batchNo++) {
                List<T> batch = list.subList(i, Math.min(i + BATCH_SIZE, total));
                long batchStart = System.currentTimeMillis();
                try {
                    int batchUpdated = TransactionManager.callInTransaction(
                            dao.getConnectionSource(), () -> {
                                int count = 0;
                                for (T entity : batch) {
                                    count += dao.update(entity);
                                }
                                return count;
                            });
                    updated += batchUpdated;

                    if (batchUpdated < batch.size()) {
                        United.logger().warning("[DB] Batch " + batchNo + "/" + batches + ": only " + batchUpdated
                                + " of " + batch.size() + " rows were updated (missing rows?).");
                    }
                } catch (SQLException e) {
                    United.logger().error("[DB] " + operation + " failed at batch " + batchNo + "/" + batches
                            + " (" + updated + "/" + total + " already committed): " + e.getMessage(),
                            "UnitedLands");
                    throw new CompletionException(e);
                }

                int processed = Math.min(i + BATCH_SIZE, total);
                United.logger().info(String.format("[DB] Update batch %d/%d done (%d/%d, %d%%) in %d ms",
                        batchNo, batches, processed, total,
                        processed * 100 / total,
                        System.currentTimeMillis() - batchStart));

                if (onProgress != null) {
                    try {
                        onProgress.accept(processed, total);
                    } catch (Exception ignored) {
                        // a faulty callback shouldn't abort the update
                    }
                }
            }

            United.logger().info("[DB] Update finished: " + updated + "/" + total + " entities in "
                    + (System.currentTimeMillis() - start) + " ms.");
            return updated;
        });
    }

    public CompletableFuture<Boolean> deleteAsync(T entity) {
        var copy = snapshot(entity);
        return writeAsync("Delete of " + describe(entity), () -> dao.delete(copy) > 0, false);
    }

    public CompletableFuture<Integer> deleteAllAsync(Collection<T> entities) {
        return deleteAllAsync(entities, null);
    }

    public CompletableFuture<Integer> deleteAllAsync(Collection<T> entities,
            BiConsumer<Integer, Integer> onProgress) {

        List<T> list = snapshot(entities);
        String operation = describe("Delete", entities);

        return submit(WRITE_EXECUTOR, () -> {
            int total = list.size();
            int batches = (total + BATCH_SIZE - 1) / BATCH_SIZE;
            int deleted = 0;
            long start = System.currentTimeMillis();

            United.logger().info("[DB] Starting deletion of " + total + " entities in " + batches + " batch(es).");

            for (int i = 0, batchNo = 1; i < total; i += BATCH_SIZE, batchNo++) {
                List<T> batch = list.subList(i, Math.min(i + BATCH_SIZE, total));
                long batchStart = System.currentTimeMillis();
                try {
                    deleted += TransactionManager.callInTransaction(
                            dao.getConnectionSource(), () -> dao.delete(batch));
                } catch (SQLException e) {
                    United.logger().error("[DB] " + operation + " failed at batch " + batchNo + "/" + batches
                            + " (" + deleted + "/" + total + " already deleted): " + e.getMessage(),
                            "UnitedLands");
                    throw new CompletionException(e);
                }

                int processed = Math.min(i + BATCH_SIZE, total);
                United.logger().info(String.format("[DB] Delete batch %d/%d done (%d/%d, %d%%) in %d ms",
                        batchNo, batches, processed, total,
                        processed * 100 / total,
                        System.currentTimeMillis() - batchStart));

                if (onProgress != null) {
                    try {
                        onProgress.accept(processed, total);
                    } catch (Exception ignored) {
                        // a faulty callback shouldn't abort the deletion
                    }
                }
            }

            United.logger().info("[DB] Deletion finished: " + deleted + "/" + total + " entities in "
                    + (System.currentTimeMillis() - start) + " ms.");
            return deleted;
        });
    }
}
