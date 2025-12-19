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

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
        // Skip for BelongsToMany and Through relations - they populate directly in their methods
        if (relation.getType() != Relation.Type.BELONGS_TO_MANY
                && relation.getType() != Relation.Type.HAS_ONE_THROUGH
                && relation.getType() != Relation.Type.HAS_MANY_THROUGH) {
            RelationPopulator.populate(entities, relatedEntities, relation);
        }

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
     * Load BelongsToMany relation (Laravel-style).
     * Step 1: Query pivot table to get parent->related mappings
     * Step 2: Query related entities
     * Step 3: Store mapping for RelationPopulator to use
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

        // Step 1: Query pivot table to get (foreignPivotKey, relatedPivotKey) pairs
        // SQL: SELECT foreign_pivot_key AS FK, related_pivot_key AS RK FROM pivot WHERE foreign_pivot_key IN (...)
        // Use uppercase aliases for cross-database compatibility
        String pivotQuery = String.format(
                "SELECT %s AS FK, %s AS RK FROM %s WHERE %s IN (%s)",
                relation.getForeignPivotKey(),
                relation.getRelatedPivotKey(),
                relation.getPivotTable(),
                relation.getForeignPivotKey(),
                parentKeys.stream().map(this::formatValue).collect(Collectors.joining(", "))
        );

        // Execute pivot query and build mapping
        Map<Object, List<Object>> parentToRelatedKeys = new HashMap<>();
        Set<Object> allRelatedKeys = new HashSet<>();

        List<Map<String, Object>> pivotRows = executor.query(
                new QueryResult(pivotQuery, Map.of()),
                rs -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("fk", rs.getObject("FK"));
                    row.put("rk", rs.getObject("RK"));
                    return row;
                }
        );

        for (Map<String, Object> row : pivotRows) {
            Object parentKey = row.get("fk");
            Object relatedKey = row.get("rk");
            if (isNull(parentKey) || isNull(relatedKey)) {
                continue;
            }
            parentToRelatedKeys.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(relatedKey);
            allRelatedKeys.add(relatedKey);
        }

        if (allRelatedKeys.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 2: Query related entities
        Table<R> relatedTable = relation.getRelatedTable();
        String relatedInClause = String.format(
                "%s IN (%s)",
                relation.getRelatedKey(),
                allRelatedKeys.stream().map(this::formatValue).collect(Collectors.joining(", "))
        );

        SelectBuilder builder = Suprim.select()
                .from(relatedTable)
                .whereRaw(relatedInClause);

        if (spec.hasConstraint()) {
            builder = spec.constraint().apply(builder);
        }

        QueryResult query = builder.build();
        List<R> relatedEntities = executor.query(query, EntityMapper.of(relatedTable.getEntityType()));

        // Step 3: Build related entity map by key
        Map<Object, R> relatedByKey = new HashMap<>();
        for (R entity : relatedEntities) {
            Object key = ReflectionUtils.getFieldValue(entity, relation.getRelatedKey());
            if (isNull(key)) {
                continue;
            }
            relatedByKey.put(key, entity);
        }

        // Step 4: Assign related to parents using pivot mapping
        String fieldName = relation.getFieldName();
        if (isNull(fieldName)) {
            return Collections.emptyList();
        }

        for (T parent : parents) {
            Object parentKey = ReflectionUtils.getFieldValue(parent, relation.getLocalKey());
            List<Object> relatedKeys = parentToRelatedKeys.getOrDefault(parentKey, Collections.emptyList());

            List<R> parentRelated = relatedKeys.stream()
                    .map(relatedByKey::get)
                    .filter(Objects::nonNull)
                    .toList();

            Class<?> fieldType = determineFieldType(parent.getClass(), fieldName);
            Object collection = Set.class.isAssignableFrom(fieldType)
                    ? new HashSet<>(parentRelated)
                    : new ArrayList<>(parentRelated);
            ReflectionUtils.setFieldValue(parent, fieldName, collection);
        }

        return Collections.emptyList();
    }

    private Class<?> determineFieldType(Class<?> clazz, String fieldName) {
        Field field = ReflectionUtils.findField(clazz, fieldName);
        return nonNull(field) ? field.getType() : List.class;
    }

    /**
     * Load HasOneThrough or HasManyThrough relation (Laravel-style).
     * Step 1: Query through table to get parent->related mappings
     * Step 2: Query related entities
     * Step 3: Assign to parents using mapping
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

        Table<?> throughTable = relation.getThroughTable();
        Table<R> relatedTable = relation.getRelatedTable();

        // Step 1: Query through table to get (firstKey, secondLocalKey) pairs
        // firstKey links to parent, secondLocalKey links to related
        // Use uppercase aliases for cross-database compatibility
        String throughQuery = String.format(
                "SELECT %s AS PK, %s AS RK FROM %s WHERE %s IN (%s)",
                relation.getFirstKey(),
                relation.getSecondLocalKey(),
                throughTable.getName(),
                relation.getFirstKey(),
                parentKeys.stream().map(this::formatValue).collect(Collectors.joining(", "))
        );

        // Execute through query and build mapping
        Map<Object, List<Object>> parentToRelatedKeys = new HashMap<>();
        Set<Object> allRelatedKeys = new HashSet<>();

        List<Map<String, Object>> throughRows = executor.query(
                new QueryResult(throughQuery, Map.of()),
                rs -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("pk", rs.getObject("PK"));
                    row.put("rk", rs.getObject("RK"));
                    return row;
                }
        );

        for (Map<String, Object> row : throughRows) {
            Object parentKey = row.get("pk");
            Object relatedKey = row.get("rk");
            if (isNull(parentKey) || isNull(relatedKey)) {
                continue;
            }
            parentToRelatedKeys.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(relatedKey);
            allRelatedKeys.add(relatedKey);
        }

        if (allRelatedKeys.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 2: Query related entities using secondKey (FK on related table)
        String relatedInClause = String.format(
                "%s IN (%s)",
                relation.getSecondKey(),
                allRelatedKeys.stream().map(this::formatValue).collect(Collectors.joining(", "))
        );

        SelectBuilder builder = Suprim.select()
                .from(relatedTable)
                .whereRaw(relatedInClause);

        if (spec.hasConstraint()) {
            builder = spec.constraint().apply(builder);
        }

        QueryResult query = builder.build();
        List<R> relatedEntities = executor.query(query, EntityMapper.of(relatedTable.getEntityType()));

        // Step 3: Build related entity map by secondKey (grouping all entities with same key)
        Map<Object, List<R>> relatedByKey = new HashMap<>();
        for (R entity : relatedEntities) {
            Object key = ReflectionUtils.getFieldValue(entity, relation.getSecondKey());
            if (isNull(key)) {
                continue;
            }
            relatedByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(entity);
        }

        // Step 4: Assign related to parents using through mapping
        String fieldName = relation.getFieldName();
        if (isNull(fieldName)) {
            return Collections.emptyList();
        }

        boolean isToMany = relation.isToMany();
        for (T parent : parents) {
            Object parentKey = ReflectionUtils.getFieldValue(parent, relation.getLocalKey());
            List<Object> throughKeys = parentToRelatedKeys.getOrDefault(parentKey, Collections.emptyList());

            List<R> parentRelated = throughKeys.stream()
                    .flatMap(tk -> relatedByKey.getOrDefault(tk, Collections.emptyList()).stream())
                    .toList();

            if (isToMany) {
                Class<?> fieldType = determineFieldType(parent.getClass(), fieldName);
                Object collection = Set.class.isAssignableFrom(fieldType)
                        ? new HashSet<>(parentRelated)
                        : new ArrayList<>(parentRelated);
                ReflectionUtils.setFieldValue(parent, fieldName, collection);
                continue;
            }
            // HasOneThrough - take first or null
            R single = parentRelated.isEmpty() ? null : parentRelated.get(0);
            ReflectionUtils.setFieldValue(parent, fieldName, single);
        }

        return Collections.emptyList();
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
