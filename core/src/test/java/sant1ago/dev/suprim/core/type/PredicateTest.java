package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Predicate types and composition.
 */
@DisplayName("Predicate Tests")
class PredicateTest {

    // ==================== SIMPLE PREDICATE ====================

    @Test
    @DisplayName("SimplePredicate with EQUALS")
    void testSimplePredicateEquals() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.EMAIL,
            Operator.EQUALS,
            new Literal<>("test@example.com", String.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"email\" = 'test@example.com'"));
    }

    @Test
    @DisplayName("SimplePredicate with NOT_EQUALS")
    void testSimplePredicateNotEquals() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.NOT_EQUALS,
            new Literal<>(18, Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" != 18"));
    }

    @Test
    @DisplayName("SimplePredicate with GREATER_THAN")
    void testSimplePredicateGreaterThan() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.GREATER_THAN,
            new Literal<>(21, Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" > 21"));
    }

    @Test
    @DisplayName("SimplePredicate with IS_NULL")
    void testSimplePredicateIsNull() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.NAME,
            Operator.IS_NULL,
            null
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"name\" IS NULL"));
    }

    @Test
    @DisplayName("SimplePredicate with IS_NOT_NULL")
    void testSimplePredicateIsNotNull() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.EMAIL,
            Operator.IS_NOT_NULL,
            null
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"email\" IS NOT NULL"));
    }

    @Test
    @DisplayName("SimplePredicate with IN")
    void testSimplePredicateIn() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.IN,
            new ListLiteral<>(java.util.List.of(18, 21, 25), Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" IN (18, 21, 25)"));
    }

    @Test
    @DisplayName("SimplePredicate with NOT_IN")
    void testSimplePredicateNotIn() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.NOT_IN,
            new ListLiteral<>(java.util.List.of(1, 2, 3), Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" NOT IN (1, 2, 3)"));
    }

    @Test
    @DisplayName("SimplePredicate with BETWEEN")
    void testSimplePredicateBetween() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.BETWEEN,
            new ListLiteral<>(java.util.List.of(18, 65), Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" BETWEEN 18 AND 65"));
    }

    @Test
    @DisplayName("SimplePredicate with LIKE")
    void testSimplePredicateLike() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.EMAIL,
            Operator.LIKE,
            new Literal<>("%@example.com", String.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"email\" LIKE '%@example.com'"));
    }

    // ==================== COMPOSITE PREDICATE ====================

    @Test
    @DisplayName("CompositePredicate with AND")
    void testCompositePredicateAnd() {
        Predicate left = TestUser_.AGE.gte(18);
        Predicate right = TestUser_.IS_ACTIVE.eq(true);
        Predicate composite = new Predicate.CompositePredicate(left, LogicalOperator.AND, right);

        String sql = composite.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("users.\"age\" >= 18"));
        assertTrue(sql.contains("users.\"is_active\""));
    }

    @Test
    @DisplayName("CompositePredicate with OR")
    void testCompositePredicateOr() {
        Predicate left = TestUser_.AGE.lt(18);
        Predicate right = TestUser_.AGE.gt(65);
        Predicate composite = new Predicate.CompositePredicate(left, LogicalOperator.OR, right);

        String sql = composite.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("OR"));
        assertTrue(sql.contains("users.\"age\" < 18"));
        assertTrue(sql.contains("users.\"age\" > 65"));
    }

    @Test
    @DisplayName("Nested CompositePredicate")
    void testNestedCompositePredicate() {
        Predicate innerLeft = TestUser_.AGE.gte(18);
        Predicate innerRight = TestUser_.IS_ACTIVE.eq(true);
        Predicate inner = new Predicate.CompositePredicate(innerLeft, LogicalOperator.AND, innerRight);

        Predicate outer = new Predicate.CompositePredicate(
            inner,
            LogicalOperator.OR,
            TestUser_.EMAIL.like("%@admin.com")
        );

        String sql = outer.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
        assertTrue(sql.contains("("));
        assertTrue(sql.contains(")"));
    }

    // ==================== NOT PREDICATE ====================

    @Test
    @DisplayName("NotPredicate negates simple predicate")
    void testNotPredicate() {
        Predicate inner = TestUser_.IS_ACTIVE.eq(true);
        Predicate not = new Predicate.NotPredicate(inner);

        String sql = not.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("NOT ("));
        assertTrue(sql.contains("users.\"is_active\""));
    }

    @Test
    @DisplayName("NotPredicate negates composite predicate")
    void testNotPredicateComposite() {
        Predicate inner = TestUser_.AGE.gte(18).and(TestUser_.IS_ACTIVE.eq(true));
        Predicate not = new Predicate.NotPredicate(inner);

        String sql = not.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("NOT ("));
        assertTrue(sql.contains("AND"));
    }

    // ==================== RAW PREDICATE ====================

    @Test
    @DisplayName("RawPredicate renders raw SQL")
    void testRawPredicate() {
        Predicate raw = new Predicate.RawPredicate("created_at > NOW() - INTERVAL '7 days'");

        String sql = raw.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("created_at > NOW() - INTERVAL '7 days'", sql);
    }

    @Test
    @DisplayName("RawPredicate with complex SQL")
    void testRawPredicateComplex() {
        Predicate raw = new Predicate.RawPredicate(
            "EXTRACT(YEAR FROM created_at) = 2024 AND status IN ('active', 'pending')"
        );

        String sql = raw.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("EXTRACT"));
        assertTrue(sql.contains("status IN"));
    }

    // ==================== PREDICATE COMPOSITION METHODS ====================

    @Test
    @DisplayName("Predicate.and() composition")
    void testPredicateAndComposition() {
        Predicate p1 = TestUser_.AGE.gte(18);
        Predicate p2 = TestUser_.IS_ACTIVE.eq(true);
        Predicate composed = p1.and(p2);

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("users.\"age\" >= 18"));
        assertTrue(sql.contains("users.\"is_active\""));
    }

    @Test
    @DisplayName("Predicate.or() composition")
    void testPredicateOrComposition() {
        Predicate p1 = TestUser_.AGE.lt(18);
        Predicate p2 = TestUser_.AGE.gt(65);
        Predicate composed = p1.or(p2);

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("OR"));
    }

    @Test
    @DisplayName("Predicate.not() negation")
    void testPredicateNotNegation() {
        Predicate p = TestUser_.IS_ACTIVE.eq(true);
        Predicate negated = p.not();

        String sql = negated.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("NOT ("));
    }

    @Test
    @DisplayName("Chained predicate composition")
    void testChainedPredicateComposition() {
        Predicate composed = TestUser_.AGE.gte(18)
            .and(TestUser_.IS_ACTIVE.eq(true))
            .and(TestUser_.EMAIL.isNotNull());

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        // Should contain multiple AND operators
        int andCount = sql.split("AND").length - 1;
        assertTrue(andCount >= 2);
    }

    @Test
    @DisplayName("Complex predicate with mixed AND/OR")
    void testComplexPredicateMixedLogic() {
        Predicate composed = TestUser_.AGE.gte(18)
            .and(TestUser_.IS_ACTIVE.eq(true))
            .or(TestUser_.EMAIL.like("%@admin.com"));

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Empty RawPredicate")
    void testEmptyRawPredicate() {
        Predicate raw = new Predicate.RawPredicate("");
        String sql = raw.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("", sql);
    }

    @Test
    @DisplayName("Double negation")
    void testDoubleNegation() {
        Predicate p = TestUser_.IS_ACTIVE.eq(true);
        Predicate notNot = p.not().not();

        String sql = notNot.toSql(PostgreSqlDialect.INSTANCE);
        // Should have two NOT wrappers
        assertTrue(sql.contains("NOT (NOT ("));
    }

    @Test
    @DisplayName("Predicate with parentheses grouping")
    void testPredicateParenthesesGrouping() {
        // (age >= 18 AND is_active = true) OR (age >= 65)
        Predicate group1 = TestUser_.AGE.gte(18).and(TestUser_.IS_ACTIVE.eq(true));
        Predicate group2 = TestUser_.AGE.gte(65);
        Predicate composed = group1.or(group2);

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        // Check that parentheses are present for grouping
        assertTrue(sql.contains("("));
        assertTrue(sql.contains(")"));
    }
}
