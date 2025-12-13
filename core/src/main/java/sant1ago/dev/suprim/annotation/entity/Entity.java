package sant1ago.dev.suprim.annotation.entity;

import java.lang.annotation.*;

/**
 * Marks a class as a database entity for query building.
 *
 * <pre>{@code
 * @Entity(table = "users", schema = "public", with = {"profile", "settings"})
 * public class User {
 *     @Id private Long id;
 *     @Column private String email;
 *     @HasOne(entity = Profile.class) private Profile profile;
 *     @HasOne(entity = Settings.class) private Settings settings;
 * }
 *
 * // Usage:
 * Suprim.table(User.class)  // "users"
 * Suprim.schema(User.class) // "public"
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Entity {
    /**
     * Table name in database (e.g., "users", "order_items").
     * Defaults to snake_case of class name if empty.
     *
     * @return the table name
     */
    String table() default "";

    /**
     * Database schema (e.g., "public", "analytics").
     * Defaults to empty (database default schema).
     *
     * @return the schema name
     */
    String schema() default "";

    /**
     * Relationships to eager load by default when querying this entity.
     * These are automatically loaded unless explicitly excluded with .without().
     * <pre>{@code
     * @Entity(table = "users", with = {"profile", "settings"})
     * public class User {
     *     @HasOne(entity = Profile.class) private Profile profile;
     *     @HasOne(entity = Settings.class) private Settings settings;
     *     @HasMany(entity = Post.class) private List<Post> posts;
     * }
     * // profile and settings are automatically loaded in all queries
     * // posts is NOT loaded unless explicitly requested with .with(User_.POSTS)
     * }</pre>
     *
     * @return array of relationship names to eager load by default
     */
    String[] with() default {};
}
