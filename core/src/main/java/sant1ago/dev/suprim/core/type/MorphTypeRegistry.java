package sant1ago.dev.suprim.core.type;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for polymorphic type aliases.
 *
 * <p>Maps type aliases (e.g., "User", "Post") to entity classes and vice versa.
 * Used by MorphTo relationships to resolve the actual entity type at runtime.
 *
 * <p>Thread-safe for concurrent access.
 */
public class MorphTypeRegistry {

    private static final Map<String, Class<?>> TYPE_TO_CLASS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String> CLASS_TO_TYPE = new ConcurrentHashMap<>();

    /**
     * Register a type alias for an entity class.
     *
     * @param alias type alias (e.g., "User")
     * @param entityClass entity class
     */
    public static void register(String alias, Class<?> entityClass) {
        TYPE_TO_CLASS.put(alias, entityClass);
        CLASS_TO_TYPE.put(entityClass, alias);
    }

    /**
     * Resolve a type alias to an entity class.
     *
     * @param type type alias or fully qualified class name
     * @return entity class
     * @throws IllegalArgumentException if type cannot be resolved
     */
    public static Class<?> resolve(String type) {
        // Try registered aliases first
        Class<?> clazz = TYPE_TO_CLASS.get(type);
        if (Objects.nonNull(clazz)) {
            return clazz;
        }

        // Try as fully qualified class name
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown morph type: " + type + ". Register it using MorphTypeRegistry.register()", e);
        }
    }

    /**
     * Get the type alias for an entity class.
     *
     * @param entityClass entity class
     * @return type alias, or simple class name if not registered
     */
    public static String getTypeAlias(Class<?> entityClass) {
        return CLASS_TO_TYPE.getOrDefault(entityClass, entityClass.getSimpleName());
    }

    /**
     * Check if a type alias is registered.
     *
     * @param alias type alias
     * @return true if registered
     */
    public static boolean isRegistered(String alias) {
        return TYPE_TO_CLASS.containsKey(alias);
    }

    /**
     * Clear all registered types (primarily for testing).
     */
    public static void clear() {
        TYPE_TO_CLASS.clear();
        CLASS_TO_TYPE.clear();
    }

    /**
     * Get all registered type aliases.
     *
     * @return map of type aliases to entity classes
     */
    public static Map<String, Class<?>> getRegisteredTypes() {
        return Map.copyOf(TYPE_TO_CLASS);
    }
}
