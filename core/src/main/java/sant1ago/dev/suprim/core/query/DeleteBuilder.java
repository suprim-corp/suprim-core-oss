package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.Table;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Fluent builder for DELETE queries.
 *
 * <pre>{@code
 * QueryResult result = Suprim.deleteFrom(User_.TABLE)
 *     .where(User_.ID.eq(1L))
 *     .build();
 * }</pre>
 */
public final class DeleteBuilder {

    private final Table<?> table;
    private Predicate whereClause;
    private final List<Column<?, ?>> returningColumns = new ArrayList<>();

    DeleteBuilder(Table<?> table) {
        this.table = Objects.requireNonNull(table, "table cannot be null");
    }

    /**
     * Add a WHERE condition.
     *
     * @param predicate the where predicate
     * @return this builder for chaining
     */
    public DeleteBuilder where(Predicate predicate) {
        this.whereClause = predicate;
        return this;
    }

    /**
     * Add AND condition to WHERE clause.
     *
     * @param predicate the predicate to add
     * @return this builder for chaining
     */
    public DeleteBuilder and(Predicate predicate) {
        if (isNull(this.whereClause)) {
            this.whereClause = predicate;
        } else {
            this.whereClause = this.whereClause.and(predicate);
        }
        return this;
    }

    /**
     * Add RETURNING clause (PostgreSQL).
     *
     * @param columns the columns to return
     * @return this builder for chaining
     */
    public DeleteBuilder returning(Column<?, ?>... columns) {
        returningColumns.addAll(Arrays.asList(columns));
        return this;
    }

    /**
     * Build using default PostgreSQL dialect.
     *
     * @return the query result
     */
    public QueryResult build() {
        return build(PostgreSqlDialect.INSTANCE);
    }

    /**
     * Build using specified dialect.
     *
     * @param dialect the SQL dialect to use
     * @return the query result
     */
    public QueryResult build(SqlDialect dialect) {
        StringBuilder sql = new StringBuilder();

        // DELETE FROM table
        sql.append("DELETE FROM ").append(table.toSql(dialect));

        // WHERE
        if (nonNull(whereClause)) {
            sql.append(" WHERE ").append(whereClause.toSql(dialect));
        }

        // RETURNING (PostgreSQL)
        if (!returningColumns.isEmpty()) {
            sql.append(" RETURNING ");
            sql.append(returningColumns.stream()
                    .map(c -> dialect.quoteIdentifier(c.getName()))
                    .collect(Collectors.joining(", ")));
        }

        return new QueryResult(sql.toString(), Collections.emptyMap());
    }
}
