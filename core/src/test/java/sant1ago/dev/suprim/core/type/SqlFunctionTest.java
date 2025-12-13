package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.dialect.MariaDbDialect;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SqlFunction.BuiltinFunction.getSqlName branch coverage.
 */
class SqlFunctionTest {

    @Test
    void testGetSqlNamePostgreSQL() {
        // PostgreSQL: neither "mysql" nor "mariadb" → returns postgresName
        String name = SqlFunction.BuiltinFunction.IFNULL.getSqlName(PostgreSqlDialect.INSTANCE);
        assertEquals("COALESCE", name);
    }

    @Test
    void testGetSqlNameMySQL() {
        // MySQL: contains "mysql" → returns mysqlName
        String name = SqlFunction.BuiltinFunction.IFNULL.getSqlName(MySqlDialect.INSTANCE);
        assertEquals("IFNULL", name);
    }

    @Test
    void testGetSqlNameMariaDB() {
        // MariaDB: doesn't contain "mysql", but contains "mariadb" → returns mysqlName
        String name = SqlFunction.BuiltinFunction.IFNULL.getSqlName(MariaDbDialect.INSTANCE);
        assertEquals("IFNULL", name);
    }

    @Test
    void testGetSqlNameUuidFunction() {
        // GEN_RANDOM_UUID has different names for Postgres vs MySQL
        assertEquals("gen_random_uuid",
                SqlFunction.BuiltinFunction.GEN_RANDOM_UUID.getSqlName(PostgreSqlDialect.INSTANCE));
        assertEquals("UUID",
                SqlFunction.BuiltinFunction.GEN_RANDOM_UUID.getSqlName(MySqlDialect.INSTANCE));
    }

    // ==================== renderArgument Branch Tests ====================

    @Test
    void testRenderArgumentBoolean() {
        // Tests arg instanceof Boolean branch
        SqlFunction<Object> fn = Fn.call("test_func", true, false);
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("test_func(TRUE, FALSE)", sql);
    }

    @Test
    void testRenderArgumentFallback() {
        // Tests else branch with non-standard type (UUID)
        java.util.UUID uuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SqlFunction<Object> fn = Fn.call("test_func", uuid);
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("test_func(550e8400-e29b-41d4-a716-446655440000)", sql);
    }

    @Test
    void testRenderArgumentBooleanMySQL() {
        // Tests Boolean branch with MySQL dialect
        SqlFunction<Object> fn = Fn.call("test_func", true);
        String sql = fn.toSql(MySqlDialect.INSTANCE);
        assertEquals("test_func(TRUE)", sql);
    }

    // ==================== toSql Branch Tests ====================

    @Test
    void testRawFunctionWithAlias() {
        // Tests RAW type with alias branch
        SqlFunction<Object> fn = Fn.raw("1 + 1").as("result");
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("1 + 1 AS \"result\"", sql);
    }

    @Test
    void testBuiltinWithoutAlias() {
        SqlFunction<?> fn = Fn.now();
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("NOW()", sql);
        assertFalse(sql.contains(" AS "));
    }

    @Test
    void testBuiltinWithAlias() {
        SqlFunction<?> fn = Fn.now().as("current_ts");
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("NOW()"));
        assertTrue(sql.contains(" AS \"current_ts\""));
    }

    @Test
    void testCustomWithoutAlias() {
        SqlFunction<Object> fn = Fn.call("my_func");
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("my_func()", sql);
        assertFalse(sql.contains(" AS "));
    }

    @Test
    void testCustomWithAlias() {
        SqlFunction<Object> fn = Fn.call("my_func").as("result");
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("my_func()"));
        assertTrue(sql.contains(" AS \"result\""));
    }

    @Test
    void testRawWithoutAlias() {
        SqlFunction<Object> fn = Fn.raw("2 * 3");
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("2 * 3", sql);
        assertFalse(sql.contains(" AS "));
    }

    @Test
    void testRawWithPlaceholderArguments() {
        // Tests renderRawExpression branch when arguments is NOT empty
        SqlFunction<Object> fn = Fn.raw("EXTRACT(YEAR FROM ?)", "2024-01-15");
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("EXTRACT(YEAR FROM '2024-01-15')", sql);
    }

    @Test
    void testRawWithMultiplePlaceholders() {
        // Tests the for loop in renderRawExpression with multiple args
        SqlFunction<Object> fn = Fn.raw("? + ?", 1, 2);
        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("1 + 2", sql);
    }

    // ==================== Reflection Tests for Defensive Null Branches ====================

    @Test
    void testConstructorWithNullArguments() throws Exception {
        // Tests line 43: nonNull(arguments) branch when arguments is null
        Constructor<SqlFunction> ctor = SqlFunction.class.getDeclaredConstructor(
                String.class, List.class, Class.class,
                SqlFunction.FunctionType.class, String.class
        );
        ctor.setAccessible(true);

        @SuppressWarnings("unchecked")
        SqlFunction<Object> fn = ctor.newInstance(
                "test_func", null, Object.class,
                SqlFunction.FunctionType.CUSTOM, null
        );

        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("test_func()", sql);
    }

    @Test
    void testRenderArgumentWithNull() throws Exception {
        // Tests line 222: isNull(arg) branch when arg is null
        // Create function with a list containing null via reflection
        Constructor<SqlFunction> ctor = SqlFunction.class.getDeclaredConstructor(
                String.class, List.class, Class.class,
                SqlFunction.FunctionType.class, String.class
        );
        ctor.setAccessible(true);

        List<Object> argsWithNull = Arrays.asList("hello", null, 42);

        @SuppressWarnings("unchecked")
        SqlFunction<Object> fn = ctor.newInstance(
                "test_func", argsWithNull, Object.class,
                SqlFunction.FunctionType.CUSTOM, null
        );

        String sql = fn.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("test_func('hello', NULL, 42)", sql);
    }

    @Test
    void testToSqlWithNullTypeThrowsNPE() throws Exception {
        // Tests the null branch in switch(type)
        Constructor<SqlFunction> ctor = SqlFunction.class.getDeclaredConstructor(
                String.class, List.class, Class.class,
                SqlFunction.FunctionType.class, String.class
        );
        ctor.setAccessible(true);

        @SuppressWarnings("unchecked")
        SqlFunction<Object> fn = ctor.newInstance(
                "test_func", List.of(), Object.class,
                null, null
        );

        assertThrows(NullPointerException.class, () -> fn.toSql(PostgreSqlDialect.INSTANCE));
    }
}
