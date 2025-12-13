package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.core.type.TypeUtils;
import sant1ago.dev.suprim.jdbc.exception.MappingException;

import java.lang.reflect.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.LoggerFactory;

/**
 * Auto-mapper that creates entity instances from ResultSet rows using @Column annotations.
 *
 * <pre>{@code
 * // For a record or class with @Column annotations:
 * @Entity
 * public record User(
 *     @Id Long id,
 *     @Column(name = "email") String email,
 *     @Column(name = "is_active") boolean active
 * ) {}
 *
 * // Create mapper and use it:
 * EntityMapper<User> mapper = EntityMapper.of(User.class);
 * List<User> users = executor.query(selectQuery, mapper);
 *
 * // Or use the convenience method:
 * List<User> users = executor.query(selectQuery, EntityMapper.of(User.class));
 *
 * // Exception handling:
 * try {
 *     List<User> users = executor.query(selectQuery, EntityMapper.of(User.class));
 * } catch (MappingException e) {
 *     log.error("Mapping failed: class={}, field={}, type={}",
 *         e.getTargetClass(), e.getFieldName(), e.getMappingErrorType());
 * }
 * }</pre>
 *
 * <p>Supports both records and regular classes:
 * <ul>
 *   <li>Records: uses canonical constructor with parameters in declaration order</li>
 *   <li>Classes: requires no-arg constructor, sets fields directly</li>
 * </ul>
 *
 * @param <T> the entity type to map to
 */
public final class EntityMapper<T> implements RowMapper<T> {

    private static final Map<Class<?>, EntityMetadata<?>> METADATA_CACHE = new ConcurrentHashMap<>();

    private final Class<T> entityClass;
    private final EntityMetadata<T> metadata;

    private EntityMapper(Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
        this.metadata = getOrCreateMetadata(entityClass);
    }

    /**
     * Create an EntityMapper for the given entity class.
     *
     * @param entityClass the class to map ResultSet rows to
     * @param <T>         the entity type
     * @return a new EntityMapper instance
     * @throws MappingException if the class cannot be mapped (no suitable constructor)
     */
    public static <T> EntityMapper<T> of(Class<T> entityClass) {
        return new EntityMapper<>(entityClass);
    }

    @Override
    public T map(ResultSet rs) throws SQLException {
        return metadata.createInstance(rs, entityClass);
    }

    /**
     * Get or create cached metadata for entity class.
     */
    private static <T> EntityMetadata<T> getOrCreateMetadata(Class<T> entityClass) {
        return TypeUtils.cast(METADATA_CACHE.computeIfAbsent(entityClass, EntityMetadata::new));
    }

    /**
     * Cached metadata for an entity class.
     */
    private static final class EntityMetadata<T> {
        private final Class<T> entityClass;
        private final boolean isRecord;
        private final Constructor<T> constructor;
        private final FieldMapping[] fieldMappings;

        EntityMetadata(Class<T> entityClass) {
            this.entityClass = entityClass;
            this.isRecord = entityClass.isRecord();

            if (isRecord) {
                this.fieldMappings = buildRecordMappings(entityClass);
                this.constructor = findRecordConstructor(entityClass);
            } else {
                this.fieldMappings = buildClassMappings(entityClass);
                this.constructor = findNoArgConstructor(entityClass);
            }
        }

        T createInstance(ResultSet rs, Class<T> targetClass) throws SQLException {
            try {
                if (isRecord) {
                    return createRecordInstance(rs, targetClass);
                } else {
                    return createClassInstance(rs, targetClass);
                }
            } catch (SQLException | MappingException e) {
                throw e;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw MappingException.cannotCreateInstance(targetClass, Objects.nonNull(cause) ? cause : e);
            } catch (Exception e) {
                throw MappingException.cannotCreateInstance(targetClass, e);
            }
        }

        private T createRecordInstance(ResultSet rs, Class<T> targetClass) throws Exception {
            Object[] args = new Object[fieldMappings.length];
            Map<String, Integer> columnIndices = getColumnIndices(rs);

            for (int i = 0; i < fieldMappings.length; i++) {
                FieldMapping mapping = fieldMappings[i];
                Integer columnIndex = columnIndices.get(mapping.columnName.toLowerCase());
                if (Objects.nonNull(columnIndex)) {
                    try {
                        args[i] = getValueFromResultSet(rs, columnIndex, mapping.fieldType);
                    } catch (ClassCastException e) {
                        throw MappingException.conversionFailed(targetClass, mapping.columnName, mapping.fieldType, e);
                    }
                } else {
                    args[i] = getDefaultValue(mapping.fieldType);
                }
            }

            try {
                return constructor.newInstance(args);
            } catch (InvocationTargetException e) {
                throw MappingException.recordConstructorFailed(targetClass, e.getCause());
            } catch (InstantiationException | IllegalAccessException e) {
                throw MappingException.cannotCreateInstance(targetClass, e);
            }
        }

        private T createClassInstance(ResultSet rs, Class<T> targetClass) throws Exception {
            T instance;
            try {
                instance = constructor.newInstance();
            } catch (InvocationTargetException e) {
                throw MappingException.cannotCreateInstance(targetClass, e.getCause());
            } catch (InstantiationException | IllegalAccessException e) {
                throw MappingException.cannotCreateInstance(targetClass, e);
            }

            Map<String, Integer> columnIndices = getColumnIndices(rs);

            for (FieldMapping mapping : fieldMappings) {
                Integer columnIndex = columnIndices.get(mapping.columnName.toLowerCase());
                if (Objects.nonNull(columnIndex)) {
                    try {
                        Object value = getValueFromResultSet(rs, columnIndex, mapping.fieldType);
                        boolean success = ReflectionUtils.setFieldValue(instance, mapping.field.getName(), value);
                        if (!success) {
                            throw MappingException.fieldAccessError(targetClass, mapping.field.getName(),
                                    new IllegalAccessException("Cannot set field. Add a public setter or disable strict mode."));
                        }
                    } catch (ClassCastException e) {
                        throw MappingException.conversionFailed(targetClass, mapping.columnName, mapping.fieldType, e);
                    }
                }
            }

            return instance;
        }

        private Map<String, Integer> getColumnIndices(ResultSet rs) throws SQLException {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            Map<String, Integer> indices = new HashMap<>(columnCount);

            for (int i = 1; i <= columnCount; i++) {
                String label = metaData.getColumnLabel(i);
                indices.put(label.toLowerCase(), i);
            }

            return indices;
        }

        private Object getValueFromResultSet(ResultSet rs, int index, Class<?> type) throws SQLException {
            Object value = rs.getObject(index);

            if (rs.wasNull()) {
                return getDefaultValue(type);
            }

            // Handle type conversions
            if (type == boolean.class || type == Boolean.class) {
                return convertToBoolean(value);
            }
            if (value instanceof Number number) {
                if (type == int.class || type == Integer.class) {
                    return number.intValue();
                }
                if (type == long.class || type == Long.class) {
                    return number.longValue();
                }
                if (type == double.class || type == Double.class) {
                    return number.doubleValue();
                }
                if (type == float.class || type == Float.class) {
                    return number.floatValue();
                }
                if (type == short.class || type == Short.class) {
                    return number.shortValue();
                }
                if (type == byte.class || type == Byte.class) {
                    return number.byteValue();
                }
            }

            // Handle java.time type conversions from JDBC types
            if (type == LocalDateTime.class) {
                return convertToLocalDateTime(value);
            }
            if (type == LocalDate.class) {
                return convertToLocalDate(value);
            }
            if (type == Instant.class) {
                return convertToInstant(value);
            }
            if (type == OffsetDateTime.class) {
                return convertToOffsetDateTime(value);
            }

            return value;
        }

        private LocalDateTime convertToLocalDateTime(Object value) {
            if (value instanceof LocalDateTime ldt) {
                return ldt;
            }
            if (value instanceof Timestamp ts) {
                return ts.toLocalDateTime();
            }
            if (value instanceof java.sql.Date date) {
                return date.toLocalDate().atStartOfDay();
            }
            if (value instanceof java.util.Date date) {
                return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            }
            return null;
        }

        private LocalDate convertToLocalDate(Object value) {
            if (value instanceof LocalDate ld) {
                return ld;
            }
            if (value instanceof java.sql.Date date) {
                return date.toLocalDate();
            }
            if (value instanceof Timestamp ts) {
                return ts.toLocalDateTime().toLocalDate();
            }
            if (value instanceof java.util.Date date) {
                return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate();
            }
            return null;
        }

        private Instant convertToInstant(Object value) {
            if (value instanceof Instant instant) {
                return instant;
            }
            if (value instanceof Timestamp ts) {
                return ts.toInstant();
            }
            if (value instanceof java.util.Date date) {
                return date.toInstant();
            }
            return null;
        }

        private OffsetDateTime convertToOffsetDateTime(Object value) {
            if (value instanceof OffsetDateTime odt) {
                return odt;
            }
            if (value instanceof Timestamp ts) {
                return OffsetDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
            }
            if (value instanceof java.util.Date date) {
                return OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            }
            return null;
        }

        private boolean convertToBoolean(Object value) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof Number number) {
                return number.intValue() != 0;
            }
            if (value instanceof String s) {
                String lower = s.toLowerCase();
                return "true".equals(lower) || "t".equals(lower) || "yes".equals(lower)
                        || "y".equals(lower) || "1".equals(lower);
            }
            return false;
        }

        private Object getDefaultValue(Class<?> type) {
            if (type.isPrimitive()) {
                if (type == boolean.class) return false;
                if (type == int.class) return 0;
                if (type == long.class) return 0L;
                if (type == double.class) return 0.0;
                if (type == float.class) return 0.0f;
                if (type == short.class) return (short) 0;
                if (type == byte.class) return (byte) 0;
                if (type == char.class) return '\0';
            }
            return null;
        }

        private FieldMapping[] buildRecordMappings(Class<T> recordClass) {
            RecordComponent[] components = recordClass.getRecordComponents();
            FieldMapping[] mappings = new FieldMapping[components.length];

            for (int i = 0; i < components.length; i++) {
                RecordComponent component = components[i];
                String columnName = resolveColumnName(component);
                mappings[i] = new FieldMapping(null, columnName, component.getType());
            }

            return mappings;
        }

        private FieldMapping[] buildClassMappings(Class<T> clazz) {
            Field[] fields = clazz.getDeclaredFields();
            FieldMapping[] mappings = new FieldMapping[fields.length];

            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                String columnName = resolveColumnName(field);
                mappings[i] = new FieldMapping(field, columnName, field.getType());
            }

            return mappings;
        }

        private String resolveColumnName(RecordComponent component) {
            Column column = component.getAnnotation(Column.class);
            if (Objects.nonNull(column) && !column.name().isEmpty()) {
                return column.name();
            }
            return toSnakeCase(component.getName());
        }

        private String resolveColumnName(Field field) {
            Column column = field.getAnnotation(Column.class);
            if (Objects.nonNull(column) && !column.name().isEmpty()) {
                return column.name();
            }
            return toSnakeCase(field.getName());
        }

        private String toSnakeCase(String name) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (Character.isUpperCase(c)) {
                    if (i > 0) {
                        result.append('_');
                    }
                    result.append(Character.toLowerCase(c));
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }

        private Constructor<T> findRecordConstructor(Class<T> recordClass) {
            RecordComponent[] components = recordClass.getRecordComponents();
            Class<?>[] paramTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                paramTypes[i] = components[i].getType();
            }

            try {
                return recordClass.getDeclaredConstructor(paramTypes);
            } catch (NoSuchMethodException e) {
                throw MappingException.noConstructor(recordClass);
            }
        }

        private Constructor<T> findNoArgConstructor(Class<T> clazz) {
            // Try public constructor first
            try {
                return clazz.getConstructor();
            } catch (NoSuchMethodException e) {
                // Fall back to declared constructor (may be private)
            }

            try {
                Constructor<T> constructor = clazz.getDeclaredConstructor();
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    if (ReflectionUtils.isStrictMode()) {
                        throw MappingException.noNoArgConstructor(clazz);
                    }
                    // Log warning for non-public constructor
                    LoggerFactory.getLogger(EntityMapper.class)
                          .warn("Suprim: Using non-public constructor for {}. Consider adding a public no-arg constructor.", clazz.getSimpleName());
                    constructor.setAccessible(true);
                }
                return constructor;
            } catch (NoSuchMethodException e) {
                throw MappingException.noNoArgConstructor(clazz);
            }
        }
    }

    /**
     * Mapping between a field and its database column.
     */
    private record FieldMapping(Field field, String columnName, Class<?> fieldType) {
    }
}
