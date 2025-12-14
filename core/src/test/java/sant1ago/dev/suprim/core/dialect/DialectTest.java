package sant1ago.dev.suprim.core.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SQL dialect implementations.
 */
class DialectTest {

    // ==================== DIALECT CAPABILITIES ====================

    @Test
    void testPostgreSqlHasFullCapabilities() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;
        DialectCapabilities caps = dialect.capabilities();

        assertTrue(caps.supportsReturning());
        assertTrue(caps.supportsIlike());
        assertTrue(caps.supportsArrays());
        assertTrue(caps.supportsJsonb());
        assertTrue(caps.supportsSkipLocked());
        assertTrue(caps.supportsNowait());
        assertTrue(caps.supportsFilterClause());
        assertTrue(caps.supportsDistinctOn());
    }

    @Test
    void testMySql57HasLimitedCapabilities() {
        SqlDialect dialect = MySqlDialect.INSTANCE;
        DialectCapabilities caps = dialect.capabilities();

        assertFalse(caps.supportsReturning());
        assertFalse(caps.supportsIlike());
        assertFalse(caps.supportsArrays());
        assertFalse(caps.supportsJsonb());
        assertFalse(caps.supportsSkipLocked());
        assertFalse(caps.supportsNowait());
        assertFalse(caps.supportsFilterClause());
        assertFalse(caps.supportsDistinctOn());
    }

    @Test
    void testMySql8HasExtendedCapabilities() {
        SqlDialect dialect = MySql8Dialect.INSTANCE;
        DialectCapabilities caps = dialect.capabilities();

        assertFalse(caps.supportsReturning());
        assertFalse(caps.supportsIlike());
        assertFalse(caps.supportsArrays());
        assertFalse(caps.supportsJsonb());
        assertTrue(caps.supportsSkipLocked());
        assertTrue(caps.supportsNowait());
        assertFalse(caps.supportsFilterClause());
        assertFalse(caps.supportsDistinctOn());
    }

    @Test
    void testMariaDbHasEnhancedCapabilities() {
        SqlDialect dialect = MariaDbDialect.INSTANCE;
        DialectCapabilities caps = dialect.capabilities();

        // MariaDB 10.3+ supports SKIP LOCKED and NOWAIT
        assertTrue(caps.supportsSkipLocked());
        assertTrue(caps.supportsNowait());
        // MariaDB 10.5+ supports RETURNING
        assertTrue(caps.supportsReturning());
        // Still lacks PostgreSQL-specific features
        assertFalse(caps.supportsIlike());
        assertFalse(caps.supportsArrays());
        assertFalse(caps.supportsJsonb());
        assertFalse(caps.supportsFilterClause());
        assertFalse(caps.supportsDistinctOn());
    }

    // ==================== IDENTIFIER QUOTING ====================

    @Test
    void testPostgreSqlQuoting() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;

        assertEquals("\"users\"", dialect.quoteIdentifier("users"));
        assertEquals("\"user\"\"name\"", dialect.quoteIdentifier("user\"name"));
        assertNull(dialect.quoteIdentifier(null));
    }

    @Test
    void testMySqlQuoting() {
        SqlDialect dialect = MySqlDialect.INSTANCE;

        assertEquals("`users`", dialect.quoteIdentifier("users"));
        assertEquals("`user``name`", dialect.quoteIdentifier("user`name"));
        assertNull(dialect.quoteIdentifier(null));
    }

    // ==================== STRING ESCAPING ====================

    @Test
    void testPostgreSqlStringEscaping() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;

        assertEquals("'hello'", dialect.quoteString("hello"));
        assertEquals("'it''s'", dialect.quoteString("it's"));
        assertEquals("NULL", dialect.quoteString(null));
    }

    @Test
    void testMySqlStringEscaping() {
        SqlDialect dialect = MySqlDialect.INSTANCE;

        assertEquals("'hello'", dialect.quoteString("hello"));
        assertEquals("'it''s'", dialect.quoteString("it's"));
        assertEquals("'path\\\\to\\\\file'", dialect.quoteString("path\\to\\file"));
        assertEquals("NULL", dialect.quoteString(null));
    }

    // ==================== BOOLEAN FORMATTING ====================

    @Test
    void testBooleanFormatting() {
        assertEquals("TRUE", PostgreSqlDialect.INSTANCE.formatBoolean(true));
        assertEquals("FALSE", PostgreSqlDialect.INSTANCE.formatBoolean(false));
        assertEquals("NULL", PostgreSqlDialect.INSTANCE.formatBoolean(null));

        assertEquals("TRUE", MySqlDialect.INSTANCE.formatBoolean(true));
        assertEquals("FALSE", MySqlDialect.INSTANCE.formatBoolean(false));
        assertEquals("NULL", MySqlDialect.INSTANCE.formatBoolean(null));
    }

    // ==================== JSON HANDLING ====================

    @Test
    void testJsonExtractWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;

        assertEquals("data->>'name'", dialect.jsonExtract("data", "name", true));
        assertEquals("data->'info'", dialect.jsonExtract("data", "info", false));
    }

    @Test
    void testJsonExtractWithMySql() {
        SqlDialect dialect = MySqlDialect.INSTANCE;

        assertEquals("data->>'$.name'", dialect.jsonExtract("data", "name", true));
        assertEquals("data->'$.info'", dialect.jsonExtract("data", "info", false));
    }

    @Test
    void testJsonContainsWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;

        assertEquals("data @> '{\"active\":true}'::jsonb",
                dialect.jsonContains("data", "{\"active\":true}"));
    }

    @Test
    void testJsonContainsWithMySql() {
        SqlDialect dialect = MySqlDialect.INSTANCE;

        assertEquals("JSON_CONTAINS(data, '{\"active\":true}')",
                dialect.jsonContains("data", "{\"active\":true}"));
    }

    @Test
    void testJsonKeyExistsWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;

        assertEquals("data ? 'email'", dialect.jsonKeyExists("data", "email"));
    }

    @Test
    void testJsonKeyExistsWithMySql() {
        SqlDialect dialect = MySqlDialect.INSTANCE;

        assertEquals("JSON_CONTAINS_PATH(data, 'one', '$.email')",
                dialect.jsonKeyExists("data", "email"));
    }

    // ==================== DIALECT NAMES ====================

    @Test
    void testDialectNames() {
        assertEquals("PostgreSQL", PostgreSqlDialect.INSTANCE.getName());
        assertEquals("MySQL", MySqlDialect.INSTANCE.getName());
        assertEquals("MySQL 8", MySql8Dialect.INSTANCE.getName());
        assertEquals("MariaDB", MariaDbDialect.INSTANCE.getName());
    }

    // ==================== COMMERCIAL FLAG ====================

    @Test
    void testFreeDialectsAreNotCommercial() {
        assertFalse(PostgreSqlDialect.INSTANCE.isCommercial());
        assertFalse(MySqlDialect.INSTANCE.isCommercial());
        assertFalse(MySql8Dialect.INSTANCE.isCommercial());
        assertFalse(MariaDbDialect.INSTANCE.isCommercial());
    }

    // ==================== ILIKE FALLBACK ====================

    @Test
    void testIlikeWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;
        assertEquals("email ILIKE :pattern", dialect.ilike("email", ":pattern"));
        assertEquals("email NOT ILIKE :pattern", dialect.notIlike("email", ":pattern"));
    }

    @Test
    void testIlikeFallbackWithMySQL() {
        SqlDialect dialect = MySqlDialect.INSTANCE;
        // MySQL doesn't support ILIKE, falls back to LOWER()
        assertEquals("LOWER(email) LIKE LOWER(:pattern)", dialect.ilike("email", ":pattern"));
        assertEquals("LOWER(email) NOT LIKE LOWER(:pattern)", dialect.notIlike("email", ":pattern"));
    }

    // ==================== FOR UPDATE/SHARE ====================

    @Test
    void testForUpdateWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;
        assertEquals("FOR UPDATE", dialect.forUpdate(false, false));
        assertEquals("FOR UPDATE NOWAIT", dialect.forUpdate(true, false));
        assertEquals("FOR UPDATE SKIP LOCKED", dialect.forUpdate(false, true));
    }

    @Test
    void testForUpdateThrowsForUnsupportedFeatures() {
        SqlDialect dialect = MySqlDialect.INSTANCE;
        assertEquals("FOR UPDATE", dialect.forUpdate(false, false));

        // MySQL 5.7 doesn't support NOWAIT
        UnsupportedDialectFeatureException ex = assertThrows(
                UnsupportedDialectFeatureException.class,
                () -> dialect.forUpdate(true, false)
        );
        assertEquals("NOWAIT", ex.getFeature());
        assertEquals("MySQL", ex.getDialectName());
    }

    @Test
    void testForUpdateWithMySql8() {
        SqlDialect dialect = MySql8Dialect.INSTANCE;
        assertEquals("FOR UPDATE NOWAIT", dialect.forUpdate(true, false));
        assertEquals("FOR UPDATE SKIP LOCKED", dialect.forUpdate(false, true));
    }

    @Test
    void testForUpdateWithMariaDb() {
        SqlDialect dialect = MariaDbDialect.INSTANCE;
        assertEquals("FOR UPDATE", dialect.forUpdate(false, false));
        assertEquals("FOR UPDATE NOWAIT", dialect.forUpdate(true, false));
        assertEquals("FOR UPDATE SKIP LOCKED", dialect.forUpdate(false, true));
    }

    @Test
    void testForShareWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;
        assertEquals("FOR SHARE", dialect.forShare(false, false));
        assertEquals("FOR SHARE NOWAIT", dialect.forShare(true, false));
        assertEquals("FOR SHARE SKIP LOCKED", dialect.forShare(false, true));
    }

    @Test
    void testForShareThrowsForUnsupportedFeatures() {
        SqlDialect dialect = MySqlDialect.INSTANCE;
        assertEquals("FOR SHARE", dialect.forShare(false, false));

        // MySQL 5.7 doesn't support NOWAIT
        UnsupportedDialectFeatureException nowaitEx = assertThrows(
                UnsupportedDialectFeatureException.class,
                () -> dialect.forShare(true, false)
        );
        assertEquals("NOWAIT", nowaitEx.getFeature());

        // MySQL 5.7 doesn't support SKIP LOCKED
        UnsupportedDialectFeatureException skipLockedEx = assertThrows(
                UnsupportedDialectFeatureException.class,
                () -> dialect.forShare(false, true)
        );
        assertEquals("SKIP LOCKED", skipLockedEx.getFeature());
    }

    @Test
    void testForShareWithMySql8() {
        SqlDialect dialect = MySql8Dialect.INSTANCE;
        assertEquals("FOR SHARE NOWAIT", dialect.forShare(true, false));
        assertEquals("FOR SHARE SKIP LOCKED", dialect.forShare(false, true));
    }

    @Test
    void testForShareWithMariaDb() {
        SqlDialect dialect = MariaDbDialect.INSTANCE;
        assertEquals("FOR SHARE", dialect.forShare(false, false));
        assertEquals("FOR SHARE NOWAIT", dialect.forShare(true, false));
        assertEquals("FOR SHARE SKIP LOCKED", dialect.forShare(false, true));
    }

    // ==================== RETURNING CLAUSE ====================

    @Test
    void testReturningWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;
        assertEquals("RETURNING id, name", dialect.returning("id, name"));
    }

    @Test
    void testReturningThrowsForMySQL() {
        SqlDialect dialect = MySqlDialect.INSTANCE;
        UnsupportedDialectFeatureException ex = assertThrows(
                UnsupportedDialectFeatureException.class,
                () -> dialect.returning("id")
        );
        assertEquals("RETURNING", ex.getFeature());
    }

    @Test
    void testReturningWithMariaDb() {
        SqlDialect dialect = MariaDbDialect.INSTANCE;
        // MariaDB 10.5+ supports RETURNING
        assertEquals("RETURNING id, name", dialect.returning("id, name"));
    }

    // ==================== ARRAY OPERATIONS ====================

    @Test
    void testArrayContainsWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;
        assertEquals("tags @> ARRAY['java']", dialect.arrayContains("tags", "'java'"));
    }

    @Test
    void testArrayContainsThrowsForMySQL() {
        SqlDialect dialect = MySqlDialect.INSTANCE;
        UnsupportedDialectFeatureException ex = assertThrows(
                UnsupportedDialectFeatureException.class,
                () -> dialect.arrayContains("tags", "'java'")
        );
        assertEquals("ARRAY", ex.getFeature());
        assertTrue(ex.getMessage().contains("JSON_CONTAINS"));
    }

    // ==================== AGGREGATE FILTER ====================

    @Test
    void testAggregateFilterWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;
        assertEquals("COUNT(*) FILTER (WHERE active = true)",
                dialect.aggregateFilter("COUNT(*)", "active = true"));
    }

    @Test
    void testAggregateFilterThrowsForMySQL() {
        SqlDialect dialect = MySqlDialect.INSTANCE;
        UnsupportedDialectFeatureException ex = assertThrows(
                UnsupportedDialectFeatureException.class,
                () -> dialect.aggregateFilter("COUNT(*)", "active = true")
        );
        assertEquals("FILTER clause", ex.getFeature());
        assertTrue(ex.getMessage().contains("CASE"));
    }

    // ==================== DISTINCT ON ====================

    @Test
    void testDistinctOnWithPostgreSql() {
        SqlDialect dialect = PostgreSqlDialect.INSTANCE;
        assertEquals("DISTINCT ON (user_id)", dialect.distinctOn("user_id"));
    }

    @Test
    void testDistinctOnThrowsForMySQL() {
        SqlDialect dialect = MySqlDialect.INSTANCE;
        UnsupportedDialectFeatureException ex = assertThrows(
                UnsupportedDialectFeatureException.class,
                () -> dialect.distinctOn("user_id")
        );
        assertEquals("DISTINCT ON", ex.getFeature());
    }
}
