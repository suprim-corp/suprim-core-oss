package sant1ago.dev.suprim.core.type;

import java.util.List;

/**
 * Utility class for unavoidable generic type casts due to Java type erasure.
 *
 * <p>These casts are necessary because:
 * <ul>
 *   <li>Java has no Class&lt;List&lt;E&gt;&gt; literal due to type erasure</li>
 *   <li>Object.getClass() returns Class&lt;?&gt; not Class&lt;T&gt;</li>
 *   <li>Wildcard capture loses type information</li>
 * </ul>
 */
public final class TypeUtils {

    private TypeUtils() {
    }

    /**
     * Internal type coercion helper.
     * Required due to Java type erasure.
     */
    @SuppressWarnings("unchecked")
    private static <T> T coerce(Object obj) {
        return (T) obj;
    }

    // ==================== CLASS UTILITIES ====================

    /**
     * Get Class&lt;List&lt;E&gt;&gt;.
     */
    public static <E> Class<List<E>> listClass() {
        return coerce(List.class);
    }

    /**
     * Get Class&lt;V&gt; for Object.class.
     */
    public static <V> Class<V> objectClass() {
        return coerce(Object.class);
    }

    /**
     * Get typed Class from value's runtime type.
     */
    public static <T> Class<T> classOf(T value) {
        return coerce(value.getClass());
    }

    /**
     * Get typed Class from Expression.
     */
    public static <V> Class<V> valueTypeOf(Expression<?> expression) {
        return coerce(expression.getValueType());
    }

    // ==================== CAST UTILITIES ====================

    /**
     * Cast wildcard Expression to typed Expression.
     */
    public static <V> Expression<V> castExpression(Expression<?> expression) {
        return coerce(expression);
    }

    /**
     * Cast wildcard Relation to typed Relation.
     */
    public static <T, R> Relation<T, R> castRelation(Relation<?, ?> relation) {
        return coerce(relation);
    }

    /**
     * Cast Table to different generic type.
     */
    public static <T> Table<T> castTable(Table<?> table) {
        return coerce(table);
    }

    /**
     * Cast any object to type T.
     */
    public static <T> T cast(Object obj) {
        return coerce(obj);
    }
}
