package sant1ago.dev.suprim.jdbc.event;

import java.util.function.Consumer;

/**
 * Listener for query execution events.
 *
 * <p>Implement this interface for full control over query lifecycle,
 * or use static factory methods for common patterns:</p>
 *
 * <pre>{@code
 * // Simple logging
 * QueryListener.onQuery(e -> log.info(e.sql()));
 *
 * // Slow query detection
 * QueryListener.onSlowQuery(100, e -> log.warn("Slow: " + e.sql()));
 *
 * // Error handling
 * QueryListener.onQueryError(e -> metrics.increment("errors"));
 *
 * // Full implementation
 * executor.addQueryListener(new QueryListener() {
 *     public void beforeQuery(QueryEvent event) {
 *         MDC.put("query_id", event.queryId());
 *     }
 *     public void afterQuery(QueryEvent event) {
 *         metrics.record(event.durationMs());
 *         MDC.remove("query_id");
 *     }
 *     public void onError(QueryEvent event) {
 *         log.error("Query failed: {}", event.error().getMessage());
 *     }
 * });
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> Implementations must be thread-safe
 * as events may fire from multiple threads concurrently.</p>
 *
 * @see QueryEvent
 */
public interface QueryListener {

    /**
     * Called before query execution.
     * Default implementation does nothing.
     *
     * @param event the query event (durationNanos will be null)
     */
    default void beforeQuery(QueryEvent event) {
    }

    /**
     * Called after successful query execution.
     * Default implementation does nothing.
     *
     * @param event the query event with timing information
     */
    default void afterQuery(QueryEvent event) {
    }

    /**
     * Called when query execution fails.
     * Default implementation does nothing.
     *
     * @param event the query event with error information
     */
    default void onError(QueryEvent event) {
    }

    // ============ Functional Factories ============

    /**
     * Create listener that fires after every successful query.
     *
     * @param handler the handler to invoke
     * @return new QueryListener
     */
    static QueryListener onQuery(Consumer<QueryEvent> handler) {
        return new QueryListener() {
            @Override
            public void afterQuery(QueryEvent event) {
                handler.accept(event);
            }
        };
    }

    /**
     * Create listener that fires only for slow queries.
     *
     * @param thresholdMs minimum duration in milliseconds to trigger
     * @param handler     the handler to invoke for slow queries
     * @return new QueryListener
     */
    static QueryListener onSlowQuery(long thresholdMs, Consumer<QueryEvent> handler) {
        return new QueryListener() {
            @Override
            public void afterQuery(QueryEvent event) {
                if (event.durationMs() >= thresholdMs) {
                    handler.accept(event);
                }
            }
        };
    }

    /**
     * Create listener that fires on query errors.
     *
     * @param handler the handler to invoke on errors
     * @return new QueryListener
     */
    static QueryListener onQueryError(Consumer<QueryEvent> handler) {
        return new QueryListener() {
            @Override
            public void onError(QueryEvent event) {
                handler.accept(event);
            }
        };
    }

    /**
     * Create listener for specific query types only.
     *
     * @param type    the query type to filter for
     * @param handler the handler to invoke
     * @return new QueryListener
     */
    static QueryListener forType(QueryType type, Consumer<QueryEvent> handler) {
        return new QueryListener() {
            @Override
            public void afterQuery(QueryEvent event) {
                if (event.type() == type) {
                    handler.accept(event);
                }
            }
        };
    }

    /**
     * Create listener that fires before every query.
     *
     * @param handler the handler to invoke before execution
     * @return new QueryListener
     */
    static QueryListener beforeQuery(Consumer<QueryEvent> handler) {
        return new QueryListener() {
            @Override
            public void beforeQuery(QueryEvent event) {
                handler.accept(event);
            }
        };
    }
}
