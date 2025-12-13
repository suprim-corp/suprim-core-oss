package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.CascadeType;
import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines an inverse polymorphic many-to-many relationship.
 *
 * <p>This is the inverse of {@link MorphToMany}. The annotated entity
 * can retrieve all instances of a specific related type that have this entity
 * through a polymorphic pivot table.
 *
 * <pre>{@code
 * @Entity(table = "tags")
 * public class Tag {
 *     @MorphedByMany(entity = Post.class, name = "taggable")
 *     private Set<Post> posts;
 *
 *     @MorphedByMany(entity = Video.class, name = "taggable")
 *     private Set<Video> videos;
 *     // Pivot: taggables (tag_id, taggable_type, taggable_id)
 * }
 * }</pre>
 *
 * <p>The pivot table contains:
 * <ul>
 *   <li>Foreign key to this entity (tag_id)</li>
 *   <li>Type discriminator column (taggable_type)</li>
 *   <li>Foreign key to related entity (taggable_id)</li>
 * </ul>
 *
 * <p>Usage in queries:
 * <pre>{@code
 * // Get tags with their posts (type filter: 'Post')
 * Suprim.select().from(Tag_.TABLE)
 *     .whereHas(Tag_.POSTS)
 *     .build();
 *
 * // Eager load posts for tags
 * Suprim.select().from(Tag_.TABLE)
 *     .with(Tag_.POSTS)
 *     .build();
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MorphedByMany {

    /**
     * The related entity class (e.g., Post.class, Video.class).
     *
     * @return related entity class
     */
    Class<?> entity();

    /**
     * The morph name used for column prefixes.
     * Example: "taggable" generates taggable_type and taggable_id columns in pivot.
     *
     * @return morph name
     */
    String name();

    /**
     * Pivot table name.
     * Default: {name}s (pluralized morph name)
     *
     * @return pivot table name
     */
    String table() default "";

    /**
     * This entity's foreign key column in pivot table.
     * Default: {thisEntity}_id (e.g., tag_id)
     *
     * @return foreign pivot key column name
     */
    String foreignPivotKey() default "";

    /**
     * Related entity's foreign key column in pivot table.
     * Default: {name}_id (e.g., taggable_id)
     *
     * @return related pivot key column name
     */
    String relatedPivotKey() default "";

    /**
     * Type discriminator column name in pivot table.
     * Default: {name}_type (e.g., taggable_type)
     *
     * @return morph type column name
     */
    String morphTypeColumn() default "";

    /**
     * Local key column (usually primary key).
     * Default: "id"
     *
     * @return local key column name
     */
    String localKey() default "id";

    /**
     * Related entity's key column.
     * Default: "id"
     *
     * @return related key column name
     */
    String relatedKey() default "id";

    /**
     * Additional pivot table columns to include in queries.
     *
     * @return array of pivot column names
     */
    String[] withPivot() default {};

    /**
     * Whether pivot table has timestamp columns (created_at, updated_at).
     *
     * @return true if pivot has timestamps
     */
    boolean withTimestamps() default false;

    /**
     * Cascade operations for this relationship.
     * @see CascadeType
     */
    CascadeType[] cascade() default {};

    /**
     * Fetch strategy for this relationship.
     * @see FetchType
     */
    FetchType fetch() default FetchType.LAZY;
}
