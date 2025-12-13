package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Literal;
import sant1ago.dev.suprim.core.type.Table;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fluent builder for INSERT queries.
 *
 * <pre>{@code
 * QueryResult result = Suprim.insertInto(User_.TABLE)
 *     .column(User_.EMAIL, "test@example.com")
 *     .column(User_.AGE, 25)
 *     .returning(User_.ID)
 *     .build();
 * }</pre>
 */
public final class InsertBuilder {

    private final Table<?> table;
    private final List<ColumnValue<?>> columnValues = new ArrayList<>();
    private final List<Column<?, ?>> returningColumns = new ArrayList<>();

    InsertBuilder(Table<?> table) {
        this.table = Objects.requireNonNull(table, "table cannot be null");
    }

    /**
     * Set column value.
     *
     * @param <V> the value type
     * @param column the column to set
     * @param value the value to insert
     * @return this builder for chaining
     */
    public <V> InsertBuilder column(Column<?, V> column, V value) {
        columnValues.add(new ColumnValue<>(column, value));
        return this;
    }

    /**
     * Add RETURNING clause (PostgreSQL).
     *
     * @param columns the columns to return
     * @return this builder for chaining
     */
    public InsertBuilder returning(Column<?, ?>... columns) {
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
        if (columnValues.isEmpty()) {
            throw new IllegalStateException("INSERT requires at least one column");
        }

        StringBuilder sql = new StringBuilder();
        Map<String, Object> parameters = new LinkedHashMap<>();
        int paramCounter = 0;

        // INSERT INTO table
        sql.append("INSERT INTO ").append(table.toSql(dialect));

        // (columns)
        sql.append(" (");
        sql.append(columnValues.stream()
                .map(cv -> dialect.quoteIdentifier(cv.column().getName()))
                .collect(Collectors.joining(", ")));
        sql.append(")");

        // VALUES (...)
        sql.append(" VALUES (");
        List<String> valuePlaceholders = new ArrayList<>();
        for (ColumnValue<?> cv : columnValues) {
            String paramName = "p" + (++paramCounter);
            valuePlaceholders.add(":" + paramName);
            parameters.put(paramName, cv.value());
        }
        sql.append(String.join(", ", valuePlaceholders));
        sql.append(")");

        // RETURNING (PostgreSQL only)
        if (!returningColumns.isEmpty()) {
            if (!dialect.capabilities().supportsReturning()) {
                throw new UnsupportedDialectFeatureException("RETURNING", dialect.getName(),
                        "Use LAST_INSERT_ID() for MySQL instead.");
            }
            sql.append(" RETURNING ");
            sql.append(returningColumns.stream()
                    .map(c -> dialect.quoteIdentifier(c.getName()))
                    .collect(Collectors.joining(", ")));
        }

        return new QueryResult(sql.toString(), parameters);
    }

    private record ColumnValue<V>(Column<?, V> column, V value) {
    }
}
