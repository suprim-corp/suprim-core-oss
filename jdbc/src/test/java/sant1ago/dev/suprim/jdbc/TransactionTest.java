package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.jdbc.event.EventDispatcher;
import sant1ago.dev.suprim.jdbc.event.TransactionEvent;
import sant1ago.dev.suprim.jdbc.exception.NoResultException;
import sant1ago.dev.suprim.jdbc.exception.NonUniqueResultException;
import sant1ago.dev.suprim.jdbc.exception.SavepointException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for Transaction operations.
 */
@DisplayName("Transaction Tests")
@ExtendWith(MockitoExtension.class)
class TransactionTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private EventDispatcher mockDispatcher;

    @Mock
    private TransactionEvent mockTransactionEvent;

    @Mock
    private Savepoint mockSavepoint;

    private Transaction transaction;

    // Simple QueryResult for testing
    private QueryResult createQueryResult(String sql, Object... params) {
        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            paramMap.put("p" + i, params[i]);
        }
        return new QueryResult(sql, paramMap);
    }

    // ==================== CONSTRUCTOR TESTS ====================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("creates transaction with connection only")
        void createsWithConnectionOnly() {
            Transaction tx = new Transaction(mockConnection);
            assertNotNull(tx);
            assertEquals(mockConnection, tx.getConnection());
        }

        @Test
        @DisplayName("creates transaction with all parameters")
        void createsWithAllParameters() {
            Transaction tx = new Transaction(mockConnection, mockDispatcher, "test-connection", mockTransactionEvent);
            assertNotNull(tx);
            assertEquals(mockConnection, tx.getConnection());
        }

        @Test
        @DisplayName("creates transaction with null dispatcher")
        void createsWithNullDispatcher() {
            Transaction tx = new Transaction(mockConnection, null, "test-connection", mockTransactionEvent);
            assertNotNull(tx);
        }

        @Test
        @DisplayName("creates transaction with null connection name")
        void createsWithNullConnectionName() {
            Transaction tx = new Transaction(mockConnection, mockDispatcher, null, mockTransactionEvent);
            assertNotNull(tx);
        }

        @Test
        @DisplayName("throws for null connection")
        void throwsForNullConnection() {
            assertThrows(NullPointerException.class, () -> new Transaction(null));
        }
    }

    // ==================== QUERY TESTS ====================

    @Nested
    @DisplayName("query()")
    class QueryTests {

        @BeforeEach
        void setup() {
            transaction = new Transaction(mockConnection, mockDispatcher, "test", null);
        }

        @Test
        @DisplayName("returns list of mapped results")
        void returnsListOfMappedResults() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getString("name")).thenReturn("Alice", "Bob");

            QueryResult qr = createQueryResult("SELECT * FROM users WHERE active = ?", true);

            List<String> results = transaction.query(qr, rs -> rs.getString("name"));

            assertEquals(2, results.size());
            assertEquals("Alice", results.get(0));
            assertEquals("Bob", results.get(1));
        }

        @Test
        @DisplayName("returns empty list when no results")
        void returnsEmptyListWhenNoResults() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            QueryResult qr = createQueryResult("SELECT * FROM users");

            List<String> results = transaction.query(qr, rs -> rs.getString("name"));

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("fires query events")
        void firesQueryEvents() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            QueryResult qr = createQueryResult("SELECT 1");
            transaction.query(qr, rs -> rs.getString(1));

            verify(mockDispatcher).fireBeforeQuery(any());
            verify(mockDispatcher).fireAfterQuery(any());
        }

        @Test
        @DisplayName("fires error event on SQLException")
        void firesErrorEventOnSqlException() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

            QueryResult qr = createQueryResult("SELECT * FROM invalid");

            assertThrows(Exception.class, () -> transaction.query(qr, rs -> rs.getString(1)));
            verify(mockDispatcher).fireQueryError(any());
        }
    }

    // ==================== QUERY ONE TESTS ====================

    @Nested
    @DisplayName("queryOne()")
    class QueryOneTests {

        @BeforeEach
        void setup() {
            transaction = new Transaction(mockConnection, mockDispatcher, "test", null);
        }

        @Test
        @DisplayName("returns optional with result")
        void returnsOptionalWithResult() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("name")).thenReturn("Alice");

            QueryResult qr = createQueryResult("SELECT * FROM users WHERE id = ?", 1);

            Optional<String> result = transaction.queryOne(qr, rs -> rs.getString("name"));

            assertTrue(result.isPresent());
            assertEquals("Alice", result.get());
        }

        @Test
        @DisplayName("returns empty optional when no results")
        void returnsEmptyOptionalWhenNoResults() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            QueryResult qr = createQueryResult("SELECT * FROM users WHERE id = ?", 999);

            Optional<String> result = transaction.queryOne(qr, rs -> rs.getString("name"));

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("throws NonUniqueResultException for multiple results")
        void throwsForMultipleResults() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, true);
            when(mockResultSet.getString("name")).thenReturn("Alice");

            QueryResult qr = createQueryResult("SELECT * FROM users");

            assertThrows(NonUniqueResultException.class, () ->
                transaction.queryOne(qr, rs -> rs.getString("name")));
        }
    }

    // ==================== QUERY ONE REQUIRED TESTS ====================

    @Nested
    @DisplayName("queryOneRequired()")
    class QueryOneRequiredTests {

        @BeforeEach
        void setup() {
            transaction = new Transaction(mockConnection, mockDispatcher, "test", null);
        }

        @Test
        @DisplayName("returns result when exactly one row")
        void returnsResultWhenExactlyOneRow() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("name")).thenReturn("Alice");

            QueryResult qr = createQueryResult("SELECT * FROM users WHERE id = ?", 1);

            String result = transaction.queryOneRequired(qr, rs -> rs.getString("name"));

            assertEquals("Alice", result);
        }

        @Test
        @DisplayName("throws NoResultException when no results")
        void throwsNoResultExceptionWhenNoResults() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            QueryResult qr = createQueryResult("SELECT * FROM users WHERE id = ?", 999);

            assertThrows(NoResultException.class, () ->
                transaction.queryOneRequired(qr, rs -> rs.getString("name")));
        }

        @Test
        @DisplayName("throws NonUniqueResultException for multiple results")
        void throwsForMultipleResults() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, true);
            when(mockResultSet.getString("name")).thenReturn("Alice");

            QueryResult qr = createQueryResult("SELECT * FROM users");

            assertThrows(NonUniqueResultException.class, () ->
                transaction.queryOneRequired(qr, rs -> rs.getString("name")));
        }
    }

    // ==================== EXECUTE TESTS ====================

    @Nested
    @DisplayName("execute()")
    class ExecuteTests {

        @BeforeEach
        void setup() {
            transaction = new Transaction(mockConnection, mockDispatcher, "test", null);
        }

        @Test
        @DisplayName("returns affected row count")
        void returnsAffectedRowCount() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(5);

            QueryResult qr = createQueryResult("UPDATE users SET active = ?", true);

            int affected = transaction.execute(qr);

            assertEquals(5, affected);
        }

        @Test
        @DisplayName("fires query events")
        void firesQueryEvents() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            QueryResult qr = createQueryResult("DELETE FROM users WHERE id = ?", 1);
            transaction.execute(qr);

            verify(mockDispatcher).fireBeforeQuery(any());
            verify(mockDispatcher).fireAfterQuery(any());
        }

        @Test
        @DisplayName("fires error event on SQLException")
        void firesErrorEventOnSqlException() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Execution failed"));

            QueryResult qr = createQueryResult("DELETE FROM nonexistent");

            assertThrows(Exception.class, () -> transaction.execute(qr));
            verify(mockDispatcher).fireQueryError(any());
        }
    }

    // ==================== SAVEPOINT TESTS ====================

    @Nested
    @DisplayName("Savepoint Operations")
    class SavepointTests {

        @BeforeEach
        void setup() {
            transaction = new Transaction(mockConnection, mockDispatcher, "test", mockTransactionEvent);
        }

        @Test
        @DisplayName("creates savepoint")
        void createsSavepoint() throws SQLException {
            when(mockConnection.setSavepoint("sp1")).thenReturn(mockSavepoint);
            when(mockTransactionEvent.savepointCreated("sp1")).thenReturn(mockTransactionEvent);

            Savepoint sp = transaction.savepoint("sp1");

            assertNotNull(sp);
            verify(mockConnection).setSavepoint("sp1");
            verify(mockDispatcher).fireTransactionEvent(any());
        }

        @Test
        @DisplayName("throws SavepointException on create failure")
        void throwsOnCreateFailure() throws SQLException {
            when(mockConnection.setSavepoint("sp1")).thenThrow(new SQLException("Savepoint failed"));

            assertThrows(SavepointException.class, () -> transaction.savepoint("sp1"));
        }

        @Test
        @DisplayName("rolls back to savepoint")
        void rollsBackToSavepoint() throws SQLException {
            when(mockSavepoint.getSavepointName()).thenReturn("sp1");
            when(mockTransactionEvent.savepointRollback("sp1")).thenReturn(mockTransactionEvent);

            transaction.rollbackTo(mockSavepoint);

            verify(mockConnection).rollback(mockSavepoint);
            verify(mockDispatcher).fireTransactionEvent(any());
        }

        @Test
        @DisplayName("throws SavepointException on rollback failure")
        void throwsOnRollbackFailure() throws SQLException {
            when(mockSavepoint.getSavepointName()).thenReturn("sp1");
            doThrow(new SQLException("Rollback failed")).when(mockConnection).rollback(mockSavepoint);

            assertThrows(SavepointException.class, () -> transaction.rollbackTo(mockSavepoint));
        }

        @Test
        @DisplayName("releases savepoint")
        void releasesSavepoint() throws SQLException {
            when(mockSavepoint.getSavepointName()).thenReturn("sp1");
            when(mockTransactionEvent.savepointReleased("sp1")).thenReturn(mockTransactionEvent);

            transaction.releaseSavepoint(mockSavepoint);

            verify(mockConnection).releaseSavepoint(mockSavepoint);
            verify(mockDispatcher).fireTransactionEvent(any());
        }

        @Test
        @DisplayName("throws SavepointException on release failure")
        void throwsOnReleaseFailure() throws SQLException {
            when(mockSavepoint.getSavepointName()).thenReturn("sp1");
            doThrow(new SQLException("Release failed")).when(mockConnection).releaseSavepoint(mockSavepoint);

            assertThrows(SavepointException.class, () -> transaction.releaseSavepoint(mockSavepoint));
        }

        @Test
        @DisplayName("handles getSavepointName exception")
        void handlesSavepointNameException() throws SQLException {
            when(mockSavepoint.getSavepointName()).thenThrow(new SQLException("No name"));
            doThrow(new SQLException("Rollback failed")).when(mockConnection).rollback(mockSavepoint);

            assertThrows(SavepointException.class, () -> transaction.rollbackTo(mockSavepoint));
        }
    }

    // ==================== SAVEPOINT WITHOUT EVENT TESTS ====================

    @Nested
    @DisplayName("Savepoint Operations Without Event")
    class SavepointWithoutEventTests {

        @BeforeEach
        void setup() {
            transaction = new Transaction(mockConnection, mockDispatcher, "test", null);
        }

        @Test
        @DisplayName("creates savepoint without firing event")
        void createsSavepointWithoutEvent() throws SQLException {
            when(mockConnection.setSavepoint("sp1")).thenReturn(mockSavepoint);

            Savepoint sp = transaction.savepoint("sp1");

            assertNotNull(sp);
            verify(mockDispatcher, never()).fireTransactionEvent(any());
        }

        @Test
        @DisplayName("rolls back without firing event")
        void rollsBackWithoutEvent() throws SQLException {
            when(mockSavepoint.getSavepointName()).thenReturn("sp1");

            transaction.rollbackTo(mockSavepoint);

            verify(mockDispatcher, never()).fireTransactionEvent(any());
        }

        @Test
        @DisplayName("releases without firing event")
        void releasesWithoutEvent() throws SQLException {
            when(mockSavepoint.getSavepointName()).thenReturn("sp1");

            transaction.releaseSavepoint(mockSavepoint);

            verify(mockDispatcher, never()).fireTransactionEvent(any());
        }
    }

    // ==================== RELATIONSHIPS TESTS ====================

    @Nested
    @DisplayName("relationships()")
    class RelationshipsTests {

        @BeforeEach
        void setup() {
            transaction = new Transaction(mockConnection);
        }

        @Test
        @DisplayName("returns RelationshipManager")
        void returnsRelationshipManager() {
            RelationshipManager rm = transaction.relationships();
            assertNotNull(rm);
        }
    }

    // ==================== SAVE ALL TESTS ====================

    @Nested
    @DisplayName("saveAll()")
    class SaveAllTests {

        @BeforeEach
        void setup() {
            transaction = new Transaction(mockConnection);
        }

        @Test
        @DisplayName("returns empty list for null input")
        void returnsEmptyListForNullInput() {
            List<Object> result = transaction.saveAll(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty input")
        void returnsEmptyListForEmptyInput() {
            List<Object> result = transaction.saveAll(new ArrayList<>());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for null input with dialect")
        void returnsEmptyListForNullInputWithDialect() {
            List<Object> result = transaction.saveAll(null, PostgreSqlDialect.INSTANCE);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty input with dialect")
        void returnsEmptyListForEmptyInputWithDialect() {
            List<Object> result = transaction.saveAll(new ArrayList<>(), PostgreSqlDialect.INSTANCE);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for null input with batch size")
        void returnsEmptyListForNullInputWithBatchSize() {
            List<Object> result = transaction.saveAll(null, 100, PostgreSqlDialect.INSTANCE);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty input with batch size")
        void returnsEmptyListForEmptyInputWithBatchSize() {
            List<Object> result = transaction.saveAll(new ArrayList<>(), 100, PostgreSqlDialect.INSTANCE);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== UPSERT ALL TESTS ====================

    @Nested
    @DisplayName("upsertAll()")
    class UpsertAllTests {

        @BeforeEach
        void setup() {
            transaction = new Transaction(mockConnection);
        }

        @Test
        @DisplayName("returns empty list for null input")
        void returnsEmptyListForNullInput() {
            List<Object> result = transaction.upsertAll(null, new String[]{"id"}, null, PostgreSqlDialect.INSTANCE);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty input")
        void returnsEmptyListForEmptyInput() {
            List<Object> result = transaction.upsertAll(new ArrayList<>(), new String[]{"id"}, null, PostgreSqlDialect.INSTANCE);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== GET CONNECTION TESTS ====================

    @Nested
    @DisplayName("getConnection()")
    class GetConnectionTests {

        @Test
        @DisplayName("returns the underlying connection")
        void returnsUnderlyingConnection() {
            Transaction tx = new Transaction(mockConnection);
            assertEquals(mockConnection, tx.getConnection());
        }
    }
}
