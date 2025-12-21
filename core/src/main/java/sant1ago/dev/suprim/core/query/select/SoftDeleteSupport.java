package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.query.SelectBuilder.SoftDeleteScope;

/**
 * Mixin interface providing soft delete scope operations.
 * Controls whether soft-deleted records are included in query results.
 */
public interface SoftDeleteSupport extends SelectBuilderCore {

    /**
     * Include soft-deleted records in query results.
     * By default, queries on entities with @SoftDeletes exclude deleted records.
     * Use this to include them.
     */
    default SelectBuilder withTrashed() {
        softDeleteScope(SoftDeleteScope.WITH_TRASHED);
        return self();
    }

    /**
     * Only return soft-deleted records.
     * Queries only records where deleted_at IS NOT NULL.
     */
    default SelectBuilder onlyTrashed() {
        softDeleteScope(SoftDeleteScope.ONLY_TRASHED);
        return self();
    }
}
