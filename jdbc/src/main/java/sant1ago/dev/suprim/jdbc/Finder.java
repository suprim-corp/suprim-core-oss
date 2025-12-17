package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.annotation.entity.CreationTimestamp;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.query.Suprim;
import sant1ago.dev.suprim.core.type.OrderSpec;
import sant1ago.dev.suprim.core.type.Table;

import sant1ago.dev.suprim.core.type.Predicate;

import sant1ago.dev.suprim.jdbc.exception.NoResultException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fluent query builder for entities with execution capability.
 *
 * <p>Provides a Laravel Eloquent-style API for querying entities:
 * <pre>{@code
 * // Find all with eager loading and sorting
 * List<User> users = executor.find(User.class)
 *     .with("posts", "comments")
 *     .orderBy("created_at", "desc")
 *     .limit(10)
 *     .get();
 *
 * // Find first matching
 * Optional<User> user = executor.find(User.class)
 *     .where("email", "test@example.com")
 *     .first();
 *
 * // Cursor pagination
 * CursorResult<User> result = executor.find(User.class)
 *     .orderBy("id")
 *     .cursor(null, 10);
 * }</pre>
 *
 * @param <T> the entity type
 */
public final class Finder<T> {

    private final SuprimExecutor executor;
    private final Class<T> entityClass;
    private final SelectBuilder builder;
    private int paramCounter = 0;

    Finder(SuprimExecutor executor, Class<T> entityClass) {
        this.executor = Objects.requireNonNull(executor, "Executor cannot be null");
        this.entityClass = Objects.requireNonNull(entityClass, "Entity class cannot be null");

        // Get table info from entity
        String tableName = Suprim.table(entityClass);
        String schema = Suprim.schema(entityClass);
        Table<T> table = schema.isEmpty()
            ? Table.of(tableName, entityClass)
            : Table.of(tableName, schema, entityClass);

        // Initialize SelectBuilder
        this.builder = Suprim.from(table);
    }

    // ==================== EAGER LOADING ====================

    /**
     * Eager load relations by field name.
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .with("posts", "comments", "profile")
     *     .get();
     * }</pre>
     *
     * @param relations relation field names to eager load
     * @return this finder for chaining
     */
    public Finder<T> with(String... relations) {
        builder.with(relations);
        return this;
    }

    /**
     * Exclude relations from default eager loading.
     *
     * @param relations relation field names to exclude
     * @return this finder for chaining
     */
    public Finder<T> without(String... relations) {
        builder.without(relations);
        return this;
    }

    // ==================== SORTING ====================

    /**
     * Order by column ascending.
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .orderBy("name")
     *     .get();
     * }</pre>
     *
     * @param column column name
     * @return this finder for chaining
     */
    public Finder<T> orderBy(String column) {
        return orderBy(column, "asc");
    }

    /**
     * Order by column with direction.
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .orderBy("created_at", "desc")
     *     .get();
     * }</pre>
     *
     * @param column    column name
     * @param direction "asc" or "desc"
     * @return this finder for chaining
     */
    public Finder<T> orderBy(String column, String direction) {
        String dir = "desc".equalsIgnoreCase(direction) ? "DESC" : "ASC";
        builder.orderBy(OrderSpec.raw(column + " " + dir));
        return this;
    }

    /**
     * Order by column descending.
     *
     * @param column column name
     * @return this finder for chaining
     */
    public Finder<T> orderByDesc(String column) {
        return orderBy(column, "desc");
    }

    /**
     * Order by newest first (creation timestamp DESC).
     *
     * <p>Uses the column from {@code @CreationTimestamp} annotation if present,
     * otherwise falls back to "created_at".
     *
     * @return this finder for chaining
     */
    public Finder<T> latest() {
        return orderBy(getCreationTimestampColumn(), "desc");
    }

    /**
     * Order by oldest first (creation timestamp ASC).
     *
     * <p>Uses the column from {@code @CreationTimestamp} annotation if present,
     * otherwise falls back to "created_at".
     *
     * @return this finder for chaining
     */
    public Finder<T> oldest() {
        return orderBy(getCreationTimestampColumn(), "asc");
    }

    /**
     * Get the creation timestamp column name from entity's @CreationTimestamp annotation.
     *
     * @return column name from annotation or "created_at" as default
     */
    private String getCreationTimestampColumn() {
        for (Field field : getAllFields(entityClass)) {
            CreationTimestamp annotation = field.getAnnotation(CreationTimestamp.class);
            if (Objects.nonNull(annotation)) {
                return annotation.column();
            }
        }
        return "created_at";
    }

    /**
     * Get all fields including inherited ones.
     */
    private static Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    // ==================== CONDITIONS ====================

    /**
     * Add WHERE condition (equals).
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .where("status", "active")
     *     .get();
     * }</pre>
     *
     * @param column column name
     * @param value  value to match
     * @return this finder for chaining
     */
    public Finder<T> where(String column, Object value) {
        String paramName = nextParamName();
        builder.whereRaw(column + " = :" + paramName, Map.of(paramName, normalizeValue(column, value)));
        return this;
    }

    /**
     * Add WHERE condition with operator.
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .where("age", ">=", 18)
     *     .get();
     * }</pre>
     *
     * @param column   column name
     * @param operator comparison operator
     * @param value    value to compare
     * @return this finder for chaining
     */
    public Finder<T> where(String column, String operator, Object value) {
        String paramName = nextParamName();
        builder.andRaw(column + " " + operator + " :" + paramName, Map.of(paramName, normalizeValue(column, value)));
        return this;
    }

    /**
     * Add WHERE IN condition with list of values.
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .whereIn("status", List.of("active", "pending"))
     *     .get();
     * }</pre>
     *
     * @param column column name
     * @param values list of values
     * @return this finder for chaining
     */
    public Finder<T> whereIn(String column, List<?> values) {
        if (values.isEmpty()) {
            builder.whereRaw("1 = 0"); // Always false
            return this;
        }
        // Build placeholders and params map - use LinkedHashMap to preserve order
        StringBuilder placeholders = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) {
            String paramName = nextParamName();
            if (i > 0) placeholders.append(", ");
            placeholders.append(":").append(paramName);
            params.put(paramName, normalizeValue(column, values.get(i)));
        }
        builder.andRaw(column + " IN (" + placeholders + ")", params);
        return this;
    }

    /**
     * Add WHERE IN condition with a subquery (Laravel-style).
     *
     * <pre>{@code
     * // Using pre-built subquery with type-safe metamodel
     * SelectBuilder subquery = Suprim.select(Permission_.RESOURCE_ID)
     *     .from(Permission_.TABLE)
     *     .where(Permission_.ACCOUNT_ID.eq(accountId));
     *
     * executor.find(Workspace.class)
     *     .whereIn("id", subquery)
     *     .get();
     * }</pre>
     *
     * @param column   column name
     * @param subquery SelectBuilder representing the subquery
     * @return this finder for chaining
     */
    public Finder<T> whereIn(String column, SelectBuilder subquery) {
        QueryResult subqueryResult = subquery.build();
        builder.andRaw(column + " IN (" + subqueryResult.sql() + ")", subqueryResult.parameters());
        return this;
    }

    /**
     * Add WHERE IN condition with a subquery builder function (Laravel closure-style).
     *
     * <pre>{@code
     * executor.find(Workspace.class)
     *     .whereIn("id", q -> q
     *         .selectRaw("resource_id")
     *         .from(Table.of("permissions"))
     *         .whereRaw("account_id = :acct", Map.of("acct", accountId)))
     *     .get();
     * }</pre>
     *
     * @param column          column name
     * @param subqueryBuilder function that builds the subquery
     * @return this finder for chaining
     */
    public Finder<T> whereIn(String column, Function<SelectBuilder, SelectBuilder> subqueryBuilder) {
        SelectBuilder subquery = subqueryBuilder.apply(Suprim.select());
        return whereIn(column, subquery);
    }

    /**
     * Add WHERE NOT IN condition with a subquery.
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .whereNotIn("id", Suprim.select(BlockedUser_.USER_ID).from(BlockedUser_.TABLE))
     *     .get();
     * }</pre>
     *
     * @param column   column name
     * @param subquery SelectBuilder representing the subquery
     * @return this finder for chaining
     */
    public Finder<T> whereNotIn(String column, SelectBuilder subquery) {
        QueryResult subqueryResult = subquery.build();
        builder.andRaw(column + " NOT IN (" + subqueryResult.sql() + ")", subqueryResult.parameters());
        return this;
    }

    /**
     * Add WHERE NOT IN condition with a subquery builder function.
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .whereNotIn("id", q -> q
     *         .selectRaw("user_id")
     *         .from(Table.of("blocked_users")))
     *     .get();
     * }</pre>
     *
     * @param column          column name
     * @param subqueryBuilder function that builds the subquery
     * @return this finder for chaining
     */
    public Finder<T> whereNotIn(String column, Function<SelectBuilder, SelectBuilder> subqueryBuilder) {
        SelectBuilder subquery = subqueryBuilder.apply(Suprim.select());
        return whereNotIn(column, subquery);
    }

    /**
     * Add WHERE IS NULL condition.
     *
     * @param column column name
     * @return this finder for chaining
     */
    public Finder<T> whereNull(String column) {
        builder.andRaw(column + " IS NULL");
        return this;
    }

    /**
     * Add WHERE IS NOT NULL condition.
     *
     * @param column column name
     * @return this finder for chaining
     */
    public Finder<T> whereNotNull(String column) {
        builder.andRaw(column + " IS NOT NULL");
        return this;
    }

    /**
     * Add raw WHERE clause.
     *
     * @param sql raw SQL condition
     * @return this finder for chaining
     */
    public Finder<T> whereRaw(String sql) {
        builder.andRaw(sql);
        return this;
    }

    // ==================== OR CONDITIONS ====================

    /**
     * Add OR condition (equals).
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .where("status", "active")
     *     .orWhere("status", "pending")
     *     .get();
     * // SQL: WHERE status = ? OR status = ?
     * }</pre>
     *
     * @param column column name
     * @param value  value to match
     * @return this finder for chaining
     */
    public Finder<T> orWhere(String column, Object value) {
        String paramName = nextParamName();
        builder.orRaw(column + " = :" + paramName, Map.of(paramName, normalizeValue(column, value)));
        return this;
    }

    /**
     * Add OR condition with operator.
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .where("age", ">=", 30)
     *     .orWhere("status", "=", "vip")
     *     .get();
     * // SQL: WHERE age >= ? OR status = ?
     * }</pre>
     *
     * @param column   column name
     * @param operator comparison operator
     * @param value    value to compare
     * @return this finder for chaining
     */
    public Finder<T> orWhere(String column, String operator, Object value) {
        String paramName = nextParamName();
        builder.orRaw(column + " " + operator + " :" + paramName, Map.of(paramName, normalizeValue(column, value)));
        return this;
    }

    /**
     * Add OR WHERE IN condition with list of values.
     *
     * <pre>{@code
     * executor.find(User.class)
     *     .where("status", "active")
     *     .orWhereIn("role", List.of("admin", "moderator"))
     *     .get();
     * // SQL: WHERE status = ? OR role IN (?, ?)
     * }</pre>
     *
     * @param column column name
     * @param values list of values
     * @return this finder for chaining
     */
    public Finder<T> orWhereIn(String column, List<?> values) {
        if (values.isEmpty()) {
            builder.orRaw("1 = 0"); // Always false
            return this;
        }
        StringBuilder placeholders = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) {
            String paramName = nextParamName();
            if (i > 0) placeholders.append(", ");
            placeholders.append(":").append(paramName);
            params.put(paramName, normalizeValue(column, values.get(i)));
        }
        builder.orRaw(column + " IN (" + placeholders + ")", params);
        return this;
    }

    /**
     * Add OR WHERE IN condition with a subquery.
     *
     * <pre>{@code
     * executor.find(Workspace.class)
     *     .where("owner_id", accountId)
     *     .orWhereIn("id", Suprim.select(Permission_.RESOURCE_ID)
     *         .from(Permission_.TABLE)
     *         .where(Permission_.ACCOUNT_ID.eq(accountId)))
     *     .get();
     * // SQL: WHERE owner_id = ? OR id IN (SELECT resource_id FROM permissions WHERE account_id = ?)
     * }</pre>
     *
     * @param column   column name
     * @param subquery SelectBuilder representing the subquery
     * @return this finder for chaining
     */
    public Finder<T> orWhereIn(String column, SelectBuilder subquery) {
        QueryResult subqueryResult = subquery.build();
        builder.orRaw(column + " IN (" + subqueryResult.sql() + ")", subqueryResult.parameters());
        return this;
    }

    /**
     * Add OR WHERE IN condition with a subquery builder function.
     *
     * <pre>{@code
     * executor.find(Workspace.class)
     *     .where("owner_id", accountId)
     *     .orWhereIn("id", q -> q
     *         .selectRaw("resource_id")
     *         .from(Table.of("permissions"))
     *         .whereRaw("account_id = :acct", Map.of("acct", accountId)))
     *     .get();
     * }</pre>
     *
     * @param column          column name
     * @param subqueryBuilder function that builds the subquery
     * @return this finder for chaining
     */
    public Finder<T> orWhereIn(String column, Function<SelectBuilder, SelectBuilder> subqueryBuilder) {
        SelectBuilder subquery = subqueryBuilder.apply(Suprim.select());
        return orWhereIn(column, subquery);
    }

    /**
     * Add OR WHERE NOT IN condition with a subquery.
     *
     * @param column   column name
     * @param subquery SelectBuilder representing the subquery
     * @return this finder for chaining
     */
    public Finder<T> orWhereNotIn(String column, SelectBuilder subquery) {
        QueryResult subqueryResult = subquery.build();
        builder.orRaw(column + " NOT IN (" + subqueryResult.sql() + ")", subqueryResult.parameters());
        return this;
    }

    /**
     * Add OR WHERE NOT IN condition with a subquery builder function.
     *
     * @param column          column name
     * @param subqueryBuilder function that builds the subquery
     * @return this finder for chaining
     */
    public Finder<T> orWhereNotIn(String column, Function<SelectBuilder, SelectBuilder> subqueryBuilder) {
        SelectBuilder subquery = subqueryBuilder.apply(Suprim.select());
        return orWhereNotIn(column, subquery);
    }

    /**
     * Add OR WHERE IS NULL condition.
     *
     * @param column column name
     * @return this finder for chaining
     */
    public Finder<T> orWhereNull(String column) {
        builder.orRaw(column + " IS NULL");
        return this;
    }

    /**
     * Add OR WHERE IS NOT NULL condition.
     *
     * @param column column name
     * @return this finder for chaining
     */
    public Finder<T> orWhereNotNull(String column) {
        builder.orRaw(column + " IS NOT NULL");
        return this;
    }

    /**
     * Add raw OR WHERE clause.
     *
     * @param sql raw SQL condition
     * @return this finder for chaining
     */
    public Finder<T> orWhereRaw(String sql) {
        builder.orRaw(sql);
        return this;
    }

    // ==================== GROUPED CONDITIONS ====================

    /**
     * Add grouped WHERE conditions (AND with current conditions).
     * The conditions inside the consumer are wrapped in parentheses.
     *
     * <pre>{@code
     * executor.find(Workspace.class)
     *     .where("is_public", true)
     *     .where(q -> q
     *         .where("owner_id", accountId)
     *         .orWhereIn("id", subq -> subq.select(...)))
     *     .get();
     * // SQL: WHERE is_public = ? AND (owner_id = ? OR id IN (SELECT ...))
     * }</pre>
     *
     * @param groupBuilder consumer that builds the grouped conditions
     * @return this finder for chaining
     */
    public Finder<T> where(Consumer<Finder<T>> groupBuilder) {
        Finder<T> groupFinder = new Finder<>(executor, entityClass);
        groupBuilder.accept(groupFinder);

        // Get predicate directly from the grouped finder's builder
        Predicate groupPredicate = groupFinder.builder.getWhereClause();
        if (Objects.nonNull(groupPredicate)) {
            builder.and(groupPredicate);
        }
        return this;
    }

    /**
     * Add grouped OR conditions.
     * The conditions inside the consumer are wrapped in parentheses and combined with OR.
     *
     * <pre>{@code
     * executor.find(Workspace.class)
     *     .where("is_public", true)
     *     .orWhere(q -> q
     *         .where("owner_id", accountId)
     *         .where("is_archived", false))
     *     .get();
     * // SQL: WHERE is_public = ? OR (owner_id = ? AND is_archived = ?)
     * }</pre>
     *
     * @param groupBuilder consumer that builds the grouped conditions
     * @return this finder for chaining
     */
    public Finder<T> orWhere(Consumer<Finder<T>> groupBuilder) {
        Finder<T> groupFinder = new Finder<>(executor, entityClass);
        groupBuilder.accept(groupFinder);

        // Get predicate directly from the grouped finder's builder
        Predicate groupPredicate = groupFinder.builder.getWhereClause();
        if (Objects.nonNull(groupPredicate)) {
            builder.or(groupPredicate);
        }
        return this;
    }

    // ==================== GROUPING ====================

    /**
     * Group by columns.
     *
     * <pre>{@code
     * executor.find(Order.class)
     *     .groupBy("status", "customer_id")
     *     .get();
     * }</pre>
     *
     * @param columns column names
     * @return this finder for chaining
     */
    public Finder<T> groupBy(String... columns) {
        for (String column : columns) {
            builder.groupByRaw(column);
        }
        return this;
    }

    // ==================== PAGINATION ====================

    /**
     * Limit number of results.
     *
     * @param limit maximum number of results
     * @return this finder for chaining
     */
    public Finder<T> limit(int limit) {
        builder.limit(limit);
        return this;
    }

    /**
     * Skip number of results.
     *
     * @param offset number of rows to skip
     * @return this finder for chaining
     */
    public Finder<T> offset(int offset) {
        builder.offset(offset);
        return this;
    }

    /**
     * Alias for limit().
     *
     * @param count number of results to take
     * @return this finder for chaining
     */
    public Finder<T> take(int count) {
        return limit(count);
    }

    /**
     * Alias for offset().
     *
     * @param count number of results to skip
     * @return this finder for chaining
     */
    public Finder<T> skip(int count) {
        return offset(count);
    }

    // ==================== SOFT DELETES ====================

    /**
     * Include soft-deleted records.
     *
     * @return this finder for chaining
     */
    public Finder<T> withTrashed() {
        builder.withTrashed();
        return this;
    }

    /**
     * Only return soft-deleted records.
     *
     * @return this finder for chaining
     */
    public Finder<T> onlyTrashed() {
        builder.onlyTrashed();
        return this;
    }

    // ==================== TERMINAL OPERATIONS ====================

    /**
     * Execute query and return all results.
     *
     * @return list of entities
     */
    public List<T> get() {
        QueryResult query = builder.build();
        return executor.query(query, EntityMapper.of(entityClass));
    }

    /**
     * Execute query and return first result.
     *
     * @return optional containing first entity, or empty
     */
    public Optional<T> first() {
        builder.limit(1);
        QueryResult query = builder.build();
        return executor.queryOne(query, EntityMapper.of(entityClass));
    }

    /**
     * Execute query and return first result, or throw if not found.
     *
     * @return the first entity
     * @throws sant1ago.dev.suprim.jdbc.exception.NoResultException if not found
     */
    public T firstOrFail() {
        return first().orElseThrow(() ->
            NoResultException.builder()
                .message("No " + entityClass.getSimpleName() + " found matching criteria")
                .build());
    }

    /**
     * Execute query with cursor pagination.
     *
     * <pre>{@code
     * CursorResult<User> first = executor.find(User.class)
     *     .orderBy("id")
     *     .cursor(null, 10);
     *
     * CursorResult<User> next = executor.find(User.class)
     *     .orderBy("id")
     *     .cursor(first.getNextCursor(), 10);
     * }</pre>
     *
     * @param cursor  cursor from previous page (null for first page)
     * @param perPage number of results per page
     * @return cursor result with data and pagination info
     */
    public CursorResult<T> cursor(String cursor, int perPage) {
        return EntityFinder.findAllWithCursor(executor, entityClass, cursor, perPage);
    }

    /**
     * Execute query with offset pagination.
     *
     * @param page    page number (1-based)
     * @param perPage items per page
     * @return paginated result with metadata
     */
    public PaginatedResult<T> paginate(int page, int perPage) {
        return executor.paginate(builder, page, perPage, entityClass);
    }

    /**
     * Count matching records.
     *
     * @return count of matching records
     */
    public long count() {
        return executor.count(builder);
    }

    /**
     * Check if any records exist matching criteria.
     *
     * @return true if at least one record exists
     */
    public boolean exists() {
        builder.limit(1);
        QueryResult query = builder.build();
        return executor.queryOne(query, EntityMapper.of(entityClass)).isPresent();
    }

    /**
     * Get the underlying SelectBuilder for advanced customization.
     *
     * @return the SelectBuilder
     */
    public SelectBuilder toBuilder() {
        return builder;
    }

    // ==================== INTERNAL ====================

    /**
     * Generate unique parameter name for this query.
     * Uses 'fp' prefix to avoid clashing with ParameterContext's 'p' prefix.
     */
    private String nextParamName() {
        return "fp" + (paramCounter++);
    }

    /**
     * Normalize value to match column type from @Column annotation.
     */
    private Object normalizeValue(String column, Object value) {
        if (Objects.isNull(value)) {
            return null;
        }

        SqlType columnType = EntityReflector.getColumnType(entityClass, column);

        return switch (columnType) {
            case UUID -> value instanceof UUID ? value : UUID.fromString(value.toString());
            case VARCHAR, TEXT, CHAR -> value.toString();
            case BIGINT, BIGSERIAL -> value instanceof Long ? value : ((Number) value).longValue();
            case INTEGER, SERIAL -> value instanceof Integer ? value : ((Number) value).intValue();
            case SMALLINT, SMALLSERIAL -> value instanceof Short ? value : ((Number) value).shortValue();
            case DOUBLE_PRECISION -> value instanceof Double ? value : ((Number) value).doubleValue();
            case REAL -> value instanceof Float ? value : ((Number) value).floatValue();
            case BOOLEAN -> value instanceof Boolean ? value : Boolean.parseBoolean(value.toString());
            default -> value;
        };
    }
}
