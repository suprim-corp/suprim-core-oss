package sant1ago.dev.suprim.jdbc.exception;

import sant1ago.dev.suprim.jdbc.SuprimException;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Exception thrown when transaction management operations fail.
 *
 * <p>Common causes:
 * <ul>
 *   <li>Commit failed</li>
 *   <li>Rollback failed</li>
 *   <li>Invalid transaction state</li>
 *   <li>Transaction timeout</li>
 * </ul>
 */
public class TransactionException extends SuprimException {

    private final TransactionFailureType failureType;

    protected TransactionException(Builder builder) {
        super(builder);
        this.failureType = builder.failureType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TransactionException commitFailed(SQLException cause) {
        return builder()
                .message("Transaction commit failed")
                .failureType(TransactionFailureType.COMMIT_FAILED)
                .cause(cause)
                .build();
    }

    public static TransactionException rollbackFailed(SQLException cause) {
        return builder()
                .message("Transaction rollback failed")
                .failureType(TransactionFailureType.ROLLBACK_FAILED)
                .cause(cause)
                .build();
    }

    public static TransactionException timeout(SQLException cause) {
        return builder()
                .message("Transaction timed out")
                .failureType(TransactionFailureType.TIMEOUT)
                .cause(cause)
                .build();
    }

    public static TransactionException invalidState(String message) {
        return builder()
                .message(message)
                .failureType(TransactionFailureType.INVALID_STATE)
                .build();
    }

    public static TransactionException alreadyActive() {
        return builder()
                .message("Transaction already active")
                .failureType(TransactionFailureType.ALREADY_ACTIVE)
                .build();
    }

    public static TransactionException notActive() {
        return builder()
                .message("No active transaction")
                .failureType(TransactionFailureType.NOT_ACTIVE)
                .build();
    }

    public static TransactionException fromSQLException(SQLException cause) {
        String sqlState = cause.getSQLState();
        if (Objects.nonNull(sqlState) && sqlState.startsWith("40")) {
            // Transaction rollback class
            return builder()
                    .message("Transaction was rolled back")
                    .failureType(TransactionFailureType.ROLLED_BACK)
                    .cause(cause)
                    .build();
        }
        return builder()
                .message("Transaction error")
                .failureType(TransactionFailureType.UNKNOWN)
                .cause(cause)
                .build();
    }

    /**
     * Get the type of transaction failure.
     */
    public TransactionFailureType getFailureType() {
        return failureType;
    }

    /**
     * Types of transaction failures.
     */
    public enum TransactionFailureType {
        /** Commit operation failed */
        COMMIT_FAILED,
        /** Rollback operation failed */
        ROLLBACK_FAILED,
        /** Transaction timed out */
        TIMEOUT,
        /** Transaction in invalid state for operation */
        INVALID_STATE,
        /** Transaction already active when trying to start new one */
        ALREADY_ACTIVE,
        /** No active transaction when one was expected */
        NOT_ACTIVE,
        /** Transaction was rolled back (possibly by database) */
        ROLLED_BACK,
        /** Unknown transaction error */
        UNKNOWN
    }

    public static class Builder extends SuprimException.Builder {
        private TransactionFailureType failureType = TransactionFailureType.UNKNOWN;

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

        public Builder failureType(TransactionFailureType failureType) {
            this.failureType = failureType;
            return this;
        }

        @Override
        public TransactionException build() {
            return new TransactionException(this);
        }
    }
}
