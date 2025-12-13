package sant1ago.dev.suprim.jdbc.exception;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Exception thrown when savepoint operations fail.
 *
 * <p>Common causes:
 * <ul>
 *   <li>Savepoint creation failed</li>
 *   <li>Invalid savepoint reference</li>
 *   <li>Rollback to savepoint failed</li>
 *   <li>Savepoint release failed</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * executor.transaction(tx -> {
 *     tx.execute(insertOrder);
 *
 *     Savepoint sp = tx.savepoint("before_items");
 *     try {
 *         tx.execute(insertItems);
 *     } catch (SuprimException e) {
 *         tx.rollbackTo(sp); // Partial rollback
 *         tx.execute(insertDefaultItems);
 *     }
 * });
 * }</pre>
 */
public class SavepointException extends TransactionException {

    private final String savepointName;
    private final SavepointOperation operation;

    protected SavepointException(Builder builder) {
        super(builder);
        this.savepointName = builder.savepointName;
        this.operation = builder.operation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SavepointException createFailed(String name, SQLException cause) {
        return builder()
                .message("Failed to create savepoint")
                .savepointName(name)
                .operation(SavepointOperation.CREATE)
                .cause(cause)
                .build();
    }

    public static SavepointException rollbackFailed(String name, SQLException cause) {
        return builder()
                .message("Failed to rollback to savepoint")
                .savepointName(name)
                .operation(SavepointOperation.ROLLBACK)
                .cause(cause)
                .build();
    }

    public static SavepointException releaseFailed(String name, SQLException cause) {
        return builder()
                .message("Failed to release savepoint")
                .savepointName(name)
                .operation(SavepointOperation.RELEASE)
                .cause(cause)
                .build();
    }

    public static SavepointException invalid(String name) {
        return builder()
                .message("Invalid savepoint")
                .savepointName(name)
                .operation(SavepointOperation.UNKNOWN)
                .build();
    }

    /**
     * Get the name of the savepoint involved.
     */
    public String getSavepointName() {
        return savepointName;
    }

    /**
     * Get the operation that failed.
     */
    public SavepointOperation getOperation() {
        return operation;
    }

    /**
     * Types of savepoint operations.
     */
    public enum SavepointOperation {
        /** Creating a new savepoint */
        CREATE,
        /** Rolling back to a savepoint */
        ROLLBACK,
        /** Releasing a savepoint */
        RELEASE,
        /** Unknown operation */
        UNKNOWN
    }

    public static class Builder extends TransactionException.Builder {
        private String savepointName;
        private SavepointOperation operation = SavepointOperation.UNKNOWN;

        @Override
        public Builder message(String message) {
            super.message(message);
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            super.cause(cause);
            return this;
        }

        public Builder savepointName(String savepointName) {
            this.savepointName = savepointName;
            return this;
        }

        public Builder operation(SavepointOperation operation) {
            this.operation = operation;
            return this;
        }

        @Override
        public SavepointException build() {
            if (Objects.nonNull(savepointName)) {
                message = message + ": " + savepointName;
            }
            return new SavepointException(this);
        }
    }
}
