package sant1ago.dev.suprim.core.util;

import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.IdGenerator;
import sant1ago.dev.suprim.casey.Casey;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extracts and caches ID field metadata from entity classes.
 * Used by query builders and persistence layer for automatic ID generation.
 */
public final class IdMetadata {

    private static final Map<Class<?>, Info> CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<? extends IdGenerator<?>>, IdGenerator<?>> GENERATOR_CACHE = new ConcurrentHashMap<>();

    private IdMetadata() {
    }

    /**
     * ID field metadata.
     *
     * @param fieldName Java field name
     * @param columnName database column name
     * @param fieldType Java type of the ID field
     * @param strategy generation strategy from @Id annotation
     * @param generatorClass custom generator class
     */
    public record Info(
        String fieldName,
        String columnName,
        Class<?> fieldType,
        GenerationType strategy,
        Class<? extends IdGenerator<?>> generatorClass
    ) {
        public boolean isApplicationGenerated() {
            return strategy.isApplicationGenerated() || hasCustomGenerator();
        }

        public boolean isDatabaseGenerated() {
            return strategy.isDatabaseGenerated();
        }

        public boolean hasCustomGenerator() {
            return generatorClass != IdGenerator.None.class;
        }

        public boolean isManual() {
            return strategy == GenerationType.NONE && !hasCustomGenerator();
        }
    }

    /**
     * Get ID metadata for an entity class.
     *
     * @param entityClass the entity class
     * @return ID metadata, or null if no @Id field found
     */
    public static Info get(Class<?> entityClass) {
        return CACHE.computeIfAbsent(entityClass, IdMetadata::extract);
    }

    /**
     * Generate an ID value based on the strategy.
     *
     * @param info ID metadata
     * @return generated ID value (UUID or String based on field type)
     */
    public static Object generateId(Info info) {
        if (info.hasCustomGenerator()) {
            IdGenerator<?> generator = getOrCreateGenerator(info.generatorClass());
            return generator.generate();
        }

        UUID uuid = switch (info.strategy()) {
            case UUID_V4 -> UUIDUtils.v4();
            case UUID_V7 -> UUIDUtils.v7();
            default -> throw new IllegalStateException(
                "Cannot generate ID for strategy: " + info.strategy()
            );
        };

        return info.fieldType() == String.class ? uuid.toString() : uuid;
    }

    private static Info extract(Class<?> entityClass) {
        Class<?> current = entityClass;
        while (Objects.nonNull(current) && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Id idAnnotation = field.getAnnotation(Id.class);
                if (Objects.isNull(idAnnotation)) {
                    continue;
                }

                Column columnAnnotation = field.getAnnotation(Column.class);
                String columnName = Objects.nonNull(columnAnnotation) && !columnAnnotation.name().isEmpty()
                    ? columnAnnotation.name()
                    : Casey.toSnakeCase(field.getName());

                return new Info(
                    field.getName(),
                    columnName,
                    field.getType(),
                    idAnnotation.strategy(),
                    idAnnotation.generator()
                );
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static IdGenerator<?> getOrCreateGenerator(Class<? extends IdGenerator<?>> generatorClass) {
        return GENERATOR_CACHE.computeIfAbsent(generatorClass, clazz -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Cannot instantiate ID generator: " + clazz.getName(), e
                );
            }
        });
    }

}
