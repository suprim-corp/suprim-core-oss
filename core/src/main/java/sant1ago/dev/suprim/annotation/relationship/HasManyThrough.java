package sant1ago.dev.suprim.annotation.relationship;

import sant1ago.dev.suprim.annotation.type.FetchType;

import java.lang.annotation.*;

/**
 * Defines a has-many-through relationship via an intermediate table.
 *
 * <pre>{@code
 * // Country hasManyThrough Posts via Users
 * @Entity(table = "countries")
 * public class Country {
 *     @Id private Long id;
 *
 *     @HasManyThrough(
 *         entity = Post.class,
 *         through = User.class,
 *         firstKey = "country_id",    // on users table
 *         secondKey = "user_id",      // on posts table
 *         localKey = "id",            // on countries table
 *         secondLocalKey = "id"       // on users table
 *     )
 *     private List<Post> posts;
 * }
 *
 * // Generated JOIN:
 * // LEFT JOIN users ON users.country_id = countries.id
 * // LEFT JOIN posts ON posts.user_id = users.id
 * }</pre>
 *
 * <p>This relationship allows access to distant relations through
 * an intermediate model. Useful when you have Country -> User -> Post
 * and want to access all posts for a country directly.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasManyThrough {

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
