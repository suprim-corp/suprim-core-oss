package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.CascadeType;
import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines a one-to-one relationship where the foreign key is on the related table.
 *
 * <pre>{@code
 * @Entity(table = "users")
 * public class User {
 *     @Id private Long id;
 *
 *     // Basic: FK "user_id" on profiles table
 *     @HasOne(entity = Profile.class)
 *     private Profile profile;
 *
 *     // Custom FK column
 *     @HasOne(entity = Profile.class, foreignKey = "owner_id")
 *     private Profile profile;
 *
 *     // EAGER loading (auto-join in SELECT)
 *     @HasOne(entity = Profile.class, fetch = FetchType.EAGER)
 *     private Profile profile;
 *
 *     // Cascade delete
 *     @HasOne(entity = Profile.class, cascade = CascadeType.AUTO)
 *     private Profile profile;
 *
 *     // Logical relationship (no FK constraint in DB)
 *     @HasOne(entity = Settings.class, noForeignKey = true)
 *     private Settings settings;
 * }
 *
 * // Generated JOIN:
 * // LEFT JOIN profiles ON profiles.user_id = users.id
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasOne {

    /**
     * The related entity class.
     *
     * @return the related entity class
     */
    Class<?> entity();

    /**
     * Foreign key column name on the related table.
     * <ul>
     *   <li>Empty string (default): auto-derived as {@code {thisEntity}_id}</li>
     *   <li>Explicit value: use that column name</li>
     * </ul>
     *
     * @return the foreign key column name
     */
    String foreignKey() default "";

    /**
     * Set to true if no physical FK constraint exists in the database.
     * The relationship is logical only - JOINs still work but no FK validation.
     *
     * @return true if no physical FK constraint
     */
    boolean noForeignKey() default false;

    /**
     * Local key column on this entity (usually the primary key).
     *
     * @return the local key column name
     */
    String localKey() default "id";

    /**
     * Cascade operations for this relationship.
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
     * When true, returns an empty instance instead of null when the relationship doesn't exist.
     * Useful for avoiding null pointer exceptions in template/view code.
     *
     * <pre>{@code
     * @HasOne(entity = Profile.class, withDefault = true)
     * private Profile profile;
     *
     * // Returns empty Profile instead of null
     * user.getProfile().getBio();  // Returns null, not NPE
     * }</pre>
     *
     * @return true if default empty instance should be returned
     */
    boolean withDefault() default false;

    /**
     * Default attribute values to apply when creating a default model instance.
     * Only applies when {@link #withDefault()} is true.
     * Format: "fieldName=value" (e.g., "bio=No bio available")
     *
     * <pre>{@code
     * @HasOne(entity = Profile.class, withDefault = true,
     *         defaultAttributes = {"bio=No bio available", "avatar=/default.png"})
     * private Profile profile;
     *
     * user.getProfile().getBio();  // "No bio available"
     * }</pre>
     *
     * @return array of default attribute values
     */
    String[] defaultAttributes() default {};
}
