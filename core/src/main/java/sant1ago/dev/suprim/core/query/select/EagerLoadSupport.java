package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.EagerLoadSpec;
import sant1ago.dev.suprim.core.query.PathResolver;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.Relation;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Mixin interface providing eager loading operations.
 * Includes with() overloads and without() for controlling relation loading.
 */
public interface EagerLoadSupport extends SelectBuilderCore {

    /**
     * Eager load one or more relations to prevent N+1 queries.
     */
    default SelectBuilder with(Relation<?, ?>... relations) {
        for (Relation<?, ?> relation : relations) {
            eagerLoads().add(EagerLoadSpec.of(relation));
        }
        return self();
    }

    /**
     * Eager load a relation with a constraint applied.
     */
    default SelectBuilder with(Relation<?, ?> relation, Function<SelectBuilder, SelectBuilder> constraint) {
        eagerLoads().add(EagerLoadSpec.of(relation, constraint));
        return self();
    }

    /**
     * Eager load relations using string path syntax for nested loading.
     * Supports dot-notation for deep nesting.
     */
    default SelectBuilder with(String... paths) {
        if (Objects.isNull(fromTable())) {
            throw new IllegalStateException("Cannot resolve string paths without FROM table. Call .from() first.");
        }

        Class<?> rootEntity = fromTable().getEntityType();
        for (String path : paths) {
            EagerLoadSpec spec = PathResolver.resolve(path, rootEntity);
            eagerLoads().add(spec);
        }
        return self();
    }

    /**
     * Eager load a typed relation with nested string path.
     * Combines type-safe parent relation with string-based nested path.
     */
    default SelectBuilder with(Relation<?, ?> relation, String nestedPath) {
        EagerLoadSpec spec = PathResolver.resolveNested(relation, nestedPath);
        eagerLoads().add(spec);
        return self();
    }

    /**
     * Eager load with string path and constraint on the final relation.
     */
    default SelectBuilder with(String path, Function<SelectBuilder, SelectBuilder> constraint) {
        if (Objects.isNull(fromTable())) {
            throw new IllegalStateException("Cannot resolve string paths without FROM table. Call .from() first.");
        }

        Class<?> rootEntity = fromTable().getEntityType();
        EagerLoadSpec spec = PathResolver.resolve(path, rootEntity, constraint);
        eagerLoads().add(spec);
        return self();
    }

    /**
     * Eager load typed relation with nested path and constraint.
     */
    default SelectBuilder with(Relation<?, ?> relation, String nestedPath, Function<SelectBuilder, SelectBuilder> constraint) {
        EagerLoadSpec spec = PathResolver.resolveNested(relation, nestedPath, constraint);
        eagerLoads().add(spec);
        return self();
    }

    /**
     * Exclude default eager loads specified in @Entity(with = {...}).
     * Use this to skip specific relations that would normally be auto-loaded.
     */
    default SelectBuilder without(Relation<?, ?>... relations) {
        for (Relation<?, ?> relation : relations) {
            withoutRelations().add(relation.getFieldName());
        }
        return self();
    }

    /**
     * Exclude default eager loads using string field names.
     */
    default SelectBuilder without(String... fieldNames) {
        withoutRelations().addAll(Arrays.asList(fieldNames));
        return self();
    }
}
