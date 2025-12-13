package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.jdbc.exception.ExceptionTranslator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Internal helper for pagination operations.
 * Package-private - not part of public API.
 */
final class PaginationHelper {

    private final SuprimExecutor executor;

    PaginationHelper(SuprimExecutor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    /**
     * Execute a query with pagination metadata.
     */
    <T> PaginatedResult<T> paginate(SelectBuilder builder, int page, int perPage, RowMapper<T> mapper) {
        if (page < 1) page = 1;
        if (perPage < 1) perPage = 10;

        // Count total
        long total = count(builder);

        // Get page data
        QueryResult dataQuery = builder.paginate(page, perPage).build();
        List<T> data = executor.query(dataQuery, mapper);

        return PaginatedResult.of(data, page, perPage, total);
    }

    /**
     * Execute true keyset (cursor-based) pagination.
     * Uses WHERE column > lastValue instead of OFFSET for O(1) performance.
     */
    <T, V> CursorResult<T> cursorPaginate(
            SelectBuilder builder,
            String cursor,
            int perPage,
            RowMapper<T> mapper,
            Column<T, V> cursorColumn
    ) {
        if (perPage < 1) perPage = 10;

        // Decode cursor to get last seen value
        String decodedCursor = CursorResult.decodeCursor(cursor);
        V lastValue = null;

        if (Objects.nonNull(decodedCursor) && !decodedCursor.isEmpty()) {
            lastValue = parseCursorValue(decodedCursor, cursorColumn.getValueType());
        }

        // Build query with keyset condition (WHERE column > lastValue)
        SelectBuilder queryBuilder = builder.orderBy(cursorColumn.asc());
        if (Objects.nonNull(lastValue)) {
            queryBuilder = queryBuilder.where(cursorColumn.gt(lastValue));
        }
        QueryResult dataQuery = queryBuilder.limit(perPage + 1).build();

        List<T> results = executor.query(dataQuery, mapper);

        // Determine if there are more pages
        boolean hasMore = results.size() > perPage;
        List<T> data = hasMore ? results.subList(0, perPage) : results;

        // Build next cursor from last item's column value
        String nextCursor = null;
        if (hasMore && !data.isEmpty()) {
            T lastItem = data.get(data.size() - 1);
            Object lastColValue = EntityReflector.getFieldByColumnName(lastItem, cursorColumn.getName());
            nextCursor = CursorResult.encodeCursor(lastColValue);
        }

        // Keyset pagination doesn't support backward navigation efficiently
        return CursorResult.of(data, nextCursor, null, perPage);
    }

    /**
     * Count total rows for a query (without pagination).
     */
    long count(SelectBuilder builder) {
        // Build the original query to get SQL and parameters
        QueryResult original = builder.build();
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(original);

        // Wrap the original query in a count query
        String countSql = "SELECT COUNT(*) FROM (" + converted.sql() + ") AS count_query";

        try (Connection conn = executor.getConnectionInternal();
             PreparedStatement ps = conn.prepareStatement(countSql)) {

            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw ExceptionTranslator.translateQuery(countSql, converted.parameters(), e);
        }
    }

    /**
     * Parse cursor value string to appropriate type with type safety.
     */
    private <V> V parseCursorValue(String value, Class<V> targetType) {
        if (Objects.isNull(value)) return null;

        Object result;
        try {
            if (targetType == Long.class || targetType == long.class) {
                result = Long.parseLong(value);
            } else if (targetType == Integer.class || targetType == int.class) {
                result = Integer.parseInt(value);
            } else if (targetType == Double.class || targetType == double.class) {
                result = Double.parseDouble(value);
            } else if (targetType == Float.class || targetType == float.class) {
                result = Float.parseFloat(value);
            } else if (targetType == String.class) {
                result = value;
            } else if (targetType == java.util.UUID.class) {
                result = java.util.UUID.fromString(value);
            } else if (targetType == java.time.LocalDateTime.class) {
                result = java.time.LocalDateTime.parse(value);
            } else if (targetType == java.time.LocalDate.class) {
                result = java.time.LocalDate.parse(value);
            } else if (targetType == java.time.Instant.class) {
                result = java.time.Instant.parse(value);
            } else {
                // Fallback: try to use as-is (String)
                result = value;
            }
            return targetType.cast(result);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot cast cursor value '" + value + "' to " + targetType.getSimpleName(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse cursor value '" + value + "' as " + targetType.getSimpleName(), e);
        }
    }

    private void setParameters(PreparedStatement ps, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            ps.setObject(i + 1, parameters[i]);
        }
    }
}
