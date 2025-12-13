package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Coalesce expression.
 */
class CoalesceTest {

    @Entity(table = "users")
    static class User {}

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final StringColumn<User> NAME = new StringColumn<>(USERS, "name", "VARCHAR(255)");
    private static final StringColumn<User> EMAIL = new StringColumn<>(USERS, "email", "VARCHAR(255)");
    private static final StringColumn<User> NICKNAME = new StringColumn<>(USERS, "nickname", "VARCHAR(255)");

    @Test
    void testOfVarargs() {
        // Tests of(Expression[]) factory method
        Coalesce<String> coalesce = Coalesce.of(NAME, EMAIL, NICKNAME);
        String sql = coalesce.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.startsWith("COALESCE("));
        assertTrue(sql.contains("\"name\""));
        assertTrue(sql.contains("\"email\""));
        assertTrue(sql.contains("\"nickname\""));
    }

    @Test
    void testOfWithLiteralFallback() {
        Coalesce<String> coalesce = Coalesce.of(NAME, "Unknown");
        String sql = coalesce.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("COALESCE("));
        assertTrue(sql.contains("'Unknown'"));
    }

    @Test
    void testOrExpression() {
        // Tests or(Expression) method
        Coalesce<String> coalesce = Coalesce.of(NAME, "default").or(EMAIL);
        String sql = coalesce.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("\"name\""));
        assertTrue(sql.contains("\"email\""));
    }

    @Test
    void testOrLiteral() {
        Coalesce<String> coalesce = Coalesce.of(NAME, "first").or("second");
        String sql = coalesce.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("'first'"));
        assertTrue(sql.contains("'second'"));
    }

    @Test
    void testAsAlias() {
        // Tests as(String) method
        Coalesce<String> coalesce = Coalesce.of(NAME, "Unknown").as("display_name");
        String sql = coalesce.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains(" AS display_name"));
    }

    @Test
    void testToSqlWithoutAlias() {
        Coalesce<String> coalesce = Coalesce.of(NAME, "Unknown");
        String sql = coalesce.toSql(PostgreSqlDialect.INSTANCE);
        assertFalse(sql.contains(" AS "));
    }

    @Test
    void testToSqlWithAlias() {
        Coalesce<String> coalesce = Coalesce.of(NAME, "Unknown").as("result");
        String sql = coalesce.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.endsWith(" AS result"));
    }

    @Test
    void testGetValueType() {
        Coalesce<String> coalesce = Coalesce.of(NAME, "Unknown");
        assertEquals(String.class, coalesce.getValueType());
    }

    @Test
    void testEmptyExpressionsList() {
        // Tests expressions.isEmpty() = true branch in constructor
        Coalesce<Object> coalesce = Coalesce.of();
        assertEquals(Object.class, coalesce.getValueType());
        assertEquals("COALESCE()", coalesce.toSql(PostgreSqlDialect.INSTANCE));
    }
}
