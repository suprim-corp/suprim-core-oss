package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

/**
 * Column with an alias for SELECT clause.
 * <pre>{@code
 * User_.EMAIL.as("user_email")  // users."email" AS user_email
 * }</pre>
 *
 * @param <T> the table entity type
 * @param <V> the column value type
 * @param column the column to alias
 * @param alias the alias name
 */
public record AliasedColumn<T, V>(Column<T, V> column, String alias) implements Expression<V> {

    @Override
    public String toSql(SqlDialect dialect) {
        return column.toSql(dialect) + " AS " + alias;
    }

    @Override
    public Class<V> getValueType() {
        return column.getValueType();
    }
}
