package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.jdbc.exception.NoResultException;
import sant1ago.dev.suprim.jdbc.exception.NonUniqueResultException;
import sant1ago.dev.suprim.jdbc.exception.SavepointException;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Transaction class - transaction context operations.
 */
@DisplayName("Transaction Tests")
class TransactionTest {

    private JdbcDataSource dataSource;
    private SuprimExecutor executor;
    private Connection setupConnection;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:txtest;DB_CLOSE_DELAY=-1");

        setupConnection = dataSource.getConnection();
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id VARCHAR(36) PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    name VARCHAR(100),
                    is_active BOOLEAN DEFAULT TRUE
                )
                """);
        }

        executor = SuprimExecutor.create(dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
        }
        setupConnection.close();
    }

    // ==================== queryOneRequired Tests ====================

    @Nested
    @DisplayName("queryOneRequired Tests")
    class QueryOneRequiredTests {

        @Test
        @DisplayName("queryOneRequired returns single result when exactly one row exists")
        void queryOneRequired_singleRow_returnsResult() {
            insertTestUser("alice@example.com", "Alice");

            executor.transaction(tx -> {
                QueryResult query = new QueryResult(
                    "SELECT email, name FROM users WHERE email = :p1",
                    Map.of("p1", "alice@example.com")
                );

                String name = tx.queryOneRequired(query, rs -> rs.getString("name"));
                assertEquals("Alice", name);
            });
        }

        @Test
        @DisplayName("queryOneRequired throws NoResultException when no rows")
        void queryOneRequired_noRows_throwsNoResultException() {
            executor.transaction(tx -> {
                QueryResult query = new QueryResult(
                    "SELECT email FROM users WHERE email = :p1",
                    Map.of("p1", "nonexistent@example.com")
                );

                assertThrows(NoResultException.class, () ->
                    tx.queryOneRequired(query, rs -> rs.getString("email"))
                );
            });
        }

        @Test
        @DisplayName("queryOneRequired throws NonUniqueResultException when multiple rows")
        void queryOneRequired_multipleRows_throwsNonUniqueResultException() {
            insertTestUser("alice@example.com", "Alice");
            insertTestUser("bob@example.com", "Bob");

            executor.transaction(tx -> {
                QueryResult query = new QueryResult(
                    "SELECT email FROM users",
                    Map.of()
                );

                assertThrows(NonUniqueResultException.class, () ->
                    tx.queryOneRequired(query, rs -> rs.getString("email"))
                );
            });
        }
    }

    // ==================== Savepoint Tests ====================

    @Nested
    @DisplayName("Savepoint Tests")
    class SavepointTests {

        @Test
        @DisplayName("savepoint creates named savepoint")
        void savepoint_createsNamedSavepoint() {
            executor.transaction(tx -> {
                insertTestUserInTx(tx, "alice@example.com", "Alice");

                Savepoint sp = tx.savepoint("before_bob");
                assertNotNull(sp);

                insertTestUserInTx(tx, "bob@example.com", "Bob");

                // Both users should exist
                List<String> emails = tx.query(
                    new QueryResult("SELECT email FROM users ORDER BY email", Map.of()),
                    rs -> rs.getString("email")
                );
                assertEquals(2, emails.size());
            });
        }

        @Test
        @DisplayName("rollbackTo reverts to savepoint state")
        void rollbackTo_revertsToSavepointState() {
            executor.transaction(tx -> {
                insertTestUserInTx(tx, "alice@example.com", "Alice");

                Savepoint sp = tx.savepoint("before_bob");

                insertTestUserInTx(tx, "bob@example.com", "Bob");

                // Rollback to savepoint
                tx.rollbackTo(sp);

                // Only Alice should exist
                List<String> emails = tx.query(
                    new QueryResult("SELECT email FROM users", Map.of()),
                    rs -> rs.getString("email")
                );
                assertEquals(1, emails.size());
                assertEquals("alice@example.com", emails.get(0));
            });
        }

        @Test
        @DisplayName("releaseSavepoint releases savepoint resources")
        void releaseSavepoint_releasesSavepoint() {
            executor.transaction(tx -> {
                insertTestUserInTx(tx, "alice@example.com", "Alice");

                Savepoint sp = tx.savepoint("checkpoint");

                insertTestUserInTx(tx, "bob@example.com", "Bob");

                // Release savepoint (frees resources but doesn't rollback)
                tx.releaseSavepoint(sp);

                // Both users should still exist
                List<String> emails = tx.query(
                    new QueryResult("SELECT email FROM users ORDER BY email", Map.of()),
                    rs -> rs.getString("email")
                );
                assertEquals(2, emails.size());
            });
        }

        @Test
        @DisplayName("multiple savepoints work correctly")
        void multipleSavepoints_workCorrectly() {
            executor.transaction(tx -> {
                insertTestUserInTx(tx, "alice@example.com", "Alice");
                Savepoint sp1 = tx.savepoint("after_alice");

                insertTestUserInTx(tx, "bob@example.com", "Bob");
                Savepoint sp2 = tx.savepoint("after_bob");

                insertTestUserInTx(tx, "charlie@example.com", "Charlie");

                // Rollback to after_bob (removes Charlie)
                tx.rollbackTo(sp2);

                List<String> emails = tx.query(
                    new QueryResult("SELECT email FROM users ORDER BY email", Map.of()),
                    rs -> rs.getString("email")
                );
                assertEquals(2, emails.size());
                assertTrue(emails.contains("alice@example.com"));
                assertTrue(emails.contains("bob@example.com"));
            });
        }
    }

    // ==================== getConnection Tests ====================

    @Nested
    @DisplayName("getConnection Tests")
    class GetConnectionTests {

        @Test
        @DisplayName("getConnection returns underlying connection")
        void getConnection_returnsConnection() {
            executor.transaction(tx -> {
                Connection conn = tx.getConnection();
                assertNotNull(conn);
                // Connection should be open (not closed)
                try {
                    assertFalse(conn.isClosed());
                } catch (java.sql.SQLException e) {
                    fail("Failed to check connection state: " + e.getMessage());
                }
            });
        }
    }

    // ==================== relationships Tests ====================

    @Nested
    @DisplayName("relationships Tests")
    class RelationshipsTests {

        @Test
        @DisplayName("relationships returns RelationshipManager")
        void relationships_returnsRelationshipManager() {
            executor.transaction(tx -> {
                RelationshipManager rm = tx.relationships();
                assertNotNull(rm);
            });
        }
    }

    // ==================== Legacy Constructor Tests ====================

    @Nested
    @DisplayName("Legacy Constructor Tests")
    class LegacyConstructorTests {

        @Test
        @DisplayName("legacy constructor creates Transaction with defaults")
        void legacyConstructor_createsTransactionWithDefaults() throws Exception {
            try (Connection conn = dataSource.getConnection()) {
                Transaction tx = new Transaction(conn);
                assertNotNull(tx);
                assertEquals(conn, tx.getConnection());
            }
        }

        @Test
        @DisplayName("legacy constructor throws on null connection")
        void legacyConstructor_nullConnection_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                new Transaction(null)
            );
        }
    }

    // ==================== Helper Methods ====================

    private void insertTestUser(String email, String name) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", UUID.randomUUID().toString());
        params.put("p2", email);
        params.put("p3", name);

        executor.execute(new QueryResult(
            "INSERT INTO users (id, email, name) VALUES (:p1, :p2, :p3)",
            params
        ));
    }

    private void insertTestUserInTx(Transaction tx, String email, String name) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", UUID.randomUUID().toString());
        params.put("p2", email);
        params.put("p3", name);

        tx.execute(new QueryResult(
            "INSERT INTO users (id, email, name) VALUES (:p1, :p2, :p3)",
            params
        ));
    }

    /**
     * Test entity for entity persistence tests.
     */
    @Entity(table = "users")
    public static class UserEntity {
        @Id(strategy = GenerationType.UUID_V4)
        @Column(name = "id", type = SqlType.UUID)
        private String id;

        @Column(name = "email", type = SqlType.VARCHAR)
        private String email;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        @Column(name = "is_active", type = SqlType.BOOLEAN)
        private Boolean isActive;

        public UserEntity() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
}
