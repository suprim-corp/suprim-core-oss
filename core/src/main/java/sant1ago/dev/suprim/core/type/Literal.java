package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.query.ParameterContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * A literal value in SQL.
 *
 * @param <V> the Java type of the value
 * @param value the literal value
 * @param type the class of the value type
 */
public record Literal<V>(V value, Class<V> type) implements Expression<V> {

    @Override
    public Class<V> getValueType() {
        return type;
    }

    @Override
    public String toSql(SqlDialect dialect) {
        if (Objects.isNull(value)) {
            return dialect.nullLiteral();
        }
        if (value instanceof String s) {
            return dialect.quoteString(s);
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean b) {
            return dialect.formatBoolean(b);
        }
        if (value instanceof LocalDateTime ldt) {
            return dialect.quoteString(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (value instanceof LocalDate ld) {
            return dialect.quoteString(ld.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (value instanceof UUID uuid) {
            return dialect.formatUuid(uuid);
        }
        // Fallback: quote as string
        return dialect.quoteString(value.toString());
    }

    @Override
    public String toSql(SqlDialect dialect, ParameterContext params) {
        if (Objects.isNull(value)) {
            return dialect.nullLiteral();
        }
        // Use parameter placeholder instead of inline value
        String paramName = params.addParameter(value);
        // UUID needs CAST for PostgreSQL (use supportsJsonb as proxy for PostgreSQL)
        if (value instanceof UUID && dialect.capabilities().supportsJsonb()) {
            return "CAST(:" + paramName + " AS uuid)";
        }
        return ":" + paramName;
    }
}
