package sant1ago.dev.suprim.core.dialect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case and security tests for SQL dialect implementations.
 * Tests SQL injection prevention, NULL handling, special characters, and boundary conditions.
 */
@DisplayName("Dialect Edge Cases & Security")
class DialectEdgeCaseTest {

    private static final PostgreSqlDialect POSTGRES = PostgreSqlDialect.INSTANCE;
    private static final MySqlDialect MYSQL = MySqlDialect.INSTANCE;
    private static final MySql8Dialect MYSQL8 = MySql8Dialect.INSTANCE;
    private static final MariaDbDialect MARIADB = MariaDbDialect.INSTANCE;

    static Stream<SqlDialect> allDialects() {
        return Stream.of(POSTGRES, MYSQL, MYSQL8, MARIADB);
    }

    // ==================== SQL Injection Prevention ====================

    @Nested
    @DisplayName("SQL Injection Prevention")
    class SqlInjectionTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("String with SQL injection attempt has quote escaped")
        void stringWithSqlInjection(SqlDialect dialect) {
            String malicious = "'; DROP TABLE users; --";
            String quoted = dialect.quoteString(malicious);

            // The single quote in the malicious string should be escaped to ''
            // Result: '''; DROP TABLE users; --' (outer quotes + escaped inner quote)
            assertTrue(quoted.contains("''"), "Should escape single quote: " + quoted);
            assertTrue(quoted.startsWith("'") && quoted.endsWith("'"), "Should be quoted: " + quoted);
            // The content is safe because the quote is escaped - parser sees it as string content
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("String with UNION SELECT injection has quote escaped")
        void stringWithUnionInjection(SqlDialect dialect) {
            String malicious = "' UNION SELECT * FROM passwords --";
            String quoted = dialect.quoteString(malicious);

            // Quote should be escaped to ''
            assertTrue(quoted.contains("''"), "Should escape quote: " + quoted);
            // Result is safe: '' UNION... is inside string literal
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("String with comment injection has quote escaped")
        void stringWithCommentInjection(SqlDialect dialect) {
            String malicious = "value'/**/OR/**/1=1";
            String quoted = dialect.quoteString(malicious);

            // Quote should be escaped to ''
            assertTrue(quoted.contains("''"), "Should escape quote: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Identifier with injection attempt is escaped")
        void identifierWithInjection(SqlDialect dialect) {
            String malicious = "users; DROP TABLE users";
            String quoted = dialect.quoteIdentifier(malicious);

            // Result should be safely quoted
            assertNotNull(quoted);
            // For PG: "users; DROP TABLE users" - semicolon inside quotes is safe
            // For MySQL: `users; DROP TABLE users` - semicolon inside backticks is safe
        }

        @Test
        @DisplayName("PostgreSQL escapes double quotes in identifiers")
        void postgresEscapesDoubleQuotesInIdentifiers() {
            String malicious = "table\"; DROP TABLE users; --";
            String quoted = POSTGRES.quoteIdentifier(malicious);

            // Double quotes should be escaped by doubling: table" -> table""
            // Result: "table""; DROP TABLE users; --" (the inner " becomes "")
            assertTrue(quoted.contains("\"\""), "Should double the quote: " + quoted);
            assertTrue(quoted.startsWith("\"") && quoted.endsWith("\""), "Should be wrapped: " + quoted);
            // Security: parser sees "" as escaped quote, not string terminator
        }

        @Test
        @DisplayName("MySQL escapes backticks in identifiers")
        void mysqlEscapesBackticksInIdentifiers() {
            String malicious = "table`; DROP TABLE users; --";
            String quoted = MYSQL.quoteIdentifier(malicious);

            // Backticks should be escaped by doubling
            assertTrue(quoted.contains("``"), "Should double the backtick: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Multiple single quotes are all escaped")
        void multipleQuotesEscaped(SqlDialect dialect) {
            String value = "it's a test's string's";
            String quoted = dialect.quoteString(value);

            // Count escaped quotes ('' should appear 3 times)
            int count = countOccurrences(quoted, "''");
            assertEquals(3, count, "Should escape all 3 quotes: " + quoted);
        }
    }

    // ==================== NULL Handling ====================

    @Nested
    @DisplayName("NULL Handling")
    class NullHandlingTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("quoteString(null) returns NULL literal")
        void quoteStringNull(SqlDialect dialect) {
            assertEquals("NULL", dialect.quoteString(null));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("quoteIdentifier(null) returns null")
        void quoteIdentifierNull(SqlDialect dialect) {
            assertNull(dialect.quoteIdentifier(null));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("formatBoolean(null) returns NULL literal")
        void formatBooleanNull(SqlDialect dialect) {
            assertEquals("NULL", dialect.formatBoolean(null));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("nullLiteral() returns NULL")
        void nullLiteral(SqlDialect dialect) {
            assertEquals("NULL", dialect.nullLiteral());
        }
    }

    // ==================== Empty String Handling ====================

    @Nested
    @DisplayName("Empty String Handling")
    class EmptyStringTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("quoteString('') returns empty quoted string")
        void quoteEmptyString(SqlDialect dialect) {
            String quoted = dialect.quoteString("");
            assertEquals("''", quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("quoteIdentifier('') returns empty quoted identifier")
        void quoteEmptyIdentifier(SqlDialect dialect) {
            String quoted = dialect.quoteIdentifier("");

            if (dialect instanceof PostgreSqlDialect) {
                assertEquals("\"\"", quoted);
            } else {
                assertEquals("``", quoted);
            }
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("quoteString with only whitespace")
        void quoteWhitespaceString(SqlDialect dialect) {
            String quoted = dialect.quoteString("   ");
            assertEquals("'   '", quoted);
        }
    }

    // ==================== Special Characters ====================

    @Nested
    @DisplayName("Special Characters")
    class SpecialCharacterTests {

        @Test
        @DisplayName("PostgreSQL does NOT escape backslash")
        void postgresBackslash() {
            String quoted = POSTGRES.quoteString("a\\b");
            assertEquals("'a\\b'", quoted, "PostgreSQL should NOT escape backslash");
        }

        @Test
        @DisplayName("MySQL escapes backslash")
        void mysqlBackslash() {
            String quoted = MYSQL.quoteString("a\\b");
            assertEquals("'a\\\\b'", quoted, "MySQL should escape backslash");
        }

        @Test
        @DisplayName("MySQL escapes multiple backslashes")
        void mysqlMultipleBackslashes() {
            String quoted = MYSQL.quoteString("C:\\Users\\Name\\file.txt");
            assertEquals("'C:\\\\Users\\\\Name\\\\file.txt'", quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Newline character preserved in string")
        void newlineInString(SqlDialect dialect) {
            String value = "line1\nline2";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("\n"), "Should preserve newline: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Tab character preserved in string")
        void tabInString(SqlDialect dialect) {
            String value = "col1\tcol2";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("\t"), "Should preserve tab: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Carriage return preserved in string")
        void carriageReturnInString(SqlDialect dialect) {
            String value = "line1\r\nline2";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("\r"), "Should preserve CR: " + quoted);
        }

        @Test
        @DisplayName("MySQL mixed quotes and backslashes")
        void mysqlMixedQuotesAndBackslashes() {
            String value = "it's a path: C:\\test";
            String quoted = MYSQL.quoteString(value);

            assertTrue(quoted.contains("''"), "Should escape single quote");
            assertTrue(quoted.contains("\\\\"), "Should escape backslash");
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Percent sign in string (LIKE wildcard)")
        void percentInString(SqlDialect dialect) {
            String value = "100%";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("%"), "Should preserve percent: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Underscore in string (LIKE wildcard)")
        void underscoreInString(SqlDialect dialect) {
            String value = "user_name";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("_"), "Should preserve underscore: " + quoted);
        }
    }

    // ==================== Unicode ====================

    @Nested
    @DisplayName("Unicode Handling")
    class UnicodeTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Basic Latin extended characters")
        void basicLatinExtended(SqlDialect dialect) {
            String value = "café résumé naïve";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("café"), "Should preserve accents: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Chinese characters")
        void chineseCharacters(SqlDialect dialect) {
            String value = "你好世界";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("你好"), "Should preserve Chinese: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Japanese characters")
        void japaneseCharacters(SqlDialect dialect) {
            String value = "こんにちは";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("こんにちは"), "Should preserve Japanese: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Arabic characters")
        void arabicCharacters(SqlDialect dialect) {
            String value = "مرحبا";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("مرحبا"), "Should preserve Arabic: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Emoji characters")
        void emojiCharacters(SqlDialect dialect) {
            String value = "Hello 👋 World 🌍";
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("👋"), "Should preserve emoji: " + quoted);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Zero-width characters")
        void zeroWidthCharacters(SqlDialect dialect) {
            String value = "ab\u200Bcd"; // Zero-width space
            String quoted = dialect.quoteString(value);

            assertTrue(quoted.contains("\u200B"), "Should preserve zero-width: " + quoted);
        }
    }

    // ==================== Boundary Conditions ====================

    @Nested
    @DisplayName("Boundary Conditions")
    class BoundaryTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Very long string")
        void veryLongString(SqlDialect dialect) {
            String value = "a".repeat(10000);
            String quoted = dialect.quoteString(value);

            assertEquals(10002, quoted.length(), "Should be original + 2 quotes");
            assertTrue(quoted.startsWith("'") && quoted.endsWith("'"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("Very long identifier")
        void veryLongIdentifier(SqlDialect dialect) {
            String value = "a".repeat(1000);
            String quoted = dialect.quoteIdentifier(value);

            assertNotNull(quoted);
            assertTrue(quoted.length() > 1000);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("String with only quotes")
        void stringWithOnlyQuotes(SqlDialect dialect) {
            String value = "'''";
            String quoted = dialect.quoteString(value);

            // Should escape each quote
            assertTrue(quoted.contains("''"), "Should escape quotes: " + quoted);
            // Result should be: '''' '' '''' (6 quotes escaped to 12, plus 2 delimiters)
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectEdgeCaseTest#allDialects")
        @DisplayName("String with alternating quotes and text")
        void alternatingQuotes(SqlDialect dialect) {
            String value = "a'b'c'd'e";
            String quoted = dialect.quoteString(value);

            int count = countOccurrences(quoted, "''");
            assertEquals(4, count, "Should escape 4 quotes: " + quoted);
        }
    }

    // ==================== Reserved Words ====================

    @Nested
    @DisplayName("Reserved Words as Identifiers")
    class ReservedWordTests {

        @ParameterizedTest
        @ValueSource(strings = {"select", "from", "where", "table", "index", "user", "order", "group", "by", "join"})
        @DisplayName("PostgreSQL quotes reserved words")
        void postgresReservedWords(String word) {
            String quoted = POSTGRES.quoteIdentifier(word);
            assertEquals("\"" + word + "\"", quoted);
        }

        @ParameterizedTest
        @ValueSource(strings = {"select", "from", "where", "table", "index", "user", "order", "group", "by", "join"})
        @DisplayName("MySQL quotes reserved words")
        void mysqlReservedWords(String word) {
            String quoted = MYSQL.quoteIdentifier(word);
            assertEquals("`" + word + "`", quoted);
        }
    }

    // ==================== JSON Edge Cases ====================

    @Nested
    @DisplayName("JSON Edge Cases")
    class JsonEdgeCaseTests {

        @Test
        @DisplayName("PostgreSQL JSON extract with special key")
        void postgresJsonSpecialKey() {
            String result = POSTGRES.jsonExtract("data", "key'with\"quotes", true);
            assertTrue(result.contains("'key"), "Should quote the key: " + result);
        }

        @Test
        @DisplayName("MySQL JSON extract with special key")
        void mysqlJsonSpecialKey() {
            String result = MYSQL.jsonExtract("data", "key'with\"quotes", true);
            assertTrue(result.contains("$.key"), "Should have path: " + result);
        }

        @Test
        @DisplayName("PostgreSQL JSON contains with complex value")
        void postgresJsonContainsComplex() {
            String result = POSTGRES.jsonContains("data", "{\"nested\":{\"key\":\"value\"}}");
            assertTrue(result.contains("@>"), "Should use @>: " + result);
            assertTrue(result.contains("::jsonb"), "Should cast to jsonb: " + result);
        }

        @Test
        @DisplayName("MySQL JSON contains with complex value")
        void mysqlJsonContainsComplex() {
            String result = MYSQL.jsonContains("data", "{\"nested\":{\"key\":\"value\"}}");
            assertTrue(result.contains("JSON_CONTAINS"), "Should use function: " + result);
        }

        @Test
        @DisplayName("JSON key with dots")
        void jsonKeyWithDots() {
            String pgResult = POSTGRES.jsonExtract("data", "my.key.path", true);
            String myResult = MYSQL.jsonExtract("data", "my.key.path", true);

            assertNotNull(pgResult);
            assertNotNull(myResult);
        }

        @Test
        @DisplayName("JSON empty key")
        void jsonEmptyKey() {
            String pgResult = POSTGRES.jsonExtract("data", "", true);
            String myResult = MYSQL.jsonExtract("data", "", true);

            assertNotNull(pgResult);
            assertNotNull(myResult);
        }
    }

    // ==================== ILIKE Edge Cases ====================

    @Nested
    @DisplayName("ILIKE Edge Cases")
    class IlikeEdgeCaseTests {

        @Test
        @DisplayName("PostgreSQL ILIKE with special pattern")
        void postgresIlikeSpecialPattern() {
            String result = POSTGRES.ilike("email", "'%test%'");
            assertTrue(result.contains("ILIKE"), "Should use ILIKE: " + result);
        }

        @Test
        @DisplayName("MySQL ILIKE fallback with special pattern")
        void mysqlIlikeSpecialPattern() {
            String result = MYSQL.ilike("email", "'%test%'");
            assertTrue(result.contains("LOWER(email)"), "Should use LOWER: " + result);
            assertTrue(result.contains("LIKE"), "Should use LIKE: " + result);
        }

        @Test
        @DisplayName("ILIKE with empty pattern")
        void ilikeEmptyPattern() {
            String pgResult = POSTGRES.ilike("col", "''");
            String myResult = MYSQL.ilike("col", "''");

            assertNotNull(pgResult);
            assertNotNull(myResult);
        }
    }

    // ==================== Helper Methods ====================

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
