package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestOrder_;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Aggregate functions.
 */
@DisplayName("Aggregate Tests")
class AggregateTest {

    // ==================== COUNT ====================

    @Test
    @DisplayName("COUNT(*) aggregate")
    void testCountStar() {
        Aggregate agg = Aggregate.count();
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("COUNT(*)", sql);
    }

    @Test
    @DisplayName("COUNT(column) aggregate")
    void testCountColumn() {
        Aggregate agg = Aggregate.count(TestUser_.EMAIL);
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(users.\"email\")"));
    }

    @Test
    @DisplayName("COUNT(*) with alias")
    void testCountWithAlias() {
        Aggregate agg = Aggregate.count().as("total");
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("AS total"));
    }

    @Test
    @DisplayName("COUNT with FILTER clause")
    void testCountWithFilter() {
        Aggregate agg = Aggregate.count()
            .filter(TestUser_.IS_ACTIVE.eq(true))
            .as("active_count");

        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("FILTER (WHERE"));
        assertTrue(sql.contains("users.\"is_active\""));
        assertTrue(sql.contains("AS active_count"));
    }

    // ==================== SUM ====================

    @Test
    @DisplayName("SUM aggregate")
    void testSum() {
        Aggregate agg = Aggregate.sum(TestOrder_.AMOUNT);
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("SUM(orders.\"amount\")"));
    }

    @Test
    @DisplayName("SUM with alias")
    void testSumWithAlias() {
        Aggregate agg = Aggregate.sum(TestOrder_.AMOUNT).as("total_amount");
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("SUM(orders.\"amount\")"));
        assertTrue(sql.contains("AS total_amount"));
    }

    @Test
    @DisplayName("SUM with FILTER clause")
    void testSumWithFilter() {
        Aggregate agg = Aggregate.sum(TestOrder_.AMOUNT)
            .filter(TestOrder_.STATUS.eq("completed"))
            .as("completed_total");

        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("SUM(orders.\"amount\")"));
        assertTrue(sql.contains("FILTER (WHERE"));
        assertTrue(sql.contains("AS completed_total"));
    }

    // ==================== AVG ====================

    @Test
    @DisplayName("AVG aggregate")
    void testAvg() {
        Aggregate agg = Aggregate.avg(TestUser_.AGE);
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("AVG(users.\"age\")"));
    }

    @Test
    @DisplayName("AVG with alias")
    void testAvgWithAlias() {
        Aggregate agg = Aggregate.avg(TestUser_.AGE).as("avg_age");
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("AVG(users.\"age\")"));
        assertTrue(sql.contains("AS avg_age"));
    }

    @Test
    @DisplayName("AVG with FILTER clause")
    void testAvgWithFilter() {
        Aggregate agg = Aggregate.avg(TestUser_.AGE)
            .filter(TestUser_.IS_ACTIVE.eq(true))
            .as("active_avg_age");

        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("AVG(users.\"age\")"));
        assertTrue(sql.contains("FILTER (WHERE"));
        assertTrue(sql.contains("AS active_avg_age"));
    }

    // ==================== MIN ====================

    @Test
    @DisplayName("MIN aggregate")
    void testMin() {
        Aggregate agg = Aggregate.min(TestUser_.AGE);
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("MIN(users.\"age\")"));
    }

    @Test
    @DisplayName("MIN with alias")
    void testMinWithAlias() {
        Aggregate agg = Aggregate.min(TestUser_.AGE).as("min_age");
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("MIN(users.\"age\")"));
        assertTrue(sql.contains("AS min_age"));
    }

    @Test
    @DisplayName("MIN with FILTER clause")
    void testMinWithFilter() {
        Aggregate agg = Aggregate.min(TestOrder_.AMOUNT)
            .filter(TestOrder_.STATUS.eq("completed"))
            .as("min_completed");

        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("MIN(orders.\"amount\")"));
        assertTrue(sql.contains("FILTER (WHERE"));
        assertTrue(sql.contains("AS min_completed"));
    }

    // ==================== MAX ====================

    @Test
    @DisplayName("MAX aggregate")
    void testMax() {
        Aggregate agg = Aggregate.max(TestUser_.AGE);
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("MAX(users.\"age\")"));
    }

    @Test
    @DisplayName("MAX with alias")
    void testMaxWithAlias() {
        Aggregate agg = Aggregate.max(TestUser_.AGE).as("max_age");
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("MAX(users.\"age\")"));
        assertTrue(sql.contains("AS max_age"));
    }

    @Test
    @DisplayName("MAX with FILTER clause")
    void testMaxWithFilter() {
        Aggregate agg = Aggregate.max(TestOrder_.AMOUNT)
            .filter(TestOrder_.STATUS.eq("completed"))
            .as("max_completed");

        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("MAX(orders.\"amount\")"));
        assertTrue(sql.contains("FILTER (WHERE"));
        assertTrue(sql.contains("AS max_completed"));
    }

    // ==================== COMPARISON OPERATORS (for HAVING) ====================

    @Test
    @DisplayName("Aggregate eq() for HAVING")
    void testAggregateEquals() {
        Aggregate agg = Aggregate.count();
        Predicate predicate = agg.eq(5);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(*) = 5"));
    }

    @Test
    @DisplayName("Aggregate gt() for HAVING")
    void testAggregateGreaterThan() {
        Aggregate agg = Aggregate.count();
        Predicate predicate = agg.gt(10);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(*) > 10"));
    }

    @Test
    @DisplayName("Aggregate gte() for HAVING")
    void testAggregateGreaterThanOrEquals() {
        Aggregate agg = Aggregate.sum(TestOrder_.AMOUNT);
        Predicate predicate = agg.gte(1000);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("SUM(orders.\"amount\") >= 1000"));
    }

    @Test
    @DisplayName("Aggregate lt() for HAVING")
    void testAggregateLessThan() {
        Aggregate agg = Aggregate.avg(TestUser_.AGE);
        Predicate predicate = agg.lt(50);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("AVG(users.\"age\") < 50"));
    }

    @Test
    @DisplayName("Aggregate lte() for HAVING")
    void testAggregateLessThanOrEquals() {
        Aggregate agg = Aggregate.max(TestOrder_.AMOUNT);
        Predicate predicate = agg.lte(500);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("MAX(orders.\"amount\") <= 500"));
    }

    // ==================== ORDERING ====================

    @Test
    @DisplayName("Aggregate asc() for ORDER BY")
    void testAggregateAsc() {
        Aggregate agg = Aggregate.count().as("cnt");
        OrderSpec orderSpec = agg.asc();
        String sql = orderSpec.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("ASC"));
    }

    @Test
    @DisplayName("Aggregate desc() for ORDER BY")
    void testAggregateDesc() {
        Aggregate agg = Aggregate.count().as("cnt");
        OrderSpec orderSpec = agg.desc();
        String sql = orderSpec.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("DESC"));
    }

    // ==================== COMPLEX FILTERS ====================

    @Test
    @DisplayName("Aggregate with complex filter")
    void testAggregateWithComplexFilter() {
        Aggregate agg = Aggregate.count()
            .filter(TestUser_.AGE.gte(18).and(TestUser_.IS_ACTIVE.eq(true)))
            .as("adult_active_count");

        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("FILTER (WHERE"));
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("AS adult_active_count"));
    }

    @Test
    @DisplayName("Aggregate with OR filter")
    void testAggregateWithOrFilter() {
        Aggregate agg = Aggregate.sum(TestOrder_.AMOUNT)
            .filter(TestOrder_.STATUS.eq("completed").or(TestOrder_.STATUS.eq("pending")))
            .as("completed_or_pending_total");

        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("SUM(orders.\"amount\")"));
        assertTrue(sql.contains("FILTER (WHERE"));
        assertTrue(sql.contains("OR"));
    }

    // ==================== VALUE TYPE ====================

    @Test
    @DisplayName("Aggregate getValueType() returns Number")
    void testAggregateValueType() {
        Aggregate agg = Aggregate.count();
        assertEquals(Number.class, agg.getValueType());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Multiple chained operations")
    void testMultipleChainedOperations() {
        Aggregate agg = Aggregate.count(TestUser_.EMAIL)
            .filter(TestUser_.IS_ACTIVE.eq(true))
            .as("active_emails");

        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(users.\"email\")"));
        assertTrue(sql.contains("FILTER (WHERE"));
        assertTrue(sql.contains("AS active_emails"));
    }

    @Test
    @DisplayName("Aggregate without alias")
    void testAggregateWithoutAlias() {
        Aggregate agg = Aggregate.count();
        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertEquals("COUNT(*)", sql);
        assertFalse(sql.contains("AS"));
    }

    @Test
    @DisplayName("Aggregate filter without alias")
    void testAggregateFilterWithoutAlias() {
        Aggregate agg = Aggregate.count()
            .filter(TestUser_.IS_ACTIVE.eq(true));

        String sql = agg.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("FILTER (WHERE"));
        assertFalse(sql.contains("AS"));
    }
}
