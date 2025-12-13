package sant1ago.dev.suprim.jdbc.event;

import java.util.function.Consumer;

/**
 * Listener for transaction lifecycle events.
 *
 * <p>Implement this interface for full control over transaction events,
 * or use static factory methods for common patterns:</p>
 *
 * <pre>{@code
 * // Track commits
 * TransactionListener.onCommit(e -> log.info("Committed in {}ms", e.durationMs()));
 *
 * // Track rollbacks
 * TransactionListener.onRollback(e -> log.warn("Rolled back: {}", e.transactionId()));
 *
 * // Track all completions
 * TransactionListener.onComplete(e -> metrics.record(e.durationMs()));
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> Implementations must be thread-safe
 * as events may fire from multiple threads concurrently.</p>
 *
 * @see TransactionEvent
 */
public interface TransactionListener {

    /**
     * Called when transaction begins.
     *
     * @param event the transaction event
     */
    default void onBegin(TransactionEvent event) {
    }

    /**
     * Called when transaction commits successfully.
     *
     * @param event the transaction event with duration
     */
    default void onCommit(TransactionEvent event) {
    }

    /**
     * Called when transaction rolls back.
     *
     * @param event the transaction event with duration
     */
    default void onRollback(TransactionEvent event) {
    }

    /**
     * Called when a savepoint is created.
     *
     * @param event the transaction event with savepoint name
     */
    default void onSavepointCreated(TransactionEvent event) {
    }

    /**
     * Called when a savepoint is released.
     *
     * @param event the transaction event with savepoint name
     */
    default void onSavepointReleased(TransactionEvent event) {
    }

    /**
     * Called when transaction rolls back to a savepoint.
     *
     * @param event the transaction event with savepoint name
     */
    default void onSavepointRollback(TransactionEvent event) {
    }

    // ============ Functional Factories ============

    /**
     * Create listener that fires on successful commit.
     *
     * @param handler the handler to invoke
     * @return new TransactionListener
     */
    static TransactionListener onCommit(Consumer<TransactionEvent> handler) {
        return new TransactionListener() {
            @Override
            public void onCommit(TransactionEvent event) {
                handler.accept(event);
            }
        };
    }

    /**
     * Create listener that fires on rollback.
     *
     * @param handler the handler to invoke
     * @return new TransactionListener
     */
    static TransactionListener onRollback(Consumer<TransactionEvent> handler) {
        return new TransactionListener() {
            @Override
            public void onRollback(TransactionEvent event) {
                handler.accept(event);
            }
        };
    }

    /**
     * Create listener that fires on both commit and rollback.
     *
     * @param handler the handler to invoke
     * @return new TransactionListener
     */
    static TransactionListener onComplete(Consumer<TransactionEvent> handler) {
        return new TransactionListener() {
            @Override
            public void onCommit(TransactionEvent event) {
                handler.accept(event);
            }

            @Override
            public void onRollback(TransactionEvent event) {
                handler.accept(event);
            }
        };
    }

    /**
     * Create listener that fires when transaction begins.
     *
     * @param handler the handler to invoke
     * @return new TransactionListener
     */
    static TransactionListener onBegin(Consumer<TransactionEvent> handler) {
        return new TransactionListener() {
            @Override
            public void onBegin(TransactionEvent event) {
                handler.accept(event);
            }
        };
    }
}
