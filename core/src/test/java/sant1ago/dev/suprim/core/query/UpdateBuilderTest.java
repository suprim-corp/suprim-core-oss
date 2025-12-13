package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for UpdateBuilder.
 */
@DisplayName("UpdateBuilder Tests")
class UpdateBuilderTest {

    @Test
    @DisplayName("Single column update with WHERE")
    void testSingleColumnUpdate() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.EMAIL, "newemail@example.com")
            .where(TestUser_.ID.eq(1L))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("SET \"email\" = :"));
        assertTrue(sql.contains("WHERE users.\"id\" = 1"));
    }

    @Test
    @DisplayName("Multiple columns update")
    void testMultiColumnUpdate() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.EMAIL, "newemail@example.com")
            .set(TestUser_.NAME, "New Name")
            .set(TestUser_.AGE, 30)
            .where(TestUser_.ID.eq(1L))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("SET"));
        assertTrue(sql.contains("\"email\" = :"));
        assertTrue(sql.contains("\"name\" = :"));
        assertTrue(sql.contains("\"age\" = :"));
        assertEquals(3, result.parameters().size());
    }

    @Test
    @DisplayName("Update with AND condition")
    void testUpdateWithAnd() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.IS_ACTIVE, false)
            .where(TestUser_.AGE.lt(18))
            .and(TestUser_.EMAIL.isNull())
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("AND"));
    }

    @Test
    @DisplayName("Update with RETURNING clause (PostgreSQL)")
    void testUpdateWithReturning() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.EMAIL, "newemail@example.com")
            .where(TestUser_.ID.eq(1L))
            .returning(TestUser_.ID, TestUser_.EMAIL, TestUser_.CREATED_AT)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("RETURNING \"id\", \"email\", \"created_at\""));
    }

    @Test
    @DisplayName("Update without WHERE clause")
    void testUpdateWithoutWhere() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.IS_ACTIVE, true)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("SET \"is_active\" = :"));
        assertFalse(sql.contains("WHERE"));
    }

    @Test
    @DisplayName("Update with complex WHERE")
    void testUpdateWithComplexWhere() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.IS_ACTIVE, false)
            .where(TestUser_.AGE.lt(18).or(TestUser_.AGE.gt(65)))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("OR"));
    }

    @Test
    @DisplayName("Build with MySQL dialect")
    void testUpdateWithMySqlDialect() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.EMAIL, "newemail@example.com")
            .where(TestUser_.ID.eq(1L))
            .build(MySqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE `users`"));
        assertTrue(sql.contains("SET `email` = :"));
    }

    @Test
    @DisplayName("Build with PostgreSQL dialect")
    void testUpdateWithPostgreSqlDialect() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.EMAIL, "newemail@example.com")
            .where(TestUser_.ID.eq(1L))
            .build(PostgreSqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("SET \"email\" = :"));
    }

    @Test
    @DisplayName("Update without SET throws exception")
    void testUpdateWithoutSetThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            Suprim.update(TestUser_.TABLE)
                .where(TestUser_.ID.eq(1L))
                .build();
        });
    }

    @Test
    @DisplayName("Update with null value")
    void testUpdateWithNullValue() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.NAME, (String) null)
            .where(TestUser_.ID.eq(1L))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("SET \"name\" = :"));
        assertTrue(result.parameters().values().stream().anyMatch(v -> v == null));
    }

    @Test
    @DisplayName("Update with boolean value")
    void testUpdateWithBooleanValue() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.IS_ACTIVE, true)
            .where(TestUser_.ID.eq(1L))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(result.parameters().containsValue(true));
    }

    @Test
    @DisplayName("Parameter names are sequential")
    void testParameterNamesAreSequential() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.EMAIL, "test@example.com")
            .set(TestUser_.NAME, "John")
            .set(TestUser_.AGE, 25)
            .where(TestUser_.ID.eq(1L))
            .build();

        assertTrue(result.parameters().containsKey("p1"));
        assertTrue(result.parameters().containsKey("p2"));
        assertTrue(result.parameters().containsKey("p3"));
    }

    @Test
    @DisplayName("Update preserves parameter values")
    void testUpdatePreservesParameterValues() {
        String email = "newemail@example.com";
        String name = "New Name";
        Integer age = 30;

        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.EMAIL, email)
            .set(TestUser_.NAME, name)
            .set(TestUser_.AGE, age)
            .where(TestUser_.ID.eq(1L))
            .build();

        assertTrue(result.parameters().containsValue(email));
        assertTrue(result.parameters().containsValue(name));
        assertTrue(result.parameters().containsValue(age));
    }

    @Test
    @DisplayName("Update with IN condition")
    void testUpdateWithInCondition() {
        QueryResult result = Suprim.update(TestUser_.TABLE)
            .set(TestUser_.IS_ACTIVE, false)
            .where(TestUser_.ID.in(1L, 2L, 3L, 4L, 5L))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("UPDATE \"users\""));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("IN"));
    }
}
