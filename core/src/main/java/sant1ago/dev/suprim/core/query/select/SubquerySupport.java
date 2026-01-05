package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Operator;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.SubqueryExpression;

import static java.util.Objects.isNull;

/**
 * Mixin interface providing subquery support operations.
 * Includes EXISTS, NOT EXISTS, IN subquery, and NOT IN subquery.
 */
public interface SubquerySupport extends SelectBuilderCore {

    /**
     * Add WHERE EXISTS (subquery).
     */
    default SelectBuilder whereExists(SelectBuilder subquery) {
        whereClause(SubqueryExpression.exists(subquery));
        return self();
    }

    /**
     * Add WHERE NOT EXISTS (subquery).
     */
    default SelectBuilder whereNotExists(SelectBuilder subquery) {
        whereClause(SubqueryExpression.notExists(subquery));
        return self();
    }

    /**
     * Add AND EXISTS (subquery).
     */
    default SelectBuilder andExists(SelectBuilder subquery) {
        if (isNull(whereClause())) {
            whereClause(SubqueryExpression.exists(subquery));
        } else {
            whereClause(whereClause().and(SubqueryExpression.exists(subquery)));
        }
        return self();
    }

    /**
     * Add AND NOT EXISTS (subquery).
     */
    default SelectBuilder andNotExists(SelectBuilder subquery) {
        if (isNull(whereClause())) {
            whereClause(SubqueryExpression.notExists(subquery));
        } else {
            whereClause(whereClause().and(SubqueryExpression.notExists(subquery)));
        }
        return self();
    }

    /**
     * Add OR EXISTS (subquery) to WHERE clause.
     */
    default SelectBuilder orExists(SelectBuilder subquery) {
        if (isNull(whereClause())) {
            whereClause(SubqueryExpression.exists(subquery));
        } else {
            whereClause(whereClause().or(SubqueryExpression.exists(subquery)));
        }
        return self();
    }

    /**
     * Add OR NOT EXISTS (subquery) to WHERE clause.
     */
    default SelectBuilder orNotExists(SelectBuilder subquery) {
        if (isNull(whereClause())) {
            whereClause(SubqueryExpression.notExists(subquery));
        } else {
            whereClause(whereClause().or(SubqueryExpression.notExists(subquery)));
        }
        return self();
    }

    /**
     * Add WHERE column IN (subquery).
     */
    default SelectBuilder whereInSubquery(Column<?, ?> column, SelectBuilder subquery) {
        SubqueryExpression<?> subExpr = new SubqueryExpression<>(subquery, Object.class);
        whereClause(new Predicate.SimplePredicate(column, Operator.IN, subExpr));
        return self();
    }

    /**
     * Add WHERE column NOT IN (subquery).
     */
    default SelectBuilder whereNotInSubquery(Column<?, ?> column, SelectBuilder subquery) {
        SubqueryExpression<?> subExpr = new SubqueryExpression<>(subquery, Object.class);
        whereClause(new Predicate.SimplePredicate(column, Operator.NOT_IN, subExpr));
        return self();
    }
}
