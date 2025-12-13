package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.query.Suprim;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages relationship mutations for BelongsTo, HasOne, HasMany, and BelongsToMany relations.
 * All operations are executed within transactions for consistency.
 *
 * <pre>{@code
 * // BelongsTo: associate/dissociate
 * manager.associate(post, Post_.AUTHOR, user);
 * manager.dissociate(post, Post_.AUTHOR);
 *
 * // HasOne/HasMany: save/create
 * manager.save(user, User_.POSTS, newPost);
 * manager.saveMany(user, User_.POSTS, List.of(post1, post2));
 * manager.create(user, User_.POSTS, Map.of("title", "Hello"));
 *
 * // BelongsToMany: attach/detach/sync/toggle
 * manager.attach(user, User_.ROLES, roleId);
 * manager.attach(user, User_.ROLES, roleId, Map.of("assigned_by", 1));
 * manager.detach(user, User_.ROLES, roleId);
 * manager.detach(user, User_.ROLES); // detach all
 * manager.sync(user, User_.ROLES, List.of(1L, 2L, 3L));
 * manager.toggle(user, User_.ROLES, List.of(1L, 2L));
 * manager.updateExistingPivot(user, User_.ROLES, roleId, Map.of("is_primary", true));
 * }</pre>
 */
public final class RelationshipManager {

    private final Transaction transaction;

    RelationshipManager(Transaction transaction) {
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    // ==================== BELONGS_TO ====================

    /**
     * Associate a BelongsTo relationship by setting the foreign key on the child entity.
     * SQL: UPDATE {entity_table} SET {fk} = ? WHERE id = ?
     *
     * @param entity the child entity (owns the FK)
     * @param relation the BelongsTo relation
     * @param parent the parent entity to associate with
     * @param <T> child entity type
     * @param <R> parent entity type
     * @return number of affected rows
     * @throws IllegalArgumentException if relation type is not BELONGS_TO
     */
    public <T, R> int associate(T entity, Relation<T, R> relation, R parent) {
        validateRelationType(relation, Relation.Type.BELONGS_TO);

        Object entityId = EntityReflector.getId(entity);
        Object parentId = EntityReflector.getId(parent);

        // Build UPDATE query to set FK
        Table<T> ownerTable = relation.getOwnerTable();
        QueryResult update = Suprim.update(ownerTable)
                .set(createColumn(ownerTable, relation.getForeignKey(), Object.class), parentId)
                .where(createColumn(ownerTable, "id", Object.class).eq(entityId))
                .build();

        return transaction.execute(update);
    }

    /**
     * Dissociate a BelongsTo relationship by clearing the foreign key on the child entity.
     * SQL: UPDATE {entity_table} SET {fk} = NULL WHERE id = ?
     *
     * @param entity the child entity (owns the FK)
     * @param relation the BelongsTo relation
     * @param <T> child entity type
     * @param <R> parent entity type
     * @return number of affected rows
     * @throws IllegalArgumentException if relation type is not BELONGS_TO
     */
    public <T, R> int dissociate(T entity, Relation<T, R> relation) {
        validateRelationType(relation, Relation.Type.BELONGS_TO);

        Object entityId = EntityReflector.getId(entity);

        // Build UPDATE query to clear FK
        Table<T> ownerTable = relation.getOwnerTable();
        QueryResult update = Suprim.update(ownerTable)
                .set(createColumn(ownerTable, relation.getForeignKey(), Object.class), (Object) null)
                .where(createColumn(ownerTable, "id", Object.class).eq(entityId))
                .build();

        return transaction.execute(update);
    }

    // ==================== HAS_ONE / HAS_MANY ====================

    /**
     * Save a related entity by inserting it with the foreign key set to the parent's ID.
     * SQL: INSERT INTO {child_table} (..., fk) VALUES (..., parent_id)
     *
     * @param parent the parent entity
     * @param relation the HasOne or HasMany relation
     * @param child the child entity to save
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return the saved child entity
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> R save(T parent, Relation<T, R> relation, R child) {
        validateRelationType(relation, Relation.Type.HAS_ONE, Relation.Type.HAS_MANY);

        Object parentId = EntityReflector.getId(parent);

        // Set FK on child entity
        EntityReflector.setFieldByColumnName(child, relation.getForeignKey(), parentId);

        // Convert child to column map
        Map<String, Object> columnMap = EntityReflector.toColumnMap(child);

        // Build INSERT query
        var insertBuilder = Suprim.insertInto(relation.getRelatedTable());
        for (Map.Entry<String, Object> entry : columnMap.entrySet()) {
            insertBuilder.column(
                    createColumn(relation.getRelatedTable(), entry.getKey(), Object.class),
                    entry.getValue()
            );
        }

        QueryResult insert = insertBuilder.build();
        transaction.execute(insert);

        return child;
    }

    /**
     * Save multiple related entities by inserting them with the foreign key set to the parent's ID.
     *
     * @param parent the parent entity
     * @param relation the HasMany relation
     * @param children the list of child entities to save
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return the list of saved child entities
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> List<R> saveMany(T parent, Relation<T, R> relation, List<R> children) {
        validateRelationType(relation, Relation.Type.HAS_ONE, Relation.Type.HAS_MANY);

        for (R child : children) {
            save(parent, relation, child);
        }

        return children;
    }

    /**
     * Create a related entity from attribute map and save it.
     * SQL: INSERT INTO {child_table} (attr1, attr2, fk) VALUES (?, ?, parent_id)
     *
     * @param parent the parent entity
     * @param relation the HasOne or HasMany relation
     * @param attributes map of column names to values
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return number of affected rows
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> int create(T parent, Relation<T, R> relation, Map<String, Object> attributes) {
        validateRelationType(relation, Relation.Type.HAS_ONE, Relation.Type.HAS_MANY);

        Object parentId = EntityReflector.getId(parent);

        // Build INSERT query with attributes + FK
        var insertBuilder = Suprim.insertInto(relation.getRelatedTable());

        // Add provided attributes
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            insertBuilder.column(
                    createColumn(relation.getRelatedTable(), entry.getKey(), Object.class),
                    entry.getValue()
            );
        }

        // Add FK
        insertBuilder.column(
                createColumn(relation.getRelatedTable(), relation.getForeignKey(), Object.class),
                parentId
        );

        QueryResult insert = insertBuilder.build();
        return transaction.execute(insert);
    }

    // ==================== BELONGS_TO_MANY ====================

    /**
     * Attach a single related entity in the pivot table.
     * SQL: INSERT INTO {pivot} (fk, related_fk) VALUES (?, ?)
     *
     * @param parent the parent entity
     * @param relation the BelongsToMany relation
     * @param relatedId the ID of the related entity to attach
     * @param <T> parent entity type
     * @param <R> related entity type
     * @return number of affected rows
     * @throws IllegalArgumentException if relation type is not BELONGS_TO_MANY
     */
    public <T, R> int attach(T parent, Relation<T, R> relation, Object relatedId) {
        return attach(parent, relation, relatedId, null);
    }

    /**
     * Attach a single related entity in the pivot table with additional pivot attributes.
     * SQL: INSERT INTO {pivot} (fk, related_fk, attr1, attr2) VALUES (?, ?, ?, ?)
     *
     * @param parent the parent entity
     * @param relation the BelongsToMany relation
     * @param relatedId the ID of the related entity to attach
     * @param pivotAttributes additional pivot column values (can be null)
     * @param <T> parent entity type
     * @param <R> related entity type
     * @return number of affected rows
     * @throws IllegalArgumentException if relation type is not BELONGS_TO_MANY
     */
    public <T, R> int attach(T parent, Relation<T, R> relation, Object relatedId, Map<String, Object> pivotAttributes) {
        validateRelationType(relation, Relation.Type.BELONGS_TO_MANY);

        Object parentId = EntityReflector.getId(parent);
        Table<?> pivotTable = new Table<>(relation.getPivotTable(), "", Object.class);

        // Build INSERT query
        var insertBuilder = Suprim.insertInto(pivotTable)
                .column(createColumn(pivotTable, relation.getForeignPivotKey(), Object.class), parentId)
                .column(createColumn(pivotTable, relation.getRelatedPivotKey(), Object.class), relatedId);

        // Add pivot attributes
        if (Objects.nonNull(pivotAttributes)) {
            for (Map.Entry<String, Object> entry : pivotAttributes.entrySet()) {
                insertBuilder.column(
                        createColumn(pivotTable, entry.getKey(), Object.class),
                        entry.getValue()
                );
            }
        }

        // Add timestamps if enabled
        if (relation.hasPivotTimestamps()) {
            Instant now = Instant.now();
            insertBuilder.column(createColumn(pivotTable, "created_at", Instant.class), now);
            insertBuilder.column(createColumn(pivotTable, "updated_at", Instant.class), now);
        }

        QueryResult insert = insertBuilder.build();
        return transaction.execute(insert);
    }

    /**
     * Detach a single related entity from the pivot table.
     * SQL: DELETE FROM {pivot} WHERE fk = ? AND related_fk = ?
     *
     * @param parent the parent entity
     * @param relation the BelongsToMany relation
     * @param relatedId the ID of the related entity to detach
     * @param <T> parent entity type
     * @param <R> related entity type
     * @return number of affected rows
     * @throws IllegalArgumentException if relation type is not BELONGS_TO_MANY
     */
    public <T, R> int detach(T parent, Relation<T, R> relation, Object relatedId) {
        validateRelationType(relation, Relation.Type.BELONGS_TO_MANY);

        Object parentId = EntityReflector.getId(parent);
        Table<?> pivotTable = new Table<>(relation.getPivotTable(), "", Object.class);

        // Build DELETE query
        QueryResult delete = Suprim.deleteFrom(pivotTable)
                .where(createColumn(pivotTable, relation.getForeignPivotKey(), Object.class).eq(parentId))
                .and(createColumn(pivotTable, relation.getRelatedPivotKey(), Object.class).eq(relatedId))
                .build();

        return transaction.execute(delete);
    }

    /**
     * Detach all related entities from the pivot table.
     * SQL: DELETE FROM {pivot} WHERE fk = ?
     *
     * @param parent the parent entity
     * @param relation the BelongsToMany relation
     * @param <T> parent entity type
     * @param <R> related entity type
     * @return number of affected rows
     * @throws IllegalArgumentException if relation type is not BELONGS_TO_MANY
     */
    public <T, R> int detach(T parent, Relation<T, R> relation) {
        validateRelationType(relation, Relation.Type.BELONGS_TO_MANY);

        Object parentId = EntityReflector.getId(parent);
        Table<?> pivotTable = new Table<>(relation.getPivotTable(), "", Object.class);

        // Build DELETE query
        QueryResult delete = Suprim.deleteFrom(pivotTable)
                .where(createColumn(pivotTable, relation.getForeignPivotKey(), Object.class).eq(parentId))
                .build();

        return transaction.execute(delete);
    }

    /**
     * Sync the pivot table to match exactly the given list of IDs.
     * Detaches IDs not in the list, attaches IDs not currently present.
     *
     * @param parent the parent entity
     * @param relation the BelongsToMany relation
     * @param relatedIds the list of related IDs that should be present
     * @param <T> parent entity type
     * @param <R> related entity type
     * @return result with attached and detached counts
     * @throws IllegalArgumentException if relation type is not BELONGS_TO_MANY
     */
    public <T, R> SyncResult sync(T parent, Relation<T, R> relation, List<?> relatedIds) {
        validateRelationType(relation, Relation.Type.BELONGS_TO_MANY);

        // Get current attachments
        Set<Object> currentIds = getCurrentAttachments(parent, relation);
        Set<Object> desiredIds = new HashSet<>(relatedIds);

        // Calculate diff
        Set<Object> toAttach = new HashSet<>(desiredIds);
        toAttach.removeAll(currentIds);

        Set<Object> toDetach = new HashSet<>(currentIds);
        toDetach.removeAll(desiredIds);

        // Execute changes
        int attachedCount = 0;
        int detachedCount = 0;

        for (Object id : toDetach) {
            detachedCount += detach(parent, relation, id);
        }

        for (Object id : toAttach) {
            attachedCount += attach(parent, relation, id);
        }

        return new SyncResult(attachedCount, detachedCount);
    }

    /**
     * Sync without detaching - only attach missing IDs, never remove existing ones.
     *
     * @param parent the parent entity
     * @param relation the BelongsToMany relation
     * @param relatedIds the list of related IDs to ensure are present
     * @param <T> parent entity type
     * @param <R> related entity type
     * @return number of attached rows
     * @throws IllegalArgumentException if relation type is not BELONGS_TO_MANY
     */
    public <T, R> int syncWithoutDetaching(T parent, Relation<T, R> relation, List<?> relatedIds) {
        validateRelationType(relation, Relation.Type.BELONGS_TO_MANY);

        // Get current attachments
        Set<Object> currentIds = getCurrentAttachments(parent, relation);
        Set<Object> desiredIds = new HashSet<>(relatedIds);

        // Only attach missing
        Set<Object> toAttach = new HashSet<>(desiredIds);
        toAttach.removeAll(currentIds);

        int attachedCount = 0;
        for (Object id : toAttach) {
            attachedCount += attach(parent, relation, id);
        }

        return attachedCount;
    }

    /**
     * Toggle attachments - attach if missing, detach if present.
     *
     * @param parent the parent entity
     * @param relation the BelongsToMany relation
     * @param relatedIds the list of related IDs to toggle
     * @param <T> parent entity type
     * @param <R> related entity type
     * @return result with lists of attached and detached IDs
     * @throws IllegalArgumentException if relation type is not BELONGS_TO_MANY
     */
    public <T, R> ToggleResult toggle(T parent, Relation<T, R> relation, List<?> relatedIds) {
        validateRelationType(relation, Relation.Type.BELONGS_TO_MANY);

        // Get current attachments
        Set<Object> currentIds = getCurrentAttachments(parent, relation);

        List<Object> attached = new ArrayList<>();
        List<Object> detached = new ArrayList<>();

        for (Object id : relatedIds) {
            if (currentIds.contains(id)) {
                detach(parent, relation, id);
                detached.add(id);
            } else {
                attach(parent, relation, id);
                attached.add(id);
            }
        }

        return new ToggleResult(attached, detached);
    }

    /**
     * Update existing pivot record attributes.
     * SQL: UPDATE {pivot} SET attr = ? WHERE fk = ? AND related_fk = ?
     *
     * @param parent the parent entity
     * @param relation the BelongsToMany relation
     * @param relatedId the ID of the related entity
     * @param attributes the pivot attributes to update
     * @param <T> parent entity type
     * @param <R> related entity type
     * @return number of affected rows
     * @throws IllegalArgumentException if relation type is not BELONGS_TO_MANY
     */
    public <T, R> int updateExistingPivot(T parent, Relation<T, R> relation, Object relatedId, Map<String, Object> attributes) {
        validateRelationType(relation, Relation.Type.BELONGS_TO_MANY);

        Object parentId = EntityReflector.getId(parent);
        Table<?> pivotTable = new Table<>(relation.getPivotTable(), "", Object.class);

        // Build UPDATE query
        var updateBuilder = Suprim.update(pivotTable);

        // Set attributes
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            updateBuilder.set(
                    createColumn(pivotTable, entry.getKey(), Object.class),
                    entry.getValue()
            );
        }

        // Update timestamp if enabled
        if (relation.hasPivotTimestamps()) {
            updateBuilder.set(createColumn(pivotTable, "updated_at", Instant.class), Instant.now());
        }

        // WHERE clause
        updateBuilder.where(createColumn(pivotTable, relation.getForeignPivotKey(), Object.class).eq(parentId))
                .and(createColumn(pivotTable, relation.getRelatedPivotKey(), Object.class).eq(relatedId));

        QueryResult update = updateBuilder.build();
        return transaction.execute(update);
    }

    // ==================== ADVANCED MUTATIONS ====================

    /**
     * Batch create multiple related entities from attribute maps.
     * SQL: INSERT INTO {child_table} (attr1, attr2, fk) VALUES (?, ?, ?), (?, ?, ?)...
     *
     * @param parent the parent entity
     * @param relation the HasOne or HasMany relation
     * @param attributesList list of attribute maps for each entity to create
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return number of entities created
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> int createMany(T parent, Relation<T, R> relation, List<Map<String, Object>> attributesList) {
        validateRelationType(relation, Relation.Type.HAS_ONE, Relation.Type.HAS_MANY);

        int count = 0;
        for (Map<String, Object> attributes : attributesList) {
            count += create(parent, relation, attributes);
        }
        return count;
    }

    /**
     * Find a related entity matching criteria, or create it if not found.
     * SQL: SELECT * FROM {child_table} WHERE fk = ? AND criteria1 = ? ... LIMIT 1
     * If not found: INSERT INTO {child_table} (criteria, defaults, fk) VALUES (...)
     *
     * @param parent the parent entity
     * @param relation the HasOne or HasMany relation
     * @param criteria search criteria (column names to values)
     * @param defaults default values for creation only (merged with criteria)
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return the found or created entity
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> R firstOrCreate(T parent, Relation<T, R> relation, Map<String, Object> criteria, Map<String, Object> defaults) {
        validateRelationType(relation, Relation.Type.HAS_ONE, Relation.Type.HAS_MANY);

        // Try to find existing
        R existing = findByCriteria(parent, relation, criteria);
        if (Objects.nonNull(existing)) {
            return existing;
        }

        // Not found - create new
        Map<String, Object> allAttributes = new LinkedHashMap<>();
        if (Objects.nonNull(defaults)) {
            allAttributes.putAll(defaults);
        }
        allAttributes.putAll(criteria);

        create(parent, relation, allAttributes);

        // Query back the created entity
        return findByCriteria(parent, relation, criteria);
    }

    /**
     * Find a related entity matching criteria, or instantiate a new one (without saving).
     * SQL: SELECT * FROM {child_table} WHERE fk = ? AND criteria1 = ? ... LIMIT 1
     * If not found: return new instance (not persisted)
     *
     * @param parent the parent entity
     * @param relation the HasOne or HasMany relation
     * @param criteria search criteria (column names to values)
     * @param defaults default values for instantiation only (merged with criteria)
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return the found entity or new instance
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> R firstOrNew(T parent, Relation<T, R> relation, Map<String, Object> criteria, Map<String, Object> defaults) {
        validateRelationType(relation, Relation.Type.HAS_ONE, Relation.Type.HAS_MANY);

        // Try to find existing
        R existing = findByCriteria(parent, relation, criteria);
        if (Objects.nonNull(existing)) {
            return existing;
        }

        // Not found - create new instance (not saved)
        Map<String, Object> allAttributes = new LinkedHashMap<>();
        if (Objects.nonNull(defaults)) {
            allAttributes.putAll(defaults);
        }
        allAttributes.putAll(criteria);

        // Set FK in attributes
        Object parentId = EntityReflector.getId(parent);
        allAttributes.put(relation.getForeignKey(), parentId);

        return EntityReflector.fromMap(relation.getRelatedTable().getEntityType(), allAttributes);
    }

    /**
     * Update an existing related entity matching criteria, or create it if not found (upsert).
     * SQL: SELECT * FROM {child_table} WHERE fk = ? AND criteria1 = ? ... LIMIT 1
     * If found: UPDATE {child_table} SET values... WHERE id = ?
     * If not found: INSERT INTO {child_table} (criteria, values, fk) VALUES (...)
     *
     * @param parent the parent entity
     * @param relation the HasOne or HasMany relation
     * @param criteria search criteria (column names to values)
     * @param values values to set (on update) or include (on create)
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return the updated or created entity
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> R updateOrCreate(T parent, Relation<T, R> relation, Map<String, Object> criteria, Map<String, Object> values) {
        validateRelationType(relation, Relation.Type.HAS_ONE, Relation.Type.HAS_MANY);

        // Try to find existing
        R existing = findByCriteria(parent, relation, criteria);

        if (Objects.nonNull(existing)) {
            // Update existing
            Object entityId = EntityReflector.getId(existing);
            Table<R> relatedTable = relation.getRelatedTable();

            var updateBuilder = Suprim.update(relatedTable);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                updateBuilder.set(
                        createColumn(relatedTable, entry.getKey(), Object.class),
                        entry.getValue()
                );
            }
            updateBuilder.where(createColumn(relatedTable, "id", Object.class).eq(entityId));

            QueryResult update = updateBuilder.build();
            transaction.execute(update);

            // Query back the updated entity
            return findByCriteria(parent, relation, criteria);
        } else {
            // Create new
            Map<String, Object> allAttributes = new LinkedHashMap<>();
            allAttributes.putAll(criteria);
            if (Objects.nonNull(values)) {
                allAttributes.putAll(values);
            }

            create(parent, relation, allAttributes);

            // Query back the created entity
            return findByCriteria(parent, relation, criteria);
        }
    }

    /**
     * Delete all related entities.
     * SQL: DELETE FROM {child_table} WHERE fk = ?
     *
     * @param parent the parent entity
     * @param relation the HasOne or HasMany relation
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return number of entities deleted
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> int delete(T parent, Relation<T, R> relation) {
        validateRelationType(relation, Relation.Type.HAS_ONE, Relation.Type.HAS_MANY);

        Object parentId = EntityReflector.getId(parent);
        Table<R> relatedTable = relation.getRelatedTable();

        QueryResult delete = Suprim.deleteFrom(relatedTable)
                .where(createColumn(relatedTable, relation.getForeignKey(), Object.class).eq(parentId))
                .build();

        return transaction.execute(delete);
    }

    /**
     * Delete related entities matching additional constraints.
     * SQL: DELETE FROM {child_table} WHERE fk = ? AND constraint...
     *
     * @param parent the parent entity
     * @param relation the HasOne or HasMany relation
     * @param constraint additional WHERE clause constraint
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return number of entities deleted
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> int delete(T parent, Relation<T, R> relation, java.util.function.Function<SelectBuilder, SelectBuilder> constraint) {
        validateRelationType(relation, Relation.Type.HAS_ONE, Relation.Type.HAS_MANY);

        Object parentId = EntityReflector.getId(parent);
        Table<R> relatedTable = relation.getRelatedTable();

        // Build base SELECT to get IDs matching constraint
        var selectBuilder = Suprim.select(createColumn(relatedTable, "id", Object.class))
                .from(relatedTable)
                .where(createColumn(relatedTable, relation.getForeignKey(), Object.class).eq(parentId));

        // Apply constraint
        selectBuilder = constraint.apply(selectBuilder);
        QueryResult selectQuery = selectBuilder.build();

        // Get IDs to delete
        List<Object> idsToDelete = transaction.query(selectQuery, rs -> rs.getObject(1));

        if (idsToDelete.isEmpty()) {
            return 0;
        }

        // Build DELETE with IN clause
        QueryResult delete = Suprim.deleteFrom(relatedTable)
                .where(createColumn(relatedTable, "id", Object.class).in(idsToDelete.toArray()))
                .build();

        return transaction.execute(delete);
    }

    /**
     * Hard delete all related entities (same as delete, but explicitly documented for soft-delete awareness).
     * SQL: DELETE FROM {child_table} WHERE fk = ?
     *
     * @param parent the parent entity
     * @param relation the HasOne or HasMany relation
     * @param <T> parent entity type
     * @param <R> child entity type
     * @return number of entities deleted
     * @throws IllegalArgumentException if relation type is not HAS_ONE or HAS_MANY
     */
    public <T, R> int forceDelete(T parent, Relation<T, R> relation) {
        // For now, forceDelete is the same as delete
        // In future with soft delete support, this would bypass the deleted_at check
        return delete(parent, relation);
    }

    // ==================== TOUCH PARENT TIMESTAMPS ====================

    /**
     * Touch timestamps on a parent entity by updating specified columns to current timestamp.
     * SQL: UPDATE {parent_table} SET col1 = NOW(), col2 = NOW() WHERE id = ?
     *
     * @param parentTable the parent entity's table
     * @param parentId the parent entity's ID
     * @param touchColumns the list of timestamp columns to update
     */
    public void touchTimestamps(Table<?> parentTable, Object parentId, List<String> touchColumns) {
        if (Objects.isNull(touchColumns) || touchColumns.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        var updateBuilder = Suprim.update(parentTable);

        // Set each touch column to current timestamp
        for (String columnName : touchColumns) {
            updateBuilder.set(createColumn(parentTable, columnName, Instant.class), now);
        }

        // WHERE id = parentId
        updateBuilder.where(createColumn(parentTable, "id", Object.class).eq(parentId));

        QueryResult update = updateBuilder.build();
        transaction.execute(update);
    }

    // ==================== HELPERS ====================

    /**
     * Find a related entity by criteria.
     * Returns null if not found.
     */
    private <T, R> R findByCriteria(T parent, Relation<T, R> relation, Map<String, Object> criteria) {
        Object parentId = EntityReflector.getId(parent);
        Table<R> relatedTable = relation.getRelatedTable();

        // Build SELECT query
        var selectBuilder = Suprim.selectAll()
                .from(relatedTable)
                .where(createColumn(relatedTable, relation.getForeignKey(), Object.class).eq(parentId));

        // Add criteria to WHERE clause
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            selectBuilder.and(createColumn(relatedTable, entry.getKey(), Object.class).eq(entry.getValue()));
        }

        selectBuilder.limit(1);

        QueryResult query = selectBuilder.build();
        List<R> results = transaction.query(query, EntityMapper.of(relatedTable.getEntityType()));

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get current pivot table attachments for a parent entity.
     *
     * @return set of related IDs currently attached
     */
    private <T, R> Set<Object> getCurrentAttachments(T parent, Relation<T, R> relation) {
        Object parentId = EntityReflector.getId(parent);
        Table<?> pivotTable = new Table<>(relation.getPivotTable(), "", Object.class);

        // Build SELECT query
        QueryResult query = Suprim.select(createColumn(pivotTable, relation.getRelatedPivotKey(), Object.class))
                .from(pivotTable)
                .where(createColumn(pivotTable, relation.getForeignPivotKey(), Object.class).eq(parentId))
                .build();

        List<Object> results = transaction.query(query, rs -> rs.getObject(1));
        return new HashSet<>(results);
    }

    /**
     * Validate that the relation is of the expected type(s).
     */
    private void validateRelationType(Relation<?, ?> relation, Relation.Type... expectedTypes) {
        for (Relation.Type expectedType : expectedTypes) {
            if (relation.getType() == expectedType) {
                return;
            }
        }
        throw new IllegalArgumentException(
                "Invalid relation type: " + relation.getType() +
                        ", expected one of: " + Arrays.toString(expectedTypes)
        );
    }

    /**
     * Create a Column instance for use in query builders.
     * This is a helper to work around the type-safe column system.
     */
    private <T, V> Column<T, V> createColumn(Table<T> table, String columnName, Class<V> valueType) {
        return new Column<>(table, columnName, valueType, "");
    }
}
