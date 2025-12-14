package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.TestCte;
import sant1ago.dev.suprim.core.TestOrder_;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.*;
import sant1ago.dev.suprim.core.type.*;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive dialect-specific tests for SelectBuilder.
 * Tests all SelectBuilder methods across PostgreSQL, MySQL 5.7, MySQL 8.0, and MariaDB.
 */
@DisplayName("SelectBuilder Dialect Tests")
class SelectBuilderDialectTest {

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

    // ==================== Local Test Entities for Relation Tests ====================

    @Entity(table = "users")
    static class LocalUser {}

    @Entity(table = "orders")
    static class LocalOrder {}

    private static final Table<LocalUser> LOCAL_USERS = Table.of("users", LocalUser.class);
    private static final Table<LocalOrder> LOCAL_ORDERS = Table.of("orders", LocalOrder.class);

    private static final ComparableColumn<LocalUser, Long> LOCAL_USER_ID =
            new ComparableColumn<>(LOCAL_USERS, "id", Long.class, "BIGINT");

    private static final ComparableColumn<LocalOrder, Long> LOCAL_ORDER_ID =
            new ComparableColumn<>(LOCAL_ORDERS, "id", Long.class, "BIGINT");
    private static final ComparableColumn<LocalOrder, BigDecimal> LOCAL_ORDER_AMOUNT =
            new ComparableColumn<>(LOCAL_ORDERS, "amount", BigDecimal.class, "DECIMAL");

    private static final Relation<LocalUser, LocalOrder> USER_ORDERS = Relation.hasMany(
            LOCAL_USERS, LOCAL_ORDERS, "id", "user_id", false, false
    );

    // ==================== 1. Basic SELECT Tests (16 tests) ====================

    @Nested
    @DisplayName("Basic SELECT")
    class BasicSelectTests {

        @Test
        @DisplayName("SELECT * - PostgreSQL uses double quotes")
        void selectAllPostgreSql() {
            String sql = Suprim.select().from(TestUser_.TABLE).build(POSTGRES).sql();
            assertTrue(sql.contains("\"users\""), "PostgreSQL uses double quotes: " + sql);
        }

        @Test
        @DisplayName("SELECT * - MySQL uses backticks")
        void selectAllMySql() {
            String sql = Suprim.select().from(TestUser_.TABLE).build(MYSQL).sql();
            assertTrue(sql.contains("`users`"), "MySQL uses backticks: " + sql);
        }

        @Test
        @DisplayName("SELECT * - MySQL 8 uses backticks")
        void selectAllMySql8() {
            String sql = Suprim.select().from(TestUser_.TABLE).build(MYSQL8).sql();
            assertTrue(sql.contains("`users`"), "MySQL 8 uses backticks: " + sql);
        }

        @Test
        @DisplayName("SELECT * - MariaDB uses backticks")
        void selectAllMariaDb() {
            String sql = Suprim.select().from(TestUser_.TABLE).build(MARIADB).sql();
            assertTrue(sql.contains("`users`"), "MariaDB uses backticks: " + sql);
        }

        @Test
        @DisplayName("SELECT columns - PostgreSQL")
        void selectColumnsPostgreSql() {
            String sql = Suprim.select(TestUser_.ID, TestUser_.NAME)
                    .from(TestUser_.TABLE).build(POSTGRES).sql();
            assertTrue(sql.contains("\"id\""), "PostgreSQL quotes id: " + sql);
            assertTrue(sql.contains("\"name\""), "PostgreSQL quotes name: " + sql);
        }

        @Test
        @DisplayName("SELECT columns - MySQL")
        void selectColumnsMySql() {
            String sql = Suprim.select(TestUser_.ID, TestUser_.NAME)
                    .from(TestUser_.TABLE).build(MYSQL).sql();
            assertTrue(sql.contains("`id`"), "MySQL quotes id: " + sql);
            assertTrue(sql.contains("`name`"), "MySQL quotes name: " + sql);
        }

        @Test
        @DisplayName("SELECT with alias - PostgreSQL")
        void selectWithAliasPostgreSql() {
            String sql = Suprim.select(TestUser_.ID.as("user_id"))
                    .from(TestUser_.TABLE).build(POSTGRES).sql();
            assertTrue(sql.contains("AS user_id"), "Should have alias: " + sql);
        }

        @Test
        @DisplayName("SELECT with alias - MySQL")
        void selectWithAliasMySql() {
            String sql = Suprim.select(TestUser_.ID.as("user_id"))
                    .from(TestUser_.TABLE).build(MYSQL).sql();
            assertTrue(sql.contains("AS user_id"), "Should have alias: " + sql);
        }

        @Test
        @DisplayName("SELECT DISTINCT - PostgreSQL")
        void selectDistinctPostgreSql() {
            String sql = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE)
                    .distinct().build(POSTGRES).sql();
            assertTrue(sql.contains("SELECT DISTINCT"), "Should have DISTINCT: " + sql);
            assertTrue(sql.contains("\"email\""), "PostgreSQL quotes: " + sql);
        }

        @Test
        @DisplayName("SELECT DISTINCT - MySQL")
        void selectDistinctMySql() {
            String sql = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE)
                    .distinct().build(MYSQL).sql();
            assertTrue(sql.contains("SELECT DISTINCT"), "Should have DISTINCT: " + sql);
            assertTrue(sql.contains("`email`"), "MySQL quotes: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("SELECT all columns structure consistent")
        void selectAllStructure(SqlDialect dialect) {
            String sql = Suprim.select().from(TestUser_.TABLE).build(dialect).sql();
            assertTrue(sql.toUpperCase().startsWith("SELECT *"), "Should start with SELECT *: " + sql);
            assertTrue(sql.toUpperCase().contains("FROM"), "Should have FROM: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("SELECT specific columns structure consistent")
        void selectColumnsStructure(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID, TestUser_.EMAIL).from(TestUser_.TABLE).build(dialect).sql();
            assertTrue(sql.toUpperCase().startsWith("SELECT"), "Should start with SELECT: " + sql);
            assertFalse(sql.contains("*"), "Should NOT have *: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("SELECT multiple columns")
        void selectMultipleColumns(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID, TestUser_.NAME, TestUser_.EMAIL, TestUser_.AGE)
                    .from(TestUser_.TABLE).build(dialect).sql();
            // All columns should be present
            assertNotNull(sql);
            assertTrue(sql.toUpperCase().contains("SELECT"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("SELECT without FROM")
        void selectWithoutFrom(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("SELECT"), "Should have SELECT");
            assertFalse(sql.toUpperCase().contains("FROM"), "Should NOT have FROM");
        }
    }

    // ==================== 2. WHERE Clause Tests (24 tests) ====================

    @Nested
    @DisplayName("WHERE Clause")
    class WhereClauseTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE equals")
        void whereEq(SqlDialect dialect) {
            var result = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com")).build(dialect);
            assertTrue(result.sql().toUpperCase().contains("WHERE"), "Should have WHERE: " + result.sql());
            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals("test@example.com", result.parameters().get("p1"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE not equals")
        void whereNe(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.ne("spam@example.com")).build(dialect).sql();
            assertTrue(sql.contains("!=") || sql.contains("<>"), "Should have != or <>: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE greater than")
        void whereGt(SqlDialect dialect) {
            var result = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.gt(18)).build(dialect);
            assertTrue(result.sql().contains("> :p1"), "Should have > :p1: " + result.sql());
            assertEquals(18, result.parameters().get("p1"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE greater than or equals")
        void whereGte(SqlDialect dialect) {
            var result = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.gte(21)).build(dialect);
            assertTrue(result.sql().contains(">= :p1"), "Should have >= :p1: " + result.sql());
            assertEquals(21, result.parameters().get("p1"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE less than")
        void whereLt(SqlDialect dialect) {
            var result = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.lt(65)).build(dialect);
            assertTrue(result.sql().contains("< :p1"), "Should have < :p1: " + result.sql());
            assertEquals(65, result.parameters().get("p1"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE less than or equals")
        void whereLte(SqlDialect dialect) {
            var result = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.lte(60)).build(dialect);
            assertTrue(result.sql().contains("<= :p1"), "Should have <= :p1: " + result.sql());
            assertEquals(60, result.parameters().get("p1"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE IS NULL")
        void whereIsNull(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.NAME.isNull()).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("IS NULL"), "Should have IS NULL: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE IS NOT NULL")
        void whereIsNotNull(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.NAME.isNotNull()).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("IS NOT NULL"), "Should have IS NOT NULL: " + sql);
        }

        @Test
        @DisplayName("WHERE LIKE - PostgreSQL")
        void whereLikePostgreSql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.like("%@test.com")).build(POSTGRES).sql();
            assertTrue(sql.toUpperCase().contains("LIKE"), "Should have LIKE: " + sql);
        }

        @Test
        @DisplayName("WHERE LIKE - MySQL")
        void whereLikeMySql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.like("%@test.com")).build(MYSQL).sql();
            assertTrue(sql.toUpperCase().contains("LIKE"), "Should have LIKE: " + sql);
        }

        @Test
        @DisplayName("WHERE ILIKE - PostgreSQL uses native ILIKE")
        void whereIlikePostgreSql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.ilike("%test%")).build(POSTGRES).sql();
            assertTrue(sql.contains("ILIKE"), "PostgreSQL should use ILIKE: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#mysqlFamily")
        @DisplayName("WHERE ILIKE - MySQL family uses LOWER()")
        void whereIlikeMySql(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.ilike("%test%")).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("LOWER"), "MySQL should use LOWER: " + sql);
            assertTrue(sql.toUpperCase().contains("LIKE"), "MySQL should use LIKE: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE IN")
        void whereIn(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.in(18, 21, 25)).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("IN ("), "Should have IN: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE NOT IN")
        void whereNotIn(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.notIn(1, 2, 3)).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("NOT IN"), "Should have NOT IN: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE BETWEEN")
        void whereBetween(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.between(18, 65)).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("BETWEEN"), "Should have BETWEEN: " + sql);
            assertTrue(sql.toUpperCase().contains("AND"), "Should have AND: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE with AND")
        void whereAnd(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.gte(18))
                    .and(TestUser_.IS_ACTIVE.eq(true)).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("AND"), "Should have AND: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE with OR")
        void whereOr(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.lt(18))
                    .or(TestUser_.AGE.gt(65)).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("OR"), "Should have OR: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE with AND and OR combined")
        void whereAndOr(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.AGE.gte(18).and(TestUser_.IS_ACTIVE.eq(true)))
                    .or(TestUser_.EMAIL.like("%@admin.com")).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("AND"), "Should have AND: " + sql);
            assertTrue(sql.toUpperCase().contains("OR"), "Should have OR: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("WHERE with NOT")
        void whereNot(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.IS_ACTIVE.eq(true).not()).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("NOT"), "Should have NOT: " + sql);
        }

        @Test
        @DisplayName("WHERE string escaping - PostgreSQL uses parameters")
        void whereStringEscapingPostgreSql() {
            var result = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.NAME.eq("O'Brien")).build(POSTGRES);
            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals("O'Brien", result.parameters().get("p1"));
        }

        @Test
        @DisplayName("WHERE string escaping - MySQL uses parameters")
        void whereStringEscapingMySql() {
            var result = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.NAME.eq("O'Brien")).build(MYSQL);
            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals("O'Brien", result.parameters().get("p1"));
        }

        @Test
        @DisplayName("WHERE backslash - PostgreSQL uses parameters")
        void whereBackslashPostgreSql() {
            var result = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.NAME.eq("C:\\path")).build(POSTGRES);
            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals("C:\\path", result.parameters().get("p1"));
        }

        @Test
        @DisplayName("WHERE backslash - MySQL uses parameters")
        void whereBackslashMySql() {
            var result = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.NAME.eq("C:\\path")).build(MYSQL);
            assertTrue(result.sql().contains(":p1"), "Should use parameter: " + result.sql());
            assertEquals("C:\\path", result.parameters().get("p1"));
        }
    }

    // ==================== 3. JOIN Clause Tests (20 tests) ====================

    @Nested
    @DisplayName("JOIN Clause")
    class JoinClauseTests {

        @Test
        @DisplayName("INNER JOIN - PostgreSQL")
        void innerJoinPostgreSql() {
            String sql = Suprim.select(TestUser_.ID, TestOrder_.AMOUNT)
                    .from(TestUser_.TABLE)
                    .join(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .build(POSTGRES).sql();
            assertTrue(sql.contains("JOIN \"orders\""), "PostgreSQL JOIN: " + sql);
            assertTrue(sql.toUpperCase().contains("ON"), "Should have ON: " + sql);
        }

        @Test
        @DisplayName("INNER JOIN - MySQL")
        void innerJoinMySql() {
            String sql = Suprim.select(TestUser_.ID, TestOrder_.AMOUNT)
                    .from(TestUser_.TABLE)
                    .join(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .build(MYSQL).sql();
            assertTrue(sql.contains("JOIN `orders`"), "MySQL JOIN: " + sql);
        }

        @Test
        @DisplayName("LEFT JOIN - PostgreSQL")
        void leftJoinPostgreSql() {
            String sql = Suprim.select(TestUser_.ID, TestOrder_.AMOUNT)
                    .from(TestUser_.TABLE)
                    .leftJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .build(POSTGRES).sql();
            assertTrue(sql.contains("LEFT JOIN \"orders\""), "PostgreSQL LEFT JOIN: " + sql);
        }

        @Test
        @DisplayName("LEFT JOIN - MySQL")
        void leftJoinMySql() {
            String sql = Suprim.select(TestUser_.ID, TestOrder_.AMOUNT)
                    .from(TestUser_.TABLE)
                    .leftJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .build(MYSQL).sql();
            assertTrue(sql.contains("LEFT JOIN `orders`"), "MySQL LEFT JOIN: " + sql);
        }

        @Test
        @DisplayName("RIGHT JOIN - PostgreSQL")
        void rightJoinPostgreSql() {
            String sql = Suprim.select(TestUser_.ID, TestOrder_.AMOUNT)
                    .from(TestUser_.TABLE)
                    .rightJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .build(POSTGRES).sql();
            assertTrue(sql.contains("RIGHT JOIN \"orders\""), "PostgreSQL RIGHT JOIN: " + sql);
        }

        @Test
        @DisplayName("RIGHT JOIN - MySQL")
        void rightJoinMySql() {
            String sql = Suprim.select(TestUser_.ID, TestOrder_.AMOUNT)
                    .from(TestUser_.TABLE)
                    .rightJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .build(MYSQL).sql();
            assertTrue(sql.contains("RIGHT JOIN `orders`"), "MySQL RIGHT JOIN: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("JOIN with AND condition")
        void joinWithAndCondition(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .join(TestOrder_.TABLE,
                            TestOrder_.USER_ID.eq(TestUser_.ID).and(TestOrder_.AMOUNT.gt(BigDecimal.ZERO)))
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("AND"), "Should have AND in ON: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("Multiple JOINs")
        void multipleJoins(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .join(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .leftJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("JOIN"), "Should have JOINs: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("JOIN structure consistent")
        void joinStructure(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .join(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("FROM"), "Should have FROM");
            assertTrue(sql.toUpperCase().contains("JOIN"), "Should have JOIN");
            assertTrue(sql.toUpperCase().contains("ON"), "Should have ON");
        }
    }

    // ==================== 4. ORDER BY Tests (12 tests) ====================

    @Nested
    @DisplayName("ORDER BY Clause")
    class OrderByTests {

        @Test
        @DisplayName("ORDER BY ASC - PostgreSQL")
        void orderByAscPostgreSql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .orderBy(TestUser_.NAME.asc()).build(POSTGRES).sql();
            assertTrue(sql.contains("ORDER BY users.\"name\" ASC"), "PostgreSQL ORDER BY ASC: " + sql);
        }

        @Test
        @DisplayName("ORDER BY ASC - MySQL")
        void orderByAscMySql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .orderBy(TestUser_.NAME.asc()).build(MYSQL).sql();
            assertTrue(sql.contains("ORDER BY users.`name` ASC"), "MySQL ORDER BY ASC: " + sql);
        }

        @Test
        @DisplayName("ORDER BY DESC - PostgreSQL")
        void orderByDescPostgreSql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .orderBy(TestUser_.CREATED_AT.desc()).build(POSTGRES).sql();
            assertTrue(sql.contains("DESC"), "Should have DESC: " + sql);
        }

        @Test
        @DisplayName("ORDER BY DESC - MySQL")
        void orderByDescMySql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .orderBy(TestUser_.CREATED_AT.desc()).build(MYSQL).sql();
            assertTrue(sql.contains("DESC"), "Should have DESC: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("ORDER BY multiple columns")
        void orderByMultipleColumns(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .orderBy(TestUser_.AGE.desc(), TestUser_.NAME.asc()).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("ORDER BY"), "Should have ORDER BY: " + sql);
            assertTrue(sql.toUpperCase().contains("DESC"), "Should have DESC");
            assertTrue(sql.toUpperCase().contains("ASC"), "Should have ASC");
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("ORDER BY structure")
        void orderByStructure(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .orderBy(TestUser_.ID.asc()).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("ORDER BY"), "Should have ORDER BY");
        }
    }

    // ==================== 5. GROUP BY / HAVING Tests (16 tests) ====================

    @Nested
    @DisplayName("GROUP BY / HAVING")
    class GroupByHavingTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("GROUP BY single column")
        void groupBySingle(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.AGE).from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("GROUP BY"), "Should have GROUP BY: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("GROUP BY multiple columns")
        void groupByMultiple(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.AGE, TestUser_.IS_ACTIVE).from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE, TestUser_.IS_ACTIVE).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("GROUP BY"), "Should have GROUP BY");
        }

        @Test
        @DisplayName("GROUP BY - PostgreSQL quoting")
        void groupByPostgreSql() {
            String sql = Suprim.select(TestUser_.AGE).from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE).build(POSTGRES).sql();
            assertTrue(sql.contains("\"age\""), "PostgreSQL quotes: " + sql);
        }

        @Test
        @DisplayName("GROUP BY - MySQL quoting")
        void groupByMySql() {
            String sql = Suprim.select(TestUser_.AGE).from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE).build(MYSQL).sql();
            assertTrue(sql.contains("`age`"), "MySQL quotes: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("HAVING clause")
        void havingClause(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.AGE).from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE)
                    .having(TestUser_.AGE.gt(18)).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("HAVING"), "Should have HAVING: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("HAVING with multiple conditions")
        void havingWithAnd(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.AGE).from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE)
                    .having(TestUser_.AGE.gt(18).and(TestUser_.AGE.lt(65))).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("HAVING"), "Should have HAVING");
            assertTrue(sql.toUpperCase().contains("AND"), "Should have AND");
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("GROUP BY with HAVING structure")
        void groupByHavingStructure(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.AGE).from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE)
                    .having(TestUser_.AGE.gte(21)).build(dialect).sql();
            int groupByPos = sql.toUpperCase().indexOf("GROUP BY");
            int havingPos = sql.toUpperCase().indexOf("HAVING");
            assertTrue(groupByPos < havingPos, "GROUP BY should come before HAVING");
        }
    }

    // ==================== 6. LIMIT / OFFSET Tests (8 tests) ====================

    @Nested
    @DisplayName("LIMIT / OFFSET")
    class LimitOffsetTests {

        @Test
        @DisplayName("LIMIT only - PostgreSQL")
        void limitOnlyPostgreSql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .limit(10).build(POSTGRES).sql();
            assertTrue(sql.contains("LIMIT 10"), "PostgreSQL LIMIT: " + sql);
        }

        @Test
        @DisplayName("LIMIT only - MySQL")
        void limitOnlyMySql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .limit(10).build(MYSQL).sql();
            assertTrue(sql.contains("LIMIT 10"), "MySQL LIMIT: " + sql);
        }

        @Test
        @DisplayName("LIMIT OFFSET - PostgreSQL")
        void limitOffsetPostgreSql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .limit(10).offset(20).build(POSTGRES).sql();
            assertTrue(sql.contains("LIMIT 10"), "Should have LIMIT: " + sql);
            assertTrue(sql.contains("OFFSET 20"), "Should have OFFSET: " + sql);
        }

        @Test
        @DisplayName("LIMIT OFFSET - MySQL")
        void limitOffsetMySql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .limit(10).offset(20).build(MYSQL).sql();
            assertTrue(sql.contains("LIMIT 10"), "Should have LIMIT: " + sql);
            assertTrue(sql.contains("OFFSET 20"), "Should have OFFSET: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("Pagination page 1")
        void paginatePage1(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .paginate(1, 20).build(dialect).sql();
            assertTrue(sql.contains("LIMIT 20"), "Should have LIMIT 20");
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("Pagination page 3")
        void paginatePage3(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .paginate(3, 20).build(dialect).sql();
            assertTrue(sql.contains("LIMIT 20"), "Should have LIMIT 20");
            assertTrue(sql.contains("OFFSET 40"), "Should have OFFSET 40");
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("LIMIT structure")
        void limitStructure(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .limit(100).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("LIMIT"), "Should have LIMIT");
        }
    }

    // ==================== 7. Relation Methods Tests (32 tests) ====================

    @Nested
    @DisplayName("Relation Methods")
    class RelationMethodTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("whereHas generates EXISTS")
        void whereHas(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .whereHas(USER_ORDERS)
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("EXISTS"), "Should have EXISTS: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("whereDoesntHave generates NOT EXISTS")
        void whereDoesntHave(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .whereDoesntHave(USER_ORDERS)
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("NOT EXISTS"), "Should have NOT EXISTS: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("whereHas with constraint")
        void whereHasWithConstraint(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .whereHas(USER_ORDERS, orders -> orders.where(LOCAL_ORDER_AMOUNT.gt(BigDecimal.valueOf(100))))
                    .build(dialect).sql();
            assertTrue(sql.contains("> 100"), "Should have constraint: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("has with count check")
        void hasCountCheck(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .has(USER_ORDERS, ">=", 3)
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("COUNT"), "Should have COUNT: " + sql);
            assertTrue(sql.contains(">= 3"), "Should have >= 3: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("withCount")
        void withCount(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .withCount(USER_ORDERS)
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("COUNT"), "Should have COUNT: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("withExists")
        void withExists(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .withExists(USER_ORDERS, "has_orders")
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("EXISTS"), "Should have EXISTS: " + sql);
            assertTrue(sql.contains("AS has_orders"), "Should have alias: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("withSum")
        void withSum(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .withSum(USER_ORDERS, LOCAL_ORDER_AMOUNT, "total_amount")
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("SUM"), "Should have SUM: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("withMax")
        void withMax(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .withMax(USER_ORDERS, LOCAL_ORDER_AMOUNT, "max_amount")
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("MAX"), "Should have MAX: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("withMin")
        void withMin(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .withMin(USER_ORDERS, LOCAL_ORDER_AMOUNT, "min_amount")
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("MIN"), "Should have MIN: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("withAvg")
        void withAvg(SqlDialect dialect) {
            String sql = new SelectBuilder(java.util.List.of(LOCAL_USER_ID))
                    .from(LOCAL_USERS)
                    .withAvg(USER_ORDERS, LOCAL_ORDER_AMOUNT, "avg_amount")
                    .build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("AVG"), "Should have AVG: " + sql);
        }
    }

    // ==================== 8. CTE Tests (12 tests) ====================

    @Nested
    @DisplayName("CTE (WITH)")
    class CteTests {

        @Test
        @DisplayName("CTE - PostgreSQL")
        void ctePostgreSql() {
            SelectBuilder activeUsers = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
                    .from(TestUser_.TABLE).where(TestUser_.IS_ACTIVE.eq(true));
            String sql = Suprim.select().with("active_users", activeUsers)
                    .from(Table.of("active_users", TestCte.class)).build(POSTGRES).sql();
            assertTrue(sql.contains("WITH active_users AS"), "PostgreSQL CTE: " + sql);
        }

        @Test
        @DisplayName("CTE - MySQL 8")
        void cteMySql8() {
            SelectBuilder activeUsers = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
                    .from(TestUser_.TABLE).where(TestUser_.IS_ACTIVE.eq(true));
            String sql = Suprim.select().with("active_users", activeUsers)
                    .from(Table.of("active_users", TestCte.class)).build(MYSQL8).sql();
            assertTrue(sql.contains("WITH active_users AS"), "MySQL 8 CTE: " + sql);
        }

        @Test
        @DisplayName("Recursive CTE - PostgreSQL")
        void recursiveCtePostgreSql() {
            String sql = Suprim.select()
                    .withRecursive("numbers", "SELECT 1 AS n UNION ALL SELECT n + 1 FROM numbers WHERE n < 10")
                    .from(Table.of("numbers", TestCte.class)).build(POSTGRES).sql();
            assertTrue(sql.contains("WITH RECURSIVE"), "PostgreSQL recursive CTE: " + sql);
        }

        @Test
        @DisplayName("Recursive CTE - MySQL 8")
        void recursiveCteMySql8() {
            String sql = Suprim.select()
                    .withRecursive("numbers", "SELECT 1 AS n UNION ALL SELECT n + 1 FROM numbers WHERE n < 10")
                    .from(Table.of("numbers", TestCte.class)).build(MYSQL8).sql();
            assertTrue(sql.contains("WITH RECURSIVE"), "MySQL 8 recursive CTE: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("CTE structure")
        void cteStructure(SqlDialect dialect) {
            SelectBuilder sub = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            String sql = Suprim.select().with("cte_test", sub)
                    .from(Table.of("cte_test", TestCte.class)).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("WITH"), "Should have WITH");
            assertTrue(sql.toUpperCase().contains("AS"), "Should have AS");
        }
    }

    // ==================== 9. Set Operations Tests (12 tests) ====================

    @Nested
    @DisplayName("Set Operations")
    class SetOperationTests {

        @Test
        @DisplayName("UNION - PostgreSQL")
        void unionPostgreSql() {
            SelectBuilder q1 = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE).where(TestUser_.AGE.lt(30));
            SelectBuilder q2 = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE).where(TestUser_.AGE.gt(60));
            String sql = q1.union(q2).build(POSTGRES).sql();
            assertTrue(sql.contains("UNION"), "PostgreSQL UNION: " + sql);
            assertFalse(sql.contains("UNION ALL"), "Should NOT be UNION ALL: " + sql);
        }

        @Test
        @DisplayName("UNION - MySQL")
        void unionMySql() {
            SelectBuilder q1 = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE).where(TestUser_.AGE.lt(30));
            SelectBuilder q2 = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE).where(TestUser_.AGE.gt(60));
            String sql = q1.union(q2).build(MYSQL).sql();
            assertTrue(sql.contains("UNION"), "MySQL UNION: " + sql);
        }

        @Test
        @DisplayName("UNION ALL - PostgreSQL")
        void unionAllPostgreSql() {
            SelectBuilder q1 = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE).where(TestUser_.AGE.lt(30));
            SelectBuilder q2 = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE).where(TestUser_.AGE.gt(60));
            String sql = q1.unionAll(q2).build(POSTGRES).sql();
            assertTrue(sql.contains("UNION ALL"), "PostgreSQL UNION ALL: " + sql);
        }

        @Test
        @DisplayName("UNION ALL - MySQL")
        void unionAllMySql() {
            SelectBuilder q1 = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE).where(TestUser_.AGE.lt(30));
            SelectBuilder q2 = Suprim.select(TestUser_.EMAIL).from(TestUser_.TABLE).where(TestUser_.AGE.gt(60));
            String sql = q1.unionAll(q2).build(MYSQL).sql();
            assertTrue(sql.contains("UNION ALL"), "MySQL UNION ALL: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("UNION structure")
        void unionStructure(SqlDialect dialect) {
            SelectBuilder q1 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            SelectBuilder q2 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            String sql = q1.union(q2).build(dialect).sql();
            assertTrue(sql.toUpperCase().contains("UNION"), "Should have UNION");
        }
    }

    // ==================== 10. Complex Queries Tests (8 tests) ====================

    @Nested
    @DisplayName("Complex Queries")
    class ComplexQueryTests {

        @Test
        @DisplayName("Complex query - PostgreSQL")
        void complexQueryPostgreSql() {
            String sql = Suprim.select(TestUser_.ID, TestUser_.EMAIL, TestUser_.AGE)
                    .from(TestUser_.TABLE)
                    .leftJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .where(TestUser_.IS_ACTIVE.eq(true))
                    .and(TestUser_.AGE.gte(18))
                    .orderBy(TestUser_.CREATED_AT.desc())
                    .limit(10)
                    .offset(5)
                    .build(POSTGRES).sql();

            assertTrue(sql.contains("\"users\""), "PostgreSQL quotes");
            assertTrue(sql.toUpperCase().contains("LEFT JOIN"), "Has LEFT JOIN");
            assertTrue(sql.toUpperCase().contains("WHERE"), "Has WHERE");
            assertTrue(sql.toUpperCase().contains("ORDER BY"), "Has ORDER BY");
            assertTrue(sql.contains("LIMIT 10"), "Has LIMIT");
            assertTrue(sql.contains("OFFSET 5"), "Has OFFSET");
        }

        @Test
        @DisplayName("Complex query - MySQL")
        void complexQueryMySql() {
            String sql = Suprim.select(TestUser_.ID, TestUser_.EMAIL, TestUser_.AGE)
                    .from(TestUser_.TABLE)
                    .leftJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
                    .where(TestUser_.IS_ACTIVE.eq(true))
                    .and(TestUser_.AGE.gte(18))
                    .orderBy(TestUser_.CREATED_AT.desc())
                    .limit(10)
                    .offset(5)
                    .build(MYSQL).sql();

            assertTrue(sql.contains("`users`"), "MySQL quotes");
            assertTrue(sql.toUpperCase().contains("LEFT JOIN"), "Has LEFT JOIN");
            assertTrue(sql.toUpperCase().contains("WHERE"), "Has WHERE");
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("Query with all clauses")
        void queryWithAllClauses(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.AGE.gte(18))
                    .groupBy(TestUser_.AGE)
                    .having(TestUser_.AGE.lt(65))
                    .orderBy(TestUser_.AGE.asc())
                    .limit(100)
                    .build(dialect).sql();

            assertTrue(sql.toUpperCase().contains("SELECT"), "Has SELECT");
            assertTrue(sql.toUpperCase().contains("FROM"), "Has FROM");
            assertTrue(sql.toUpperCase().contains("WHERE"), "Has WHERE");
            assertTrue(sql.toUpperCase().contains("GROUP BY"), "Has GROUP BY");
            assertTrue(sql.toUpperCase().contains("HAVING"), "Has HAVING");
            assertTrue(sql.toUpperCase().contains("ORDER BY"), "Has ORDER BY");
            assertTrue(sql.toUpperCase().contains("LIMIT"), "Has LIMIT");
        }

        @Test
        @DisplayName("Subquery in WHERE - PostgreSQL")
        void subqueryInWherePostgreSql() {
            SelectBuilder subquery = Suprim.select(TestOrder_.USER_ID)
                    .from(TestOrder_.TABLE)
                    .where(TestOrder_.AMOUNT.gt(BigDecimal.valueOf(100)));
            String sql = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .whereInSubquery(TestUser_.ID, subquery)
                    .build(POSTGRES).sql();
            assertTrue(sql.toUpperCase().contains("IN"), "Has IN subquery: " + sql);
        }

        @Test
        @DisplayName("Subquery in WHERE - MySQL")
        void subqueryInWhereMySql() {
            SelectBuilder subquery = Suprim.select(TestOrder_.USER_ID)
                    .from(TestOrder_.TABLE)
                    .where(TestOrder_.AMOUNT.gt(BigDecimal.valueOf(100)));
            String sql = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .whereInSubquery(TestUser_.ID, subquery)
                    .build(MYSQL).sql();
            assertTrue(sql.toUpperCase().contains("IN"), "Has IN subquery: " + sql);
        }

        @Test
        @DisplayName("EXISTS subquery - PostgreSQL")
        void existsSubqueryPostgreSql() {
            SelectBuilder subquery = Suprim.select(TestOrder_.ID)
                    .from(TestOrder_.TABLE)
                    .where(TestOrder_.USER_ID.eq(TestUser_.ID));
            String sql = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .whereExists(subquery)
                    .build(POSTGRES).sql();
            assertTrue(sql.toUpperCase().contains("EXISTS"), "Has EXISTS: " + sql);
        }

        @Test
        @DisplayName("EXISTS subquery - MySQL")
        void existsSubqueryMySql() {
            SelectBuilder subquery = Suprim.select(TestOrder_.ID)
                    .from(TestOrder_.TABLE)
                    .where(TestOrder_.USER_ID.eq(TestUser_.ID));
            String sql = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .whereExists(subquery)
                    .build(MYSQL).sql();
            assertTrue(sql.toUpperCase().contains("EXISTS"), "Has EXISTS: " + sql);
        }
    }

    // ==================== 11. Row Locking Tests (8 tests) ====================

    @Nested
    @DisplayName("Row Locking")
    class RowLockingTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.SelectBuilderDialectTest#allDialects")
        @DisplayName("FOR UPDATE")
        void forUpdate(SqlDialect dialect) {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.ID.eq(1L))
                    .forUpdate()
                    .build(dialect).sql();
            assertTrue(sql.contains("FOR UPDATE"), "Should have FOR UPDATE: " + sql);
        }

        @Test
        @DisplayName("FOR UPDATE NOWAIT - PostgreSQL")
        void forUpdateNowaitPostgreSql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.ID.eq(1L))
                    .forUpdateNowait()
                    .build(POSTGRES).sql();
            assertTrue(sql.contains("FOR UPDATE NOWAIT"), "PostgreSQL NOWAIT: " + sql);
        }

        @Test
        @DisplayName("FOR UPDATE NOWAIT - MySQL 8")
        void forUpdateNowaitMySql8() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.ID.eq(1L))
                    .forUpdateNowait()
                    .build(MYSQL8).sql();
            assertTrue(sql.contains("FOR UPDATE NOWAIT"), "MySQL 8 NOWAIT: " + sql);
        }

        @Test
        @DisplayName("FOR UPDATE SKIP LOCKED - PostgreSQL")
        void forUpdateSkipLockedPostgreSql() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.ID.eq(1L))
                    .forUpdateSkipLocked()
                    .build(POSTGRES).sql();
            assertTrue(sql.contains("FOR UPDATE SKIP LOCKED"), "PostgreSQL SKIP LOCKED: " + sql);
        }

        @Test
        @DisplayName("FOR UPDATE SKIP LOCKED - MySQL 8")
        void forUpdateSkipLockedMySql8() {
            String sql = Suprim.select(TestUser_.ID).from(TestUser_.TABLE)
                    .where(TestUser_.ID.eq(1L))
                    .forUpdateSkipLocked()
                    .build(MYSQL8).sql();
            assertTrue(sql.contains("FOR UPDATE SKIP LOCKED"), "MySQL 8 SKIP LOCKED: " + sql);
        }
    }
}
