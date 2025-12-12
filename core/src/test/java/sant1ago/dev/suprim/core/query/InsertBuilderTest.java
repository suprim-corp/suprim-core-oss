package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for InsertBuilder.
 */
@DisplayName("InsertBuilder Tests")
class InsertBuilderTest {

    @Test
    @DisplayName("Single column insert")
    void testSingleColumnInsert() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("(\"email\")"));
        assertTrue(sql.contains("VALUES"));
        assertEquals(1, result.parameters().size());
    }

    @Test
    @DisplayName("Multi-column insert")
    void testMultiColumnInsert() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .column(TestUser_.AGE, 25)
            .column(TestUser_.IS_ACTIVE, true)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("(\"email\", \"name\", \"age\", \"is_active\")"));
        assertTrue(sql.contains("VALUES"));
        assertEquals(4, result.parameters().size());
    }

    @Test
    @DisplayName("Insert with RETURNING clause (PostgreSQL)")
    void testInsertWithReturning() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .returning(TestUser_.ID, TestUser_.CREATED_AT)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("RETURNING \"id\", \"created_at\""));
    }

    @Test
    @DisplayName("Insert with all data types")
    void testInsertWithAllTypes() {
        LocalDateTime now = LocalDateTime.now();

        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .column(TestUser_.AGE, 30)
            .column(TestUser_.IS_ACTIVE, true)
            .column(TestUser_.CREATED_AT, now)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("(\"email\", \"name\", \"age\", \"is_active\", \"created_at\")"));
        assertEquals(5, result.parameters().size());
    }

    @Test
    @DisplayName("Build with MySQL dialect")
    void testInsertWithMySqlDialect() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .build(MySqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO `users`"));
        assertTrue(sql.contains("(`email`, `name`)"));
    }

    @Test
    @DisplayName("Build with PostgreSQL dialect")
    void testInsertWithPostgreSqlDialect() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .build(PostgreSqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("(\"email\", \"name\")"));
    }

    @Test
    @DisplayName("RETURNING clause with MySQL throws exception")
    void testReturningWithMySqlThrowsException() {
        assertThrows(UnsupportedDialectFeatureException.class, () -> {
            Suprim.insertInto(TestUser_.TABLE)
                .column(TestUser_.EMAIL, "test@example.com")
                .returning(TestUser_.ID)
                .build(MySqlDialect.INSTANCE);
        });
    }

    @Test
    @DisplayName("Insert without columns throws exception")
    void testInsertWithoutColumnsThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            Suprim.insertInto(TestUser_.TABLE).build();
        });
    }

    @Test
    @DisplayName("Insert with null values")
    void testInsertWithNullValues() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, null)
            .column(TestUser_.AGE, null)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertEquals(3, result.parameters().size());
        assertTrue(result.parameters().values().stream().anyMatch(v -> v == null));
    }

    @Test
    @DisplayName("Parameter names are sequential")
    void testParameterNamesAreSequential() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John")
            .column(TestUser_.AGE, 25)
            .build();

        assertTrue(result.parameters().containsKey("p1"));
        assertTrue(result.parameters().containsKey("p2"));
        assertTrue(result.parameters().containsKey("p3"));
    }

    @Test
    @DisplayName("Insert preserves parameter values")
    void testInsertPreservesParameterValues() {
        String email = "test@example.com";
        String name = "John Doe";
        Integer age = 25;

        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, email)
            .column(TestUser_.NAME, name)
            .column(TestUser_.AGE, age)
            .build();

        assertTrue(result.parameters().containsValue(email));
        assertTrue(result.parameters().containsValue(name));
        assertTrue(result.parameters().containsValue(age));
    }
}
