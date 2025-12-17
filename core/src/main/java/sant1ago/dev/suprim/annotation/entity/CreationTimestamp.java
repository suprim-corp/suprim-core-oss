package sant1ago.dev.suprim.annotation.entity;

import sant1ago.dev.suprim.annotation.type.SqlType;

import java.lang.annotation.*;

/**
 * Marks a field to be automatically set to the current timestamp on entity creation.
 *
 * <p>When an entity is saved for the first time, the annotated field will be
 * set to the current timestamp. The field is only set on INSERT, not on UPDATE.
 *
 * <pre>{@code
 * @Entity(table = "users")
 * public class User extends SuprimEntity {
 *     @Id private UUID id;
 *     @Column private String email;
 *
 *     @CreationTimestamp
 *     private LocalDateTime createdAt;
 *
 *     // Custom behavior: always overwrite even if user sets a value
 *     @CreationTimestamp(onCreation = TimestampAction.NOW)
 *     private LocalDateTime forcedCreatedAt;
 *
 *     // Disable auto-setting, handle manually
 *     @CreationTimestamp(onCreation = TimestampAction.NEVER)
 *     private LocalDateTime manualCreatedAt;
 * }
 *
 * // Usage:
 * User user = new User();
 * user.setEmail("test@example.com");
 * user.save();  // createdAt is automatically set to NOW()
 *
 * user.setEmail("updated@example.com");
 * user.update();  // createdAt remains unchanged
 * }</pre>
 *
 * <p><b>Supported field types:</b>
 * <ul>
 *   <li>{@code LocalDateTime}</li>
 *   <li>{@code Instant}</li>
 *   <li>{@code OffsetDateTime}</li>
 *   <li>{@code ZonedDateTime}</li>
 *   <li>{@code java.sql.Timestamp}</li>
 *   <li>{@code java.util.Date}</li>
 *   <li>{@code Long} (epoch millis)</li>
 * </ul>
 *
 * <p><b>Database schema:</b>
 * <pre>{@code
 * -- PostgreSQL
 * created_at TIMESTAMP NOT NULL DEFAULT NOW()
 *
 * -- MySQL
 * created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
 * }</pre>
 *
 * @see UpdateTimestamp
 * @see TimestampAction
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CreationTimestamp {

    /**
     * Column name in database.
     * Defaults to "created_at".
     *
     * @return the column name
     */
    String column() default "created_at";

    /**
     * SQL column type.
     * Defaults to TIMESTAMP.
     *
     * @return the SQL type
     */
    SqlType type() default SqlType.TIMESTAMP;

    /**
     * Action to take when setting the timestamp on entity creation.
     *
     * <ul>
     *   <li>{@code IF_NULL} (default) - Set only if the field is null</li>
     *   <li>{@code NOW} - Always set to current timestamp</li>
     *   <li>{@code NEVER} - Never auto-set, handle manually</li>
     * </ul>
     *
     * @return the timestamp action
     */
    TimestampAction onCreation() default TimestampAction.IF_NULL;
}
