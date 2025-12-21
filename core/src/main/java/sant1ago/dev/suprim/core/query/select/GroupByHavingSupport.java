package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.GroupByItem;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Expression;
import sant1ago.dev.suprim.core.type.Predicate;

import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Mixin interface providing GROUP BY and HAVING operations.
 */
public interface GroupByHavingSupport extends SelectBuilderCore {

    // ==================== GROUP BY ====================

    /**
     * Add GROUP BY columns.
     */
    default SelectBuilder groupBy(Column<?, ?>... columns) {
        for (Column<?, ?> col : columns) {
            groupByItems().add(GroupByItem.of(col));
        }
        return self();
    }

    /**
     * Add GROUP BY expressions.
     */
    default SelectBuilder groupByExpression(Expression<?>... expressions) {
        for (Expression<?> expr : expressions) {
            groupByItems().add(GroupByItem.of(expr));
        }
        return self();
    }

    /**
     * Add raw GROUP BY expression.
     */
    default SelectBuilder groupByRaw(String rawSql) {
        groupByItems().add(GroupByItem.raw(rawSql));
        return self();
    }

    // ==================== HAVING ====================

    /**
     * Add HAVING clause.
     */
    default SelectBuilder having(Predicate predicate) {
        havingClause(predicate);
        return self();
    }

    /**
     * Add raw SQL HAVING condition.
     */
    default SelectBuilder havingRaw(String rawSql) {
        Predicate rawPredicate = new Predicate.RawPredicate(rawSql);
        if (isNull(havingClause())) {
            havingClause(rawPredicate);
        } else {
            havingClause(havingClause().and(rawPredicate));
        }
        return self();
    }

    /**
     * Add raw SQL HAVING condition with parameters.
     */
    default SelectBuilder havingRaw(String rawSql, Map<String, Object> params) {
        Predicate rawPredicate = new Predicate.ParameterizedRawPredicate(rawSql, params);
        if (isNull(havingClause())) {
            havingClause(rawPredicate);
        } else {
            havingClause(havingClause().and(rawPredicate));
        }
        return self();
    }

    /**
     * Add raw SQL OR HAVING condition.
     */
    default SelectBuilder orHavingRaw(String rawSql) {
        Predicate rawPredicate = new Predicate.RawPredicate(rawSql);
        if (isNull(havingClause())) {
            havingClause(rawPredicate);
        } else {
            havingClause(havingClause().or(rawPredicate));
        }
        return self();
    }

    /**
     * Add raw SQL OR HAVING condition with parameters.
     */
    default SelectBuilder orHavingRaw(String rawSql, Map<String, Object> params) {
        Predicate rawPredicate = new Predicate.ParameterizedRawPredicate(rawSql, params);
        if (isNull(havingClause())) {
            havingClause(rawPredicate);
        } else {
            havingClause(havingClause().or(rawPredicate));
        }
        return self();
    }
}
