package sant1ago.dev.suprim.annotation.entity;

import java.lang.annotation.*;

/**
 * Marks a class whose fields should be inherited by @Entity subclasses.
 * Used for base entity classes that define common columns (id, createdAt, etc.).
 *
 * <pre>{@code
 * @MappedSuperclass
 * public class BaseEntity {
 *     @Column(type = SqlType.UUID)
 *     private String id;
 *
 *     @Column(name = "created_at", type = SqlType.TIMESTAMPTZ)
 *     private OffsetDateTime createdAt;
 * }
 *
 * @Entity(table = "users")
 * public class User extends BaseEntity {
 *     @Column
 *     private String email;
 * }
 *
 * // Generated User_ will include: ID, CREATED_AT, EMAIL
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MappedSuperclass {
}
