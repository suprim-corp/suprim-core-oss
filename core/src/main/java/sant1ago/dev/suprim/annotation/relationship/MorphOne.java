package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.CascadeType;
import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines a polymorphic one-to-one relationship.
 *
 * <p>The related entity belongs to this entity through polymorphic columns
 * (type discriminator and foreign key).
 *
 * <pre>{@code
 * @Entity(table = "users")
 * public class User {
 *     @MorphOne(entity = Image.class, name = "imageable")
 *     private Image avatar;
 *     // images.imageable_type = 'User', images.imageable_id = users.id
 * }
 * }</pre>
 *
 * <p>The type discriminator column stores the entity type (simple class name by default).
 * The ID column stores the foreign key reference.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MorphOne {

    /**
     * The related entity class.
     *
     * @return related entity class
     */
    Class<?> entity();

    /**
     * The morph name used for column prefixes.
     * Example: "imageable" generates imageable_type and imageable_id columns.
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
     * Local key column (usually primary key).
     * Default: "id"
     *
     * @return local key column name
     */
    String localKey() default "id";

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
