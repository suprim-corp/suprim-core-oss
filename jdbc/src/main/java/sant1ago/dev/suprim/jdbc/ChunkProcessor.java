package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.jdbc.exception.ExceptionTranslator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Internal helper for chunk/batch processing operations.
 * Package-private - not part of public API.
 */
final class ChunkProcessor {

    private final SuprimExecutor executor;

    ChunkProcessor(SuprimExecutor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    /**
     * Process query results in chunks with custom mapper.
     */
    <T> long chunk(SelectBuilder builder, int chunkSize, RowMapper<T> mapper, Function<List<T>, Boolean> processor) {
        if (chunkSize < 1) chunkSize = 1000;

        long totalProcessed = 0;
        int offset = 0;

        while (true) {
            QueryResult chunkQuery = builder.limit(chunkSize).offset(offset).build();
            List<T> chunk = executor.query(chunkQuery, mapper);

            if (chunk.isEmpty()) {
                break;
            }

            totalProcessed += chunk.size();

            Boolean shouldContinue = processor.apply(chunk);
            if (Boolean.FALSE.equals(shouldContinue)) {
                break;
            }

            if (chunk.size() < chunkSize) {
                break; // Last chunk
            }

            offset += chunkSize;
        }

        return totalProcessed;
    }

    /**
     * Process query results in chunks by ID using keyset pagination (safe for updates during iteration).
     * Uses WHERE id > lastId instead of OFFSET for O(1) performance on large datasets.
     */
    <T, V> long chunkById(
            SelectBuilder builder,
            int chunkSize,
            RowMapper<T> mapper,
            Column<T, V> idColumn,
            Function<List<T>, Boolean> processor
    ) {
        if (chunkSize < 1) chunkSize = 1000;

        long totalProcessed = 0;
        V lastId = null;
        Class<V> idType = idColumn.getValueType();

        while (true) {
            // Build query with keyset condition (WHERE id > lastId)
            SelectBuilder queryBuilder = builder.orderBy(idColumn.asc());
            if (Objects.nonNull(lastId)) {
                queryBuilder = queryBuilder.where(idColumn.gt(lastId));
            }
            QueryResult chunkQuery = queryBuilder.limit(chunkSize).build();

            List<T> chunk = executor.query(chunkQuery, mapper);

            if (chunk.isEmpty()) {
                break;
            }

            totalProcessed += chunk.size();

            Boolean shouldContinue = processor.apply(chunk);
            if (Boolean.FALSE.equals(shouldContinue)) {
                break;
            }

            if (chunk.size() < chunkSize) {
                break; // Last chunk
            }

            // Get last ID for next iteration (keyset) - type-safe cast via Class.cast()
            T lastItem = chunk.get(chunk.size() - 1);
            Object lastIdValue = EntityReflector.getFieldByColumnName(lastItem, idColumn.getName());
            lastId = idType.cast(lastIdValue);
        }

        return totalProcessed;
    }

    /**
     * Create a lazy stream with custom mapper.
     */
    <T> Stream<T> lazy(QueryResult queryResult, RowMapper<T> mapper) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        try {
            Connection conn = executor.getConnectionInternal();
            PreparedStatement ps = conn.prepareStatement(
                    converted.sql(),
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY
            );
            ps.setFetchSize(100); // Stream in batches

            setParameters(ps, converted.parameters());
            ResultSet rs = ps.executeQuery();

            Iterator<T> iterator = new Iterator<>() {
                private boolean hasNext;
                private boolean nextChecked = false;

                @Override
                public boolean hasNext() {
                    if (!nextChecked) {
                        try {
                            hasNext = rs.next();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        nextChecked = true;
                    }
                    return hasNext;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    nextChecked = false;
                    try {
                        return mapper.map(rs);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
            return StreamSupport.stream(spliterator, false)
                    .onClose(() -> {
                        try {
                            rs.close();
                            ps.close();
                            conn.close();
                        } catch (SQLException ignored) {
                        }
                    });
        } catch (SQLException e) {
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
        }
    }

    private void setParameters(PreparedStatement ps, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            ps.setObject(i + 1, parameters[i]);
        }
    }
}
