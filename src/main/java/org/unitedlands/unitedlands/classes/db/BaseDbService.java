package org.unitedlands.unitedlands.classes.db;

import org.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.libs.ormlite.misc.TransactionManager;
import org.unitedlands.utils.United;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public abstract class BaseDbService<T extends Identifiable> {

    protected final Dao<T, UUID> dao;

    private static final int BATCH_SIZE = 500;

    // Dedicated pool; keep it <= your Hikari maximumPoolSize
    private static final ExecutorService DB_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "unitedlands-db");
        t.setDaemon(true);
        return t;
    });

    public BaseDbService(Dao<T, UUID> dao) {
        this.dao = dao;
    }

    public CompletableFuture<Optional<T>> getAsync(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Optional.ofNullable(dao.queryForId(id));
            } catch (SQLException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }

    public CompletableFuture<List<T>> getAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.queryForAll();
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

    public CompletableFuture<Boolean> createAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.create(entity) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    // public CompletableFuture<Boolean> createAllAsync(Set<T> entities) {
    // return CompletableFuture.supplyAsync(() -> {
    // try {
    // return dao.create(entities) > 0;
    // } catch (SQLException e) {
    // e.printStackTrace();
    // return false;
    // }
    // });
    // }

    public CompletableFuture<Integer> createAllAsync(Collection<T> entities) {
        return createAllAsync(entities, null);
    }

    public CompletableFuture<Integer> createAllAsync(Collection<T> entities,
            BiConsumer<Integer, Integer> onProgress) {

        return CompletableFuture.supplyAsync(() -> {
            List<T> list = new ArrayList<>(entities);
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
                    United.logger().error("[DB] Import failed at batch " + batchNo + "/" + batches
                            + " (" + created + "/" + total + " already committed).");
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
        }, DB_EXECUTOR);
    }

    public CompletableFuture<Boolean> updateAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.update(entity) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Integer> updateAllAsync(Collection<T> entities) {
        return updateAllAsync(entities, null);
    }

    public CompletableFuture<Integer> updateAllAsync(Collection<T> entities,
            BiConsumer<Integer, Integer> onProgress) {
        return CompletableFuture.supplyAsync(() -> {
            List<T> list = new ArrayList<>(entities);
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
                    United.logger().error("[DB] Update failed at batch " + batchNo + "/" + batches
                            + " (" + updated + "/" + total + " already committed).");
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
        }, DB_EXECUTOR);
    }

    public CompletableFuture<Boolean> deleteAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.delete(entity) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Integer> deleteAllAsync(Collection<T> entities) {
        return deleteAllAsync(entities, null);
    }

    public CompletableFuture<Integer> deleteAllAsync(Collection<T> entities,
            BiConsumer<Integer, Integer> onProgress) {
        return CompletableFuture.supplyAsync(() -> {
            List<T> list = new ArrayList<>(entities);
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
                    United.logger().error("[DB] Deletion failed at batch " + batchNo + "/" + batches
                            + " (" + deleted + "/" + total + " already deleted).");
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
        }, DB_EXECUTOR);
    }
}
