package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines the inverse of a hasOne/hasMany relationship where the foreign key is on THIS table.
 *
 * <pre>{@code
 * @Entity(table = "posts")
 * public class Post {
 *     @Id private Long id;
 *
 *     // Basic: FK "user_id" on THIS table (posts)
 *     @BelongsTo(entity = User.class)
 *     private User author;
 *
 *     // Custom FK column
 *     @BelongsTo(entity = User.class, foreignKey = "created_by")
 *     private User creator;
 *
 *     // EAGER loading
 *     @BelongsTo(entity = User.class, fetch = FetchType.EAGER)
 *     private User author;
 *
 *     // Logical relationship
 *     @BelongsTo(entity = Category.class, noForeignKey = true)
 *     private Category category;
 * }
 *
 * // Generated JOIN:
 * // LEFT JOIN users ON posts.user_id = users.id
 * }</pre>
 *
 * <p>Note: Cascade is typically not used with belongsTo because the parent
 * entity controls the lifecycle. The child doesn't cascade to its parent.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BelongsTo {

    /**
     * The parent entity class.
     *
     * @return the parent entity class
     */
    Class<?> entity();

    /**
     * Foreign key column name on THIS table.
     * <ul>
     *   <li>Empty string (default): auto-derived as {@code {relatedEntity}_id}</li>
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
     * @return true if no foreign key constraint
     */
    boolean noForeignKey() default false;

    /**
     * Primary key column on the parent entity.
     *
     * @return the owner key column name
     */
    String ownerKey() default "id";

    /**
     * Fetch strategy for this relationship.
     *
     * @return the fetch type
     * @see FetchType
     */
    FetchType fetch() default FetchType.LAZY;

    /**
     * When true, updates parent's timestamp columns when this entity is saved/updated.
     * Useful for cache invalidation and audit trails.
     *
     * <pre>{@code
     * @Entity(table = "comments")
     * public class Comment {
     *     @BelongsTo(entity = Post.class, touch = true)
     *     private Post post;
     * }
     *
     * // When comment is saved, post.updated_at is automatically updated
     * manager.save(post, Post_.COMMENTS, comment);
     * }</pre>
     *
     * @return true if parent should be touched on save
     */
    boolean touch() default false;

    /**
     * Timestamp columns to update on the parent when this entity is saved/updated.
     * Only applies when {@link #touch()} is true.
     * Defaults to ["updated_at"].
     *
     * <pre>{@code
     * @BelongsTo(entity = Post.class, touch = true, touches = {"updated_at", "comment_updated_at"})
     * private Post post;
     * }</pre>
     *
     * @return the columns to touch
     */
    String[] touches() default {"updated_at"};

    /**
     * When true, returns an empty instance instead of null when the relationship doesn't exist.
     * Useful for avoiding null pointer exceptions in template/view code.
     *
     * <pre>{@code
     * @BelongsTo(entity = User.class, withDefault = true)
     * private User author;
     *
     * // Returns empty User instead of null
     * post.getAuthor().getName();  // Returns null, not NPE
     * }</pre>
     *
     * @return true if default instance should be returned
     */
    boolean withDefault() default false;

    /**
     * Default attribute values to apply when creating a default model instance.
     * Only applies when {@link #withDefault()} is true.
     * Format: "fieldName=value" (e.g., "name=Unknown User")
     *
     * <pre>{@code
     * @BelongsTo(entity = User.class, withDefault = true,
     *         defaultAttributes = {"name=Unknown User", "email=unknown@example.com"})
     * private User author;
     *
     * post.getAuthor().getName();  // "Unknown User"
     * }</pre>
     *
     * @return the default attribute values
     */
    String[] defaultAttributes() default {};
}
