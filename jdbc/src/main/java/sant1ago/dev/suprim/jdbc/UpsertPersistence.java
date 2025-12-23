package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.CreationTimestamp;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Handles entity upsert (INSERT ON CONFLICT) operations.
 *
 * <p>Generates dialect-specific SQL:
 * <ul>
 *   <li>PostgreSQL: {@code INSERT INTO t (...) VALUES (...) ON CONFLICT (id) DO UPDATE SET ...}</li>
 *   <li>MySQL: {@code INSERT INTO t (...) VALUES (...) ON DUPLICATE KEY UPDATE ...}</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * User user = new User();
 * user.setEmail("test@example.com");
 * user.setName("Test");
 *
 * // Upsert with ID as conflict column, update all other columns
 * User saved = UpsertPersistence.upsert(user, connection, dialect, new String[]{"id"}, null);
 *
 * // Upsert with email as conflict, update only name
 * User saved = UpsertPersistence.upsert(user, connection, dialect, new String[]{"email"}, new String[]{"name"});
 * }</pre>
 */
public final class UpsertPersistence {

    private static final Map<Class<? extends IdGenerator<?>>, IdGenerator<?>> GENERATOR_CACHE = new ConcurrentHashMap<>();

    private UpsertPersistence() {
        // Utility class
    }

    /**
     * Upsert a single entity.
     *
     * @param entity          the entity to upsert
     * @param connection      the database connection
     * @param dialect         the SQL dialect
     * @param conflictColumns columns that define the conflict (typically PK or unique constraint)
     * @param updateColumns   columns to update on conflict (null = all non-conflict columns)
     * @param <T>             entity type
     * @return the upserted entity
     */
    public static <T> T upsert(T entity, Connection connection, SqlDialect dialect,
                               String[] conflictColumns, String[] updateColumns) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(connection, "Connection cannot be null");
        Objects.requireNonNull(dialect, "Dialect cannot be null");
        Objects.requireNonNull(conflictColumns, "Conflict columns cannot be null");

        if (conflictColumns.length == 0) {
            throw new IllegalArgumentException("At least one conflict column is required");
        }

        Class<?> entityClass = entity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        // Generate ID if needed
        if (idMeta.isApplicationGenerated()) {
            Object existingId = EntityReflector.getIdOrNull(entity);
            if (Objects.isNull(existingId)) {
                Object generatedId = generateId(idMeta);
                EntityReflector.setId(entity, generatedId);
            }
        }

        // Build column metadata
        List<ColumnMeta> columnMetas = buildColumnMetas(entity, idMeta);
        if (columnMetas.isEmpty()) {
            throw new PersistenceException("No columns to upsert", entityClass);
        }

        // Determine update columns (all non-conflict if not specified)
        List<String> conflictList = Arrays.asList(conflictColumns);
        List<String> updateList;
        if (Objects.isNull(updateColumns) || updateColumns.length == 0) {
            updateList = new ArrayList<>();
            for (ColumnMeta meta : columnMetas) {
                if (!conflictList.contains(meta.columnName())) {
                    updateList.add(meta.columnName());
                }
            }
        } else {
            updateList = Arrays.asList(updateColumns);
        }

        String sql = buildUpsertSql(entityMeta, columnMetas, conflictList, updateList, dialect);
        List<Object> params = collectParameters(entity, columnMetas);

        boolean supportsReturning = dialect.capabilities().supportsReturning();

        try {
            if (supportsReturning && idMeta.isDatabaseGenerated()) {
                // PostgreSQL with RETURNING
                try (PreparedStatement ps = connection.prepareStatement(sql + " RETURNING " + dialect.quoteIdentifier(idMeta.columnName()))) {
                    setParameters(ps, params);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Object generatedId = rs.getObject(1);
                            generatedId = convertIdType(generatedId, idMeta.fieldType());
                            EntityReflector.setId(entity, generatedId);
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    setParameters(ps, params);
                    ps.executeUpdate();
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to upsert entity: " + e.getMessage(),
                entityClass,
                e
            );
        }
    }

    /**
     * Upsert multiple entities in batch.
     *
     * @param entities        list of entities to upsert
     * @param connection      the database connection
     * @param dialect         the SQL dialect
     * @param conflictColumns columns that define the conflict
     * @param updateColumns   columns to update on conflict (null = all non-conflict columns)
     * @param <T>             entity type
     * @return the upserted entities
     */
    public static <T> List<T> upsertAll(List<T> entities, Connection connection, SqlDialect dialect,
                                        String[] conflictColumns, String[] updateColumns) {
        if (Objects.isNull(entities) || entities.isEmpty()) {
            return new ArrayList<>();
        }

        Objects.requireNonNull(connection, "Connection cannot be null");
        Objects.requireNonNull(dialect, "Dialect cannot be null");
        Objects.requireNonNull(conflictColumns, "Conflict columns cannot be null");

        if (conflictColumns.length == 0) {
            throw new IllegalArgumentException("At least one conflict column is required");
        }

        T firstEntity = entities.get(0);
        Class<?> entityClass = firstEntity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        // Generate IDs for all entities if needed
        if (idMeta.isApplicationGenerated()) {
            for (T entity : entities) {
                Object existingId = EntityReflector.getIdOrNull(entity);
                if (Objects.isNull(existingId)) {
                    Object generatedId = generateId(idMeta);
                    EntityReflector.setId(entity, generatedId);
                }
            }
        }

        // Build column metadata from first entity
        List<ColumnMeta> columnMetas = buildColumnMetas(firstEntity, idMeta);
        if (columnMetas.isEmpty()) {
            throw new PersistenceException("No columns to upsert", entityClass);
        }

        // Determine update columns
        List<String> conflictList = Arrays.asList(conflictColumns);
        List<String> updateList;
        if (Objects.isNull(updateColumns) || updateColumns.length == 0) {
            updateList = new ArrayList<>();
            for (ColumnMeta meta : columnMetas) {
                if (!conflictList.contains(meta.columnName())) {
                    updateList.add(meta.columnName());
                }
            }
        } else {
            updateList = Arrays.asList(updateColumns);
        }

        String sql = buildBatchUpsertSql(entityMeta, columnMetas, conflictList, updateList, entities.size(), dialect);
        List<Object> allParams = new ArrayList<>();
        for (T entity : entities) {
            allParams.addAll(collectParameters(entity, columnMetas));
        }

        boolean supportsReturning = dialect.capabilities().supportsReturning();

        try {
            if (supportsReturning && idMeta.isDatabaseGenerated()) {
                String sqlWithReturning = sql + " RETURNING " + dialect.quoteIdentifier(idMeta.columnName());
                try (PreparedStatement ps = connection.prepareStatement(sqlWithReturning)) {
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
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    setParameters(ps, allParams);
                    ps.executeUpdate();
                }
            }
            return entities;
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to batch upsert entities: " + e.getMessage(),
                entityClass,
                e
            );
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

    private static IdGenerator<?> getOrCreateGenerator(Class<? extends IdGenerator<?>> generatorClass) {
        return GENERATOR_CACHE.computeIfAbsent(generatorClass, clazz -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot instantiate ID generator: " + clazz.getName(), e);
            }
        });
    }

    /**
     * Column metadata for upsert operations.
     */
    private record ColumnMeta(String columnName, String fieldName, Field field, SqlType sqlType) {}

    /**
     * Build column metadata from entity.
     */
    private static List<ColumnMeta> buildColumnMetas(Object entity, EntityReflector.IdMeta idMeta) {
        List<ColumnMeta> metas = new ArrayList<>();
        Class<?> entityClass = entity.getClass();

        // Add ID column first
        Field idField = findIdField(entityClass);
        if (Objects.nonNull(idField)) {
            metas.add(new ColumnMeta(idMeta.columnName(), idField.getName(), idField, idMeta.columnType()));
        }

        for (Field field : getAllFields(entityClass)) {
            Column column = field.getAnnotation(Column.class);
            boolean hasCreationTimestamp = field.isAnnotationPresent(CreationTimestamp.class);
            boolean hasUpdateTimestamp = field.isAnnotationPresent(UpdateTimestamp.class);

            if (Objects.isNull(column) && !hasCreationTimestamp && !hasUpdateTimestamp) {
                continue;
            }

            // Skip ID field (already added)
            if (field.isAnnotationPresent(sant1ago.dev.suprim.annotation.entity.Id.class)) {
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

            SqlType sqlType = Objects.nonNull(column) ? column.type() : SqlType.AUTO;
            metas.add(new ColumnMeta(columnName, field.getName(), field, sqlType));
        }

        return metas;
    }

    private static Field findIdField(Class<?> entityClass) {
        for (Field field : getAllFields(entityClass)) {
            if (field.isAnnotationPresent(sant1ago.dev.suprim.annotation.entity.Id.class)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Collect parameter values from entity.
     */
    private static List<Object> collectParameters(Object entity, List<ColumnMeta> columnMetas) {
        List<Object> params = new ArrayList<>();
        Instant now = Instant.now();

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
                if (meta.sqlType() == SqlType.UUID && value instanceof String str) {
                    value = UUID.fromString(str);
                }
            }

            params.add(value);
        }

        return params;
    }

    /**
     * Build single entity upsert SQL.
     */
    private static String buildUpsertSql(
        EntityReflector.EntityMeta entityMeta,
        List<ColumnMeta> columnMetas,
        List<String> conflictColumns,
        List<String> updateColumns,
        SqlDialect dialect
    ) {
        boolean isPostgres = dialect.getName().toLowerCase().contains("postgres");

        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        StringBuilder sql = new StringBuilder();

        if (!isPostgres && updateColumns.isEmpty()) {
            // MySQL INSERT IGNORE for doNothing
            sql.append("INSERT IGNORE INTO ").append(tableName);
        } else {
            sql.append("INSERT INTO ").append(tableName);
        }

        // Columns
        sql.append(" (");
        StringJoiner columnJoiner = new StringJoiner(", ");
        for (ColumnMeta meta : columnMetas) {
            columnJoiner.add(dialect.quoteIdentifier(meta.columnName()));
        }
        sql.append(columnJoiner).append(")");

        // VALUES
        sql.append(" VALUES (");
        StringJoiner valueJoiner = new StringJoiner(", ");
        for (int i = 0; i < columnMetas.size(); i++) {
            valueJoiner.add("?");
        }
        sql.append(valueJoiner).append(")");

        // Conflict handling
        if (isPostgres) {
            sql.append(" ON CONFLICT (");
            sql.append(String.join(", ", conflictColumns.stream().map(dialect::quoteIdentifier).toList()));
            sql.append(")");
            if (updateColumns.isEmpty()) {
                sql.append(" DO NOTHING");
            } else {
                sql.append(" DO UPDATE SET ");
                StringJoiner updateJoiner = new StringJoiner(", ");
                for (String col : updateColumns) {
                    updateJoiner.add(dialect.quoteIdentifier(col) + " = EXCLUDED." + dialect.quoteIdentifier(col));
                }
                sql.append(updateJoiner);
            }
        } else if (!updateColumns.isEmpty()) {
            // MySQL ON DUPLICATE KEY UPDATE
            sql.append(" ON DUPLICATE KEY UPDATE ");
            StringJoiner updateJoiner = new StringJoiner(", ");
            for (String col : updateColumns) {
                updateJoiner.add(dialect.quoteIdentifier(col) + " = VALUES(" + dialect.quoteIdentifier(col) + ")");
            }
            sql.append(updateJoiner);
        }

        return sql.toString();
    }

    /**
     * Build batch upsert SQL for multiple entities.
     */
    private static String buildBatchUpsertSql(
        EntityReflector.EntityMeta entityMeta,
        List<ColumnMeta> columnMetas,
        List<String> conflictColumns,
        List<String> updateColumns,
        int rowCount,
        SqlDialect dialect
    ) {
        boolean isPostgres = dialect.getName().toLowerCase().contains("postgres");

        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        StringBuilder sql = new StringBuilder();

        if (!isPostgres && updateColumns.isEmpty()) {
            sql.append("INSERT IGNORE INTO ").append(tableName);
        } else {
            sql.append("INSERT INTO ").append(tableName);
        }

        // Columns
        sql.append(" (");
        StringJoiner columnJoiner = new StringJoiner(", ");
        for (ColumnMeta meta : columnMetas) {
            columnJoiner.add(dialect.quoteIdentifier(meta.columnName()));
        }
        sql.append(columnJoiner).append(")");

        // VALUES with multiple rows
        sql.append(" VALUES ");
        String rowPlaceholder = "(" + String.join(", ", java.util.Collections.nCopies(columnMetas.size(), "?")) + ")";
        StringJoiner rowJoiner = new StringJoiner(", ");
        for (int i = 0; i < rowCount; i++) {
            rowJoiner.add(rowPlaceholder);
        }
        sql.append(rowJoiner);

        // Conflict handling
        if (isPostgres) {
            sql.append(" ON CONFLICT (");
            sql.append(String.join(", ", conflictColumns.stream().map(dialect::quoteIdentifier).toList()));
            sql.append(")");
            if (updateColumns.isEmpty()) {
                sql.append(" DO NOTHING");
            } else {
                sql.append(" DO UPDATE SET ");
                StringJoiner updateJoiner = new StringJoiner(", ");
                for (String col : updateColumns) {
                    updateJoiner.add(dialect.quoteIdentifier(col) + " = EXCLUDED." + dialect.quoteIdentifier(col));
                }
                sql.append(updateJoiner);
            }
        } else if (!updateColumns.isEmpty()) {
            sql.append(" ON DUPLICATE KEY UPDATE ");
            StringJoiner updateJoiner = new StringJoiner(", ");
            for (String col : updateColumns) {
                updateJoiner.add(dialect.quoteIdentifier(col) + " = VALUES(" + dialect.quoteIdentifier(col) + ")");
            }
            sql.append(updateJoiner);
        }

        return sql.toString();
    }

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

    private static Object convertTimestampToFieldType(Object value, Class<?> targetType) {
        if (Objects.isNull(value) || targetType.isInstance(value)) {
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
