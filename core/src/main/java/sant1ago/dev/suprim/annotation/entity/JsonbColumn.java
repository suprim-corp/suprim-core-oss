package sant1ago.dev.suprim.annotation.entity;

import java.lang.annotation.*;

/**
 * Marks column as PostgreSQL JSONB type.
 * Enables JSONB operators ({@code @>}, {@code ->}, {@code ->>}) in queries.
 *
 * <pre>{@code
 * @JsonbColumn
 * @Column(name = "metadata", type = "JSONB")
 * private String metadata;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonbColumn {
}
