package sant1ago.dev.suprim.annotation.type;

/**
 * Fetch strategy hints for query optimization.
 *
 * <pre>{@code
 * // Always load profile with user (auto-join in SELECT)
 * @HasOne(entity = Profile.class, fetch = FetchType.EAGER)
 * private Profile profile;
 *
 * // Load posts only when explicitly joined (default)
 * @HasMany(entity = Post.class, fetch = FetchType.LAZY)
 * private List<Post> posts;
 *
 * // Query behavior:
 * Suprim.select(User_.ID).from(User_.TABLE).build();
 * // SQL: SELECT ... FROM users LEFT JOIN profiles ON ... (EAGER auto-joined)
 *
 * // Disable EAGER for specific query:
 * Suprim.select(User_.ID).from(User_.TABLE).withoutEager().build();
 * // SQL: SELECT ... FROM users (no join)
 * }</pre>
 */
public enum FetchType {

    /**
     * Load relationship data only when explicitly joined.
     * Default behavior - no auto-join.
     */
    LAZY,

    /**
     * Auto-join in SELECT queries.
     * <ul>
     *   <li>SELECT queries: relationship is automatically joined</li>
     *   <li>UPDATE/DELETE queries: EAGER is ignored (no auto-join)</li>
     *   <li>Can be disabled per-query with {@code .withoutEager()}</li>
     * </ul>
     */
    EAGER
}
