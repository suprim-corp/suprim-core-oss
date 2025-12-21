package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.CreationTimestamp;
import sant1ago.dev.suprim.annotation.entity.JsonbColumn;
import sant1ago.dev.suprim.annotation.entity.SoftDeletes;
import sant1ago.dev.suprim.annotation.entity.TimestampAction;
import sant1ago.dev.suprim.annotation.entity.UpdateTimestamp;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.IdGenerator;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.casey.Casey;
import sant1ago.dev.suprim.core.util.UUIDUtils;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
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
import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles entity persistence with automatic ID generation based on @Id strategy.
 *
 * <pre>{@code
 * // In transaction context:
 * executor.transaction(tx -> {
 *     User user = new User();
 *     user.setEmail("test@example.com");
 *
 *     User saved = tx.save(user);  // ID generated automatically
 *     System.out.println(saved.getId());  // Works!
 * });
 *
 * // Supports all ID strategies:
 * @Id(strategy = GenerationType.UUID_V7)   // App generates before insert
 * @Id(strategy = GenerationType.IDENTITY)  // DB generates (SERIAL/AUTO_INCREMENT)
 * @Id(strategy = GenerationType.UUID_DB)   // DB generates (gen_random_uuid())
 * @Id(generator = CustomGenerator.class)   // Custom logic
 * }</pre>
 */
final class EntityPersistence {

    private static final Map<Class<? extends IdGenerator<?>>, IdGenerator<?>> GENERATOR_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, SoftDeleteMeta> SOFT_DELETE_CACHE = new ConcurrentHashMap<>();

    /**
     * Soft delete metadata for an entity class.
     *
     * @param enabled true if entity has @SoftDeletes
     * @param columnName the deleted_at column name
     */
    record SoftDeleteMeta(boolean enabled, String columnName) {
        static final SoftDeleteMeta DISABLED = new SoftDeleteMeta(false, null);
    }

    private EntityPersistence() {
        // Utility class
    }

    /**
     * Save an entity with automatic ID generation.
     *
     * @param entity the entity to save
     * @param connection the database connection
     * @param dialect the SQL dialect
     * @param <T> entity type
     * @return the saved entity with ID set
     */
    static <T> T save(T entity, Connection connection, SqlDialect dialect) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(connection, "Connection cannot be null");

        Class<?> entityClass = entity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        // Check if ID already set
        Object existingId = EntityReflector.getIdOrNull(entity);

        if (Objects.nonNull(existingId)) {
            // ID already set - just insert
            return executeInsert(entity, entityMeta, idMeta, connection, dialect, false);
        }

        // Generate ID based on strategy
        if (idMeta.isManual()) {
            throw new PersistenceException(
                "Entity ID is null and strategy is NONE. " +
                "Either set the ID manually or configure a generation strategy: " +
                "@Id(strategy = GenerationType.UUID_V7)",
                entityClass
            );
        }

        if (idMeta.isApplicationGenerated()) {
            // Generate ID in application
            Object generatedId = generateId(idMeta);
            EntityReflector.setId(entity, generatedId);
            return executeInsert(entity, entityMeta, idMeta, connection, dialect, false);
        }

        if (idMeta.isDatabaseGenerated()) {
            // Let database generate ID
            return executeInsert(entity, entityMeta, idMeta, connection, dialect, true);
        }

        throw new PersistenceException(
            "Unsupported ID generation strategy: " + idMeta.strategy(),
            entityClass
        );
    }

    /**
     * Generate an ID value based on the strategy.
     */
    private static Object generateId(EntityReflector.IdMeta idMeta) {
        // Custom generator takes precedence
        if (idMeta.hasCustomGenerator()) {
            IdGenerator<?> generator = getOrCreateGenerator(idMeta.generatorClass());
            return generator.generate();
        }

        // Built-in strategies - use UUIDUtils directly
        UUID uuid = switch (idMeta.strategy()) {
            case UUID_V4 -> UUIDUtils.v4();
            case UUID_V7 -> UUIDUtils.v7();
            default -> throw new IllegalStateException(
                "Cannot generate ID for strategy: " + idMeta.strategy()
            );
        };

        // Return UUID or String based on field type
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
                    "Cannot instantiate ID generator: " + clazz.getName() +
                    ". Ensure it has a no-arg constructor.", e
                );
            }
        });
    }

    /**
     * Execute INSERT and optionally retrieve generated ID.
     */
    private static <T> T executeInsert(
        T entity,
        EntityReflector.EntityMeta entityMeta,
        EntityReflector.IdMeta idMeta,
        Connection connection,
        SqlDialect dialect,
        boolean retrieveGeneratedId
    ) {
        // Build column map from entity
        Map<String, Object> columns = buildColumnMap(entity, idMeta, retrieveGeneratedId);

        if (columns.isEmpty()) {
            throw new PersistenceException(
                "No columns to insert. Entity has no @Column annotated fields with values.",
                entity.getClass()
            );
        }

        // Build SQL
        String sql = buildInsertSql(entityMeta, idMeta, columns, dialect, retrieveGeneratedId);

        try {
            if (retrieveGeneratedId) {
                return executeWithGeneratedKeys(entity, idMeta, columns, sql, connection, dialect);
            } else {
                executeSimpleInsert(columns, sql, connection);
                return entity;
            }
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to save entity: " + e.getMessage(),
                entity.getClass(),
                e
            );
        }
    }

    /**
     * Build column name to value map from entity.
     * Automatically sets @CreatedAt and @UpdatedAt fields for insert operations.
     */
    private static Map<String, Object> buildColumnMap(Object entity, EntityReflector.IdMeta idMeta, boolean skipId) {
        return buildColumnMap(entity, idMeta, skipId, true);
    }

    /**
     * Build column name to value map from entity.
     *
     * @param entity the entity
     * @param idMeta ID metadata
     * @param skipId whether to skip the ID column
     * @param isInsert true for INSERT (sets @CreatedAt), false for UPDATE
     */
    private static Map<String, Object> buildColumnMap(Object entity, EntityReflector.IdMeta idMeta, boolean skipId, boolean isInsert) {
        Map<String, Object> columns = new LinkedHashMap<>();
        Class<?> entityClass = entity.getClass();
        Instant now = Instant.now();

        for (Field field : getAllFields(entityClass)) {
            Column column = field.getAnnotation(Column.class);
            boolean hasCreationTimestamp = field.isAnnotationPresent(CreationTimestamp.class);
            boolean hasUpdateTimestamp = field.isAnnotationPresent(UpdateTimestamp.class);

            // Skip fields without @Column, @CreationTimestamp, or @UpdateTimestamp
            if (Objects.isNull(column) && !hasCreationTimestamp && !hasUpdateTimestamp) {
                continue;
            }

            // Determine column name: @Column.name > @Timestamp.column > snake_case
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

            // Skip ID column if database will generate it
            if (skipId && columnName.equals(idMeta.columnName())) {
                continue;
            }

            Object value = ReflectionUtils.getFieldValue(entity, field.getName());

            // Handle @CreationTimestamp - set on insert only based on onCreation action
            if (isInsert && hasCreationTimestamp) {
                CreationTimestamp creationTs = field.getAnnotation(CreationTimestamp.class);
                TimestampAction action = creationTs.onCreation();
                if (action == TimestampAction.NOW || (action == TimestampAction.IF_NULL && Objects.isNull(value))) {
                    value = convertTimestampToFieldType(now, field.getType());
                    ReflectionUtils.setFieldValue(entity, field.getName(), value);
                }
            }

            // Handle @UpdateTimestamp - set on both insert and update based on onModification action
            if (hasUpdateTimestamp) {
                UpdateTimestamp updateTs = field.getAnnotation(UpdateTimestamp.class);
                TimestampAction action = updateTs.onModification();
                if (action == TimestampAction.NOW || (action == TimestampAction.IF_NULL && Objects.isNull(value))) {
                    value = convertTimestampToFieldType(now, field.getType());
                    ReflectionUtils.setFieldValue(entity, field.getName(), value);
                }
            }

            if (Objects.nonNull(value)) {
                // Convert @JsonbColumn fields to PGobject for PostgreSQL JSONB
                if (field.isAnnotationPresent(JsonbColumn.class)) {
                    value = EntityReflector.toJsonbObject(value);
                }
                // Convert String to UUID for SqlType.UUID columns
                if (Objects.nonNull(column) && column.type() == SqlType.UUID && value instanceof String str) {
                    try {
                        value = UUID.fromString(str);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                            "Invalid UUID format for column '" + columnName + "': " + str, e);
                    }
                }
                // Convert to PGobject for SqlType.VECTOR columns (pgvector)
                if (Objects.nonNull(column) && column.type() == SqlType.VECTOR) {
                    value = EntityReflector.toVectorObject(value);
                }
                columns.put(columnName, value);
            }
        }

        // Add ID if set and not skipping
        if (!skipId) {
            Object idValue = EntityReflector.getIdOrNull(entity);
            if (Objects.nonNull(idValue)) {
                // Convert String ID to UUID only if column type is UUID
                if (idMeta.columnType() == SqlType.UUID && idValue instanceof String str) {
                    try {
                        idValue = UUID.fromString(str);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                            "Invalid UUID format for ID column '" + idMeta.columnName() + "': " + str, e);
                    }
                }
                columns.put(idMeta.columnName(), idValue);
            }
        }

        return columns;
    }

    /**
     * Build INSERT SQL statement.
     */
    private static String buildInsertSql(
        EntityReflector.EntityMeta entityMeta,
        EntityReflector.IdMeta idMeta,
        Map<String, Object> columns,
        SqlDialect dialect,
        boolean returningId
    ) {
        StringBuilder sql = new StringBuilder();

        // Table name with optional schema
        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        sql.append("INSERT INTO ").append(tableName).append(" (");

        // Column names
        StringJoiner columnJoiner = new StringJoiner(", ");
        for (String col : columns.keySet()) {
            columnJoiner.add(dialect.quoteIdentifier(col));
        }

        // Handle UUID_DB - insert the function call
        if (returningId && idMeta.strategy() == GenerationType.UUID_DB) {
            columnJoiner.add(dialect.quoteIdentifier(idMeta.columnName()));
        }

        sql.append(columnJoiner).append(") VALUES (");

        // Placeholders
        StringJoiner valueJoiner = new StringJoiner(", ");
        for (int i = 0; i < columns.size(); i++) {
            valueJoiner.add("?");
        }

        // Handle UUID_DB
        if (returningId && idMeta.strategy() == GenerationType.UUID_DB) {
            String uuidFn = dialect instanceof MySqlDialect ? "UUID()" : "gen_random_uuid()";
            valueJoiner.add(uuidFn);
        }

        sql.append(valueJoiner).append(")");

        // RETURNING clause for PostgreSQL
        if (returningId && dialect.capabilities().supportsReturning()) {
            sql.append(" RETURNING ").append(dialect.quoteIdentifier(idMeta.columnName()));
        }

        return sql.toString();
    }

    /**
     * Execute INSERT and retrieve generated key.
     */
    private static <T> T executeWithGeneratedKeys(
        T entity,
        EntityReflector.IdMeta idMeta,
        Map<String, Object> columns,
        String sql,
        Connection connection,
        SqlDialect dialect
    ) throws SQLException {
        boolean supportsReturning = dialect.capabilities().supportsReturning();

        if (supportsReturning) {
            // PostgreSQL: use RETURNING clause
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                setParameters(ps, columns.values().toArray());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Object generatedId = rs.getObject(1);
                        generatedId = convertIdType(generatedId, idMeta.fieldType());
                        EntityReflector.setId(entity, generatedId);
                    }
                }
            }
        } else {
            // MySQL: use RETURN_GENERATED_KEYS
            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setParameters(ps, columns.values().toArray());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        Object generatedId = rs.getObject(1);
                        generatedId = convertIdType(generatedId, idMeta.fieldType());
                        EntityReflector.setId(entity, generatedId);
                    }
                }
            }
        }

        return entity;
    }

    /**
     * Execute simple INSERT without returning.
     */
    private static void executeSimpleInsert(
        Map<String, Object> columns,
        String sql,
        Connection connection
    ) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            setParameters(ps, columns.values().toArray());
            ps.executeUpdate();
        }
    }

    /**
     * Set parameters on prepared statement.
     * Handles special types like enums by converting to their string representation.
     */
    private static void setParameters(PreparedStatement ps, Object[] values) throws SQLException {
        int i = 1;
        for (Object value : values) {
            if (value instanceof Enum<?> enumValue) {
                // Convert Java enums to string for database compatibility
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
        if (Objects.isNull(value)) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        // Handle common conversions
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

        if (targetType == java.util.UUID.class) {
            if (value instanceof String s) {
                return java.util.UUID.fromString(s);
            }
        }

        return value;
    }

    /**
     * Update an existing entity by ID.
     *
     * @param entity the entity to update
     * @param connection the database connection
     * @param dialect the SQL dialect
     * @param <T> entity type
     * @return the updated entity
     */
    static <T> T update(T entity, Connection connection, SqlDialect dialect) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(connection, "Connection cannot be null");

        Class<?> entityClass = entity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        Object id = EntityReflector.getIdOrNull(entity);
        if (Objects.isNull(id)) {
            throw new PersistenceException(
                "Cannot update entity without ID. Set the ID or use save() for new entities.",
                entityClass
            );
        }

        // Use isInsert=false so @CreatedAt is preserved, @UpdatedAt is set
        Map<String, Object> columns = buildColumnMap(entity, idMeta, true, false);
        if (columns.isEmpty()) {
            return entity;
        }

        String sql = buildUpdateSql(entityMeta, idMeta, columns, dialect);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            Object[] values = columns.values().toArray();
            setParameters(ps, values);
            ps.setObject(values.length + 1, convertIdForQuery(id, idMeta));
            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to update entity: " + e.getMessage(),
                entityClass,
                e
            );
        }
    }

    /**
     * Delete an entity by ID.
     *
     * @param entity the entity to delete
     * @param connection the database connection
     * @param dialect the SQL dialect
     */
    static void delete(Object entity, Connection connection, SqlDialect dialect) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(connection, "Connection cannot be null");

        Class<?> entityClass = entity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        Object id = EntityReflector.getIdOrNull(entity);
        if (Objects.isNull(id)) {
            throw new PersistenceException(
                "Cannot delete entity without ID.",
                entityClass
            );
        }

        // Check for soft deletes
        SoftDeleteMeta softDeleteMeta = getSoftDeleteMeta(entityClass);
        if (softDeleteMeta.enabled()) {
            softDelete(entity, connection, dialect);
            return;
        }

        // Hard delete
        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        String sql = "DELETE FROM " + tableName +
            " WHERE " + dialect.quoteIdentifier(idMeta.columnName()) + " = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, convertIdForQuery(id, idMeta));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to delete entity: " + e.getMessage(),
                entityClass,
                e
            );
        }
    }

    /**
     * Refresh an entity from the database.
     *
     * @param entity the entity to refresh
     * @param connection the database connection
     * @param dialect the SQL dialect
     * @param <T> entity type
     * @return the refreshed entity
     */
    static <T> T refresh(T entity, Connection connection, SqlDialect dialect) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(connection, "Connection cannot be null");

        Class<?> entityClass = entity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        Object id = EntityReflector.getIdOrNull(entity);
        if (Objects.isNull(id)) {
            throw new PersistenceException(
                "Cannot refresh entity without ID.",
                entityClass
            );
        }

        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        String sql = "SELECT * FROM " + tableName +
            " WHERE " + dialect.quoteIdentifier(idMeta.columnName()) + " = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, convertIdForQuery(id, idMeta));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    populateEntityFromResultSet(entity, rs);
                    return entity;
                }
                throw new PersistenceException(
                    "Entity not found in database with ID: " + id,
                    entityClass
                );
            }
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to refresh entity: " + e.getMessage(),
                entityClass,
                e
            );
        }
    }

    /**
     * Build UPDATE SQL statement.
     */
    private static String buildUpdateSql(
        EntityReflector.EntityMeta entityMeta,
        EntityReflector.IdMeta idMeta,
        Map<String, Object> columns,
        SqlDialect dialect
    ) {
        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");

        StringJoiner setJoiner = new StringJoiner(", ");
        for (String col : columns.keySet()) {
            setJoiner.add(dialect.quoteIdentifier(col) + " = ?");
        }
        sql.append(setJoiner);

        sql.append(" WHERE ").append(dialect.quoteIdentifier(idMeta.columnName())).append(" = ?");

        return sql.toString();
    }

    /**
     * Convert ID value for query parameter.
     */
    private static Object convertIdForQuery(Object id, EntityReflector.IdMeta idMeta) {
        if (idMeta.columnType() == SqlType.UUID && id instanceof String str) {
            return UUID.fromString(str);
        }
        return id;
    }

    /**
     * Populate entity fields from ResultSet.
     */
    private static void populateEntityFromResultSet(Object entity, ResultSet rs) throws SQLException {
        Class<?> entityClass = entity.getClass();
        for (Field field : getAllFields(entityClass)) {
            Column column = field.getAnnotation(Column.class);
            boolean hasCreationTimestamp = field.isAnnotationPresent(CreationTimestamp.class);
            boolean hasUpdateTimestamp = field.isAnnotationPresent(UpdateTimestamp.class);

            // Skip fields without @Column, @CreationTimestamp, or @UpdateTimestamp
            if (Objects.isNull(column) && !hasCreationTimestamp && !hasUpdateTimestamp) {
                continue;
            }

            // Determine column name
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

            try {
                Object value = rs.getObject(columnName);
                if (Objects.nonNull(value)) {
                    // Convert temporal types (Timestamp -> LocalDateTime/Instant/etc.)
                    if (isTemporalType(field.getType())) {
                        value = convertTimestampToFieldType(value, field.getType());
                    } else {
                        value = convertValueToFieldType(value, field.getType());
                    }
                    ReflectionUtils.setFieldValue(entity, field.getName(), value);
                } else if (isTemporalType(field.getType())) {
                    // For temporal fields (like deleted_at), set null to ensure refresh clears them
                    ReflectionUtils.setFieldValue(entity, field.getName(), null);
                }
                // For non-temporal fields with null DB values, preserve in-memory state
            } catch (SQLException e) {
                // Column might not exist in result set, skip
            }
        }
    }

    /**
     * Check if a type is a temporal type that needs timestamp conversion.
     */
    private static boolean isTemporalType(Class<?> type) {
        return type == LocalDateTime.class
                || type == Instant.class
                || type == java.time.OffsetDateTime.class
                || type == Timestamp.class
                || type == java.util.Date.class;
    }

    /**
     * Convert value from ResultSet to field type.
     */
    private static Object convertValueToFieldType(Object value, Class<?> targetType) {
        if (Objects.isNull(value) || targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == UUID.class && value instanceof String str) {
            return UUID.fromString(str);
        }

        if ((targetType == Long.class || targetType == long.class) && value instanceof Number n) {
            return n.longValue();
        }

        if ((targetType == Integer.class || targetType == int.class) && value instanceof Number n) {
            return n.intValue();
        }

        return value;
    }

    /**
     * Get all fields for a class including inherited fields from parent classes.
     * Traverses up the hierarchy until Object.class (exclusive).
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

    // ==================== Soft Delete Support ====================

    /**
     * Get soft delete metadata for an entity class.
     *
     * @param entityClass the entity class
     * @return soft delete metadata
     */
    static SoftDeleteMeta getSoftDeleteMeta(Class<?> entityClass) {
        return SOFT_DELETE_CACHE.computeIfAbsent(entityClass, clazz -> {
            SoftDeletes annotation = clazz.getAnnotation(SoftDeletes.class);
            if (Objects.isNull(annotation)) {
                return SoftDeleteMeta.DISABLED;
            }
            return new SoftDeleteMeta(true, annotation.column());
        });
    }

    /**
     * Check if an entity is soft deleted (has deleted_at set).
     *
     * @param entity the entity to check
     * @return true if soft deleted
     */
    static boolean isTrashed(Object entity) {
        Class<?> entityClass = entity.getClass();
        SoftDeleteMeta softDeleteMeta = getSoftDeleteMeta(entityClass);

        if (!softDeleteMeta.enabled()) {
            return false;
        }

        // Find the deleted_at field
        Object deletedAt = getDeletedAtValue(entity, softDeleteMeta.columnName());
        return Objects.nonNull(deletedAt);
    }

    /**
     * Get the deleted_at field value from an entity.
     */
    private static Object getDeletedAtValue(Object entity, String columnName) {
        Class<?> entityClass = entity.getClass();
        for (Field field : getAllFields(entityClass)) {
            Column column = field.getAnnotation(Column.class);
            if (Objects.nonNull(column)) {
                String colName = column.name().isEmpty()
                    ? Casey.toSnakeCase(field.getName())
                    : column.name();
                if (colName.equals(columnName)) {
                    return ReflectionUtils.getFieldValue(entity, field.getName());
                }
            }
            // Also check if field name matches (for deletedAt -> deleted_at)
            if (Casey.toSnakeCase(field.getName()).equals(columnName)) {
                return ReflectionUtils.getFieldValue(entity, field.getName());
            }
        }
        return null;
    }

    /**
     * Set the deleted_at field value on an entity.
     * Converts the value to the appropriate type based on the field's declared type.
     */
    private static void setDeletedAtValue(Object entity, String columnName, Object value) {
        Class<?> entityClass = entity.getClass();
        for (Field field : getAllFields(entityClass)) {
            Column column = field.getAnnotation(Column.class);
            String colName = null;
            if (Objects.nonNull(column)) {
                colName = column.name().isEmpty()
                    ? Casey.toSnakeCase(field.getName())
                    : column.name();
            }

            // Check if field matches by column name or field name
            boolean matches = (Objects.nonNull(colName) && colName.equals(columnName))
                    || Casey.toSnakeCase(field.getName()).equals(columnName);

            if (matches) {
                Object convertedValue = convertTimestampToFieldType(value, field.getType());
                ReflectionUtils.setFieldValue(entity, field.getName(), convertedValue);
                return;
            }
        }
    }

    /**
     * Convert a timestamp value to the field's declared type.
     * Supports LocalDateTime, Instant, OffsetDateTime, java.sql.Timestamp.
     *
     * @param value the timestamp value (Instant from current time)
     * @param targetType the field's declared type
     * @return converted value or null if input is null
     */
    private static Object convertTimestampToFieldType(Object value, Class<?> targetType) {
        if (Objects.isNull(value)) {
            return null;
        }

        // If value is already the target type, return as-is
        if (targetType.isInstance(value)) {
            return value;
        }

        // Convert from Instant (our canonical source)
        Instant instant;
        if (value instanceof Instant i) {
            instant = i;
        } else if (value instanceof Timestamp ts) {
            instant = ts.toInstant();
        } else if (value instanceof LocalDateTime ldt) {
            instant = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant();
        } else if (value instanceof java.time.OffsetDateTime odt) {
            instant = odt.toInstant();
        } else {
            // Unknown type, return as-is
            return value;
        }

        // Convert to target type
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        } else if (targetType == Instant.class) {
            return instant;
        } else if (targetType == java.time.OffsetDateTime.class) {
            return java.time.OffsetDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        } else if (targetType == Timestamp.class) {
            return Timestamp.from(instant);
        } else if (targetType == java.util.Date.class) {
            return java.util.Date.from(instant);
        }

        // Fallback - return the instant
        return instant;
    }

    /**
     * Soft delete an entity (set deleted_at to current timestamp).
     * Only used internally - called from delete() when @SoftDeletes is present.
     *
     * @param entity the entity to soft delete
     * @param connection the database connection
     * @param dialect the SQL dialect
     */
    private static void softDelete(Object entity, Connection connection, SqlDialect dialect) {
        Class<?> entityClass = entity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);
        SoftDeleteMeta softDeleteMeta = getSoftDeleteMeta(entityClass);

        Object id = EntityReflector.getIdOrNull(entity);

        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        // Use database-appropriate current timestamp
        String sql = "UPDATE " + tableName +
            " SET " + dialect.quoteIdentifier(softDeleteMeta.columnName()) + " = ?" +
            " WHERE " + dialect.quoteIdentifier(idMeta.columnName()) + " = ?";

        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, timestamp);
            ps.setObject(2, convertIdForQuery(id, idMeta));
            ps.executeUpdate();

            // Update the entity's deleted_at field (auto-converts to field's type)
            setDeletedAtValue(entity, softDeleteMeta.columnName(), now);
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to soft delete entity: " + e.getMessage(),
                entityClass,
                e
            );
        }
    }

    /**
     * Force delete an entity (actually remove from database).
     * Bypasses soft delete and performs a real DELETE.
     *
     * @param entity the entity to delete
     * @param connection the database connection
     * @param dialect the SQL dialect
     */
    static void forceDelete(Object entity, Connection connection, SqlDialect dialect) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(connection, "Connection cannot be null");

        Class<?> entityClass = entity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        Object id = EntityReflector.getIdOrNull(entity);
        if (Objects.isNull(id)) {
            throw new PersistenceException(
                "Cannot delete entity without ID.",
                entityClass
            );
        }

        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        String sql = "DELETE FROM " + tableName +
            " WHERE " + dialect.quoteIdentifier(idMeta.columnName()) + " = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, convertIdForQuery(id, idMeta));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to force delete entity: " + e.getMessage(),
                entityClass,
                e
            );
        }
    }

    /**
     * Restore a soft-deleted entity (set deleted_at to NULL).
     *
     * @param entity the entity to restore
     * @param connection the database connection
     * @param dialect the SQL dialect
     */
    static void restore(Object entity, Connection connection, SqlDialect dialect) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(connection, "Connection cannot be null");

        Class<?> entityClass = entity.getClass();
        SoftDeleteMeta softDeleteMeta = getSoftDeleteMeta(entityClass);

        if (!softDeleteMeta.enabled()) {
            throw new PersistenceException(
                "Cannot restore entity without @SoftDeletes annotation.",
                entityClass
            );
        }

        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        Object id = EntityReflector.getIdOrNull(entity);
        if (Objects.isNull(id)) {
            throw new PersistenceException(
                "Cannot restore entity without ID.",
                entityClass
            );
        }

        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        String sql = "UPDATE " + tableName +
            " SET " + dialect.quoteIdentifier(softDeleteMeta.columnName()) + " = NULL" +
            " WHERE " + dialect.quoteIdentifier(idMeta.columnName()) + " = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, convertIdForQuery(id, idMeta));
            ps.executeUpdate();

            // Clear the entity's deleted_at field
            setDeletedAtValue(entity, softDeleteMeta.columnName(), null);
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to restore entity: " + e.getMessage(),
                entityClass,
                e
            );
        }
    }

    /**
     * Touch the entity's updated_at timestamp.
     * Updates the updated_at column to the current time.
     *
     * @param entity     the entity to touch
     * @param connection the database connection
     * @param dialect    the SQL dialect
     */
    static void touch(Object entity, Connection connection, SqlDialect dialect) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(connection, "Connection cannot be null");

        Class<?> entityClass = entity.getClass();
        EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(entityClass);
        EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(entityClass);

        Object id = EntityReflector.getIdOrNull(entity);
        if (Objects.isNull(id)) {
            throw new PersistenceException(
                "Cannot touch entity without ID.",
                entityClass
            );
        }

        // Find the updated_at column
        String updatedAtColumn = findUpdatedAtColumn(entityClass);
        if (Objects.isNull(updatedAtColumn)) {
            throw new PersistenceException(
                "Cannot touch entity without @UpdateTimestamp annotation.",
                entityClass
            );
        }

        String tableName = Objects.nonNull(entityMeta.schema())
            ? dialect.quoteIdentifier(entityMeta.schema()) + "." + dialect.quoteIdentifier(entityMeta.tableName())
            : dialect.quoteIdentifier(entityMeta.tableName());

        String sql = "UPDATE " + tableName +
            " SET " + dialect.quoteIdentifier(updatedAtColumn) + " = " + dialect.currentTimestampFunction() +
            " WHERE " + dialect.quoteIdentifier(idMeta.columnName()) + " = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, convertIdForQuery(id, idMeta));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(
                "Failed to touch entity: " + e.getMessage(),
                entityClass,
                e
            );
        }
    }

    /**
     * Find the updated_at column name from @UpdateTimestamp annotation.
     */
    private static String findUpdatedAtColumn(Class<?> entityClass) {
        Class<?> current = entityClass;
        while (current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                sant1ago.dev.suprim.annotation.entity.UpdateTimestamp updateAnn =
                    field.getAnnotation(sant1ago.dev.suprim.annotation.entity.UpdateTimestamp.class);
                if (Objects.nonNull(updateAnn)) {
                    return updateAnn.column();
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
