package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.query.QueryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts named parameters (:paramName) to positional parameters (?) for JDBC.
 *
 * <pre>{@code
 * // Suprim generates: INSERT INTO users (email) VALUES (:p1)
 * // JDBC needs:       INSERT INTO users (email) VALUES (?)
 *
 * SqlParameterConverter.Result result = SqlParameterConverter.convert(queryResult);
 * PreparedStatement ps = conn.prepareStatement(result.sql());
 * for (int i = 0; i < result.parameters().length; i++) {
 *     ps.setObject(i + 1, result.parameters()[i]);
 * }
 * }</pre>
 */
public final class SqlParameterConverter {

    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":(\\w+)");

    private SqlParameterConverter() {
        // Utility class
    }

    /**
     * Convert a QueryResult with named parameters to positional parameters.
     *
     * @param queryResult the query result from Suprim builders
     * @return converted result with positional SQL and ordered parameter array
     */
    public static Result convert(QueryResult queryResult) {
        String sql = queryResult.sql();
        Map<String, Object> namedParams = queryResult.parameters();

        if (namedParams.isEmpty()) {
            return new Result(sql, new Object[0]);
        }

        List<Object> orderedParams = new ArrayList<>();
        StringBuffer convertedSql = new StringBuffer();

        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = namedParams.get(paramName);
            orderedParams.add(value);
            matcher.appendReplacement(convertedSql, "?");
        }
        matcher.appendTail(convertedSql);

        return new Result(convertedSql.toString(), orderedParams.toArray());
    }

    /**
     * Result of parameter conversion.
     *
     * @param sql the SQL with positional parameters (?)
     * @param parameters ordered parameter values matching the ? placeholders
     */
    public record Result(String sql, Object[] parameters) {
    }
}
