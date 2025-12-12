package sant1ago.dev.suprim.core.dialect;

import java.util.Objects;

/**
 * PostgreSQL dialect implementation.
 * Uses double quotes for identifiers, handles JSONB, arrays, and PostgreSQL-specific features.
 */
public final class PostgreSqlDialect implements SqlDialect {

    public static final PostgreSqlDialect INSTANCE = new PostgreSqlDialect();

    private PostgreSqlDialect() {
    }

    @Override
    public String quoteIdentifier(String identifier) {
        if (Objects.isNull(identifier)) {
            return null;
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String quoteString(String value) {
        if (Objects.isNull(value)) {
            return nullLiteral();
        }
        return "'" + value.replace("'", "''") + "'";
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
        return "PostgreSQL";
    }
}
