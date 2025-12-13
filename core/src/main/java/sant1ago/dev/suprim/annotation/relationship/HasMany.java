package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.CascadeType;
import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines a one-to-many relationship where the foreign key is on the related table.
 *
 * <pre>{@code
 * @Entity(table = "users")
 * public class User {
 *     @Id private Long id;
 *
 *     // Basic: FK "user_id" on posts table
 *     @HasMany(entity = Post.class)
 *     private List<Post> posts;
 *
 *     // Custom FK column
 *     @HasMany(entity = Post.class, foreignKey = "author_id")
 *     private List<Post> posts;
 *
 *     // Auto-detect cascade (checks DB, falls back to app queries)
 *     @HasMany(entity = Post.class, cascade = CascadeType.AUTO)
 *     private List<Post> posts;
 *
 *     // Delete orphans when removed from collection
 *     @HasMany(entity = Comment.class, cascade = {CascadeType.AUTO, CascadeType.DELETE_ORPHAN})
 *     private List<Comment> comments;
 *
 *     // Logical relationship (no FK constraint)
 *     @HasMany(entity = AuditLog.class, noForeignKey = true)
 *     private List<AuditLog> auditLogs;
 * }
 *
 * // Generated JOIN:
 * // LEFT JOIN posts ON posts.user_id = users.id
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasMany {

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
}
