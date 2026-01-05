package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.query.SelectItem;
import sant1ago.dev.suprim.core.type.OrderDirection;
import sant1ago.dev.suprim.core.type.OrderSpec;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.Relation;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

/**
 * Mixin interface providing pivot table operations.
 * Includes wherePivot, wherePivotIn, wherePivotNull, orderByPivot, selectPivot.
 */
public interface PivotQuerySupport extends SelectBuilderCore {

    /**
     * Filter by a pivot table column value.
     */
    default SelectBuilder wherePivot(Relation<?, ?> relation, String column, Object value) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivot can only be used with BelongsToMany relations");
        }
        String paramName = nextParamName();
        String pivotColumn = relation.getPivotTable() + "." + column;
        Predicate pivotPredicate = new Predicate.RawPredicate(pivotColumn + " = :" + paramName);
        parameters().put(paramName, value);

        if (isNull(whereClause())) {
            whereClause(pivotPredicate);
        } else {
            whereClause(whereClause().and(pivotPredicate));
        }
        return self();
    }

    /**
     * Filter by pivot column being IN a list of values.
     */
    default SelectBuilder wherePivotIn(Relation<?, ?> relation, String column, List<?> values) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotIn can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        List<String> paramNames = new ArrayList<>();
        for (Object value : values) {
            String paramName = nextParamName();
            paramNames.add(":" + paramName);
            parameters().put(paramName, value);
        }
        String inClause = pivotColumn + " IN (" + String.join(", ", paramNames) + ")";
        Predicate pivotPredicate = new Predicate.RawPredicate(inClause);

        if (isNull(whereClause())) {
            whereClause(pivotPredicate);
        } else {
            whereClause(whereClause().and(pivotPredicate));
        }
        return self();
    }

    /**
     * Filter by pivot column being NOT IN a list of values.
     */
    default SelectBuilder wherePivotNotIn(Relation<?, ?> relation, String column, List<?> values) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotNotIn can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        List<String> paramNames = new ArrayList<>();
        for (Object value : values) {
            String paramName = nextParamName();
            paramNames.add(":" + paramName);
            parameters().put(paramName, value);
        }
        String inClause = pivotColumn + " NOT IN (" + String.join(", ", paramNames) + ")";
        Predicate pivotPredicate = new Predicate.RawPredicate(inClause);

        if (isNull(whereClause())) {
            whereClause(pivotPredicate);
        } else {
            whereClause(whereClause().and(pivotPredicate));
        }
        return self();
    }

    /**
     * Filter by pivot column being NULL.
     */
    default SelectBuilder wherePivotNull(Relation<?, ?> relation, String column) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotNull can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        Predicate pivotPredicate = new Predicate.RawPredicate(pivotColumn + " IS NULL");

        if (isNull(whereClause())) {
            whereClause(pivotPredicate);
        } else {
            whereClause(whereClause().and(pivotPredicate));
        }
        return self();
    }

    /**
     * Filter by pivot column being NOT NULL.
     */
    default SelectBuilder wherePivotNotNull(Relation<?, ?> relation, String column) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotNotNull can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        Predicate pivotPredicate = new Predicate.RawPredicate(pivotColumn + " IS NOT NULL");

        if (isNull(whereClause())) {
            whereClause(pivotPredicate);
        } else {
            whereClause(whereClause().and(pivotPredicate));
        }
        return self();
    }

    /**
     * Filter by pivot column between two values.
     */
    default SelectBuilder wherePivotBetween(Relation<?, ?> relation, String column, Object start, Object end) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotBetween can only be used with BelongsToMany relations");
        }
        String startParam = nextParamName();
        String endParam = nextParamName();
        String pivotColumn = relation.getPivotTable() + "." + column;
        String betweenClause = pivotColumn + " BETWEEN :" + startParam + " AND :" + endParam;
        parameters().put(startParam, start);
        parameters().put(endParam, end);
        Predicate pivotPredicate = new Predicate.RawPredicate(betweenClause);

        if (isNull(whereClause())) {
            whereClause(pivotPredicate);
        } else {
            whereClause(whereClause().and(pivotPredicate));
        }
        return self();
    }

    /**
     * Order by a pivot table column.
     */
    default SelectBuilder orderByPivot(Relation<?, ?> relation, String column, OrderDirection direction) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("orderByPivot can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        orderSpecs().add(OrderSpec.raw(pivotColumn + " " + direction.name()));
        return self();
    }

    /**
     * Select a pivot table column with alias.
     */
    default SelectBuilder selectPivot(Relation<?, ?> relation, String column, String alias) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("selectPivot can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        selectItems().add(SelectItem.raw(pivotColumn + " AS " + alias));
        return self();
    }

    /**
     * Select a pivot table column without alias.
     */
    default SelectBuilder selectPivot(Relation<?, ?> relation, String column) {
        return selectPivot(relation, column, column);
    }
}
