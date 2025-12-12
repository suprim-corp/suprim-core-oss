package sant1ago.dev.suprim.core.dialect;

/**
 * Abstraction for database-specific SQL generation.
 * Handles identifier quoting, string escaping, and type formatting.
 */
public interface SqlDialect {

    /**
     * Quote an identifier (table name, column name).
     * Example: PostgreSQL uses "identifier", MySQL uses `identifier`
     *
     * @param identifier the identifier to quote
     * @return quoted identifier
     */
    String quoteIdentifier(String identifier);

    /**
     * Escape and quote a string literal.
     * Must prevent SQL injection by properly escaping special characters.
     *
     * @param value the string value to quote
     * @return quoted string literal
     */
    String quoteString(String value);

    /**
     * Format boolean value for SQL.
     * PostgreSQL: TRUE/FALSE, MySQL: 1/0
     *
     * @param value the boolean value to format
     * @return formatted boolean literal
     */
    String formatBoolean(Boolean value);

    /**
     * Get parameter placeholder format.
     * Named: ":paramName", Positional: "?"
     *
     * @param paramName the parameter name
     * @return parameter placeholder string
     */
    default String parameterPlaceholder(String paramName) {
        return ":" + paramName;
    }

    /**
     * Get NULL literal representation.
     *
     * @return NULL literal string
     */
    default String nullLiteral() {
        return "NULL";
    }

    /**
     * Get dialect name for identification.
     *
     * @return dialect name
     */
    String getName();

    /**
     * Get dialect capabilities for feature detection.
     * Default returns full capabilities (PostgreSQL).
     *
     * @return dialect capabilities
     */
    default DialectCapabilities capabilities() {
        return DialectCapabilities.FULL;
    }

    // ==================== JSON SUPPORT ====================

    /**
     * Format JSON path extraction.
     * PostgreSQL: column->'key' or column->>'key'
     * MySQL: column->'$.key' or column->>'$.key'
     *
     * @param column the column name
     * @param key the JSON key to extract
     * @param asText whether to extract as text
     * @return formatted JSON extraction expression
     */
    default String jsonExtract(String column, String key, boolean asText) {
        String op = asText ? "->>" : "->";
        return column + op + quoteString(key);
    }

    /**
     * Format JSON contains check.
     * PostgreSQL: column @> 'value'::jsonb
     * MySQL: JSON_CONTAINS(column, 'value')
     *
     * @param column the column name
     * @param jsonValue the JSON value to check
     * @return formatted JSON contains expression
     */
    default String jsonContains(String column, String jsonValue) {
        return column + " @> " + quoteString(jsonValue) + "::jsonb";
    }

    /**
     * Format JSON key exists check.
     * PostgreSQL: column ? 'key'
     * MySQL: JSON_CONTAINS_PATH(column, 'one', '$.key')
     *
     * @param column the column name
     * @param key the JSON key to check
     * @return formatted JSON key exists expression
     */
    default String jsonKeyExists(String column, String key) {
        return column + " ? " + quoteString(key);
    }

    // ==================== CASE-INSENSITIVE SEARCH ====================

    /**
     * Format ILIKE expression with fallback for unsupported dialects.
     * PostgreSQL: column ILIKE pattern
     * MySQL/Others: LOWER(column) LIKE LOWER(pattern)
     *
     * @param column the column SQL
     * @param pattern the pattern to match (with placeholders)
     * @return formatted SQL expression for case-insensitive LIKE
     */
    default String ilike(String column, String pattern) {
        if (capabilities().supportsIlike()) {
            return column + " ILIKE " + pattern;
        }
        // Fallback for dialects without ILIKE support
        return "LOWER(" + column + ") LIKE LOWER(" + pattern + ")";
    }

    /**
     * Format ILIKE with NOT for negation.
     * PostgreSQL: column NOT ILIKE pattern
     * MySQL/Others: LOWER(column) NOT LIKE LOWER(pattern)
     *
     * @param column the column SQL
     * @param pattern the pattern to match (with placeholders)
     * @return formatted SQL expression for case-insensitive NOT LIKE
     */
    default String notIlike(String column, String pattern) {
        if (capabilities().supportsIlike()) {
            return column + " NOT ILIKE " + pattern;
        }
        return "LOWER(" + column + ") NOT LIKE LOWER(" + pattern + ")";
    }

    // ==================== LOCKING SUPPORT ====================

    /**
     * Format FOR UPDATE clause with optional modifiers.
     * Throws UnsupportedDialectFeatureException if requested feature not supported.
     *
     * @param nowait if true, add NOWAIT modifier (requires supportsNowait())
     * @param skipLocked if true, add SKIP LOCKED modifier (requires supportsSkipLocked())
     * @return formatted FOR UPDATE clause
     * @throws UnsupportedDialectFeatureException if requested modifier not supported
     */
    default String forUpdate(boolean nowait, boolean skipLocked) {
        StringBuilder sql = new StringBuilder("FOR UPDATE");

        if (nowait) {
            if (!capabilities().supportsNowait()) {
                throw new UnsupportedDialectFeatureException("NOWAIT", getName(),
                        "Consider upgrading to MySQL 8.0+ or use PostgreSQL");
            }
            sql.append(" NOWAIT");
        }

        if (skipLocked) {
            if (!capabilities().supportsSkipLocked()) {
                throw new UnsupportedDialectFeatureException("SKIP LOCKED", getName(),
                        "Consider upgrading to MySQL 8.0+ or use PostgreSQL");
            }
            sql.append(" SKIP LOCKED");
        }

        return sql.toString();
    }

    /**
     * Format FOR SHARE clause with optional modifiers.
     *
     * @param nowait if true, add NOWAIT modifier
     * @param skipLocked if true, add SKIP LOCKED modifier
     * @return formatted FOR SHARE clause
     * @throws UnsupportedDialectFeatureException if requested modifier not supported
     */
    default String forShare(boolean nowait, boolean skipLocked) {
        StringBuilder sql = new StringBuilder("FOR SHARE");

        if (nowait) {
            if (!capabilities().supportsNowait()) {
                throw new UnsupportedDialectFeatureException("NOWAIT", getName());
            }
            sql.append(" NOWAIT");
        }

        if (skipLocked) {
            if (!capabilities().supportsSkipLocked()) {
                throw new UnsupportedDialectFeatureException("SKIP LOCKED", getName());
            }
            sql.append(" SKIP LOCKED");
        }

        return sql.toString();
    }

    // ==================== RETURNING CLAUSE ====================

    /**
     * Format RETURNING clause for INSERT/UPDATE/DELETE.
     * Throws if dialect doesn't support RETURNING.
     *
     * @param columns the columns to return (comma-separated)
     * @return formatted RETURNING clause
     * @throws UnsupportedDialectFeatureException if RETURNING not supported
     */
    default String returning(String columns) {
        if (!capabilities().supportsReturning()) {
            throw new UnsupportedDialectFeatureException("RETURNING", getName(),
                    "Use SELECT LAST_INSERT_ID() after INSERT for MySQL");
        }
        return "RETURNING " + columns;
    }

    // ==================== ARRAY SUPPORT ====================

    /**
     * Format array contains check.
     * PostgreSQL: column @> ARRAY['value']
     * Others: throws UnsupportedDialectFeatureException
     *
     * @param column the array column
     * @param value the value to check
     * @return formatted array contains expression
     * @throws UnsupportedDialectFeatureException if arrays not supported
     */
    default String arrayContains(String column, String value) {
        if (!capabilities().supportsArrays()) {
            throw new UnsupportedDialectFeatureException("ARRAY", getName(),
                    "Consider using JSON arrays with JSON_CONTAINS() for MySQL");
        }
        return column + " @> ARRAY[" + value + "]";
    }

    /**
     * Format array overlap check.
     * PostgreSQL: column &amp;&amp; ARRAY['value1', 'value2']
     *
     * @param column the array column
     * @param values the array literal to check overlap
     * @return formatted array overlap expression
     * @throws UnsupportedDialectFeatureException if arrays not supported
     */
    default String arrayOverlap(String column, String values) {
        if (!capabilities().supportsArrays()) {
            throw new UnsupportedDialectFeatureException("ARRAY", getName());
        }
        return column + " && " + values;
    }

    // ==================== AGGREGATE FILTER ====================

    /**
     * Format aggregate FILTER clause.
     * PostgreSQL: COUNT(*) FILTER (WHERE condition)
     * Others: throws UnsupportedDialectFeatureException (use CASE instead)
     *
     * @param aggregate the aggregate function (e.g., "COUNT(*)")
     * @param condition the filter condition
     * @return formatted aggregate with filter
     * @throws UnsupportedDialectFeatureException if FILTER clause not supported
     */
    default String aggregateFilter(String aggregate, String condition) {
        if (!capabilities().supportsFilterClause()) {
            throw new UnsupportedDialectFeatureException("FILTER clause", getName(),
                    "Use CASE expression: SUM(CASE WHEN condition THEN 1 ELSE 0 END)");
        }
        return aggregate + " FILTER (WHERE " + condition + ")";
    }

    // ==================== DISTINCT ON ====================

    /**
     * Format DISTINCT ON clause.
     * PostgreSQL: SELECT DISTINCT ON (columns) ...
     *
     * @param columns the columns for DISTINCT ON
     * @return formatted DISTINCT ON clause
     * @throws UnsupportedDialectFeatureException if DISTINCT ON not supported
     */
    default String distinctOn(String columns) {
        if (!capabilities().supportsDistinctOn()) {
            throw new UnsupportedDialectFeatureException("DISTINCT ON", getName(),
                    "Use GROUP BY with subquery or window functions instead");
        }
        return "DISTINCT ON (" + columns + ")";
    }

    // ==================== LICENSING ====================

    /**
     * Check if this dialect requires a commercial license.
     * Open-source databases (PostgreSQL, MySQL, MariaDB) return false.
     * Commercial databases (Oracle, SQL Server, DB2) return true.
     *
     * @return true if dialect requires commercial license
     */
    default boolean isCommercial() {
        return false;
    }
}
