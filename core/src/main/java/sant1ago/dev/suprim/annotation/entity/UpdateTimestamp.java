package sant1ago.dev.suprim.annotation.entity;

import sant1ago.dev.suprim.annotation.type.SqlType;

import java.lang.annotation.*;

/**
 * Marks a field to be automatically set to the current timestamp on insert and update.
 *
 * <p>When an entity is saved or updated, the annotated field will be set to
 * the current timestamp. The field is set on both INSERT and UPDATE operations.
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
 *     @UpdateTimestamp
 *     private LocalDateTime updatedAt;
 *
 *     // Only set if null (preserves existing value on updates)
 *     @UpdateTimestamp(onModification = TimestampAction.IF_NULL)
 *     private LocalDateTime firstModifiedAt;
 *
 *     // Disable auto-setting, handle manually
 *     @UpdateTimestamp(onModification = TimestampAction.NEVER)
 *     private LocalDateTime manualUpdatedAt;
 * }
 *
 * // Usage:
 * User user = new User();
 * user.setEmail("test@example.com");
 * user.save();  // Both createdAt and updatedAt are set to NOW()
 *
 * user.setEmail("updated@example.com");
 * user.update();  // Only updatedAt is updated to NOW()
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
 * updated_at TIMESTAMP NOT NULL DEFAULT NOW()
 *
 * -- MySQL
 * updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
 * }</pre>
 *
 * @see CreationTimestamp
 * @see TimestampAction
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UpdateTimestamp {

    /**
     * Column name in database.
     * Defaults to "updated_at".
     *
     * @return the column name
     */
    String column() default "updated_at";

    /**
     * SQL column type.
     * Defaults to TIMESTAMP.
     *
     * @return the SQL type
     */
    SqlType type() default SqlType.TIMESTAMP;

    /**
     * Action to take when setting the timestamp on entity modification.
     *
     * <ul>
     *   <li>{@code NOW} (default) - Always set to current timestamp</li>
     *   <li>{@code IF_NULL} - Set only if the field is null</li>
     *   <li>{@code NEVER} - Never auto-set, handle manually</li>
     * </ul>
     *
     * @return the timestamp action
     */
    TimestampAction onModification() default TimestampAction.NOW;
}
