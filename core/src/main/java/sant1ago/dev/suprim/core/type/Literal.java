package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

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
        // Fallback: quote as string
        return dialect.quoteString(value.toString());
    }
}
