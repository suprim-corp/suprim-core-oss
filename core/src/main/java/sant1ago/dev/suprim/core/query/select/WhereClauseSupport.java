package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.Predicate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Mixin interface providing WHERE clause operations.
 * Includes where, and, or, raw variants, and conditional methods.
 */
public interface WhereClauseSupport extends SelectBuilderCore {

    // ==================== WHERE ====================

    /**
     * Add WHERE condition.
     */
    default SelectBuilder where(Predicate predicate) {
        whereClause(predicate);
        return self();
    }

    /**
     * Add grouped WHERE condition using closure (Laravel-style).
     * Conditions inside closure are grouped with parentheses.
     */
    default SelectBuilder where(Function<SelectBuilder, SelectBuilder> group) {
        SelectBuilder nested = new SelectBuilder(List.of());
        nested = group.apply(nested);
        Predicate groupedPredicate = nested.getWhereClause();
        if (nonNull(groupedPredicate)) {
            whereClause(groupedPredicate);
        }
        return self();
    }

    /**
     * Add WHERE condition only if value is present (not null).
     */
    default <V> SelectBuilder whereIfPresent(V value, Supplier<Predicate> predicateSupplier) {
        if (nonNull(value)) {
            whereClause(predicateSupplier.get());
        }
        return self();
    }

    /**
     * Add raw SQL WHERE condition.
     */
    default SelectBuilder whereRaw(String rawSql) {
        whereClause(new Predicate.RawPredicate(rawSql));
        return self();
    }

    /**
     * Add raw SQL WHERE condition with parameters.
     */
    default SelectBuilder whereRaw(String rawSql, Map<String, Object> params) {
        whereClause(new Predicate.ParameterizedRawPredicate(rawSql, params));
        return self();
    }

    /**
     * Add WHERE condition only if boolean condition is true.
     */
    default SelectBuilder whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return self();
    }

    // ==================== AND ====================

    /**
     * Add AND condition to WHERE clause.
     */
    default SelectBuilder and(Predicate predicate) {
        if (isNull(whereClause())) {
            whereClause(predicate);
        } else {
            whereClause(whereClause().and(predicate));
        }
        return self();
    }

    /**
     * Add grouped AND condition using closure (Laravel-style).
     * Conditions inside closure are grouped with parentheses.
     */
    default SelectBuilder and(Function<SelectBuilder, SelectBuilder> group) {
        SelectBuilder nested = new SelectBuilder(List.of());
        nested = group.apply(nested);
        Predicate groupedPredicate = nested.getWhereClause();
        if (nonNull(groupedPredicate)) {
            if (isNull(whereClause())) {
                whereClause(groupedPredicate);
            } else {
                whereClause(whereClause().and(groupedPredicate));
            }
        }
        return self();
    }

    /**
     * Add AND condition only if value is present (not null).
     */
    default <V> SelectBuilder andIfPresent(V value, Supplier<Predicate> predicateSupplier) {
        if (nonNull(value)) {
            and(predicateSupplier.get());
        }
        return self();
    }

    /**
     * Add raw SQL AND condition.
     */
    default SelectBuilder andRaw(String rawSql) {
        Predicate rawPredicate = new Predicate.RawPredicate(rawSql);
        if (isNull(whereClause())) {
            whereClause(rawPredicate);
        } else {
            whereClause(whereClause().and(rawPredicate));
        }
        return self();
    }

    /**
     * Add raw SQL AND condition with parameters.
     */
    default SelectBuilder andRaw(String rawSql, Map<String, Object> params) {
        Predicate rawPredicate = new Predicate.ParameterizedRawPredicate(rawSql, params);
        if (isNull(whereClause())) {
            whereClause(rawPredicate);
        } else {
            whereClause(whereClause().and(rawPredicate));
        }
        return self();
    }

    /**
     * Add AND condition only if boolean condition is true.
     */
    default SelectBuilder andIf(boolean condition, Predicate predicate) {
        if (condition) {
            and(predicate);
        }
        return self();
    }

    // ==================== OR ====================

    /**
     * Add OR condition to WHERE clause.
     */
    default SelectBuilder or(Predicate predicate) {
        if (isNull(whereClause())) {
            whereClause(predicate);
        } else {
            whereClause(whereClause().or(predicate));
        }
        return self();
    }

    /**
     * Add grouped OR condition using closure (Laravel-style).
     * Conditions inside closure are grouped with parentheses.
     */
    default SelectBuilder or(Function<SelectBuilder, SelectBuilder> group) {
        SelectBuilder nested = new SelectBuilder(List.of());
        nested = group.apply(nested);
        Predicate groupedPredicate = nested.getWhereClause();
        if (nonNull(groupedPredicate)) {
            if (isNull(whereClause())) {
                whereClause(groupedPredicate);
            } else {
                whereClause(whereClause().or(groupedPredicate));
            }
        }
        return self();
    }

    /**
     * Add OR condition only if value is present (not null).
     */
    default <V> SelectBuilder orIfPresent(V value, Supplier<Predicate> predicateSupplier) {
        if (nonNull(value)) {
            or(predicateSupplier.get());
        }
        return self();
    }

    /**
     * Add raw SQL OR condition.
     */
    default SelectBuilder orRaw(String rawSql) {
        Predicate rawPredicate = new Predicate.RawPredicate(rawSql);
        if (isNull(whereClause())) {
            whereClause(rawPredicate);
        } else {
            whereClause(whereClause().or(rawPredicate));
        }
        return self();
    }

    /**
     * Add raw SQL OR condition with parameters.
     */
    default SelectBuilder orRaw(String rawSql, Map<String, Object> params) {
        Predicate rawPredicate = new Predicate.ParameterizedRawPredicate(rawSql, params);
        if (isNull(whereClause())) {
            whereClause(rawPredicate);
        } else {
            whereClause(whereClause().or(rawPredicate));
        }
        return self();
    }
}
