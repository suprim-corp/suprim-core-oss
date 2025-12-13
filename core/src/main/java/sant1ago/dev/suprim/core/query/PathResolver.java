package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Resolves dot-notation relation paths to nested EagerLoadSpec structures.
 * <p>
 * Examples:
 * <pre>{@code
 * // "posts.comments" -> EagerLoadSpec(POSTS, nested=[EagerLoadSpec(COMMENTS)])
 * PathResolver.resolve("posts.comments", User.class);
 *
 * // "posts.comments.author" -> 3-level nesting
 * PathResolver.resolve("posts.comments.author", User.class);
 * }</pre>
 */
public final class PathResolver {

    // Cache for metamodel class lookups to improve performance
    private static final Map<Class<?>, Class<?>> METAMODEL_CACHE = new ConcurrentHashMap<>();

    /**
     * Resolve a dot-notation path to an EagerLoadSpec tree.
     *
     * @param path       dot-notation path like "posts.comments.author"
     * @param rootEntity the root entity class
     * @return nested EagerLoadSpec structure
     * @throws IllegalArgumentException if path is invalid or relation not found
     */
    public static EagerLoadSpec resolve(String path, Class<?> rootEntity) {
        if (Objects.isNull(path) || path.isBlank()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        if (Objects.isNull(rootEntity)) {
            throw new IllegalArgumentException("Root entity class cannot be null");
        }

        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        // Build the tree from bottom-up (reverse order)
        EagerLoadSpec current = null;
        Class<?> currentEntity = rootEntity;

        for (int i = parts.length - 1; i >= 0; i--) {
            String relationName = parts[i];
            Relation<?, ?> relation = resolveRelation(currentEntity, relationName);

            // Create spec with nested child (if any)
            List<EagerLoadSpec> nested = Objects.nonNull(current) ? List.of(current) : new ArrayList<>();
            current = new EagerLoadSpec(relation, null, nested);

            // Move to parent entity for next iteration
            currentEntity = relation.getOwnerTable().getEntityType();
        }

        return current;
    }

    /**
     * Resolve a path with a constraint applied to the final relation.
     *
     * @param path       dot-notation path
     * @param rootEntity root entity class
     * @param constraint constraint to apply to the final relation
     * @return nested EagerLoadSpec structure with constraint on last level
     */
    public static EagerLoadSpec resolve(
            String path,
            Class<?> rootEntity,
            Function<SelectBuilder, SelectBuilder> constraint
    ) {
        if (Objects.isNull(constraint)) {
            return resolve(path, rootEntity);
        }

        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        // Build tree from bottom-up
        EagerLoadSpec current = null;
        Class<?> currentEntity = rootEntity;

        for (int i = parts.length - 1; i >= 0; i--) {
            String relationName = parts[i];
            Relation<?, ?> relation = resolveRelation(currentEntity, relationName);

            // Apply constraint only to the deepest (last) level
            Function<SelectBuilder, SelectBuilder> specConstraint = (i == parts.length - 1) ? constraint : null;

            List<EagerLoadSpec> nested = Objects.nonNull(current) ? List.of(current) : new ArrayList<>();
            current = new EagerLoadSpec(relation, specConstraint, nested);

            currentEntity = relation.getOwnerTable().getEntityType();
        }

        return current;
    }

    /**
     * Resolve a relation starting from a typed Relation with a nested string path.
     * <p>
     * Example: with(User_.POSTS, "comments.author")
     *
     * @param relation   typed relation (e.g., User_.POSTS)
     * @param nestedPath remaining path (e.g., "comments.author")
     * @return EagerLoadSpec with nested path resolved
     */
    public static EagerLoadSpec resolveNested(Relation<?, ?> relation, String nestedPath) {
        if (Objects.isNull(nestedPath) || nestedPath.isBlank()) {
            return EagerLoadSpec.of(relation);
        }

        // Resolve nested path starting from relation's related entity
        Class<?> relatedEntity = relation.getRelatedTable().getEntityType();
        EagerLoadSpec nestedSpec = resolve(nestedPath, relatedEntity);

        // Create parent spec with nested
        return new EagerLoadSpec(relation, null, List.of(nestedSpec));
    }

    /**
     * Resolve a relation starting from a typed Relation with nested path and constraint.
     *
     * @param relation   typed relation
     * @param nestedPath nested path
     * @param constraint constraint to apply
     * @return EagerLoadSpec with nested path and constraint
     */
    public static EagerLoadSpec resolveNested(
            Relation<?, ?> relation,
            String nestedPath,
            Function<SelectBuilder, SelectBuilder> constraint
    ) {
        if (Objects.isNull(nestedPath) || nestedPath.isBlank()) {
            return EagerLoadSpec.of(relation, constraint);
        }

        // Resolve nested path with constraint
        Class<?> relatedEntity = relation.getRelatedTable().getEntityType();
        EagerLoadSpec nestedSpec = resolve(nestedPath, relatedEntity, constraint);

        // Create parent spec with nested
        return new EagerLoadSpec(relation, null, List.of(nestedSpec));
    }

    /**
     * Resolve a relation field name to a Relation instance using reflection.
     *
     * @param entityClass entity class (e.g., User.class)
     * @param fieldName   relation field name in CONSTANT_CASE or camelCase (e.g., "POSTS" or "posts")
     * @return Relation instance
     * @throws IllegalArgumentException if relation not found
     */
    private static Relation<?, ?> resolveRelation(Class<?> entityClass, String fieldName) {
        try {
            // Get or find metamodel class (e.g., User -> User_)
            Class<?> metamodelClass = getMetamodelClass(entityClass);

            // Try CONSTANT_CASE first (e.g., "posts" -> "POSTS")
            String constantName = toConstantCase(fieldName);
            Field field = findField(metamodelClass, constantName);

            // If not found, try original name
            if (Objects.isNull(field)) {
                field = findField(metamodelClass, fieldName);
            }

            if (Objects.isNull(field)) {
                throw new IllegalArgumentException(
                        String.format("Relation '%s' not found on metamodel class %s. " +
                                        "Tried field names: %s, %s",
                                fieldName, metamodelClass.getSimpleName(), constantName, fieldName)
                );
            }

            // Check if field is actually a Relation
            if (!Relation.class.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException(
                        String.format("Field '%s' on %s is not a Relation (found: %s)",
                                field.getName(), metamodelClass.getSimpleName(), field.getType().getSimpleName())
                );
            }

            // Try to access as public field first (metamodel fields should be public)
            try {
                return (Relation<?, ?>) field.get(null);
            } catch (IllegalAccessException e) {
                // Fallback to setAccessible for non-public fields
                field.setAccessible(true);
                return (Relation<?, ?>) field.get(null);
            }

        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                    "Failed to access relation field '" + fieldName + "' on " + entityClass.getSimpleName(), e
            );
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Metamodel class not found for entity " + entityClass.getSimpleName() + ". " +
                            "Ensure annotation processor has been run.", e
            );
        }
    }

    /**
     * Get the metamodel class for an entity (e.g., User -> User_).
     * Caches results for performance.
     */
    private static Class<?> getMetamodelClass(Class<?> entityClass) throws ClassNotFoundException {
        return METAMODEL_CACHE.computeIfAbsent(entityClass, key -> {
            String metamodelName = key.getName() + "_";
            try {
                return Class.forName(metamodelName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Find a field by name on a class, including static fields.
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * Convert a field name to CONSTANT_CASE.
     * Examples:
     * - "posts" -> "POSTS"
     * - "postComments" -> "POST_COMMENTS"
     * - "user_roles" -> "USER_ROLES"
     */
    private static String toConstantCase(String fieldName) {
        if (Objects.isNull(fieldName) || fieldName.isEmpty()) {
            return fieldName;
        }

        // If already CONSTANT_CASE, return as-is
        if (fieldName.equals(fieldName.toUpperCase()) && fieldName.contains("_")) {
            return fieldName;
        }

        // Convert camelCase to snake_case, then uppercase
        String snakeCase = fieldName
                .replaceAll("([a-z])([A-Z])", "$1_$2")  // camelCase -> camel_Case
                .replaceAll("([A-Z])([A-Z][a-z])", "$1_$2"); // XMLParser -> XML_Parser

        return snakeCase.toUpperCase();
    }
}
