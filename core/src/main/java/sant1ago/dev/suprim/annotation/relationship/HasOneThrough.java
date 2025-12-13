package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines a has-one-through relationship via an intermediate table.
 *
 * <pre>{@code
 * // Mechanic hasOne Car owner through Car
 * @Entity(table = "mechanics")
 * public class Mechanic {
 *     @Id private Long id;
 *
 *     @HasOneThrough(
 *         entity = Owner.class,
 *         through = Car.class,
 *         firstKey = "mechanic_id",    // on cars table
 *         secondKey = "car_id",         // on owners table
 *         localKey = "id",              // on mechanics table
 *         secondLocalKey = "id"         // on cars table
 *     )
 *     private Owner carOwner;
 * }
 *
 * // Generated JOIN:
 * // LEFT JOIN cars ON cars.mechanic_id = mechanics.id
 * // LEFT JOIN owners ON owners.car_id = cars.id
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasOneThrough {

    /**
     * The final related entity class.
     *
     * @return the final related entity class
     */
    Class<?> entity();

    /**
     * The intermediate entity class.
     *
     * @return the intermediate entity class
     */
    Class<?> through();

    /**
     * Foreign key on the intermediate table pointing to this entity.
     * Default: {thisEntity}_id
     *
     * @return the first foreign key column name
     */
    String firstKey() default "";

    /**
     * Foreign key on the final table pointing to the intermediate entity.
     * Default: {throughEntity}_id
     *
     * @return the second foreign key column name
     */
    String secondKey() default "";

    /**
     * Local key on this entity (usually primary key).
     *
     * @return the local key column name
     */
    String localKey() default "id";

    /**
     * Local key on the intermediate entity (usually primary key).
     *
     * @return the second local key column name
     */
    String secondLocalKey() default "id";

    /**
     * Fetch strategy for this relationship.
     *
     * @return the fetch type
     */
    FetchType fetch() default FetchType.LAZY;
}
