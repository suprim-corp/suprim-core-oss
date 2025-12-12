package sant1ago.dev.suprim.jdbc.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable event fired during transaction lifecycle.
 *
 * <p>Transaction events track begin, commit, rollback, and savepoint operations.</p>
 *
 * <pre>{@code
 * executor.addTransactionListener(new TransactionListener() {
 *     public void onCommit(TransactionEvent event) {
 *         log.info("Transaction {} committed in {}ms",
 *             event.transactionId(), event.durationMs());
 *     }
 * });
 * }</pre>
 *
 * @param transactionId  Unique ID for this transaction
 * @param type           Event type (BEGIN/COMMIT/ROLLBACK/SAVEPOINT_*)
 * @param connectionName DataSource/connection name
 * @param timestamp      When event occurred
 * @param savepointName  Name of savepoint (for savepoint events, null otherwise)
 * @param durationNanos  Total transaction time (for COMMIT/ROLLBACK, null otherwise)
 * @see TransactionListener
 * @see TransactionEventType
 */
public record TransactionEvent(
        String transactionId,
        TransactionEventType type,
        String connectionName,
        Instant timestamp,
        String savepointName,
        Long durationNanos
) {

    /**
     * Create a BEGIN event for a new transaction.
     *
     * @param connectionName connection/datasource name
     * @return new TransactionEvent for transaction start
     */
    public static TransactionEvent begin(String connectionName) {
        return new TransactionEvent(
                UUID.randomUUID().toString(),
                TransactionEventType.BEGIN,
                connectionName,
                Instant.now(),
                null,
                null
        );
    }

    /**
     * Create COMMIT event from this transaction's BEGIN event.
     *
     * @param durationNanos total transaction duration in nanoseconds
     * @return new TransactionEvent for successful commit
     */
    public TransactionEvent commit(long durationNanos) {
        return new TransactionEvent(
                transactionId,
                TransactionEventType.COMMIT,
                connectionName,
                Instant.now(),
                null,
                durationNanos
        );
    }

    /**
     * Create ROLLBACK event from this transaction's BEGIN event.
     *
     * @param durationNanos total transaction duration in nanoseconds
     * @return new TransactionEvent for rollback
     */
    public TransactionEvent rollback(long durationNanos) {
        return new TransactionEvent(
                transactionId,
                TransactionEventType.ROLLBACK,
                connectionName,
                Instant.now(),
                null,
                durationNanos
        );
    }

    /**
     * Create SAVEPOINT_CREATED event.
     *
     * @param savepointName name of the savepoint
     * @return new TransactionEvent for savepoint creation
     */
    public TransactionEvent savepointCreated(String savepointName) {
        return new TransactionEvent(
                transactionId,
                TransactionEventType.SAVEPOINT_CREATED,
                connectionName,
                Instant.now(),
                savepointName,
                null
        );
    }

    /**
     * Create SAVEPOINT_RELEASED event.
     *
     * @param savepointName name of the savepoint
     * @return new TransactionEvent for savepoint release
     */
    public TransactionEvent savepointReleased(String savepointName) {
        return new TransactionEvent(
                transactionId,
                TransactionEventType.SAVEPOINT_RELEASED,
                connectionName,
                Instant.now(),
                savepointName,
                null
        );
    }

    /**
     * Create SAVEPOINT_ROLLBACK event.
     *
     * @param savepointName name of the savepoint
     * @return new TransactionEvent for savepoint rollback
     */
    public TransactionEvent savepointRollback(String savepointName) {
        return new TransactionEvent(
                transactionId,
                TransactionEventType.SAVEPOINT_ROLLBACK,
                connectionName,
                Instant.now(),
                savepointName,
                null
        );
    }

    /**
     * Get transaction duration in milliseconds.
     *
     * @return duration in ms, or 0 if not applicable
     */
    public double durationMs() {
        return Objects.nonNull(durationNanos) ? durationNanos / 1_000_000.0 : 0;
    }
}
