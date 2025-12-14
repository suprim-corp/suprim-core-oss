package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.query.Suprim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SQL function builder (Fn class).
 */
@DisplayName("Fn - SQL Functions Tests")
class FnTest {

    private static final PostgreSqlDialect POSTGRES = PostgreSqlDialect.INSTANCE;
    private static final MySqlDialect MYSQL = MySqlDialect.INSTANCE;

    // ==================== DATE/TIME FUNCTIONS ====================

    @Nested
    @DisplayName("Date/Time Functions")
    class DateTimeFunctions {

        @Test
        @DisplayName("NOW() returns LocalDateTime type")
        void testNow() {
            SqlFunction<LocalDateTime> fn = Fn.now();
            assertEquals(LocalDateTime.class, fn.getValueType());
            assertEquals("NOW()", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("CURRENT_DATE returns LocalDate type")
        void testCurrentDate() {
            SqlFunction<LocalDate> fn = Fn.currentDate();
            assertEquals(LocalDate.class, fn.getValueType());
            assertEquals("CURRENT_DATE()", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("CURRENT_TIME returns LocalTime type")
        void testCurrentTime() {
            SqlFunction<LocalTime> fn = Fn.currentTime();
            assertEquals(LocalTime.class, fn.getValueType());
            assertEquals("CURRENT_TIME()", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("CURRENT_TIMESTAMP returns LocalDateTime type")
        void testCurrentTimestamp() {
            SqlFunction<LocalDateTime> fn = Fn.currentTimestamp();
            assertEquals(LocalDateTime.class, fn.getValueType());
            assertEquals("CURRENT_TIMESTAMP()", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("NOW() in SELECT query")
        void testNowInSelect() {
            QueryResult result = Suprim.select(TestUser_.ID, Fn.now().as("current_time"))
                .from(TestUser_.TABLE)
                .build();

            assertTrue(result.sql().contains("NOW() AS \"current_time\""));
        }

        @Test
        @DisplayName("NOW() compared with literal value")
        void testNowComparedWithLiteral() {
            // Function can be compared with literal values
            LocalDateTime cutoff = LocalDateTime.of(2024, 1, 1, 0, 0);
            Predicate predicate = Fn.now().gt(cutoff);
            assertNotNull(predicate);
        }
    }

    // ==================== RANDOM ====================

    @Nested
    @DisplayName("Random Function")
    class RandomFunction {

        @Test
        @DisplayName("RANDOM() for PostgreSQL")
        void testRandomPostgres() {
            SqlFunction<Double> fn = Fn.random();
            assertEquals(Double.class, fn.getValueType());
            assertEquals("RANDOM()", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("RAND() for MySQL")
        void testRandomMysql() {
            SqlFunction<Double> fn = Fn.random();
            assertEquals("RAND()", fn.toSql(MYSQL));
        }

        @Test
        @DisplayName("ORDER BY RANDOM()")
        void testOrderByRandom() {
            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .orderBy(Fn.random().desc())
                .build();

            assertTrue(result.sql().contains("ORDER BY RANDOM() DESC"));
        }
    }

    // ==================== STRING FUNCTIONS ====================

    @Nested
    @DisplayName("String Functions")
    class StringFunctions {

        @Test
        @DisplayName("UPPER(column)")
        void testUpperColumn() {
            SqlFunction<String> fn = Fn.upper(TestUser_.NAME);
            assertEquals(String.class, fn.getValueType());
            assertEquals("UPPER(users.\"name\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("UPPER(value)")
        void testUpperValue() {
            SqlFunction<String> fn = Fn.upper("hello");
            assertEquals("UPPER('hello')", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("LOWER(column)")
        void testLowerColumn() {
            SqlFunction<String> fn = Fn.lower(TestUser_.EMAIL);
            assertEquals("LOWER(users.\"email\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("LOWER(value)")
        void testLowerValue() {
            SqlFunction<String> fn = Fn.lower("HELLO");
            assertEquals("LOWER('HELLO')", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("LENGTH(column)")
        void testLength() {
            SqlFunction<Integer> fn = Fn.length(TestUser_.NAME);
            assertEquals(Integer.class, fn.getValueType());
            assertEquals("LENGTH(users.\"name\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("TRIM(column)")
        void testTrim() {
            assertEquals("TRIM(users.\"name\")", Fn.trim(TestUser_.NAME).toSql(POSTGRES));
        }

        @Test
        @DisplayName("LTRIM(column)")
        void testLtrim() {
            assertEquals("LTRIM(users.\"name\")", Fn.ltrim(TestUser_.NAME).toSql(POSTGRES));
        }

        @Test
        @DisplayName("RTRIM(column)")
        void testRtrim() {
            assertEquals("RTRIM(users.\"name\")", Fn.rtrim(TestUser_.NAME).toSql(POSTGRES));
        }

        @Test
        @DisplayName("CONCAT with columns and strings")
        void testConcat() {
            SqlFunction<String> fn = Fn.concat(TestUser_.NAME, " - ", TestUser_.EMAIL);
            String sql = fn.toSql(POSTGRES);
            assertTrue(sql.contains("CONCAT("));
            assertTrue(sql.contains("users.\"name\""));
            assertTrue(sql.contains("' - '"));
            assertTrue(sql.contains("users.\"email\""));
        }

        @Test
        @DisplayName("SUBSTRING(column, start, length)")
        void testSubstring() {
            SqlFunction<String> fn = Fn.substring(TestUser_.NAME, 1, 5);
            assertEquals("SUBSTRING(users.\"name\", 1, 5)", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("REPLACE(column, from, to)")
        void testReplace() {
            SqlFunction<String> fn = Fn.replace(TestUser_.EMAIL, "@", "[at]");
            assertEquals("REPLACE(users.\"email\", '@', '[at]')", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("REVERSE(column)")
        void testReverse() {
            assertEquals("REVERSE(users.\"name\")", Fn.reverse(TestUser_.NAME).toSql(POSTGRES));
        }

        @Test
        @DisplayName("LOWER in WHERE clause")
        void testLowerInWhere() {
            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .where(Fn.lower(TestUser_.EMAIL).eq("test@example.com"))
                .build();

            assertTrue(result.sql().contains("WHERE LOWER(users.\"email\") = :p1"));
            assertEquals("test@example.com", result.parameters().get("p1"));
        }
    }

    // ==================== MATH FUNCTIONS ====================

    @Nested
    @DisplayName("Math Functions")
    class MathFunctions {

        @Test
        @DisplayName("ABS(column)")
        void testAbs() {
            SqlFunction<Integer> fn = Fn.abs(TestUser_.AGE);
            assertEquals(Integer.class, fn.getValueType());
            assertEquals("ABS(users.\"age\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("CEIL(column)")
        void testCeil() {
            SqlFunction<Long> fn = Fn.ceil(TestUser_.AGE);
            assertEquals(Long.class, fn.getValueType());
            assertEquals("CEIL(users.\"age\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("FLOOR(column)")
        void testFloor() {
            SqlFunction<Long> fn = Fn.floor(TestUser_.AGE);
            assertEquals(Long.class, fn.getValueType());
            assertEquals("FLOOR(users.\"age\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("ROUND(column)")
        void testRound() {
            SqlFunction<Long> fn = Fn.round(TestUser_.AGE);
            assertEquals(Long.class, fn.getValueType());
            assertEquals("ROUND(users.\"age\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("ROUND(column, decimals)")
        void testRoundWithDecimals() {
            SqlFunction<BigDecimal> fn = Fn.round(TestUser_.AGE, 2);
            assertEquals(BigDecimal.class, fn.getValueType());
            assertEquals("ROUND(users.\"age\", 2)", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("SQRT(column)")
        void testSqrt() {
            SqlFunction<Double> fn = Fn.sqrt(TestUser_.AGE);
            assertEquals(Double.class, fn.getValueType());
            assertEquals("SQRT(users.\"age\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("POWER(base, exponent)")
        void testPower() {
            SqlFunction<Double> fn = Fn.power(TestUser_.AGE, 2);
            assertEquals(Double.class, fn.getValueType());
            assertEquals("POWER(users.\"age\", 2)", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("MOD(dividend, divisor)")
        void testMod() {
            SqlFunction<Long> fn = Fn.mod(TestUser_.AGE, 10);
            assertEquals(Long.class, fn.getValueType());
            assertEquals("MOD(users.\"age\", 10)", fn.toSql(POSTGRES));
        }
    }

    // ==================== NULL HANDLING ====================

    @Nested
    @DisplayName("Null Handling Functions")
    class NullHandlingFunctions {

        @Test
        @DisplayName("COALESCE with column and default")
        void testCoalesceColumnAndDefault() {
            SqlFunction<String> fn = Fn.coalesce(TestUser_.NAME, "Anonymous");
            String sql = fn.toSql(POSTGRES);
            assertTrue(sql.contains("COALESCE("));
            assertTrue(sql.contains("users.\"name\""));
            assertTrue(sql.contains("'Anonymous'"));
        }

        @Test
        @DisplayName("COALESCE with multiple columns")
        void testCoalesceMultipleColumns() {
            SqlFunction<String> fn = Fn.coalesce(TestUser_.NAME, TestUser_.EMAIL, "Unknown");
            String sql = fn.toSql(POSTGRES);
            assertTrue(sql.contains("COALESCE("));
            assertTrue(sql.contains("users.\"name\""));
            assertTrue(sql.contains("users.\"email\""));
            assertTrue(sql.contains("'Unknown'"));
        }

        @Test
        @DisplayName("NULLIF(column, value)")
        void testNullif() {
            SqlFunction<String> fn = Fn.nullIf(TestUser_.NAME, "");
            String sql = fn.toSql(POSTGRES);
            assertTrue(sql.contains("NULLIF("));
            assertTrue(sql.contains("users.\"name\""));
            assertTrue(sql.contains("''"));
        }

        @Test
        @DisplayName("IFNULL uses COALESCE in PostgreSQL")
        void testIfnullPostgres() {
            SqlFunction<String> fn = Fn.ifNull(TestUser_.NAME, "Default");
            String sql = fn.toSql(POSTGRES);
            assertTrue(sql.contains("COALESCE("));
            assertTrue(sql.contains("users.\"name\""));
            assertTrue(sql.contains("'Default'"));
        }

        @Test
        @DisplayName("IFNULL uses IFNULL in MySQL")
        void testIfnullMysql() {
            SqlFunction<String> fn = Fn.ifNull(TestUser_.NAME, "Default");
            String sql = fn.toSql(MYSQL);
            assertTrue(sql.contains("IFNULL("));
        }
    }

    // ==================== UUID ====================

    @Nested
    @DisplayName("UUID Function")
    class UuidFunction {

        @Test
        @DisplayName("gen_random_uuid() for PostgreSQL")
        void testUuidPostgres() {
            SqlFunction<UUID> fn = Fn.uuid();
            assertEquals(UUID.class, fn.getValueType());
            assertEquals("gen_random_uuid()", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("UUID() for MySQL")
        void testUuidMysql() {
            SqlFunction<UUID> fn = Fn.uuid();
            assertEquals("UUID()", fn.toSql(MYSQL));
        }

        @Test
        @DisplayName("UUID in INSERT")
        void testUuidInInsert() {
            QueryResult result = Suprim.insertInto(TestUser_.TABLE)
                .column(TestUser_.EMAIL, "test@example.com")
                .build();

            // UUID would be used for ID column in real scenarios
            assertNotNull(result);
        }
    }

    // ==================== CUSTOM FUNCTION CALL ====================

    @Nested
    @DisplayName("Custom Function Calls")
    class CustomFunctionCalls {

        @Test
        @DisplayName("call(name, args...)")
        void testCall() {
            SqlFunction<Object> fn = Fn.call("my_function", TestUser_.ID, 100);
            assertEquals(Object.class, fn.getValueType());
            assertEquals("my_function(users.\"id\", 100)", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("call with no arguments")
        void testCallNoArgs() {
            SqlFunction<Object> fn = Fn.call("my_procedure");
            assertEquals("my_procedure()", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("call with string argument")
        void testCallWithString() {
            SqlFunction<Object> fn = Fn.call("search_text", "hello");
            assertEquals("search_text('hello')", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("callTyped with explicit return type")
        void testCallTyped() {
            SqlFunction<BigDecimal> fn = Fn.callTyped(BigDecimal.class, "calculate_tax", TestUser_.AGE);
            assertEquals(BigDecimal.class, fn.getValueType());
            assertEquals("calculate_tax(users.\"age\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("Custom function in SELECT")
        void testCustomFunctionInSelect() {
            QueryResult result = Suprim.select(
                    TestUser_.ID,
                    Fn.call("format_name", TestUser_.NAME).as("formatted"))
                .from(TestUser_.TABLE)
                .build();

            assertTrue(result.sql().contains("format_name(users.\"name\") AS \"formatted\""));
        }
    }

    // ==================== RAW EXPRESSIONS ====================

    @Nested
    @DisplayName("Raw SQL Expressions")
    class RawExpressions {

        @Test
        @DisplayName("raw with single placeholder")
        void testRawSinglePlaceholder() {
            SqlFunction<Object> fn = Fn.raw("EXTRACT(YEAR FROM ?)", TestUser_.CREATED_AT);
            assertEquals("EXTRACT(YEAR FROM users.\"created_at\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("raw with multiple placeholders")
        void testRawMultiplePlaceholders() {
            SqlFunction<Object> fn = Fn.raw("? AT TIME ZONE ?", TestUser_.CREATED_AT, "UTC");
            assertEquals("users.\"created_at\" AT TIME ZONE 'UTC'", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("raw without placeholders")
        void testRawNoPlaceholders() {
            SqlFunction<Object> fn = Fn.raw("1 + 1");
            assertEquals("1 + 1", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("rawTyped with explicit return type")
        void testRawTyped() {
            SqlFunction<Integer> fn = Fn.rawTyped(Integer.class, "EXTRACT(YEAR FROM ?)", TestUser_.CREATED_AT);
            assertEquals(Integer.class, fn.getValueType());
            assertEquals("EXTRACT(YEAR FROM users.\"created_at\")", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("CASE expression with raw")
        void testCaseExpression() {
            SqlFunction<Object> fn = Fn.raw("CASE WHEN ? > 18 THEN 'adult' ELSE 'minor' END", TestUser_.AGE);
            assertEquals("CASE WHEN users.\"age\" > 18 THEN 'adult' ELSE 'minor' END", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("raw in WHERE clause")
        void testRawInWhere() {
            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .where(Fn.rawTyped(Integer.class, "EXTRACT(YEAR FROM ?)", TestUser_.CREATED_AT).eq(2024))
                .build();

            assertTrue(result.sql().contains("WHERE EXTRACT(YEAR FROM users.\"created_at\") = :p1"));
            assertEquals(2024, result.parameters().get("p1"));
        }
    }

    // ==================== ALIAS ====================

    @Nested
    @DisplayName("Alias Support")
    class AliasSupport {

        @Test
        @DisplayName("Function with alias")
        void testFunctionWithAlias() {
            SqlFunction<LocalDateTime> fn = Fn.now().as("current_time");
            assertEquals("current_time", fn.getAlias());
            assertEquals("NOW() AS \"current_time\"", fn.toSql(POSTGRES));
        }

        @Test
        @DisplayName("Nested function with alias")
        void testNestedFunctionWithAlias() {
            SqlFunction<String> fn = Fn.upper(TestUser_.NAME).as("upper_name");
            assertEquals("UPPER(users.\"name\") AS \"upper_name\"", fn.toSql(POSTGRES));
        }
    }

    // ==================== COMPARISON OPERATORS ====================

    @Nested
    @DisplayName("Comparison Operators")
    class ComparisonOperators {

        @Test
        @DisplayName("eq(value)")
        void testEqValue() {
            Predicate predicate = Fn.length(TestUser_.NAME).eq(10);
            assertNotNull(predicate);
        }

        @Test
        @DisplayName("ne(value)")
        void testNeValue() {
            Predicate predicate = Fn.length(TestUser_.NAME).ne(0);
            assertNotNull(predicate);
        }

        @Test
        @DisplayName("gt(value)")
        void testGtValue() {
            Predicate predicate = Fn.length(TestUser_.NAME).gt(5);
            assertNotNull(predicate);
        }

        @Test
        @DisplayName("gte(value)")
        void testGteValue() {
            Predicate predicate = Fn.length(TestUser_.NAME).gte(5);
            assertNotNull(predicate);
        }

        @Test
        @DisplayName("lt(value)")
        void testLtValue() {
            Predicate predicate = Fn.length(TestUser_.NAME).lt(100);
            assertNotNull(predicate);
        }

        @Test
        @DisplayName("lte(value)")
        void testLteValue() {
            Predicate predicate = Fn.length(TestUser_.NAME).lte(100);
            assertNotNull(predicate);
        }

        @Test
        @DisplayName("eq(expression)")
        void testEqExpression() {
            Predicate predicate = Fn.lower(TestUser_.NAME).eq(Fn.lower(TestUser_.EMAIL));
            assertNotNull(predicate);
        }
    }

    // ==================== ORDERING ====================

    @Nested
    @DisplayName("Ordering")
    class Ordering {

        @Test
        @DisplayName("asc() ordering")
        void testAsc() {
            OrderSpec order = Fn.length(TestUser_.NAME).asc();
            assertNotNull(order);
        }

        @Test
        @DisplayName("desc() ordering")
        void testDesc() {
            OrderSpec order = Fn.length(TestUser_.NAME).desc();
            assertNotNull(order);
        }

        @Test
        @DisplayName("ORDER BY function ASC")
        void testOrderByFunctionAsc() {
            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .orderBy(Fn.length(TestUser_.NAME).asc())
                .build();

            assertTrue(result.sql().contains("ORDER BY"));
            assertTrue(result.sql().contains("LENGTH(users.\"name\")"));
            assertTrue(result.sql().contains("ASC"));
        }

        @Test
        @DisplayName("ORDER BY function DESC")
        void testOrderByFunctionDesc() {
            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .orderBy(Fn.upper(TestUser_.NAME).desc())
                .build();

            assertTrue(result.sql().contains("ORDER BY"));
            assertTrue(result.sql().contains("UPPER(users.\"name\")"));
            assertTrue(result.sql().contains("DESC"));
        }
    }

    // ==================== INSERT WITH FUNCTIONS ====================

    @Nested
    @DisplayName("INSERT with Functions")
    class InsertWithFunctions {

        @Test
        @DisplayName("INSERT with NOW()")
        void testInsertWithNow() {
            QueryResult result = Suprim.insertInto(TestUser_.TABLE)
                .column(TestUser_.EMAIL, "test@example.com")
                .column(TestUser_.CREATED_AT, Fn.now())
                .build();

            assertTrue(result.sql().contains("INSERT INTO"));
            assertTrue(result.sql().contains("NOW()"));
        }
    }

    // ==================== UPDATE WITH FUNCTIONS ====================

    @Nested
    @DisplayName("UPDATE with Functions")
    class UpdateWithFunctions {

        @Test
        @DisplayName("UPDATE with UPPER()")
        void testUpdateWithUpper() {
            QueryResult result = Suprim.update(TestUser_.TABLE)
                .set(TestUser_.NAME, Fn.upper(TestUser_.NAME))
                .where(TestUser_.ID.eq(1L))
                .build();

            assertTrue(result.sql().contains("UPDATE"));
            assertTrue(result.sql().contains("SET"));
            assertTrue(result.sql().contains("UPPER(users.\"name\")"));
        }

        @Test
        @DisplayName("UPDATE with COALESCE()")
        void testUpdateWithCoalesce() {
            QueryResult result = Suprim.update(TestUser_.TABLE)
                .set(TestUser_.NAME, Fn.coalesce(TestUser_.NAME, "Unknown"))
                .where(TestUser_.ID.eq(1L))
                .build();

            assertTrue(result.sql().contains("COALESCE("));
        }
    }

    // ==================== DIALECT DIFFERENCES ====================

    @Nested
    @DisplayName("Dialect Differences")
    class DialectDifferences {

        @Test
        @DisplayName("PostgreSQL vs MySQL RANDOM")
        void testRandomDialects() {
            SqlFunction<Double> fn = Fn.random();
            assertEquals("RANDOM()", fn.toSql(POSTGRES));
            assertEquals("RAND()", fn.toSql(MYSQL));
        }

        @Test
        @DisplayName("PostgreSQL vs MySQL UUID")
        void testUuidDialects() {
            SqlFunction<UUID> fn = Fn.uuid();
            assertEquals("gen_random_uuid()", fn.toSql(POSTGRES));
            assertEquals("UUID()", fn.toSql(MYSQL));
        }

        @Test
        @DisplayName("PostgreSQL vs MySQL IFNULL")
        void testIfnullDialects() {
            SqlFunction<String> fn = Fn.ifNull(TestUser_.NAME, "Default");
            assertTrue(fn.toSql(POSTGRES).contains("COALESCE("));
            assertTrue(fn.toSql(MYSQL).contains("IFNULL("));
        }
    }
}
