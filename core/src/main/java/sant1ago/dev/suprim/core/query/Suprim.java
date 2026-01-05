package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.casey.Casey;
import sant1ago.dev.suprim.core.type.Aggregate;
import sant1ago.dev.suprim.core.type.Coalesce;
import sant1ago.dev.suprim.core.type.Expression;
import sant1ago.dev.suprim.core.type.Table;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Entry point for building type-safe SQL queries and accessing entity metadata.
 *
 * <h2>Entity Metadata</h2>
 * <pre>{@code
 * @Entity(table = "users", schema = "public")
 * public class User {
 *     @Column(name = "id") private Long id;
 *     @Column(name = "email") private String email;
 * }
 *
 * Suprim.table(User.class)   // "users"
 * Suprim.schema(User.class)  // "public"
 * Suprim.column(User.class, "email")  // "email"
 * }</pre>
 *
 * <h2>SELECT</h2>
 * <pre>{@code
 * QueryResult query = Suprim.select(User_.ID, User_.EMAIL)
 *     .from(User_.TABLE)
 *     .where(User_.EMAIL.eq("test@example.com"))
 *     .build();
 * }</pre>
 */
public final class Suprim {

    private static final Map<Class<?>, Entity> entityCache = new ConcurrentHashMap<>();
    private static final Map<String, String> columnCache = new ConcurrentHashMap<>();

    private Suprim() {
        // Utility class
    }

    // ==================== ENTITY METADATA ====================

    /**
     * Get table name for entity class.
     *
     * @param entityClass class annotated with @Entity
     * @return table name from annotation, or snake_case of class name if not specified
     */
    public static String table(Class<?> entityClass) {
        Entity entity = getEntity(entityClass);
        String table = entity.table();
        return table.isEmpty() ? Casey.toSnakeCase(entityClass.getSimpleName()) : table;
    }

    /**
     * Get schema name for entity class.
     *
     * @param entityClass class annotated with @Entity
     * @return schema name from annotation, or empty string if not specified
     */
    public static String schema(Class<?> entityClass) {
        return getEntity(entityClass).schema();
    }

    /**
     * Get default eager loads for entity class.
     *
     * @param entityClass class annotated with @Entity
     * @return array of relation field names to eager load by default
     */
    public static String[] defaultEagerLoads(Class<?> entityClass) {
        return getEntity(entityClass).with();
    }

    /**
     * Get column name for a field in entity class.
     *
     * @param entityClass class annotated with @Entity
     * @param fieldName Java field name
     * @return column name from @Column annotation, or snake_case of field name
     */
    public static String column(Class<?> entityClass, String fieldName) {
        String cacheKey = entityClass.getName() + "." + fieldName;
        return columnCache.computeIfAbsent(cacheKey, k -> {
            try {
                Field field = entityClass.getDeclaredField(fieldName);
                Column col = field.getAnnotation(Column.class);
                if (nonNull(col) && !col.name().isEmpty()) {
                    return col.name();
                }
                return Casey.toSnakeCase(fieldName);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Field not found: " + fieldName + " in " + entityClass.getName());
            }
        });
    }

    /**
     * Get column type for a field in entity class.
     *
     * @param entityClass class annotated with @Entity
     * @param fieldName Java field name
     * @return SQL type from @Column annotation
     */
    public static String columnType(Class<?> entityClass, String fieldName) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            Column col = field.getAnnotation(Column.class);
            if (nonNull(col)) {
                // Custom definition takes priority
                if (!col.definition().isEmpty()) {
                    return col.definition();
                }
                SqlType sqlType = col.type();
                if (!sqlType.isAuto()) {
                    return buildSqlTypeString(sqlType, col);
                }
            }
            return "";
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field not found: " + fieldName + " in " + entityClass.getName());
        }
    }

    /**
     * Build SQL type string with length/precision/scale if applicable.
     */
    private static String buildSqlTypeString(SqlType sqlType, Column col) {
        String base = sqlType.getSql();

        // Handle length for VARCHAR, CHAR
        if (sqlType.supportsLength()) {
            int length = col.length();
            if (length > 0) {
                return base + "(" + length + ")";
            } else if (length == 0 && sqlType == SqlType.VARCHAR) {
                return base + "(255)";
            }
        }

        // Handle precision/scale for NUMERIC, DECIMAL
        if (sqlType.supportsPrecision()) {
            int precision = col.precision();
            int scale = col.scale();
            if (precision > 0) {
                if (scale > 0) {
                    return base + "(" + precision + ", " + scale + ")";
                }
                return base + "(" + precision + ")";
            }
        }

        return base;
    }

    /**
     * Get column name using getter method reference.
     *
     * <pre>{@code
     * Suprim.column(User::getEmail)  // "email"
     * Suprim.column(User::getCreatedAt)  // "created_at"
     * }</pre>
     *
     * @param getter method reference to getter (e.g., User::getEmail)
     * @return column name from @Column annotation or snake_case of field name
     */
    public static <T, R> String column(Getter<T, R> getter) {
        SerializedLambda lambda = getSerializedLambda(getter);
        String methodName = lambda.getImplMethodName();
        String fieldName = extractFieldName(methodName);
        String implClass = lambda.getImplClass();

        Class<?> entityClass = resolveClass(implClass);
        return column(entityClass, fieldName);
    }

    /**
     * Resolves a class from its internal name (with / separators).
     * Package-private for testing.
     */
    static Class<?> resolveClass(String internalClassName) {
        try {
            return Class.forName(internalClassName.replace('/', '.'));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find class: " + internalClassName, e);
        }
    }

    /**
     * Serializable function interface for getter method references.
     *
     * @param <T> the input type
     * @param <R> the result type
     */
    @FunctionalInterface
    public interface Getter<T, R> extends Function<T, R>, Serializable {
    }

    private static SerializedLambda getSerializedLambda(Serializable lambda) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(lambda);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not extract lambda information", e);
        }
    }

    private static String extractFieldName(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        return methodName;
    }

    private static Entity getEntity(Class<?> entityClass) {
        return entityCache.computeIfAbsent(entityClass, clazz -> {
            Entity entity = clazz.getAnnotation(Entity.class);
            if (isNull(entity)) {
                throw new IllegalArgumentException(clazz.getName() + " is not annotated with @Entity");
            }
            return entity;
        });
    }

    // ==================== SELECT ====================

    /**
     * Start SELECT query with specified columns/expressions.
     * Accepts Column, AliasedColumn (via .as()), or any Expression.
     * <pre>{@code
     * Suprim.select(User_.ID, User_.EMAIL.as("email_alias"), User_.NAME)
     * }</pre>
     */
    public static SelectBuilder select(Expression<?>... expressions) {
        return new SelectBuilder(Arrays.asList(expressions));
    }

    /**
     * Start SELECT * query.
     */
    public static SelectBuilder selectAll() {
        return new SelectBuilder(Collections.emptyList());
    }

    /**
     * Start SELECT query with raw SQL expression.
     * <pre>{@code
     * Suprim.selectRaw("COUNT(*) as total, MAX(age) as max_age")
     *     .from(User_.TABLE)
     *     .build();
     * }</pre>
     */
    public static SelectBuilder selectRaw(String rawSql) {
        return new SelectBuilder(Collections.emptyList()).selectRaw(rawSql);
    }

    /**
     * Start SELECT query from table (shortcut for selectAll().from(table)).
     */
    public static SelectBuilder from(Table<?> table) {
        return new SelectBuilder(Collections.emptyList()).from(table);
    }

    // ==================== INSERT ====================

    /**
     * Start INSERT INTO query.
     */
    public static <T> InsertBuilder<T> insertInto(Table<T> table) {
        return new InsertBuilder<>(table);
    }

    /**
     * Start batch INSERT INTO query for multiple rows.
     *
     * <pre>{@code
     * QueryResult query = Suprim.batchInsertInto(User_.TABLE)
     *     .columns(User_.ID, User_.NAME, User_.EMAIL)
     *     .values(Map.of("id", uuid1, "name", "John", "email", "john@ex.com"))
     *     .values(Map.of("id", uuid2, "name", "Jane", "email", "jane@ex.com"))
     *     .returning(User_.ID)
     *     .build(dialect);
     * }</pre>
     *
     * @param table the table to insert into
     * @param <T>   entity type
     * @return BatchInsertBuilder for chaining
     */
    public static <T> BatchInsertBuilder<T> batchInsertInto(Table<T> table) {
        return new BatchInsertBuilder<>(table);
    }

    // ==================== UPSERT ====================

    /**
     * Start UPSERT (INSERT ... ON CONFLICT) query.
     *
     * <p>Generates dialect-specific SQL:
     * <ul>
     *   <li>PostgreSQL: {@code INSERT ... ON CONFLICT (id) DO UPDATE SET ...}</li>
     *   <li>MySQL: {@code INSERT ... ON DUPLICATE KEY UPDATE ...}</li>
     * </ul>
     *
     * <pre>{@code
     * QueryResult query = Suprim.upsertInto(User_.TABLE)
     *     .columns(User_.ID, User_.EMAIL, User_.NAME)
     *     .values(Map.of("id", uuid, "email", "test@ex.com", "name", "Test"))
     *     .onConflict(User_.ID)
     *     .doUpdate(User_.EMAIL, User_.NAME)
     *     .build(dialect);
     *
     * // Or ignore duplicates:
     * QueryResult query = Suprim.upsertInto(User_.TABLE)
     *     .columns(User_.EMAIL)
     *     .values(Map.of("email", "test@ex.com"))
     *     .onConflict(User_.EMAIL)
     *     .doNothing()
     *     .build(dialect);
     * }</pre>
     *
     * @param table the table to upsert into
     * @param <T>   entity type
     * @return UpsertBuilder for chaining
     */
    public static <T> UpsertBuilder<T> upsertInto(Table<T> table) {
        return new UpsertBuilder<>(table);
    }

    // ==================== UPDATE ====================

    /**
     * Start UPDATE query.
     */
    public static UpdateBuilder update(Table<?> table) {
        return new UpdateBuilder(table);
    }

    // ==================== DELETE ====================

    /**
     * Start DELETE FROM query.
     */
    public static DeleteBuilder deleteFrom(Table<?> table) {
        return new DeleteBuilder(table);
    }

    // ==================== BULK SOFT DELETE / RESTORE ====================

    /**
     * Build a bulk soft delete query (sets deleted_at = NOW()).
     * Use with .where() to specify which records to soft delete.
     *
     * <pre>{@code
     * // Soft delete all inactive users
     * QueryResult query = Suprim.softDelete(User_.TABLE, "deleted_at")
     *     .where(User_.IS_ACTIVE.eq(false))
     *     .build();
     *
     * tx.execute(query);
     * }</pre>
     *
     * @param table the table to update
     * @param deletedAtColumn the name of the deleted_at column
     * @return UpdateBuilder for chaining
     */
    public static UpdateBuilder softDelete(Table<?> table, String deletedAtColumn) {
        return new UpdateBuilder(table)
                .setRaw(deletedAtColumn, sant1ago.dev.suprim.core.type.Fn.now());
    }

    /**
     * Build a bulk restore query (sets deleted_at = NULL).
     * Use with .where() to specify which records to restore.
     *
     * <pre>{@code
     * // Restore all users deleted in the last 30 days
     * QueryResult query = Suprim.restore(User_.TABLE, "deleted_at")
     *     .whereRaw("deleted_at > NOW() - INTERVAL '30 days'")
     *     .build();
     *
     * tx.execute(query);
     * }</pre>
     *
     * @param table the table to update
     * @param deletedAtColumn the name of the deleted_at column
     * @return UpdateBuilder for chaining
     */
    public static UpdateBuilder restore(Table<?> table, String deletedAtColumn) {
        return new UpdateBuilder(table)
                .setNull(deletedAtColumn);
    }

    // ==================== AGGREGATE FUNCTIONS ====================

    /**
     * COUNT(*) aggregate function.
     * <pre>{@code
     * Suprim.count().as("total")
     * Suprim.count().filter(User_.IS_ACTIVE.eq(true)).as("active_count")
     * }</pre>
     */
    public static Aggregate count() {
        return Aggregate.count();
    }

    /**
     * COUNT(column) aggregate function.
     * <pre>{@code
     * Suprim.count(Message_.RATING).as("rated_count")
     * }</pre>
     */
    public static Aggregate count(Expression<?> column) {
        return Aggregate.count(column);
    }

    /**
     * SUM(column) aggregate function.
     * <pre>{@code
     * Suprim.sum(Order_.AMOUNT).as("total_revenue")
     * }</pre>
     */
    public static Aggregate sum(Expression<?> column) {
        return Aggregate.sum(column);
    }

    /**
     * AVG(column) aggregate function.
     * <pre>{@code
     * Suprim.avg(Product_.RATING).as("avg_rating")
     * }</pre>
     */
    public static Aggregate avg(Expression<?> column) {
        return Aggregate.avg(column);
    }

    /**
     * MIN(column) aggregate function.
     * <pre>{@code
     * Suprim.min(Order_.CREATED_AT).as("first_order")
     * }</pre>
     */
    public static Aggregate min(Expression<?> column) {
        return Aggregate.min(column);
    }

    /**
     * MAX(column) aggregate function.
     * <pre>{@code
     * Suprim.max(Order_.AMOUNT).as("largest_order")
     * }</pre>
     */
    public static Aggregate max(Expression<?> column) {
        return Aggregate.max(column);
    }

    // ==================== COALESCE ====================

    /**
     * COALESCE function - returns first non-null value.
     * <pre>{@code
     * Suprim.coalesce(User_.NICKNAME, User_.NAME, User_.EMAIL).as("display_name")
     * }</pre>
     */
    public static <V> Coalesce<V> coalesce(Expression<?>... expressions) {
        return Coalesce.of(expressions);
    }

    /**
     * COALESCE with column and literal fallback.
     * <pre>{@code
     * Suprim.coalesce(User_.NAME, "Unknown").as("display_name")
     * }</pre>
     */
    public static <V> Coalesce<V> coalesce(Expression<V> column, V fallback) {
        return Coalesce.of(column, fallback);
    }
}
