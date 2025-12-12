package sant1ago.dev.suprim.core.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of building a query: SQL string, parameters, and eager load specifications.
 *
 * @param sql the generated SQL query string
 * @param parameters the named parameters map
 * @param eagerLoads the list of eager load specifications
 */
public record QueryResult(
        String sql,
        Map<String, Object> parameters,
        List<EagerLoadSpec> eagerLoads
) {

    /**
     * Constructor without eager loads (for backwards compatibility).
     */
    public QueryResult(String sql, Map<String, Object> parameters) {
        this(sql, parameters, Collections.emptyList());
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueryResult{sql='").append(sql).append("', params=").append(parameters);
        if (hasEagerLoads()) {
            sb.append(", eagerLoads=").append(eagerLoads.size());
        }
        sb.append("}");
        return sb.toString();
    }
}
