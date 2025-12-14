package sant1ago.dev.suprim.core.dialect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.*;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full query integration tests - end-to-end query building across all dialects.
 * Validates complete SQL generation with proper dialect-specific handling.
 */
@DisplayName("Dialect Query Integration Tests")
class DialectQueryIntegrationTest {

    private static final PostgreSqlDialect POSTGRES = PostgreSqlDialect.INSTANCE;
    private static final MySqlDialect MYSQL = MySqlDialect.INSTANCE;
    private static final MySql8Dialect MYSQL8 = MySql8Dialect.INSTANCE;
    private static final MariaDbDialect MARIADB = MariaDbDialect.INSTANCE;

    @Entity(table = "users")
    static class User {}

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final ComparableColumn<User, Long> USER_ID =
            new ComparableColumn<>(USERS, "id", Long.class, "BIGINT");
    private static final StringColumn<User> USER_NAME =
            new StringColumn<>(USERS, "name", "VARCHAR");
    private static final StringColumn<User> USER_EMAIL =
            new StringColumn<>(USERS, "email", "VARCHAR");
    private static final Column<User, Boolean> USER_ACTIVE =
            new Column<>(USERS, "is_active", Boolean.class, "BOOLEAN");
    private static final ComparableColumn<User, Integer> USER_AGE =
            new ComparableColumn<>(USERS, "age", Integer.class, "INTEGER");
    private static final StringColumn<User> USER_STATUS =
            new StringColumn<>(USERS, "status", "VARCHAR");

    static Stream<SqlDialect> allDialects() {
        return Stream.of(POSTGRES, MYSQL, MYSQL8, MARIADB);
    }

    static Stream<SqlDialect> mysqlFamily() {
        return Stream.of(MYSQL, MYSQL8, MARIADB);
    }

    // ==================== Query Building Tests ====================

    @Nested
    @DisplayName("Basic Query Building")
    class BasicQueryTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("Query with predicate builds successfully")
        void queryWithPredicate(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of(USER_ID, USER_NAME))
                    .from(USERS)
                    .where(USER_ID.eq(1L))
                    .build(dialect).sql();

            assertNotNull(sql, "SQL should not be null");
            assertTrue(sql.length() > 0, "SQL should not be empty");
            assertTrue(sql.toUpperCase().contains("SELECT"), "Should have SELECT");
            assertTrue(sql.toUpperCase().contains("WHERE"), "Should have WHERE");
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("Query with ORDER BY builds successfully")
        void queryWithOrderBy(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .orderBy(USER_ID.desc())
                    .build(dialect).sql();

            assertNotNull(sql);
            assertTrue(sql.toUpperCase().contains("ORDER BY"));
            assertTrue(sql.toUpperCase().contains("DESC"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("Query with LIMIT/OFFSET builds successfully")
        void queryWithPagination(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .limit(10)
                    .offset(20)
                    .build(dialect).sql();

            assertNotNull(sql);
            assertTrue(sql.toUpperCase().contains("LIMIT"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("Query with GROUP BY builds successfully")
        void queryWithGroupBy(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .groupBy(USER_STATUS)
                    .build(dialect).sql();

            assertNotNull(sql);
            assertTrue(sql.toUpperCase().contains("GROUP BY"));
        }
    }

    // ==================== Predicate Composition Tests ====================

    @Nested
    @DisplayName("Predicate Composition")
    class PredicateCompositionTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("AND predicate composition")
        void andPredicate(SqlDialect dialect) {
            Predicate combined = USER_ACTIVE.eq(true).and(USER_AGE.gte(18));

            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(combined)
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("AND"), "Should have AND: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("OR predicate composition")
        void orPredicate(SqlDialect dialect) {
            Predicate combined = USER_STATUS.eq("active").or(USER_STATUS.eq("pending"));

            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(combined)
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("OR"), "Should have OR: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("NOT predicate")
        void notPredicate(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_ACTIVE.eq(true).not())
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("NOT"), "Should have NOT: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("Complex nested predicates")
        void complexNested(SqlDialect dialect) {
            // (active = true AND age >= 21) OR status = 'vip'
            Predicate group1 = USER_ACTIVE.eq(true).and(USER_AGE.gte(21));
            Predicate group2 = USER_STATUS.eq("vip");

            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(group1.or(group2))
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("AND"), "Should have AND");
            assertTrue(sql.toUpperCase().contains("OR"), "Should have OR");
        }
    }

    // ==================== Comparison Operators Tests ====================

    @Nested
    @DisplayName("Comparison Operators")
    class ComparisonOperatorTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("IS NULL operator")
        void isNull(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_EMAIL.isNull())
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("IS NULL"), "Should have IS NULL: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("IS NOT NULL operator")
        void isNotNull(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_EMAIL.isNotNull())
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("IS NOT NULL"), "Should have IS NOT NULL: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("IN operator")
        void inOperator(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_STATUS.in("active", "pending"))
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("IN ("), "Should have IN: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("BETWEEN operator")
        void betweenOperator(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_AGE.between(18, 65))
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("BETWEEN"), "Should have BETWEEN: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("LIKE operator")
        void likeOperator(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_EMAIL.like("%@test.com"))
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("LIKE"), "Should have LIKE: " + sql);
        }
    }

    // ==================== Dialect-Specific Tests ====================

    @Nested
    @DisplayName("Dialect-Specific Behavior")
    class DialectSpecificTests {

        @Test
        @DisplayName("PostgreSQL uses native ILIKE")
        void postgresIlike() {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_EMAIL.ilike("%test%"))
                    .build(POSTGRES).sql();

            assertTrue(sql.contains("ILIKE"), "PostgreSQL should use ILIKE: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#mysqlFamily")
        @DisplayName("MySQL family uses LOWER() for ILIKE")
        void mysqlIlike(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_EMAIL.ilike("%test%"))
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("LOWER"), "MySQL should use LOWER: " + sql);
            assertTrue(sql.toUpperCase().contains("LIKE"), "MySQL should use LIKE: " + sql);
        }

        @Test
        @DisplayName("PostgreSQL boolean uses parameters")
        void postgresBooleanFormat() {
            var result = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_ACTIVE.eq(true))
                    .build(POSTGRES);

            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals(true, result.parameters().get("p1"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#mysqlFamily")
        @DisplayName("MySQL boolean uses parameters")
        void mysqlBooleanFormat(SqlDialect dialect) {
            var result = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_ACTIVE.eq(true))
                    .build(dialect);

            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals(true, result.parameters().get("p1"));
        }
    }

    // ==================== String Values as Parameters Tests ====================

    @Nested
    @DisplayName("String Values as Parameters")
    class StringEscapingTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("String values are parameterized")
        void singleQuoteEscaped(SqlDialect dialect) {
            var result = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_NAME.eq("O'Brien"))
                    .build(dialect);

            // Value should be in parameters, not inline SQL
            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals("O'Brien", result.parameters().get("p1"));
        }

        @Test
        @DisplayName("MySQL uses parameters for string values")
        void mysqlBackslashEscaped() {
            var result = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_NAME.eq("C:\\path"))
                    .build(MYSQL);

            // Value should be in parameters, not inline SQL
            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals("C:\\path", result.parameters().get("p1"));
        }

        @Test
        @DisplayName("PostgreSQL uses parameters for string values")
        void postgresBackslashPreserved() {
            var result = new SelectBuilder(List.of())
                    .from(USERS)
                    .where(USER_NAME.eq("C:\\path"))
                    .build(POSTGRES);

            // Value should be in parameters, not inline SQL
            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals("C:\\path", result.parameters().get("p1"));
        }
    }

    // ==================== Cross-Dialect Consistency Tests ====================

    @Nested
    @DisplayName("Cross-Dialect Consistency")
    class ConsistencyTests {

        @Test
        @DisplayName("Same builder produces valid SQL for all dialects")
        void sameBuilderAllDialects() {
            SelectBuilder builder = new SelectBuilder(List.of(USER_ID, USER_NAME))
                    .from(USERS)
                    .where(USER_ACTIVE.eq(true).and(USER_AGE.gte(18)))
                    .orderBy(USER_NAME.asc())
                    .limit(10);

            for (SqlDialect dialect : new SqlDialect[]{POSTGRES, MYSQL, MYSQL8, MARIADB}) {
                String sql = builder.build(dialect).sql();

                assertNotNull(sql, "SQL null for " + dialect.getName());
                assertTrue(sql.toUpperCase().contains("SELECT"), "SELECT missing for " + dialect.getName());
                assertTrue(sql.toUpperCase().contains("WHERE"), "WHERE missing for " + dialect.getName());
                assertTrue(sql.toUpperCase().contains("ORDER BY"), "ORDER BY missing for " + dialect.getName());
                assertTrue(sql.toUpperCase().contains("LIMIT"), "LIMIT missing for " + dialect.getName());
            }
        }

        @Test
        @DisplayName("All dialects handle complex predicates")
        void complexPredicatesAllDialects() {
            Predicate complex = USER_ACTIVE.eq(true)
                    .and(USER_STATUS.in("active", "pending"))
                    .and(USER_AGE.between(18, 65));

            for (SqlDialect dialect : new SqlDialect[]{POSTGRES, MYSQL, MYSQL8, MARIADB}) {
                String sql = new SelectBuilder(List.of())
                        .from(USERS)
                        .where(complex)
                        .build(dialect).sql();

                assertNotNull(sql);
                assertTrue(sql.toUpperCase().contains("AND"));
                assertTrue(sql.toUpperCase().contains("IN"));
                assertTrue(sql.toUpperCase().contains("BETWEEN"));
            }
        }
    }

    // ==================== Raw SQL Tests ====================

    @Nested
    @DisplayName("Raw SQL")
    class RawSqlTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("whereRaw includes raw SQL")
        void whereRaw(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .whereRaw("created_at > NOW()")
                    .build(dialect).sql();

            assertTrue(sql.contains("created_at > NOW()"), "Should contain raw SQL: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.dialect.DialectQueryIntegrationTest#allDialects")
        @DisplayName("selectRaw includes raw select")
        void selectRaw(SqlDialect dialect) {
            String sql = new SelectBuilder(List.of())
                    .from(USERS)
                    .selectRaw("COUNT(*) as total")
                    .build(dialect).sql();

            assertTrue(sql.contains("COUNT(*)"), "Should contain COUNT: " + sql);
        }
    }
}
