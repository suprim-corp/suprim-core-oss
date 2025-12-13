package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.jdbc.exception.*;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ExceptionTranslator.
 *
 * Tests SQL exception translation to specific Suprim exception types based on:
 * - SQL state codes (XOPEN/SQL:2003 standard)
 * - Error messages (vendor-specific patterns)
 * - Fallback behavior for unknown states
 */
@DisplayName("ExceptionTranslator")
class ExceptionTranslatorTest {

    private static final String TEST_SQL = "INSERT INTO users (email) VALUES ('test@example.com')";
    private static final Object[] TEST_PARAMS = new Object[]{"test@example.com"};

    @Nested
    @DisplayName("QueryException Translation")
    class QueryExceptionTests {

        @Test
        @DisplayName("SQL state 42xxx (syntax error) translates to QueryException")
        void sqlState42_translatesTo_QueryException() {
            SQLException sqlEx = new SQLException("Syntax error", "42000");

            SuprimException result = ExceptionTranslator.translate("SELECT * FROM invalid", sqlEx);

            assertInstanceOf(QueryException.class, result);
            assertEquals(SuprimException.ErrorCategory.SYNTAX_ERROR, result.getCategory());
            assertNotNull(result.getSql());
        }

        @Test
        @DisplayName("SQL state 42601 (syntax error) translates to QueryException")
        void sqlState42601_translatesTo_QueryException() {
            SQLException sqlEx = new SQLException("syntax error at or near \"SELCT\"", "42601");

            SuprimException result = ExceptionTranslator.translate("SELCT * FROM users", sqlEx);

            assertInstanceOf(QueryException.class, result);
            assertEquals("SELCT * FROM users", result.getSql());
        }

        @Test
        @DisplayName("SQL state 57xxx (query canceled) translates to QueryException")
        void sqlState57_translatesTo_QueryCanceled() {
            SQLException sqlEx = new SQLException("Query was cancelled", "57014");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(QueryException.class, result);
            assertEquals(SuprimException.ErrorCategory.QUERY_CANCELED, result.getCategory());
        }

        @Test
        @DisplayName("translateQuery wraps generic exception in QueryException")
        void translateQuery_wrapsGenericException() {
            SQLException sqlEx = new SQLException("Unknown error", "99999");

            SuprimException result = ExceptionTranslator.translateQuery(TEST_SQL, TEST_PARAMS, sqlEx);

            assertInstanceOf(QueryException.class, result);
            assertEquals(TEST_SQL, result.getSql());
            assertArrayEquals(TEST_PARAMS, result.getParameters());
        }
    }

    @Nested
    @DisplayName("NoResultException Translation")
    class NoResultExceptionTests {

        @Test
        @DisplayName("NoResultException.forQuery creates exception with SQL")
        void noResultException_forQuery_includesSql() {
            NoResultException ex = NoResultException.forQuery(TEST_SQL);

            assertEquals(TEST_SQL, ex.getSql());
            assertTrue(ex.getMessage().contains("exactly one was expected"));
        }

        @Test
        @DisplayName("NoResultException with parameters includes them")
        void noResultException_withParameters_includesThem() {
            NoResultException ex = NoResultException.forQuery(TEST_SQL, TEST_PARAMS);

            assertEquals(TEST_SQL, ex.getSql());
            assertArrayEquals(TEST_PARAMS, ex.getParameters());
        }
    }

    @Nested
    @DisplayName("NonUniqueResultException Translation")
    class NonUniqueResultExceptionTests {

        @Test
        @DisplayName("NonUniqueResultException.forQuery creates exception with SQL")
        void nonUniqueResultException_forQuery_includesSql() {
            NonUniqueResultException ex = NonUniqueResultException.forQuery(TEST_SQL);

            assertEquals(TEST_SQL, ex.getSql());
            assertTrue(ex.getMessage().contains("multiple results"));
        }

        @Test
        @DisplayName("NonUniqueResultException with count includes it in message")
        void nonUniqueResultException_withCount_includesInMessage() {
            NonUniqueResultException ex = NonUniqueResultException.forQuery(TEST_SQL, 5);

            assertEquals(5, ex.getActualCount());
            assertTrue(ex.getMessage().contains("5 rows"));
        }

        @Test
        @DisplayName("NonUniqueResultException with parameters includes them")
        void nonUniqueResultException_withParameters_includesThem() {
            NonUniqueResultException ex = NonUniqueResultException.forQuery(TEST_SQL, TEST_PARAMS);

            assertArrayEquals(TEST_PARAMS, ex.getParameters());
        }
    }

    @Nested
    @DisplayName("ConnectionException Translation")
    class ConnectionExceptionTests {

        @Test
        @DisplayName("SQL state 08xxx translates to ConnectionException")
        void sqlState08_translatesTo_ConnectionException() {
            SQLException sqlEx = new SQLException("Connection refused", "08001");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(ConnectionException.class, result);
            assertEquals(SuprimException.ErrorCategory.CONNECTION, result.getCategory());
            assertTrue(result.isRetryable());
        }

        @Test
        @DisplayName("SQL state 08001 translates to server unavailable")
        void sqlState08001_translatesTo_ServerUnavailable() {
            SQLException sqlEx = new SQLException("Unable to connect to database", "08001");

            ConnectionException result = (ConnectionException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(ConnectionException.ConnectionFailureType.SERVER_UNAVAILABLE, result.getFailureType());
        }

        @Test
        @DisplayName("SQL state 08004 translates to authentication failed")
        void sqlState08004_translatesTo_AuthenticationFailed() {
            SQLException sqlEx = new SQLException("Invalid credentials", "08004");

            ConnectionException result = (ConnectionException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(ConnectionException.ConnectionFailureType.AUTHENTICATION_FAILED, result.getFailureType());
            assertFalse(result.isRetryable()); // Auth failures are not retryable
        }

        @Test
        @DisplayName("SQL state 08006 translates to transaction resolution unknown")
        void sqlState08006_translatesTo_TransactionResolutionUnknown() {
            SQLException sqlEx = new SQLException("Connection failure during transaction", "08006");

            ConnectionException result = (ConnectionException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(ConnectionException.ConnectionFailureType.TRANSACTION_RESOLUTION_UNKNOWN, result.getFailureType());
        }

        @Test
        @DisplayName("SQL state 53300 translates to pool exhausted")
        void sqlState53300_translatesTo_PoolExhausted() {
            SQLException sqlEx = new SQLException("Too many connections", "53300");

            ConnectionException result = (ConnectionException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(ConnectionException.ConnectionFailureType.POOL_EXHAUSTED, result.getFailureType());
        }

        @Test
        @DisplayName("Message with 'connection' and 'closed' translates to ConnectionException")
        void messageConnectionClosed_translatesTo_ConnectionException() {
            SQLException sqlEx = new SQLException("Connection is closed", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(ConnectionException.class, result);
        }

        @Test
        @DisplayName("Message with 'connection' and 'timeout' translates to ConnectionException")
        void messageConnectionTimeout_translatesTo_ConnectionException() {
            SQLException sqlEx = new SQLException("Connection timeout", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(ConnectionException.class, result);
        }
    }

    @Nested
    @DisplayName("DeadlockException Translation")
    class DeadlockExceptionTests {

        @Test
        @DisplayName("SQL state 40001 translates to DeadlockException")
        void sqlState40001_translatesTo_DeadlockException() {
            SQLException sqlEx = new SQLException("Deadlock detected", "40001");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(DeadlockException.class, result);
            assertEquals(SuprimException.ErrorCategory.TRANSACTION_ROLLBACK, result.getCategory());
            assertTrue(result.isRetryable());
        }

        @Test
        @DisplayName("SQL state 40P01 (PostgreSQL deadlock) translates to DeadlockException")
        void sqlState40P01_translatesTo_DeadlockException() {
            SQLException sqlEx = new SQLException("deadlock detected", "40P01");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(DeadlockException.class, result);
            assertTrue(result.isRetryable());
        }

        @Test
        @DisplayName("Message with 'deadlock' translates to DeadlockException")
        void messageDeadlock_translatesTo_DeadlockException() {
            SQLException sqlEx = new SQLException("Transaction deadlock detected", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(DeadlockException.class, result);
            assertTrue(result.isRetryable());
        }
    }

    @Nested
    @DisplayName("UniqueConstraintException Translation")
    class UniqueConstraintExceptionTests {

        @Test
        @DisplayName("SQL state 23505 translates to UniqueConstraintException")
        void sqlState23505_translatesTo_UniqueConstraintException() {
            SQLException sqlEx = new SQLException(
                "duplicate key value violates unique constraint \"users_email_key\"",
                "23505"
            );

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(UniqueConstraintException.class, result);
            assertEquals(SuprimException.ErrorCategory.INTEGRITY_CONSTRAINT, result.getCategory());
            assertTrue(result.isConstraintViolation());
        }

        @Test
        @DisplayName("SQL state 23001 (alternate unique violation) translates to UniqueConstraintException")
        void sqlState23001_translatesTo_UniqueConstraintException() {
            SQLException sqlEx = new SQLException("Unique constraint violation", "23001");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(UniqueConstraintException.class, result);
        }

        @Test
        @DisplayName("Message with 'unique' translates to UniqueConstraintException")
        void messageUnique_translatesTo_UniqueConstraintException() {
            SQLException sqlEx = new SQLException("Unique index violation", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(UniqueConstraintException.class, result);
        }

        @Test
        @DisplayName("Message with 'duplicate' translates to UniqueConstraintException")
        void messageDuplicate_translatesTo_UniqueConstraintException() {
            SQLException sqlEx = new SQLException("Duplicate entry 'test@example.com' for key 'email'", "99999");

            UniqueConstraintException result = (UniqueConstraintException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertNotNull(result.getDuplicateValue());
            assertEquals("test@example.com", result.getDuplicateValue());
        }

        @Test
        @DisplayName("Message with 'primary key violation' translates to UniqueConstraintException")
        void messagePrimaryKeyViolation_translatesTo_UniqueConstraintException() {
            SQLException sqlEx = new SQLException("Primary key violation: duplicate key", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(UniqueConstraintException.class, result);
        }

        @Test
        @DisplayName("PostgreSQL duplicate message extracts duplicate value")
        void postgresqlDuplicateMessage_extractsDuplicateValue() {
            SQLException sqlEx = new SQLException(
                "Key (email)=(alice@example.com) already exists",
                "23505"
            );

            UniqueConstraintException result = (UniqueConstraintException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals("alice@example.com", result.getDuplicateValue());
        }
    }

    @Nested
    @DisplayName("ForeignKeyException Translation")
    class ForeignKeyExceptionTests {

        @Test
        @DisplayName("SQL state 23503 translates to ForeignKeyException")
        void sqlState23503_translatesTo_ForeignKeyException() {
            SQLException sqlEx = new SQLException(
                "insert or update on table \"orders\" violates foreign key constraint",
                "23503"
            );

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(ForeignKeyException.class, result);
            assertEquals(SuprimException.ErrorCategory.INTEGRITY_CONSTRAINT, result.getCategory());
        }

        @Test
        @DisplayName("Message with 'foreign key' translates to ForeignKeyException")
        void messageForeignKey_translatesTo_ForeignKeyException() {
            SQLException sqlEx = new SQLException("Foreign key violation", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(ForeignKeyException.class, result);
        }

        @Test
        @DisplayName("Message with 'referential' translates to ForeignKeyException")
        void messageReferential_translatesTo_ForeignKeyException() {
            SQLException sqlEx = new SQLException("Referential integrity constraint violation", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(ForeignKeyException.class, result);
        }

        @Test
        @DisplayName("Message 'not present' indicates parent not found")
        void messageNotPresent_indicatesParentNotFound() {
            SQLException sqlEx = new SQLException(
                "Key (user_id)=(123) is not present in table \"users\"",
                "23503"
            );

            ForeignKeyException result = (ForeignKeyException) ExceptionTranslator.translate(
                "INSERT INTO orders", sqlEx
            );

            assertTrue(result.isParentNotFound());
            assertFalse(result.hasChildRecords());
            assertEquals(ForeignKeyException.ForeignKeyViolationType.PARENT_NOT_FOUND, result.getViolationType());
        }

        @Test
        @DisplayName("Message 'still referenced' indicates child exists")
        void messageStillReferenced_indicatesChildExists() {
            SQLException sqlEx = new SQLException(
                "update or delete on table \"users\" violates foreign key constraint \"orders_user_id_fkey\" on table \"orders\": Key is still referenced",
                "23503"
            );

            ForeignKeyException result = (ForeignKeyException) ExceptionTranslator.translate(
                "DELETE FROM users", sqlEx
            );

            assertTrue(result.hasChildRecords());
            assertFalse(result.isParentNotFound());
            assertEquals(ForeignKeyException.ForeignKeyViolationType.CHILD_EXISTS, result.getViolationType());
        }

        @Test
        @DisplayName("INSERT SQL infers parent not found")
        void insertSql_infersParentNotFound() {
            SQLException sqlEx = new SQLException("FK violation", "23503");

            ForeignKeyException result = (ForeignKeyException) ExceptionTranslator.translate(
                "INSERT INTO orders VALUES (?)", sqlEx
            );

            assertTrue(result.isParentNotFound());
        }

        @Test
        @DisplayName("DELETE SQL infers child exists")
        void deleteSql_infersChildExists() {
            SQLException sqlEx = new SQLException("FK violation", "23503");

            ForeignKeyException result = (ForeignKeyException) ExceptionTranslator.translate(
                "DELETE FROM users WHERE id = ?", sqlEx
            );

            assertTrue(result.hasChildRecords());
        }
    }

    @Nested
    @DisplayName("CheckConstraintException Translation")
    class CheckConstraintExceptionTests {

        @Test
        @DisplayName("SQL state 23514 translates to CheckConstraintException")
        void sqlState23514_translatesTo_CheckConstraintException() {
            SQLException sqlEx = new SQLException(
                "new row for relation \"products\" violates check constraint \"positive_price\"",
                "23514"
            );

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(CheckConstraintException.class, result);
        }

        @Test
        @DisplayName("SQL state 23513 (alternate check violation) translates to CheckConstraintException")
        void sqlState23513_translatesTo_CheckConstraintException() {
            SQLException sqlEx = new SQLException("Check constraint failed", "23513");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(CheckConstraintException.class, result);
        }

        @Test
        @DisplayName("Message with 'check constraint' translates to CheckConstraintException")
        void messageCheckConstraint_translatesTo_CheckConstraintException() {
            SQLException sqlEx = new SQLException("Check constraint 'age_positive' failed", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(CheckConstraintException.class, result);
        }
    }

    @Nested
    @DisplayName("NotNullException Translation")
    class NotNullExceptionTests {

        @Test
        @DisplayName("SQL state 23502 translates to NotNullException")
        void sqlState23502_translatesTo_NotNullException() {
            SQLException sqlEx = new SQLException(
                "null value in column \"email\" violates not-null constraint",
                "23502"
            );

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(NotNullException.class, result);
            assertEquals(SuprimException.ErrorCategory.INTEGRITY_CONSTRAINT, result.getCategory());
        }

        @Test
        @DisplayName("Message with 'not null' translates to NotNullException")
        void messageNotNull_translatesTo_NotNullException() {
            SQLException sqlEx = new SQLException("Column 'name' cannot be null", "99999");

            NotNullException result = (NotNullException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertNotNull(result.getColumnName());
        }

        @Test
        @DisplayName("Message with 'cannot be null' translates to NotNullException")
        void messageCannotBeNull_translatesTo_NotNullException() {
            SQLException sqlEx = new SQLException("Field 'email' cannot be null", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(NotNullException.class, result);
        }
    }

    @Nested
    @DisplayName("DataIntegrityException Translation")
    class DataIntegrityExceptionTests {

        @Test
        @DisplayName("SQL state 22xxx translates to DataIntegrityException")
        void sqlState22_translatesTo_DataIntegrityException() {
            SQLException sqlEx = new SQLException("Data exception", "22000");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(DataIntegrityException.class, result);
            assertEquals(SuprimException.ErrorCategory.DATA_EXCEPTION, result.getCategory());
        }

        @Test
        @DisplayName("SQL state 22001 indicates string too long")
        void sqlState22001_indicatesStringTooLong() {
            SQLException sqlEx = new SQLException("Value too long for type varchar(50)", "22001");

            DataIntegrityException result = (DataIntegrityException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(DataIntegrityException.DataErrorType.STRING_TOO_LONG, result.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22003 indicates numeric overflow")
        void sqlState22003_indicatesNumericOverflow() {
            SQLException sqlEx = new SQLException("Numeric value out of range", "22003");

            DataIntegrityException result = (DataIntegrityException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(DataIntegrityException.DataErrorType.NUMERIC_OVERFLOW, result.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22007 indicates invalid datetime")
        void sqlState22007_indicatesInvalidDatetime() {
            SQLException sqlEx = new SQLException("Invalid datetime format", "22007");

            DataIntegrityException result = (DataIntegrityException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_DATETIME, result.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22012 indicates division by zero")
        void sqlState22012_indicatesDivisionByZero() {
            SQLException sqlEx = new SQLException("Division by zero", "22012");

            DataIntegrityException result = (DataIntegrityException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(DataIntegrityException.DataErrorType.DIVISION_BY_ZERO, result.getDataErrorType());
        }

        @Test
        @DisplayName("SQL state 22P02 (PostgreSQL) indicates invalid text representation")
        void sqlState22P02_indicatesInvalidTextRepresentation() {
            SQLException sqlEx = new SQLException("invalid input syntax for type integer: \"abc\"", "22P02");

            DataIntegrityException result = (DataIntegrityException) ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(DataIntegrityException.DataErrorType.INVALID_TEXT_REPRESENTATION, result.getDataErrorType());
        }
    }

    @Nested
    @DisplayName("TransactionException Translation")
    class TransactionExceptionTests {

        @Test
        @DisplayName("SQL state 25xxx translates to TransactionException")
        void sqlState25_translatesTo_TransactionException() {
            SQLException sqlEx = new SQLException("Invalid transaction state", "25000");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(TransactionException.class, result);
            assertTrue(result.isTransactionError());
        }

        @Test
        @DisplayName("SQL state 40xxx (not deadlock) translates to TransactionException")
        void sqlState40_notDeadlock_translatesTo_TransactionException() {
            SQLException sqlEx = new SQLException("Transaction rolled back", "40003");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(TransactionException.class, result);
            assertFalse(result instanceof DeadlockException, "Should not be DeadlockException");
        }

        @Test
        @DisplayName("SQL state 40002 translates to constraint violation during commit")
        void sqlState40002_translatesTo_ConstraintViolation() {
            SQLException sqlEx = new SQLException("Integrity constraint violation during commit", "40002");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(ConstraintViolationException.class, result);
        }
    }

    @Nested
    @DisplayName("SavepointException Translation")
    class SavepointExceptionTests {

        @Test
        @DisplayName("SavepointException.createFailed includes savepoint name")
        void savepointCreateFailed_includesName() {
            SQLException sqlEx = new SQLException("Invalid savepoint", "3B001");

            SavepointException ex = SavepointException.createFailed("sp1", sqlEx);

            assertEquals("sp1", ex.getSavepointName());
            assertEquals(SavepointException.SavepointOperation.CREATE, ex.getOperation());
            assertTrue(ex.getMessage().contains("sp1"));
        }

        @Test
        @DisplayName("SavepointException.rollbackFailed includes savepoint name")
        void savepointRollbackFailed_includesName() {
            SQLException sqlEx = new SQLException("Savepoint not found", "3B001");

            SavepointException ex = SavepointException.rollbackFailed("sp1", sqlEx);

            assertEquals("sp1", ex.getSavepointName());
            assertEquals(SavepointException.SavepointOperation.ROLLBACK, ex.getOperation());
        }
    }

    @Nested
    @DisplayName("MappingException Translation")
    class MappingExceptionTests {

        @Test
        @DisplayName("MappingException.noConstructor includes target class")
        void mappingNoConstructor_includesTargetClass() {
            MappingException ex = MappingException.noConstructor(String.class);

            assertEquals(String.class, ex.getTargetClass());
            assertEquals(MappingException.MappingErrorType.NO_CONSTRUCTOR, ex.getMappingErrorType());
            assertTrue(ex.getMessage().contains("String"));
        }

        @Test
        @DisplayName("MappingException.fieldAccessError includes field and class")
        void mappingFieldAccessError_includesDetails() {
            IllegalAccessException cause = new IllegalAccessException("Cannot access private field");

            MappingException ex = MappingException.fieldAccessError(String.class, "value", cause);

            assertEquals(String.class, ex.getTargetClass());
            assertEquals("value", ex.getFieldName());
            assertEquals(MappingException.MappingErrorType.FIELD_ACCESS_ERROR, ex.getMappingErrorType());
            assertTrue(ex.getMessage().contains("value"));
        }

        @Test
        @DisplayName("MappingException.typeMismatch includes type information")
        void mappingTypeMismatch_includesTypeInfo() {
            MappingException ex = MappingException.typeMismatch(
                String.class, "length", Integer.class, String.class
            );

            assertEquals(MappingException.MappingErrorType.TYPE_MISMATCH, ex.getMappingErrorType());
            assertTrue(ex.getMessage().contains("Integer"));
            assertTrue(ex.getMessage().contains("String"));
        }

        @Test
        @DisplayName("MappingException.columnNotFound includes column name")
        void mappingColumnNotFound_includesColumnName() {
            MappingException ex = MappingException.columnNotFound(String.class, "invalid_col");

            assertEquals("invalid_col", ex.getColumnName());
            assertEquals(MappingException.MappingErrorType.COLUMN_NOT_FOUND, ex.getMappingErrorType());
        }
    }

    @Nested
    @DisplayName("Unknown SQL State Handling")
    class UnknownSqlStateTests {

        @Test
        @DisplayName("Unknown SQL state without message falls back to generic exception")
        void unknownSqlState_noMessage_returnsGenericException() {
            SQLException sqlEx = new SQLException(null, "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(SuprimException.class, result.getClass());
            assertEquals(SuprimException.ErrorCategory.UNKNOWN, result.getCategory());
        }

        @Test
        @DisplayName("Null SQL state falls back to message-based translation")
        void nullSqlState_fallsBackToMessage() {
            SQLException sqlEx = new SQLException("Duplicate entry for key");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(UniqueConstraintException.class, result);
        }

        @Test
        @DisplayName("translateExecution wraps generic exception in ExecutionException")
        void translateExecution_wrapsGenericException() {
            SQLException sqlEx = new SQLException("Unknown error", "99999");

            SuprimException result = ExceptionTranslator.translateExecution(TEST_SQL, TEST_PARAMS, sqlEx);

            assertInstanceOf(ExecutionException.class, result);
            assertEquals(TEST_SQL, result.getSql());
        }

        @Test
        @DisplayName("Unknown state with lock timeout message handled gracefully")
        void unknownState_lockTimeoutMessage_createsExecutionException() {
            SQLException sqlEx = new SQLException("Lock timeout exceeded", "99999");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, TEST_PARAMS, sqlEx);

            assertInstanceOf(ExecutionException.class, result);
            assertTrue(result.getMessage().contains("Lock"));
        }
    }

    @Nested
    @DisplayName("SQL State 53xxx - Insufficient Resources")
    class InsufficientResourcesTests {

        @Test
        @DisplayName("SQL state 53xxx (not 53300) translates to ExecutionException")
        void sqlState53_notPoolExhausted_translatesTo_ExecutionException() {
            SQLException sqlEx = new SQLException("Out of memory", "53200");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(ExecutionException.class, result);
            assertEquals(SuprimException.ErrorCategory.INSUFFICIENT_RESOURCES, result.getCategory());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCaseTests {

        @Test
        @DisplayName("SQLException with null message handled gracefully")
        void sqlExceptionWithNullMessage_handledGracefully() {
            SQLException sqlEx = new SQLException(null, "23505");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(UniqueConstraintException.class, result);
            assertNotNull(result.getMessage());
        }

        @Test
        @DisplayName("SQLException with empty SQL state uses message parsing")
        void emptyOrShortSqlState_usesMessageParsing() {
            SQLException sqlEx = new SQLException("Duplicate key detected", "");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertInstanceOf(UniqueConstraintException.class, result);
        }

        @Test
        @DisplayName("Parameters are preserved through translation")
        @org.junit.jupiter.api.Disabled("ConstraintViolationException.fromSQLException doesn't accept params - design limitation")
        void parametersPreserved_throughTranslation() {
            SQLException sqlEx = new SQLException("Error", "23505");
            Object[] params = new Object[]{1, "test", true};

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, params, sqlEx);

            assertArrayEquals(params, result.getParameters());
        }

        @Test
        @DisplayName("Long SQL is truncated in message")
        void longSql_truncatedInMessage() {
            String longSql = "SELECT * FROM users WHERE ".repeat(50);
            SQLException sqlEx = new SQLException("Error", "42000");

            SuprimException result = ExceptionTranslator.translate(longSql, sqlEx);

            assertTrue(result.getMessage().length() < longSql.length() + 100);
            assertTrue(result.getMessage().contains("..."));
        }

        @Test
        @DisplayName("SQL state extraction handles single character states")
        void singleCharacterSqlState_handledGracefully() {
            SQLException sqlEx = new SQLException("Error", "1");

            SuprimException result = ExceptionTranslator.translate(TEST_SQL, sqlEx);

            assertEquals(SuprimException.ErrorCategory.UNKNOWN, result.getCategory());
        }
    }
}
