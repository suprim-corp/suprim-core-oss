package sant1ago.dev.suprim.core.type;

import java.util.List;
import java.util.Objects;

/**
 * Represents a relationship between entities for type-safe query building.
 *
 * @param <T> The owner entity class
 * @param <R> The related entity class
 */
public final class Relation<T, R> {

    /**
     * Type of relationship.
     */
    public enum Type {
        HAS_ONE,
        HAS_MANY,
        BELONGS_TO,
        BELONGS_TO_MANY,
        HAS_ONE_THROUGH,
        HAS_MANY_THROUGH,
        MORPH_ONE,
        MORPH_MANY,
        MORPH_TO,
        MORPH_TO_MANY,
        MORPHED_BY_MANY,
        LATEST_OF_MANY,
        OLDEST_OF_MANY,
        OF_MANY;

        public boolean isThrough() {
            return this == HAS_ONE_THROUGH || this == HAS_MANY_THROUGH;
        }

        public boolean isToMany() {
            return this == HAS_MANY || this == BELONGS_TO_MANY || this == HAS_MANY_THROUGH || this == MORPH_MANY || this == MORPH_TO_MANY || this == MORPHED_BY_MANY;
        }

        public boolean isMorphic() {
            return this == MORPH_ONE || this == MORPH_MANY || this == MORPH_TO || this == MORPH_TO_MANY || this == MORPHED_BY_MANY;
        }

        public boolean isOfMany() {
            return this == LATEST_OF_MANY || this == OLDEST_OF_MANY || this == OF_MANY;
        }
    }

    private final Type type;
    private final Table<T> ownerTable;
    private final Table<R> relatedTable;
    private final String foreignKey;
    private final String localKey;
    private final String relatedKey;
    private final String pivotTable;
    private final String foreignPivotKey;
    private final String relatedPivotKey;
    private final List<String> pivotColumns;
    private final boolean pivotTimestamps;
    private final boolean noForeignKey;
    private final boolean eager;
    private final String fieldName;

    // Through relationship fields
    private final Table<?> throughTable;
    private final String firstKey;      // FK on through table pointing to owner
    private final String secondKey;     // FK on final table pointing to through entity
    private final String secondLocalKey; // PK on through table

    // Morph relationship fields
    private final String morphName;
    private final String morphTypeColumn;
    private final String morphIdColumn;

    // Touch parent timestamps (for BelongsTo)
    private final boolean shouldTouch;
    private final List<String> touchColumns;

    // OfMany relationship fields (for LATEST_OF_MANY, OLDEST_OF_MANY, OF_MANY)
    private final String orderColumn;
    private final String aggregateColumn;
    private final String aggregateFunction;

    // Default model support
    private final boolean withDefault;
    private final List<String> defaultAttributes;

    private Relation(Builder<T, R> builder) {
        this.type = builder.type;
        this.ownerTable = builder.ownerTable;
        this.relatedTable = builder.relatedTable;
        this.foreignKey = builder.foreignKey;
        this.localKey = builder.localKey;
        this.relatedKey = builder.relatedKey;
        this.pivotTable = builder.pivotTable;
        this.foreignPivotKey = builder.foreignPivotKey;
        this.relatedPivotKey = builder.relatedPivotKey;
        this.pivotColumns = builder.pivotColumns;
        this.pivotTimestamps = builder.pivotTimestamps;
        this.noForeignKey = builder.noForeignKey;
        this.eager = builder.eager;
        this.fieldName = builder.fieldName;
        this.throughTable = builder.throughTable;
        this.firstKey = builder.firstKey;
        this.secondKey = builder.secondKey;
        this.secondLocalKey = builder.secondLocalKey;
        this.morphName = builder.morphName;
        this.morphTypeColumn = builder.morphTypeColumn;
        this.morphIdColumn = builder.morphIdColumn;
        this.shouldTouch = builder.shouldTouch;
        this.touchColumns = builder.touchColumns;
        this.orderColumn = builder.orderColumn;
        this.aggregateColumn = builder.aggregateColumn;
        this.aggregateFunction = builder.aggregateFunction;
        this.withDefault = builder.withDefault;
        this.defaultAttributes = builder.defaultAttributes;
    }

    // ==================== Factory Methods ====================

    /**
     * Create a HAS_ONE relationship with default support.
     */
    public static <T, R> Relation<T, R> hasOne(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            boolean noForeignKey,
            boolean eager,
            String fieldName,
            boolean withDefault,
            List<String> defaultAttributes
    ) {
        return new Builder<>(Type.HAS_ONE, ownerTable, relatedTable)
                .foreignKey(foreignKey)
                .localKey(localKey)
                .noForeignKey(noForeignKey)
                .eager(eager)
                .fieldName(fieldName)
                .withDefault(withDefault)
                .defaultAttributes(defaultAttributes)
                .build();
    }

    /**
     * Create a HAS_ONE relationship without defaults (for backward compatibility).
     */
    public static <T, R> Relation<T, R> hasOne(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            boolean noForeignKey,
            boolean eager,
            String fieldName
    ) {
        return hasOne(ownerTable, relatedTable, foreignKey, localKey, noForeignKey, eager, fieldName, false, List.of());
    }

    /**
     * Create a HAS_ONE relationship without field name (for backward compatibility).
     */
    public static <T, R> Relation<T, R> hasOne(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            boolean noForeignKey,
            boolean eager
    ) {
        return hasOne(ownerTable, relatedTable, foreignKey, localKey, noForeignKey, eager, null);
    }

    /**
     * Create a HAS_MANY relationship.
     * User hasMany Posts (posts.user_id → users.id)
     */
    public static <T, R> Relation<T, R> hasMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            boolean noForeignKey,
            boolean eager,
            String fieldName
    ) {
        return new Builder<>(Type.HAS_MANY, ownerTable, relatedTable)
                .foreignKey(foreignKey)
                .localKey(localKey)
                .noForeignKey(noForeignKey)
                .eager(eager)
                .fieldName(fieldName)
                .build();
    }

    public static <T, R> Relation<T, R> hasMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            boolean noForeignKey,
            boolean eager
    ) {
        return hasMany(ownerTable, relatedTable, foreignKey, localKey, noForeignKey, eager, null);
    }

    /**
     * Create a BELONGS_TO relationship.
     * Post belongsTo User (posts.user_id → users.id)
     */
    public static <T, R> Relation<T, R> belongsTo(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String ownerKey,
            boolean noForeignKey,
            boolean eager,
            String fieldName
    ) {
        return belongsTo(ownerTable, relatedTable, foreignKey, ownerKey, noForeignKey, eager, fieldName, false, List.of("updated_at"));
    }

    /**
     * Create a BELONGS_TO relationship with touch and default support.
     */
    public static <T, R> Relation<T, R> belongsTo(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String ownerKey,
            boolean noForeignKey,
            boolean eager,
            String fieldName,
            boolean shouldTouch,
            List<String> touchColumns,
            boolean withDefault,
            List<String> defaultAttributes
    ) {
        return new Builder<>(Type.BELONGS_TO, ownerTable, relatedTable)
                .foreignKey(foreignKey)
                .relatedKey(ownerKey)
                .noForeignKey(noForeignKey)
                .eager(eager)
                .fieldName(fieldName)
                .shouldTouch(shouldTouch)
                .touchColumns(touchColumns)
                .withDefault(withDefault)
                .defaultAttributes(defaultAttributes)
                .build();
    }

    /**
     * Create a BELONGS_TO relationship with touch support (for backward compatibility).
     */
    public static <T, R> Relation<T, R> belongsTo(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String ownerKey,
            boolean noForeignKey,
            boolean eager,
            String fieldName,
            boolean shouldTouch,
            List<String> touchColumns
    ) {
        return belongsTo(ownerTable, relatedTable, foreignKey, ownerKey, noForeignKey, eager, fieldName,
                shouldTouch, touchColumns, false, List.of());
    }

    public static <T, R> Relation<T, R> belongsTo(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String ownerKey,
            boolean noForeignKey,
            boolean eager
    ) {
        return belongsTo(ownerTable, relatedTable, foreignKey, ownerKey, noForeignKey, eager, null);
    }

    /**
     * Create a BELONGS_TO_MANY relationship.
     * User belongsToMany Roles (via role_user pivot)
     */
    public static <T, R> Relation<T, R> belongsToMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String pivotTable,
            String foreignPivotKey,
            String relatedPivotKey,
            String localKey,
            String relatedKey,
            List<String> pivotColumns,
            boolean pivotTimestamps,
            boolean eager,
            String fieldName
    ) {
        return new Builder<>(Type.BELONGS_TO_MANY, ownerTable, relatedTable)
                .pivotTable(pivotTable)
                .foreignPivotKey(foreignPivotKey)
                .relatedPivotKey(relatedPivotKey)
                .localKey(localKey)
                .relatedKey(relatedKey)
                .pivotColumns(pivotColumns)
                .pivotTimestamps(pivotTimestamps)
                .eager(eager)
                .fieldName(fieldName)
                .build();
    }

    public static <T, R> Relation<T, R> belongsToMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String pivotTable,
            String foreignPivotKey,
            String relatedPivotKey,
            String localKey,
            String relatedKey,
            List<String> pivotColumns,
            boolean pivotTimestamps,
            boolean eager
    ) {
        return belongsToMany(ownerTable, relatedTable, pivotTable, foreignPivotKey, relatedPivotKey,
                localKey, relatedKey, pivotColumns, pivotTimestamps, eager, null);
    }

    /**
     * Create a HAS_ONE_THROUGH relationship.
     * Mechanic hasOneThrough Owner via Car
     * (mechanics → cars.mechanic_id → owners.car_id)
     *
     * @param ownerTable    owner entity table (mechanics)
     * @param relatedTable  final related entity table (owners)
     * @param throughTable  intermediate entity table (cars)
     * @param firstKey      FK on through table pointing to owner (mechanic_id)
     * @param secondKey     FK on final table pointing to through entity (car_id)
     * @param localKey      owner PK (id)
     * @param secondLocalKey through entity PK (id)
     */
    public static <T, R> Relation<T, R> hasOneThrough(
            Table<T> ownerTable,
            Table<R> relatedTable,
            Table<?> throughTable,
            String firstKey,
            String secondKey,
            String localKey,
            String secondLocalKey,
            boolean eager,
            String fieldName
    ) {
        return new Builder<>(Type.HAS_ONE_THROUGH, ownerTable, relatedTable)
                .throughTable(throughTable)
                .firstKey(firstKey)
                .secondKey(secondKey)
                .localKey(localKey)
                .secondLocalKey(secondLocalKey)
                .eager(eager)
                .fieldName(fieldName)
                .build();
    }

    public static <T, R> Relation<T, R> hasOneThrough(
            Table<T> ownerTable,
            Table<R> relatedTable,
            Table<?> throughTable,
            String firstKey,
            String secondKey,
            String localKey,
            String secondLocalKey,
            boolean eager
    ) {
        return hasOneThrough(ownerTable, relatedTable, throughTable, firstKey, secondKey,
                localKey, secondLocalKey, eager, null);
    }

    /**
     * Create a HAS_MANY_THROUGH relationship.
     * Country hasManyThrough Posts via Users
     * (countries → users.country_id → posts.user_id)
     *
     * @param ownerTable    owner entity table (countries)
     * @param relatedTable  final related entity table (posts)
     * @param throughTable  intermediate entity table (users)
     * @param firstKey      FK on through table pointing to owner (country_id)
     * @param secondKey     FK on final table pointing to through entity (user_id)
     * @param localKey      owner PK (id)
     * @param secondLocalKey through entity PK (id)
     */
    public static <T, R> Relation<T, R> hasManyThrough(
            Table<T> ownerTable,
            Table<R> relatedTable,
            Table<?> throughTable,
            String firstKey,
            String secondKey,
            String localKey,
            String secondLocalKey,
            boolean eager,
            String fieldName
    ) {
        return new Builder<>(Type.HAS_MANY_THROUGH, ownerTable, relatedTable)
                .throughTable(throughTable)
                .firstKey(firstKey)
                .secondKey(secondKey)
                .localKey(localKey)
                .secondLocalKey(secondLocalKey)
                .eager(eager)
                .fieldName(fieldName)
                .build();
    }

    public static <T, R> Relation<T, R> hasManyThrough(
            Table<T> ownerTable,
            Table<R> relatedTable,
            Table<?> throughTable,
            String firstKey,
            String secondKey,
            String localKey,
            String secondLocalKey,
            boolean eager
    ) {
        return hasManyThrough(ownerTable, relatedTable, throughTable, firstKey, secondKey,
                localKey, secondLocalKey, eager, null);
    }

    /**
     * Create a MORPH_ONE relationship.
     * User morphOne Image (images.imageable_type = 'User', images.imageable_id = users.id)
     */
    public static <T, R> Relation<T, R> morphOne(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String morphName,
            String morphTypeColumn,
            String morphIdColumn,
            String localKey,
            boolean eager
    ) {
        return new Builder<>(Type.MORPH_ONE, ownerTable, relatedTable)
                .localKey(localKey)
                .eager(eager)
                .morphName(morphName)
                .morphTypeColumn(morphTypeColumn)
                .morphIdColumn(morphIdColumn)
                .build();
    }

    /**
     * Create a MORPH_MANY relationship.
     * Post morphMany Comments (comments.commentable_type = 'Post', comments.commentable_id = posts.id)
     */
    public static <T, R> Relation<T, R> morphMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String morphName,
            String morphTypeColumn,
            String morphIdColumn,
            String localKey,
            boolean eager
    ) {
        return new Builder<>(Type.MORPH_MANY, ownerTable, relatedTable)
                .localKey(localKey)
                .eager(eager)
                .morphName(morphName)
                .morphTypeColumn(morphTypeColumn)
                .morphIdColumn(morphIdColumn)
                .build();
    }

    /**
     * Create a MORPH_TO relationship.
     * Image morphTo imageable (imageable_type VARCHAR, imageable_id BIGINT)
     */
    public static <T> Relation<T, Object> morphTo(
            Table<T> ownerTable,
            String morphName,
            String morphTypeColumn,
            String morphIdColumn,
            boolean eager
    ) {
        Table<Object> relatedTable = TypeUtils.castTable(ownerTable);
        return new Builder<>(Type.MORPH_TO, ownerTable, relatedTable)
                .eager(eager)
                .morphName(morphName)
                .morphTypeColumn(morphTypeColumn)
                .morphIdColumn(morphIdColumn)
                .build();
    }

    /**
     * Create a MORPH_TO_MANY relationship.
     * Post morphToMany Tags (taggables: taggable_type, taggable_id, tag_id)
     */
    public static <T, R> Relation<T, R> morphToMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String pivotTable,
            String morphName,
            String morphTypeColumn,
            String morphIdColumn,
            String relatedPivotKey,
            String localKey,
            String relatedKey,
            List<String> pivotColumns,
            boolean pivotTimestamps,
            boolean eager
    ) {
        return new Builder<>(Type.MORPH_TO_MANY, ownerTable, relatedTable)
                .pivotTable(pivotTable)
                .localKey(localKey)
                .relatedKey(relatedKey)
                .relatedPivotKey(relatedPivotKey)
                .pivotColumns(pivotColumns)
                .pivotTimestamps(pivotTimestamps)
                .eager(eager)
                .morphName(morphName)
                .morphTypeColumn(morphTypeColumn)
                .morphIdColumn(morphIdColumn)
                .build();
    }

    /**
     * Create a MORPHED_BY_MANY relationship (inverse of MorphToMany).
     * Tag morphedByMany Posts (taggables: tag_id, taggable_type, taggable_id)
     *
     * @param ownerTable         owner entity table (tags)
     * @param relatedTable       related entity table (posts)
     * @param pivotTable         pivot table name (taggables)
     * @param morphName          morph name (taggable)
     * @param morphTypeColumn    type discriminator column (taggable_type)
     * @param morphIdColumn      related FK column (taggable_id)
     * @param foreignPivotKey    owner FK column (tag_id)
     * @param localKey           owner PK (id)
     * @param relatedKey         related PK (id)
     * @param pivotColumns       additional pivot columns
     * @param pivotTimestamps    whether pivot has timestamps
     * @param eager              whether to eager load
     */
    public static <T, R> Relation<T, R> morphedByMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String pivotTable,
            String morphName,
            String morphTypeColumn,
            String morphIdColumn,
            String foreignPivotKey,
            String localKey,
            String relatedKey,
            List<String> pivotColumns,
            boolean pivotTimestamps,
            boolean eager
    ) {
        return new Builder<>(Type.MORPHED_BY_MANY, ownerTable, relatedTable)
                .pivotTable(pivotTable)
                .foreignPivotKey(foreignPivotKey)
                .localKey(localKey)
                .relatedKey(relatedKey)
                .pivotColumns(pivotColumns)
                .pivotTimestamps(pivotTimestamps)
                .eager(eager)
                .morphName(morphName)
                .morphTypeColumn(morphTypeColumn)
                .morphIdColumn(morphIdColumn)
                .build();
    }

    /**
     * Create a LATEST_OF_MANY relationship.
     * User latestOfMany Order (orders.user_id → users.id, ORDER BY created_at DESC LIMIT 1)
     *
     * @param ownerTable    owner entity table (users)
     * @param relatedTable  related entity table (orders)
     * @param foreignKey    FK on related table (user_id)
     * @param localKey      owner PK (id)
     * @param orderColumn   column to order by descending (created_at)
     * @param eager         whether to eager load
     * @param fieldName     field name for reference
     */
    public static <T, R> Relation<T, R> latestOfMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            String orderColumn,
            boolean eager,
            String fieldName
    ) {
        return new Builder<>(Type.LATEST_OF_MANY, ownerTable, relatedTable)
                .foreignKey(foreignKey)
                .localKey(localKey)
                .orderColumn(orderColumn)
                .eager(eager)
                .fieldName(fieldName)
                .build();
    }

    /**
     * Create a LATEST_OF_MANY relationship without field name.
     */
    public static <T, R> Relation<T, R> latestOfMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            String orderColumn,
            boolean eager
    ) {
        return latestOfMany(ownerTable, relatedTable, foreignKey, localKey, orderColumn, eager, null);
    }

    /**
     * Create an OLDEST_OF_MANY relationship.
     * User oldestOfMany Order (orders.user_id → users.id, ORDER BY created_at ASC LIMIT 1)
     *
     * @param ownerTable    owner entity table (users)
     * @param relatedTable  related entity table (orders)
     * @param foreignKey    FK on related table (user_id)
     * @param localKey      owner PK (id)
     * @param orderColumn   column to order by ascending (created_at)
     * @param eager         whether to eager load
     * @param fieldName     field name for reference
     */
    public static <T, R> Relation<T, R> oldestOfMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            String orderColumn,
            boolean eager,
            String fieldName
    ) {
        return new Builder<>(Type.OLDEST_OF_MANY, ownerTable, relatedTable)
                .foreignKey(foreignKey)
                .localKey(localKey)
                .orderColumn(orderColumn)
                .eager(eager)
                .fieldName(fieldName)
                .build();
    }

    /**
     * Create an OLDEST_OF_MANY relationship without field name.
     */
    public static <T, R> Relation<T, R> oldestOfMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            String orderColumn,
            boolean eager
    ) {
        return oldestOfMany(ownerTable, relatedTable, foreignKey, localKey, orderColumn, eager, null);
    }

    /**
     * Create an OF_MANY relationship.
     * Auction ofMany Bid (bids.auction_id → auctions.id, aggregate MAX on amount)
     *
     * @param ownerTable        owner entity table (auctions)
     * @param relatedTable      related entity table (bids)
     * @param foreignKey        FK on related table (auction_id)
     * @param localKey          owner PK (id)
     * @param aggregateColumn   column to apply aggregate (amount)
     * @param aggregateFunction aggregate function (MAX, MIN, etc.)
     * @param eager             whether to eager load
     * @param fieldName         field name for reference
     */
    public static <T, R> Relation<T, R> ofMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            String aggregateColumn,
            String aggregateFunction,
            boolean eager,
            String fieldName
    ) {
        return new Builder<>(Type.OF_MANY, ownerTable, relatedTable)
                .foreignKey(foreignKey)
                .localKey(localKey)
                .aggregateColumn(aggregateColumn)
                .aggregateFunction(aggregateFunction)
                .eager(eager)
                .fieldName(fieldName)
                .build();
    }

    /**
     * Create an OF_MANY relationship without field name.
     */
    public static <T, R> Relation<T, R> ofMany(
            Table<T> ownerTable,
            Table<R> relatedTable,
            String foreignKey,
            String localKey,
            String aggregateColumn,
            String aggregateFunction,
            boolean eager
    ) {
        return ofMany(ownerTable, relatedTable, foreignKey, localKey, aggregateColumn, aggregateFunction, eager, null);
    }

    // ==================== Getters ====================

    public Type getType() {
        return type;
    }

    public Table<T> getOwnerTable() {
        return ownerTable;
    }

    public Table<R> getRelatedTable() {
        return relatedTable;
    }

    public String getForeignKey() {
        return foreignKey;
    }

    public String getLocalKey() {
        return localKey;
    }

    public String getRelatedKey() {
        return relatedKey;
    }

    public String getPivotTable() {
        return pivotTable;
    }

    public String getForeignPivotKey() {
        return foreignPivotKey;
    }

    public String getRelatedPivotKey() {
        return relatedPivotKey;
    }

    public List<String> getPivotColumns() {
        return pivotColumns;
    }

    public boolean hasPivotTimestamps() {
        return pivotTimestamps;
    }

    public boolean isNoForeignKey() {
        return noForeignKey;
    }

    public boolean isEager() {
        return eager;
    }

    public boolean isToMany() {
        return type == Type.HAS_MANY || type == Type.BELONGS_TO_MANY || type == Type.HAS_MANY_THROUGH || type == Type.MORPH_MANY || type == Type.MORPH_TO_MANY || type == Type.MORPHED_BY_MANY;
    }

    public boolean usesPivotTable() {
        return type == Type.BELONGS_TO_MANY || type == Type.MORPH_TO_MANY || type == Type.MORPHED_BY_MANY;
    }

    public boolean isThrough() {
        return type == Type.HAS_ONE_THROUGH || type == Type.HAS_MANY_THROUGH;
    }

    public Table<?> getThroughTable() {
        return throughTable;
    }

    public String getFirstKey() {
        return firstKey;
    }

    public String getSecondKey() {
        return secondKey;
    }

    public String getSecondLocalKey() {
        return secondLocalKey;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMorphName() {
        return morphName;
    }

    public String getMorphTypeColumn() {
        return morphTypeColumn;
    }

    public String getMorphIdColumn() {
        return morphIdColumn;
    }

    public boolean isMorphic() {
        return type.isMorphic();
    }

    public boolean shouldTouch() {
        return shouldTouch;
    }

    public List<String> getTouchColumns() {
        return touchColumns;
    }

    public String getOrderColumn() {
        return orderColumn;
    }

    public String getAggregateColumn() {
        return aggregateColumn;
    }

    public String getAggregateFunction() {
        return aggregateFunction;
    }

    public boolean isOfMany() {
        return type.isOfMany();
    }

    public boolean hasDefault() {
        return withDefault;
    }

    public List<String> getDefaultAttributes() {
        return defaultAttributes;
    }

    // ==================== SQL Generation Helpers ====================

    /**
     * Generate the WHERE clause condition linking owner to related table.
     * Used for EXISTS subqueries in whereHas().
     *
     * @param ownerAlias alias/name of the owner table in outer query
     * @return SQL condition string
     */
    public String getExistsCondition(String ownerAlias) {
        return switch (type) {
            case HAS_ONE, HAS_MANY ->
                // related.fk = owner.localKey
                relatedTable.getName() + "." + foreignKey + " = " + ownerAlias + "." + localKey;
            case BELONGS_TO ->
                // owner.fk = related.relatedKey
                ownerAlias + "." + foreignKey + " = " + relatedTable.getName() + "." + relatedKey;
            case BELONGS_TO_MANY ->
                // Through pivot table
                pivotTable + "." + foreignPivotKey + " = " + ownerAlias + "." + localKey;
            case HAS_ONE_THROUGH, HAS_MANY_THROUGH ->
                // Through intermediate table: through.firstKey = owner.localKey
                throughTable.getName() + "." + firstKey + " = " + ownerAlias + "." + localKey;
            case MORPH_ONE, MORPH_MANY ->
                // Polymorphic: related.morphId = owner.localKey AND related.morphType = 'OwnerType'
                relatedTable.getName() + "." + morphIdColumn + " = " + ownerAlias + "." + localKey
                + " AND " + relatedTable.getName() + "." + morphTypeColumn + " = '" + ownerTable.getEntityType().getSimpleName() + "'";
            case MORPH_TO ->
                // Inverse polymorphic: owner.morphId = related.relatedKey
                // Type check done at runtime
                ownerAlias + "." + morphIdColumn + " = " + relatedTable.getName() + "." + relatedKey;
            case MORPH_TO_MANY ->
                // Polymorphic many-to-many through pivot
                pivotTable + "." + morphIdColumn + " = " + ownerAlias + "." + localKey
                + " AND " + pivotTable + "." + morphTypeColumn + " = '" + ownerTable.getEntityType().getSimpleName() + "'";
            case MORPHED_BY_MANY ->
                // Inverse polymorphic M2M: pivot.foreignPivotKey = owner.localKey AND pivot.morphType = 'RelatedType'
                pivotTable + "." + foreignPivotKey + " = " + ownerAlias + "." + localKey
                + " AND " + pivotTable + "." + morphTypeColumn + " = '" + relatedTable.getEntityType().getSimpleName() + "'";
            case LATEST_OF_MANY, OLDEST_OF_MANY, OF_MANY ->
                // OfMany relations are special HasOne variants - same join logic as HAS_ONE
                relatedTable.getName() + "." + foreignKey + " = " + ownerAlias + "." + localKey;
        };
    }

    /**
     * Get the table name to use in EXISTS subquery FROM clause.
     */
    public String getExistsFromTable() {
        return relatedTable.getName();
    }

    /**
     * Get additional JOINs needed in EXISTS subquery.
     * For BelongsToMany: joins pivot table.
     * For Through: joins intermediate table.
     * Returns null for other relationship types.
     */
    public String getPivotJoinForExists() {
        return switch (type) {
            case BELONGS_TO_MANY, MORPH_TO_MANY ->
                // JOIN pivot ON pivot.related_key = related.id
                "JOIN " + pivotTable + " ON " + pivotTable + "." + relatedPivotKey
                        + " = " + relatedTable.getName() + "." + relatedKey;
            case MORPHED_BY_MANY ->
                // Inverse: JOIN pivot ON pivot.morphIdColumn = related.relatedKey AND pivot.morphType = 'RelatedType'
                "JOIN " + pivotTable + " ON " + pivotTable + "." + morphIdColumn
                        + " = " + relatedTable.getName() + "." + relatedKey
                        + " AND " + pivotTable + "." + morphTypeColumn
                        + " = '" + relatedTable.getEntityType().getSimpleName() + "'";
            case HAS_ONE_THROUGH, HAS_MANY_THROUGH ->
                // JOIN through ON related.secondKey = through.secondLocalKey
                // Example: JOIN users ON posts.user_id = users.id
                "JOIN " + throughTable.getName() + " ON " + relatedTable.getName() + "." + secondKey
                        + " = " + throughTable.getName() + "." + secondLocalKey;
            default -> null;
        };
    }

    /**
     * Get the count alias for withCount().
     * Converts relation field name to snake_case + "_count".
     */
    public String getCountAlias(String fieldName) {
        return toSnakeCase(fieldName) + "_count";
    }

    /**
     * Get the ORDER BY clause for ofMany relationships.
     * Returns null for non-ofMany relations.
     */
    public String getOfManyOrderBy() {
        if (!type.isOfMany()) {
            return null;
        }

        return switch (type) {
            case LATEST_OF_MANY -> relatedTable.getName() + "." + orderColumn + " DESC";
            case OLDEST_OF_MANY -> relatedTable.getName() + "." + orderColumn + " ASC";
            case OF_MANY -> {
                // For OF_MANY, we order by the aggregate column
                // e.g., ORDER BY amount DESC for MAX(amount)
                String direction = aggregateFunction.equalsIgnoreCase("MAX") ? "DESC" : "ASC";
                yield relatedTable.getName() + "." + aggregateColumn + " " + direction;
            }
            default -> null;
        };
    }

    /**
     * Whether this relationship requires LIMIT 1 in EXISTS queries.
     */
    public boolean requiresLimitOne() {
        return type.isOfMany();
    }

    private static String toSnakeCase(String camelCase) {
        if (Objects.isNull(camelCase) || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    // ==================== Builder ====================

    private static class Builder<T, R> {
        private final Type type;
        private final Table<T> ownerTable;
        private final Table<R> relatedTable;
        private String foreignKey = "";
        private String localKey = "id";
        private String relatedKey = "id";
        private String pivotTable = "";
        private String foreignPivotKey = "";
        private String relatedPivotKey = "";
        private List<String> pivotColumns = List.of();
        private boolean pivotTimestamps = false;
        private boolean noForeignKey = false;
        private boolean eager = false;
        private String fieldName = null;

        // Through relationship fields
        private Table<?> throughTable = null;
        private String firstKey = "";
        private String secondKey = "";
        private String secondLocalKey = "id";

        // Morph relationship fields
        private String morphName = null;
        private String morphTypeColumn = null;
        private String morphIdColumn = null;

        // Touch parent timestamps
        private boolean shouldTouch = false;
        private List<String> touchColumns = List.of("updated_at");

        // OfMany relationship fields
        private String orderColumn = null;
        private String aggregateColumn = null;
        private String aggregateFunction = null;

        // Default model support
        private boolean withDefault = false;
        private List<String> defaultAttributes = List.of();

        Builder(Type type, Table<T> ownerTable, Table<R> relatedTable) {
            this.type = type;
            this.ownerTable = ownerTable;
            this.relatedTable = relatedTable;
        }

        Builder<T, R> foreignKey(String foreignKey) {
            this.foreignKey = foreignKey;
            return this;
        }

        Builder<T, R> localKey(String localKey) {
            this.localKey = localKey;
            return this;
        }

        Builder<T, R> relatedKey(String relatedKey) {
            this.relatedKey = relatedKey;
            return this;
        }

        Builder<T, R> pivotTable(String pivotTable) {
            this.pivotTable = pivotTable;
            return this;
        }

        Builder<T, R> foreignPivotKey(String foreignPivotKey) {
            this.foreignPivotKey = foreignPivotKey;
            return this;
        }

        Builder<T, R> relatedPivotKey(String relatedPivotKey) {
            this.relatedPivotKey = relatedPivotKey;
            return this;
        }

        Builder<T, R> pivotColumns(List<String> pivotColumns) {
            this.pivotColumns = Objects.nonNull(pivotColumns) ? pivotColumns : List.of();
            return this;
        }

        Builder<T, R> pivotTimestamps(boolean pivotTimestamps) {
            this.pivotTimestamps = pivotTimestamps;
            return this;
        }

        Builder<T, R> noForeignKey(boolean noForeignKey) {
            this.noForeignKey = noForeignKey;
            return this;
        }

        Builder<T, R> eager(boolean eager) {
            this.eager = eager;
            return this;
        }

        Builder<T, R> throughTable(Table<?> throughTable) {
            this.throughTable = throughTable;
            return this;
        }

        Builder<T, R> firstKey(String firstKey) {
            this.firstKey = firstKey;
            return this;
        }

        Builder<T, R> secondKey(String secondKey) {
            this.secondKey = secondKey;
            return this;
        }

        Builder<T, R> secondLocalKey(String secondLocalKey) {
            this.secondLocalKey = secondLocalKey;
            return this;
        }

        Builder<T, R> fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        Builder<T, R> morphName(String morphName) {
            this.morphName = morphName;
            return this;
        }

        Builder<T, R> morphTypeColumn(String morphTypeColumn) {
            this.morphTypeColumn = morphTypeColumn;
            return this;
        }

        Builder<T, R> morphIdColumn(String morphIdColumn) {
            this.morphIdColumn = morphIdColumn;
            return this;
        }

        Builder<T, R> shouldTouch(boolean shouldTouch) {
            this.shouldTouch = shouldTouch;
            return this;
        }

        Builder<T, R> touchColumns(List<String> touchColumns) {
            this.touchColumns = Objects.nonNull(touchColumns) ? touchColumns : List.of("updated_at");
            return this;
        }

        Builder<T, R> orderColumn(String orderColumn) {
            this.orderColumn = orderColumn;
            return this;
        }

        Builder<T, R> aggregateColumn(String aggregateColumn) {
            this.aggregateColumn = aggregateColumn;
            return this;
        }

        Builder<T, R> aggregateFunction(String aggregateFunction) {
            this.aggregateFunction = aggregateFunction;
            return this;
        }

        Builder<T, R> withDefault(boolean withDefault) {
            this.withDefault = withDefault;
            return this;
        }

        Builder<T, R> defaultAttributes(List<String> defaultAttributes) {
            this.defaultAttributes = Objects.nonNull(defaultAttributes) ? defaultAttributes : List.of();
            return this;
        }

        Relation<T, R> build() {
            return new Relation<>(this);
        }
    }
}
