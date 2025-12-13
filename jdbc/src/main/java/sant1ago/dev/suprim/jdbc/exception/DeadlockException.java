package sant1ago.dev.suprim.jdbc.exception;

import java.sql.SQLException;

/**
 * Exception thrown when a database deadlock is detected.
 *
 * <p>A deadlock occurs when two or more transactions are waiting for each other
 * to release locks, creating a circular dependency.
 *
 * <p>This exception is typically retryable - the operation can be attempted again
 * after the deadlock victim transaction is rolled back.
 *
 * <p>Usage:
 * <pre>{@code
 * int maxRetries = 3;
 * for (int attempt = 0; attempt < maxRetries; attempt++) {
 *     try {
 *         executor.transaction(tx -> {
 *             tx.execute(updateBalanceQuery);
 *         });
 *         break; // Success
 *     } catch (DeadlockException e) {
 *         if (attempt == maxRetries - 1) {
 *             throw e; // Give up
 *         }
 *         Thread.sleep(100 * (attempt + 1)); // Backoff
 *     }
 * }
 * }</pre>
 */
public class DeadlockException extends ExecutionException {

    protected DeadlockException(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DeadlockException fromSQLException(String sql, SQLException cause) {
        return builder()
                .message("Deadlock detected - transaction was rolled back")
                .sql(sql)
                .cause(cause)
                .build();
    }

    @Override
    public boolean isRetryable() {
        return true;
    }

    public static class Builder extends ExecutionException.Builder {
        @Override
        public Builder message(String message) {
            super.message(message);
            return this;
        }

        @Override
        public Builder sql(String sql) {
            super.sql(sql);
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            super.cause(cause);
            return this;
        }

        @Override
        public DeadlockException build() {
            this.category(ErrorCategory.TRANSACTION_ROLLBACK);
            return new DeadlockException(this);
        }
    }
}
