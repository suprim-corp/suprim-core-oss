package sant1ago.dev.suprim.jdbc.eager;

import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.jdbc.DefaultModelRegistry;
import sant1ago.dev.suprim.jdbc.ReflectionUtils;
import sant1ago.dev.suprim.jdbc.exception.MappingException;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Populates relation fields on parent entities using reflection.
 * Handles both single relations (HasOne, BelongsTo) and collection relations (HasMany, BelongsToMany).
 *
 * <p>Uses {@link ReflectionUtils} for safe field access with public setter preference.
 */
public final class RelationPopulator {

    private RelationPopulator() {
        // Utility class
    }

    /**
     * Populate a relation on parent entities with related entities.
     *
     * @param parents  the parent entities to populate
     * @param related  the related entities loaded from database
     * @param relation the relation specification
     * @param <T>      parent entity type
     * @param <R>      related entity type
     */
    public static <T, R> void populate(List<T> parents, List<R> related, Relation<T, R> relation) {
        if (parents.isEmpty() || Objects.isNull(relation.getFieldName())) {
            return;
        }

        if (relation.isToMany()) {
            populateCollection(parents, related, relation);
        } else {
            populateSingle(parents, related, relation);
        }
    }

    /**
     * Populate a single relation (HasOne, BelongsTo).
     */
    private static <T, R> void populateSingle(
            List<T> parents,
            List<R> related,
            Relation<T, R> relation
    ) {
        // Create a map of related entities by their key
        Map<Object, R> relatedByKey = new HashMap<>();

        for (R relatedEntity : related) {
            Object keyValue = extractKeyValue(relatedEntity, relation);
            if (Objects.nonNull(keyValue)) {
                relatedByKey.put(keyValue, relatedEntity);
            }
        }

        String fieldName = relation.getFieldName();

        // Match parents to related entities
        for (T parent : parents) {
            Object parentKeyValue = extractParentKeyValue(parent, relation);
            R relatedEntity = null;

            if (Objects.nonNull(parentKeyValue)) {
                relatedEntity = relatedByKey.get(parentKeyValue);
                if (Objects.isNull(relatedEntity) && relation.hasDefault()) {
                    relatedEntity = createDefaultInstance(relation);
                }
            } else if (relation.hasDefault()) {
                relatedEntity = createDefaultInstance(relation);
            }

            if (Objects.nonNull(relatedEntity) || Objects.nonNull(parentKeyValue)) {
                boolean success = ReflectionUtils.setFieldValue(parent, fieldName, relatedEntity);
                if (!success) {
                    throw MappingException.fieldAccessError(
                            parent.getClass(),
                            fieldName,
                            new IllegalAccessException("Cannot set field '" + fieldName + "'. Add a public setter or enable non-strict mode.")
                    );
                }
            }
        }
    }

    /**
     * Create a default model instance with default attributes applied.
     * Type safety guaranteed by Relation<?, R> generic parameter.
     */
    private static <R> R createDefaultInstance(Relation<?, R> relation) {
        try {
            Class<R> relatedClass = relation.getRelatedTable().getEntityType();
            R instance = relatedClass.getDeclaredConstructor().newInstance();

            // Apply default attributes
            for (String attr : relation.getDefaultAttributes()) {
                String[] parts = attr.split("=", 2);
                if (parts.length == 2) {
                    String fieldName = parts[0].trim();
                    String value = parts[1].trim();
                    ReflectionUtils.setFieldValue(instance, fieldName, value);
                }
            }

            // Mark as default to prevent accidental save
            DefaultModelRegistry.markAsDefault(instance);

            return instance;
        } catch (Exception e) {
            throw MappingException.fieldAccessError(
                    relation.getRelatedTable().getEntityType(),
                    "default instance creation",
                    e
            );
        }
    }

    /**
     * Populate a collection relation (HasMany).
     * Note: BelongsToMany and Through are handled directly by EagerLoader.
     */
    private static <T, R> void populateCollection(
            List<T> parents,
            List<R> related,
            Relation<T, R> relation
    ) {
        String fieldName = relation.getFieldName();
        Class<?> parentClass = parents.get(0).getClass();
        Class<?> fieldType = determineFieldType(parentClass, fieldName);

        // Group related entities by parent key (FK on related entity)
        Map<Object, List<R>> relatedByParentKey = new HashMap<>();

        for (R relatedEntity : related) {
            Object parentKeyValue = extractKeyValue(relatedEntity, relation);
            if (Objects.nonNull(parentKeyValue)) {
                relatedByParentKey.computeIfAbsent(parentKeyValue, k -> new ArrayList<>()).add(relatedEntity);
            }
        }

        // Match parents to related entities
        for (T parent : parents) {
            Object parentKeyValue = extractParentKeyValue(parent, relation);

            List<R> relatedList = relatedByParentKey.getOrDefault(parentKeyValue, Collections.emptyList());

            Object collection;
            if (Set.class.isAssignableFrom(fieldType)) {
                collection = new HashSet<>(relatedList);
            } else {
                collection = new ArrayList<>(relatedList);
            }

            boolean success = ReflectionUtils.setFieldValue(parent, fieldName, collection);
            if (!success) {
                throw MappingException.fieldAccessError(
                        parentClass,
                        fieldName,
                        new IllegalAccessException("Cannot set field '" + fieldName + "'. Add a public setter or enable non-strict mode.")
                );
            }
        }
    }

    /**
     * Determine the field type for a given field name.
     */
    private static Class<?> determineFieldType(Class<?> clazz, String fieldName) {
        Field field = ReflectionUtils.findField(clazz, fieldName);
        if (Objects.nonNull(field)) {
            return field.getType();
        }
        // Default to List if field not found
        return List.class;
    }

    /**
     * Extract the key value from a related entity.
     * For HasOne/HasMany: foreign key value
     * For BelongsTo: related key (PK) value
     */
    private static <R> Object extractKeyValue(R entity, Relation<?, R> relation) {
        String keyField = switch (relation.getType()) {
            case HAS_ONE, HAS_MANY, LATEST_OF_MANY, OLDEST_OF_MANY, OF_MANY -> relation.getForeignKey();
            case BELONGS_TO -> relation.getRelatedKey();
            case BELONGS_TO_MANY, HAS_ONE_THROUGH, HAS_MANY_THROUGH -> relation.getRelatedKey();
            case MORPH_ONE, MORPH_MANY, MORPH_TO, MORPH_TO_MANY, MORPHED_BY_MANY ->
                throw new UnsupportedOperationException("Polymorphic relationships not yet fully supported");
        };

        return ReflectionUtils.getFieldValue(entity, keyField);
    }

    /**
     * Extract the parent key value that matches the relation.
     * For HasOne/HasMany: parent's local key
     * For BelongsTo: parent's foreign key
     */
    private static <T> Object extractParentKeyValue(T parent, Relation<T, ?> relation) {
        String keyField = switch (relation.getType()) {
            case HAS_ONE, HAS_MANY, LATEST_OF_MANY, OLDEST_OF_MANY, OF_MANY -> relation.getLocalKey();
            case BELONGS_TO -> relation.getForeignKey();
            case BELONGS_TO_MANY, HAS_ONE_THROUGH, HAS_MANY_THROUGH -> relation.getLocalKey();
            case MORPH_ONE, MORPH_MANY, MORPH_TO, MORPH_TO_MANY, MORPHED_BY_MANY ->
                throw new UnsupportedOperationException("Polymorphic relationships not yet fully supported");
        };

        return ReflectionUtils.getFieldValue(parent, keyField);
    }
}
