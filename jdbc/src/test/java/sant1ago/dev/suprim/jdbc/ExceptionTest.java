package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.jdbc.exception.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    private JdbcDataSource dataSource;
    private SuprimExecutor executor;
    private Connection setupConnection;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:exceptiontest;DB_CLOSE_DELAY=-1");

        setupConnection = dataSource.getConnection();
        try (Statement stmt = setupConnection.createStatement()) {
            // Users table with unique email
            stmt.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    age INT CHECK (age >= 0)
                )
                """);

            // Orders table with foreign key
            stmt.execute("""
                CREATE TABLE orders (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    amount DECIMAL(10, 2),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """);
        }

        executor = SuprimExecutor.create(dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("DROP TABLE IF EXISTS users");
        }
        setupConnection.close();
    }

    // ==================== Unique Constraint Tests ====================

    @Test
    void execute_duplicateEmail_throwsUniqueConstraintException() {
        // Insert first user
        insertUser("alice@example.com", "Alice", 25);

        // Try to insert duplicate email
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "alice@example.com");
        params.put("p2", "Bob");
        params.put("p3", 30);

        QueryResult insertQuery = new QueryResult(
                "INSERT INTO users (email, name, age) VALUES (:p1, :p2, :p3)",
                params
        );

        UniqueConstraintException ex = assertThrows(UniqueConstraintException.class, () ->
                executor.execute(insertQuery)
        );

        assertTrue(ex.isConstraintViolation());
        assertEquals(SuprimException.ErrorCategory.INTEGRITY_CONSTRAINT, ex.getCategory());
        assertNotNull(ex.getSql());
    }

    // ==================== NOT NULL Constraint Tests ====================

    @Test
    void execute_nullRequiredField_throwsNotNullException() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "test@example.com");
        params.put("p2", null); // name is NOT NULL
        params.put("p3", 25);

        QueryResult insertQuery = new QueryResult(
                "INSERT INTO users (email, name, age) VALUES (:p1, :p2, :p3)",
                params
        );

        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class, () ->
                executor.execute(insertQuery)
        );

        assertTrue(ex.isConstraintViolation());
    }

    // ==================== Check Constraint Tests ====================

    @Test
    void execute_negativeAge_throwsCheckConstraintException() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "test@example.com");
        params.put("p2", "Test");
        params.put("p3", -5); // age must be >= 0

        QueryResult insertQuery = new QueryResult(
                "INSERT INTO users (email, name, age) VALUES (:p1, :p2, :p3)",
                params
        );

        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class, () ->
                executor.execute(insertQuery)
        );

        assertTrue(ex.isConstraintViolation());
    }

    // ==================== Foreign Key Tests ====================

    @Test
    void execute_invalidForeignKey_throwsForeignKeyException() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", 999L); // Non-existent user_id
        params.put("p2", 100.00);

        QueryResult insertQuery = new QueryResult(
                "INSERT INTO orders (user_id, amount) VALUES (:p1, :p2)",
                params
        );

        ForeignKeyException ex = assertThrows(ForeignKeyException.class, () ->
                executor.execute(insertQuery)
        );

        assertTrue(ex.isConstraintViolation());
        assertTrue(ex.isParentNotFound());
    }

    @Test
    void execute_deleteReferencedUser_throwsForeignKeyException() {
        // Insert user and order
        insertUser("alice@example.com", "Alice", 25);

        executor.execute(new QueryResult(
                "INSERT INTO orders (user_id, amount) VALUES (:p1, :p2)",
                Map.of("p1", 1L, "p2", 100.00)
        ));

        // Try to delete user with orders
        QueryResult deleteQuery = new QueryResult(
                "DELETE FROM users WHERE id = :p1",
                Map.of("p1", 1L)
        );

        ForeignKeyException ex = assertThrows(ForeignKeyException.class, () ->
                executor.execute(deleteQuery)
        );

        assertTrue(ex.hasChildRecords());
    }

    // ==================== No Result / Non-Unique Result Tests ====================

    @Test
    void queryOneRequired_noRows_throwsNoResultException() {
        QueryResult selectQuery = new QueryResult(
                "SELECT id, email FROM users WHERE email = :p1",
                Map.of("p1", "nonexistent@example.com")
        );

        NoResultException ex = assertThrows(NoResultException.class, () ->
                executor.queryOneRequired(selectQuery, rs -> rs.getString("email"))
        );

        assertNotNull(ex.getSql());
    }

    @Test
    void queryOne_multipleRows_throwsNonUniqueResultException() {
        insertUser("alice@example.com", "Alice", 25);
        insertUser("bob@example.com", "Bob", 30);

        QueryResult selectQuery = new QueryResult(
                "SELECT id, email FROM users",
                Map.of()
        );

        NonUniqueResultException ex = assertThrows(NonUniqueResultException.class, () ->
                executor.queryOne(selectQuery, rs -> rs.getString("email"))
        );

        assertNotNull(ex.getSql());
    }

    // ==================== Mapping Exception Tests ====================

    @Test
    void entityMapper_noNoArgConstructor_throwsMappingException() {
        MappingException ex = assertThrows(MappingException.class, () ->
                EntityMapper.of(ClassWithoutNoArgConstructor.class)
        );

        assertEquals(MappingException.MappingErrorType.NO_NO_ARG_CONSTRUCTOR, ex.getMappingErrorType());
        assertEquals(ClassWithoutNoArgConstructor.class, ex.getTargetClass());
    }

    // ==================== Transaction Tests ====================

    @Test
    void transaction_constraintViolation_rollsBackAndThrows() {
        insertUser("alice@example.com", "Alice", 25);

        try {
            executor.transaction(tx -> {
                // This should succeed
                tx.execute(new QueryResult(
                        "INSERT INTO users (email, name, age) VALUES (:p1, :p2, :p3)",
                        Map.of("p1", "bob@example.com", "p2", "Bob", "p3", 30)
                ));

                // This should fail (duplicate email)
                tx.execute(new QueryResult(
                        "INSERT INTO users (email, name, age) VALUES (:p1, :p2, :p3)",
                        Map.of("p1", "alice@example.com", "p2", "Alice2", "p3", 35)
                ));
            });
            fail("Should have thrown UniqueConstraintException");
        } catch (UniqueConstraintException e) {
            // Expected
        }

        // Verify rollback - only Alice should exist
        long count = executor.queryOneRequired(
                new QueryResult("SELECT COUNT(*) as cnt FROM users", Map.of()),
                rs -> rs.getLong("cnt")
        );

        assertEquals(1L, count);
    }

    // ==================== Error Category Tests ====================

    @Test
    void errorCategory_fromSqlState_mapsCorrectly() {
        assertEquals(SuprimException.ErrorCategory.CONNECTION,
                SuprimException.ErrorCategory.fromSqlState("08001"));
        assertEquals(SuprimException.ErrorCategory.INTEGRITY_CONSTRAINT,
                SuprimException.ErrorCategory.fromSqlState("23505"));
        assertEquals(SuprimException.ErrorCategory.SYNTAX_ERROR,
                SuprimException.ErrorCategory.fromSqlState("42000"));
        assertEquals(SuprimException.ErrorCategory.TRANSACTION_ROLLBACK,
                SuprimException.ErrorCategory.fromSqlState("40001"));
        assertEquals(SuprimException.ErrorCategory.UNKNOWN,
                SuprimException.ErrorCategory.fromSqlState("99999"));
        assertEquals(SuprimException.ErrorCategory.UNKNOWN,
                SuprimException.ErrorCategory.fromSqlState(null));
    }

    // ==================== Exception Builder Tests ====================

    @Test
    void suprimException_builder_setsAllFields() {
        SuprimException ex = SuprimException.builder()
                .message("Test error")
                .sql("SELECT * FROM test")
                .sqlState("42000")
                .vendorCode(1234)
                .category(SuprimException.ErrorCategory.SYNTAX_ERROR)
                .build();

        assertTrue(ex.getMessage().contains("Test error"));
        assertTrue(ex.getMessage().contains("SELECT * FROM test"));
        assertTrue(ex.getMessage().contains("42000"));
        assertEquals("SELECT * FROM test", ex.getSql());
        assertEquals("42000", ex.getSqlState());
        assertEquals(1234, ex.getVendorCode());
        assertEquals(SuprimException.ErrorCategory.SYNTAX_ERROR, ex.getCategory());
    }

    @Test
    void suprimException_isRetryable_returnsTrueForRetryableErrors() {
        SuprimException retryable = SuprimException.builder()
                .message("Deadlock")
                .category(SuprimException.ErrorCategory.TRANSACTION_ROLLBACK)
                .build();

        SuprimException nonRetryable = SuprimException.builder()
                .message("Syntax error")
                .category(SuprimException.ErrorCategory.SYNTAX_ERROR)
                .build();

        assertTrue(retryable.isRetryable());
        assertFalse(nonRetryable.isRetryable());
    }

    // ==================== Helper Methods ====================

    private void insertUser(String email, String name, int age) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", email);
        params.put("p2", name);
        params.put("p3", age);

        executor.execute(new QueryResult(
                "INSERT INTO users (email, name, age) VALUES (:p1, :p2, :p3)",
                params
        ));
    }

    // Test class without no-arg constructor
    static class ClassWithoutNoArgConstructor {
        private final String name;

        ClassWithoutNoArgConstructor(String name) {
            this.name = name;
        }
    }
}
