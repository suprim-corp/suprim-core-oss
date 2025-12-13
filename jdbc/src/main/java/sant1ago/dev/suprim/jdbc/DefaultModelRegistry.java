package sant1ago.dev.suprim.jdbc;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Registry for tracking default model instances.
 * Uses WeakHashMap to avoid memory leaks - default instances will be garbage collected
 * when no longer referenced.
 *
 * <p>Default models are created when a relationship doesn't exist but @HasOne/@BelongsTo
 * is annotated with withDefault=true. These instances should never be persisted to the database.
 */
public final class DefaultModelRegistry {

    // WeakHashMap allows GC to collect entries when keys are no longer referenced
    private static final Set<Object> defaults = Collections.newSetFromMap(new WeakHashMap<>());

    private DefaultModelRegistry() {
        // Utility class
    }

    /**
     * Mark an entity instance as a default model.
     * Default models cannot be saved to the database.
     *
     * @param entity the entity instance to mark as default
     */
    public static void markAsDefault(Object entity) {
        if (Objects.nonNull(entity)) {
            defaults.add(entity);
        }
    }

    /**
     * Check if an entity instance is a default model.
     *
     * @param entity the entity instance to check
     * @return true if the entity is a default model, false otherwise
     */
    public static boolean isDefault(Object entity) {
        return Objects.nonNull(entity) && defaults.contains(entity);
    }

    /**
     * Clear all tracked default instances.
     * Useful for testing.
     */
    static void clear() {
        defaults.clear();
    }

    /**
     * Get the number of tracked default instances.
     * Useful for testing and monitoring.
     *
     * @return the number of tracked default instances
     */
    static int size() {
        return defaults.size();
    }
}
