package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;

import static java.util.Objects.nonNull;

/**
 * Order specification for ORDER BY clauses.
 *
 * @param column the column to order by, may be null for raw SQL
 * @param direction the order direction (ASC/DESC), may be null for raw SQL
 * @param rawSql raw SQL order expression, used when column is null
 */
public record OrderSpec(Column<?, ?> column, OrderDirection direction, String rawSql) {

    /**
     * Create an OrderSpec from a column and direction.
     */
    public OrderSpec(Column<?, ?> column, OrderDirection direction) {
        this(column, direction, null);
    }

    /**
     * Create a raw SQL OrderSpec.
     */
    public static OrderSpec raw(String rawSql) {
        return new OrderSpec(null, null, rawSql);
    }

    /**
     * Create an OrderSpec from an expression and direction.
     */
    public static OrderSpec of(Expression<?> expression, OrderDirection direction) {
        if (expression instanceof Column<?, ?> col) {
            return new OrderSpec(col, direction, null);
        }
        return raw(expression.toSql(PostgreSqlDialect.INSTANCE) + " " + direction.name());
    }

    /**
     * Render as SQL: column ASC/DESC
     */
    public String toSql(SqlDialect dialect) {
        if (nonNull(rawSql)) {
            return rawSql;
        }
        return column.toSql(dialect) + " " + direction.name();
    }
}
