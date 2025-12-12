package sant1ago.dev.suprim.annotation.entity;

import java.lang.annotation.*;

/**
 * Specifies database table name and schema.
 * If not present, entity class name is converted to snake_case.
 *
 * <pre>{@code
 * @Entity
 * @Table(name = "user_accounts", schema = "public")
 * public class UserAccount { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
    /**
     * Table name in database (e.g., "users", "order_items").
     * Defaults to snake_case of class name if empty.
     */
    String name() default "";

    /**
     * Database schema (e.g., "public", "analytics").
     * Defaults to database default schema if empty.
     */
    String schema() default "";
}
