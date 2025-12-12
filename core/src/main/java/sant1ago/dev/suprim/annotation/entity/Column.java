package sant1ago.dev.suprim.annotation.entity;

import sant1ago.dev.suprim.annotation.type.SqlType;

import java.lang.annotation.*;

/**
 * Defines column mapping and SQL metadata.
 * If not present on a field, field name is converted to snake_case.
 *
 * <pre>{@code
 * // Simple type
 * @Column(type = SqlType.TEXT)
 * private String description;
 *
 * // VARCHAR with length
 * @Column(type = SqlType.VARCHAR, length = 36)
 * private String uuid;
 *
 * // NUMERIC with precision/scale
 * @Column(type = SqlType.NUMERIC, precision = 10, scale = 2)
 * private BigDecimal amount;
 *
 * // Custom SQL type string (escape hatch)
 * @Column(definition = "VARCHAR(36) CHECK (length(id) = 36)")
 * private String id;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column {

    /**
     * Column name in database.
     * Defaults to snake_case of field name if empty.
     *
     * @return the column name
     */
    String name() default "";

    /**
     * SQL column type.
     * Defaults to AUTO (inferred from Java type).
     *
     * @return the SQL type
     */
    SqlType type() default SqlType.AUTO;

    /**
     * Length for VARCHAR, CHAR, BIT, VARBIT types.
     * Default 255 for VARCHAR if not specified.
     * Set to -1 to omit length (unbounded).
     *
     * @return the column length
     */
    int length() default 0;

    /**
     * Precision for NUMERIC, DECIMAL types.
     * Total number of digits.
     *
     * @return the precision
     */
    int precision() default 0;

    /**
     * Scale for NUMERIC, DECIMAL types.
     * Number of digits after decimal point.
     *
     * @return the scale
     */
    int scale() default 0;

    /**
     * Custom SQL type definition (escape hatch).
     * If specified, overrides type/length/precision/scale.
     * Use for complex types like "VARCHAR(100)[]" or types with constraints.
     *
     * @return the custom definition
     */
    String definition() default "";

    /**
     * Whether column accepts NULL values.
     *
     * @return true if nullable
     */
    boolean nullable() default true;

    /**
     * Whether column has unique constraint.
     *
     * @return true if unique
     */
    boolean unique() default false;
}
