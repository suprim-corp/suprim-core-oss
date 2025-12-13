package sant1ago.dev.suprim.processor.model;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;

/**
 * Metadata extracted from @Entity annotated class.
 */
public record EntityMetadata(
        TypeElement entityElement,
        String entityClassName,
        String packageName,
        String tableName,
        String schema,
        List<ColumnMetadata> columns,
        List<RelationshipMetadata> relationships,
        List<String> defaultEagerLoads
) {
    /**
     * Backwards-compatible constructor without relationships.
     */
    public EntityMetadata(
            TypeElement entityElement,
            String entityClassName,
            String packageName,
            String tableName,
            String schema,
            List<ColumnMetadata> columns
    ) {
        this(entityElement, entityClassName, packageName, tableName, schema, columns, List.of(), List.of());
    }

    /**
     * Backwards-compatible constructor without default eager loads.
     */
    public EntityMetadata(
            TypeElement entityElement,
            String entityClassName,
            String packageName,
            String tableName,
            String schema,
            List<ColumnMetadata> columns,
            List<RelationshipMetadata> relationships
    ) {
        this(entityElement, entityClassName, packageName, tableName, schema, columns, relationships, List.of());
    }

    /**
     * Get the metamodel class name (Entity + "_" suffix).
     */
    public String getMetamodelClassName() {
        return entityClassName + "_";
    }

    /**
     * Whether this entity has any relationships defined.
     */
    public boolean hasRelationships() {
        return Objects.nonNull(relationships) && !relationships.isEmpty();
    }

    /**
     * Get relationships that should be eagerly loaded.
     */
    public List<RelationshipMetadata> getEagerRelationships() {
        if (Objects.isNull(relationships)) return List.of();
        return relationships.stream()
                .filter(RelationshipMetadata::isEager)
                .toList();
    }
}
