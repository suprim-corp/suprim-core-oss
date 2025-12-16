package sant1ago.dev.suprim.core.dialect;

import java.util.Objects;

/**
 * MySQL 5.7+ dialect implementation.
 * Uses backticks for identifiers and handles MySQL-specific JSON syntax.
 */
public class MySqlDialect implements SqlDialect {

    public static final MySqlDialect INSTANCE = new MySqlDialect();

    protected MySqlDialect() {
    }

    @Override
    public String quoteIdentifier(String identifier) {
        if (Objects.isNull(identifier)) {
            return null;
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    @Override
    public String quoteString(String value) {
        if (Objects.isNull(value)) {
            return nullLiteral();
        }
        return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
    }

    @Override
    public String formatBoolean(Boolean value) {
        if (Objects.isNull(value)) {
            return nullLiteral();
        }
        return value ? "TRUE" : "FALSE";
    }

    @Override
    public String getName() {
        return "MySQL";
    }

    @Override
    public DialectCapabilities capabilities() {
        return DialectCapabilities.MYSQL_5_7;
    }

    @Override
    public String formatUuid(java.util.UUID uuid) {
        // MySQL stores UUIDs as CHAR(36), no cast needed
        return quoteString(uuid.toString());
    }

    @Override
    public String jsonExtract(String column, String key, boolean asText) {
        String op = asText ? "->>" : "->";
        return column + op + "'$." + escapeJsonKey(key) + "'";
    }

    @Override
    public String jsonContains(String column, String jsonValue) {
        return "JSON_CONTAINS(" + column + ", " + quoteString(jsonValue) + ")";
    }

    @Override
    public String jsonKeyExists(String column, String key) {
        return "JSON_CONTAINS_PATH(" + column + ", 'one', '$." + escapeJsonKey(key) + "')";
    }

    /**
     * Escape special characters in JSON key for MySQL syntax.
     *
     * @param key the JSON key to escape
     * @return escaped JSON key
     */
    private String escapeJsonKey(String key) {
        return key.replace("\"", "\\\"").replace("'", "\\'");
    }
}
