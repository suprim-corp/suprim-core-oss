package sant1ago.dev.suprim.core.type;

import java.util.List;

/**
 * Utility class for unavoidable generic type casts due to Java type erasure.
 * Consolidates all @SuppressWarnings("unchecked") in one place.
 *
 * <p>These casts are necessary because:
 * <ul>
 *   <li>Java has no Class&lt;List&lt;E&gt;&gt; literal due to type erasure</li>
 *   <li>Object.getClass() returns Class&lt;?&gt; not Class&lt;T&gt;</li>
 *   <li>Wildcard capture loses type information</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
public final class TypeUtils {

    private TypeUtils() {
        // Utility class
    }

    /**
     * Get Class&lt;List&lt;E&gt;&gt; - impossible without cast due to type erasure.
     * Java has no List&lt;E&gt;.class literal.
     */
    public static <E> Class<List<E>> listClass() {
        return (Class<List<E>>) (Class<?>) List.class;
    }

    /**
     * Get Class&lt;V&gt; for Object.class - used as fallback when type is unknown.
     */
    public static <V> Class<V> objectClass() {
        return (Class<V>) Object.class;
    }

    /**
     * Cast Object.getClass() result to typed Class&lt;T&gt;.
     * Object.getClass() returns Class&lt;?&gt; but we know runtime type.
     */
    public static <T> Class<T> classOf(T value) {
        return (Class<T>) value.getClass();
    }

    /**
     * Cast Expression&lt;?&gt;.getValueType() to Class&lt;V&gt;.
     * Wildcard capture loses type info.
     */
    public static <V> Class<V> valueTypeOf(Expression<?> expression) {
        return (Class<V>) expression.getValueType();
    }

    /**
     * Cast wildcard Expression to typed Expression.
     */
    public static <V> Expression<V> castExpression(Expression<?> expression) {
        return (Expression<V>) expression;
    }

    /**
     * Cast wildcard Relation to typed Relation.
     */
    public static <T, R> Relation<T, R> castRelation(Relation<?, ?> relation) {
        return (Relation<T, R>) relation;
    }

    /**
     * Cast Table to different generic type.
     * Used for polymorphic relations where target type is unknown at compile time.
     */
    public static <T> Table<T> castTable(Table<?> table) {
        return (Table<T>) table;
    }

    /**
     * Cast any object to type T.
     * Use for type-safe heterogeneous container patterns where runtime type is guaranteed.
     */
    public static <T> T cast(Object obj) {
        return (T) obj;
    }
}
