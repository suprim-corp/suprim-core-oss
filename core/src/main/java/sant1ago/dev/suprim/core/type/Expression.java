package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

/**
 * Base interface for all SQL expressions.
 * Expressions can be columns, literals, function calls, or subqueries.
 *
 * @param <V> the Java type of the expression's value
 */
public sealed interface Expression<V> permits Column, Literal, ListLiteral, JsonbColumn.JsonPathExpression, JsonbColumn.JsonLiteral, AliasedColumn, ArrayColumn.ArrayLiteral, SubqueryExpression, Aggregate, Coalesce, SqlFunction {

    /**
     * Get the Java type of this expression's value.
     *
     * @return the value type class
     */
    Class<V> getValueType();

    /**
     * Render this expression as SQL using the given dialect.
     *
     * @param dialect the SQL dialect to use for rendering
     * @return the SQL string representation
     */
    String toSql(SqlDialect dialect);
}
