package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.SelectBuilder;

/**
 * Mixin interface providing set operations (UNION, INTERSECT, EXCEPT).
 */
public interface SetOperationSupport extends SelectBuilderCore {

    /**
     * UNION with another query.
     */
    default SelectBuilder union(SelectBuilder other) {
        setOperations().add(new SetOperation("UNION", other));
        return self();
    }

    /**
     * UNION ALL with another query.
     */
    default SelectBuilder unionAll(SelectBuilder other) {
        setOperations().add(new SetOperation("UNION ALL", other));
        return self();
    }

    /**
     * INTERSECT with another query.
     */
    default SelectBuilder intersect(SelectBuilder other) {
        setOperations().add(new SetOperation("INTERSECT", other));
        return self();
    }

    /**
     * EXCEPT with another query.
     */
    default SelectBuilder except(SelectBuilder other) {
        setOperations().add(new SetOperation("EXCEPT", other));
        return self();
    }
}
