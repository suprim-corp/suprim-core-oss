package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for DeleteBuilder.
 */
@DisplayName("DeleteBuilder Tests")
class DeleteBuilderTest {

    @Test
    @DisplayName("Delete with single WHERE condition")
    void testDeleteWithSingleCondition() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.ID.eq(1L))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE users.\"id\" = 1"));
    }

    @Test
    @DisplayName("Delete with multiple AND conditions")
    void testDeleteWithAndConditions() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(false))
            .and(TestUser_.AGE.lt(18))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("AND"));
    }

    @Test
    @DisplayName("Delete with OR condition")
    void testDeleteWithOrCondition() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.AGE.lt(18).or(TestUser_.AGE.gt(100)))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("OR"));
    }

    @Test
    @DisplayName("Delete without WHERE clause")
    void testDeleteWithoutWhere() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertFalse(sql.contains("WHERE"));
    }

    @Test
    @DisplayName("Delete with RETURNING clause (PostgreSQL)")
    void testDeleteWithReturning() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.ID.eq(1L))
            .returning(TestUser_.ID, TestUser_.EMAIL)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("RETURNING \"id\", \"email\""));
    }

    @Test
    @DisplayName("Delete with IN condition")
    void testDeleteWithIn() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.ID.in(1L, 2L, 3L))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    @DisplayName("Delete with IS NULL condition")
    void testDeleteWithIsNull() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.EMAIL.isNull())
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE users.\"email\" IS NULL"));
    }

    @Test
    @DisplayName("Delete with IS NOT NULL condition")
    void testDeleteWithIsNotNull() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.NAME.isNotNull())
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE users.\"name\" IS NOT NULL"));
    }

    @Test
    @DisplayName("Delete with BETWEEN condition")
    void testDeleteWithBetween() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.AGE.between(18, 25))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE users.\"age\" BETWEEN 18 AND 25"));
    }

    @Test
    @DisplayName("Delete with complex condition")
    void testDeleteWithComplexCondition() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(false))
            .and(TestUser_.AGE.lt(18).or(TestUser_.EMAIL.isNull()))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
    }

    @Test
    @DisplayName("Build with MySQL dialect")
    void testDeleteWithMySqlDialect() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.ID.eq(1L))
            .build(MySqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM `users`"));
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    @DisplayName("Build with PostgreSQL dialect")
    void testDeleteWithPostgreSqlDialect() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.ID.eq(1L))
            .build(PostgreSqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    @DisplayName("Delete has no parameters by default")
    void testDeleteHasNoParameters() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.ID.eq(1L))
            .build();

        // WHERE clause is rendered directly in SQL, no parameters needed
        assertTrue(result.parameters().isEmpty());
    }

    @Test
    @DisplayName("Delete with NOT IN condition")
    void testDeleteWithNotIn() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.ID.notIn(1L, 2L, 3L))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("NOT IN"));
    }

    @Test
    @DisplayName("Delete with string LIKE condition")
    void testDeleteWithLike() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .where(TestUser_.EMAIL.like("%@spam.com"))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("LIKE"));
    }

    @Test
    @DisplayName("and() without prior where() sets whereClause")
    void testAndWithoutPriorWhere() {
        QueryResult result = Suprim.deleteFrom(TestUser_.TABLE)
            .and(TestUser_.IS_ACTIVE.eq(false))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("DELETE FROM \"users\""));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("is_active"));
    }
}
