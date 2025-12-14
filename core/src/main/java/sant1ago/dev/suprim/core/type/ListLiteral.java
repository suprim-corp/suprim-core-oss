package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.query.ParameterContext;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A list of literal values for IN clauses.
 *
 * @param <V> the Java type of the list elements
 * @param values the list of values
 * @param elementType the class of the element type
 */
public record ListLiteral<V>(List<V> values, Class<V> elementType) implements Expression<V> {

    @Override
    public Class<V> getValueType() {
        return elementType;
    }

    @Override
    public String toSql(SqlDialect dialect) {
        if (Objects.isNull(values) || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(v -> new Literal<>(v, elementType).toSql(dialect))
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toSql(SqlDialect dialect, ParameterContext params) {
        if (Objects.isNull(values) || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(v -> new Literal<>(v, elementType).toSql(dialect, params))
                .collect(Collectors.joining(", "));
    }
}
