package sant1ago.dev.suprim.core.type;

import java.util.Arrays;

/**
 * Column with comparison operators ({@code >}, {@code <}, {@code >=}, {@code <=}).
 * Used for numeric and temporal types.
 *
 * @param <T> Table entity type
 * @param <V> Column value type (must be Comparable)
 */
public final class ComparableColumn<T, V extends Comparable<? super V>> extends Column<T, V> {

    /**
     * Create a comparable column.
     *
     * @param table the table this column belongs to
     * @param name the column name
     * @param valueType the value type class
     * @param sqlType the SQL type string
     */
    public ComparableColumn(Table<T> table, String name, Class<V> valueType, String sqlType) {
        super(table, name, valueType, sqlType);
    }

    /**
     * Greater than: column &gt; value.
     *
     * @param value the value to compare
     * @return predicate for the comparison
     */
    public Predicate gt(V value) {
        return new Predicate.SimplePredicate(this, Operator.GREATER_THAN, new Literal<>(value, getValueType()));
    }

    /**
     * Greater than or equals: column &gt;= value.
     *
     * @param value the value to compare
     * @return predicate for the comparison
     */
    public Predicate gte(V value) {
        return new Predicate.SimplePredicate(this, Operator.GREATER_THAN_OR_EQUALS, new Literal<>(value, getValueType()));
    }

    /**
     * Less than: column &lt; value.
     *
     * @param value the value to compare
     * @return predicate for the comparison
     */
    public Predicate lt(V value) {
        return new Predicate.SimplePredicate(this, Operator.LESS_THAN, new Literal<>(value, getValueType()));
    }

    /**
     * Less than or equals: column &lt;= value.
     *
     * @param value the value to compare
     * @return predicate for the comparison
     */
    public Predicate lte(V value) {
        return new Predicate.SimplePredicate(this, Operator.LESS_THAN_OR_EQUALS, new Literal<>(value, getValueType()));
    }

    /**
     * Between: column BETWEEN min AND max.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return predicate for the between check
     */
    public Predicate between(V min, V max) {
        return new Predicate.SimplePredicate(this, Operator.BETWEEN,
                new ListLiteral<>(Arrays.asList(min, max), getValueType()));
    }
}
