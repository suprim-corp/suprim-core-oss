package sant1ago.dev.suprim.annotation.relationship;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a HasOne relationship as returning the oldest related record based on an ordering column.
 * <p>
 * This is a specialized variant of HasOne that adds ORDER BY ASC LIMIT 1 to queries.
 * Commonly used for "first order", "earliest post", etc.
 * <p>
 * Must be used together with {@link HasOne} annotation.
 * <p>
 * Example:
 * <pre>
 * {@code
 * @HasOne(entity = Post.class, foreignKey = "user_id")
 * @OldestOfMany(column = "published_at")
 * private Post firstPost;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OldestOfMany {

    /**
     * The column to order by (ascending).
     * Defaults to "created_at".
     *
     * @return column name
     */
    String column() default "created_at";
}
