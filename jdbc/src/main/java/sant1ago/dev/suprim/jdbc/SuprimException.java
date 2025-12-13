package sant1ago.dev.suprim.jdbc;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Base runtime exception for all Suprim JDBC errors.
 * Provides rich context about database operations that failed.
 *
 * <p>Exception hierarchy:
 * <pre>
 * SuprimException (base)
 * ├── ConnectionException      - Connection pool/acquisition failures
 * ├── QueryException           - SELECT query execution failures
 * │   ├── NoResultException    - Expected result but got none
 * │   └── NonUniqueResultException - Expected single result but got multiple
 * ├── ExecutionException       - INSERT/UPDATE/DELETE failures
 * │   ├── ConstraintViolationException - Unique/FK/Check constraint violations
 * │   │   ├── UniqueConstraintException
 * │   │   ├── ForeignKeyException
 * │   │   └── CheckConstraintException
 * │   ├── DataIntegrityException - Data type/format issues
 * │   └── DeadlockException    - Deadlock detected
 * ├── TransactionException     - Transaction management failures
 * │   ├── TransactionRollbackException
 * │   └── SavepointException
 * └── MappingException         - Entity mapping failures
 * </pre>
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     executor.execute(insertQuery);
 * } catch (UniqueConstraintException e) {
 *     // Handle duplicate key
 *     log.warn("Duplicate entry: {}", e.getConstraintName());
 * } catch (ForeignKeyException e) {
 *     // Handle missing reference
 *     log.warn("Referenced record not found: {}", e.getConstraintName());
 * } catch (SuprimException e) {
 *     // Handle other database errors
 *     log.error("Database error: {} [SQL State: {}]", e.getMessage(), e.getSqlState());
 * }
 * }</pre>
 */
public class SuprimException extends RuntimeException {

    private final String sql;
    private final Object[] parameters;
    private final String sqlState;
    private final int vendorCode;
    private final ErrorCategory category;

    protected SuprimException(Builder builder) {
        super(builder.buildMessage(), builder.cause);
        this.sql = builder.sql;
        this.parameters = builder.parameters;
        this.sqlState = builder.sqlState;
        this.vendorCode = builder.vendorCode;
        this.category = builder.category;
    }

    public SuprimException(String message) {
        this(builder().message(message));
    }

    public SuprimException(String message, Throwable cause) {
        this(builder().message(message).cause(cause));
    }

    public SuprimException(String message, String sql, SQLException cause) {
        this(builder()
                .message(message)
                .sql(sql)
                .cause(cause)
                .sqlState(cause.getSQLState())
                .vendorCode(cause.getErrorCode())
                .category(ErrorCategory.fromSqlState(cause.getSQLState())));
    }

    /**
     * Create a builder for constructing SuprimException with full context.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the SQL statement that caused the exception, if available.
     */
    public String getSql() {
        return sql;
    }

    /**
     * Get the parameter values used in the query, if available.
     */
    public Object[] getParameters() {
        return parameters;
    }

    /**
     * Get the SQL state code (XOPEN or SQL:2003 standard).
     * @see <a href="https://en.wikipedia.org/wiki/SQLSTATE">SQLSTATE</a>
     */
    public String getSqlState() {
        return sqlState;
    }

    /**
     * Get the vendor-specific error code.
     */
    public int getVendorCode() {
        return vendorCode;
    }

    /**
     * Get the error category for programmatic handling.
     */
    public ErrorCategory getCategory() {
        return category;
    }

    /**
     * Get the underlying SQLException, if this exception wraps one.
     */
    public SQLException getSQLException() {
        Throwable cause = getCause();
        return cause instanceof SQLException ? (SQLException) cause : null;
    }

    /**
     * Check if this exception is due to a constraint violation.
     */
    public boolean isConstraintViolation() {
        return category == ErrorCategory.INTEGRITY_CONSTRAINT;
    }

    /**
     * Check if this exception is due to a connection issue.
     */
    public boolean isConnectionError() {
        return category == ErrorCategory.CONNECTION;
    }

    /**
     * Check if this exception is due to a transaction issue.
     */
    public boolean isTransactionError() {
        return category == ErrorCategory.TRANSACTION_ROLLBACK
                || category == ErrorCategory.INVALID_TRANSACTION_STATE;
    }

    /**
     * Check if this exception is retryable (deadlock, timeout, connection lost).
     */
    public boolean isRetryable() {
        return category == ErrorCategory.TRANSACTION_ROLLBACK
                || category == ErrorCategory.CONNECTION
                || (Objects.nonNull(sqlState) && sqlState.startsWith("40")); // Transaction rollback class
    }

    /**
     * Error categories based on SQL state class codes.
     */
    public enum ErrorCategory {
        /** Successful completion (00) */
        SUCCESS,
        /** Warning (01) */
        WARNING,
        /** No data (02) */
        NO_DATA,
        /** Connection exception (08) */
        CONNECTION,
        /** Feature not supported (0A) */
        FEATURE_NOT_SUPPORTED,
        /** Invalid transaction state (25) */
        INVALID_TRANSACTION_STATE,
        /** Invalid authorization (28) */
        INVALID_AUTHORIZATION,
        /** Syntax error or access rule violation (42) */
        SYNTAX_ERROR,
        /** Integrity constraint violation (23) */
        INTEGRITY_CONSTRAINT,
        /** Data exception (22) */
        DATA_EXCEPTION,
        /** Transaction rollback (40) */
        TRANSACTION_ROLLBACK,
        /** Insufficient resources (53) */
        INSUFFICIENT_RESOURCES,
        /** Program limit exceeded (54) */
        PROGRAM_LIMIT_EXCEEDED,
        /** Object not in prerequisite state (55) */
        OBJECT_NOT_IN_STATE,
        /** Query canceled (57) */
        QUERY_CANCELED,
        /** System error (58) */
        SYSTEM_ERROR,
        /** Unknown/unclassified */
        UNKNOWN;

        /**
         * Map SQL state code to error category.
         */
        public static ErrorCategory fromSqlState(String sqlState) {
            if (Objects.isNull(sqlState) || sqlState.length() < 2) {
                return UNKNOWN;
            }
            String stateClass = sqlState.substring(0, 2);
            return switch (stateClass) {
                case "00" -> SUCCESS;
                case "01" -> WARNING;
                case "02" -> NO_DATA;
                case "08" -> CONNECTION;
                case "0A" -> FEATURE_NOT_SUPPORTED;
                case "22" -> DATA_EXCEPTION;
                case "23" -> INTEGRITY_CONSTRAINT;
                case "25" -> INVALID_TRANSACTION_STATE;
                case "28" -> INVALID_AUTHORIZATION;
                case "40" -> TRANSACTION_ROLLBACK;
                case "42" -> SYNTAX_ERROR;
                case "53" -> INSUFFICIENT_RESOURCES;
                case "54" -> PROGRAM_LIMIT_EXCEEDED;
                case "55" -> OBJECT_NOT_IN_STATE;
                case "57" -> QUERY_CANCELED;
                case "58" -> SYSTEM_ERROR;
                default -> UNKNOWN;
            };
        }
    }

    /**
     * Builder for constructing SuprimException with rich context.
     */
    public static class Builder {
        protected String message;
        protected String sql;
        protected Object[] parameters;
        protected Throwable cause;
        protected String sqlState;
        protected int vendorCode;
        protected ErrorCategory category = ErrorCategory.UNKNOWN;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder parameters(Object[] parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            if (cause instanceof SQLException sqlEx) {
                if (Objects.isNull(this.sqlState)) {
                    this.sqlState = sqlEx.getSQLState();
                }
                if (this.vendorCode == 0) {
                    this.vendorCode = sqlEx.getErrorCode();
                }
                if (this.category == ErrorCategory.UNKNOWN) {
                    this.category = ErrorCategory.fromSqlState(sqlEx.getSQLState());
                }
            }
            return this;
        }

        public Builder sqlState(String sqlState) {
            this.sqlState = sqlState;
            return this;
        }

        public Builder vendorCode(int vendorCode) {
            this.vendorCode = vendorCode;
            return this;
        }

        public Builder category(ErrorCategory category) {
            this.category = category;
            return this;
        }

        protected String buildMessage() {
            StringBuilder sb = new StringBuilder();
            if (Objects.nonNull(message)) {
                sb.append(message);
            }
            if (Objects.nonNull(sql)) {
                sb.append(" [SQL: ").append(truncateSql(sql)).append("]");
            }
            if (Objects.nonNull(sqlState)) {
                sb.append(" [State: ").append(sqlState).append("]");
            }
            if (vendorCode != 0) {
                sb.append(" [Code: ").append(vendorCode).append("]");
            }
            return sb.toString();
        }

        private String truncateSql(String sql) {
            if (sql.length() > 200) {
                return sql.substring(0, 200) + "...";
            }
            return sql;
        }

        public SuprimException build() {
            return new SuprimException(this);
        }
    }
}
