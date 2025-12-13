package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines an inverse polymorphic relationship.
 *
 * <p>This entity can belong to multiple different parent entity types.
 * The actual parent type is determined at runtime using the type discriminator column.
 *
 * <pre>{@code
 * @Entity(table = "images")
 * public class Image {
 *     @MorphTo(name = "imageable")
 *     private Object imageable;  // Could be User, Post, Product, etc.
 *     // Columns: imageable_type VARCHAR, imageable_id BIGINT
 * }
 * }</pre>
 *
 * <p>The type discriminator column stores the parent entity type.
 * The ID column stores the foreign key to the parent entity.
 *
 * <p><b>Note:</b> The field type should be {@code Object} since the actual
 * type is resolved at runtime.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MorphTo {

    /**
     * The morph name used for column prefixes.
     * Example: "imageable" expects imageable_type and imageable_id columns.
     *
     * @return morph name
     */
    String name();

    /**
     * Type discriminator column name.
     * Default: {name}_type
     *
     * @return type column name
     */
    String type() default "";

    /**
     * Foreign key ID column name.
     * Default: {name}_id
     *
     * @return foreign key column name
     */
    String id() default "";

    /**
     * Fetch strategy for this relationship.
     * @see FetchType
     */
    FetchType fetch() default FetchType.LAZY;
}
