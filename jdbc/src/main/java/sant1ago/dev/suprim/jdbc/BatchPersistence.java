package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.CreationTimestamp;
import sant1ago.dev.suprim.annotation.entity.JsonbColumn;
import sant1ago.dev.suprim.annotation.entity.TimestampAction;
import sant1ago.dev.suprim.annotation.entity.UpdateTimestamp;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.IdGenerator;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.casey.Casey;
import sant1ago.dev.suprim.core.util.UUIDUtils;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.jdbc.exception.PersistenceException;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles batch entity persistence with efficient multi-row INSERT.
 *
 * <p>Generates optimized SQL with multiple VALUES rows:
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
 * List<User> users = List.of(user1, user2, user3);
 * List<User> saved = BatchPersistence.saveAll(users, connection, dialect);
 * // All entities now have IDs set
 * }</pre>
 *
 * <p>Supports all ID generation strategies:
 * <ul>
 *   <li>UUID_V4 / UUID_V7 - Generated in application before insert</li>
 *   <li>IDENTITY - Database generates (SERIAL/AUTO_INCREMENT)</li>
 *   <li>UUID_DB - Database generates (gen_random_uuid())</li>
 *   <li>Custom IdGenerator - Your implementation</li>
 * </ul>
 */
public final class BatchPersistence {

    private static final int DEFAULT_BATCH_SIZE = 500;
    private static final int MAX_BATCH_SIZE = 1000;
    private static final Map<Class<? extends IdGenerator<?>>, IdGenerator<?>> GENERATOR_CACHE = new ConcurrentHashMap<>();

    private BatchPersistence() {
        // Utility class
    }

    /**
     * Save multiple entities in efficient batch INSERT operations.
     * IDs are generated and set on all entities.
     *
     * @param entities   list of entities to save
     * @param connection the database connection
     * @param dialect    the SQL dialect
     * @param <T>        entity type
     * @return the saved entities with IDs set
     */
    public static <T> List<T> saveAll(List<T> entities, Connection connection, SqlDialect dialect) {
        return saveAll(entities, connection, dialect, DEFAULT_BATCH_SIZE);
    }

    /**
     * Save multiple entities with custom batch size.
     *
     * @param entities   list of entities to save
     * @param connection the database connection
     * @param dialect    the SQL dialect
     * @param batchSize  maximum entities per batch (1-1000)
     * @param <T>        entity type
     * @return the saved entities with IDs set
     */
    public static <T> List<T> saveAll(List<T> entities, Connection connection, SqlDialect dialect, int batchSize) {
        if (Objects.isNull(entities) || entities.isEmpty()) {
            return new ArrayList<>();
        }

        Objects.requireNonNull(connection, "Connection cannot be null");
        Objects.requireNonNull(dialect, "Dialect cannot be null");

        int effectiveBatchSize = Math.min(Math.max(1, batchSize), MAX_BATCH_SIZE);
        List<T> result = new ArrayList<>(entities.size());

        // Process in batches
        for (int i = 0; i < entities.size(); i += effectiveBatchSize) {
            int end = Math.min(i + effectiveBatchSize, entities.size());
            List<T> batch = entities.subList(i, end);
            List<T> saved = saveBatch(batch, connection, dialect);
            result.addAll(saved);
        }

        return result;
    }

    /**
     * Save a single batch of entities.
     */
    private static <T> List<T> saveBatch(List<T> entities, Connection connection, SqlDialect dialect) {
        if (entities.isEmpty()) {
            return entities;
        }

        T firstEntity = entities.get(0);
        Class<?> entityClass = firstEntity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        // Determine if we need database-generated IDs
        boolean needsDbGeneratedId = needsDatabaseGeneratedId(entities, idMeta);

        if (needsDbGeneratedId && idMeta.isDatabaseGenerated()) {
            return saveBatchWithGeneratedKeys(entities, entityMeta, idMeta, connection, dialect);
        }

        // Generate IDs for application-generated strategies
        if (idMeta.isApplicationGenerated()) {
            generateIdsForEntities(entities, idMeta);
        }

        return saveBatchSimple(entities, entityMeta, idMeta, connection, dialect);
    }

    /**
     * Check if any entity needs a database-generated ID.
     */
    private static <T> boolean needsDatabaseGeneratedId(List<T> entities, EntityReflector.IdMeta idMeta) {
        if (idMeta.isManual()) {
            return false;
        }
        for (T entity : entities) {
            Object id = EntityReflector.getIdOrNull(entity);
            if (Objects.isNull(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate IDs for entities that don't have one.
     */
    private static <T> void generateIdsForEntities(List<T> entities, EntityReflector.IdMeta idMeta) {
        for (T entity : entities) {
            Object existingId = EntityReflector.getIdOrNull(entity);
            if (Objects.isNull(existingId)) {
                Object generatedId = generateId(idMeta);
                EntityReflector.setId(entity, generatedId);
            }
        }
    }

    /**
     * Generate an ID value based on the strategy.
     */
    private static Object generateId(EntityReflector.IdMeta idMeta) {
        if (idMeta.hasCustomGenerator()) {
            IdGenerator<?> generator = getOrCreateGenerator(idMeta.generatorClass());
            return generator.generate();
        }

        UUID uuid = switch (idMeta.strategy()) {
            case UUID_V4 -> UUIDUtils.v4();
            case UUID_V7 -> UUIDUtils.v7();
            default -> throw new IllegalStateException("Cannot generate ID for strategy: " + idMeta.strategy());
        };

        return idMeta.fieldType() == String.class ? uuid.toString() : uuid;
    }

    /**
     * Get or create a cached generator instance.
     */
    private static IdGenerator<?> getOrCreateGenerator(Class<? extends IdGenerator<?>> generatorClass) {
        return GENERATOR_CACHE.computeIfAbsent(generatorClass, clazz -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Cannot instantiate ID generator: " + clazz.getName(), e);
            }
        });
    }

    /**
     * Save batch with database-generated keys (IDENTITY, UUID_DB).
     * Uses RETURNING for PostgreSQL or GENERATED_KEYS for MySQL.
     */
    private static <T> List<T> saveBatchWithGeneratedKeys(
        List<T> entities,
        EntityReflector.EntityMeta entityMeta,
        EntityReflector.IdMeta idMeta,
        Connection connection,
        SqlDialect dialect
    ) {
        // Build column metadata from first entity
        List<ColumnMeta> columnMetas = buildColumnMetas(entities.get(0), idMeta, true);
        if (columnMetas.isEmpty()) {
            throw new PersistenceException("No columns to insert", entities.get(0).getClass());
        }

        String sql = buildBatchInsertSql(entityMeta, idMeta, columnMetas, entities.size(), dialect, true);
        List<Object> allParams = collectParameters(entities, columnMetas, idMeta, true);

        boolean supportsReturning = dialect.capabilities().supportsReturning();

        try {
            if (supportsReturning) {
                // PostgreSQL: use RETURNING clause
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    setParameters(ps, allParams);
                    try (ResultSet rs = ps.executeQuery()) {
                        int idx = 0;
                        while (rs.next() && idx < entities.size()) {
                            Object generatedId = rs.getObject(1);
                            generatedId = convertIdType(generatedId, idMeta.fieldType());
                            EntityReflector.setId(entities.get(idx), generatedId);
                            idx++;
                        }
                    }
                }
            } else {
                // MySQL: use RETURN_GENERATED_KEYS
                try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    setParameters(ps, allParams);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        int idx = 0;
                        while (rs.next() && idx < entities.size()) {
                            Object generatedId = rs.getObject(1);
                            generatedId = convertIdType(generatedId, idMeta.fieldType());
                            EntityReflector.setId(entities.get(idx), generatedId);
                            idx++;
                        }
                    }
                }
            }
            return entities;
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to batch save entities: " + e.getMessage(),
                entities.get(0).getClass(),
                e
            );
        }
    }

    /**
     * Save batch without needing generated keys.
     */
    private static <T> List<T> saveBatchSimple(
        List<T> entities,
        EntityReflector.EntityMeta entityMeta,
        EntityReflector.IdMeta idMeta,
        Connection connection,
        SqlDialect dialect
    ) {
        List<ColumnMeta> columnMetas = buildColumnMetas(entities.get(0), idMeta, false);
        if (columnMetas.isEmpty()) {
            throw new PersistenceException("No columns to insert", entities.get(0).getClass());
        }

        String sql = buildBatchInsertSql(entityMeta, idMeta, columnMetas, entities.size(), dialect, false);
        List<Object> allParams = collectParameters(entities, columnMetas, idMeta, false);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            setParameters(ps, allParams);
            ps.executeUpdate();
            return entities;
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to batch save entities: " + e.getMessage(),
                entities.get(0).getClass(),
                e
            );
        }
    }

    /**
     * Column metadata for batch operations.
     */
    private record ColumnMeta(String columnName, String fieldName, Field field, SqlType sqlType) {}

    /**
     * Build column metadata from entity.
     */
    private static List<ColumnMeta> buildColumnMetas(Object entity, EntityReflector.IdMeta idMeta, boolean skipId) {
        List<ColumnMeta> metas = new ArrayList<>();
        Class<?> entityClass = entity.getClass();

        for (Field field : getAllFields(entityClass)) {
            Column column = field.getAnnotation(Column.class);
            boolean hasCreationTimestamp = field.isAnnotationPresent(CreationTimestamp.class);
            boolean hasUpdateTimestamp = field.isAnnotationPresent(UpdateTimestamp.class);

            if (Objects.isNull(column) && !hasCreationTimestamp && !hasUpdateTimestamp) {
                continue;
            }

            String columnName;
            if (Objects.nonNull(column) && !column.name().isEmpty()) {
                columnName = column.name();
            } else if (hasCreationTimestamp) {
                columnName = field.getAnnotation(CreationTimestamp.class).column();
            } else if (hasUpdateTimestamp) {
                columnName = field.getAnnotation(UpdateTimestamp.class).column();
            } else {
                columnName = Casey.toSnakeCase(field.getName());
            }

            if (skipId && columnName.equals(idMeta.columnName())) {
                continue;
            }

            SqlType sqlType = Objects.nonNull(column) ? column.type() : SqlType.AUTO;
            metas.add(new ColumnMeta(columnName, field.getName(), field, sqlType));
        }

        // Add ID column if not skipping and entity has one
        if (!skipId) {
            Object idValue = EntityReflector.getIdOrNull(entity);
            if (Objects.nonNull(idValue)) {
                Field idField = findIdField(entityClass);
                if (Objects.nonNull(idField)) {
                    metas.add(0, new ColumnMeta(idMeta.columnName(), idField.getName(), idField, idMeta.columnType()));
                }
            }
        }

        return metas;
    }

    /**
     * Find the ID field in an entity class.
     */
    private static Field findIdField(Class<?> entityClass) {
        for (Field field : getAllFields(entityClass)) {
            if (field.isAnnotationPresent(sant1ago.dev.suprim.annotation.entity.Id.class)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Collect all parameter values from entities.
     */
    private static <T> List<Object> collectParameters(
        List<T> entities,
        List<ColumnMeta> columnMetas,
        EntityReflector.IdMeta idMeta,
        boolean skipId
    ) {
        List<Object> params = new ArrayList<>();
        Instant now = Instant.now();

        for (T entity : entities) {
            for (ColumnMeta meta : columnMetas) {
                Object value = ReflectionUtils.getFieldValue(entity, meta.fieldName());

                // Handle timestamps
                if (meta.field().isAnnotationPresent(CreationTimestamp.class)) {
                    CreationTimestamp ts = meta.field().getAnnotation(CreationTimestamp.class);
                    if (ts.onCreation() == TimestampAction.NOW ||
                        (ts.onCreation() == TimestampAction.IF_NULL && Objects.isNull(value))) {
                        value = convertTimestampToFieldType(now, meta.field().getType());
                        ReflectionUtils.setFieldValue(entity, meta.fieldName(), value);
                    }
                }

                if (meta.field().isAnnotationPresent(UpdateTimestamp.class)) {
                    UpdateTimestamp ts = meta.field().getAnnotation(UpdateTimestamp.class);
                    if (ts.onModification() == TimestampAction.NOW ||
                        (ts.onModification() == TimestampAction.IF_NULL && Objects.isNull(value))) {
                        value = convertTimestampToFieldType(now, meta.field().getType());
                        ReflectionUtils.setFieldValue(entity, meta.fieldName(), value);
                    }
                }

                // Convert value types
                if (Objects.nonNull(value)) {
                    if (meta.field().isAnnotationPresent(JsonbColumn.class)) {
                        value = EntityReflector.toJsonbObject(value);
                    }
                    if (meta.sqlType() == SqlType.UUID && value instanceof String str) {
                        value = UUID.fromString(str);
                    }
                    if (meta.sqlType() == SqlType.VECTOR) {
                        value = EntityReflector.toVectorObject(value);
                    }
                }

                params.add(value);
            }
        }

        return params;
    }

    /**
     * Build batch INSERT SQL statement.
     */
    private static String buildBatchInsertSql(
        EntityReflector.EntityMeta entityMeta,
        EntityReflector.IdMeta idMeta,
        List<ColumnMeta> columnMetas,
        int rowCount,
        SqlDialect dialect,
        boolean returningId
    ) {
        StringBuilder sql = new StringBuilder();

        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        sql.append("INSERT INTO ").append(tableName).append(" (");

        // Column names
        StringJoiner columnJoiner = new StringJoiner(", ");
        for (ColumnMeta meta : columnMetas) {
            columnJoiner.add(dialect.quoteIdentifier(meta.columnName()));
        }

        // Add ID column for UUID_DB strategy
        if (returningId && idMeta.strategy() == GenerationType.UUID_DB) {
            columnJoiner.add(dialect.quoteIdentifier(idMeta.columnName()));
        }

        sql.append(columnJoiner).append(") VALUES ");

        // Build value rows
        String rowPlaceholder = buildRowPlaceholder(columnMetas.size(), idMeta, returningId, dialect);
        StringJoiner rowJoiner = new StringJoiner(", ");
        for (int i = 0; i < rowCount; i++) {
            rowJoiner.add(rowPlaceholder);
        }
        sql.append(rowJoiner);

        // RETURNING clause for PostgreSQL
        if (returningId && dialect.capabilities().supportsReturning()) {
            sql.append(" RETURNING ").append(dialect.quoteIdentifier(idMeta.columnName()));
        }

        return sql.toString();
    }

    /**
     * Build placeholder for a single row.
     */
    private static String buildRowPlaceholder(int columnCount, EntityReflector.IdMeta idMeta, boolean returningId, SqlDialect dialect) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (int i = 0; i < columnCount; i++) {
            joiner.add("?");
        }

        if (returningId && idMeta.strategy() == GenerationType.UUID_DB) {
            String uuidFn = dialect.getName().contains("MySQL") ? "UUID()" : "gen_random_uuid()";
            joiner.add(uuidFn);
        }

        return joiner.toString();
    }

    /**
     * Set parameters on prepared statement.
     */
    private static void setParameters(PreparedStatement ps, List<Object> values) throws SQLException {
        int i = 1;
        for (Object value : values) {
            if (value instanceof Enum<?> enumValue) {
                ps.setString(i++, enumValue.name());
            } else {
                ps.setObject(i++, value);
            }
        }
    }

    /**
     * Convert generated ID to expected field type.
     */
    private static Object convertIdType(Object value, Class<?> targetType) {
        if (Objects.isNull(value) || targetType.isInstance(value)) {
            return value;
        }

        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number n) {
                return n.longValue();
            }
        }

        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number n) {
                return n.intValue();
            }
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == UUID.class && value instanceof String s) {
            return UUID.fromString(s);
        }

        return value;
    }

    /**
     * Convert timestamp to field type.
     */
    private static Object convertTimestampToFieldType(Object value, Class<?> targetType) {
        if (Objects.isNull(value)) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        Instant instant;
        if (value instanceof Instant i) {
            instant = i;
        } else if (value instanceof Timestamp ts) {
            instant = ts.toInstant();
        } else if (value instanceof LocalDateTime ldt) {
            instant = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant();
        } else {
            return value;
        }

        if (targetType == LocalDateTime.class) {
            return LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        } else if (targetType == Instant.class) {
            return instant;
        } else if (targetType == java.time.OffsetDateTime.class) {
            return java.time.OffsetDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        } else if (targetType == Timestamp.class) {
            return Timestamp.from(instant);
        }

        return instant;
    }

    /**
     * Get all fields including inherited ones.
     */
    private static Field[] getAllFields(Class<?> clazz) {
        if (Objects.isNull(clazz) || clazz == Object.class) {
            return new Field[0];
        }
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }
}
