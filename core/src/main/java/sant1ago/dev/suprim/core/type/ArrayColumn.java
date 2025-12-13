package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import java.util.List;

/**
 * Column for PostgreSQL ARRAY type with array-specific operators.
 *
 * @param <T> Table entity type
 * @param <E> Array element type
 */
public final class ArrayColumn<T, E> extends Column<T, List<E>> {

    private final Class<E> elementType;

    /**
     * Create a new array column.
     *
     * @param table the table this column belongs to
     * @param name the column name
     * @param elementType the array element type class
     * @param sqlType the SQL type string
     */
    public ArrayColumn(Table<T> table, String name, Class<E> elementType, String sqlType) {
        super(table, name, TypeUtils.listClass(), sqlType);
        this.elementType = elementType;
    }

    /**
     * Array contains value: column @&gt; ARRAY[value].
     *
     * @param value the value to check for containment
     * @return predicate for the array contains check
     */
    public Predicate contains(E value) {
        return new Predicate.SimplePredicate(this, Operator.ARRAY_CONTAINS,
                new ArrayLiteral<>(List.of(value), elementType));
    }

    /**
     * Array contains all values: column @&gt; ARRAY[values].
     *
     * @param values the values to check for containment
     * @return predicate for the array contains all check
     */
    @SafeVarargs
    public final Predicate containsAll(E... values) {
        return new Predicate.SimplePredicate(this, Operator.ARRAY_CONTAINS,
                new ArrayLiteral<>(List.of(values), elementType));
    }

    /**
     * Array is contained by: column &lt;@ ARRAY[values].
     *
     * @param values the values to check against
     * @return predicate for the array contained by check
     */
    @SafeVarargs
    public final Predicate containedBy(E... values) {
        return new Predicate.SimplePredicate(this, Operator.ARRAY_CONTAINED_BY,
                new ArrayLiteral<>(List.of(values), elementType));
    }

    /**
     * Array overlap (any element in common): column &amp;&amp; ARRAY[values].
     *
     * @param values the values to check for overlap
     * @return predicate for the array overlap check
     */
    @SafeVarargs
    public final Predicate overlap(E... values) {
        return new Predicate.SimplePredicate(this, Operator.ARRAY_OVERLAP,
                new ArrayLiteral<>(List.of(values), elementType));
    }

    /**
     * Array overlap with list: column &amp;&amp; ARRAY[values].
     *
     * @param values the list of values to check for overlap
     * @return predicate for the array overlap check
     */
    public Predicate overlap(List<E> values) {
        return new Predicate.SimplePredicate(this, Operator.ARRAY_OVERLAP,
                new ArrayLiteral<>(values, elementType));
    }

    /**
     * Get the array element type.
     *
     * @return the element type class
     */
    public Class<E> getElementType() {
        return elementType;
    }

    /**
     * Array literal for PostgreSQL ARRAY operators.
     *
     * @param <E> the array element type
     * @param values the list of values in the array
     * @param elementType the class of the element type
     */
    public record ArrayLiteral<E>(List<E> values, Class<E> elementType) implements Expression<List<E>> {
        @Override
        public Class<List<E>> getValueType() {
            return TypeUtils.listClass();
        }

        @Override
        public String toSql(SqlDialect dialect) {
            if (values.isEmpty()) {
                return "ARRAY[]";
            }
            StringBuilder sb = new StringBuilder("ARRAY[");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sb.append(", ");
                E val = values.get(i);
                if (val instanceof String s) {
                    sb.append(dialect.quoteString(s));
                } else {
                    sb.append(val);
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
