package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.jdbc.exception.ConnectionException;
import sant1ago.dev.suprim.jdbc.exception.ExceptionTranslator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Internal helper for static entity finder methods.
 * Provides query execution for {@link SuprimEntity} static finders.
 */
final class EntityFinder {

    private EntityFinder() {
        // Utility class
    }

    /**
     * Find all entities with offset-based pagination.
     *
     * @param executor    the executor for database access
     * @param entityClass the entity class
     * @param limit       maximum results
     * @param offset      rows to skip
     * @param <T>         entity type
     * @return list of entities
     */
    static <T> List<T> findAll(SuprimExecutor executor, Class<T> entityClass, int limit, int offset) {
        if (Objects.isNull(entityClass)) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }

        EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(entityClass);

        try (Connection conn = executor.getConnectionInternal()) {
            SqlDialect dialect = getDialect(executor, conn);

            String tableName = buildTableName(meta, dialect);
            String idColumn = safeQuoteIdentifier(meta.idColumn(), dialect);

            // Build query with ORDER BY id for consistent pagination
            String sql = "SELECT * FROM " + tableName +
                " ORDER BY " + idColumn +
                " LIMIT ? OFFSET ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ps.setInt(2, offset);

                try (ResultSet rs = ps.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    RowMapper<T> mapper = EntityMapper.of(entityClass);
                    while (rs.next()) {
                        results.add(mapper.map(rs));
                    }
                    return results;
                }
            } catch (SQLException e) {
                throw ExceptionTranslator.translateQuery(sql, new Object[]{limit, offset}, e);
            }
        } catch (SQLException e) {
            throw ConnectionException.fromSQLException(e);
        }
    }

    /**
     * Find all entities with cursor-based pagination.
     *
     * @param executor    the executor for database access
     * @param entityClass the entity class
     * @param cursor      cursor from previous page (null for first)
     * @param limit       results per page
     * @param <T>         entity type
     * @return cursor result with data and pagination info
     */
    static <T> CursorResult<T> findAllWithCursor(SuprimExecutor executor, Class<T> entityClass, String cursor, int limit) {
        if (Objects.isNull(entityClass)) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }

        EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(entityClass);
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);

        try (Connection conn = executor.getConnectionInternal()) {
            SqlDialect dialect = getDialect(executor, conn);

            String tableName = buildTableName(meta, dialect);
            String idColumn = safeQuoteIdentifier(meta.idColumn(), dialect);

            // Decode cursor
            String decodedCursor = CursorResult.decodeCursor(cursor);
            Object cursorValue = parseCursorValue(decodedCursor, idMeta.fieldType());

            // Build query - fetch limit + 1 to check for more pages
            String sql;
            Object[] params;

            if (Objects.isNull(cursorValue)) {
                // First page - no cursor condition
                sql = "SELECT * FROM " + tableName +
                    " ORDER BY " + idColumn +
                    " LIMIT ?";
                params = new Object[]{limit + 1};
            } else {
                // Subsequent pages - WHERE id > cursor
                sql = "SELECT * FROM " + tableName +
                    " WHERE " + idColumn + " > ?" +
                    " ORDER BY " + idColumn +
                    " LIMIT ?";
                params = new Object[]{cursorValue, limit + 1};
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    RowMapper<T> mapper = EntityMapper.of(entityClass);
                    while (rs.next()) {
                        results.add(mapper.map(rs));
                    }

                    // Determine if there are more pages
                    boolean hasMore = results.size() > limit;
                    if (hasMore) {
                        results = results.subList(0, limit);
                    }

                    // Build cursors
                    String nextCursor = null;
                    String prevCursor = null;

                    if (hasMore && !results.isEmpty()) {
                        // Get last item's ID for next cursor
                        T lastItem = results.get(results.size() - 1);
                        Object lastId = EntityReflector.getIdOrNull(lastItem);
                        nextCursor = CursorResult.encodeCursor(lastId);
                    }

                    if (Objects.nonNull(cursorValue)) {
                        // We have a previous page (we came from somewhere)
                        prevCursor = cursor;
                    }

                    return CursorResult.of(results, nextCursor, prevCursor, limit);
                }
            } catch (SQLException e) {
                throw ExceptionTranslator.translateQuery(sql, params, e);
            }
        } catch (SQLException e) {
            throw ConnectionException.fromSQLException(e);
        }
    }

    /**
     * Build fully qualified table name with schema if present.
     */
    private static String buildTableName(EntityReflector.EntityMeta meta, SqlDialect dialect) {
        if (Objects.nonNull(meta.schema()) && !meta.schema().isEmpty()) {
            return safeQuoteIdentifier(meta.schema(), dialect) + "." +
                safeQuoteIdentifier(meta.tableName(), dialect);
        }
        return safeQuoteIdentifier(meta.tableName(), dialect);
    }

    /**
     * Quote identifier only if needed (contains special chars).
     */
    private static String safeQuoteIdentifier(String identifier, SqlDialect dialect) {
        if (Objects.isNull(identifier) || identifier.isEmpty()) {
            return identifier;
        }
        // Only quote if identifier contains special characters
        if (identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return identifier;
        }
        return dialect.quoteIdentifier(identifier);
    }

    /**
     * Parse cursor value to appropriate type based on ID field type.
     */
    private static Object parseCursorValue(String cursor, Class<?> idType) {
        if (Objects.isNull(cursor) || cursor.isEmpty()) {
            return null;
        }

        if (idType == Long.class || idType == long.class) {
            return Long.parseLong(cursor);
        } else if (idType == Integer.class || idType == int.class) {
            return Integer.parseInt(cursor);
        } else if (idType == UUID.class) {
            return UUID.fromString(cursor);
        } else if (idType == String.class) {
            return cursor;
        }
        // Default: return as string
        return cursor;
    }

    /**
     * Get dialect from executor, auto-detecting if needed.
     */
    private static SqlDialect getDialect(SuprimExecutor executor, Connection conn) {
        // Use reflection to access private dialect field or rely on executor's method
        // For now, we'll detect from connection
        return sant1ago.dev.suprim.core.dialect.DialectRegistry.detect(conn);
    }
}
