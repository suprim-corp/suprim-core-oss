package sant1ago.dev.suprim.annotation.type;

/**
 * Cascade operations for relationships.
 * Supports both DB-level and application-level cascading.
 *
 * <pre>{@code
 * // Auto-detect: use DB CASCADE if exists, else generate app queries
 * @HasMany(entity = Post.class, cascade = CascadeType.AUTO)
 * private List<Post> posts;
 *
 * // Strict: require DB CASCADE, throw if missing
 * @HasMany(entity = Comment.class, cascade = CascadeType.DB)
 * private List<Comment> comments;
 *
 * // Always generate application-level queries
 * @HasMany(entity = AuditLog.class, cascade = CascadeType.APPLICATION)
 * private List<AuditLog> auditLogs;
 * }</pre>
 */
public enum CascadeType {

    /**
     * No cascade operations (default).
     */
    NONE,

    /**
     * Use database CASCADE constraint.
     * Throws exception at runtime if DB doesn't have CASCADE.
     * Use when you're certain the FK has ON DELETE CASCADE.
     */
    DB,

    /**
     * Auto-detect: use DB CASCADE if exists, else generate application queries.
     * Best for portability - works with or without FK constraints.
     * Recommended for most use cases.
     */
    AUTO,

    /**
     * Always generate application-level cascade queries.
     * Safe option - doesn't rely on DB constraints.
     * Use when FK may not exist or for cross-database relationships.
     */
    APPLICATION,

    /**
     * Cascade all operations (DELETE + UPDATE) using APPLICATION strategy.
     */
    ALL,

    /**
     * Remove orphaned records when removed from collection.
     * Only applicable to hasMany relationships.
     * Records are deleted when removed from the collection, not just unlinked.
     */
    DELETE_ORPHAN
}
