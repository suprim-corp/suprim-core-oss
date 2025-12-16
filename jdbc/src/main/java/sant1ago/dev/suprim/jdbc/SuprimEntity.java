package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.jdbc.exception.PersistenceException;

import java.sql.Connection;
import java.util.Objects;

/**
 * Abstract base class enabling Active Record pattern.
 *
 * <p>Extend this class to add {@code save()} capability to your entities:
 *
 * <pre>{@code
 * @Entity(table = "users")
 * public class User extends SuprimEntity {
 *     @Id(strategy = GenerationType.UUID_V7)
 *     private UUID id;
 *     @Column private String email;
 *     // getters/setters
 * }
 *
 * // Auto-commit mode (register executor once at startup)
 * SuprimContext.setGlobalExecutor(executor);
 *
 * User user = new User();
 * user.setEmail("test@example.com");
 * user.save();  // Auto-commits immediately
 *
 * // Explicit transaction (for atomic multi-operations)
 * executor.transaction(tx -> {
 *     tx.save(user);
 *     tx.save(profile);  // Both commit together
 * });
 * }</pre>
 *
 * <p>The entity must have:
 * <ul>
 *   <li>{@code @Entity} annotation with table name</li>
 *   <li>A field annotated with {@code @Id} (with strategy or manual)</li>
 *   <li>Fields annotated with {@code @Column}</li>
 * </ul>
 */
public abstract class SuprimEntity {

    /**
     * Save this entity to the database.
     *
     * <p>Works in two modes:
     * <ul>
     *   <li><b>Transaction mode:</b> Uses the current transaction's connection</li>
     *   <li><b>Auto-commit mode:</b> Gets a new connection and commits immediately
     *       (requires {@link SuprimContext#setGlobalExecutor(SuprimExecutor)} at startup)</li>
     * </ul>
     *
     * <p>Supports all ID generation strategies:
     * <ul>
     *   <li>{@code UUID_V7} / {@code UUID_V4} - Application generates before insert</li>
     *   <li>{@code IDENTITY} - Database generates (SERIAL/AUTO_INCREMENT)</li>
     *   <li>{@code UUID_DB} - Database generates (gen_random_uuid())</li>
     *   <li>Custom generator - Your own IdGenerator implementation</li>
     * </ul>
     *
     * @return this entity (with ID set if generated)
     * @throws IllegalStateException if no transaction context and no global executor
     * @throws PersistenceException if save fails
     */
    public SuprimEntity save() {
        if (SuprimContext.hasContext()) {
            // In transaction - use transaction's connection
            Connection connection = SuprimContext.getConnection();
            SqlDialect dialect = SuprimContext.getDialect();
            EntityPersistence.save(this, connection, dialect);
        } else {
            // Auto-commit mode - use global executor
            SuprimExecutor executor = SuprimContext.getGlobalExecutor();
            if (Objects.isNull(executor)) {
                throw new IllegalStateException(
                    "No active transaction context and no global executor registered. " +
                    "Either call save() within executor.transaction(tx -> {...}) " +
                    "or register a global executor with SuprimContext.setGlobalExecutor(executor)"
                );
            }
            executor.executeAutoCommit((conn, dialect) -> {
                EntityPersistence.save(this, conn, dialect);
                return null;
            });
        }
        return this;
    }

    /**
     * Update this entity in the database.
     *
     * <p>Entity must have an ID set. Updates all non-null columns.
     * Supports both transaction and auto-commit modes.
     *
     * @return this entity
     * @throws IllegalStateException if no transaction context and no global executor
     * @throws PersistenceException if update fails or entity has no ID
     */
    public SuprimEntity update() {
        if (SuprimContext.hasContext()) {
            Connection connection = SuprimContext.getConnection();
            SqlDialect dialect = SuprimContext.getDialect();
            EntityPersistence.update(this, connection, dialect);
        } else {
            SuprimExecutor executor = SuprimContext.getGlobalExecutor();
            if (Objects.isNull(executor)) {
                throw new IllegalStateException(
                    "No active transaction context and no global executor registered. " +
                    "Either call update() within executor.transaction(tx -> {...}) " +
                    "or register a global executor with SuprimContext.setGlobalExecutor(executor)"
                );
            }
            executor.executeAutoCommit((conn, dialect) -> {
                EntityPersistence.update(this, conn, dialect);
                return null;
            });
        }
        return this;
    }

    /**
     * Delete this entity from the database.
     *
     * <p>Entity must have an ID set.
     * Supports both transaction and auto-commit modes.
     *
     * @throws IllegalStateException if no transaction context and no global executor
     * @throws PersistenceException if delete fails or entity has no ID
     */
    public void delete() {
        if (SuprimContext.hasContext()) {
            Connection connection = SuprimContext.getConnection();
            SqlDialect dialect = SuprimContext.getDialect();
            EntityPersistence.delete(this, connection, dialect);
        } else {
            SuprimExecutor executor = SuprimContext.getGlobalExecutor();
            if (Objects.isNull(executor)) {
                throw new IllegalStateException(
                    "No active transaction context and no global executor registered. " +
                    "Either call delete() within executor.transaction(tx -> {...}) " +
                    "or register a global executor with SuprimContext.setGlobalExecutor(executor)"
                );
            }
            executor.executeAutoCommitVoid((conn, dialect) -> {
                EntityPersistence.delete(this, conn, dialect);
            });
        }
    }

    /**
     * Refresh this entity from the database.
     *
     * <p>Reloads all column values from the database. Entity must have an ID set.
     * Supports both transaction and auto-commit modes.
     *
     * @return this entity with refreshed values
     * @throws IllegalStateException if no transaction context and no global executor
     * @throws PersistenceException if refresh fails or entity not found
     */
    public SuprimEntity refresh() {
        if (SuprimContext.hasContext()) {
            Connection connection = SuprimContext.getConnection();
            SqlDialect dialect = SuprimContext.getDialect();
            EntityPersistence.refresh(this, connection, dialect);
        } else {
            SuprimExecutor executor = SuprimContext.getGlobalExecutor();
            if (Objects.isNull(executor)) {
                throw new IllegalStateException(
                    "No active transaction context and no global executor registered. " +
                    "Either call refresh() within executor.transaction(tx -> {...}) " +
                    "or register a global executor with SuprimContext.setGlobalExecutor(executor)"
                );
            }
            executor.executeAutoCommit((conn, dialect) -> {
                EntityPersistence.refresh(this, conn, dialect);
                return null;
            });
        }
        return this;
    }

    // ==================== Soft Delete Methods ====================

    /**
     * Check if this entity is soft-deleted.
     *
     * <p>Returns true if the entity has {@code @SoftDeletes} annotation
     * and the {@code deleted_at} column is not null.
     *
     * @return true if soft-deleted, false otherwise
     */
    public boolean trashed() {
        return EntityPersistence.isTrashed(this);
    }

    /**
     * Restore a soft-deleted entity.
     *
     * <p>Sets {@code deleted_at} to NULL, making the entity visible again.
     * Only works on entities with {@code @SoftDeletes} annotation.
     *
     * @return this entity
     * @throws IllegalStateException if no transaction context and no global executor
     * @throws PersistenceException if entity doesn't have @SoftDeletes or restore fails
     */
    public SuprimEntity restore() {
        if (SuprimContext.hasContext()) {
            Connection connection = SuprimContext.getConnection();
            SqlDialect dialect = SuprimContext.getDialect();
            EntityPersistence.restore(this, connection, dialect);
        } else {
            SuprimExecutor executor = SuprimContext.getGlobalExecutor();
            if (Objects.isNull(executor)) {
                throw new IllegalStateException(
                    "No active transaction context and no global executor registered. " +
                    "Either call restore() within executor.transaction(tx -> {...}) " +
                    "or register a global executor with SuprimContext.setGlobalExecutor(executor)"
                );
            }
            executor.executeAutoCommit((conn, dialect) -> {
                EntityPersistence.restore(this, conn, dialect);
                return null;
            });
        }
        return this;
    }

    /**
     * Force delete this entity from the database.
     *
     * <p>Performs a real DELETE, bypassing soft delete even if
     * {@code @SoftDeletes} is present. Use this to permanently
     * remove a record.
     *
     * @throws IllegalStateException if no transaction context and no global executor
     * @throws PersistenceException if delete fails or entity has no ID
     */
    public void forceDelete() {
        if (SuprimContext.hasContext()) {
            Connection connection = SuprimContext.getConnection();
            SqlDialect dialect = SuprimContext.getDialect();
            EntityPersistence.forceDelete(this, connection, dialect);
        } else {
            SuprimExecutor executor = SuprimContext.getGlobalExecutor();
            if (Objects.isNull(executor)) {
                throw new IllegalStateException(
                    "No active transaction context and no global executor registered. " +
                    "Either call forceDelete() within executor.transaction(tx -> {...}) " +
                    "or register a global executor with SuprimContext.setGlobalExecutor(executor)"
                );
            }
            executor.executeAutoCommitVoid((conn, dialect) -> {
                EntityPersistence.forceDelete(this, conn, dialect);
            });
        }
    }
}
