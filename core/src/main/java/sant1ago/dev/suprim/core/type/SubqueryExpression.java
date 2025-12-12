package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.query.SelectBuilder;

/**
 * Represents a subquery expression for use in WHERE clauses.
 * Supports EXISTS, NOT EXISTS, and IN subquery operations.
 *
 * @param <V> the return type of the subquery
 * @param subquery the select builder containing the subquery
 * @param valueType the class representing the value type
 */
public record SubqueryExpression<V>(SelectBuilder subquery, Class<V> valueType) implements Expression<V> {

    @Override
    public Class<V> getValueType() {
        return valueType;
    }

    @Override
    public String toSql(SqlDialect dialect) {
        // No parentheses here - the caller (IN operator) adds them
        return subquery.build(dialect).sql();
    }

    /**
     * Create EXISTS predicate: EXISTS (subquery)
     */
    public static Predicate exists(SelectBuilder subquery) {
        return new ExistsPredicate(subquery, false);
    }

    /**
     * Create NOT EXISTS predicate: NOT EXISTS (subquery)
     */
    public static Predicate notExists(SelectBuilder subquery) {
        return new ExistsPredicate(subquery, true);
    }

    /**
     * EXISTS/NOT EXISTS predicate implementation.
     *
     * @param subquery the subquery to check existence
     * @param negated true for NOT EXISTS, false for EXISTS
     */
    public record ExistsPredicate(SelectBuilder subquery, boolean negated) implements Predicate {
        @Override
        public String toSql(SqlDialect dialect) {
            String prefix = negated ? "NOT EXISTS" : "EXISTS";
            return prefix + " (" + subquery.build(dialect).sql() + ")";
        }
    }
}
