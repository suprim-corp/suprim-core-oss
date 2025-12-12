package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

/**
 * SQL COALESCE function - returns first non-null value.
 *
 * <pre>{@code
 * // COALESCE(name, 'Unknown') AS display_name
 * Coalesce.of(User_.NAME, "Unknown").as("display_name")
 *
 * // COALESCE(workspace_member.name, account.username) AS user_name
 * Coalesce.of(WorkspaceMember_.NAME, Account_.USERNAME).as("user_name")
 *
 * // COALESCE(nickname, first_name, email) AS display
 * Coalesce.of(User_.NICKNAME, User_.FIRST_NAME, User_.EMAIL).as("display")
 * }</pre>
 *
 * @param <V> the type of the coalesce result
 */
public final class Coalesce<V> implements Expression<V> {

    private final List<Expression<?>> expressions;
    private final Class<V> valueType;
    private String alias;

    private Coalesce(List<Expression<?>> expressions) {
        this.expressions = new ArrayList<>(expressions);
        this.valueType = expressions.isEmpty()
                ? TypeUtils.objectClass()
                : TypeUtils.valueTypeOf(expressions.get(0));
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create COALESCE with multiple expressions.
     * <pre>{@code
     * Coalesce.of(User_.NAME, User_.EMAIL).as("display")
     * }</pre>
     *
     * @param <V> the result type
     * @param expressions the expressions to coalesce
     * @return coalesce expression
     */
    public static <V> Coalesce<V> of(Expression<?>... expressions) {
        return new Coalesce<>(Arrays.asList(expressions));
    }

    /**
     * Create COALESCE with column and literal fallback.
     * <pre>{@code
     * Coalesce.of(User_.NAME, "Unknown").as("display_name")
     * }</pre>
     *
     * @param <V> the result type
     * @param expression the primary expression
     * @param fallback the fallback value if expression is null
     * @return coalesce expression
     */
    public static <V> Coalesce<V> of(Expression<V> expression, V fallback) {
        Class<V> fallbackType = TypeUtils.classOf(fallback);
        return new Coalesce<>(Arrays.asList(expression, new Literal<>(fallback, fallbackType)));
    }

    // ==================== MODIFIERS ====================

    /**
     * Add another expression to the COALESCE chain.
     *
     * @param expression the expression to add
     * @return this coalesce for chaining
     */
    public Coalesce<V> or(Expression<?> expression) {
        this.expressions.add(expression);
        return this;
    }

    /**
     * Add a literal fallback value.
     *
     * @param fallback the fallback value
     * @return this coalesce for chaining
     */
    public Coalesce<V> or(V fallback) {
        Class<V> fallbackType = TypeUtils.classOf(fallback);
        this.expressions.add(new Literal<>(fallback, fallbackType));
        return this;
    }

    /**
     * Add alias to COALESCE expression.
     *
     * @param alias the alias name
     * @return this coalesce for chaining
     */
    public Coalesce<V> as(String alias) {
        this.alias = alias;
        return this;
    }

    // ==================== EXPRESSION ====================

    @Override
    public Class<V> getValueType() {
        return valueType;
    }

    @Override
    public String toSql(SqlDialect dialect) {
        String args = expressions.stream()
                .map(e -> e.toSql(dialect))
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("COALESCE(").append(args).append(")");

        if (nonNull(alias)) {
            sb.append(" AS ").append(alias);
        }

        return sb.toString();
    }
}
