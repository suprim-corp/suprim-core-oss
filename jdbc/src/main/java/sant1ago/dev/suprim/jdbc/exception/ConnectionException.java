package sant1ago.dev.suprim.jdbc.exception;

import sant1ago.dev.suprim.jdbc.SuprimException;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Exception thrown when connection acquisition or management fails.
 *
 * <p>Common causes:
 * <ul>
 *   <li>Connection pool exhausted</li>
 *   <li>Database server unavailable</li>
 *   <li>Network connectivity issues</li>
 *   <li>Authentication failures</li>
 *   <li>Connection timeout</li>
 * </ul>
 *
 * <p>This exception is typically retryable after a delay.
 */
public class ConnectionException extends SuprimException {

    private final ConnectionFailureType failureType;

    protected ConnectionException(Builder builder) {
        super(builder);
        this.failureType = builder.failureType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ConnectionException poolExhausted() {
        return builder()
                .message("Connection pool exhausted - no available connections")
                .failureType(ConnectionFailureType.POOL_EXHAUSTED)
                .build();
    }

    public static ConnectionException timeout(SQLException cause) {
        return builder()
                .message("Connection timeout")
                .failureType(ConnectionFailureType.TIMEOUT)
                .cause(cause)
                .build();
    }

    public static ConnectionException unavailable(SQLException cause) {
        return builder()
                .message("Database server unavailable")
                .failureType(ConnectionFailureType.SERVER_UNAVAILABLE)
                .cause(cause)
                .build();
    }

    public static ConnectionException authenticationFailed(SQLException cause) {
        return builder()
                .message("Database authentication failed")
                .failureType(ConnectionFailureType.AUTHENTICATION_FAILED)
                .cause(cause)
                .build();
    }

    public static ConnectionException closed() {
        return builder()
                .message("Connection is closed")
                .failureType(ConnectionFailureType.CONNECTION_CLOSED)
                .build();
    }

    public static ConnectionException fromSQLException(SQLException cause) {
        String sqlState = cause.getSQLState();
        if (Objects.nonNull(sqlState) && sqlState.startsWith("08")) {
            return switch (sqlState) {
                case "08001" -> unavailable(cause);
                case "08004" -> authenticationFailed(cause);
                case "08006" -> builder()
                        .message("Connection failure during transaction")
                        .failureType(ConnectionFailureType.TRANSACTION_RESOLUTION_UNKNOWN)
                        .cause(cause)
                        .build();
                default -> builder()
                        .message("Connection error")
                        .failureType(ConnectionFailureType.UNKNOWN)
                        .cause(cause)
                        .build();
            };
        }
        return builder()
                .message("Connection error")
                .failureType(ConnectionFailureType.UNKNOWN)
                .cause(cause)
                .build();
    }

    /**
     * Get the type of connection failure.
     */
    public ConnectionFailureType getFailureType() {
        return failureType;
    }

    @Override
    public boolean isRetryable() {
        return failureType != ConnectionFailureType.AUTHENTICATION_FAILED;
    }

    /**
     * Types of connection failures.
     */
    public enum ConnectionFailureType {
        /** Connection pool has no available connections */
        POOL_EXHAUSTED,
        /** Connection attempt timed out */
        TIMEOUT,
        /** Database server is not reachable */
        SERVER_UNAVAILABLE,
        /** Authentication credentials rejected */
        AUTHENTICATION_FAILED,
        /** Connection was closed unexpectedly */
        CONNECTION_CLOSED,
        /** Connection failed during transaction - outcome unknown */
        TRANSACTION_RESOLUTION_UNKNOWN,
        /** Unknown connection error */
        UNKNOWN
    }

    public static class Builder extends SuprimException.Builder {
        private ConnectionFailureType failureType = ConnectionFailureType.UNKNOWN;

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

        public Builder failureType(ConnectionFailureType failureType) {
            this.failureType = failureType;
            return this;
        }

        @Override
        public ConnectionException build() {
            this.category(ErrorCategory.CONNECTION);
            return new ConnectionException(this);
        }
    }
}
