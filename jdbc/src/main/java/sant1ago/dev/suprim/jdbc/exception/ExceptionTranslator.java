package sant1ago.dev.suprim.jdbc.exception;

import sant1ago.dev.suprim.jdbc.SuprimException;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Translates SQLException to appropriate Suprim exception types.
 *
 * <p>Uses SQL state codes (XOPEN/SQL:2003 standard) and vendor-specific
 * error codes to determine the most specific exception type.
 *
 * <p>SQL State Class Codes:
 * <ul>
 *   <li>00 - Successful completion</li>
 *   <li>01 - Warning</li>
 *   <li>02 - No data</li>
 *   <li>08 - Connection exception</li>
 *   <li>0A - Feature not supported</li>
 *   <li>22 - Data exception</li>
 *   <li>23 - Integrity constraint violation</li>
 *   <li>25 - Invalid transaction state</li>
 *   <li>28 - Invalid authorization specification</li>
 *   <li>40 - Transaction rollback</li>
 *   <li>42 - Syntax error or access rule violation</li>
 *   <li>53 - Insufficient resources</li>
 *   <li>57 - Operator intervention (query canceled)</li>
 *   <li>58 - System error</li>
 * </ul>
 */
public final class ExceptionTranslator {

    private ExceptionTranslator() {
        // Utility class
    }

    /**
     * Translate a SQLException to the appropriate Suprim exception.
     *
     * @param sql   the SQL statement that caused the error
     * @param cause the original SQLException
     * @return translated SuprimException subclass
     */
    public static SuprimException translate(String sql, SQLException cause) {
        return translate(sql, null, cause);
    }

    /**
     * Translate a SQLException to the appropriate Suprim exception.
     *
     * @param sql        the SQL statement that caused the error
     * @param parameters the parameters used in the query
     * @param cause      the original SQLException
     * @return translated SuprimException subclass
     */
    public static SuprimException translate(String sql, Object[] parameters, SQLException cause) {
        String sqlState = cause.getSQLState();
        int errorCode = cause.getErrorCode();
        String message = cause.getMessage();

        // First, try SQL state based translation
        if (Objects.nonNull(sqlState)) {
            SuprimException translated = translateBySqlState(sql, parameters, sqlState, cause);
            if (Objects.nonNull(translated)) {
                return translated;
            }
        }

        // Fall back to message-based detection
        if (Objects.nonNull(message)) {
            SuprimException translated = translateByMessage(sql, parameters, message, cause);
            if (Objects.nonNull(translated)) {
                return translated;
            }
        }

        // Default to generic exception
        return SuprimException.builder()
                .message("Database error")
                .sql(sql)
                .parameters(parameters)
                .cause(cause)
                .build();
    }

    private static SuprimException translateBySqlState(String sql, Object[] parameters,
                                                        String sqlState, SQLException cause) {
        // Handle empty or too-short SQL states gracefully
        if (Objects.isNull(sqlState) || sqlState.length() < 2) {
            return null;
        }

        String stateClass = sqlState.substring(0, 2);

        return switch (stateClass) {
            case "08" -> translateConnectionException(sql, cause);
            case "22" -> DataIntegrityException.fromSQLException(sql, cause);
            case "23" -> ConstraintViolationException.fromSQLException(sql, cause);
            case "25" -> TransactionException.fromSQLException(cause);
            case "40" -> translateTransactionRollback(sql, sqlState, cause);
            case "42" -> QueryException.executionFailed(sql, parameters, cause);
            case "53" -> translateInsufficientResources(sql, cause);
            case "57" -> QueryException.cancelled(sql);
            default -> null;
        };
    }

    private static SuprimException translateConnectionException(String sql, SQLException cause) {
        return ConnectionException.fromSQLException(cause);
    }

    private static SuprimException translateTransactionRollback(String sql, String sqlState,
                                                                 SQLException cause) {
        // 40001 = Serialization failure (deadlock)
        // 40002 = Integrity constraint violation during commit
        // 40003 = Statement completion unknown
        // 40P01 = PostgreSQL deadlock detected
        return switch (sqlState) {
            case "40001", "40P01" -> DeadlockException.fromSQLException(sql, cause);
            case "40002" -> ConstraintViolationException.fromSQLException(sql, cause);
            default -> TransactionException.fromSQLException(cause);
        };
    }

    private static SuprimException translateInsufficientResources(String sql, SQLException cause) {
        // 53000 = Insufficient resources
        // 53100 = Disk full
        // 53200 = Out of memory
        // 53300 = Too many connections
        String sqlState = cause.getSQLState();
        if ("53300".equals(sqlState)) {
            return ConnectionException.poolExhausted();
        }
        return ExecutionException.failed(sql, cause);
    }

    private static SuprimException translateByMessage(String sql, Object[] parameters,
                                                       String message, SQLException cause) {
        String lowerMessage = message.toLowerCase();

        // Constraint violations
        if (lowerMessage.contains("unique") || lowerMessage.contains("duplicate")
                || lowerMessage.contains("primary key violation")) {
            return UniqueConstraintException.fromSQLException(sql, cause);
        }
        if (lowerMessage.contains("foreign key") || lowerMessage.contains("referential")) {
            return ForeignKeyException.fromSQLException(sql, cause);
        }
        if (lowerMessage.contains("check constraint")) {
            return CheckConstraintException.fromSQLException(sql, cause);
        }
        if (lowerMessage.contains("not null") || lowerMessage.contains("cannot be null")) {
            return NotNullException.fromSQLException(sql, cause);
        }

        // Connection issues
        if (lowerMessage.contains("connection") && (lowerMessage.contains("closed")
                || lowerMessage.contains("refused") || lowerMessage.contains("timeout"))) {
            return ConnectionException.fromSQLException(cause);
        }

        // Deadlock
        if (lowerMessage.contains("deadlock")) {
            return DeadlockException.fromSQLException(sql, cause);
        }

        // Lock timeout
        if (lowerMessage.contains("lock") && lowerMessage.contains("timeout")) {
            return ExecutionException.builder()
                    .message("Lock acquisition timeout")
                    .sql(sql)
                    .parameters(parameters)
                    .cause(cause)
                    .build();
        }

        return null;
    }

    /**
     * Translate exception specifically for query operations.
     */
    public static SuprimException translateQuery(String sql, Object[] parameters, SQLException cause) {
        SuprimException translated = translate(sql, parameters, cause);

        // Wrap in QueryException if not already a more specific type
        if (translated.getClass() == SuprimException.class) {
            return QueryException.executionFailed(sql, parameters, cause);
        }

        return translated;
    }

    /**
     * Translate exception specifically for execute operations (INSERT/UPDATE/DELETE).
     */
    public static SuprimException translateExecution(String sql, Object[] parameters, SQLException cause) {
        SuprimException translated = translate(sql, parameters, cause);

        // Wrap in ExecutionException if not already a more specific type
        if (translated.getClass() == SuprimException.class) {
            return ExecutionException.failed(sql, parameters, cause);
        }

        return translated;
    }
}
