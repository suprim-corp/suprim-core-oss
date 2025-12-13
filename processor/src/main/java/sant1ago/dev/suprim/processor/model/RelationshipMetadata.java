package sant1ago.dev.suprim.processor.model;

import sant1ago.dev.suprim.annotation.type.CascadeType;
import sant1ago.dev.suprim.annotation.type.FetchType;

import java.util.List;
import java.util.Objects;

/**
 * Metadata extracted from relationship annotations (@HasOne, @HasMany, @BelongsTo, @BelongsToMany).
 */
public record RelationshipMetadata(
        /**
         * Field name in the entity class.
         */
        String fieldName,

        /**
         * Type of relationship.
         */
        RelationType relationType,

        /**
         * Fully qualified class name of the related entity.
         */
        String relatedEntityClass,

        /**
         * Simple class name of the related entity.
         */
        String relatedEntitySimpleName,

        /**
         * Foreign key column name.
         * For HasOne/HasMany: column on related table.
         * For BelongsTo: column on this table.
         * For BelongsToMany: foreignPivotKey on pivot table.
         * For Through: firstKey on intermediate table.
         */
        String foreignKey,

        /**
         * For BelongsToMany only: related entity's FK column in pivot table.
         * For Through: secondKey on final table.
         */
        String relatedPivotKey,

        /**
         * Local key column (usually primary key).
         */
        String localKey,

        /**
         * For BelongsTo: owner's key column (usually "id").
         * For BelongsToMany: related entity's key column.
         * For Through: secondLocalKey on intermediate table.
         */
        String relatedKey,

        /**
         * Pivot table name (BelongsToMany only).
         */
        String pivotTable,

        /**
         * Additional pivot columns to include in queries.
         */
        List<String> pivotColumns,

        /**
         * Whether pivot table has timestamp columns.
         */
        boolean pivotTimestamps,

        /**
         * Whether this is a logical-only relationship (no FK constraint in DB).
         */
        boolean noForeignKey,

        /**
         * Cascade operations for this relationship.
         */
        List<CascadeType> cascadeTypes,

        /**
         * Fetch strategy (LAZY or EAGER).
         */
        FetchType fetchType,

        /**
         * For Through relationships: fully qualified class name of intermediate entity.
         */
        String throughEntityClass,

        /**
         * For Through relationships: simple class name of intermediate entity.
         */
        String throughEntitySimpleName,

        /**
         * For Morph relationships: the polymorphic name (e.g., "imageable", "commentable").
         */
        String morphName,

        /**
         * For Morph relationships: the type discriminator column name.
         */
        String morphTypeColumn,

        /**
         * For Morph relationships: the polymorphic ID column name.
         */
        String morphIdColumn,

        /**
         * For BelongsTo relationships: whether to touch parent timestamps on save/update.
         */
        boolean shouldTouch,

        /**
         * For BelongsTo relationships: which timestamp columns to update on parent.
         */
        List<String> touchColumns,

        /**
         * For OfMany relationships: the column to order by (latestOfMany/oldestOfMany).
         */
        String orderColumn,

        /**
         * For OfMany relationships: the column to apply aggregate function (ofMany).
         */
        String aggregateColumn,

        /**
         * For OfMany relationships: the aggregate function (MAX, MIN, etc.).
         */
        String aggregateFunction,

        /**
         * Whether to return a default model instance instead of null when relationship doesn't exist.
         */
        boolean withDefault,

        /**
         * Default attribute values to apply when creating a default model instance.
         */
        List<String> defaultAttributes
) {
    /**
     * Backwards-compatible constructor without through and morph fields.
     */
    public RelationshipMetadata(
            String fieldName,
            RelationType relationType,
            String relatedEntityClass,
            String relatedEntitySimpleName,
            String foreignKey,
            String relatedPivotKey,
            String localKey,
            String relatedKey,
            String pivotTable,
            List<String> pivotColumns,
            boolean pivotTimestamps,
            boolean noForeignKey,
            List<CascadeType> cascadeTypes,
            FetchType fetchType
    ) {
        this(fieldName, relationType, relatedEntityClass, relatedEntitySimpleName,
             foreignKey, relatedPivotKey, localKey, relatedKey, pivotTable,
             pivotColumns, pivotTimestamps, noForeignKey, cascadeTypes, fetchType,
             null, null, null, null, null, false, List.of("updated_at"),
             null, null, null, false, List.of());
    }

    /**
     * Constructor with through/morph fields but without touch fields.
     */
    public RelationshipMetadata(
            String fieldName,
            RelationType relationType,
            String relatedEntityClass,
            String relatedEntitySimpleName,
            String foreignKey,
            String relatedPivotKey,
            String localKey,
            String relatedKey,
            String pivotTable,
            List<String> pivotColumns,
            boolean pivotTimestamps,
            boolean noForeignKey,
            List<CascadeType> cascadeTypes,
            FetchType fetchType,
            String throughEntityClass,
            String throughEntitySimpleName,
            String morphName,
            String morphTypeColumn,
            String morphIdColumn
    ) {
        this(fieldName, relationType, relatedEntityClass, relatedEntitySimpleName,
             foreignKey, relatedPivotKey, localKey, relatedKey, pivotTable,
             pivotColumns, pivotTimestamps, noForeignKey, cascadeTypes, fetchType,
             throughEntityClass, throughEntitySimpleName, morphName, morphTypeColumn, morphIdColumn,
             false, List.of("updated_at"),
             null, null, null, false, List.of());
    }
    /**
     * Convert field name to UPPER_SNAKE_CASE for constant name.
     */
    public String getConstantName() {
        return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    /**
     * Whether this relationship should be eagerly loaded.
     */
    public boolean isEager() {
        return fetchType == FetchType.EAGER;
    }

    /**
     * Whether cascade operations are defined.
     */
    public boolean hasCascade() {
        return Objects.nonNull(cascadeTypes) && !cascadeTypes.isEmpty();
    }

    /**
     * Whether AUTO cascade is enabled (runtime DB detection).
     */
    public boolean hasAutoCascade() {
        return Objects.nonNull(cascadeTypes) && cascadeTypes.contains(CascadeType.AUTO);
    }

    /**
     * Get the related entity's metamodel class name.
     */
    public String getRelatedMetamodelClassName() {
        return relatedEntitySimpleName + "_";
    }

    /**
     * Whether this is a through relationship.
     */
    public boolean isThrough() {
        return relationType == RelationType.HAS_ONE_THROUGH || relationType == RelationType.HAS_MANY_THROUGH;
    }

    /**
     * Get the through entity's metamodel class name.
     */
    public String getThroughMetamodelClassName() {
        return Objects.nonNull(throughEntitySimpleName) ? throughEntitySimpleName + "_" : null;
    }

    /**
     * Whether this is a morphic relationship.
     */
    public boolean isMorphic() {
        return relationType.isMorphic();
    }

    /**
     * Whether this is an ofMany relationship.
     */
    public boolean isOfMany() {
        return relationType.isOfMany();
    }
}
