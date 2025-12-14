package sant1ago.dev.suprim.core.dialect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for all SQL dialect implementations.
 * Tests PostgreSQL, MySQL 5.7, MySQL 8.0, and MariaDB.
 */
@DisplayName("Dialect Comprehensive Tests")
class DialectComprehensiveTest {

    private static final PostgreSqlDialect POSTGRES = PostgreSqlDialect.INSTANCE;
    private static final MySqlDialect MYSQL = MySqlDialect.INSTANCE;
    private static final MySql8Dialect MYSQL8 = MySql8Dialect.INSTANCE;
    private static final MariaDbDialect MARIADB = MariaDbDialect.INSTANCE;

    static Stream<SqlDialect> allDialects() {
        return Stream.of(POSTGRES, MYSQL, MYSQL8, MARIADB);
    }

    static Stream<SqlDialect> mysqlFamily() {
        return Stream.of(MYSQL, MYSQL8, MARIADB);
    }

    // ==================== Identifier Quoting Tests ====================

    @Nested
    @DisplayName("Identifier Quoting")
    class IdentifierQuotingTests {

        @Test
        @DisplayName("PostgreSQL quotes with double quotes")
        void postgresQuotesWithDoubleQuotes() {
            assertEquals("\"users\"", POSTGRES.quoteIdentifier("users"));
            assertEquals("\"user_table\"", POSTGRES.quoteIdentifier("user_table"));
            assertEquals("\"MyTable\"", POSTGRES.quoteIdentifier("MyTable"));
        }

        @Test
        @DisplayName("PostgreSQL escapes embedded double quotes")
        void postgresEscapesEmbeddedDoubleQuotes() {
            assertEquals("\"user\"\"name\"", POSTGRES.quoteIdentifier("user\"name"));
            assertEquals("\"a\"\"b\"\"c\"", POSTGRES.quoteIdentifier("a\"b\"c"));
        }

        @Test
        @DisplayName("MySQL quotes with backticks")
        void mysqlQuotesWithBackticks() {
            assertEquals("`users`", MYSQL.quoteIdentifier("users"));
            assertEquals("`user_table`", MYSQL.quoteIdentifier("user_table"));
            assertEquals("`MyTable`", MYSQL.quoteIdentifier("MyTable"));
        }

        @Test
        @DisplayName("MySQL escapes embedded backticks")
        void mysqlEscapesEmbeddedBackticks() {
            assertEquals("`user``name`", MYSQL.quoteIdentifier("user`name"));
            assertEquals("`a``b``c`", MYSQL.quoteIdentifier("a`b`c"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("All dialects return null for null identifier")
        void allDialectsReturnNullForNullIdentifier(SqlDialect dialect) {
            assertNull(dialect.quoteIdentifier(null));
        }

        @Test
        @DisplayName("MySQL family uses backticks consistently")
        void mysqlFamilyUsesBackticks() {
            String identifier = "test_table";
            assertEquals("`test_table`", MYSQL.quoteIdentifier(identifier));
            assertEquals("`test_table`", MYSQL8.quoteIdentifier(identifier));
            assertEquals("`test_table`", MARIADB.quoteIdentifier(identifier));
        }

        @Test
        @DisplayName("Reserved words are quoted properly")
        void reservedWordsAreQuoted() {
            assertEquals("\"select\"", POSTGRES.quoteIdentifier("select"));
            assertEquals("`select`", MYSQL.quoteIdentifier("select"));
            assertEquals("\"from\"", POSTGRES.quoteIdentifier("from"));
            assertEquals("`from`", MYSQL.quoteIdentifier("from"));
        }
    }

    // ==================== String Escaping Tests ====================

    @Nested
    @DisplayName("String Escaping")
    class StringEscapingTests {

        @Test
        @DisplayName("PostgreSQL escapes single quotes by doubling")
        void postgresEscapesSingleQuotes() {
            assertEquals("'hello'", POSTGRES.quoteString("hello"));
            assertEquals("'it''s'", POSTGRES.quoteString("it's"));
            assertEquals("'O''Brien''s'", POSTGRES.quoteString("O'Brien's"));
        }

        @Test
        @DisplayName("MySQL escapes single quotes by doubling")
        void mysqlEscapesSingleQuotes() {
            assertEquals("'hello'", MYSQL.quoteString("hello"));
            assertEquals("'it''s'", MYSQL.quoteString("it's"));
            assertEquals("'O''Brien''s'", MYSQL.quoteString("O'Brien's"));
        }

        @Test
        @DisplayName("MySQL escapes backslashes")
        void mysqlEscapesBackslashes() {
            assertEquals("'path\\\\to\\\\file'", MYSQL.quoteString("path\\to\\file"));
            assertEquals("'a\\\\b'", MYSQL.quoteString("a\\b"));
            assertEquals("'C:\\\\Users\\\\Name'", MYSQL.quoteString("C:\\Users\\Name"));
        }

        @Test
        @DisplayName("PostgreSQL does not escape backslashes")
        void postgresDoesNotEscapeBackslashes() {
            assertEquals("'path\\to\\file'", POSTGRES.quoteString("path\\to\\file"));
            assertEquals("'a\\b'", POSTGRES.quoteString("a\\b"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("All dialects return NULL for null string")
        void allDialectsReturnNullForNullString(SqlDialect dialect) {
            assertEquals("NULL", dialect.quoteString(null));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("All dialects handle empty string")
        void allDialectsHandleEmptyString(SqlDialect dialect) {
            assertEquals("''", dialect.quoteString(""));
        }

        @Test
        @DisplayName("MySQL family escapes backslashes consistently")
        void mysqlFamilyEscapesBackslashes() {
            String value = "a\\b";
            assertEquals("'a\\\\b'", MYSQL.quoteString(value));
            assertEquals("'a\\\\b'", MYSQL8.quoteString(value));
            assertEquals("'a\\\\b'", MARIADB.quoteString(value));
        }

        @Test
        @DisplayName("Unicode strings are preserved")
        void unicodeStringsArePreserved() {
            assertEquals("'café'", POSTGRES.quoteString("café"));
            assertEquals("'café'", MYSQL.quoteString("café"));
            assertEquals("'你好'", POSTGRES.quoteString("你好"));
            assertEquals("'你好'", MYSQL.quoteString("你好"));
        }

        @Test
        @DisplayName("Whitespace is preserved")
        void whitespaceIsPreserved() {
            assertEquals("'   '", POSTGRES.quoteString("   "));
            assertEquals("'line1\\nline2'", POSTGRES.quoteString("line1\\nline2"));
        }

        @Test
        @DisplayName("Mixed quotes and backslashes in MySQL")
        void mixedQuotesAndBackslashesInMySql() {
            assertEquals("'it''s a path: C:\\\\test'", MYSQL.quoteString("it's a path: C:\\test"));
        }
    }

    // ==================== Boolean Formatting Tests ====================

    @Nested
    @DisplayName("Boolean Formatting")
    class BooleanFormattingTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("All dialects format TRUE correctly")
        void allDialectsFormatTrue(SqlDialect dialect) {
            assertEquals("TRUE", dialect.formatBoolean(true));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("All dialects format FALSE correctly")
        void allDialectsFormatFalse(SqlDialect dialect) {
            assertEquals("FALSE", dialect.formatBoolean(false));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("All dialects format null as NULL")
        void allDialectsFormatNullAsNull(SqlDialect dialect) {
            assertEquals("NULL", dialect.formatBoolean(null));
        }
    }

    // ==================== JSON Operations Tests ====================

    @Nested
    @DisplayName("JSON Operations")
    class JsonOperationsTests {

        @Test
        @DisplayName("PostgreSQL jsonExtract uses arrow operators")
        void postgresJsonExtractUsesArrowOperators() {
            assertEquals("data->>'name'", POSTGRES.jsonExtract("data", "name", true));
            assertEquals("data->'info'", POSTGRES.jsonExtract("data", "info", false));
        }

        @Test
        @DisplayName("MySQL jsonExtract uses dollar path syntax")
        void mysqlJsonExtractUsesDollarPath() {
            assertEquals("data->>'$.name'", MYSQL.jsonExtract("data", "name", true));
            assertEquals("data->'$.info'", MYSQL.jsonExtract("data", "info", false));
        }

        @Test
        @DisplayName("MySQL family uses same jsonExtract syntax")
        void mysqlFamilyUsesSameJsonExtractSyntax() {
            assertEquals("data->>'$.key'", MYSQL.jsonExtract("data", "key", true));
            assertEquals("data->>'$.key'", MYSQL8.jsonExtract("data", "key", true));
            assertEquals("data->>'$.key'", MARIADB.jsonExtract("data", "key", true));
        }

        @Test
        @DisplayName("PostgreSQL jsonContains uses @> operator")
        void postgresJsonContainsUsesContainsOperator() {
            assertEquals("data @> CAST('{\"active\":true}' AS jsonb)",
                    POSTGRES.jsonContains("data", "{\"active\":true}"));
        }

        @Test
        @DisplayName("MySQL jsonContains uses JSON_CONTAINS function")
        void mysqlJsonContainsUsesFunction() {
            assertEquals("JSON_CONTAINS(data, '{\"active\":true}')",
                    MYSQL.jsonContains("data", "{\"active\":true}"));
        }

        @Test
        @DisplayName("PostgreSQL jsonKeyExists uses ? operator")
        void postgresJsonKeyExistsUsesQuestionMark() {
            assertEquals("data ? 'email'", POSTGRES.jsonKeyExists("data", "email"));
        }

        @Test
        @DisplayName("MySQL jsonKeyExists uses JSON_CONTAINS_PATH")
        void mysqlJsonKeyExistsUsesContainsPath() {
            assertEquals("JSON_CONTAINS_PATH(data, 'one', '$.email')",
                    MYSQL.jsonKeyExists("data", "email"));
        }

        @Test
        @DisplayName("MySQL family uses same jsonContains syntax")
        void mysqlFamilyUsesSameJsonContainsSyntax() {
            String expected = "JSON_CONTAINS(col, '{}')" ;
            assertEquals(expected, MYSQL.jsonContains("col", "{}"));
            assertEquals(expected, MYSQL8.jsonContains("col", "{}"));
            assertEquals(expected, MARIADB.jsonContains("col", "{}"));
        }
    }

    // ==================== ILIKE Tests ====================

    @Nested
    @DisplayName("ILIKE Handling")
    class IlikeTests {

        @Test
        @DisplayName("PostgreSQL uses native ILIKE")
        void postgresUsesNativeIlike() {
            assertEquals("email ILIKE :pattern", POSTGRES.ilike("email", ":pattern"));
            assertEquals("email NOT ILIKE :pattern", POSTGRES.notIlike("email", ":pattern"));
        }

        @Test
        @DisplayName("MySQL falls back to LOWER() LIKE LOWER()")
        void mysqlFallsBackToLower() {
            assertEquals("LOWER(email) LIKE LOWER(:pattern)", MYSQL.ilike("email", ":pattern"));
            assertEquals("LOWER(email) NOT LIKE LOWER(:pattern)", MYSQL.notIlike("email", ":pattern"));
        }

        @Test
        @DisplayName("MySQL 8 falls back to LOWER() LIKE LOWER()")
        void mysql8FallsBackToLower() {
            assertEquals("LOWER(email) LIKE LOWER(:pattern)", MYSQL8.ilike("email", ":pattern"));
        }

        @Test
        @DisplayName("MariaDB falls back to LOWER() LIKE LOWER()")
        void mariaDbFallsBackToLower() {
            assertEquals("LOWER(email) LIKE LOWER(:pattern)", MARIADB.ilike("email", ":pattern"));
        }
    }

    // ==================== Locking Tests ====================

    @Nested
    @DisplayName("Locking Clauses")
    class LockingTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("All dialects support basic FOR UPDATE")
        void allDialectsSupportBasicForUpdate(SqlDialect dialect) {
            assertEquals("FOR UPDATE", dialect.forUpdate(false, false));
        }

        @Test
        @DisplayName("PostgreSQL supports FOR UPDATE NOWAIT")
        void postgresSupportsNowait() {
            assertEquals("FOR UPDATE NOWAIT", POSTGRES.forUpdate(true, false));
        }

        @Test
        @DisplayName("PostgreSQL supports FOR UPDATE SKIP LOCKED")
        void postgresSupportsSkipLocked() {
            assertEquals("FOR UPDATE SKIP LOCKED", POSTGRES.forUpdate(false, true));
        }

        @Test
        @DisplayName("MySQL 5.7 throws for NOWAIT")
        void mysql57ThrowsForNowait() {
            UnsupportedDialectFeatureException ex = assertThrows(
                    UnsupportedDialectFeatureException.class,
                    () -> MYSQL.forUpdate(true, false)
            );
            assertEquals("NOWAIT", ex.getFeature());
            assertEquals("MySQL", ex.getDialectName());
        }

        @Test
        @DisplayName("MySQL 5.7 throws for SKIP LOCKED")
        void mysql57ThrowsForSkipLocked() {
            UnsupportedDialectFeatureException ex = assertThrows(
                    UnsupportedDialectFeatureException.class,
                    () -> MYSQL.forUpdate(false, true)
            );
            assertEquals("SKIP LOCKED", ex.getFeature());
        }

        @Test
        @DisplayName("MySQL 8 supports NOWAIT")
        void mysql8SupportsNowait() {
            assertEquals("FOR UPDATE NOWAIT", MYSQL8.forUpdate(true, false));
        }

        @Test
        @DisplayName("MySQL 8 supports SKIP LOCKED")
        void mysql8SupportsSkipLocked() {
            assertEquals("FOR UPDATE SKIP LOCKED", MYSQL8.forUpdate(false, true));
        }

        @Test
        @DisplayName("MariaDB supports NOWAIT")
        void mariaDbSupportsNowait() {
            assertEquals("FOR UPDATE NOWAIT", MARIADB.forUpdate(true, false));
        }

        @Test
        @DisplayName("MariaDB supports SKIP LOCKED")
        void mariaDbSupportsSkipLocked() {
            assertEquals("FOR UPDATE SKIP LOCKED", MARIADB.forUpdate(false, true));
        }

        @Test
        @DisplayName("FOR SHARE works similarly")
        void forShareWorksSimilarly() {
            assertEquals("FOR SHARE", POSTGRES.forShare(false, false));
            assertEquals("FOR SHARE NOWAIT", POSTGRES.forShare(true, false));
            assertEquals("FOR SHARE SKIP LOCKED", POSTGRES.forShare(false, true));
        }
    }

    // ==================== RETURNING Tests ====================

    @Nested
    @DisplayName("RETURNING Clause")
    class ReturningTests {

        @Test
        @DisplayName("PostgreSQL supports RETURNING")
        void postgresSupportsReturning() {
            assertEquals("RETURNING id", POSTGRES.returning("id"));
            assertEquals("RETURNING id, name", POSTGRES.returning("id, name"));
        }

        @Test
        @DisplayName("MySQL 5.7 throws for RETURNING")
        void mysql57ThrowsForReturning() {
            UnsupportedDialectFeatureException ex = assertThrows(
                    UnsupportedDialectFeatureException.class,
                    () -> MYSQL.returning("id")
            );
            assertEquals("RETURNING", ex.getFeature());
            assertTrue(ex.getMessage().contains("LAST_INSERT_ID"));
        }

        @Test
        @DisplayName("MySQL 8 throws for RETURNING")
        void mysql8ThrowsForReturning() {
            assertThrows(UnsupportedDialectFeatureException.class, () -> MYSQL8.returning("id"));
        }

        @Test
        @DisplayName("MariaDB supports RETURNING")
        void mariaDbSupportsReturning() {
            assertEquals("RETURNING id", MARIADB.returning("id"));
            assertEquals("RETURNING id, name, email", MARIADB.returning("id, name, email"));
        }
    }

    // ==================== Array Tests ====================

    @Nested
    @DisplayName("Array Operations")
    class ArrayTests {

        @Test
        @DisplayName("PostgreSQL supports array contains")
        void postgresSupportsArrayContains() {
            assertEquals("tags @> ARRAY['java']", POSTGRES.arrayContains("tags", "'java'"));
        }

        @Test
        @DisplayName("PostgreSQL supports array overlap")
        void postgresSupportsArrayOverlap() {
            assertEquals("tags && ARRAY['a', 'b']", POSTGRES.arrayOverlap("tags", "ARRAY['a', 'b']"));
        }

        @Test
        @DisplayName("MySQL throws for array contains")
        void mysqlThrowsForArrayContains() {
            UnsupportedDialectFeatureException ex = assertThrows(
                    UnsupportedDialectFeatureException.class,
                    () -> MYSQL.arrayContains("tags", "'java'")
            );
            assertEquals("ARRAY", ex.getFeature());
            assertTrue(ex.getMessage().contains("JSON_CONTAINS"));
        }

        @Test
        @DisplayName("MySQL 8 throws for array contains")
        void mysql8ThrowsForArrayContains() {
            assertThrows(UnsupportedDialectFeatureException.class, () -> MYSQL8.arrayContains("tags", "'java'"));
        }

        @Test
        @DisplayName("MariaDB throws for array contains")
        void mariaDbThrowsForArrayContains() {
            assertThrows(UnsupportedDialectFeatureException.class, () -> MARIADB.arrayContains("tags", "'java'"));
        }

        @Test
        @DisplayName("MySQL throws for array overlap")
        void mysqlThrowsForArrayOverlap() {
            assertThrows(UnsupportedDialectFeatureException.class, () -> MYSQL.arrayOverlap("tags", "values"));
        }
    }

    // ==================== Aggregate FILTER Tests ====================

    @Nested
    @DisplayName("Aggregate FILTER Clause")
    class AggregateFilterTests {

        @Test
        @DisplayName("PostgreSQL supports FILTER clause")
        void postgresSupportsFilterClause() {
            assertEquals("COUNT(*) FILTER (WHERE active = true)",
                    POSTGRES.aggregateFilter("COUNT(*)", "active = true"));
            assertEquals("SUM(amount) FILTER (WHERE status = 'paid')",
                    POSTGRES.aggregateFilter("SUM(amount)", "status = 'paid'"));
        }

        @Test
        @DisplayName("MySQL throws for FILTER clause")
        void mysqlThrowsForFilterClause() {
            UnsupportedDialectFeatureException ex = assertThrows(
                    UnsupportedDialectFeatureException.class,
                    () -> MYSQL.aggregateFilter("COUNT(*)", "active = true")
            );
            assertEquals("FILTER clause", ex.getFeature());
            assertTrue(ex.getMessage().contains("CASE"));
        }

        @Test
        @DisplayName("MySQL 8 throws for FILTER clause")
        void mysql8ThrowsForFilterClause() {
            assertThrows(UnsupportedDialectFeatureException.class,
                    () -> MYSQL8.aggregateFilter("COUNT(*)", "active = true"));
        }

        @Test
        @DisplayName("MariaDB throws for FILTER clause")
        void mariaDbThrowsForFilterClause() {
            assertThrows(UnsupportedDialectFeatureException.class,
                    () -> MARIADB.aggregateFilter("COUNT(*)", "active = true"));
        }
    }

    // ==================== DISTINCT ON Tests ====================

    @Nested
    @DisplayName("DISTINCT ON")
    class DistinctOnTests {

        @Test
        @DisplayName("PostgreSQL supports DISTINCT ON")
        void postgresSupportsDistinctOn() {
            assertEquals("DISTINCT ON (user_id)", POSTGRES.distinctOn("user_id"));
            assertEquals("DISTINCT ON (category, date)", POSTGRES.distinctOn("category, date"));
        }

        @Test
        @DisplayName("MySQL throws for DISTINCT ON")
        void mysqlThrowsForDistinctOn() {
            UnsupportedDialectFeatureException ex = assertThrows(
                    UnsupportedDialectFeatureException.class,
                    () -> MYSQL.distinctOn("user_id")
            );
            assertEquals("DISTINCT ON", ex.getFeature());
            assertTrue(ex.getMessage().contains("GROUP BY") || ex.getMessage().contains("window"));
        }

        @Test
        @DisplayName("MySQL 8 throws for DISTINCT ON")
        void mysql8ThrowsForDistinctOn() {
            assertThrows(UnsupportedDialectFeatureException.class, () -> MYSQL8.distinctOn("user_id"));
        }

        @Test
        @DisplayName("MariaDB throws for DISTINCT ON")
        void mariaDbThrowsForDistinctOn() {
            assertThrows(UnsupportedDialectFeatureException.class, () -> MARIADB.distinctOn("user_id"));
        }
    }

    // ==================== Capability Flag Tests ====================

    @Nested
    @DisplayName("Capability Flags")
    class CapabilityFlagTests {

        @Test
        @DisplayName("PostgreSQL has full capabilities")
        void postgresHasFullCapabilities() {
            DialectCapabilities caps = POSTGRES.capabilities();
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
        @DisplayName("MySQL 5.7 has limited capabilities")
        void mysql57HasLimitedCapabilities() {
            DialectCapabilities caps = MYSQL.capabilities();
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
        @DisplayName("MySQL 8 adds NOWAIT and SKIP LOCKED")
        void mysql8AddsLockingFeatures() {
            DialectCapabilities caps = MYSQL8.capabilities();
            assertFalse(caps.supportsReturning());
            assertFalse(caps.supportsIlike());
            assertFalse(caps.supportsArrays());
            assertTrue(caps.supportsSkipLocked());
            assertTrue(caps.supportsNowait());
            assertFalse(caps.supportsFilterClause());
        }

        @Test
        @DisplayName("MariaDB adds RETURNING and locking features")
        void mariaDbAddsReturningAndLocking() {
            DialectCapabilities caps = MARIADB.capabilities();
            assertTrue(caps.supportsReturning());
            assertFalse(caps.supportsIlike());
            assertFalse(caps.supportsArrays());
            assertTrue(caps.supportsSkipLocked());
            assertTrue(caps.supportsNowait());
            assertFalse(caps.supportsFilterClause());
        }
    }

    // ==================== Dialect Names Tests ====================

    @Nested
    @DisplayName("Dialect Names")
    class DialectNamesTests {

        @Test
        @DisplayName("Each dialect has correct name")
        void eachDialectHasCorrectName() {
            assertEquals("PostgreSQL", POSTGRES.getName());
            assertEquals("MySQL", MYSQL.getName());
            assertEquals("MySQL 8", MYSQL8.getName());
            assertEquals("MariaDB", MARIADB.getName());
        }
    }

    // ==================== Commercial Flag Tests ====================

    @Nested
    @DisplayName("Commercial Flag")
    class CommercialFlagTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("Open source dialects are not commercial")
        void openSourceDialectsAreNotCommercial(SqlDialect dialect) {
            assertFalse(dialect.isCommercial());
        }
    }

    // ==================== Null Literal Tests ====================

    @Nested
    @DisplayName("Null Literal")
    class NullLiteralTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("All dialects return NULL literal")
        void allDialectsReturnNullLiteral(SqlDialect dialect) {
            assertEquals("NULL", dialect.nullLiteral());
        }
    }

    // ==================== Parameter Placeholder Tests ====================

    @Nested
    @DisplayName("Parameter Placeholder")
    class ParameterPlaceholderTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectComprehensiveTest#allDialects")
        @DisplayName("All dialects use named parameters")
        void allDialectsUseNamedParameters(SqlDialect dialect) {
            assertEquals(":userId", dialect.parameterPlaceholder("userId"));
            assertEquals(":email", dialect.parameterPlaceholder("email"));
        }
    }
}
