package sant1ago.dev.suprim.jdbc.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable event fired during query lifecycle.
 *
 * <p>Events are created before query execution and completed after.
 * Use {@link #isSuccess()} to check completion status.</p>
 *
 * <pre>{@code
 * executor.addQueryListener(event -> {
 *     if (event.isSuccess()) {
 *         log.info("Query took {}ms", event.durationMs());
 *     }
 * });
 * }</pre>
 *
 * @param queryId        Unique ID for correlation/tracing
 * @param sql            The SQL statement
 * @param parameters     Bound parameters
 * @param type           Query type (SELECT/INSERT/UPDATE/DELETE/OTHER)
 * @param connectionName DataSource/connection name
 * @param startTime      When query started
 * @param durationNanos  Execution time in nanoseconds (null before execution)
 * @param affectedRows   Rows affected for INSERT/UPDATE/DELETE (null for SELECT)
 * @param error          Exception if query failed (null on success)
 * @see QueryListener
 * @see QueryType
 */
public record QueryEvent(
        String queryId,
        String sql,
        Object[] parameters,
        QueryType type,
        String connectionName,
        Instant startTime,
        Long durationNanos,
        Integer affectedRows,
        Throwable error
) {

    /**
     * Create a "before execution" event.
     *
     * @param sql            the SQL statement
     * @param parameters     bound parameters
     * @param connectionName connection/datasource name
     * @return new QueryEvent for before-execution phase
     */
    public static QueryEvent before(String sql, Object[] parameters, String connectionName) {
        return new QueryEvent(
                UUID.randomUUID().toString(),
                sql,
                parameters,
                QueryType.fromSql(sql),
                connectionName,
                Instant.now(),
                null,
                null,
                null
        );
    }

    /**
     * Create completed event from this before-event.
     *
     * @param durationNanos execution time in nanoseconds
     * @param affectedRows  rows affected (or result count for SELECT)
     * @return new QueryEvent representing successful completion
     */
    public QueryEvent completed(long durationNanos, Integer affectedRows) {
        return new QueryEvent(
                queryId,
                sql,
                parameters,
                type,
                connectionName,
                startTime,
                durationNanos,
                affectedRows,
                null
        );
    }

    /**
     * Create failed event from this before-event.
     *
     * @param durationNanos execution time until failure
     * @param error         the exception that caused failure
     * @return new QueryEvent representing failure
     */
    public QueryEvent failed(long durationNanos, Throwable error) {
        return new QueryEvent(
                queryId,
                sql,
                parameters,
                type,
                connectionName,
                startTime,
                durationNanos,
                null,
                error
        );
    }

    /**
     * Get execution duration in milliseconds.
     *
     * @return duration in ms, or 0 if not yet executed
     */
    public double durationMs() {
        return Objects.nonNull(durationNanos) ? durationNanos / 1_000_000.0 : 0;
    }

    /**
     * Check if query completed successfully.
     *
     * @return true if executed without error
     */
    public boolean isSuccess() {
        return Objects.isNull(error) && Objects.nonNull(durationNanos);
    }

    /**
     * Check if query failed.
     *
     * @return true if execution resulted in error
     */
    public boolean isFailed() {
        return Objects.nonNull(error);
    }
}
