package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.SelectBuilder;

/**
 * Mixin interface providing row locking operations (FOR UPDATE, FOR SHARE).
 */
public interface LockingSupport extends SelectBuilderCore {

    /**
     * Add FOR UPDATE lock (PostgreSQL).
     * Locks selected rows for update.
     */
    default SelectBuilder forUpdate() {
        lockMode("FOR UPDATE");
        return self();
    }

    /**
     * Add FOR UPDATE NOWAIT lock.
     * Fails immediately if lock cannot be acquired.
     */
    default SelectBuilder forUpdateNowait() {
        lockMode("FOR UPDATE NOWAIT");
        return self();
    }

    /**
     * Add FOR UPDATE SKIP LOCKED.
     * Skips rows that are already locked.
     */
    default SelectBuilder forUpdateSkipLocked() {
        lockMode("FOR UPDATE SKIP LOCKED");
        return self();
    }

    /**
     * Add FOR SHARE lock (PostgreSQL).
     * Allows concurrent reads but prevents updates.
     */
    default SelectBuilder forShare() {
        lockMode("FOR SHARE");
        return self();
    }

    /**
     * Add FOR SHARE NOWAIT lock.
     * Fails immediately if lock cannot be acquired.
     */
    default SelectBuilder forShareNowait() {
        lockMode("FOR SHARE NOWAIT");
        return self();
    }

    /**
     * Add FOR SHARE SKIP LOCKED.
     * Skips rows that are already locked.
     */
    default SelectBuilder forShareSkipLocked() {
        lockMode("FOR SHARE SKIP LOCKED");
        return self();
    }
}
