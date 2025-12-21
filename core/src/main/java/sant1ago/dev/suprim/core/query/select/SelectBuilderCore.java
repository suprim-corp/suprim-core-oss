package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.query.EagerLoadSpec;
import sant1ago.dev.suprim.core.query.GroupByItem;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.query.SelectBuilder.SoftDeleteScope;
import sant1ago.dev.suprim.core.query.SelectItem;
import sant1ago.dev.suprim.core.type.OrderSpec;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.Table;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Core interface providing state access for SelectBuilder mixins.
 * All mixin interfaces extend this to access builder state.
 * Enables trait-style composition via default methods.
 */
public interface SelectBuilderCore {

    // ==================== STATE ACCESSORS ====================

    /**
     * Get the current WHERE clause predicate.
     */
    Predicate whereClause();

    /**
     * Set the WHERE clause predicate.
     */
    void whereClause(Predicate predicate);

    /**
     * Get the list of JOIN clauses.
     */
    List<JoinClause> joins();

    /**
     * Get the list of SELECT items.
     */
    List<SelectItem> selectItems();

    /**
     * Get the list of ORDER BY specifications.
     */
    List<OrderSpec> orderSpecs();

    /**
     * Get the list of GROUP BY items.
     */
    List<GroupByItem> groupByItems();

    /**
     * Get the current HAVING clause predicate.
     */
    Predicate havingClause();

    /**
     * Set the HAVING clause predicate.
     */
    void havingClause(Predicate predicate);

    /**
     * Get the FROM table.
     */
    Table<?> fromTable();

    /**
     * Get the parameters map.
     */
    Map<String, Object> parameters();

    /**
     * Get the list of eager load specifications.
     */
    List<EagerLoadSpec> eagerLoads();

    /**
     * Get the set of relations excluded from eager loading.
     */
    Set<String> withoutRelations();

    /**
     * Get the list of CTE clauses.
     */
    List<CteClause> ctes();

    /**
     * Check if CTEs are recursive.
     */
    boolean recursive();

    /**
     * Set whether CTEs are recursive.
     */
    void recursive(boolean recursive);

    /**
     * Get the list of set operations (UNION, INTERSECT, EXCEPT).
     */
    List<SetOperation> setOperations();

    /**
     * Get the current lock mode.
     */
    String lockMode();

    /**
     * Set the lock mode.
     */
    void lockMode(String mode);

    /**
     * Get the soft delete scope.
     */
    SoftDeleteScope softDeleteScope();

    /**
     * Set the soft delete scope.
     */
    void softDeleteScope(SoftDeleteScope scope);

    // ==================== FLUENT SUPPORT ====================

    /**
     * Return this builder for fluent chaining.
     * Implementations should return (SelectBuilder) this.
     */
    SelectBuilder self();

    /**
     * Generate the next parameter name (p1, p2, p3, ...).
     */
    String nextParamName();

    // ==================== INNER TYPES ====================

    /**
     * Type of JOIN operation.
     */
    enum JoinType {
        INNER("JOIN"),
        LEFT("LEFT JOIN"),
        RIGHT("RIGHT JOIN"),
        FULL("FULL JOIN"),
        CROSS("CROSS JOIN"),
        RAW("");

        private final String sql;

        JoinType(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    /**
     * Represents a JOIN clause in a query.
     */
    record JoinClause(JoinType type, Table<?> table, Predicate on) {
    }

    /**
     * Represents a Common Table Expression (CTE) clause.
     */
    final class CteClause {
        private final String name;
        private final SelectBuilder subquery;
        private final String rawSql;

        public CteClause(String name, SelectBuilder subquery) {
            this.name = name;
            this.subquery = subquery;
            this.rawSql = null;
        }

        public CteClause(String name, String rawSql) {
            this.name = name;
            this.subquery = null;
            this.rawSql = rawSql;
        }

        public String name() {
            return name;
        }

        public SelectBuilder subquery() {
            return subquery;
        }

        public String rawSql() {
            return rawSql;
        }

        public String toSql(SqlDialect dialect) {
            if (nonNull(rawSql)) {
                return name + " AS (" + rawSql + ")";
            }
            // subquery is guaranteed non-null when rawSql is null (see constructors)
            return name + " AS (" + requireNonNull(subquery).build(dialect).sql() + ")";
        }
    }

    /**
     * Represents a set operation (UNION, INTERSECT, EXCEPT).
     */
    record SetOperation(String operator, SelectBuilder other) {
    }
}
