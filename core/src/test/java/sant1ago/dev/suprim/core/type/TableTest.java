package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Table class.
 */
class TableTest {

    @Entity(table = "users")
    static class User {}

    // ==================== Factory Methods ====================

    @Test
    void testOfWithNameAndEntityType() {
        Table<User> table = Table.of("users", User.class);
        assertEquals("users", table.getName());
        assertEquals(User.class, table.getEntityType());
        assertEquals("", table.getSchema());
        assertNull(table.getAlias());
    }

    @Test
    void testOfWithNameSchemaAndEntityType() {
        // Tests of(String, String, Class) - 3-param factory
        Table<User> table = Table.of("users", "public", User.class);
        assertEquals("users", table.getName());
        assertEquals("public", table.getSchema());
        assertEquals(User.class, table.getEntityType());
        assertNull(table.getAlias());
    }

    // ==================== as() method ====================

    @Test
    void testAs() {
        // Tests as(String) and the private 4-param constructor
        Table<User> table = Table.of("users", User.class);
        Table<User> aliased = table.as("u");

        assertEquals("users", aliased.getName());
        assertEquals("u", aliased.getAlias());
        assertEquals(User.class, aliased.getEntityType());
    }

    @Test
    void testAsWithSchema() {
        Table<User> table = Table.of("users", "public", User.class);
        Table<User> aliased = table.as("u");

        assertEquals("users", aliased.getName());
        assertEquals("public", aliased.getSchema());
        assertEquals("u", aliased.getAlias());
    }

    // ==================== getQualifiedName() ====================

    @Test
    void testGetQualifiedNameWithSchema() {
        // Tests schema + "." + name branch
        Table<User> table = Table.of("users", "public", User.class);
        assertEquals("public.users", table.getQualifiedName());
    }

    @Test
    void testGetQualifiedNameWithEmptySchema() {
        // Tests empty schema branch - returns just name
        Table<User> table = Table.of("users", "", User.class);
        assertEquals("users", table.getQualifiedName());
    }

    @Test
    void testGetQualifiedNameWithNullSchema() {
        // Tests null schema branch - returns just name
        Table<User> table = Table.of("users", null, User.class);
        assertEquals("users", table.getQualifiedName());
    }

    @Test
    void testGetQualifiedNameWithoutSchema() {
        // Tests of(name, entityType) which sets empty schema
        Table<User> table = Table.of("users", User.class);
        assertEquals("users", table.getQualifiedName());
    }

    // ==================== getSqlReference() ====================

    @Test
    void testGetSqlReferenceWithAlias() {
        // Tests alias != null branch
        Table<User> table = Table.of("users", User.class).as("u");
        assertEquals("u", table.getSqlReference());
    }

    @Test
    void testGetSqlReferenceWithoutAlias() {
        // Tests alias == null branch - returns name
        Table<User> table = Table.of("users", User.class);
        assertEquals("users", table.getSqlReference());
    }

    // ==================== toSql() ====================

    @Test
    void testToSqlWithSchemaAndAlias() {
        // Tests schema non-empty + alias set branch
        Table<User> table = Table.of("users", "public", User.class).as("u");
        String sql = table.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("\"public\".\"users\""));
        assertTrue(sql.endsWith(" u"));
    }

    @Test
    void testToSqlWithSchemaNoAlias() {
        // Tests schema non-empty + no alias
        Table<User> table = Table.of("users", "public", User.class);
        String sql = table.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("\"public\".\"users\"", sql);
    }

    @Test
    void testToSqlWithoutSchemaWithAlias() {
        // Tests no schema + alias set
        Table<User> table = Table.of("users", User.class).as("u");
        String sql = table.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("\"users\" u", sql);
    }

    @Test
    void testToSqlWithoutSchemaNoAlias() {
        // Tests no schema + no alias
        Table<User> table = Table.of("users", User.class);
        String sql = table.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("\"users\"", sql);
    }

    @Test
    void testToSqlWithNullSchema() {
        // Tests null schema path
        Table<User> table = Table.of("users", null, User.class);
        String sql = table.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("\"users\"", sql);
    }

    @Test
    void testToSqlWithMySqlDialect() {
        // Tests with MySQL dialect (uses backticks)
        Table<User> table = Table.of("users", "mydb", User.class);
        String sql = table.toSql(MySqlDialect.INSTANCE);
        assertTrue(sql.contains("`mydb`.`users`"));
    }

    // ==================== Getters ====================

    @Test
    void testGetSchema() {
        Table<User> table = Table.of("users", "public", User.class);
        assertEquals("public", table.getSchema());
    }

    @Test
    void testGetAlias() {
        Table<User> table = Table.of("users", User.class).as("alias");
        assertEquals("alias", table.getAlias());
    }

    @Test
    void testGetAliasWhenNull() {
        Table<User> table = Table.of("users", User.class);
        assertNull(table.getAlias());
    }
}
