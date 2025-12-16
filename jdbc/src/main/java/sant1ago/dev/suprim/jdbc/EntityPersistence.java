package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.JsonbColumn;
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
     */
    private static Map<String, Object> buildColumnMap(Object entity, EntityReflector.IdMeta idMeta, boolean skipId) {
        Map<String, Object> columns = new LinkedHashMap<>();
        Class<?> entityClass = entity.getClass();

        for (Field field : getAllFields(entityClass)) {
            Column column = field.getAnnotation(Column.class);
            if (Objects.isNull(column)) {
                continue;
            }

            String columnName = column.name().isEmpty()
                ? Casey.toSnakeCase(field.getName())
                : column.name();

            // Skip ID column if database will generate it
            if (skipId && columnName.equals(idMeta.columnName())) {
                continue;
            }

            Object value = ReflectionUtils.getFieldValue(entity, field.getName());
            if (Objects.nonNull(value)) {
                // Convert @JsonbColumn fields to PGobject for PostgreSQL JSONB
                if (field.isAnnotationPresent(JsonbColumn.class)) {
                    value = EntityReflector.toJsonbObject(value);
                }
                // Convert String to UUID for SqlType.UUID columns
                if (column.type() == SqlType.UUID && value instanceof String str) {
                    try {
                        value = UUID.fromString(str);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                            "Invalid UUID format for column '" + columnName + "': " + str, e);
                    }
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
     */
    private static void setParameters(PreparedStatement ps, Object[] values) throws SQLException {
        int i = 1;
        for (Object value : values) {
            ps.setObject(i++, value);
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

        Map<String, Object> columns = buildColumnMap(entity, idMeta, true);
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
            if (Objects.isNull(column)) {
                continue;
            }
            String columnName = column.name().isEmpty()
                ? Casey.toSnakeCase(field.getName())
                : column.name();

            try {
                Object value = rs.getObject(columnName);
                if (Objects.nonNull(value)) {
                    value = convertValueToFieldType(value, field.getType());
                    ReflectionUtils.setFieldValue(entity, field.getName(), value);
                }
            } catch (SQLException e) {
                // Column might not exist in result set, skip
            }
        }
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
}
