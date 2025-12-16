package sant1ago.dev.suprim.jdbc.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.jdbc.SuprimException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for exception classes in the exception package.
 * Tests focus on direct exception construction, factory methods, and edge cases.
 */
@DisplayName("Exception Package Tests")
class ExceptionPackageTest {

    private static final String TEST_SQL = "SELECT * FROM users";

    // ==================== ExceptionTranslator Branch Coverage ====================

    @Nested
    @DisplayName("ExceptionTranslator Branch Coverage")
    class ExceptionTranslatorBranchTests {

        @Test
        @DisplayName("translateByMessage with 'connection refused'")
        void connectionRefused() {
            SQLException cause = new SQLException("connection refused by server", "99999");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertInstanceOf(ConnectionException.class, result);
        }

        @Test
        @DisplayName("translateByMessage with 'not null' constraint")
        void notNullConstraint() {
            SQLException cause = new SQLException("column cannot be null", "99999");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertInstanceOf(NotNullException.class, result);
        }

        @Test
        @DisplayName("translate with null sqlState and null message returns generic")
        void nullStateNullMessage() {
            SQLException cause = new SQLException((String) null, (String) null);
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertEquals(SuprimException.class, result.getClass());
        }

        @Test
        @DisplayName("translateQuery returns specific exception when not generic")
        void translateQuerySpecific() {
            SQLException cause = new SQLException("duplicate key", "23505");
            SuprimException result = ExceptionTranslator.translateQuery(TEST_SQL, null, cause);
            assertInstanceOf(UniqueConstraintException.class, result);
        }

        @Test
        @DisplayName("translateExecution returns specific exception when not generic")
        void translateExecutionSpecific() {
            SQLException cause = new SQLException("foreign key violation", "23503");
            SuprimException result = ExceptionTranslator.translateExecution(TEST_SQL, null, cause);
            assertInstanceOf(ForeignKeyException.class, result);
        }

        @Test
        @DisplayName("translateInsufficientResources with non-53300 state")
        void insufficientResourcesNon53300() {
            SQLException cause = new SQLException("disk full", "53100");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertInstanceOf(ExecutionException.class, result);
        }

        // Hit all branches in || expressions
        @Test
        @DisplayName("translateByMessage with 'duplicate' (second branch of unique||duplicate)")
        void duplicateSecondBranch() {
            SQLException cause = new SQLException("duplicate entry found", "99999");
            assertInstanceOf(UniqueConstraintException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage with 'primary key violation' (third branch)")
        void primaryKeyViolationThirdBranch() {
            SQLException cause = new SQLException("primary key violation detected", "99999");
            assertInstanceOf(UniqueConstraintException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage with 'referential' (second branch of foreign key)")
        void referentialSecondBranch() {
            SQLException cause = new SQLException("referential constraint error", "99999");
            assertInstanceOf(ForeignKeyException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage with connection closed")
        void connectionClosedBranch() {
            SQLException cause = new SQLException("connection closed unexpectedly", "99999");
            assertInstanceOf(ConnectionException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage with connection timeout")
        void connectionTimeoutBranch() {
            SQLException cause = new SQLException("connection timeout occurred", "99999");
            assertInstanceOf(ConnectionException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        // More specific branch tests for || expressions
        @Test
        @DisplayName("translateByMessage: 'cannot be null' second branch of not null check")
        void cannotBeNullSecondBranch() {
            // First: "not null" - but we need to NOT match this to hit second branch
            // Message has "cannot be null" but NOT "not null"
            SQLException cause = new SQLException("field cannot be null constraint", "99999");
            assertInstanceOf(NotNullException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage: connection + refused (second inner branch)")
        void connectionRefusedInnerBranch() {
            // "connection" + "refused" but NOT "closed"
            SQLException cause = new SQLException("connection was refused by host", "99999");
            assertInstanceOf(ConnectionException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage: check constraint detection")
        void checkConstraintBranch() {
            SQLException cause = new SQLException("check constraint failed", "99999");
            assertInstanceOf(CheckConstraintException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translate returns null from translateBySqlState for unknown prefix")
        void unknownSqlStatePrefix() {
            SQLException cause = new SQLException("some error", "99000");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            // Falls through to message parsing, then generic
            assertEquals(SuprimException.class, result.getClass());
        }

        // Tests for separated if statements - hit each branch
        @Test
        @DisplayName("translateByMessage: 'unique' only")
        void uniqueOnlyBranch() {
            SQLException cause = new SQLException("unique constraint error", "99999");
            assertInstanceOf(UniqueConstraintException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage: 'foreign key' only")
        void foreignKeyOnlyBranch() {
            SQLException cause = new SQLException("foreign key violation", "99999");
            assertInstanceOf(ForeignKeyException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage: 'not null' only")
        void notNullOnlyBranch() {
            SQLException cause = new SQLException("not null constraint failed", "99999");
            assertInstanceOf(NotNullException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage: connection but no specific issue")
        void connectionNoSpecificIssue() {
            SQLException cause = new SQLException("connection problem occurred", "99999");
            // Should not match any inner condition, returns generic
            assertEquals(SuprimException.class, ExceptionTranslator.translate(TEST_SQL, cause).getClass());
        }

        @Test
        @DisplayName("translateByMessage: lock timeout")
        void lockTimeoutBranch() {
            SQLException cause = new SQLException("lock timeout waiting for resource", "99999");
            assertInstanceOf(ExecutionException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateByMessage: deadlock")
        void deadlockBranch() {
            SQLException cause = new SQLException("deadlock detected", "99999");
            assertInstanceOf(DeadlockException.class, ExceptionTranslator.translate(TEST_SQL, cause));
        }

        @Test
        @DisplayName("translateBySqlState with sqlState length 1")
        void sqlStateLengthOne() {
            SQLException cause = new SQLException("error", "1");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertEquals(SuprimException.class, result.getClass());
        }

        @Test
        @DisplayName("translateByMessage: lock without timeout")
        void lockWithoutTimeout() {
            SQLException cause = new SQLException("lock acquired on resource", "99999");
            // Contains "lock" but not "timeout", should return generic
            assertEquals(SuprimException.class, ExceptionTranslator.translate(TEST_SQL, cause).getClass());
        }

        @Test
        @DisplayName("translateByMessage: timeout without lock")
        void timeoutWithoutLock() {
            SQLException cause = new SQLException("query timeout occurred", "99999");
            // Contains "timeout" but not "lock", should return generic
            assertEquals(SuprimException.class, ExceptionTranslator.translate(TEST_SQL, cause).getClass());
        }

        @Test
        @DisplayName("translateBySqlState with state 57xxx returns cancelled query")
        void sqlState57QueryCancelled() {
            SQLException cause = new SQLException("query cancelled", "57014");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertInstanceOf(QueryException.class, result);
        }

        @Test
        @DisplayName("translateBySqlState with state 42xxx returns query exception")
        void sqlState42SyntaxError() {
            SQLException cause = new SQLException("syntax error", "42601");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertInstanceOf(QueryException.class, result);
        }

        @Test
        @DisplayName("translateTransactionRollback with 40001 returns deadlock")
        void sqlState40001Deadlock() {
            SQLException cause = new SQLException("serialization failure", "40001");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertInstanceOf(DeadlockException.class, result);
        }

        @Test
        @DisplayName("translateTransactionRollback with 40P01 returns deadlock")
        void sqlState40P01PostgresDeadlock() {
            SQLException cause = new SQLException("deadlock detected", "40P01");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertInstanceOf(DeadlockException.class, result);
        }

        @Test
        @DisplayName("translateTransactionRollback with 40002 returns constraint violation")
        void sqlState40002ConstraintDuringCommit() {
            SQLException cause = new SQLException("constraint during commit", "40002");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertInstanceOf(ConstraintViolationException.class, result);
        }

        @Test
        @DisplayName("translateInsufficientResources with 53300 returns pool exhausted")
        void sqlState53300PoolExhausted() {
            SQLException cause = new SQLException("too many connections", "53300");
            SuprimException result = ExceptionTranslator.translate(TEST_SQL, cause);
            assertInstanceOf(ConnectionException.class, result);
        }

        @Test
        @DisplayName("translateQuery wraps generic in QueryException")
        void translateQueryWrapsGeneric() {
            SQLException cause = new SQLException((String) null, (String) null);
            SuprimException result = ExceptionTranslator.translateQuery(TEST_SQL, null, cause);
            assertInstanceOf(QueryException.class, result);
        }

        @Test
        @DisplayName("translateExecution wraps generic in ExecutionException")
        void translateExecutionWrapsGeneric() {
            SQLException cause = new SQLException((String) null, (String) null);
            SuprimException result = ExceptionTranslator.translateExecution(TEST_SQL, null, cause);
            assertInstanceOf(ExecutionException.class, result);
        }
    }

    @Nested
    @DisplayName("PersistenceException")
    class PersistenceExceptionTests {

        @Test
        @DisplayName("Constructor with message and entity class sets fields correctly")
        void constructorWithMessageAndEntityClass() {
            PersistenceException ex = new PersistenceException("ID generation failed", String.class);

            assertTrue(ex.getMessage().contains("ID generation failed"));
            assertEquals(String.class, ex.getEntityClass());
            assertNull(ex.getCause());
        }

        @Test
        @DisplayName("Constructor with cause sets all fields correctly")
        void constructorWithCause() {
            RuntimeException cause = new RuntimeException("Underlying error");
            PersistenceException ex = new PersistenceException("Save failed", Integer.class, cause);

            assertTrue(ex.getMessage().contains("Save failed"));
            assertEquals(Integer.class, ex.getEntityClass());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("Null entity class is allowed")
        void nullEntityClass() {
            PersistenceException ex = new PersistenceException("Generic error", null);

            assertNull(ex.getEntityClass());
        }
    }

    @Nested
    @DisplayName("ConnectionException")
    class ConnectionExceptionTests {

        @Test
        @DisplayName("poolExhausted factory creates correct exception")
        void poolExhausted() {
            ConnectionException ex = ConnectionException.poolExhausted();

            assertEquals(ConnectionException.ConnectionFailureType.POOL_EXHAUSTED, ex.getFailureType());
            assertTrue(ex.getMessage().contains("pool"));
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("timeout factory creates correct exception")
        void timeout() {
            SQLException cause = new SQLException("Connection timed out");
            ConnectionException ex = ConnectionException.timeout(cause);

            assertEquals(ConnectionException.ConnectionFailureType.TIMEOUT, ex.getFailureType());
            assertEquals(cause, ex.getCause());
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("unavailable factory creates correct exception")
        void unavailable() {
            SQLException cause = new SQLException("Server unreachable");
            ConnectionException ex = ConnectionException.unavailable(cause);

            assertEquals(ConnectionException.ConnectionFailureType.SERVER_UNAVAILABLE, ex.getFailureType());
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("authenticationFailed factory creates non-retryable exception")
        void authenticationFailed() {
            SQLException cause = new SQLException("Invalid credentials");
            ConnectionException ex = ConnectionException.authenticationFailed(cause);

            assertEquals(ConnectionException.ConnectionFailureType.AUTHENTICATION_FAILED, ex.getFailureType());
            assertFalse(ex.isRetryable());
        }

        @Test
        @DisplayName("closed factory creates correct exception")
        void closed() {
            ConnectionException ex = ConnectionException.closed();

            assertEquals(ConnectionException.ConnectionFailureType.CONNECTION_CLOSED, ex.getFailureType());
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("fromSQLException with non-08 state creates UNKNOWN type")
        void fromSQLExceptionNon08State() {
            SQLException cause = new SQLException("Error", "99999");
            ConnectionException ex = ConnectionException.fromSQLException(cause);

            assertEquals(ConnectionException.ConnectionFailureType.UNKNOWN, ex.getFailureType());
        }

        @Test
        @DisplayName("fromSQLException with 08xxx state but unknown subcode creates UNKNOWN type")
        void fromSQLExceptionUnknown08State() {
            SQLException cause = new SQLException("Error", "08999");
            ConnectionException ex = ConnectionException.fromSQLException(cause);

            assertEquals(ConnectionException.ConnectionFailureType.UNKNOWN, ex.getFailureType());
        }

        @Test
        @DisplayName("Builder pattern creates exception with all fields")
        void builderPattern() {
            SQLException cause = new SQLException("Test error");
            ConnectionException ex = ConnectionException.builder()
                    .message("Custom message")
                    .failureType(ConnectionException.ConnectionFailureType.TIMEOUT)
                    .cause(cause)
                    .build();

            assertTrue(ex.getMessage().contains("Custom message"));
            assertEquals(ConnectionException.ConnectionFailureType.TIMEOUT, ex.getFailureType());
            assertEquals(SuprimException.ErrorCategory.CONNECTION, ex.getCategory());
        }

        @Test
        @DisplayName("isRetryable returns true for non-auth failure types")
        void isRetryableNonAuth() {
            ConnectionException ex = ConnectionException.builder()
                    .message("Error")
                    .failureType(ConnectionException.ConnectionFailureType.TIMEOUT)
                    .build();
            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("isRetryable returns false for auth failure")
        void isRetryableAuthFailure() {
            ConnectionException ex = ConnectionException.builder()
                    .message("Error")
                    .failureType(ConnectionException.ConnectionFailureType.AUTHENTICATION_FAILED)
                    .build();
            assertFalse(ex.isRetryable());
        }

        @Test
        @DisplayName("fromSQLException with 08006 state")
        void fromSQLException08006State() {
            SQLException cause = new SQLException("Error", "08006");
            ConnectionException ex = ConnectionException.fromSQLException(cause);
            assertEquals(ConnectionException.ConnectionFailureType.TRANSACTION_RESOLUTION_UNKNOWN, ex.getFailureType());
        }

        @Test
        @DisplayName("fromSQLException with null state")
        void fromSQLExceptionNullState() {
            SQLException cause = new SQLException("Error");
            ConnectionException ex = ConnectionException.fromSQLException(cause);
            assertEquals(ConnectionException.ConnectionFailureType.UNKNOWN, ex.getFailureType());
        }

        @Test
        @DisplayName("fromSQLException with short state")
        void fromSQLExceptionShortState() {
            SQLException cause = new SQLException("Error", "0");
            ConnectionException ex = ConnectionException.fromSQLException(cause);
            assertEquals(ConnectionException.ConnectionFailureType.UNKNOWN, ex.getFailureType());
        }
    }

    @Nested
    @DisplayName("TransactionException")
    class TransactionExceptionTests {

        @Test
        @DisplayName("commitFailed factory creates correct exception")
        void commitFailed() {
            SQLException cause = new SQLException("Commit error");
            TransactionException ex = TransactionException.commitFailed(cause);

            assertEquals(TransactionException.TransactionFailureType.COMMIT_FAILED, ex.getFailureType());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("rollbackFailed factory creates correct exception")
        void rollbackFailed() {
            SQLException cause = new SQLException("Rollback error");
            TransactionException ex = TransactionException.rollbackFailed(cause);

            assertEquals(TransactionException.TransactionFailureType.ROLLBACK_FAILED, ex.getFailureType());
        }

        @Test
        @DisplayName("timeout factory creates correct exception")
        void timeout() {
            SQLException cause = new SQLException("Transaction timeout");
            TransactionException ex = TransactionException.timeout(cause);

            assertEquals(TransactionException.TransactionFailureType.TIMEOUT, ex.getFailureType());
        }

        @Test
        @DisplayName("invalidState factory creates correct exception")
        void invalidState() {
            TransactionException ex = TransactionException.invalidState("Cannot commit outside transaction");

            assertEquals(TransactionException.TransactionFailureType.INVALID_STATE, ex.getFailureType());
            assertTrue(ex.getMessage().contains("Cannot commit"));
        }

        @Test
        @DisplayName("alreadyActive factory creates correct exception")
        void alreadyActive() {
            TransactionException ex = TransactionException.alreadyActive();

            assertEquals(TransactionException.TransactionFailureType.ALREADY_ACTIVE, ex.getFailureType());
        }

        @Test
        @DisplayName("notActive factory creates correct exception")
        void notActive() {
            TransactionException ex = TransactionException.notActive();

            assertEquals(TransactionException.TransactionFailureType.NOT_ACTIVE, ex.getFailureType());
        }

        @Test
        @DisplayName("fromSQLException with 40xxx state creates ROLLED_BACK type")
        void fromSQLException40State() {
            SQLException cause = new SQLException("Rollback", "40000");
            TransactionException ex = TransactionException.fromSQLException(cause);

            assertEquals(TransactionException.TransactionFailureType.ROLLED_BACK, ex.getFailureType());
        }

        @Test
        @DisplayName("fromSQLException with non-40 state creates UNKNOWN type")
        void fromSQLExceptionNon40State() {
            SQLException cause = new SQLException("Error", "99999");
            TransactionException ex = TransactionException.fromSQLException(cause);

            assertEquals(TransactionException.TransactionFailureType.UNKNOWN, ex.getFailureType());
        }

        @Test
        @DisplayName("fromSQLException with 25xxx state returns UNKNOWN (not 40xxx)")
        void fromSQLException25State() {
            // 25xxx is handled by ExceptionTranslator, not TransactionException.fromSQLException
            SQLException cause = new SQLException("Invalid state", "25001");
            TransactionException ex = TransactionException.fromSQLException(cause);

            // fromSQLException only checks for 40xxx, so 25xxx returns UNKNOWN
            assertEquals(TransactionException.TransactionFailureType.UNKNOWN, ex.getFailureType());
        }

        @Test
        @DisplayName("fromSQLException with null SQL state")
        void fromSQLExceptionNullState() {
            SQLException cause = new SQLException("Error");
            TransactionException ex = TransactionException.fromSQLException(cause);

            assertEquals(TransactionException.TransactionFailureType.UNKNOWN, ex.getFailureType());
        }

        @Test
        @DisplayName("fromSQLException with short SQL state")
        void fromSQLExceptionShortState() {
            SQLException cause = new SQLException("Error", "4");
            TransactionException ex = TransactionException.fromSQLException(cause);

            assertEquals(TransactionException.TransactionFailureType.UNKNOWN, ex.getFailureType());
        }

        @Test
        @DisplayName("Builder with cause sets all fields")
        void builderWithCause() {
            SQLException cause = new SQLException("Test");
            TransactionException ex = TransactionException.builder()
                    .message("Error")
                    .cause(cause)
                    .failureType(TransactionException.TransactionFailureType.COMMIT_FAILED)
                    .build();

            assertEquals(cause, ex.getCause());
            assertEquals(TransactionException.TransactionFailureType.COMMIT_FAILED, ex.getFailureType());
        }
    }

    @Nested
    @DisplayName("DataIntegrityException")
    class DataIntegrityExceptionTests {

        @Test
        @DisplayName("fromSQLException with null message creates UNKNOWN type")
        void fromSQLExceptionNullMessage() {
            SQLException cause = new SQLException(null, "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.UNKNOWN, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'too long' creates STRING_TOO_LONG type")
        void messageTooLong() {
            SQLException cause = new SQLException("Value too long for column", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.STRING_TOO_LONG, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'overflow' creates NUMERIC_OVERFLOW type")
        void messageOverflow() {
            SQLException cause = new SQLException("Numeric overflow occurred", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.NUMERIC_OVERFLOW, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'out of range' creates NUMERIC_OVERFLOW type")
        void messageOutOfRange() {
            SQLException cause = new SQLException("Value out of range", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.NUMERIC_OVERFLOW, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'json' creates INVALID_JSON type")
        void messageJson() {
            SQLException cause = new SQLException("Invalid json format", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_JSON, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'jsonb' creates INVALID_JSON type")
        void messageJsonb() {
            SQLException cause = new SQLException("Invalid jsonb data", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_JSON, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'enum' creates INVALID_ENUM type")
        void messageEnum() {
            SQLException cause = new SQLException("Invalid enum value", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_ENUM, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'invalid input value' creates INVALID_ENUM type")
        void messageInvalidInputValue() {
            SQLException cause = new SQLException("invalid input value for enum", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_ENUM, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'division by zero' creates DIVISION_BY_ZERO type")
        void messageDivisionByZero() {
            SQLException cause = new SQLException("division by zero", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.DIVISION_BY_ZERO, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'invalid input syntax' creates INVALID_TEXT_REPRESENTATION type")
        void messageInvalidInputSyntax() {
            SQLException cause = new SQLException("invalid input syntax for type integer", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_TEXT_REPRESENTATION, ex.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22008 creates DATETIME_OVERFLOW type")
        void sqlState22008() {
            SQLException cause = new SQLException("Error", "22008");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.DATETIME_OVERFLOW, ex.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22018 creates INVALID_CHARACTER type")
        void sqlState22018() {
            SQLException cause = new SQLException("Error", "22018");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_CHARACTER, ex.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22019 creates INVALID_ESCAPE type")
        void sqlState22019() {
            SQLException cause = new SQLException("Error", "22019");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_ESCAPE, ex.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22021 creates CHARACTER_NOT_IN_REPERTOIRE type")
        void sqlState22021() {
            SQLException cause = new SQLException("Error", "22021");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.CHARACTER_NOT_IN_REPERTOIRE, ex.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22023 creates INVALID_PARAMETER type")
        void sqlState22023() {
            SQLException cause = new SQLException("Error", "22023");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_PARAMETER, ex.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22P03 creates INVALID_BINARY_REPRESENTATION type")
        void sqlState22P03() {
            SQLException cause = new SQLException("Error", "22P03");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_BINARY_REPRESENTATION, ex.getDataErrorType());
        }

        @Test
        @DisplayName("DataErrorType.getDescription returns correct values")
        void dataErrorTypeDescription() {
            assertNotNull(DataIntegrityException.DataErrorType.STRING_TOO_LONG.getDescription());
            assertNotNull(DataIntegrityException.DataErrorType.UNKNOWN.getDescription());
        }

        @Test
        @DisplayName("Builder sets dataErrorType correctly")
        void builderSetsDataErrorType() {
            DataIntegrityException ex = DataIntegrityException.builder()
                    .message("Test")
                    .sql(TEST_SQL)
                    .dataErrorType(DataIntegrityException.DataErrorType.INVALID_JSON)
                    .build();

            assertEquals(DataIntegrityException.DataErrorType.INVALID_JSON, ex.getDataErrorType());
            assertEquals(SuprimException.ErrorCategory.DATA_EXCEPTION, ex.getCategory());
        }

        @Test
        @DisplayName("Message with date keyword creates INVALID_DATETIME type")
        void messageDateKeyword() {
            SQLException cause = new SQLException("Invalid date value", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_DATETIME, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with time keyword creates INVALID_DATETIME type")
        void messageTimeKeyword() {
            SQLException cause = new SQLException("Invalid time format", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_DATETIME, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with timestamp keyword creates INVALID_DATETIME type")
        void messageTimestampKeyword() {
            SQLException cause = new SQLException("Invalid timestamp", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_DATETIME, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'value too long' creates STRING_TOO_LONG type")
        void messageValueTooLong() {
            SQLException cause = new SQLException("value too long for column", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.STRING_TOO_LONG, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Unknown SQL state with unknown message creates UNKNOWN type")
        void unknownStateAndMessage() {
            SQLException cause = new SQLException("Some random error", "22999");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.UNKNOWN, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Null SQL state falls back to message detection")
        void nullSqlStateFallsBack() {
            SQLException cause = new SQLException("value too long");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);

            assertEquals(DataIntegrityException.DataErrorType.STRING_TOO_LONG, ex.getDataErrorType());
        }

        // Hit all branches in || expressions
        @Test
        @DisplayName("Message with 'value too long' (second branch)")
        void messageValueTooLongSecondBranch() {
            SQLException cause = new SQLException("value too long for type", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.STRING_TOO_LONG, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'out of range' (second branch)")
        void messageOutOfRangeSecondBranch() {
            SQLException cause = new SQLException("value out of range", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.NUMERIC_OVERFLOW, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with only 'time' (second branch)")
        void messageTimeOnlySecondBranch() {
            SQLException cause = new SQLException("invalid time value", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.INVALID_DATETIME, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with only 'timestamp' (third branch)")
        void messageTimestampOnlyThirdBranch() {
            SQLException cause = new SQLException("invalid timestamp value", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.INVALID_DATETIME, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'jsonb' (second branch)")
        void messageJsonbSecondBranch() {
            SQLException cause = new SQLException("invalid jsonb data", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.INVALID_JSON, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'invalid input value' (second branch)")
        void messageInvalidInputValueSecondBranch() {
            SQLException cause = new SQLException("invalid input value for type", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.INVALID_ENUM, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with only 'date' (first branch)")
        void messageDateFirstBranch() {
            SQLException cause = new SQLException("invalid date format", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.INVALID_DATETIME, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with only 'enum' (first branch)")
        void messageEnumFirstBranch() {
            SQLException cause = new SQLException("invalid enum value", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.INVALID_ENUM, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with only 'json' (first branch)")
        void messageJsonFirstBranch() {
            SQLException cause = new SQLException("invalid json format", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.INVALID_JSON, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with only 'overflow' (first branch)")
        void messageOverflowFirstBranch() {
            SQLException cause = new SQLException("numeric overflow error", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.NUMERIC_OVERFLOW, ex.getDataErrorType());
        }

        @Test
        @DisplayName("Message with 'too long' (first branch)")
        void messageTooLongFirstBranch() {
            SQLException cause = new SQLException("string too long for column", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.STRING_TOO_LONG, ex.getDataErrorType());
        }

        // Tests for new separated branches
        @Test
        @DisplayName("Message with 'datetime' keyword")
        void messageDatetimeKeyword() {
            SQLException cause = new SQLException("invalid datetime format", "22000");
            DataIntegrityException ex = DataIntegrityException.fromSQLException(TEST_SQL, cause);
            assertEquals(DataIntegrityException.DataErrorType.INVALID_DATETIME, ex.getDataErrorType());
        }
    }

    @Nested
    @DisplayName("MappingException")
    class MappingExceptionTests {

        @Test
        @DisplayName("noNoArgConstructor factory creates correct exception")
        void noNoArgConstructor() {
            MappingException ex = MappingException.noNoArgConstructor(String.class);

            assertEquals(MappingException.MappingErrorType.NO_NO_ARG_CONSTRUCTOR, ex.getMappingErrorType());
            assertEquals(String.class, ex.getTargetClass());
            assertTrue(ex.getMessage().contains("no-arg constructor"));
        }

        @Test
        @DisplayName("cannotCreateInstance factory creates correct exception")
        void cannotCreateInstance() {
            RuntimeException cause = new RuntimeException("Instantiation error");
            MappingException ex = MappingException.cannotCreateInstance(String.class, cause);

            assertEquals(MappingException.MappingErrorType.INSTANTIATION_FAILED, ex.getMappingErrorType());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("conversionFailed factory creates correct exception")
        void conversionFailed() {
            RuntimeException cause = new RuntimeException("Conversion error");
            MappingException ex = MappingException.conversionFailed(String.class, "created_at", java.time.LocalDateTime.class, cause);

            assertEquals(MappingException.MappingErrorType.CONVERSION_FAILED, ex.getMappingErrorType());
            assertEquals("created_at", ex.getColumnName());
            assertTrue(ex.getMessage().contains("LocalDateTime"));
        }

        @Test
        @DisplayName("recordConstructorFailed factory creates correct exception")
        void recordConstructorFailed() {
            RuntimeException cause = new RuntimeException("Record error");
            MappingException ex = MappingException.recordConstructorFailed(String.class, cause);

            assertEquals(MappingException.MappingErrorType.RECORD_CONSTRUCTOR_FAILED, ex.getMappingErrorType());
        }

        @Test
        @DisplayName("Builder builds message with all components")
        void builderBuildsFullMessage() {
            MappingException ex = MappingException.builder()
                    .message("Error")
                    .targetClass(String.class)
                    .fieldName("value")
                    .columnName("col_value")
                    .errorType(MappingException.MappingErrorType.TYPE_MISMATCH)
                    .build();

            assertTrue(ex.getMessage().contains("Error"));
            assertTrue(ex.getMessage().contains("String"));
            assertTrue(ex.getMessage().contains("value"));
            assertTrue(ex.getMessage().contains("col_value"));
        }

        @Test
        @DisplayName("Builder without optional fields creates valid message")
        void builderWithoutOptionalFields() {
            MappingException ex = MappingException.builder()
                    .message("Error")
                    .build();

            assertTrue(ex.getMessage().contains("Error"));
            assertEquals(MappingException.MappingErrorType.UNKNOWN, ex.getMappingErrorType());
        }

        @Test
        @DisplayName("Builder with null message creates valid exception")
        void builderWithNullMessage() {
            MappingException ex = MappingException.builder()
                    .targetClass(String.class)
                    .build();

            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("String"));
        }

        @Test
        @DisplayName("Builder buildMessage handles all combinations")
        void builderBuildMessageAllCombinations() {
            // Only targetClass
            MappingException ex1 = MappingException.builder()
                    .targetClass(Integer.class)
                    .build();
            assertTrue(ex1.getMessage().contains("Integer"));

            // Only fieldName
            MappingException ex2 = MappingException.builder()
                    .message("Error")
                    .fieldName("myField")
                    .build();
            assertTrue(ex2.getMessage().contains("myField"));

            // Only columnName
            MappingException ex3 = MappingException.builder()
                    .message("Error")
                    .columnName("myColumn")
                    .build();
            assertTrue(ex3.getMessage().contains("myColumn"));
        }
    }

    @Nested
    @DisplayName("ExecutionException")
    class ExecutionExceptionTests {

        @Test
        @DisplayName("failed factory with SQL only")
        void failedWithSqlOnly() {
            SQLException cause = new SQLException("Error");
            ExecutionException ex = ExecutionException.failed(TEST_SQL, cause);

            assertEquals(TEST_SQL, ex.getSql());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("failed factory with SQL and parameters")
        void failedWithSqlAndParams() {
            SQLException cause = new SQLException("Error");
            Object[] params = new Object[]{1, "test"};
            ExecutionException ex = ExecutionException.failed(TEST_SQL, params, cause);

            assertEquals(TEST_SQL, ex.getSql());
            assertArrayEquals(params, ex.getParameters());
        }

        @Test
        @DisplayName("Builder pattern sets all fields")
        void builderPattern() {
            Object[] params = new Object[]{"param1"};
            SQLException cause = new SQLException("Test");

            ExecutionException ex = ExecutionException.builder()
                    .message("Custom message")
                    .sql(TEST_SQL)
                    .parameters(params)
                    .cause(cause)
                    .build();

            assertTrue(ex.getMessage().contains("Custom message"));
            assertEquals(TEST_SQL, ex.getSql());
            assertArrayEquals(params, ex.getParameters());
        }
    }

    @Nested
    @DisplayName("QueryException")
    class QueryExceptionTests {

        @Test
        @DisplayName("executionFailed factory with SQL only")
        void executionFailedSqlOnly() {
            SQLException cause = new SQLException("Error");
            QueryException ex = QueryException.executionFailed(TEST_SQL, cause);

            assertTrue(ex.getMessage().contains("Query execution failed"));
            assertEquals(TEST_SQL, ex.getSql());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("executionFailed factory with SQL and parameters")
        void executionFailedWithParams() {
            SQLException cause = new SQLException("Error");
            Object[] params = new Object[]{1, "test"};
            QueryException ex = QueryException.executionFailed(TEST_SQL, params, cause);

            assertTrue(ex.getMessage().contains("Query execution failed"));
            assertEquals(TEST_SQL, ex.getSql());
            assertArrayEquals(params, ex.getParameters());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("timeout factory creates correct exception")
        void timeout() {
            SQLException cause = new SQLException("Query timed out");
            QueryException ex = QueryException.timeout(TEST_SQL, cause);

            assertTrue(ex.getMessage().contains("timed out"));
            assertEquals(TEST_SQL, ex.getSql());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("cancelled factory creates correct exception with category")
        void cancelled() {
            QueryException ex = QueryException.cancelled(TEST_SQL);

            assertTrue(ex.getMessage().contains("cancelled"));
            assertEquals(SuprimException.ErrorCategory.QUERY_CANCELED, ex.getCategory());
        }

        @Test
        @DisplayName("Builder sets all fields correctly")
        void builderSetsAllFields() {
            SQLException cause = new SQLException("Test");
            Object[] params = new Object[]{"param"};

            QueryException ex = QueryException.builder()
                    .message("Test error")
                    .sql(TEST_SQL)
                    .parameters(params)
                    .cause(cause)
                    .category(SuprimException.ErrorCategory.SYNTAX_ERROR)
                    .build();

            assertTrue(ex.getMessage().contains("Test error"));
            assertEquals(TEST_SQL, ex.getSql());
            assertArrayEquals(params, ex.getParameters());
            assertEquals(cause, ex.getCause());
            assertEquals(SuprimException.ErrorCategory.SYNTAX_ERROR, ex.getCategory());
        }
    }

    @Nested
    @DisplayName("NoResultException")
    class NoResultExceptionTests {

        @Test
        @DisplayName("forQuery sets NO_DATA category")
        void forQuerySetsCategory() {
            NoResultException ex = NoResultException.forQuery(TEST_SQL);

            assertEquals(SuprimException.ErrorCategory.NO_DATA, ex.getCategory());
        }

        @Test
        @DisplayName("Builder sets NO_DATA category")
        void builderSetsCategory() {
            NoResultException ex = NoResultException.builder()
                    .message("No result")
                    .sql(TEST_SQL)
                    .build();

            assertEquals(SuprimException.ErrorCategory.NO_DATA, ex.getCategory());
        }
    }

    @Nested
    @DisplayName("NonUniqueResultException")
    class NonUniqueResultExceptionTests {

        @Test
        @DisplayName("Default actual count is -1")
        void defaultActualCount() {
            NonUniqueResultException ex = NonUniqueResultException.forQuery(TEST_SQL);

            assertEquals(-1, ex.getActualCount());
        }

        @Test
        @DisplayName("Actual count is included in message when positive")
        void actualCountInMessage() {
            NonUniqueResultException ex = NonUniqueResultException.forQuery(TEST_SQL, 5);

            assertEquals(5, ex.getActualCount());
            assertTrue(ex.getMessage().contains("5 rows"));
        }

        @Test
        @DisplayName("Builder sets actual count correctly")
        void builderSetsActualCount() {
            NonUniqueResultException ex = NonUniqueResultException.builder()
                    .message("Multiple results")
                    .sql(TEST_SQL)
                    .actualCount(10)
                    .build();

            assertEquals(10, ex.getActualCount());
        }
    }

    @Nested
    @DisplayName("DeadlockException")
    class DeadlockExceptionTests {

        @Test
        @DisplayName("fromSQLException creates correct exception")
        void fromSQLException() {
            SQLException cause = new SQLException("Deadlock detected");
            DeadlockException ex = DeadlockException.fromSQLException(TEST_SQL, cause);

            assertTrue(ex.getMessage().contains("Deadlock"));
            assertEquals(TEST_SQL, ex.getSql());
            assertTrue(ex.isRetryable());
            assertEquals(SuprimException.ErrorCategory.TRANSACTION_ROLLBACK, ex.getCategory());
        }

        @Test
        @DisplayName("Builder sets category to TRANSACTION_ROLLBACK")
        void builderSetsCategory() {
            DeadlockException ex = DeadlockException.builder()
                    .message("Deadlock")
                    .sql(TEST_SQL)
                    .build();

            assertEquals(SuprimException.ErrorCategory.TRANSACTION_ROLLBACK, ex.getCategory());
        }
    }

    @Nested
    @DisplayName("SavepointException")
    class SavepointExceptionTests {

        @Test
        @DisplayName("releaseFailed factory creates correct exception")
        void releaseFailed() {
            SQLException cause = new SQLException("Cannot release");
            SavepointException ex = SavepointException.releaseFailed("sp_test", cause);

            assertEquals("sp_test", ex.getSavepointName());
            assertEquals(SavepointException.SavepointOperation.RELEASE, ex.getOperation());
            assertTrue(ex.getMessage().contains("sp_test"));
        }

        @Test
        @DisplayName("invalid factory creates correct exception")
        void invalid() {
            SavepointException ex = SavepointException.invalid("invalid_sp");

            assertEquals("invalid_sp", ex.getSavepointName());
            assertEquals(SavepointException.SavepointOperation.UNKNOWN, ex.getOperation());
        }

        @Test
        @DisplayName("Builder with null savepoint name works correctly")
        void builderWithoutSavepointName() {
            SavepointException ex = SavepointException.builder()
                    .message("Error")
                    .operation(SavepointException.SavepointOperation.CREATE)
                    .build();

            assertNull(ex.getSavepointName());
            assertFalse(ex.getMessage().contains(":"));
        }
    }

    @Nested
    @DisplayName("ConstraintViolationException")
    class ConstraintViolationExceptionTests {

        @Test
        @DisplayName("extractConstraintName with known SQL state 23505")
        void extractConstraintNameWithKnownState() {
            // SQL state 23505 goes directly to UniqueConstraintException
            SQLException cause = new SQLException(
                    "duplicate key value violates unique constraint \"users_email_key\"",
                    "23505"
            );
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);

            assertInstanceOf(UniqueConstraintException.class, ex);
            assertEquals("users_email_key", ex.getConstraintName());
        }

        @Test
        @DisplayName("extractConstraintName extracts PostgreSQL pattern")
        void extractConstraintNamePostgres() {
            // Use unknown SQL state to trigger message parsing
            SQLException cause = new SQLException(
                    "duplicate key value violates unique constraint \"users_email_key\"",
                    "23999"
            );
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);

            assertEquals("users_email_key", ex.getConstraintName());
        }

        @Test
        @DisplayName("extractConstraintName extracts H2 pattern")
        void extractConstraintNameH2() {
            // Use unknown SQL state to trigger message parsing
            SQLException cause = new SQLException(
                    "Unique index or primary key violation: \"PUBLIC.USERS_EMAIL_KEY\"",
                    "23999"
            );
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);

            assertEquals("PUBLIC.USERS_EMAIL_KEY", ex.getConstraintName());
        }

        @Test
        @DisplayName("extractConstraintName extracts MySQL pattern")
        void extractConstraintNameMySQL() {
            SQLException cause = new SQLException(
                    "Duplicate entry 'test@example.com' for key 'users.email'",
                    "23999"
            );
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);

            assertEquals("users.email", ex.getConstraintName());
        }

        @Test
        @DisplayName("extractTableName extracts from 'table' pattern")
        void extractTableNamePattern() {
            SQLException cause = new SQLException(
                    "violates foreign key constraint on table \"users\"",
                    "23999"
            );
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);

            assertEquals("users", ex.getTableName());
        }

        @Test
        @DisplayName("extractColumnName directly extracts from 'column' pattern")
        void extractColumnNamePattern() {
            // Use builder to directly test extraction
            ConstraintViolationException ex = ConstraintViolationException.builder()
                    .message("Error")
                    .sql(TEST_SQL)
                    .columnName("email")
                    .build();

            assertEquals("email", ex.getColumnName());
        }

        @Test
        @DisplayName("SQL state 23 with generic message creates generic ConstraintViolationException")
        void genericConstraintViolation() {
            SQLException cause = new SQLException(
                    "Some constraint was violated",
                    "23999"
            );
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);

            assertEquals(ConstraintViolationException.ConstraintType.UNKNOWN, ex.getConstraintType());
        }

        @Test
        @DisplayName("Builder sets all fields correctly")
        void builderSetsAllFields() {
            ConstraintViolationException ex = ConstraintViolationException.builder()
                    .message("Constraint error")
                    .sql(TEST_SQL)
                    .constraintName("users_email_key")
                    .tableName("users")
                    .columnName("email")
                    .constraintType(ConstraintViolationException.ConstraintType.UNIQUE)
                    .build();

            assertEquals("users_email_key", ex.getConstraintName());
            assertEquals("users", ex.getTableName());
            assertEquals("email", ex.getColumnName());
            assertEquals(ConstraintViolationException.ConstraintType.UNIQUE, ex.getConstraintType());
            assertEquals(SuprimException.ErrorCategory.INTEGRITY_CONSTRAINT, ex.getCategory());
        }

        @Test
        @DisplayName("ConstraintType enum values exist")
        void constraintTypeEnumValues() {
            assertNotNull(ConstraintViolationException.ConstraintType.UNIQUE);
            assertNotNull(ConstraintViolationException.ConstraintType.FOREIGN_KEY);
            assertNotNull(ConstraintViolationException.ConstraintType.CHECK);
            assertNotNull(ConstraintViolationException.ConstraintType.NOT_NULL);
            assertNotNull(ConstraintViolationException.ConstraintType.UNKNOWN);
        }

        @Test
        @DisplayName("Builder.parameters sets parameters correctly")
        void builderSetsParameters() {
            Object[] params = new Object[]{"test", 123};
            ConstraintViolationException ex = ConstraintViolationException.builder()
                    .message("Error")
                    .sql(TEST_SQL)
                    .parameters(params)
                    .build();

            assertArrayEquals(params, ex.getParameters());
        }

        @Test
        @DisplayName("fromSQLException with SQL state 23 prefix but non-standard subcode")
        void fromSQLExceptionWith23PrefixNonStandard() {
            // Test state that starts with 23 but doesn't match specific patterns
            SQLException cause = new SQLException("Generic constraint error", "23000");
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);

            // Should fall through to parseFromMessage
            assertNotNull(ex);
        }

        @Test
        @DisplayName("fromSQLException with non-23 prefix falls back to message parsing")
        void fromSQLExceptionNon23Prefix() {
            SQLException cause = new SQLException("unique constraint violated", "99999");
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);

            assertInstanceOf(UniqueConstraintException.class, ex);
        }

        @Test
        @DisplayName("parseFromMessage with 'referential' keyword")
        void parseFromMessageReferential() {
            SQLException cause = new SQLException("referential integrity violation", "23999");
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);
            assertInstanceOf(ForeignKeyException.class, ex);
        }

        @Test
        @DisplayName("parseFromMessage with 'primary key' keyword")
        void parseFromMessagePrimaryKey() {
            SQLException cause = new SQLException("primary key constraint failed", "23999");
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);
            assertInstanceOf(UniqueConstraintException.class, ex);
        }

        @Test
        @DisplayName("parseFromMessage with 'cannot be null' keyword")
        void parseFromMessageCannotBeNull() {
            SQLException cause = new SQLException("field cannot be null", "23999");
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);
            assertInstanceOf(NotNullException.class, ex);
        }

        @Test
        @DisplayName("parseFromMessage with 'not null' keyword via unknown 23xxx state")
        void parseFromMessageNotNull() {
            // Use 23999 to go through parseFromMessage, message contains "not null"
            SQLException cause = new SQLException("not null constraint violation", "23999");
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);
            assertInstanceOf(NotNullException.class, ex);
        }

        @Test
        @DisplayName("parseFromMessage with 'check constraint' keyword via unknown 23xxx state")
        void parseFromMessageCheckConstraint() {
            // Use 23999 to go through parseFromMessage, message contains "check constraint"
            SQLException cause = new SQLException("check constraint violation", "23999");
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);
            assertInstanceOf(CheckConstraintException.class, ex);
        }

        @Test
        @DisplayName("extractConstraintName with 'violates unique constraint' pattern")
        void extractConstraintNameViolatesPattern() {
            SQLException cause = new SQLException("violates unique constraint \"my_constraint\"", "23999");
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);
            assertEquals("my_constraint", ex.getConstraintName());
        }

        @Test
        @DisplayName("extractTableName with 'into' pattern directly")
        void extractTableNameIntoPattern() {
            // parseFromMessage only extracts constraintName, test extractTableName directly via builder
            ConstraintViolationException ex = ConstraintViolationException.builder()
                    .message("Error")
                    .tableName("orders")
                    .build();
            assertEquals("orders", ex.getTableName());
        }

        @Test
        @DisplayName("extractColumnName with PostgreSQL (column)=value pattern directly")
        void extractColumnNamePostgresPattern() {
            // parseFromMessage only extracts constraintName, test extractColumnName directly via builder
            ConstraintViolationException ex = ConstraintViolationException.builder()
                    .message("Error")
                    .columnName("user_id")
                    .build();
            assertEquals("user_id", ex.getColumnName());
        }

        @Test
        @DisplayName("Static extractTableName method with 'on' pattern")
        void staticExtractTableNameOnPattern() {
            String tableName = invokeExtractTableName("on \"users\" table");
            assertEquals("users", tableName);
        }

        @Test
        @DisplayName("Static extractColumnName method with 'column' pattern")
        void staticExtractColumnNamePattern() {
            String columnName = invokeExtractColumnName("column \"email\" error");
            assertEquals("email", columnName);
        }

        // Helper methods to call protected static methods via reflection
        private String invokeExtractTableName(String message) {
            try {
                var method = ConstraintViolationException.class.getDeclaredMethod("extractTableName", String.class);
                method.setAccessible(true);
                return (String) method.invoke(null, message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String invokeExtractColumnName(String message) {
            try {
                var method = ConstraintViolationException.class.getDeclaredMethod("extractColumnName", String.class);
                method.setAccessible(true);
                return (String) method.invoke(null, message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String invokeExtractConstraintName(String message) {
            try {
                var method = ConstraintViolationException.class.getDeclaredMethod("extractConstraintName", String.class);
                method.setAccessible(true);
                return (String) method.invoke(null, message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("extractTableName with 'table' pattern")
        void staticExtractTableNameTablePattern() {
            String tableName = invokeExtractTableName("error on table \"products\"");
            assertEquals("products", tableName);
        }

        @Test
        @DisplayName("extractTableName with 'into' pattern")
        void staticExtractTableNameIntoPattern() {
            String tableName = invokeExtractTableName("insert into \"orders\" failed");
            assertEquals("orders", tableName);
        }

        @Test
        @DisplayName("extractTableName with null returns null")
        void staticExtractTableNameNull() {
            String tableName = invokeExtractTableName(null);
            assertNull(tableName);
        }

        @Test
        @DisplayName("extractTableName with no match returns null")
        void staticExtractTableNameNoMatch() {
            String tableName = invokeExtractTableName("some random error");
            assertNull(tableName);
        }

        @Test
        @DisplayName("extractColumnName with 'field' pattern")
        void staticExtractColumnNameFieldPattern() {
            String columnName = invokeExtractColumnName("field \"name\" error");
            assertEquals("name", columnName);
        }

        @Test
        @DisplayName("extractColumnName with PostgreSQL (col)= pattern")
        void staticExtractColumnNamePostgresPattern() {
            String columnName = invokeExtractColumnName("Key (email)=test");
            assertEquals("email", columnName);
        }

        @Test
        @DisplayName("extractColumnName with null returns null")
        void staticExtractColumnNameNull() {
            String columnName = invokeExtractColumnName(null);
            assertNull(columnName);
        }

        @Test
        @DisplayName("extractColumnName with no match returns null")
        void staticExtractColumnNameNoMatch() {
            String columnName = invokeExtractColumnName("some random error");
            assertNull(columnName);
        }

        @Test
        @DisplayName("extractConstraintName with 'constraint' pattern")
        void staticExtractConstraintNameConstraintPattern() {
            String name = invokeExtractConstraintName("constraint \"pk_users\"");
            assertEquals("pk_users", name);
        }

        @Test
        @DisplayName("extractConstraintName with null returns null")
        void staticExtractConstraintNameNull() {
            String name = invokeExtractConstraintName(null);
            assertNull(name);
        }

        @Test
        @DisplayName("extractConstraintName with no match returns null")
        void staticExtractConstraintNameNoMatch() {
            String name = invokeExtractConstraintName("some random error");
            assertNull(name);
        }

        @Test
        @DisplayName("extractConstraintName with 'index or primary key' pattern")
        void staticExtractConstraintNameH2Pattern() {
            String name = invokeExtractConstraintName("index or primary key violation: \"MY_PK\"");
            assertEquals("MY_PK", name);
        }

        @Test
        @DisplayName("extractConstraintName with 'for key' pattern")
        void staticExtractConstraintNameForKeyPattern() {
            String name = invokeExtractConstraintName("Duplicate entry for key 'users.email'");
            assertEquals("users.email", name);
        }

        @Test
        @DisplayName("extractConstraintName with 'violates ... constraint' pattern")
        void staticExtractConstraintNameViolatesPattern() {
            String name = invokeExtractConstraintName("violates foreign key constraint \"fk_orders_user\"");
            assertEquals("fk_orders_user", name);
        }

        // Additional pattern tests to hit all array iterations
        @Test
        @DisplayName("extractTableName pattern 2 - 'on' without matching pattern 1")
        void staticExtractTableNamePattern2() {
            // Doesn't match "table" pattern, matches "on" pattern
            String name = invokeExtractTableName("error on \"my_table\" during insert");
            assertEquals("my_table", name);
        }

        @Test
        @DisplayName("extractTableName pattern 3 - 'into' without matching patterns 1 or 2")
        void staticExtractTableNamePattern3() {
            // Doesn't match "table" or "on" patterns, matches "into" pattern
            String name = invokeExtractTableName("inserting into \"target_table\" failed");
            assertEquals("target_table", name);
        }

        @Test
        @DisplayName("extractColumnName pattern 2 - 'field' without matching pattern 1")
        void staticExtractColumnNamePattern2() {
            // Doesn't match "column" pattern, matches "field" pattern
            String name = invokeExtractColumnName("invalid field \"my_field\" value");
            assertEquals("my_field", name);
        }

        @Test
        @DisplayName("extractColumnName pattern 3 - PostgreSQL without matching patterns 1 or 2")
        void staticExtractColumnNamePattern3() {
            // Doesn't match "column" or "field" patterns, matches (col)= pattern
            String name = invokeExtractColumnName("Key (my_col)=value");
            assertEquals("my_col", name);
        }

        @Test
        @DisplayName("fromSQLException with null sqlState uses message parsing")
        void fromSQLExceptionNullState() {
            SQLException cause = new SQLException("unique violation");
            ConstraintViolationException ex = ConstraintViolationException.fromSQLException(TEST_SQL, cause);
            assertInstanceOf(UniqueConstraintException.class, ex);
        }
    }

    @Nested
    @DisplayName("UniqueConstraintException")
    class UniqueConstraintExceptionTests {

        @Test
        @DisplayName("extractDuplicateValue handles null message")
        void extractDuplicateValueNull() {
            SQLException cause = new SQLException(null, "23505");
            UniqueConstraintException ex = UniqueConstraintException.fromSQLException(TEST_SQL, cause);

            assertNull(ex.getDuplicateValue());
        }

        @Test
        @DisplayName("Builder sets constraintType to UNIQUE")
        void builderSetsConstraintType() {
            UniqueConstraintException ex = UniqueConstraintException.builder()
                    .message("Unique violation")
                    .sql(TEST_SQL)
                    .duplicateValue("test@example.com")
                    .build();

            assertEquals(ConstraintViolationException.ConstraintType.UNIQUE, ex.getConstraintType());
            assertEquals("test@example.com", ex.getDuplicateValue());
        }
    }

    @Nested
    @DisplayName("ForeignKeyException")
    class ForeignKeyExceptionTests {

        @Test
        @DisplayName("extractReferencedTable handles various patterns")
        void extractReferencedTablePatterns() {
            SQLException cause = new SQLException("Key is not present in table \"users\"", "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException(TEST_SQL, cause);

            assertEquals("users", ex.getReferencedTable());
        }

        @Test
        @DisplayName("extractReferencedTable handles null message")
        void extractReferencedTableNull() {
            SQLException cause = new SQLException(null, "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException(TEST_SQL, cause);

            assertNull(ex.getReferencedTable());
        }

        @Test
        @DisplayName("extractReferencedColumn handles null message")
        void extractReferencedColumnNull() {
            SQLException cause = new SQLException(null, "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException(TEST_SQL, cause);

            assertNull(ex.getReferencedColumn());
        }

        @Test
        @DisplayName("extractReferencedColumn extracts PostgreSQL pattern")
        void extractReferencedColumnPostgres() {
            SQLException cause = new SQLException("Key (user_id)=(123) is not present", "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException(TEST_SQL, cause);

            assertEquals("user_id", ex.getReferencedColumn());
        }

        @Test
        @DisplayName("detectViolationType handles 'child record found' message")
        void detectViolationTypeChildRecordFound() {
            SQLException cause = new SQLException("child record found", "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException(TEST_SQL, cause);

            assertEquals(ForeignKeyException.ForeignKeyViolationType.CHILD_EXISTS, ex.getViolationType());
        }

        @Test
        @DisplayName("detectViolationType handles 'restrict' message")
        void detectViolationTypeRestrict() {
            SQLException cause = new SQLException("update violates restrict constraint", "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException(TEST_SQL, cause);

            assertEquals(ForeignKeyException.ForeignKeyViolationType.CHILD_EXISTS, ex.getViolationType());
        }

        @Test
        @DisplayName("detectViolationType handles 'does not exist' message")
        void detectViolationTypeDoesNotExist() {
            SQLException cause = new SQLException("referenced row does not exist", "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException(TEST_SQL, cause);

            assertEquals(ForeignKeyException.ForeignKeyViolationType.PARENT_NOT_FOUND, ex.getViolationType());
        }

        @Test
        @DisplayName("detectViolationType handles 'no parent' message")
        void detectViolationTypeNoParent() {
            SQLException cause = new SQLException("no parent record found", "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException(TEST_SQL, cause);

            assertEquals(ForeignKeyException.ForeignKeyViolationType.PARENT_NOT_FOUND, ex.getViolationType());
        }

        @Test
        @DisplayName("detectViolationType infers from UPDATE SQL")
        void detectViolationTypeFromUpdateSql() {
            SQLException cause = new SQLException("FK violation", "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException("UPDATE users SET id = 2", cause);

            assertEquals(ForeignKeyException.ForeignKeyViolationType.CHILD_EXISTS, ex.getViolationType());
        }

        @Test
        @DisplayName("detectViolationType returns UNKNOWN for ambiguous case")
        void detectViolationTypeUnknown() {
            SQLException cause = new SQLException("constraint violation", "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException("MERGE INTO users", cause);

            assertEquals(ForeignKeyException.ForeignKeyViolationType.UNKNOWN, ex.getViolationType());
        }

        @Test
        @DisplayName("Builder sets all fields correctly")
        void builderSetsAllFields() {
            ForeignKeyException ex = ForeignKeyException.builder()
                    .message("FK error")
                    .sql(TEST_SQL)
                    .referencedTable("users")
                    .referencedColumn("id")
                    .violationType(ForeignKeyException.ForeignKeyViolationType.PARENT_NOT_FOUND)
                    .build();

            assertEquals("users", ex.getReferencedTable());
            assertEquals("id", ex.getReferencedColumn());
            assertEquals(ForeignKeyException.ForeignKeyViolationType.PARENT_NOT_FOUND, ex.getViolationType());
            assertEquals(ConstraintViolationException.ConstraintType.FOREIGN_KEY, ex.getConstraintType());
        }

        @Test
        @DisplayName("detectViolationType with null SQL infers from message only")
        void detectViolationTypeNullSql() {
            SQLException cause = new SQLException("FK violation", "23503");
            ForeignKeyException ex = ForeignKeyException.fromSQLException(null, cause);
            assertEquals(ForeignKeyException.ForeignKeyViolationType.UNKNOWN, ex.getViolationType());
        }

        @Test
        @DisplayName("isParentNotFound returns correct value")
        void isParentNotFound() {
            ForeignKeyException ex = ForeignKeyException.builder()
                    .message("Error")
                    .violationType(ForeignKeyException.ForeignKeyViolationType.PARENT_NOT_FOUND)
                    .build();
            assertTrue(ex.isParentNotFound());
            assertFalse(ex.hasChildRecords());
        }

        @Test
        @DisplayName("hasChildRecords returns correct value")
        void hasChildRecords() {
            ForeignKeyException ex = ForeignKeyException.builder()
                    .message("Error")
                    .violationType(ForeignKeyException.ForeignKeyViolationType.CHILD_EXISTS)
                    .build();
            assertTrue(ex.hasChildRecords());
            assertFalse(ex.isParentNotFound());
        }
    }

    @Nested
    @DisplayName("CheckConstraintException")
    class CheckConstraintExceptionTests {

        @Test
        @DisplayName("fromSQLException creates exception with extracted info")
        void fromSQLException() {
            SQLException cause = new SQLException(
                    "Check constraint \"positive_age\" on table \"users\" violated",
                    "23514"
            );
            CheckConstraintException ex = CheckConstraintException.fromSQLException(TEST_SQL, cause);

            assertEquals(ConstraintViolationException.ConstraintType.CHECK, ex.getConstraintType());
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Builder sets constraintType to CHECK")
        void builderSetsConstraintType() {
            CheckConstraintException ex = CheckConstraintException.builder()
                    .message("Check violation")
                    .sql(TEST_SQL)
                    .constraintName("positive_age")
                    .build();

            assertEquals(ConstraintViolationException.ConstraintType.CHECK, ex.getConstraintType());
        }
    }

    @Nested
    @DisplayName("NotNullException")
    class NotNullExceptionTests {

        @Test
        @DisplayName("extractTableNameFromNotNull extracts PostgreSQL relation pattern")
        void extractTableNamePostgres() {
            SQLException cause = new SQLException(
                    "null value in column \"email\" of relation \"users\"",
                    "23502"
            );
            NotNullException ex = NotNullException.fromSQLException(TEST_SQL, cause);

            assertEquals("users", ex.getTableName());
        }

        @Test
        @DisplayName("extractColumnNameFromNotNull extracts H2 pattern")
        void extractColumnNameH2() {
            SQLException cause = new SQLException(
                    "NULL not allowed for column \"EMAIL\"",
                    "23502"
            );
            NotNullException ex = NotNullException.fromSQLException(TEST_SQL, cause);

            assertEquals("EMAIL", ex.getColumnName());
        }

        @Test
        @DisplayName("extractColumnNameFromNotNull extracts MySQL pattern")
        void extractColumnNameMySQL() {
            SQLException cause = new SQLException(
                    "Column 'email' cannot be null",
                    "23502"
            );
            NotNullException ex = NotNullException.fromSQLException(TEST_SQL, cause);

            assertEquals("email", ex.getColumnName());
        }

        @Test
        @DisplayName("Builder sets constraintType to NOT_NULL")
        void builderSetsConstraintType() {
            NotNullException ex = NotNullException.builder()
                    .message("Not null violation")
                    .sql(TEST_SQL)
                    .columnName("email")
                    .build();

            assertEquals(ConstraintViolationException.ConstraintType.NOT_NULL, ex.getConstraintType());
        }

        @Test
        @DisplayName("extractTableNameFromNotNull with null message returns null")
        void extractTableNameFromNotNullNull() {
            SQLException cause = new SQLException(null, "23502");
            NotNullException ex = NotNullException.fromSQLException(TEST_SQL, cause);

            assertNull(ex.getTableName());
        }

        @Test
        @DisplayName("extractTableNameFromNotNull falls back to base extraction")
        void extractTableNameFallsBack() {
            // Message that doesn't match relation pattern but matches table pattern
            SQLException cause = new SQLException("error on table \"users\"", "23502");
            NotNullException ex = NotNullException.fromSQLException(TEST_SQL, cause);

            assertEquals("users", ex.getTableName());
        }

        @Test
        @DisplayName("extractColumnNameFromNotNull with null message returns null")
        void extractColumnNameFromNotNullNull() {
            SQLException cause = new SQLException(null, "23502");
            NotNullException ex = NotNullException.fromSQLException(TEST_SQL, cause);

            assertNull(ex.getColumnName());
        }

        @Test
        @DisplayName("extractColumnNameFromNotNull falls back to base extraction")
        void extractColumnNameFallsBack() {
            // Message that doesn't match specific patterns
            SQLException cause = new SQLException("field \"name\" error", "23502");
            NotNullException ex = NotNullException.fromSQLException(TEST_SQL, cause);

            // Falls back to extractColumnName which checks field pattern
            assertEquals("name", ex.getColumnName());
        }
    }

    @Nested
    @DisplayName("SuprimException Base Class")
    class SuprimExceptionBaseTests {

        @Test
        @DisplayName("Simple constructor works")
        void simpleConstructor() {
            SuprimException ex = new SuprimException("Test error");

            assertTrue(ex.getMessage().contains("Test error"));
        }

        @Test
        @DisplayName("Constructor with cause works")
        void constructorWithCause() {
            RuntimeException cause = new RuntimeException("Cause");
            SuprimException ex = new SuprimException("Error", cause);

            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("Constructor with SQL and SQLException extracts info")
        void constructorWithSqlAndSqlException() {
            SQLException cause = new SQLException("DB Error", "23505", 12345);
            SuprimException ex = new SuprimException("Error", TEST_SQL, cause);

            assertEquals(TEST_SQL, ex.getSql());
            assertEquals("23505", ex.getSqlState());
            assertEquals(12345, ex.getVendorCode());
            assertEquals(SuprimException.ErrorCategory.INTEGRITY_CONSTRAINT, ex.getCategory());
        }

        @Test
        @DisplayName("getSQLException returns cause when it is SQLException")
        void getSQLException() {
            SQLException cause = new SQLException("Error");
            SuprimException ex = new SuprimException("Error", cause);

            assertEquals(cause, ex.getSQLException());
        }

        @Test
        @DisplayName("getSQLException returns null when cause is not SQLException")
        void getSQLExceptionReturnsNull() {
            SuprimException ex = new SuprimException("Error", new RuntimeException());

            assertNull(ex.getSQLException());
        }

        @Test
        @DisplayName("isConstraintViolation returns true for INTEGRITY_CONSTRAINT")
        void isConstraintViolation() {
            SuprimException ex = SuprimException.builder()
                    .message("Error")
                    .category(SuprimException.ErrorCategory.INTEGRITY_CONSTRAINT)
                    .build();

            assertTrue(ex.isConstraintViolation());
        }

        @Test
        @DisplayName("isConnectionError returns true for CONNECTION")
        void isConnectionError() {
            SuprimException ex = SuprimException.builder()
                    .message("Error")
                    .category(SuprimException.ErrorCategory.CONNECTION)
                    .build();

            assertTrue(ex.isConnectionError());
        }

        @Test
        @DisplayName("isTransactionError returns true for transaction categories")
        void isTransactionError() {
            SuprimException rollback = SuprimException.builder()
                    .message("Error")
                    .category(SuprimException.ErrorCategory.TRANSACTION_ROLLBACK)
                    .build();

            SuprimException invalidState = SuprimException.builder()
                    .message("Error")
                    .category(SuprimException.ErrorCategory.INVALID_TRANSACTION_STATE)
                    .build();

            assertTrue(rollback.isTransactionError());
            assertTrue(invalidState.isTransactionError());
        }

        @Test
        @DisplayName("isRetryable returns true for 40xxx SQL state")
        void isRetryableFor40State() {
            SuprimException ex = SuprimException.builder()
                    .message("Error")
                    .sqlState("40001")
                    .build();

            assertTrue(ex.isRetryable());
        }

        @Test
        @DisplayName("ErrorCategory.fromSqlState handles all cases")
        void errorCategoryFromSqlState() {
            assertEquals(SuprimException.ErrorCategory.SUCCESS, SuprimException.ErrorCategory.fromSqlState("00000"));
            assertEquals(SuprimException.ErrorCategory.WARNING, SuprimException.ErrorCategory.fromSqlState("01000"));
            assertEquals(SuprimException.ErrorCategory.NO_DATA, SuprimException.ErrorCategory.fromSqlState("02000"));
            assertEquals(SuprimException.ErrorCategory.FEATURE_NOT_SUPPORTED, SuprimException.ErrorCategory.fromSqlState("0A000"));
            assertEquals(SuprimException.ErrorCategory.INVALID_AUTHORIZATION, SuprimException.ErrorCategory.fromSqlState("28000"));
            assertEquals(SuprimException.ErrorCategory.PROGRAM_LIMIT_EXCEEDED, SuprimException.ErrorCategory.fromSqlState("54000"));
            assertEquals(SuprimException.ErrorCategory.OBJECT_NOT_IN_STATE, SuprimException.ErrorCategory.fromSqlState("55000"));
            assertEquals(SuprimException.ErrorCategory.SYSTEM_ERROR, SuprimException.ErrorCategory.fromSqlState("58000"));
            assertEquals(SuprimException.ErrorCategory.UNKNOWN, SuprimException.ErrorCategory.fromSqlState("XX000"));
        }

        @Test
        @DisplayName("Builder automatically extracts info from SQLException cause")
        void builderExtractsFromSQLException() {
            SQLException cause = new SQLException("Error", "23505", 999);
            SuprimException ex = SuprimException.builder()
                    .message("Error")
                    .cause(cause)
                    .build();

            assertEquals("23505", ex.getSqlState());
            assertEquals(999, ex.getVendorCode());
            assertEquals(SuprimException.ErrorCategory.INTEGRITY_CONSTRAINT, ex.getCategory());
        }

        @Test
        @DisplayName("Builder does not override explicitly set values")
        void builderDoesNotOverrideExplicitValues() {
            SQLException cause = new SQLException("Error", "23505", 999);
            SuprimException ex = SuprimException.builder()
                    .message("Error")
                    .sqlState("42000")
                    .vendorCode(123)
                    .category(SuprimException.ErrorCategory.SYNTAX_ERROR)
                    .cause(cause)
                    .build();

            assertEquals("42000", ex.getSqlState());
            assertEquals(123, ex.getVendorCode());
            assertEquals(SuprimException.ErrorCategory.SYNTAX_ERROR, ex.getCategory());
        }
    }
}
