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
 * Fluent builder for UPSERT (INSERT ... ON CONFLICT) queries.
 *
 * <p>Generates dialect-specific SQL:
 * <ul>
 *   <li>PostgreSQL: {@code INSERT INTO t (...) VALUES (...) ON CONFLICT (id) DO UPDATE SET ...}</li>
 *   <li>MySQL: {@code INSERT INTO t (...) VALUES (...) ON DUPLICATE KEY UPDATE ...}</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * QueryResult result = Suprim.upsertInto(User_.TABLE)
 *     .columns(User_.ID, User_.EMAIL, User_.NAME)
 *     .values(Map.of("id", uuid, "email", "test@ex.com", "name", "Test"))
 *     .onConflict(User_.ID)
 *     .doUpdate(User_.EMAIL, User_.NAME)
 *     .build(dialect);
 *
 * // Or with doNothing:
 * QueryResult result = Suprim.upsertInto(User_.TABLE)
 *     .columns(User_.EMAIL, User_.NAME)
 *     .values(Map.of("email", "test@ex.com", "name", "Test"))
 *     .onConflict(User_.EMAIL)
 *     .doNothing()
 *     .build(dialect);
 * }</pre>
 *
 * @param <T> entity type
 */
public final class UpsertBuilder<T> {

    private final Table<T> table;
    private final List<String> columnNames = new ArrayList<>();
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private final List<String> conflictColumns = new ArrayList<>();
    private final List<String> updateColumns = new ArrayList<>();
    private boolean doNothing = false;
    private final List<String> returningColumns = new ArrayList<>();

    UpsertBuilder(Table<T> table) {
        this.table = Objects.requireNonNull(table, "table cannot be null");
    }

    /**
     * Define columns for the upsert.
     *
     * @param columns columns to insert
     * @return this builder for chaining
     */
    @SafeVarargs
    public final UpsertBuilder<T> columns(Column<?, ?>... columns) {
        for (Column<?, ?> column : columns) {
            columnNames.add(column.getName());
        }
        return this;
    }

    /**
     * Define columns by name.
     *
     * @param names column names
     * @return this builder for chaining
     */
    public UpsertBuilder<T> columns(String... names) {
        for (String name : names) {
            columnNames.add(name);
        }
        return this;
    }

    /**
     * Define columns from a list.
     *
     * @param names column names
     * @return this builder for chaining
     */
    public UpsertBuilder<T> columns(List<String> names) {
        columnNames.addAll(names);
        return this;
    }

    /**
     * Add a row of values for upsert.
     *
     * @param values map of column name to value
     * @return this builder for chaining
     */
    public UpsertBuilder<T> values(Map<String, Object> values) {
        rows.add(new LinkedHashMap<>(values));
        return this;
    }

    /**
     * Add multiple rows of values.
     *
     * @param valuesList list of value maps
     * @return this builder for chaining
     */
    public UpsertBuilder<T> values(List<Map<String, Object>> valuesList) {
        for (Map<String, Object> values : valuesList) {
            rows.add(new LinkedHashMap<>(values));
        }
        return this;
    }

    /**
     * Set a single column value (alternative to values map).
     *
     * @param column the column
     * @param value  the value
     * @return this builder for chaining
     */
    public <V> UpsertBuilder<T> set(Column<T, V> column, V value) {
        if (!columnNames.contains(column.getName())) {
            columnNames.add(column.getName());
        }
        if (rows.isEmpty()) {
            rows.add(new LinkedHashMap<>());
        }
        rows.get(rows.size() - 1).put(column.getName(), value);
        return this;
    }

    /**
     * Define conflict columns for ON CONFLICT clause.
     *
     * @param columns conflict columns (typically primary key or unique constraint)
     * @return this builder for chaining
     */
    @SafeVarargs
    public final UpsertBuilder<T> onConflict(Column<?, ?>... columns) {
        for (Column<?, ?> column : columns) {
            conflictColumns.add(column.getName());
        }
        return this;
    }

    /**
     * Define conflict columns by name.
     *
     * @param names conflict column names
     * @return this builder for chaining
     */
    public UpsertBuilder<T> onConflict(String... names) {
        for (String name : names) {
            conflictColumns.add(name);
        }
        return this;
    }

    /**
     * Define columns to update on conflict.
     *
     * @param columns columns to update
     * @return this builder for chaining
     */
    @SafeVarargs
    public final UpsertBuilder<T> doUpdate(Column<?, ?>... columns) {
        this.doNothing = false;
        for (Column<?, ?> column : columns) {
            updateColumns.add(column.getName());
        }
        return this;
    }

    /**
     * Define columns to update by name.
     *
     * @param names column names to update
     * @return this builder for chaining
     */
    public UpsertBuilder<T> doUpdate(String... names) {
        this.doNothing = false;
        for (String name : names) {
            updateColumns.add(name);
        }
        return this;
    }

    /**
     * Update all non-conflict columns on conflict.
     *
     * @return this builder for chaining
     */
    public UpsertBuilder<T> doUpdateAll() {
        this.doNothing = false;
        for (String col : columnNames) {
            if (!conflictColumns.contains(col)) {
                updateColumns.add(col);
            }
        }
        return this;
    }

    /**
     * Do nothing on conflict (ignore duplicates).
     *
     * @return this builder for chaining
     */
    public UpsertBuilder<T> doNothing() {
        this.doNothing = true;
        this.updateColumns.clear();
        return this;
    }

    /**
     * Add RETURNING clause (PostgreSQL only).
     *
     * @param columns columns to return
     * @return this builder for chaining
     */
    @SafeVarargs
    public final UpsertBuilder<T> returning(Column<?, ?>... columns) {
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
    public UpsertBuilder<T> returning(String... names) {
        for (String name : names) {
            returningColumns.add(name);
        }
        return this;
    }

    /**
     * Build the UPSERT query.
     *
     * @param dialect SQL dialect to use
     * @return query result with SQL and parameters
     */
    public QueryResult build(SqlDialect dialect) {
        if (columnNames.isEmpty()) {
            throw new IllegalStateException("UPSERT requires at least one column");
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("UPSERT requires at least one row of values");
        }
        if (conflictColumns.isEmpty()) {
            throw new IllegalStateException("UPSERT requires conflict columns (onConflict)");
        }
        if (!doNothing && updateColumns.isEmpty()) {
            throw new IllegalStateException("UPSERT requires either doUpdate columns or doNothing()");
        }

        boolean isPostgres = dialect.getName().toLowerCase().contains("postgres");

        if (isPostgres) {
            return buildPostgresUpsert(dialect);
        } else {
            return buildMysqlUpsert(dialect);
        }
    }

    /**
     * Build PostgreSQL ON CONFLICT DO UPDATE/DO NOTHING syntax.
     */
    private QueryResult buildPostgresUpsert(SqlDialect dialect) {
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

        // VALUES clause
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

        // ON CONFLICT clause
        sql.append(" ON CONFLICT (");
        sql.append(conflictColumns.stream()
            .map(dialect::quoteIdentifier)
            .collect(Collectors.joining(", ")));
        sql.append(")");

        if (doNothing) {
            sql.append(" DO NOTHING");
        } else {
            sql.append(" DO UPDATE SET ");
            sql.append(updateColumns.stream()
                .map(col -> dialect.quoteIdentifier(col) + " = EXCLUDED." + dialect.quoteIdentifier(col))
                .collect(Collectors.joining(", ")));
        }

        // RETURNING clause
        if (!returningColumns.isEmpty() && dialect.capabilities().supportsReturning()) {
            sql.append(" RETURNING ");
            sql.append(returningColumns.stream()
                .map(dialect::quoteIdentifier)
                .collect(Collectors.joining(", ")));
        }

        return new QueryResult(sql.toString(), parameters);
    }

    /**
     * Build MySQL ON DUPLICATE KEY UPDATE syntax.
     */
    private QueryResult buildMysqlUpsert(SqlDialect dialect) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> parameters = new LinkedHashMap<>();
        int paramCounter = 0;

        // INSERT INTO table
        if (doNothing) {
            sql.append("INSERT IGNORE INTO ").append(table.toSql(dialect));
        } else {
            sql.append("INSERT INTO ").append(table.toSql(dialect));
        }

        // Column list
        sql.append(" (");
        sql.append(columnNames.stream()
            .map(dialect::quoteIdentifier)
            .collect(Collectors.joining(", ")));
        sql.append(")");

        // VALUES clause
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

        // ON DUPLICATE KEY UPDATE (only if not doNothing, since INSERT IGNORE handles that)
        if (!doNothing) {
            sql.append(" ON DUPLICATE KEY UPDATE ");
            sql.append(updateColumns.stream()
                .map(col -> dialect.quoteIdentifier(col) + " = VALUES(" + dialect.quoteIdentifier(col) + ")")
                .collect(Collectors.joining(", ")));
        }

        return new QueryResult(sql.toString(), parameters);
    }

    /**
     * Get the number of rows to be upserted.
     *
     * @return row count
     */
    public int rowCount() {
        return rows.size();
    }
}
