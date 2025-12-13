package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.CascadeType;
import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines a many-to-many relationship using a pivot table.
 *
 * <pre>{@code
 * @Entity(table = "users")
 * public class User {
 *     @Id private Long id;
 *
 *     // Basic: pivot table "role_user" (alphabetical)
 *     @BelongsToMany(entity = Role.class)
 *     private Set<Role> roles;
 *
 *     // Custom pivot table
 *     @BelongsToMany(entity = Role.class, table = "user_roles")
 *     private Set<Role> roles;
 *
 *     // Include pivot columns in queries
 *     @BelongsToMany(
 *         entity = Role.class,
 *         withPivot = {"assigned_at", "assigned_by"}
 *     )
 *     private Set<Role> roles;
 *
 *     // Pivot table has timestamps
 *     @BelongsToMany(
 *         entity = Role.class,
 *         withTimestamps = true  // includes created_at, updated_at
 *     )
 *     private Set<Role> roles;
 *
 *     // Full customization
 *     @BelongsToMany(
 *         entity = Role.class,
 *         table = "user_roles",
 *         foreignPivotKey = "usr_id",
 *         relatedPivotKey = "role_id",
 *         cascade = CascadeType.APPLICATION
 *     )
 *     private Set<Role> roles;
 * }
 *
 * // Generated JOINs:
 * // LEFT JOIN role_user ON role_user.user_id = users.id
 * // LEFT JOIN roles ON roles.id = role_user.role_id
 * }</pre>
 *
 * <p>Pivot table naming convention (when table is empty):
 * Entity names are converted to snake_case and sorted alphabetically.
 * Example: User + Role → role_user (r &lt; u)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BelongsToMany {

    /**
     * The related entity class.
     *
     * @return the related entity class
     */
    Class<?> entity();

    /**
     * Pivot table name.
     * <ul>
     *   <li>Empty string (default): auto-derived from alphabetically sorted entity names</li>
     *   <li>Explicit value: use that table name</li>
     * </ul>
     *
     * @return the pivot table name
     */
    String table() default "";

    /**
     * This entity's foreign key column in the pivot table.
     * Default: {@code {thisEntity}_id}
     *
     * @return the foreign pivot key column name
     */
    String foreignPivotKey() default "";

    /**
     * Related entity's foreign key column in the pivot table.
     * Default: {@code {relatedEntity}_id}
     *
     * @return the related pivot key column name
     */
    String relatedPivotKey() default "";

    /**
     * Local key column on this entity (usually the primary key).
     *
     * @return the local key column name
     */
    String localKey() default "id";

    /**
     * Primary key column on the related entity.
     *
     * @return the related key column name
     */
    String relatedKey() default "id";

    /**
     * Cascade operations for this relationship.
     * Affects pivot table records.
     *
     * @return the cascade types
     * @see CascadeType
     */
    CascadeType[] cascade() default {};

    /**
     * Fetch strategy for this relationship.
     *
     * @return the fetch type
     * @see FetchType
     */
    FetchType fetch() default FetchType.LAZY;

    /**
     * Additional pivot table columns to include in queries.
     * These columns will be selected when joining through the pivot.
     *
     * @return the pivot columns to include
     */
    String[] withPivot() default {};

    /**
     * Whether the pivot table has timestamp columns (created_at, updated_at).
     * If true, these columns can be accessed in queries.
     *
     * @return true if pivot has timestamps
     */
    boolean withTimestamps() default false;
}
