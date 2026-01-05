package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Fluent builder for batch INSERT queries with multi-value support.
 *
 * <p>Generates efficient SQL with multiple VALUES rows:
 * <pre>{@code
 * INSERT INTO users (id, name, email) VALUES
 *     (?, ?, ?),
 *     (?, ?, ?),
 *     (?, ?, ?)
 * RETURNING id
 * }</pre>
 *
 * <p>Usage:
 * <pre>{@code
 * QueryResult result = Suprim.batchInsertInto(User_.TABLE)
 *     .columns(User_.ID, User_.NAME, User_.EMAIL)
 *     .values(Map.of("id", uuid1, "name", "John", "email", "john@ex.com"))
 *     .values(Map.of("id", uuid2, "name", "Jane", "email", "jane@ex.com"))
 *     .returning(User_.ID)
 *     .build(dialect);
 * }</pre>
 *
 * @param <T> entity type
 */
public final class BatchInsertBuilder<T> {

    private final Table<T> table;
    private final List<String> columnNames = new ArrayList<>();
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private final List<String> returningColumns = new ArrayList<>();

    BatchInsertBuilder(Table<T> table) {
        this.table = Objects.requireNonNull(table, "table cannot be null");
    }

    /**
     * Define columns for the batch insert.
     *
     * @param columns columns to insert
     * @return this builder for chaining
     */
    @SafeVarargs
    public final BatchInsertBuilder<T> columns(Column<?, ?>... columns) {
        for (Column<?, ?> column : columns) {
            columnNames.add(column.getName());
        }
        return this;
    }

    /**
     * Define columns by name for the batch insert.
     *
     * @param names column names to insert
     * @return this builder for chaining
     */
    public BatchInsertBuilder<T> columns(String... names) {
        for (String name : names) {
            columnNames.add(name);
        }
        return this;
    }

    /**
     * Define columns from a list of names.
     *
     * @param names column names
     * @return this builder for chaining
     */
    public BatchInsertBuilder<T> columns(List<String> names) {
        columnNames.addAll(names);
        return this;
    }

    /**
     * Add a row of values for batch insert.
     * Keys should match column names.
     *
     * @param values map of column name to value
     * @return this builder for chaining
     */
    public BatchInsertBuilder<T> values(Map<String, Object> values) {
        rows.add(new LinkedHashMap<>(values));
        return this;
    }

    /**
     * Add multiple rows of values.
     *
     * @param valuesList list of value maps
     * @return this builder for chaining
     */
    public BatchInsertBuilder<T> values(List<Map<String, Object>> valuesList) {
        for (Map<String, Object> values : valuesList) {
            rows.add(new LinkedHashMap<>(values));
        }
        return this;
    }

    /**
     * Add RETURNING clause (PostgreSQL only).
     *
     * @param columns columns to return
     * @return this builder for chaining
     */
    @SafeVarargs
    public final BatchInsertBuilder<T> returning(Column<?, ?>... columns) {
        for (Column<?, ?> column : columns) {
            returningColumns.add(column.getName());
        }
        return this;
    }

    /**
     * Add RETURNING clause by column name.
     *
     * @param names column names to return
     * @return this builder for chaining
     */
    public BatchInsertBuilder<T> returning(String... names) {
        for (String name : names) {
            returningColumns.add(name);
        }
        return this;
    }

    /**
     * Build the batch INSERT query.
     *
     * @param dialect SQL dialect to use
     * @return query result with SQL and parameters
     */
    public QueryResult build(SqlDialect dialect) {
        if (columnNames.isEmpty()) {
            throw new IllegalStateException("Batch INSERT requires at least one column");
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("Batch INSERT requires at least one row of values");
        }

        StringBuilder sql = new StringBuilder();
        Map<String, Object> parameters = new LinkedHashMap<>();
        int paramCounter = 0;

        // INSERT INTO table
        sql.append("INSERT INTO ").append(table.toSql(dialect));

        // Column list
        sql.append(" (");
        sql.append(columnNames.stream()
                .map(dialect::quoteIdentifier)
                .collect(Collectors.joining(", ")));
        sql.append(")");

        // VALUES clause with multiple rows
        sql.append(" VALUES ");
        List<String> rowPlaceholders = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            List<String> valuePlaceholders = new ArrayList<>();
            for (String colName : columnNames) {
                Object value = row.get(colName);
                String paramName = "p" + (++paramCounter);
                valuePlaceholders.add(":" + paramName);
                parameters.put(paramName, value);
            }
            rowPlaceholders.add("(" + String.join(", ", valuePlaceholders) + ")");
        }
        sql.append(String.join(", ", rowPlaceholders));

        // RETURNING clause (PostgreSQL)
        if (!returningColumns.isEmpty() && dialect.capabilities().supportsReturning()) {
            sql.append(" RETURNING ");
            sql.append(returningColumns.stream()
                    .map(dialect::quoteIdentifier)
                    .collect(Collectors.joining(", ")));
        }

        return new QueryResult(sql.toString(), parameters);
    }

    /**
     * Get the number of rows to be inserted.
     *
     * @return row count
     */
    public int rowCount() {
        return rows.size();
    }

    /**
     * Get column names.
     *
     * @return list of column names
     */
    public List<String> getColumnNames() {
        return new ArrayList<>(columnNames);
    }
}
