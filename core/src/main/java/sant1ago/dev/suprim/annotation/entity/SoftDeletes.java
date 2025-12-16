package sant1ago.dev.suprim.annotation.entity;

import java.lang.annotation.*;

/**
 * Enables soft deletes for an entity.
 *
 * <p>When an entity has this annotation, calling {@code delete()} will set
 * the {@code deleted_at} column to the current timestamp instead of actually
 * deleting the row. The record remains in the database but is "hidden" from
 * normal queries.
 *
 * <pre>{@code
 * @Entity(table = "users")
 * @SoftDeletes
 * public class User extends SuprimEntity {
 *     @Id private UUID id;
 *     @Column private String email;
 *     @Column private LocalDateTime deletedAt; // or Instant, OffsetDateTime, Timestamp
 * }
 *
 * // Usage:
 * user.delete();      // Sets deleted_at = NOW(), doesn't actually delete
 * user.restore();     // Sets deleted_at = NULL
 * user.forceDelete(); // Actually deletes from database
 * user.trashed();     // Returns true if deleted_at is not null
 *
 * // Query scopes:
 * Suprim.select(...).from(User_.TABLE).build();              // Excludes soft-deleted (default)
 * Suprim.select(...).from(User_.TABLE).withTrashed().build(); // Includes soft-deleted
 * Suprim.select(...).from(User_.TABLE).onlyTrashed().build(); // Only soft-deleted
 * }</pre>
 *
 * <p><b>Supported field types:</b> {@code LocalDateTime}, {@code Instant},
 * {@code OffsetDateTime}, {@code java.sql.Timestamp}, {@code java.util.Date}
 *
 * <p><b>Database schema:</b> Add a nullable timestamp column named {@code deleted_at}
 * (or custom name via {@link #column()}):
 * <pre>{@code
 * -- PostgreSQL
 * ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP NULL;
 *
 * -- MySQL
 * ALTER TABLE users ADD COLUMN deleted_at DATETIME NULL;
 * }</pre>
 *
 * <p><b>Performance:</b> For optimal query performance, add a partial index:
 * <pre>{@code
 * -- PostgreSQL (partial index for active records)
 * CREATE INDEX idx_users_active ON users (id) WHERE deleted_at IS NULL;
 *
 * -- PostgreSQL (partial index for soft-deleted records)
 * CREATE INDEX idx_users_trashed ON users (deleted_at) WHERE deleted_at IS NOT NULL;
 *
 * -- MySQL (filtered index via generated column)
 * ALTER TABLE users ADD COLUMN is_deleted BOOLEAN GENERATED ALWAYS AS (deleted_at IS NOT NULL);
 * CREATE INDEX idx_users_active ON users (id, is_deleted);
 * }</pre>
 *
 * @see sant1ago.dev.suprim.jdbc.SuprimEntity#delete()
 * @see sant1ago.dev.suprim.jdbc.SuprimEntity#restore()
 * @see sant1ago.dev.suprim.jdbc.SuprimEntity#forceDelete()
 * @see sant1ago.dev.suprim.jdbc.SuprimEntity#trashed()
 * @see sant1ago.dev.suprim.core.query.SelectBuilder#withTrashed()
 * @see sant1ago.dev.suprim.core.query.SelectBuilder#onlyTrashed()
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SoftDeletes {

    /**
     * Column name for the deleted timestamp.
     * Defaults to "deleted_at".
     *
     * @return the column name for soft delete timestamp
     */
    String column() default "deleted_at";
}
