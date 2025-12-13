package sant1ago.dev.suprim.jdbc;

/**
 * Result of a sync operation on a BelongsToMany relationship.
 * Contains counts of attached and detached records.
 */
public record SyncResult(int attached, int detached) {

    /**
     * Total number of changes made (attached + detached).
     */
    public int totalChanges() {
        return attached + detached;
    }

    /**
     * Check if any changes were made.
     */
    public boolean hasChanges() {
        return attached > 0 || detached > 0;
    }
}
