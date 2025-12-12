package sant1ago.dev.suprim.jdbc.event;

/**
 * Type of transaction lifecycle event.
 */
public enum TransactionEventType {
    /** Transaction started */
    BEGIN,
    /** Transaction committed successfully */
    COMMIT,
    /** Transaction rolled back */
    ROLLBACK,
    /** Savepoint created */
    SAVEPOINT_CREATED,
    /** Savepoint released */
    SAVEPOINT_RELEASED,
    /** Rolled back to savepoint */
    SAVEPOINT_ROLLBACK
}
