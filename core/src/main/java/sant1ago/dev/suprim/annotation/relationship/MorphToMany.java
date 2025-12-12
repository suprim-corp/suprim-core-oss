package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.CascadeType;
import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines a polymorphic many-to-many relationship.
 *
 * <p>This entity can be related to many instances of the target entity
 * through a polymorphic pivot table that includes a type discriminator.
 *
 * <pre>{@code
 * @Entity(table = "posts")
 * public class Post {
 *     @MorphToMany(entity = Tag.class, name = "taggable")
 *     private Set<Tag> tags;
 *     // Pivot: taggables (taggable_type, taggable_id, tag_id)
 * }
 * }</pre>
 *
 * <p>The pivot table contains:
 * <ul>
 *   <li>Type discriminator column: stores the parent entity type</li>
 *   <li>Foreign key column: references the parent entity</li>
 *   <li>Related key column: references the related entity</li>
 * </ul>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MorphToMany {

    /**
     * The related entity class.
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
     * Type discriminator column name in pivot table.
     * Default: {name}_type
     *
     * @return type column name
     */
    String type() default "";

    /**
     * Foreign key column name in pivot table.
     * Default: {name}_id
     *
     * @return foreign key column name
     */
    String id() default "";

    /**
     * This entity's foreign key column in pivot table.
     * Alias for id() parameter.
     * Default: {name}_id
     *
     * @return foreign pivot key column name
     */
    String foreignPivotKey() default "";

    /**
     * Related entity's foreign key column in pivot table.
     * Default: {relatedEntity}_id
     *
     * @return related pivot key column name
     */
    String relatedPivotKey() default "";

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
