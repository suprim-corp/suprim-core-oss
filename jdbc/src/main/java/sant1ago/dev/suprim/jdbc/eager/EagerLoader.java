package sant1ago.dev.suprim.jdbc.eager;

import sant1ago.dev.suprim.core.query.EagerLoadSpec;
import sant1ago.dev.suprim.core.query.Suprim;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;
import sant1ago.dev.suprim.core.type.TypeUtils;
import sant1ago.dev.suprim.jdbc.EntityMapper;
import sant1ago.dev.suprim.jdbc.ReflectionUtils;
import sant1ago.dev.suprim.jdbc.SuprimExecutor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads related entities eagerly using batch queries to prevent N+1 query problems.
 * Supports all relation types and nested eager loading.
 */
public final class EagerLoader {

    private final SuprimExecutor executor;

    public EagerLoader(SuprimExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    /**
     * Load relations for a list of entities based on eager load specifications.
     *
     * @param entities the parent entities
     * @param specs    the eager load specifications
     * @param <T>      parent entity type
     */
    public <T> void loadRelations(List<T> entities, List<EagerLoadSpec> specs) {
        if (entities.isEmpty() || specs.isEmpty()) {
            return;
        }

        for (EagerLoadSpec spec : specs) {
            loadRelation(entities, spec);
        }
    }

    /**
     * Load a single relation for entities.
     */
    private <T, R> void loadRelation(List<T> entities, EagerLoadSpec spec) {
        Relation<T, R> relation = TypeUtils.castRelation(spec.relation());

        // Load related entities based on the relation type
        List<R> relatedEntities = switch (relation.getType()) {
            case HAS_ONE, HAS_MANY, LATEST_OF_MANY, OLDEST_OF_MANY, OF_MANY -> loadHasOneOrMany(entities, relation, spec);
            case BELONGS_TO -> loadBelongsTo(entities, relation, spec);
            case BELONGS_TO_MANY -> loadBelongsToMany(entities, relation, spec);
            case HAS_ONE_THROUGH, HAS_MANY_THROUGH -> loadThrough(entities, relation, spec);
            case MORPH_ONE, MORPH_MANY, MORPH_TO, MORPH_TO_MANY, MORPHED_BY_MANY ->
                    throw new UnsupportedOperationException("Polymorphic relationships not yet implemented in eager loading");
        };

        // Populate relation field on parent entities
        RelationPopulator.populate(entities, relatedEntities, relation);

        // Recursively load nested relations
        if (spec.hasNested() && !relatedEntities.isEmpty()) {
            for (EagerLoadSpec nestedSpec : spec.nested()) {
                loadRelation(relatedEntities, nestedSpec);
            }
        }
    }

    /**
     * Load HasOne or HasMany relation.
     * SQL: SELECT * FROM related WHERE fk IN (parent_ids)
     */
    private <T, R> List<R> loadHasOneOrMany(
            List<T> parents,
            Relation<T, R> relation,
            EagerLoadSpec spec
    ) {
        // Extract parent key values
        Set<Object> parentKeys = extractKeys(parents, relation.getLocalKey());
        if (parentKeys.isEmpty()) {
            return Collections.emptyList();
        }

        // Build query: SELECT * FROM related WHERE fk IN (...)
        Table<R> relatedTable = relation.getRelatedTable();

        // Build IN clause using raw SQL since we're dealing with Object values
        String inClause = String.format(
                "%s IN (%s)",
                relation.getForeignKey(),
                parentKeys.stream()
                        .map(this::formatValue)
                        .collect(Collectors.joining(", "))
        );

        SelectBuilder builder = Suprim.select()
                .from(relatedTable)
                .whereRaw(inClause);

        // Apply constraint if provided
        if (spec.hasConstraint()) {
            builder = spec.constraint().apply(builder);
        }

        QueryResult query = builder.build();
        return executor.query(query, EntityMapper.of(relatedTable.getEntityType()));
    }

    /**
     * Load BelongsTo relation.
     * SQL: SELECT * FROM related WHERE pk IN (fk_values)
     */
    private <T, R> List<R> loadBelongsTo(
            List<T> parents,
            Relation<T, R> relation,
            EagerLoadSpec spec
    ) {
        // Extract foreign key values from parents
        Set<Object> foreignKeys = extractKeys(parents, relation.getForeignKey());
        if (foreignKeys.isEmpty()) {
            return Collections.emptyList();
        }

        // Build query: SELECT * FROM related WHERE id IN (...)
        Table<R> relatedTable = relation.getRelatedTable();

        // Build IN clause using raw SQL
        String inClause = String.format(
                "%s IN (%s)",
                relation.getRelatedKey(),
                foreignKeys.stream()
                        .map(this::formatValue)
                        .collect(Collectors.joining(", "))
        );

        SelectBuilder builder = Suprim.select()
                .from(relatedTable)
                .whereRaw(inClause);

        // Apply constraint if provided
        if (spec.hasConstraint()) {
            builder = spec.constraint().apply(builder);
        }

        QueryResult query = builder.build();
        return executor.query(query, EntityMapper.of(relatedTable.getEntityType()));
    }

    /**
     * Load BelongsToMany relation.
     * SQL: SELECT related.*, pivot.fk FROM related
     *      JOIN pivot ON pivot.related_key = related.id
     *      WHERE pivot.fk IN (parent_ids)
     */
    private <T, R> List<R> loadBelongsToMany(
            List<T> parents,
            Relation<T, R> relation,
            EagerLoadSpec spec
    ) {
        // Extract parent key values
        Set<Object> parentKeys = extractKeys(parents, relation.getLocalKey());
        if (parentKeys.isEmpty()) {
            return Collections.emptyList();
        }

        // Build query with pivot table join
        Table<R> relatedTable = relation.getRelatedTable();

        // SELECT related.* FROM related
        SelectBuilder builder = Suprim.select()
                .from(relatedTable);

        // JOIN pivot ON pivot.related_pivot_key = related.related_key
        String pivotJoin = String.format(
                "JOIN %s ON %s.%s = %s.%s",
                relation.getPivotTable(),
                relation.getPivotTable(),
                relation.getRelatedPivotKey(),
                relatedTable.getName(),
                relation.getRelatedKey()
        );
        builder.joinRaw(pivotJoin);

        // WHERE pivot.foreign_pivot_key IN (parent_ids)
        String pivotWhereClause = String.format(
                "%s.%s IN (%s)",
                relation.getPivotTable(),
                relation.getForeignPivotKey(),
                parentKeys.stream()
                        .map(this::formatValue)
                        .collect(Collectors.joining(", "))
        );
        builder.whereRaw(pivotWhereClause);

        // Apply constraint if provided
        if (spec.hasConstraint()) {
            builder = spec.constraint().apply(builder);
        }

        QueryResult query = builder.build();
        return executor.query(query, EntityMapper.of(relatedTable.getEntityType()));
    }

    /**
     * Load HasOneThrough or HasManyThrough relation.
     * SQL: SELECT related.*, through.fk FROM related
     *      JOIN through ON related.second_key = through.second_local_key
     *      WHERE through.first_key IN (parent_ids)
     */
    private <T, R> List<R> loadThrough(
            List<T> parents,
            Relation<T, R> relation,
            EagerLoadSpec spec
    ) {
        // Extract parent key values
        Set<Object> parentKeys = extractKeys(parents, relation.getLocalKey());
        if (parentKeys.isEmpty()) {
            return Collections.emptyList();
        }

        // Build query with through table join
        Table<R> relatedTable = relation.getRelatedTable();
        Table<?> throughTable = relation.getThroughTable();

        // SELECT related.* FROM related
        SelectBuilder builder = Suprim.select()
                .from(relatedTable);

        // JOIN through ON related.second_key = through.second_local_key
        String throughJoin = String.format(
                "JOIN %s ON %s.%s = %s.%s",
                throughTable.getName(),
                relatedTable.getName(),
                relation.getSecondKey(),
                throughTable.getName(),
                relation.getSecondLocalKey()
        );
        builder.joinRaw(throughJoin);

        // WHERE through.first_key IN (parent_ids)
        String throughWhereClause = String.format(
                "%s.%s IN (%s)",
                throughTable.getName(),
                relation.getFirstKey(),
                parentKeys.stream()
                        .map(this::formatValue)
                        .collect(Collectors.joining(", "))
        );
        builder.whereRaw(throughWhereClause);

        // Apply constraint if provided
        if (spec.hasConstraint()) {
            builder = spec.constraint().apply(builder);
        }

        QueryResult query = builder.build();
        return executor.query(query, EntityMapper.of(relatedTable.getEntityType()));
    }

    /**
     * Extract key values from entities using reflection.
     */
    private <T> Set<Object> extractKeys(List<T> entities, String keyField) {
        Set<Object> keys = new HashSet<>();

        for (T entity : entities) {
            Object keyValue = ReflectionUtils.getFieldValue(entity, keyField);
            if (Objects.nonNull(keyValue)) {
                keys.add(keyValue);
            }
        }

        return keys;
    }

    /**
     * Format a value for SQL (quote strings, leave numbers as-is).
     */
    private String formatValue(Object value) {
        if (Objects.isNull(value)) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        // Quote strings and other types
        return "'" + value.toString().replace("'", "''") + "'";
    }
}
