package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JsonbColumn and JsonLiteral.
 */
class JsonbColumnTest {

    @Entity(table = "users")
    static class User {}

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final JsonbColumn<User> META_DATA = new JsonbColumn<>(USERS, "meta_data", "JSONB");

    // ==================== JsonLiteral Tests ====================

    @Test
    void testJsonLiteralGetValueType() {
        JsonbColumn.JsonLiteral literal = new JsonbColumn.JsonLiteral("{\"key\": \"value\"}");
        assertEquals(String.class, literal.getValueType());
    }

    @Test
    void testJsonLiteralToSqlPostgres() {
        // PostgreSQL: adds CAST(... AS jsonb)
        JsonbColumn.JsonLiteral literal = new JsonbColumn.JsonLiteral("{\"key\": \"value\"}");
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.startsWith("CAST("));
        assertTrue(sql.endsWith(" AS jsonb)"));
        assertTrue(sql.contains("{\"key\": \"value\"}"));
    }

    @Test
    void testJsonLiteralToSqlMySQL() {
        // MySQL: no cast needed
        JsonbColumn.JsonLiteral literal = new JsonbColumn.JsonLiteral("{\"key\": \"value\"}");
        String sql = literal.toSql(MySqlDialect.INSTANCE);
        assertFalse(sql.contains("CAST("));
        assertTrue(sql.contains("{\"key\": \"value\"}"));
    }

    @Test
    void testJsonLiteralRecordAccessor() {
        JsonbColumn.JsonLiteral literal = new JsonbColumn.JsonLiteral("{\"test\": 123}");
        assertEquals("{\"test\": 123}", literal.json());
    }

    // ==================== JSONB Predicate Tests (SimplePredicate branches) ====================

    @Test
    void testJsonbContainsWithJsonLiteral() {
        // Tests JSONB_CONTAINS case and extractJsonValue with JsonLiteral
        Predicate predicate = META_DATA.jsonbContains("{\"key\": \"value\"}");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("@>") || sql.contains("JSON_CONTAINS"));
    }

    @Test
    void testJsonbKeyExists() {
        // Tests JSONB_KEY_EXISTS case and extractStringValue
        Predicate predicate = META_DATA.jsonbKeyExists("myKey");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("?") || sql.contains("JSON_CONTAINS_PATH"));
    }

    @Test
    void testJsonbContainsWithMySql() {
        // Tests MySQL dialect path for JSONB_CONTAINS
        Predicate predicate = META_DATA.jsonbContains("{\"active\": true}");
        String sql = predicate.toSql(MySqlDialect.INSTANCE);
        assertTrue(sql.contains("JSON_CONTAINS"));
    }

    @Test
    void testJsonbKeyExistsWithMySql() {
        // Tests MySQL dialect path for JSONB_KEY_EXISTS
        Predicate predicate = META_DATA.jsonbKeyExists("status");
        String sql = predicate.toSql(MySqlDialect.INSTANCE);
        assertTrue(sql.contains("JSON_CONTAINS_PATH"));
    }

    @Test
    void testJsonbContainedBy() {
        Predicate predicate = META_DATA.jsonbContainedBy("{\"a\": 1, \"b\": 2}");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("<@"));
    }

    // ==================== JsonbColumn Method Tests ====================

    @Test
    void testArrow() {
        JsonbColumn.JsonPathExpression expr = META_DATA.arrow("key");
        String sql = expr.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("->"));
        assertTrue(sql.contains("'key'"));
    }

    @Test
    void testArrowText() {
        JsonbColumn.JsonPathExpression expr = META_DATA.arrowText("key");
        String sql = expr.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("->>"));
    }

    @Test
    void testJsonPath() {
        // Tests jsonPath(String...) with multiple keys
        JsonbColumn.JsonPathExpression expr = META_DATA.jsonPath("level1", "level2", "value");
        String sql = expr.toSql(PostgreSqlDialect.INSTANCE);
        assertNotNull(sql);
        assertTrue(sql.contains("meta_data"));
    }

    @Test
    void testJsonPathSingleKey() {
        JsonbColumn.JsonPathExpression expr = META_DATA.jsonPath("key");
        String sql = expr.toSql(PostgreSqlDialect.INSTANCE);
        assertNotNull(sql);
    }

    @Test
    void testJsonPathEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> META_DATA.jsonPath());
    }

    @Test
    void testJsonPathJson() {
        // Tests jsonPathJson(String...) - returns JSONB not text
        JsonbColumn.JsonPathExpression expr = META_DATA.jsonPathJson("nested", "object");
        String sql = expr.toSql(PostgreSqlDialect.INSTANCE);
        assertNotNull(sql);
    }

    @Test
    void testJsonPathJsonSingleKey() {
        JsonbColumn.JsonPathExpression expr = META_DATA.jsonPathJson("data");
        String sql = expr.toSql(PostgreSqlDialect.INSTANCE);
        assertNotNull(sql);
    }

    @Test
    void testJsonPathJsonEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> META_DATA.jsonPathJson());
    }

    // ==================== JsonPathExpression Method Tests ====================

    @Test
    void testJsonPathExpressionAs() {
        // Tests as(String) method
        JsonbColumn.JsonPathExpression expr = META_DATA.arrow("key").as("json_key");
        String sql = expr.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains(" AS json_key"));
    }

    @Test
    void testJsonPathExpressionEq() {
        // Tests eq(String) method
        JsonbColumn.JsonPathExpression expr = META_DATA.arrowText("status");
        Predicate predicate = expr.eq("active");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("="));
        assertTrue(sql.contains("'active'"));
    }

    @Test
    void testJsonPathExpressionIsNull() {
        // Tests isNull() method
        JsonbColumn.JsonPathExpression expr = META_DATA.arrowText("optional_field");
        Predicate predicate = expr.isNull();
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    void testJsonPathExpressionIsNotNull() {
        // Tests isNotNull() method
        JsonbColumn.JsonPathExpression expr = META_DATA.arrowText("required_field");
        Predicate predicate = expr.isNotNull();
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("IS NOT NULL"));
    }

    @Test
    void testJsonPathExpressionGetValueType() {
        // Tests getValueType() method
        JsonbColumn.JsonPathExpression expr = META_DATA.arrow("key");
        assertEquals(String.class, expr.getValueType());
    }

    @Test
    void testJsonPathExpressionWithMySQL() {
        // Tests JsonPathExpression toSql with MySQL dialect
        // MySQL uses ->> with '$.key' format
        JsonbColumn.JsonPathExpression expr = META_DATA.arrowText("name");
        String sql = expr.toSql(MySqlDialect.INSTANCE);
        assertNotNull(sql);
        assertTrue(sql.contains("->>") && sql.contains("$.name"));
    }

    // ==================== extractJsonValue/extractStringValue branch tests ====================

    @Test
    void testExtractJsonValueWithLiteral() {
        // Tests extractJsonValue with Literal (not JsonLiteral) - line 115-116
        Predicate predicate = new Predicate.SimplePredicate(
                META_DATA, Operator.JSONB_CONTAINS, new Literal<>("{\"x\":1}", String.class)
        );
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertNotNull(sql);
    }

    @Test
    void testExtractJsonValueWithExpression() {
        // Tests extractJsonValue else branch (line 118) - non-Literal expression
        Predicate predicate = new Predicate.SimplePredicate(
                META_DATA, Operator.JSONB_CONTAINS, META_DATA  // Column as right expression
        );
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertNotNull(sql);
    }

    @Test
    void testExtractStringValueWithExpression() {
        // Tests extractStringValue else branch (line 125) - non-Literal expression
        Predicate predicate = new Predicate.SimplePredicate(
                META_DATA, Operator.JSONB_KEY_EXISTS, META_DATA  // Column as right expression
        );
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertNotNull(sql);
    }
}
