package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Expression;
import sant1ago.dev.suprim.core.type.JsonbColumn;
import sant1ago.dev.suprim.core.type.Table;
import sant1ago.dev.suprim.core.util.IdMetadata;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
 *
 * @param <T> entity type
 */
public final class InsertBuilder<T> {

    private final Table<T> table;
    private final List<ColumnValue<?>> columnValues = new ArrayList<>();
    private final List<Column<?, ?>> returningColumns = new ArrayList<>();

    InsertBuilder(Table<T> table) {
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
    public <V> InsertBuilder<T> column(Column<?, V> column, V value) {
        columnValues.add(new ColumnValue<>(column, value, null));
        return this;
    }

    /**
     * Set column value using an expression (e.g., Fn.now()).
     *
     * <pre>{@code
     * Suprim.insertInto(User_.TABLE)
     *     .column(User_.EMAIL, "test@example.com")
     *     .column(User_.CREATED_AT, Fn.now())
     *     .build();
     * }</pre>
     *
     * @param <V> the value type
     * @param column the column to set
     * @param expression the expression to use as value
     * @return this builder for chaining
     */
    public <V> InsertBuilder<T> column(Column<?, V> column, Expression<V> expression) {
        columnValues.add(new ColumnValue<>(column, null, expression));
        return this;
    }

    /**
     * Add RETURNING clause (PostgreSQL).
     *
     * @param columns the columns to return
     * @return this builder for chaining
     */
    public InsertBuilder<T> returning(Column<?, ?>... columns) {
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
        List<ColumnValue<?>> finalColumns = maybeAddGeneratedId();

        if (finalColumns.isEmpty()) {
            throw new IllegalStateException("INSERT requires at least one column");
        }

        StringBuilder sql = new StringBuilder();
        Map<String, Object> parameters = new LinkedHashMap<>();
        int paramCounter = 0;

        sql.append("INSERT INTO ").append(table.toSql(dialect));

        sql.append(" (");
        sql.append(finalColumns.stream()
                .map(cv -> dialect.quoteIdentifier(cv.column().getName()))
                .collect(Collectors.joining(", ")));
        sql.append(")");

        sql.append(" VALUES (");
        List<String> valuePlaceholders = new ArrayList<>();
        for (ColumnValue<?> cv : finalColumns) {
            if (nonNull(cv.expression())) {
                valuePlaceholders.add(cv.expression().toSql(dialect));
            } else {
                String paramName = "p" + (++paramCounter);
                // JSONB columns need CAST for PostgreSQL
                if (cv.column() instanceof JsonbColumn && dialect.capabilities().supportsJsonb()) {
                    valuePlaceholders.add("CAST(:" + paramName + " AS jsonb)");
                } else {
                    valuePlaceholders.add(":" + paramName);
                }
                parameters.put(paramName, cv.value());
            }
        }
        sql.append(String.join(", ", valuePlaceholders));
        sql.append(")");

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

    private List<ColumnValue<?>> maybeAddGeneratedId() {
        IdMetadata.Info idInfo = IdMetadata.get(table.getEntityType());

        if (isNull(idInfo) || hasIdColumn(idInfo.columnName()) || !idInfo.isApplicationGenerated()) {
            return columnValues;
        }

        Object generatedId = IdMetadata.generateId(idInfo);
        List<ColumnValue<?>> result = new ArrayList<>();
        result.add(new ColumnValue<>(new Column<>(table, idInfo.columnName(), Object.class, null), generatedId, null));
        result.addAll(columnValues);
        return result;
    }

    private boolean hasIdColumn(String columnName) {
        return columnValues.stream().anyMatch(cv -> cv.column().getName().equals(columnName));
    }

    private record ColumnValue<V>(Column<?, V> column, V value, Expression<V> expression) {
    }
}
