package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.type.AliasedColumn;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Expression;

/**
 * Type-safe representation of items that can appear in a SELECT clause.
 * Supports Column, AliasedColumn, Aggregate, Coalesce, JSONB expressions, or raw SQL.
 */
public sealed interface SelectItem permits SelectItem.ColumnItem, SelectItem.AliasedItem, SelectItem.RawItem, SelectItem.ExpressionItem {

    /**
     * Render this select item as SQL.
     */
    String toSql(SqlDialect dialect);

    /**
     * Wrap a Column as a SelectItem.
     */
    static SelectItem of(Column<?, ?> column) {
        return new ColumnItem(column);
    }

    /**
     * Wrap an AliasedColumn as a SelectItem.
     */
    static SelectItem of(AliasedColumn<?, ?> aliased) {
        return new AliasedItem(aliased);
    }

    /**
     * Wrap any Expression as a SelectItem.
     * Handles Column, AliasedColumn, and generic expressions (Aggregate, Coalesce, etc.).
     */
    static SelectItem of(Expression<?> expr) {
        if (expr instanceof Column<?, ?> col) {
            return new ColumnItem(col);
        } else if (expr instanceof AliasedColumn<?, ?> aliased) {
            return new AliasedItem(aliased);
        }
        // For other expressions (Aggregate, Coalesce, JsonPathExpression, etc.)
        // Store the expression and render with dialect at build time
        return new ExpressionItem(expr);
    }

    /**
     * Create a raw SQL select item.
     */
    static SelectItem raw(String sql) {
        return new RawItem(sql);
    }

    /**
     * Column select item.
     *
     * @param column the column to select
     */
    record ColumnItem(Column<?, ?> column) implements SelectItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return column.toSql(dialect);
        }
    }

    /**
     * Aliased column select item.
     *
     * @param aliased the aliased column to select
     */
    record AliasedItem(AliasedColumn<?, ?> aliased) implements SelectItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return aliased.toSql(dialect);
        }
    }

    /**
     * Raw SQL select item.
     *
     * @param sql the raw SQL expression
     */
    record RawItem(String sql) implements SelectItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return sql;
        }
    }

    /**
     * Generic expression select item (for Aggregate, Coalesce, JsonPathExpression, etc.).
     *
     * @param expression the expression to select
     */
    record ExpressionItem(Expression<?> expression) implements SelectItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return expression.toSql(dialect);
        }
    }
}
