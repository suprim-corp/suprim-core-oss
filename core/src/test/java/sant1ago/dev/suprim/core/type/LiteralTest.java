package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

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

    // ==================== FALLBACK LITERAL ====================

    @Test
    @DisplayName("UUID literal uses fallback to quoteString")
    void testUuidLiteralFallback() {
        java.util.UUID uuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Literal<java.util.UUID> literal = new Literal<>(uuid, java.util.UUID.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("'550e8400-e29b-41d4-a716-446655440000'", sql);
    }
}
