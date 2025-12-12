package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import static java.util.Objects.nonNull;

/**
 * SQL aggregate functions with type-safe filter support.
 *
 * <pre>{@code
 * // COUNT(*) AS total
 * Aggregate.count().as("total")
 *
 * // COUNT(column) AS rated
 * Aggregate.count(Message_.RATING).as("rated")
 *
 * // COUNT(*) FILTER (WHERE rating = 1) AS likes
 * Aggregate.count().filter(Message_.RATING.eq(1)).as("likes")
 *
 * // SUM(amount) FILTER (WHERE status = 'completed') AS total_revenue
 * Aggregate.sum(Order_.AMOUNT).filter(Order_.STATUS.eq("completed")).as("total_revenue")
 *
 * // AVG(rating) AS avg_rating
 * Aggregate.avg(Product_.RATING).as("avg_rating")
 * }</pre>
 */
public final class Aggregate implements Expression<Number> {

    private final AggregateType type;
    private final Expression<?> column;
    private Predicate filterPredicate;
    private String alias;

    private Aggregate(AggregateType type, Expression<?> column) {
        this.type = type;
        this.column = column;
    }

    // ==================== FACTORY METHODS ====================

    /**
     * COUNT(*) - count all rows.
     *
     * @return aggregate for COUNT(*)
     */
    public static Aggregate count() {
        return new Aggregate(AggregateType.COUNT, null);
    }

    /**
     * COUNT(column) - count non-null values.
     *
     * @param column the column to count
     * @return aggregate for COUNT(column)
     */
    public static Aggregate count(Expression<?> column) {
        return new Aggregate(AggregateType.COUNT, column);
    }

    /**
     * SUM(column) - sum of values.
     *
     * @param column the column to sum
     * @return aggregate for SUM(column)
     */
    public static Aggregate sum(Expression<?> column) {
        return new Aggregate(AggregateType.SUM, column);
    }

    /**
     * AVG(column) - average of values.
     *
     * @param column the column to average
     * @return aggregate for AVG(column)
     */
    public static Aggregate avg(Expression<?> column) {
        return new Aggregate(AggregateType.AVG, column);
    }

    /**
     * MIN(column) - minimum value.
     *
     * @param column the column to find minimum
     * @return aggregate for MIN(column)
     */
    public static Aggregate min(Expression<?> column) {
        return new Aggregate(AggregateType.MIN, column);
    }

    /**
     * MAX(column) - maximum value.
     *
     * @param column the column to find maximum
     * @return aggregate for MAX(column)
     */
    public static Aggregate max(Expression<?> column) {
        return new Aggregate(AggregateType.MAX, column);
    }

    // ==================== MODIFIERS ====================

    /**
     * Add FILTER clause to aggregate (PostgreSQL).
     * <pre>{@code
     * Aggregate.count().filter(Message_.RATING.eq(1)).as("likes")
     * // COUNT(*) FILTER (WHERE "rating" = 1) AS likes
     * }</pre>
     *
     * @param predicate the filter condition
     * @return this aggregate for chaining
     */
    public Aggregate filter(Predicate predicate) {
        this.filterPredicate = predicate;
        return this;
    }

    /**
     * Add alias to aggregate expression.
     *
     * @param alias the column alias
     * @return this aggregate for chaining
     */
    public Aggregate as(String alias) {
        this.alias = alias;
        return this;
    }

    // ==================== COMPARISON OPERATORS ====================

    /**
     * Equals: aggregate = value (for HAVING clause)
     * <pre>{@code
     * .having(Aggregate.count().eq(5))
     * // HAVING COUNT(*) = 5
     * }</pre>
     *
     * @param value the value to compare against
     * @return predicate for the comparison
     */
    public Predicate eq(Number value) {
        return new Predicate.SimplePredicate(this, Operator.EQUALS, new Literal<>(value, Number.class));
    }

    /**
     * Greater than: aggregate &gt; value (for HAVING clause)
     * <pre>{@code
     * .having(Aggregate.count().gt(10))
     * // HAVING COUNT(*) > 10
     * }</pre>
     *
     * @param value the value to compare against
     * @return predicate for the comparison
     */
    public Predicate gt(Number value) {
        return new Predicate.SimplePredicate(this, Operator.GREATER_THAN, new Literal<>(value, Number.class));
    }

    /**
     * Greater than or equals: aggregate &gt;= value (for HAVING clause)
     * <pre>{@code
     * .having(Aggregate.sum(Order_.AMOUNT).gte(1000))
     * // HAVING SUM("amount") >= 1000
     * }</pre>
     *
     * @param value the value to compare against
     * @return predicate for the comparison
     */
    public Predicate gte(Number value) {
        return new Predicate.SimplePredicate(this, Operator.GREATER_THAN_OR_EQUALS, new Literal<>(value, Number.class));
    }

    /**
     * Less than: aggregate &lt; value (for HAVING clause)
     * <pre>{@code
     * .having(Aggregate.avg(Product_.PRICE).lt(100))
     * // HAVING AVG("price") &lt; 100
     * }</pre>
     *
     * @param value the value to compare against
     * @return predicate for the comparison
     */
    public Predicate lt(Number value) {
        return new Predicate.SimplePredicate(this, Operator.LESS_THAN, new Literal<>(value, Number.class));
    }

    /**
     * Less than or equals: aggregate &lt;= value (for HAVING clause)
     * <pre>{@code
     * .having(Aggregate.max(Order_.TOTAL).lte(500))
     * // HAVING MAX("total") &lt;= 500
     * }</pre>
     *
     * @param value the value to compare against
     * @return predicate for the comparison
     */
    public Predicate lte(Number value) {
        return new Predicate.SimplePredicate(this, Operator.LESS_THAN_OR_EQUALS, new Literal<>(value, Number.class));
    }

    // ==================== ORDERING ====================

    /**
     * Ascending order for ORDER BY clause.
     * <pre>{@code
     * .orderBy(Aggregate.count().as("cnt").asc())
     * // ORDER BY COUNT(*) AS cnt ASC
     * }</pre>
     *
     * @return order specification for ascending sort
     */
    public OrderSpec asc() {
        return OrderSpec.raw(this.toSql(sant1ago.dev.suprim.core.dialect.PostgreSqlDialect.INSTANCE) + " ASC");
    }

    /**
     * Descending order for ORDER BY clause.
     * <pre>{@code
     * .orderBy(Aggregate.count().as("cnt").desc())
     * // ORDER BY COUNT(*) AS cnt DESC
     * }</pre>
     *
     * @return order specification for descending sort
     */
    public OrderSpec desc() {
        return OrderSpec.raw(this.toSql(sant1ago.dev.suprim.core.dialect.PostgreSqlDialect.INSTANCE) + " DESC");
    }

    // ==================== EXPRESSION ====================

    @Override
    public Class<Number> getValueType() {
        return Number.class;
    }

    @Override
    public String toSql(SqlDialect dialect) {
        StringBuilder sb = new StringBuilder();

        // Function name
        sb.append(type.getSql()).append("(");

        // Column or *
        if (nonNull(column)) {
            sb.append(column.toSql(dialect));
        } else {
            sb.append("*");
        }

        sb.append(")");

        // FILTER clause (PostgreSQL)
        if (nonNull(filterPredicate)) {
            sb.append(" FILTER (WHERE ").append(filterPredicate.toSql(dialect)).append(")");
        }

        // Alias
        if (nonNull(alias)) {
            sb.append(" AS ").append(alias);
        }

        return sb.toString();
    }

    // ==================== AGGREGATE TYPES ====================

    private enum AggregateType {
        COUNT("COUNT"),
        SUM("SUM"),
        AVG("AVG"),
        MIN("MIN"),
        MAX("MAX");

        private final String sql;

        AggregateType(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }
}
