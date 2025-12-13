package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Type-safe column reference.
 *
 * @param <T> Table entity type
 * @param <V> Column value type (String, Long, LocalDateTime, etc.)
 */
public non-sealed class Column<T, V> implements Expression<V> {
    private final Table<T> table;
    private final String name;
    private final Class<V> valueType;
    private final String sqlType;

    /**
     * Constructs a new Column.
     *
     * @param table the table this column belongs to
     * @param name the column name
     * @param valueType the Java type of column values
     * @param sqlType the SQL type (can be null for computed columns)
     */
    public Column(Table<T> table, String name, Class<V> valueType, String sqlType) {
        this.table = Objects.requireNonNull(table, "table cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.valueType = Objects.requireNonNull(valueType, "valueType cannot be null");
        this.sqlType = sqlType; // sqlType can be null for computed columns
    }

    /**
     * Create a dynamic column reference (for internal use).
     *
     * @param name column name
     * @param table table reference
     * @return column reference
     */
    public static <T> Column<T, Object> of(String name, Table<T> table) {
        return new Column<>(table, name, Object.class, null);
    }

    // ==================== EQUALITY OPERATORS ====================

    /**
     * Equals: column = value.
     *
     * @param value the value to compare
     * @return predicate for the equality check
     */
    public Predicate eq(V value) {
        return new Predicate.SimplePredicate(this, Operator.EQUALS, new Literal<>(value, valueType));
    }

    /**
     * Equals another column: column1 = column2 (for JOIN conditions).
     *
     * @param other the column to compare
     * @return predicate for the equality check
     */
    public Predicate eq(Column<?, V> other) {
        return new Predicate.SimplePredicate(this, Operator.EQUALS, other);
    }

    /**
     * Not equals: column != value.
     *
     * @param value the value to compare
     * @return predicate for the inequality check
     */
    public Predicate ne(V value) {
        return new Predicate.SimplePredicate(this, Operator.NOT_EQUALS, new Literal<>(value, valueType));
    }

    /**
     * Not equals another column: column1 != column2.
     *
     * @param other the column to compare
     * @return predicate for the inequality check
     */
    public Predicate ne(Column<?, V> other) {
        return new Predicate.SimplePredicate(this, Operator.NOT_EQUALS, other);
    }

    /**
     * Is null: column IS NULL.
     *
     * @return predicate for null check
     */
    public Predicate isNull() {
        return new Predicate.SimplePredicate(this, Operator.IS_NULL, null);
    }

    /**
     * Is not null: column IS NOT NULL.
     *
     * @return predicate for not null check
     */
    public Predicate isNotNull() {
        return new Predicate.SimplePredicate(this, Operator.IS_NOT_NULL, null);
    }

    // ==================== COMPARISON OPERATORS ====================

    /**
     * Greater than: column &gt; value.
     *
     * @param value the value to compare
     * @return predicate for the comparison
     */
    public Predicate gt(V value) {
        return new Predicate.SimplePredicate(this, Operator.GREATER_THAN, new Literal<>(value, valueType));
    }

    /**
     * Greater than or equals: column &gt;= value.
     *
     * @param value the value to compare
     * @return predicate for the comparison
     */
    public Predicate gte(V value) {
        return new Predicate.SimplePredicate(this, Operator.GREATER_THAN_OR_EQUALS, new Literal<>(value, valueType));
    }

    /**
     * Less than: column &lt; value.
     *
     * @param value the value to compare
     * @return predicate for the comparison
     */
    public Predicate lt(V value) {
        return new Predicate.SimplePredicate(this, Operator.LESS_THAN, new Literal<>(value, valueType));
    }

    /**
     * Less than or equals: column &lt;= value.
     *
     * @param value the value to compare
     * @return predicate for the comparison
     */
    public Predicate lte(V value) {
        return new Predicate.SimplePredicate(this, Operator.LESS_THAN_OR_EQUALS, new Literal<>(value, valueType));
    }

    // ==================== IN OPERATORS ====================

    /**
     * In list: column IN (values).
     *
     * @param values the values to check
     * @return predicate for the IN check
     */
    @SafeVarargs
    public final Predicate in(V... values) {
        return new Predicate.SimplePredicate(this, Operator.IN, new ListLiteral<>(Arrays.asList(values), valueType));
    }

    /**
     * In list: column IN (values).
     *
     * @param values the list of values to check
     * @return predicate for the IN check
     */
    public Predicate in(List<V> values) {
        return new Predicate.SimplePredicate(this, Operator.IN, new ListLiteral<>(values, valueType));
    }

    /**
     * Not in list: column NOT IN (values).
     *
     * @param values the values to check
     * @return predicate for the NOT IN check
     */
    @SafeVarargs
    public final Predicate notIn(V... values) {
        return new Predicate.SimplePredicate(this, Operator.NOT_IN, new ListLiteral<>(Arrays.asList(values), valueType));
    }

    // ==================== ORDERING ====================

    /**
     * Ascending order for ORDER BY clause.
     *
     * @return order specification for ascending sort
     */
    public OrderSpec asc() {
        return new OrderSpec(this, OrderDirection.ASC);
    }

    /**
     * Descending order for ORDER BY clause.
     *
     * @return order specification for descending sort
     */
    public OrderSpec desc() {
        return new OrderSpec(this, OrderDirection.DESC);
    }

    // ==================== ALIAS ====================

    /**
     * Create aliased column reference for SELECT clause.
     * <pre>{@code
     * Suprim.select(User_.EMAIL.as("user_email"), User_.NAME.as("display_name"))
     * }</pre>
     *
     * @param alias the alias name
     * @return aliased column
     */
    public AliasedColumn<T, V> as(String alias) {
        return new AliasedColumn<>(this, alias);
    }

    // ==================== EXPRESSION INTERFACE ====================

    @Override
    public Class<V> getValueType() {
        return valueType;
    }

    @Override
    public String toSql(SqlDialect dialect) {
        return table.getSqlReference() + "." + dialect.quoteIdentifier(name);
    }

    // ==================== GETTERS ====================

    /**
     * Gets the table this column belongs to.
     *
     * @return the table instance
     */
    public Table<T> getTable() {
        return table;
    }

    /**
     * Gets the column name.
     *
     * @return the column name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the SQL type of this column.
     *
     * @return the SQL type, or null for computed columns
     */
    public String getSqlType() {
        return sqlType;
    }
}
