package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.query.ParameterContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Literal values.
 */
@DisplayName("Literal Tests")
class LiteralTest {

    // ==================== STRING LITERALS ====================

    @Test
    @DisplayName("String literal renders with quotes")
    void testStringLiteral() {
        Literal<String> literal = new Literal<>("test@example.com", String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("'test@example.com'", sql);
    }

    @Test
    @DisplayName("Empty string literal")
    void testEmptyStringLiteral() {
        Literal<String> literal = new Literal<>("", String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("''", sql);
    }

    @Test
    @DisplayName("String literal with single quote escaping (PostgreSQL)")
    void testStringLiteralWithQuotePostgres() {
        Literal<String> literal = new Literal<>("O'Brien", String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        // PostgreSQL escapes single quotes by doubling them
        assertEquals("'O''Brien'", sql);
    }

    @Test
    @DisplayName("String literal with special characters")
    void testStringLiteralWithSpecialChars() {
        Literal<String> literal = new Literal<>("Test\nNew\tLine", String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.startsWith("'"));
        assertTrue(sql.endsWith("'"));
    }

    // ==================== NUMBER LITERALS ====================

    @Test
    @DisplayName("Integer literal")
    void testIntegerLiteral() {
        Literal<Integer> literal = new Literal<>(42, Integer.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("42", sql);
    }

    @Test
    @DisplayName("Long literal")
    void testLongLiteral() {
        Literal<Long> literal = new Literal<>(1234567890L, Long.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("1234567890", sql);
    }

    @Test
    @DisplayName("Double literal")
    void testDoubleLiteral() {
        Literal<Double> literal = new Literal<>(3.14159, Double.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("3.14159", sql);
    }

    @Test
    @DisplayName("Negative number literal")
    void testNegativeNumberLiteral() {
        Literal<Integer> literal = new Literal<>(-100, Integer.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("-100", sql);
    }

    @Test
    @DisplayName("Zero literal")
    void testZeroLiteral() {
        Literal<Integer> literal = new Literal<>(0, Integer.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("0", sql);
    }

    // ==================== BOOLEAN LITERALS ====================

    @Test
    @DisplayName("Boolean true literal (PostgreSQL)")
    void testBooleanTrueLiteralPostgres() {
        Literal<Boolean> literal = new Literal<>(true, Boolean.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("TRUE", sql);
    }

    @Test
    @DisplayName("Boolean false literal (PostgreSQL)")
    void testBooleanFalseLiteralPostgres() {
        Literal<Boolean> literal = new Literal<>(false, Boolean.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("FALSE", sql);
    }

    @Test
    @DisplayName("Boolean true literal (MySQL)")
    void testBooleanTrueLiteralMySql() {
        Literal<Boolean> literal = new Literal<>(true, Boolean.class);
        String sql = literal.toSql(MySqlDialect.INSTANCE);

        // MySQL uses TRUE for true
        assertEquals("TRUE", sql);
    }

    @Test
    @DisplayName("Boolean false literal (MySQL)")
    void testBooleanFalseLiteralMySql() {
        Literal<Boolean> literal = new Literal<>(false, Boolean.class);
        String sql = literal.toSql(MySqlDialect.INSTANCE);

        // MySQL uses FALSE for false
        assertEquals("FALSE", sql);
    }

    // ==================== DATE/TIME LITERALS ====================

    @Test
    @DisplayName("LocalDate literal")
    void testLocalDateLiteral() {
        LocalDate date = LocalDate.of(2024, 12, 13);
        Literal<LocalDate> literal = new Literal<>(date, LocalDate.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("'2024-12-13'", sql);
    }

    @Test
    @DisplayName("LocalDateTime literal")
    void testLocalDateTimeLiteral() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 13, 15, 30, 45);
        Literal<LocalDateTime> literal = new Literal<>(dateTime, LocalDateTime.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("'2024-12-13T15:30:45'", sql);
    }

    @Test
    @DisplayName("LocalDateTime with microseconds")
    void testLocalDateTimeWithMicroseconds() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 13, 15, 30, 45, 123456789);
        Literal<LocalDateTime> literal = new Literal<>(dateTime, LocalDateTime.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.startsWith("'2024-12-13T15:30:45"));
        assertTrue(sql.endsWith("'"));
    }

    // ==================== NULL LITERALS ====================

    @Test
    @DisplayName("Null literal (PostgreSQL)")
    void testNullLiteralPostgres() {
        Literal<String> literal = new Literal<>(null, String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("NULL", sql);
    }

    @Test
    @DisplayName("Null literal (MySQL)")
    void testNullLiteralMySql() {
        Literal<Integer> literal = new Literal<>(null, Integer.class);
        String sql = literal.toSql(MySqlDialect.INSTANCE);

        assertEquals("NULL", sql);
    }

    // ==================== VALUE TYPE ====================

    @Test
    @DisplayName("Literal getValueType() returns correct type")
    void testLiteralValueType() {
        Literal<String> stringLiteral = new Literal<>("test", String.class);
        assertEquals(String.class, stringLiteral.getValueType());

        Literal<Integer> intLiteral = new Literal<>(42, Integer.class);
        assertEquals(Integer.class, intLiteral.getValueType());

        Literal<Boolean> boolLiteral = new Literal<>(true, Boolean.class);
        assertEquals(Boolean.class, boolLiteral.getValueType());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Very long string literal")
    void testVeryLongStringLiteral() {
        String longString = "a".repeat(1000);
        Literal<String> literal = new Literal<>(longString, String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.startsWith("'"));
        assertTrue(sql.endsWith("'"));
        assertTrue(sql.length() > 1000);
    }

    @Test
    @DisplayName("String with newlines and tabs")
    void testStringWithWhitespace() {
        Literal<String> literal = new Literal<>("Line1\nLine2\tTab", String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("\n"));
        assertTrue(sql.contains("\t"));
    }

    @Test
    @DisplayName("Large integer literal")
    void testLargeIntegerLiteral() {
        Literal<Long> literal = new Literal<>(9223372036854775807L, Long.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("9223372036854775807", sql);
    }

    @Test
    @DisplayName("Decimal literal")
    void testDecimalLiteral() {
        java.math.BigDecimal decimal = new java.math.BigDecimal("123.456789");
        Literal<java.math.BigDecimal> literal = new Literal<>(decimal, java.math.BigDecimal.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("123.456789", sql);
    }

    @Test
    @DisplayName("Unicode string literal")
    void testUnicodeStringLiteral() {
        Literal<String> literal = new Literal<>("Hello 世界 🌍", String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.startsWith("'"));
        assertTrue(sql.endsWith("'"));
        assertTrue(sql.contains("世界"));
        assertTrue(sql.contains("🌍"));
    }

    // ==================== LIST LITERAL ====================

    @Test
    @DisplayName("ListLiteral with integers")
    void testListLiteralIntegers() {
        ListLiteral<Integer> listLiteral = new ListLiteral<>(
            java.util.List.of(1, 2, 3, 4, 5),
            Integer.class
        );
        String sql = listLiteral.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("1, 2, 3, 4, 5", sql);
    }

    @Test
    @DisplayName("ListLiteral with strings")
    void testListLiteralStrings() {
        ListLiteral<String> listLiteral = new ListLiteral<>(
            java.util.List.of("apple", "banana", "cherry"),
            String.class
        );
        String sql = listLiteral.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("'apple', 'banana', 'cherry'", sql);
    }

    @Test
    @DisplayName("Empty ListLiteral")
    void testEmptyListLiteral() {
        ListLiteral<Integer> listLiteral = new ListLiteral<>(
            java.util.List.of(),
            Integer.class
        );
        String sql = listLiteral.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("", sql);
    }

    @Test
    @DisplayName("ListLiteral with single item")
    void testListLiteralSingleItem() {
        ListLiteral<Integer> listLiteral = new ListLiteral<>(
            java.util.List.of(42),
            Integer.class
        );
        String sql = listLiteral.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("42", sql);
    }

    @Test
    @DisplayName("ListLiteral with null values returns empty string")
    void testListLiteralNullValues() {
        ListLiteral<Integer> listLiteral = new ListLiteral<>(null, Integer.class);
        String sql = listLiteral.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("", sql);
    }

    @Test
    @DisplayName("ListLiteral getValueType returns element type")
    void testListLiteralGetValueType() {
        ListLiteral<String> listLiteral = new ListLiteral<>(
            java.util.List.of("a", "b"),
            String.class
        );
        assertEquals(String.class, listLiteral.getValueType());
    }

    // ==================== UUID LITERALS ====================

    @Test
    @DisplayName("UUID literal formats with dialect-specific casting")
    void testUuidLiteralWithCast() {
        java.util.UUID uuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Literal<java.util.UUID> literal = new Literal<>(uuid, java.util.UUID.class);

        // PostgreSQL casts to uuid type
        String pgSql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("CAST('550e8400-e29b-41d4-a716-446655440000' AS uuid)", pgSql);

        // MySQL just quotes (stored as CHAR(36))
        String mysqlSql = literal.toSql(MySqlDialect.INSTANCE);
        assertEquals("'550e8400-e29b-41d4-a716-446655440000'", mysqlSql);
    }

    @Test
    @DisplayName("UUID v7 literal formats correctly")
    void testUuidV7Literal() {
        // UUID v7 format
        java.util.UUID uuid = java.util.UUID.fromString("019b1d5d-7a8e-7000-8000-000000000001");
        Literal<java.util.UUID> literal = new Literal<>(uuid, java.util.UUID.class);

        String pgSql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("CAST('019b1d5d-7a8e-7000-8000-000000000001' AS uuid)", pgSql);
    }

    @Test
    @DisplayName("Null UUID literal")
    void testNullUuidLiteral() {
        Literal<java.util.UUID> literal = new Literal<>(null, java.util.UUID.class);

        String pgSql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("NULL", pgSql);

        String mysqlSql = literal.toSql(MySqlDialect.INSTANCE);
        assertEquals("NULL", mysqlSql);
    }

    @Test
    @DisplayName("ListLiteral with UUIDs formats correctly")
    void testListLiteralUuids() {
        java.util.UUID uuid1 = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        java.util.UUID uuid2 = java.util.UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
        ListLiteral<java.util.UUID> listLiteral = new ListLiteral<>(
            java.util.List.of(uuid1, uuid2),
            java.util.UUID.class
        );

        // PostgreSQL casts each UUID
        String pgSql = listLiteral.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("CAST('550e8400-e29b-41d4-a716-446655440000' AS uuid), CAST('6ba7b810-9dad-11d1-80b4-00c04fd430c8' AS uuid)", pgSql);

        // MySQL just quotes
        String mysqlSql = listLiteral.toSql(MySqlDialect.INSTANCE);
        assertEquals("'550e8400-e29b-41d4-a716-446655440000', '6ba7b810-9dad-11d1-80b4-00c04fd430c8'", mysqlSql);
    }

    @Test
    @DisplayName("Single UUID in ListLiteral")
    void testListLiteralSingleUuid() {
        java.util.UUID uuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        ListLiteral<java.util.UUID> listLiteral = new ListLiteral<>(
            java.util.List.of(uuid),
            java.util.UUID.class
        );

        String pgSql = listLiteral.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("CAST('550e8400-e29b-41d4-a716-446655440000' AS uuid)", pgSql);
    }

    // ==================== UUID STRING LITERALS ====================
    // Strings are NOT auto-cast to UUID - use UUID type for UUID columns

    @Test
    @DisplayName("UUID-format string literal stays as string (no auto-cast)")
    void testUuidStringLiteralNoAutoCast() {
        // String that looks like UUID - stays as string for VARCHAR columns
        Literal<String> literal = new Literal<>("550e8400-e29b-41d4-a716-446655440000", String.class);

        // PostgreSQL: no auto-cast, just quoted string
        String pgSql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("'550e8400-e29b-41d4-a716-446655440000'", pgSql);

        // MySQL: same behavior
        String mysqlSql = literal.toSql(MySqlDialect.INSTANCE);
        assertEquals("'550e8400-e29b-41d4-a716-446655440000'", mysqlSql);
    }

    @Test
    @DisplayName("Non-UUID string literal stays as regular string")
    void testNonUuidStringLiteralNoAutoCast() {
        Literal<String> literal = new Literal<>("not-a-uuid-string", String.class);

        String pgSql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("'not-a-uuid-string'", pgSql);
    }

    @Test
    @DisplayName("UUID v7 format string stays as string (no auto-cast)")
    void testUuidV7StringNoAutoCast() {
        // UUID v7 format as string - stays as string
        Literal<String> literal = new Literal<>("019b1d5d-7a8e-7000-8000-000000000001", String.class);

        String pgSql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("'019b1d5d-7a8e-7000-8000-000000000001'", pgSql);
    }

    @Test
    @DisplayName("Uppercase UUID-format string stays as string")
    void testUppercaseUuidStringNoAutoCast() {
        Literal<String> literal = new Literal<>("550E8400-E29B-41D4-A716-446655440000", String.class);

        // No conversion, stays as-is
        String pgSql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("'550E8400-E29B-41D4-A716-446655440000'", pgSql);
    }

    @Test
    @DisplayName("ListLiteral with UUID-format strings stays as strings")
    void testListLiteralUuidStringsNoAutoCast() {
        ListLiteral<String> listLiteral = new ListLiteral<>(
            java.util.List.of("550e8400-e29b-41d4-a716-446655440000", "6ba7b810-9dad-11d1-80b4-00c04fd430c8"),
            String.class
        );

        // No UUID cast - just quoted strings
        String pgSql = listLiteral.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("'550e8400-e29b-41d4-a716-446655440000', '6ba7b810-9dad-11d1-80b4-00c04fd430c8'", pgSql);
    }

    // ==================== FALLBACK LITERAL ====================

    @Test
    @DisplayName("Custom object falls back to quoteString")
    void testCustomObjectFallback() {
        // Custom object that's not explicitly handled
        record CustomValue(String data) {
            @Override
            public String toString() {
                return "custom:" + data;
            }
        }
        Literal<CustomValue> literal = new Literal<>(new CustomValue("test"), null);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("'custom:test'", sql);
    }

    // ==================== PARAMETERIZED LITERAL TESTS ====================

    @Nested
    @DisplayName("Parameterized Literals")
    class ParameterizedLiteralTests {

        @Test
        @DisplayName("String literal parameterized")
        void testParameterizedStringLiteral() {
            ParameterContext params = new ParameterContext();
            Literal<String> literal = new Literal<>("test@example.com", String.class);
            String sql = literal.toSql(PostgreSqlDialect.INSTANCE, params);

            assertEquals(":p1", sql);
            assertEquals("test@example.com", params.getParameters().get("p1"));
        }

        @Test
        @DisplayName("Integer literal parameterized")
        void testParameterizedIntegerLiteral() {
            ParameterContext params = new ParameterContext();
            Literal<Integer> literal = new Literal<>(42, Integer.class);
            String sql = literal.toSql(PostgreSqlDialect.INSTANCE, params);

            assertEquals(":p1", sql);
            assertEquals(42, params.getParameters().get("p1"));
        }

        @Test
        @DisplayName("Null literal parameterized returns NULL")
        void testParameterizedNullLiteral() {
            ParameterContext params = new ParameterContext();
            Literal<String> literal = new Literal<>(null, String.class);
            String sql = literal.toSql(PostgreSqlDialect.INSTANCE, params);

            assertEquals("NULL", sql);
            assertTrue(params.getParameters().isEmpty());
        }

        @Test
        @DisplayName("UUID literal parameterized with CAST for PostgreSQL")
        void testParameterizedUuidPostgresql() {
            ParameterContext params = new ParameterContext();
            java.util.UUID uuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            Literal<java.util.UUID> literal = new Literal<>(uuid, java.util.UUID.class);
            String sql = literal.toSql(PostgreSqlDialect.INSTANCE, params);

            assertEquals("CAST(:p1 AS uuid)", sql);
            assertEquals(uuid, params.getParameters().get("p1"));
        }

        @Test
        @DisplayName("UUID literal parameterized without CAST for MySQL")
        void testParameterizedUuidMysql() {
            ParameterContext params = new ParameterContext();
            java.util.UUID uuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            Literal<java.util.UUID> literal = new Literal<>(uuid, java.util.UUID.class);
            String sql = literal.toSql(MySqlDialect.INSTANCE, params);

            assertEquals(":p1", sql);
            assertEquals(uuid, params.getParameters().get("p1"));
        }

        @Test
        @DisplayName("Multiple literals use incremental parameter names")
        void testParameterizedMultipleLiterals() {
            ParameterContext params = new ParameterContext();
            Literal<String> l1 = new Literal<>("one", String.class);
            Literal<String> l2 = new Literal<>("two", String.class);
            Literal<Integer> l3 = new Literal<>(3, Integer.class);

            assertEquals(":p1", l1.toSql(PostgreSqlDialect.INSTANCE, params));
            assertEquals(":p2", l2.toSql(PostgreSqlDialect.INSTANCE, params));
            assertEquals(":p3", l3.toSql(PostgreSqlDialect.INSTANCE, params));

            assertEquals("one", params.getParameters().get("p1"));
            assertEquals("two", params.getParameters().get("p2"));
            assertEquals(3, params.getParameters().get("p3"));
        }

        @Test
        @DisplayName("ListLiteral parameterized")
        void testParameterizedListLiteral() {
            ParameterContext params = new ParameterContext();
            ListLiteral<Integer> listLiteral = new ListLiteral<>(
                    java.util.List.of(1, 2, 3), Integer.class
            );
            String sql = listLiteral.toSql(PostgreSqlDialect.INSTANCE, params);

            assertEquals(":p1, :p2, :p3", sql);
            assertEquals(1, params.getParameters().get("p1"));
            assertEquals(2, params.getParameters().get("p2"));
            assertEquals(3, params.getParameters().get("p3"));
        }

        @Test
        @DisplayName("Empty ListLiteral parameterized")
        void testParameterizedEmptyListLiteral() {
            ParameterContext params = new ParameterContext();
            ListLiteral<Integer> listLiteral = new ListLiteral<>(
                    java.util.List.of(), Integer.class
            );
            String sql = listLiteral.toSql(PostgreSqlDialect.INSTANCE, params);

            assertEquals("", sql);
            assertTrue(params.getParameters().isEmpty());
        }

        @Test
        @DisplayName("Null ListLiteral parameterized")
        void testParameterizedNullListLiteral() {
            ParameterContext params = new ParameterContext();
            ListLiteral<Integer> listLiteral = new ListLiteral<>(null, Integer.class);
            String sql = listLiteral.toSql(PostgreSqlDialect.INSTANCE, params);

            assertEquals("", sql);
            assertTrue(params.getParameters().isEmpty());
        }

        @Test
        @DisplayName("ListLiteral with UUIDs parameterized for PostgreSQL")
        void testParameterizedListLiteralUuidsPostgres() {
            ParameterContext params = new ParameterContext();
            java.util.UUID uuid1 = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            java.util.UUID uuid2 = java.util.UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
            ListLiteral<java.util.UUID> listLiteral = new ListLiteral<>(
                    java.util.List.of(uuid1, uuid2), java.util.UUID.class
            );
            String sql = listLiteral.toSql(PostgreSqlDialect.INSTANCE, params);

            assertEquals("CAST(:p1 AS uuid), CAST(:p2 AS uuid)", sql);
            assertEquals(uuid1, params.getParameters().get("p1"));
            assertEquals(uuid2, params.getParameters().get("p2"));
        }
    }

    // ==================== PARAMETER CONTEXT TESTS ====================

    @Nested
    @DisplayName("ParameterContext")
    class ParameterContextTests {

        @Test
        @DisplayName("getCount returns correct count")
        void testGetCount() {
            ParameterContext params = new ParameterContext();
            assertEquals(0, params.getCount());

            params.addParameter("value1");
            assertEquals(1, params.getCount());

            params.addParameter("value2");
            assertEquals(2, params.getCount());

            params.addParameter(123);
            assertEquals(3, params.getCount());
        }

        @Test
        @DisplayName("addParameter returns incremental names")
        void testAddParameterNames() {
            ParameterContext params = new ParameterContext();

            assertEquals("p1", params.addParameter("first"));
            assertEquals("p2", params.addParameter("second"));
            assertEquals("p3", params.addParameter("third"));
        }

        @Test
        @DisplayName("getParameters returns all added parameters")
        void testGetParameters() {
            ParameterContext params = new ParameterContext();
            params.addParameter("value1");
            params.addParameter(42);
            params.addParameter(true);

            var map = params.getParameters();
            assertEquals(3, map.size());
            assertEquals("value1", map.get("p1"));
            assertEquals(42, map.get("p2"));
            assertEquals(true, map.get("p3"));
        }
    }
}
