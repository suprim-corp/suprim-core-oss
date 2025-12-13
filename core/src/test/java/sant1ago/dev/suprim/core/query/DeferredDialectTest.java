package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.dialect.MariaDbDialect;
import sant1ago.dev.suprim.core.dialect.MySql8Dialect;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.ComparableColumn;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for deferred SQL generation with dialect support.
 * Tests SelectItem and Predicate types for PostgreSQL, MySQL 5.7, MySQL 8.0, and MariaDB.
 */
@DisplayName("Deferred Dialect SQL Generation")
class DeferredDialectTest {

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

    // ==================== Test Entities ====================

    @Entity(table = "users")
    static class User {}

    @Entity(table = "posts")
    static class Post {}

    @Entity(table = "orders")
    static class Order {}

    // ==================== Tables and Columns ====================

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final Table<Post> POSTS = Table.of("posts", Post.class);
    private static final Table<Order> ORDERS = Table.of("orders", Order.class);

    private static final ComparableColumn<User, Long> USER_ID =
            new ComparableColumn<>(USERS, "id", Long.class, "BIGINT");
    private static final Column<User, Boolean> USER_ACTIVE =
            new Column<>(USERS, "is_active", Boolean.class, "BOOLEAN");

    private static final ComparableColumn<Post, Long> POST_ID =
            new ComparableColumn<>(POSTS, "id", Long.class, "BIGINT");
    private static final Column<Post, Boolean> POST_PUBLISHED =
            new Column<>(POSTS, "published", Boolean.class, "BOOLEAN");

    private static final ComparableColumn<Order, Long> ORDER_ID =
            new ComparableColumn<>(ORDERS, "id", Long.class, "BIGINT");
    private static final ComparableColumn<Order, BigDecimal> ORDER_AMOUNT =
            new ComparableColumn<>(ORDERS, "amount", BigDecimal.class, "DECIMAL");

    // ==================== Relations ====================

    private static final Relation<User, Post> USER_POSTS = Relation.hasMany(
            USERS, POSTS, "id", "user_id", false, false
    );

    private static final Relation<User, Order> USER_ORDERS = Relation.hasMany(
            USERS, ORDERS, "id", "user_id", false, false
    );

    // BelongsToMany for pivot join testing
    private static final Relation<User, Order> USER_ORDERS_PIVOT = Relation.belongsToMany(
            USERS, ORDERS, "user_order", "user_id", "order_id",
            "id", "id", java.util.List.of(), false, false
    );

    // ==================== CountFilterItem Tests ====================

    @Nested
    @DisplayName("CountFilterItem")
    class CountFilterItemTests {

        @Test
        @DisplayName("PostgreSQL supports FILTER clause")
        void postgresSupportsFilter() {
            SelectItem item = SelectItem.countFilter(USER_ID, USER_ACTIVE.eq(true), "active_count");
            String sql = item.toSql(POSTGRES);

            assertTrue(sql.contains("COUNT("), "Should have COUNT: " + sql);
            assertTrue(sql.contains("FILTER (WHERE"), "Should have FILTER: " + sql);
            assertTrue(sql.contains("AS active_count"), "Should have alias: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#mysqlFamily")
        @DisplayName("MySQL family throws for FILTER")
        void mysqlFamilyThrowsForFilter(SqlDialect dialect) {
            SelectItem item = SelectItem.countFilter(USER_ID, USER_ACTIVE.eq(true), "active_count");
            assertThrows(UnsupportedDialectFeatureException.class, () -> item.toSql(dialect));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("CountFilter without filter works for all")
        void countWithoutFilterWorks(SqlDialect dialect) {
            SelectItem item = SelectItem.countFilter(USER_ID, null, "total_count");
            String sql = item.toSql(dialect);

            assertTrue(sql.contains("COUNT("), "Should have COUNT: " + sql);
            assertFalse(sql.contains("FILTER"), "Should NOT have FILTER: " + sql);
        }

        @Test
        @DisplayName("PostgreSQL FILTER with AND predicate")
        void postgresFilterWithAnd() {
            Predicate filter = USER_ACTIVE.eq(true).and(USER_ID.gt(100L));
            SelectItem item = SelectItem.countFilter(USER_ID, filter, "count");
            String sql = item.toSql(POSTGRES);

            assertTrue(sql.contains("FILTER"), "Should have FILTER: " + sql);
            assertTrue(sql.contains("AND"), "Should have AND: " + sql);
        }

        @Test
        @DisplayName("PostgreSQL FILTER with OR predicate")
        void postgresFilterWithOr() {
            Predicate filter = USER_ACTIVE.eq(true).or(USER_ID.lt(50L));
            SelectItem item = SelectItem.countFilter(USER_ID, filter, "count");
            String sql = item.toSql(POSTGRES);

            assertTrue(sql.contains("OR"), "Should have OR: " + sql);
        }
    }

    // ==================== SubqueryItem COUNT Tests ====================

    @Nested
    @DisplayName("SubqueryItem COUNT")
    class SubqueryCountTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT subquery structure")
        void countSubqueryStructure(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.COUNT, USER_POSTS, null, null, "posts_count", "users"
            );
            String sql = item.toSql(dialect);

            assertTrue(sql.contains("SELECT COUNT(*)"), "Should have COUNT: " + sql);
            assertTrue(sql.contains("FROM posts"), "Should have FROM: " + sql);
            assertTrue(sql.contains("AS posts_count"), "Should have alias: " + sql);
        }

        @Test
        @DisplayName("COUNT subquery identical for all dialects")
        void countSubqueryIdentical() {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.COUNT, USER_POSTS, null, null, "posts_count", "users"
            );

            String pgSql = item.toSql(POSTGRES);
            assertEquals(pgSql, item.toSql(MYSQL));
            assertEquals(pgSql, item.toSql(MYSQL8));
            assertEquals(pgSql, item.toSql(MARIADB));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT subquery with GT constraint")
        void countSubqueryWithGt(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.COUNT, USER_POSTS, null,
                    b -> b.where(POST_ID.gt(100L)), "recent_posts", "users"
            );
            assertTrue(item.toSql(dialect).contains("> 100"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT subquery with LTE constraint")
        void countSubqueryWithLte(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.COUNT, USER_POSTS, null,
                    b -> b.where(POST_ID.lte(50L)), "old_posts", "users"
            );
            assertTrue(item.toSql(dialect).contains("<= 50"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT subquery with EQ constraint")
        void countSubqueryWithEq(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.COUNT, USER_POSTS, null,
                    b -> b.where(POST_PUBLISHED.eq(true)), "published", "users"
            );
            assertTrue(item.toSql(dialect).contains("= TRUE"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT subquery with multiple constraints")
        void countSubqueryWithMultiple(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.COUNT, USER_POSTS, null,
                    b -> b.where(POST_ID.gt(10L)).and(POST_PUBLISHED.eq(true)), "count", "users"
            );
            String sql = item.toSql(dialect);
            assertTrue(sql.contains("> 10") && sql.contains("AND"));
        }
    }

    // ==================== SubqueryItem EXISTS Tests ====================

    @Nested
    @DisplayName("SubqueryItem EXISTS")
    class SubqueryExistsTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("EXISTS subquery structure")
        void existsSubqueryStructure(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.EXISTS, USER_POSTS, null, null, "has_posts", "users"
            );
            String sql = item.toSql(dialect);
            assertTrue(sql.contains("EXISTS(SELECT 1"));
            assertTrue(sql.contains("AS has_posts"));
        }

        @Test
        @DisplayName("EXISTS subquery identical for all dialects")
        void existsSubqueryIdentical() {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.EXISTS, USER_POSTS, null, null, "has_posts", "users"
            );
            String pgSql = item.toSql(POSTGRES);
            assertEquals(pgSql, item.toSql(MYSQL));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("EXISTS subquery with constraint")
        void existsSubqueryWithConstraint(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.EXISTS, USER_POSTS, null,
                    b -> b.where(POST_PUBLISHED.eq(true)), "has_published", "users"
            );
            assertTrue(item.toSql(dialect).contains("= TRUE"));
        }
    }

    // ==================== SubqueryItem Aggregates Tests ====================

    @Nested
    @DisplayName("SubqueryItem Aggregates")
    class SubqueryAggregateTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("SUM subquery structure")
        void sumSubquery(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.SUM, USER_ORDERS, ORDER_AMOUNT, null, "total", "users"
            );
            assertTrue(item.toSql(dialect).contains("SELECT SUM(amount)"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("MAX subquery structure")
        void maxSubquery(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.MAX, USER_ORDERS, ORDER_AMOUNT, null, "max_order", "users"
            );
            assertTrue(item.toSql(dialect).contains("SELECT MAX(amount)"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("MIN subquery structure")
        void minSubquery(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.MIN, USER_ORDERS, ORDER_AMOUNT, null, "min_order", "users"
            );
            assertTrue(item.toSql(dialect).contains("SELECT MIN(amount)"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("AVG subquery structure")
        void avgSubquery(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.AVG, USER_ORDERS, ORDER_AMOUNT, null, "avg_order", "users"
            );
            assertTrue(item.toSql(dialect).contains("SELECT AVG(amount)"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("SUM subquery with constraint")
        void sumSubqueryWithConstraint(SqlDialect dialect) {
            SelectItem item = SelectItem.subquery(
                    SelectItem.SubqueryType.SUM, USER_ORDERS, ORDER_AMOUNT,
                    b -> b.where(ORDER_AMOUNT.gt(new BigDecimal("100"))), "large_total", "users"
            );
            assertTrue(item.toSql(dialect).contains("> 100"));
        }
    }

    // ==================== RelationExistsPredicate Tests ====================

    @Nested
    @DisplayName("RelationExistsPredicate")
    class RelationExistsPredicateTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("EXISTS predicate structure")
        void existsPredicateStructure(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationExistsPredicate(USER_POSTS, null, false, "users");
            String sql = predicate.toSql(dialect);
            assertTrue(sql.startsWith("EXISTS (SELECT 1"));
            assertTrue(sql.contains("FROM posts"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("NOT EXISTS predicate structure")
        void notExistsPredicateStructure(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationExistsPredicate(USER_POSTS, null, true, "users");
            assertTrue(predicate.toSql(dialect).startsWith("NOT EXISTS (SELECT 1"));
        }

        @Test
        @DisplayName("EXISTS predicate identical for all dialects")
        void existsPredicateIdentical() {
            Predicate predicate = new Predicate.RelationExistsPredicate(USER_POSTS, null, false, "users");
            String pgSql = predicate.toSql(POSTGRES);
            assertEquals(pgSql, predicate.toSql(MYSQL));
            assertEquals(pgSql, predicate.toSql(MYSQL8));
            assertEquals(pgSql, predicate.toSql(MARIADB));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("EXISTS with GT constraint")
        void existsWithGt(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationExistsPredicate(
                    USER_POSTS, b -> b.where(POST_ID.gt(100L)), false, "users"
            );
            assertTrue(predicate.toSql(dialect).contains("> 100"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("EXISTS with LTE constraint")
        void existsWithLte(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationExistsPredicate(
                    USER_POSTS, b -> b.where(POST_ID.lte(50L)), false, "users"
            );
            assertTrue(predicate.toSql(dialect).contains("<= 50"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("EXISTS with AND constraint")
        void existsWithAnd(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationExistsPredicate(
                    USER_POSTS, b -> b.where(POST_ID.gt(10L)).and(POST_PUBLISHED.eq(true)), false, "users"
            );
            String sql = predicate.toSql(dialect);
            assertTrue(sql.contains("> 10") && sql.contains("AND"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("EXISTS with OR constraint")
        void existsWithOr(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationExistsPredicate(
                    USER_POSTS, b -> b.where(POST_ID.lt(10L).or(POST_ID.gt(100L))), false, "users"
            );
            assertTrue(predicate.toSql(dialect).contains("OR"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("EXISTS with BelongsToMany includes pivot JOIN")
        void existsWithPivotJoin(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationExistsPredicate(
                    USER_ORDERS_PIVOT, null, false, "users"
            );
            String sql = predicate.toSql(dialect);
            assertTrue(sql.contains("JOIN user_order"), "Should have pivot JOIN: " + sql);
            assertTrue(sql.contains("user_order.user_id = users.id"), "Should have pivot condition: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("EXISTS with constraint that adds no WHERE clause")
        void existsWithEmptyConstraint(SqlDialect dialect) {
            // Constraint that doesn't call where() - whereClause will be null
            Predicate predicate = new Predicate.RelationExistsPredicate(
                    USER_POSTS, b -> b, false, "users"
            );
            String sql = predicate.toSql(dialect);
            assertTrue(sql.contains("EXISTS (SELECT 1"));
            // Should not have extra AND since whereClause is null
            assertFalse(sql.contains(" AND "));
        }
    }

    // ==================== RelationCountPredicate Tests ====================

    @Nested
    @DisplayName("RelationCountPredicate")
    class RelationCountPredicateTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT predicate with = operator")
        void countPredicateEq(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationCountPredicate(USER_POSTS, "=", 5, null, "users");
            String sql = predicate.toSql(dialect);
            assertTrue(sql.contains("SELECT COUNT(*)") && sql.contains("= 5"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT predicate with <> operator")
        void countPredicateNe(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationCountPredicate(USER_POSTS, "<>", 0, null, "users");
            assertTrue(predicate.toSql(dialect).contains("<> 0"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT predicate with > operator")
        void countPredicateGt(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationCountPredicate(USER_POSTS, ">", 10, null, "users");
            assertTrue(predicate.toSql(dialect).contains("> 10"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT predicate with >= operator")
        void countPredicateGte(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationCountPredicate(USER_POSTS, ">=", 3, null, "users");
            assertTrue(predicate.toSql(dialect).contains(">= 3"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT predicate with < operator")
        void countPredicateLt(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationCountPredicate(USER_POSTS, "<", 100, null, "users");
            assertTrue(predicate.toSql(dialect).contains("< 100"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT predicate with <= operator")
        void countPredicateLte(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationCountPredicate(USER_POSTS, "<=", 50, null, "users");
            assertTrue(predicate.toSql(dialect).contains("<= 50"));
        }

        @Test
        @DisplayName("COUNT predicate identical for all dialects")
        void countPredicateIdentical() {
            Predicate predicate = new Predicate.RelationCountPredicate(USER_POSTS, ">=", 5, null, "users");
            String pgSql = predicate.toSql(POSTGRES);
            assertEquals(pgSql, predicate.toSql(MYSQL));
            assertEquals(pgSql, predicate.toSql(MYSQL8));
            assertEquals(pgSql, predicate.toSql(MARIADB));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT predicate with constraint")
        void countPredicateWithConstraint(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationCountPredicate(
                    USER_POSTS, "=", 0, b -> b.where(POST_PUBLISHED.eq(true)), "users"
            );
            String sql = predicate.toSql(dialect);
            assertTrue(sql.contains("= TRUE") && sql.contains("= 0"));
        }

        @Test
        @DisplayName("COUNT predicate with zero count")
        void countPredicateZero() {
            Predicate predicate = new Predicate.RelationCountPredicate(USER_POSTS, "=", 0, null, "users");
            assertTrue(predicate.toSql(POSTGRES).contains("= 0"));
        }

        @Test
        @DisplayName("COUNT predicate with large count")
        void countPredicateLarge() {
            Predicate predicate = new Predicate.RelationCountPredicate(USER_POSTS, ">=", 1000000, null, "users");
            assertTrue(predicate.toSql(POSTGRES).contains(">= 1000000"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT with BelongsToMany includes pivot JOIN")
        void countWithPivotJoin(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationCountPredicate(
                    USER_ORDERS_PIVOT, ">=", 1, null, "users"
            );
            String sql = predicate.toSql(dialect);
            assertTrue(sql.contains("JOIN user_order"), "Should have pivot JOIN: " + sql);
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("COUNT with constraint that adds no WHERE clause")
        void countWithEmptyConstraint(SqlDialect dialect) {
            Predicate predicate = new Predicate.RelationCountPredicate(
                    USER_POSTS, "=", 5, b -> b, "users"
            );
            String sql = predicate.toSql(dialect);
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertFalse(sql.contains(" AND "));
        }
    }

    // ==================== Full Query Integration Tests ====================

    @Nested
    @DisplayName("Full Query Integration")
    class FullQueryIntegrationTests {

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("whereHas generates correct SQL")
        void whereHas(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).whereHas(USER_POSTS);
            assertTrue(builder.build(dialect).sql().contains("EXISTS (SELECT 1 FROM posts"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("whereDoesntHave generates correct SQL")
        void whereDoesntHave(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).whereDoesntHave(USER_POSTS);
            assertTrue(builder.build(dialect).sql().contains("NOT EXISTS (SELECT 1 FROM posts"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("whereHas with constraint")
        void whereHasWithConstraint(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).whereHas(USER_POSTS, posts -> posts.where(POST_ID.gt(100L)));
            assertTrue(builder.build(dialect).sql().contains("> 100"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("has() generates correct SQL")
        void has(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).has(USER_POSTS, ">=", 3);
            String sql = builder.build(dialect).sql();
            assertTrue(sql.contains("(SELECT COUNT(*)") && sql.contains(">= 3"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("withCount generates correct SQL")
        void withCount(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).withCount(USER_POSTS);
            String sql = builder.build(dialect).sql();
            assertTrue(sql.contains("(SELECT COUNT(*)") && sql.contains("AS posts_count"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("withCount with constraint")
        void withCountWithConstraint(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).withCount(USER_POSTS, posts -> posts.where(POST_PUBLISHED.eq(true)), "published_count");
            assertTrue(builder.build(dialect).sql().contains("= TRUE"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("withExists generates correct SQL")
        void withExists(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).withExists(USER_POSTS, "has_posts");
            String sql = builder.build(dialect).sql();
            assertTrue(sql.contains("EXISTS(SELECT 1") && sql.contains("AS has_posts"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("withSum generates correct SQL")
        void withSum(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).withSum(USER_ORDERS, ORDER_AMOUNT, "total_amount");
            assertTrue(builder.build(dialect).sql().contains("SELECT SUM(amount)"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("withMax generates correct SQL")
        void withMax(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).withMax(USER_ORDERS, ORDER_AMOUNT, "max_amount");
            assertTrue(builder.build(dialect).sql().contains("SELECT MAX(amount)"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("withMin generates correct SQL")
        void withMin(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).withMin(USER_ORDERS, ORDER_AMOUNT, "min_amount");
            assertTrue(builder.build(dialect).sql().contains("SELECT MIN(amount)"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("withAvg generates correct SQL")
        void withAvg(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS).withAvg(USER_ORDERS, ORDER_AMOUNT, "avg_amount");
            assertTrue(builder.build(dialect).sql().contains("SELECT AVG(amount)"));
        }

        @ParameterizedTest
        @MethodSource("sant1ago.dev.suprim.core.query.DeferredDialectTest#allDialects")
        @DisplayName("Complex query with multiple deferred types")
        void complexQuery(SqlDialect dialect) {
            SelectBuilder builder = new SelectBuilder(List.of());
            builder.from(USERS)
                    .withCount(USER_POSTS)
                    .withExists(USER_ORDERS, "has_orders")
                    .whereHas(USER_POSTS, posts -> posts.where(POST_PUBLISHED.eq(true)))
                    .has(USER_ORDERS, ">=", 1);
            String sql = builder.build(dialect).sql();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("EXISTS(SELECT 1"));
            assertTrue(sql.contains("EXISTS (SELECT 1"));
        }
    }
}
