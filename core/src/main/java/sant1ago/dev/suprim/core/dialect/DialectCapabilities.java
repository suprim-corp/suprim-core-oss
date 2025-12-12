package sant1ago.dev.suprim.core.dialect;

/**
 * Feature capability detection for SQL dialects.
 * Used to check if a dialect supports specific SQL features at runtime.
 */
public interface DialectCapabilities {

    /**
     * Supports INSERT/UPDATE/DELETE RETURNING clause.
     *
     * @return true if RETURNING is supported
     */
    boolean supportsReturning();

    /**
     * Supports ILIKE (case-insensitive LIKE).
     *
     * @return true if ILIKE is supported
     */
    boolean supportsIlike();

    /**
     * Supports native ARRAY types and operators.
     *
     * @return true if arrays are supported
     */
    boolean supportsArrays();

    /**
     * Supports JSONB type with native operators.
     *
     * @return true if JSONB is supported
     */
    boolean supportsJsonb();

    /**
     * Supports FOR UPDATE/SHARE SKIP LOCKED.
     *
     * @return true if SKIP LOCKED is supported
     */
    boolean supportsSkipLocked();

    /**
     * Supports FOR UPDATE/SHARE NOWAIT.
     *
     * @return true if NOWAIT is supported
     */
    boolean supportsNowait();

    /**
     * Supports aggregate FILTER clause.
     *
     * @return true if FILTER clause is supported
     */
    boolean supportsFilterClause();

    /**
     * Supports DISTINCT ON (columns).
     *
     * @return true if DISTINCT ON is supported
     */
    boolean supportsDistinctOn();

    // ==================== PRE-BUILT INSTANCES ====================

    /** Full capabilities - PostgreSQL (primary dialect). */
    DialectCapabilities FULL = new DialectCapabilities() {
        @Override public boolean supportsReturning() { return true; }
        @Override public boolean supportsIlike() { return true; }
        @Override public boolean supportsArrays() { return true; }
        @Override public boolean supportsJsonb() { return true; }
        @Override public boolean supportsSkipLocked() { return true; }
        @Override public boolean supportsNowait() { return true; }
        @Override public boolean supportsFilterClause() { return true; }
        @Override public boolean supportsDistinctOn() { return true; }
    };

    /** MySQL 5.7 capabilities. */
    DialectCapabilities MYSQL_5_7 = new DialectCapabilities() {
        @Override public boolean supportsReturning() { return false; }
        @Override public boolean supportsIlike() { return false; }
        @Override public boolean supportsArrays() { return false; }
        @Override public boolean supportsJsonb() { return false; }
        @Override public boolean supportsSkipLocked() { return false; }
        @Override public boolean supportsNowait() { return false; }
        @Override public boolean supportsFilterClause() { return false; }
        @Override public boolean supportsDistinctOn() { return false; }
    };

    /** MySQL 8.0+ capabilities - adds SKIP LOCKED and NOWAIT support. */
    DialectCapabilities MYSQL_8 = new DialectCapabilities() {
        @Override public boolean supportsReturning() { return false; }
        @Override public boolean supportsIlike() { return false; }
        @Override public boolean supportsArrays() { return false; }
        @Override public boolean supportsJsonb() { return false; }
        @Override public boolean supportsSkipLocked() { return true; }
        @Override public boolean supportsNowait() { return true; }
        @Override public boolean supportsFilterClause() { return false; }
        @Override public boolean supportsDistinctOn() { return false; }
    };

    /** MariaDB 10.5+ capabilities - similar to MySQL 8 with RETURNING support. */
    DialectCapabilities MARIADB = new DialectCapabilities() {
        @Override public boolean supportsReturning() { return true; }  // MariaDB 10.5+
        @Override public boolean supportsIlike() { return false; }
        @Override public boolean supportsArrays() { return false; }
        @Override public boolean supportsJsonb() { return false; }
        @Override public boolean supportsSkipLocked() { return true; }  // MariaDB 10.3+
        @Override public boolean supportsNowait() { return true; }      // MariaDB 10.3+
        @Override public boolean supportsFilterClause() { return false; }
        @Override public boolean supportsDistinctOn() { return false; }
    };
}
