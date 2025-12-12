package sant1ago.dev.suprim.annotation.relationship;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a HasOne relationship as returning the latest related record based on an ordering column.
 * <p>
 * This is a specialized variant of HasOne that adds ORDER BY DESC LIMIT 1 to queries.
 * Commonly used for "latest order", "most recent post", etc.
 * <p>
 * Must be used together with {@link HasOne} annotation.
 * <p>
 * Example:
 * <pre>
 * {@code
 * @HasOne(entity = Order.class, foreignKey = "user_id")
 * @LatestOfMany(column = "created_at")
 * private Order latestOrder;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LatestOfMany {

    /**
     * The column to order by (descending).
     * Defaults to "created_at".
     *
     * @return column name
     */
    String column() default "created_at";
}
