package sant1ago.dev.suprim.jdbc.event;

import java.util.Objects;

/**
 * Type of SQL query being executed.
 */
public enum QueryType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    OTHER;

    /**
     * Determine query type from SQL string.
     *
     * @param sql the SQL statement
     * @return the query type
     */
    public static QueryType fromSql(String sql) {
        if (Objects.isNull(sql) || sql.isBlank()) {
            return OTHER;
        }
        String upper = sql.trim().toUpperCase();
        if (upper.startsWith("SELECT")) return SELECT;
        if (upper.startsWith("INSERT")) return INSERT;
        if (upper.startsWith("UPDATE")) return UPDATE;
        if (upper.startsWith("DELETE")) return DELETE;
        if (upper.startsWith("WITH")) {
            // CTEs - check what follows
            if (upper.contains("SELECT")) return SELECT;
            if (upper.contains("INSERT")) return INSERT;
            if (upper.contains("UPDATE")) return UPDATE;
            if (upper.contains("DELETE")) return DELETE;
        }
        return OTHER;
    }
}
