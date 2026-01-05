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
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.jdbc.exception.ConnectionException;
import sant1ago.dev.suprim.jdbc.exception.NoResultException;
import sant1ago.dev.suprim.jdbc.exception.TransactionException;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SuprimExecutorTest {

    private JdbcDataSource dataSource;
    private SuprimExecutor executor;
    private Connection setupConnection;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        setupConnection = dataSource.getConnection();
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL,
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

    @Test
    void execute_insert_returnsAffectedRows() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "test@example.com");
        params.put("p2", "Test User");

        QueryResult insertQuery = new QueryResult(
                "INSERT INTO users (email, name) VALUES (:p1, :p2)",
                params
        );

        int affected = executor.execute(insertQuery);

        assertEquals(1, affected);
    }

    @Test
    void query_selectAll_returnsMappedResults() {
        // Insert test data
        insertTestUser("alice@example.com", "Alice");
        insertTestUser("bob@example.com", "Bob");

        QueryResult selectQuery = new QueryResult(
                "SELECT id, email, name, is_active FROM users ORDER BY email",
                Map.of()
        );

        List<TestUser> users = executor.query(selectQuery, rs -> new TestUser(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("name"),
                rs.getBoolean("is_active")
        ));

        assertEquals(2, users.size());
        assertEquals("alice@example.com", users.get(0).email());
        assertEquals("bob@example.com", users.get(1).email());
    }

    @Test
    void query_withParameters_filtersResults() {
        insertTestUser("alice@example.com", "Alice");
        insertTestUser("bob@example.com", "Bob");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "alice@example.com");

        QueryResult selectQuery = new QueryResult(
                "SELECT id, email, name FROM users WHERE email = :p1",
                params
        );

        List<TestUser> users = executor.query(selectQuery, rs -> new TestUser(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("name"),
                true
        ));

        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).name());
    }

    @Test
    void queryOne_existingRecord_returnsOptionalWithValue() {
        insertTestUser("alice@example.com", "Alice");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "alice@example.com");

        QueryResult selectQuery = new QueryResult(
                "SELECT id, email, name FROM users WHERE email = :p1",
                params
        );

        Optional<TestUser> user = executor.queryOne(selectQuery, rs -> new TestUser(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("name"),
                true
        ));

        assertTrue(user.isPresent());
        assertEquals("Alice", user.get().name());
    }

    @Test
    void queryOne_noRecord_returnsEmpty() {
        QueryResult selectQuery = new QueryResult(
                "SELECT id, email, name FROM users WHERE email = :p1",
                Map.of("p1", "nonexistent@example.com")
        );

        Optional<TestUser> user = executor.queryOne(selectQuery, rs -> new TestUser(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("name"),
                true
        ));

        assertTrue(user.isEmpty());
    }

    @Test
    void queryOne_multipleRecords_throwsException() {
        insertTestUser("alice@example.com", "Alice");
        insertTestUser("bob@example.com", "Bob");

        QueryResult selectQuery = new QueryResult(
                "SELECT id, email, name FROM users",
                Map.of()
        );

        assertThrows(SuprimException.class, () ->
                executor.queryOne(selectQuery, rs -> new TestUser(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("name"),
                        true
                ))
        );
    }

    @Test
    void execute_update_modifiesRecords() {
        insertTestUser("alice@example.com", "Alice");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "Alice Updated");
        params.put("p2", "alice@example.com");

        QueryResult updateQuery = new QueryResult(
                "UPDATE users SET name = :p1 WHERE email = :p2",
                params
        );

        int affected = executor.execute(updateQuery);

        assertEquals(1, affected);

        // Verify update
        Optional<TestUser> user = executor.queryOne(
                new QueryResult("SELECT name FROM users WHERE email = :p1", Map.of("p1", "alice@example.com")),
                rs -> new TestUser(0L, "", rs.getString("name"), true)
        );

        assertTrue(user.isPresent());
        assertEquals("Alice Updated", user.get().name());
    }

    @Test
    void execute_delete_removesRecords() {
        insertTestUser("alice@example.com", "Alice");
        insertTestUser("bob@example.com", "Bob");

        QueryResult deleteQuery = new QueryResult(
                "DELETE FROM users WHERE email = :p1",
                Map.of("p1", "alice@example.com")
        );

        int affected = executor.execute(deleteQuery);

        assertEquals(1, affected);

        // Verify deletion
        List<TestUser> remaining = executor.query(
                new QueryResult("SELECT email FROM users", Map.of()),
                rs -> new TestUser(0L, rs.getString("email"), "", true)
        );

        assertEquals(1, remaining.size());
        assertEquals("bob@example.com", remaining.get(0).email());
    }

    @Test
    void transaction_success_commitsChanges() {
        executor.transaction(tx -> {
            Map<String, Object> params1 = new LinkedHashMap<>();
            params1.put("p1", "alice@example.com");
            params1.put("p2", "Alice");

            tx.execute(new QueryResult(
                    "INSERT INTO users (email, name) VALUES (:p1, :p2)",
                    params1
            ));

            Map<String, Object> params2 = new LinkedHashMap<>();
            params2.put("p1", "bob@example.com");
            params2.put("p2", "Bob");

            tx.execute(new QueryResult(
                    "INSERT INTO users (email, name) VALUES (:p1, :p2)",
                    params2
            ));
        });

        // Verify both inserts committed
        List<TestUser> users = executor.query(
                new QueryResult("SELECT email FROM users ORDER BY email", Map.of()),
                rs -> new TestUser(0L, rs.getString("email"), "", true)
        );

        assertEquals(2, users.size());
    }

    @Test
    void transaction_exception_rollsBack() {
        try {
            executor.transaction(tx -> {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("p1", "alice@example.com");
                params.put("p2", "Alice");

                tx.execute(new QueryResult(
                        "INSERT INTO users (email, name) VALUES (:p1, :p2)",
                        params
                ));

                // Force exception
                throw new RuntimeException("Simulated failure");
            });
        } catch (SuprimException e) {
            // Expected
        }

        // Verify rollback - no users should exist
        List<TestUser> users = executor.query(
                new QueryResult("SELECT email FROM users", Map.of()),
                rs -> new TestUser(0L, rs.getString("email"), "", true)
        );

        assertEquals(0, users.size());
    }

    @Test
    void transactionWithResult_returnsValue() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "alice@example.com");
        params.put("p2", "Alice");

        Long count = executor.transactionWithResult(tx -> {
            tx.execute(new QueryResult(
                    "INSERT INTO users (email, name) VALUES (:p1, :p2)",
                    params
            ));

            return tx.queryOne(
                    new QueryResult("SELECT COUNT(*) as cnt FROM users", Map.of()),
                    rs -> rs.getLong("cnt")
            ).orElse(0L);
        });

        assertEquals(1L, count);
    }

    @Test
    void transaction_queryWithinTx_seesUncommittedData() {
        executor.transaction(tx -> {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("p1", "alice@example.com");
            params.put("p2", "Alice");

            tx.execute(new QueryResult(
                    "INSERT INTO users (email, name) VALUES (:p1, :p2)",
                    params
            ));

            // Query within same transaction should see the insert
            List<TestUser> users = tx.query(
                    new QueryResult("SELECT email FROM users", Map.of()),
                    rs -> new TestUser(0L, rs.getString("email"), "", true)
            );

            assertEquals(1, users.size());
            assertEquals("alice@example.com", users.get(0).email());
        });
    }

    private void insertTestUser(String email, String name) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", email);
        params.put("p2", name);

        executor.execute(new QueryResult(
                "INSERT INTO users (email, name) VALUES (:p1, :p2)",
                params
        ));
    }

    record TestUser(Long id, String email, String name, boolean active) {
    }

    // ==================== findById tests ====================

    @Test
    void findById_existingEntity_returnsOptionalWithEntity() {
        insertTestUser("alice@example.com", "Alice");

        Optional<UserEntity> user = executor.findById(UserEntity.class, 1L);

        assertTrue(user.isPresent());
        assertEquals("alice@example.com", user.get().getEmail());
        assertEquals("Alice", user.get().getName());
    }

    @Test
    void findById_nonExistingEntity_returnsEmptyOptional() {
        Optional<UserEntity> user = executor.findById(UserEntity.class, 999L);

        assertTrue(user.isEmpty());
    }

    @Test
    void findById_nullClass_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                executor.findById(null, 1L)
        );
    }

    @Test
    void findById_nullId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                executor.findById(UserEntity.class, null)
        );
    }

    @Test
    void findByIdOrFail_existingEntity_returnsEntity() {
        insertTestUser("bob@example.com", "Bob");

        UserEntity user = executor.findByIdOrFail(UserEntity.class, 1L);

        assertEquals("bob@example.com", user.getEmail());
    }

    @Test
    void findByIdOrFail_nonExistingEntity_throwsNoResultException() {
        assertThrows(NoResultException.class, () ->
                executor.findByIdOrFail(UserEntity.class, 999L)
        );
    }

    /**
     * Test entity with proper Suprim annotations.
     */
    @Entity(table = "users")
    public static class UserEntity {
        @Id
        @Column(name = "id", type = SqlType.BIGINT)
        private Long id;

        @Column(name = "email", type = SqlType.VARCHAR)
        private String email;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        @Column(name = "is_active", type = SqlType.BOOLEAN)
        private Boolean isActive;

        public UserEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }

    // ==================== AUTO-COMMIT EXCEPTION TESTS ====================

    @Nested
    @DisplayName("executeAutoCommit exception handling")
    class ExecuteAutoCommitExceptionTests {

        @Test
        @DisplayName("executeAutoCommit throws ConnectionException on getConnection failure")
        void executeAutoCommit_getConnectionFails_throwsConnectionException() {
            SuprimExecutor failingExecutor = SuprimExecutor.create(new FailingDataSource());

            assertThrows(ConnectionException.class, () -> {
                failingExecutor.executeAutoCommit((conn, dialect) -> "result");
            });
        }

        @Test
        @DisplayName("executeAutoCommitVoid throws ConnectionException on getConnection failure")
        void executeAutoCommitVoid_getConnectionFails_throwsConnectionException() {
            SuprimExecutor failingExecutor = SuprimExecutor.create(new FailingDataSource());

            assertThrows(ConnectionException.class, () -> {
                failingExecutor.executeAutoCommitVoid((conn, dialect) -> {});
            });
        }

        @Test
        @DisplayName("executeAutoCommit throws TransactionException on setAutoCommit failure")
        void executeAutoCommit_setAutoCommitFails_throwsTransactionException() {
            SuprimExecutor failingExecutor = SuprimExecutor.create(new SetAutoCommitFailingDataSource());

            assertThrows(TransactionException.class, () -> {
                failingExecutor.executeAutoCommit((conn, dialect) -> "result");
            });
        }

        @Test
        @DisplayName("executeAutoCommitVoid throws TransactionException on setAutoCommit failure")
        void executeAutoCommitVoid_setAutoCommitFails_throwsTransactionException() {
            SuprimExecutor failingExecutor = SuprimExecutor.create(new SetAutoCommitFailingDataSource());

            assertThrows(TransactionException.class, () -> {
                failingExecutor.executeAutoCommitVoid((conn, dialect) -> {});
            });
        }
    }

    /**
     * DataSource that always throws SQLException on getConnection().
     */
    static class FailingDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("Simulated connection failure", "08001");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("Simulated connection failure", "08001");
        }

        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not supported"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    // ==================== BUILDER TESTS ====================

    @Nested
    @DisplayName("Builder pattern tests")
    class BuilderTests {

        @Test
        @DisplayName("builder creates executor with connectionName")
        void builder_createsExecutorWithConnectionName() {
            SuprimExecutor builtExecutor = SuprimExecutor.builder(dataSource)
                .connectionName("test-connection")
                .build();

            assertNotNull(builtExecutor);
            assertEquals("test-connection", builtExecutor.getConnectionName());
        }

        @Test
        @DisplayName("builder with onQuery listener fires on query")
        void builder_withOnQueryListener_firesOnQuery() {
            java.util.concurrent.atomic.AtomicBoolean listenerFired = new java.util.concurrent.atomic.AtomicBoolean(false);

            SuprimExecutor builtExecutor = SuprimExecutor.builder(dataSource)
                .onQuery(event -> listenerFired.set(true))
                .build();

            builtExecutor.query(
                new QueryResult("SELECT 1", Map.of()),
                rs -> rs.getInt(1)
            );

            assertTrue(listenerFired.get());
        }

        @Test
        @DisplayName("builder with onSlowQuery listener fires for slow queries")
        void builder_withOnSlowQueryListener_firesForSlowQueries() {
            java.util.concurrent.atomic.AtomicBoolean listenerFired = new java.util.concurrent.atomic.AtomicBoolean(false);

            SuprimExecutor builtExecutor = SuprimExecutor.builder(dataSource)
                .onSlowQuery(0, event -> listenerFired.set(true)) // 0ms threshold - always fires
                .build();

            builtExecutor.query(
                new QueryResult("SELECT 1", Map.of()),
                rs -> rs.getInt(1)
            );

            assertTrue(listenerFired.get());
        }

        @Test
        @DisplayName("builder with onQueryError listener fires on error")
        void builder_withOnQueryErrorListener_firesOnError() {
            java.util.concurrent.atomic.AtomicBoolean listenerFired = new java.util.concurrent.atomic.AtomicBoolean(false);

            SuprimExecutor builtExecutor = SuprimExecutor.builder(dataSource)
                .onQueryError(event -> listenerFired.set(true))
                .build();

            try {
                builtExecutor.query(
                    new QueryResult("SELECT * FROM nonexistent_table", Map.of()),
                    rs -> rs.getInt(1)
                );
            } catch (Exception ignored) {}

            assertTrue(listenerFired.get());
        }

        @Test
        @DisplayName("builder throws on null dataSource")
        void builder_nullDataSource_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                SuprimExecutor.builder(null)
            );
        }
    }

    // ==================== LISTENER MANAGEMENT TESTS ====================

    @Nested
    @DisplayName("Listener management tests")
    class ListenerManagementTests {

        @Test
        @DisplayName("addQueryListener adds listener at runtime")
        void addQueryListener_addsListenerAtRuntime() {
            java.util.concurrent.atomic.AtomicBoolean listenerFired = new java.util.concurrent.atomic.AtomicBoolean(false);

            executor.addQueryListener(sant1ago.dev.suprim.jdbc.event.QueryListener.onQuery(
                event -> listenerFired.set(true)
            ));

            executor.query(
                new QueryResult("SELECT 1", Map.of()),
                rs -> rs.getInt(1)
            );

            assertTrue(listenerFired.get());
        }

        @Test
        @DisplayName("removeQueryListener removes listener")
        void removeQueryListener_removesListener() {
            java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);

            sant1ago.dev.suprim.jdbc.event.QueryListener listener =
                sant1ago.dev.suprim.jdbc.event.QueryListener.onQuery(event -> callCount.incrementAndGet());

            executor.addQueryListener(listener);

            // First query should fire listener
            executor.query(new QueryResult("SELECT 1", Map.of()), rs -> rs.getInt(1));
            assertEquals(1, callCount.get());

            // Remove listener
            boolean removed = executor.removeQueryListener(listener);
            assertTrue(removed);

            // Second query should not fire listener
            executor.query(new QueryResult("SELECT 1", Map.of()), rs -> rs.getInt(1));
            assertEquals(1, callCount.get());
        }

        @Test
        @DisplayName("addTransactionListener adds listener at runtime")
        void addTransactionListener_addsListenerAtRuntime() {
            java.util.concurrent.atomic.AtomicBoolean listenerFired = new java.util.concurrent.atomic.AtomicBoolean(false);

            executor.addTransactionListener(new sant1ago.dev.suprim.jdbc.event.TransactionListener() {
                @Override
                public void onBegin(sant1ago.dev.suprim.jdbc.event.TransactionEvent event) {
                    listenerFired.set(true);
                }
            });

            // Transaction listener fires on transaction begin, so we just need to start a tx
            executor.transaction(tx -> {
                // Empty transaction - listener fires on begin
            });

            assertTrue(listenerFired.get());
        }

        @Test
        @DisplayName("removeTransactionListener removes listener")
        void removeTransactionListener_removesListener() {
            java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);

            sant1ago.dev.suprim.jdbc.event.TransactionListener listener = new sant1ago.dev.suprim.jdbc.event.TransactionListener() {
                @Override
                public void onBegin(sant1ago.dev.suprim.jdbc.event.TransactionEvent event) {
                    callCount.incrementAndGet();
                }
            };

            executor.addTransactionListener(listener);

            // First transaction should fire listener
            executor.transaction(tx -> {});
            int countAfterFirst = callCount.get();
            assertTrue(countAfterFirst > 0);

            // Remove listener
            boolean removed = executor.removeTransactionListener(listener);
            assertTrue(removed);

            // Second transaction should not fire listener
            executor.transaction(tx -> {});
            assertEquals(countAfterFirst, callCount.get());
        }
    }

    // ==================== STATIC RELATIONSHIPS METHOD TEST ====================

    @Nested
    @DisplayName("Static relationships method tests")
    class StaticRelationshipsTests {

        @Test
        @DisplayName("relationships static method returns RelationshipManager")
        void relationships_returnsRelationshipManager() {
            executor.transaction(tx -> {
                RelationshipManager rm = SuprimExecutor.relationships(tx);
                assertNotNull(rm);
            });
        }
    }

    /**
     * DataSource that returns a connection which throws on setAutoCommit().
     */
    static class SetAutoCommitFailingDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if ("setAutoCommit".equals(method.getName())) {
                        throw new SQLException("Simulated setAutoCommit failure", "25000");
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return false;
                    }
                    throw new UnsupportedOperationException("Method not mocked: " + method.getName());
                }
            );
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not supported"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
