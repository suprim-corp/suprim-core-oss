package sant1ago.dev.suprim.annotation.relationship;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a HasOne relationship as returning a single related record based on a custom aggregate function.
 * <p>
 * This is a specialized variant of HasOne that applies an aggregate function (MAX, MIN, etc.)
 * to select the appropriate record. Commonly used for "highest bid", "lowest price", etc.
 * <p>
 * Must be used together with {@link HasOne} annotation.
 * <p>
 * Example:
 * <pre>
 * {@code
 * @HasOne(entity = Bid.class, foreignKey = "auction_id")
 * @OfMany(column = "amount", aggregate = "MAX")
 * private Bid highestBid;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OfMany {

    /**
     * The column to apply the aggregate function to.
     * Required.
     *
     * @return column name
     */
    String column();

    /**
     * The aggregate function to use.
     * Supported values: "MAX", "MIN", "SUM", "AVG"
     * Required.
     *
     * @return aggregate function name
     */
    String aggregate();
}
