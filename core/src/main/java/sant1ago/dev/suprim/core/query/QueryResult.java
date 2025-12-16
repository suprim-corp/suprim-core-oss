package sant1ago.dev.suprim.core.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of building a query: SQL string, parameters, eager load specifications, and soft delete scope.
 *
 * @param sql the generated SQL query string
 * @param parameters the named parameters map
 * @param eagerLoads the list of eager load specifications
 * @param softDeleteScope the soft delete scope for query filtering
 */
public record QueryResult(
        String sql,
        Map<String, Object> parameters,
        List<EagerLoadSpec> eagerLoads,
        SelectBuilder.SoftDeleteScope softDeleteScope
) {

    /**
     * Constructor without eager loads and soft delete scope (for backwards compatibility).
     */
    public QueryResult(String sql, Map<String, Object> parameters) {
        this(sql, parameters, Collections.emptyList(), SelectBuilder.SoftDeleteScope.DEFAULT);
    }

    /**
     * Constructor without soft delete scope (for backwards compatibility).
     */
    public QueryResult(String sql, Map<String, Object> parameters, List<EagerLoadSpec> eagerLoads) {
        this(sql, parameters, eagerLoads, SelectBuilder.SoftDeleteScope.DEFAULT);
    }

    /**
     * Get unmodifiable parameter map.
     */
    @Override
    public Map<String, Object> parameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Get unmodifiable eager loads list.
     */
    @Override
    public List<EagerLoadSpec> eagerLoads() {
        return Collections.unmodifiableList(eagerLoads);
    }

    /**
     * Check if this query has eager loads.
     *
     * @return true if there are eager load specifications
     */
    public boolean hasEagerLoads() {
        return Objects.nonNull(eagerLoads) && !eagerLoads.isEmpty();
    }

    /**
     * Get parameter values as an ordered array.
     * Useful for JDBC prepared statements when parameters are in order (p1, p2, ...).
     *
     * @return array of parameter values in key order
     */
    public Object[] parameterValues() {
        return parameters.values().toArray();
    }

    /**
     * Check if this query has any parameters.
     *
     * @return true if parameters map is not empty
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    /**
     * Check if soft-deleted records should be included.
     *
     * @return true if WITH_TRASHED scope is set
     */
    public boolean isWithTrashed() {
        return softDeleteScope == SelectBuilder.SoftDeleteScope.WITH_TRASHED;
    }

    /**
     * Check if only soft-deleted records should be returned.
     *
     * @return true if ONLY_TRASHED scope is set
     */
    public boolean isOnlyTrashed() {
        return softDeleteScope == SelectBuilder.SoftDeleteScope.ONLY_TRASHED;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueryResult{sql='").append(sql).append("', params=").append(parameters);
        if (hasEagerLoads()) {
            sb.append(", eagerLoads=").append(eagerLoads.size());
        }
        if (softDeleteScope != SelectBuilder.SoftDeleteScope.DEFAULT) {
            sb.append(", softDeleteScope=").append(softDeleteScope);
        }
        sb.append("}");
        return sb.toString();
    }
}
