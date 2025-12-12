package sant1ago.dev.suprim.jdbc;

import java.util.List;

/**
 * Result of a toggle operation on a BelongsToMany relationship.
 * Contains lists of IDs that were attached and detached.
 */
public record ToggleResult(List<?> attached, List<?> detached) {

    /**
     * Total number of changes made (size of both lists).
     */
    public int totalChanges() {
        return attached.size() + detached.size();
    }

    /**
     * Check if any changes were made.
     */
    public boolean hasChanges() {
        return !attached.isEmpty() || !detached.isEmpty();
    }
}
