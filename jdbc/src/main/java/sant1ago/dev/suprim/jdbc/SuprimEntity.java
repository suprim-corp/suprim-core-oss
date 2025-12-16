package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.jdbc.exception.PersistenceException;

import java.sql.Connection;

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
 * // Usage
 * executor.transaction(tx -> {
 *     User user = new User();
 *     user.setEmail("test@example.com");
 *     user.save();  // ID generated automatically
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
     * <p>Must be called within a transaction context established by
     * {@link SuprimExecutor#transaction(java.util.function.Consumer)}.
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
     * @throws IllegalStateException if not within transaction context
     * @throws PersistenceException if save fails
     */
    public SuprimEntity save() {
        Connection connection = SuprimContext.getConnection();
        SqlDialect dialect = SuprimContext.getDialect();
        EntityPersistence.save(this, connection, dialect);
        return this;
    }

    /**
     * Update this entity in the database.
     *
     * <p>Entity must have an ID set. Updates all non-null columns.
     *
     * @return this entity
     * @throws IllegalStateException if not within transaction context
     * @throws PersistenceException if update fails or entity has no ID
     */
    public SuprimEntity update() {
        Connection connection = SuprimContext.getConnection();
        SqlDialect dialect = SuprimContext.getDialect();
        EntityPersistence.update(this, connection, dialect);
        return this;
    }

    /**
     * Delete this entity from the database.
     *
     * <p>Entity must have an ID set.
     *
     * @throws IllegalStateException if not within transaction context
     * @throws PersistenceException if delete fails or entity has no ID
     */
    public void delete() {
        Connection connection = SuprimContext.getConnection();
        SqlDialect dialect = SuprimContext.getDialect();
        EntityPersistence.delete(this, connection, dialect);
    }

    /**
     * Refresh this entity from the database.
     *
     * <p>Reloads all column values from the database. Entity must have an ID set.
     *
     * @return this entity with refreshed values
     * @throws IllegalStateException if not within transaction context
     * @throws PersistenceException if refresh fails or entity not found
     */
    public SuprimEntity refresh() {
        Connection connection = SuprimContext.getConnection();
        SqlDialect dialect = SuprimContext.getDialect();
        EntityPersistence.refresh(this, connection, dialect);
        return this;
    }
}
