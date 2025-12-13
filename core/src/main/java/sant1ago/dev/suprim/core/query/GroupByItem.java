package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Expression;

/**
 * Type-safe representation of items that can appear in a GROUP BY clause.
 * Only allows Column or raw SQL expression.
 */
public sealed interface GroupByItem permits GroupByItem.ColumnItem, GroupByItem.RawItem, GroupByItem.ExpressionItem {

    /**
     * Render this group by item as SQL.
     *
     * @param dialect the SQL dialect to use for rendering
     * @return the SQL representation of this group by item
     */
    String toSql(SqlDialect dialect);

    /**
     * Wrap a Column as a GroupByItem.
     *
     * @param column the column to wrap
     * @return a GroupByItem for the column
     */
    static GroupByItem of(Column<?, ?> column) {
        return new ColumnItem(column);
    }

    /**
     * Wrap any Expression as a GroupByItem.
     * Handles Column and generic expressions (JsonPathExpression, etc.).
     *
     * @param expr the expression to wrap
     * @return a GroupByItem for the expression
     */
    static GroupByItem of(Expression<?> expr) {
        if (expr instanceof Column<?, ?> col) {
            return new ColumnItem(col);
        }
        return new ExpressionItem(expr);
    }

    /**
     * Create a raw SQL group by item.
     *
     * @param sql the raw SQL expression
     * @return a GroupByItem for the raw SQL
     */
    static GroupByItem raw(String sql) {
        return new RawItem(sql);
    }

    /**
     * Column group by item.
     *
     * @param column the column to group by
     */
    record ColumnItem(Column<?, ?> column) implements GroupByItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return column.toSql(dialect);
        }
    }

    /**
     * Raw SQL group by item.
     *
     * @param sql the raw SQL expression
     */
    record RawItem(String sql) implements GroupByItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return sql;
        }
    }

    /**
     * Generic expression group by item (for JsonPathExpression, etc.).
     *
     * @param expression the expression to group by
     */
    record ExpressionItem(Expression<?> expression) implements GroupByItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return expression.toSql(dialect);
        }
    }
}
