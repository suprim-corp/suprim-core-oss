package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Expression;
import sant1ago.dev.suprim.core.type.JsonbColumn;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.Table;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Fluent builder for UPDATE queries.
 *
 * <pre>{@code
 * QueryResult result = Suprim.update(User_.TABLE)
 *     .set(User_.EMAIL, "new@example.com")
 *     .set(User_.AGE, 30)
 *     .where(User_.ID.eq(1L))
 *     .build();
 * }</pre>
 */
public final class UpdateBuilder {

    private final Table<?> table;
    private final List<ColumnValue<?>> setValues = new ArrayList<>();
    private Predicate whereClause;
    private final List<Column<?, ?>> returningColumns = new ArrayList<>();

    UpdateBuilder(Table<?> table) {
        this.table = Objects.requireNonNull(table, "table cannot be null");
    }

    /**
     * Set column to value.
     */
    public <V> UpdateBuilder set(Column<?, V> column, V value) {
        setValues.add(new ColumnValue<>(column, value, null));
        return this;
    }

    /**
     * Set column to expression value (e.g., Fn.now()).
     *
     * <pre>{@code
     * Suprim.update(User_.TABLE)
     *     .set(User_.UPDATED_AT, Fn.now())
     *     .where(User_.ID.eq(1L))
     *     .build();
     * }</pre>
     */
    public <V> UpdateBuilder set(Column<?, V> column, Expression<V> expression) {
        setValues.add(new ColumnValue<>(column, null, expression));
        return this;
    }

    /**
     * Add WHERE condition.
     */
    public UpdateBuilder where(Predicate predicate) {
        this.whereClause = predicate;
        return this;
    }

    /**
     * Add AND condition to WHERE clause.
     */
    public UpdateBuilder and(Predicate predicate) {
        if (isNull(this.whereClause)) {
            this.whereClause = predicate;
        } else {
            this.whereClause = this.whereClause.and(predicate);
        }
        return this;
    }

    /**
     * Add RETURNING clause (PostgreSQL).
     */
    public UpdateBuilder returning(Column<?, ?>... columns) {
        returningColumns.addAll(Arrays.asList(columns));
        return this;
    }

    /**
     * Build using default PostgreSQL dialect.
     */
    public QueryResult build() {
        return build(PostgreSqlDialect.INSTANCE);
    }

    /**
     * Build using specified dialect.
     */
    public QueryResult build(SqlDialect dialect) {
        if (setValues.isEmpty()) {
            throw new IllegalStateException("UPDATE requires at least one SET clause");
        }

        StringBuilder sql = new StringBuilder();
        Map<String, Object> parameters = new LinkedHashMap<>();
        int paramCounter = 0;

        // UPDATE table
        sql.append("UPDATE ").append(table.toSql(dialect));

        // SET column = value, ...
        sql.append(" SET ");
        List<String> setClauses = new ArrayList<>();
        for (ColumnValue<?> cv : setValues) {
            String colName = dialect.quoteIdentifier(cv.column().getName());
            if (nonNull(cv.expression())) {
                // Use expression SQL directly (e.g., NOW())
                setClauses.add(colName + " = " + cv.expression().toSql(dialect));
            } else {
                // Use parameter placeholder
                String paramName = "p" + (++paramCounter);
                // JSONB columns need CAST for PostgreSQL
                if (cv.column() instanceof JsonbColumn && dialect.capabilities().supportsJsonb()) {
                    setClauses.add(colName + " = CAST(:" + paramName + " AS jsonb)");
                } else {
                    setClauses.add(colName + " = :" + paramName);
                }
                parameters.put(paramName, cv.value());
            }
        }
        sql.append(String.join(", ", setClauses));

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

        return new QueryResult(sql.toString(), parameters);
    }

    private record ColumnValue<V>(Column<?, V> column, V value, Expression<V> expression) {
    }
}
