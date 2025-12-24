package sant1ago.dev.suprim.jdbc;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.jdbc.exception.PersistenceException;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
 *
 * <p><b>Lombok Builder Support:</b> Use {@code @SuperBuilder} on child classes:
 * <pre>{@code
 * @Entity(table = "users")
 * @SuperBuilder
 * @NoArgsConstructor
 * public class User extends SuprimEntity {
 *     // fields...
 * }
 *
 * User user = User.builder()
 *     .email("test@example.com")
 *     .build();
 * }</pre>
 */
@SuperBuilder
@NoArgsConstructor
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
     * Save multiple entities in efficient batch INSERT operations.
     * IDs are generated and set on all entities.
     *
     * <p>For best performance, this method:
     * <ul>
     *   <li>Uses single multi-value INSERT statement (10-50x faster)</li>
     *   <li>Generates application-side IDs for UUID_V4/UUID_V7 strategies</li>
     *   <li>Uses RETURNING clause (PostgreSQL) or GENERATED_KEYS (MySQL) for DB-generated IDs</li>
     *   <li>Automatically chunks large batches (default 500 entities per batch)</li>
     * </ul>
     *
     * <pre>{@code
     * List<User> users = List.of(user1, user2, user3);
     * List<User> saved = SuprimEntity.saveAll(users);
     * // All entities now have IDs set
     * }</pre>
     *
     * @param entities list of entities to save
     * @param <T>      entity type
     * @return the saved entities with IDs set
     * @throws IllegalStateException if no transaction context and no global executor
     * @throws PersistenceException  if batch save fails
     */
    public static <T extends SuprimEntity> List<T> saveAll(List<T> entities) {
        if (Objects.isNull(entities) || entities.isEmpty()) {
            return new ArrayList<>();
        }

        if (SuprimContext.hasContext()) {
            Connection connection = SuprimContext.getConnection();
            SqlDialect dialect = SuprimContext.getDialect();
            return BatchPersistence.saveAll(entities, connection, dialect);
        } else {
            SuprimExecutor executor = SuprimContext.getGlobalExecutor();
            if (Objects.isNull(executor)) {
                throw new IllegalStateException(
                    "No active transaction context and no global executor registered. " +
                    "Either call saveAll() within executor.transaction(tx -> {...}) " +
                    "or register a global executor with SuprimContext.setGlobalExecutor(executor)"
                );
            }
            return executor.executeAutoCommit((conn, dialect) ->
                BatchPersistence.saveAll(entities, conn, dialect));
        }
    }

    /**
     * Upsert this entity (INSERT ... ON CONFLICT DO UPDATE).
     *
     * <p>If a record with the same conflict columns exists, it will be updated.
     * Otherwise, a new record is inserted.
     *
     * <pre>{@code
     * User user = new User();
     * user.setEmail("test@example.com");
     * user.setName("Test");
     * user.upsert("email");  // Upsert with email as conflict column
     * }</pre>
     *
     * @param conflictColumns columns that define the conflict (PK or unique constraint)
     * @return this entity
     * @throws IllegalStateException if no transaction context and no global executor
     * @throws PersistenceException  if upsert fails
     */
    public SuprimEntity upsert(String... conflictColumns) {
        return upsert(conflictColumns, null);
    }

    /**
     * Upsert this entity with specific columns to update on conflict.
     *
     * @param conflictColumns columns that define the conflict
     * @param updateColumns   columns to update on conflict (null = all non-conflict)
     * @return this entity
     */
    public SuprimEntity upsert(String[] conflictColumns, String[] updateColumns) {
        if (SuprimContext.hasContext()) {
            Connection connection = SuprimContext.getConnection();
            SqlDialect dialect = SuprimContext.getDialect();
            UpsertPersistence.upsert(this, connection, dialect, conflictColumns, updateColumns);
        } else {
            SuprimExecutor executor = SuprimContext.getGlobalExecutor();
            if (Objects.isNull(executor)) {
                throw new IllegalStateException(
                    "No active transaction context and no global executor registered. " +
                    "Either call upsert() within executor.transaction(tx -> {...}) " +
                    "or register a global executor with SuprimContext.setGlobalExecutor(executor)"
                );
            }
            executor.executeAutoCommit((conn, dialect) -> {
                UpsertPersistence.upsert(this, conn, dialect, conflictColumns, updateColumns);
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

    // ==================== Active Record Enhancements ====================

    /**
     * Create a copy of this entity without the ID.
     * Useful for duplicating records.
     *
     * <pre>{@code
     * User original = executor.findById(User.class, id);
     * User copy = original.replicate();
     * copy.setEmail("new@email.com");
     * copy.save();  // Inserts as new record
     * }</pre>
     *
     * @return copy of entity with ID set to null
     */
    @SuppressWarnings("unchecked")
    public <T extends SuprimEntity> T replicate() {
        try {
            T copy = (T) this.getClass().getDeclaredConstructor().newInstance();

            // Copy all fields except ID
            for (Field field : getAllFields(this.getClass())) {
                field.setAccessible(true);

                // Skip ID fields
                if (field.isAnnotationPresent(Id.class)) {
                    continue;
                }

                Object value = field.get(this);
                field.set(copy, value);
            }

            return copy;
        } catch (Exception e) {
            throw new PersistenceException("Failed to replicate entity: " + e.getMessage(), this.getClass());
        }
    }

    /**
     * Create a copy of this entity, excluding specified fields.
     *
     * @param except field names to exclude from copy
     * @return copy of entity
     */
    @SuppressWarnings("unchecked")
    public <T extends SuprimEntity> T replicate(String... except) {
        try {
            T copy = (T) this.getClass().getDeclaredConstructor().newInstance();
            Set<String> excludeSet = Set.of(except);

            for (Field field : getAllFields(this.getClass())) {
                field.setAccessible(true);

                // Skip ID fields
                if (field.isAnnotationPresent(Id.class)) {
                    continue;
                }

                // Skip excluded fields
                if (excludeSet.contains(field.getName())) {
                    continue;
                }

                // Check column annotation for column name match
                Column colAnn = field.getAnnotation(Column.class);
                if (Objects.nonNull(colAnn) && excludeSet.contains(colAnn.name())) {
                    continue;
                }

                Object value = field.get(this);
                field.set(copy, value);
            }

            return copy;
        } catch (Exception e) {
            throw new PersistenceException("Failed to replicate entity: " + e.getMessage(), this.getClass());
        }
    }

    /**
     * Update the updated_at timestamp to current time.
     *
     * <pre>{@code
     * user.touch();  // Updates updated_at to NOW()
     * }</pre>
     *
     * @return this entity
     */
    public SuprimEntity touch() {
        if (SuprimContext.hasContext()) {
            Connection connection = SuprimContext.getConnection();
            SqlDialect dialect = SuprimContext.getDialect();
            EntityPersistence.touch(this, connection, dialect);
        } else {
            SuprimExecutor executor = SuprimContext.getGlobalExecutor();
            if (Objects.isNull(executor)) {
                throw new IllegalStateException(
                    "No active transaction context and no global executor registered."
                );
            }
            executor.executeAutoCommit((conn, dialect) -> {
                EntityPersistence.touch(this, conn, dialect);
                return null;
            });
        }
        return this;
    }

    /**
     * Get all fields including inherited ones.
     */
    private static Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[0]);
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
