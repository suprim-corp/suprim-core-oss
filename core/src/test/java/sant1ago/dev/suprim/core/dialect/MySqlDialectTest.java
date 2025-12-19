package sant1ago.dev.suprim.core.dialect;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MySqlDialect.
 */
class MySqlDialectTest {

    private final MySqlDialect dialect = MySqlDialect.INSTANCE;

    @Test
    void quoteIdentifier_quotesWithBackticks() {
        assertEquals("`users`", dialect.quoteIdentifier("users"));
    }

    @Test
    void quoteIdentifier_escapesBackticks() {
        assertEquals("`user``name`", dialect.quoteIdentifier("user`name"));
    }

    @Test
    void quoteIdentifier_nullReturnsNull() {
        assertNull(dialect.quoteIdentifier(null));
    }

    @Test
    void quoteString_quotesWithSingleQuotes() {
        assertEquals("'hello'", dialect.quoteString("hello"));
    }

    @Test
    void quoteString_escapesBackslashes() {
        assertEquals("'path\\\\to\\\\file'", dialect.quoteString("path\\to\\file"));
    }

    @Test
    void quoteString_escapesSingleQuotes() {
        assertEquals("'it''s'", dialect.quoteString("it's"));
    }

    @Test
    void quoteString_nullReturnsNullLiteral() {
        assertEquals("NULL", dialect.quoteString(null));
    }

    @Test
    void formatBoolean_trueReturnsTrue() {
        assertEquals("TRUE", dialect.formatBoolean(true));
    }

    @Test
    void formatBoolean_falseReturnsFalse() {
        assertEquals("FALSE", dialect.formatBoolean(false));
    }

    @Test
    void formatBoolean_nullReturnsNullLiteral() {
        assertEquals("NULL", dialect.formatBoolean(null));
    }

    @Test
    void getName_returnsMySQL() {
        assertEquals("MySQL", dialect.getName());
    }

    @Test
    void capabilities_returnsMySQL57() {
        assertEquals(DialectCapabilities.MYSQL_5_7, dialect.capabilities());
    }

    @Test
    void formatUuid_quotesUuidString() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("'550e8400-e29b-41d4-a716-446655440000'", dialect.formatUuid(uuid));
    }

    @Test
    void formatUuidParameter_returnsParameterName() {
        assertEquals(":userId", dialect.formatUuidParameter("userId"));
    }

    @Test
    void jsonExtract_asText_usesArrowArrowOperator() {
        assertEquals("data->>'$.name'", dialect.jsonExtract("data", "name", true));
    }

    @Test
    void jsonExtract_notAsText_usesArrowOperator() {
        assertEquals("data->'$.name'", dialect.jsonExtract("data", "name", false));
    }

    @Test
    void jsonContains_formatsCorrectly() {
        assertEquals("JSON_CONTAINS(data, '{\"key\":\"value\"}')", dialect.jsonContains("data", "{\"key\":\"value\"}"));
    }

    @Test
    void jsonKeyExists_formatsCorrectly() {
        assertEquals("JSON_CONTAINS_PATH(data, 'one', '$.name')", dialect.jsonKeyExists("data", "name"));
    }

    @Test
    void instance_isSingleton() {
        assertSame(MySqlDialect.INSTANCE, MySqlDialect.INSTANCE);
    }
}
