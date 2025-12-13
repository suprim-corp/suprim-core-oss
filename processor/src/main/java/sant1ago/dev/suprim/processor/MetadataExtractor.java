package sant1ago.dev.suprim.processor;

import sant1ago.dev.suprim.annotation.entity.*;
import sant1ago.dev.suprim.annotation.type.*;
import sant1ago.dev.suprim.annotation.relationship.*;
import sant1ago.dev.suprim.casey.Casey;
import sant1ago.dev.suprim.processor.model.ColumnMetadata;
import sant1ago.dev.suprim.processor.model.EntityMetadata;
import sant1ago.dev.suprim.processor.model.RelationshipMetadata;
import sant1ago.dev.suprim.processor.model.RelationType;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Extracts metadata from @Entity annotated classes.
 * Supports inheritance via @MappedSuperclass.
 */
public class MetadataExtractor {

    private final Elements elementUtils;
    private Types typeUtils;

    private static final Set<String> COMPARABLE_TYPES = Set.of(
            "java.lang.Long", "long",
            "java.lang.Integer", "int",
            "java.lang.Short", "short",
            "java.lang.Byte", "byte",
            "java.lang.Double", "double",
            "java.lang.Float", "float",
            "java.math.BigDecimal", "java.math.BigInteger",
            "java.time.LocalDateTime", "java.time.LocalDate", "java.time.LocalTime",
            "java.time.OffsetDateTime", "java.time.ZonedDateTime", "java.time.Instant",
            "java.sql.Timestamp", "java.sql.Date", "java.sql.Time"
    );

    public MetadataExtractor(Elements elementUtils) {
        this.elementUtils = Objects.requireNonNull(elementUtils, "elementUtils cannot be null");
    }

    public MetadataExtractor(Elements elementUtils, Types typeUtils) {
        this.elementUtils = Objects.requireNonNull(elementUtils, "elementUtils cannot be null");
        this.typeUtils = typeUtils; // typeUtils can be null
    }

    /**
     * Extract metadata from an @Entity annotated class.
     * Walks up the class hierarchy to include fields from @MappedSuperclass parents.
     */
    public EntityMetadata extract(TypeElement entityElement) {
        Objects.requireNonNull(entityElement, "entityElement cannot be null");
        // Extract from @Entity annotation (table and schema are now on @Entity)
        Entity entityAnnotation = entityElement.getAnnotation(Entity.class);
        String tableName = nonNull(entityAnnotation) && !entityAnnotation.table().isEmpty()
                ? entityAnnotation.table()
                : Casey.toSnakeCase(entityElement.getSimpleName().toString());
        String schema = nonNull(entityAnnotation) ? entityAnnotation.schema() : "";

        // Extract default eager loads from @Entity(with = {...})
        List<String> defaultEagerLoads = nonNull(entityAnnotation) && entityAnnotation.with().length > 0
                ? Arrays.asList(entityAnnotation.with())
                : List.of();

        // Collect all fields including inherited ones (parent fields first)
        List<ColumnMetadata> columns = new ArrayList<>();
        List<RelationshipMetadata> relationships = new ArrayList<>();
        collectFieldsFromHierarchy(entityElement, columns, relationships, new LinkedHashSet<>(), tableName);

        String packageName = elementUtils.getPackageOf(entityElement).getQualifiedName().toString();

        return new EntityMetadata(
                entityElement,
                entityElement.getSimpleName().toString(),
                packageName,
                tableName,
                schema,
                columns,
                relationships,
                defaultEagerLoads
        );
    }

    /**
     * Recursively collect fields from class hierarchy.
     * Parent class fields are added first (correct column order).
     */
    private void collectFieldsFromHierarchy(
            TypeElement classElement,
            List<ColumnMetadata> columns,
            List<RelationshipMetadata> relationships,
            Set<String> seenFields,
            String ownerTableName
    ) {
        // First, process parent class (if it's a MappedSuperclass or has @Column fields)
        TypeMirror superclass = classElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE && superclass.getKind() == TypeKind.DECLARED) {
            Element superElement = ((DeclaredType) superclass).asElement();
            if (superElement instanceof TypeElement superTypeElement) {
                // Check if parent is @MappedSuperclass or has Suprim annotations
                if (nonNull(superTypeElement.getAnnotation(MappedSuperclass.class)) ||
                        hasSuprimAnnotatedFields(superTypeElement)) {
                    collectFieldsFromHierarchy(superTypeElement, columns, relationships, seenFields, ownerTableName);
                }
            }
        }

        // Then, process current class fields
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();

                // Skip if already seen (child class overrides parent)
                if (seenFields.contains(fieldName)) {
                    continue;
                }

                // Skip static and transient fields
                Set<Modifier> modifiers = field.getModifiers();
                if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.TRANSIENT)) {
                    continue;
                }

                // Check for relationship annotations first
                RelationshipMetadata relationship = extractRelationship(field, ownerTableName);
                if (nonNull(relationship)) {
                    relationships.add(relationship);
                    seenFields.add(fieldName);
                    continue;
                }

                // Include fields with @Column, @Id, @JsonbColumn, or all non-static fields
                if (nonNull(field.getAnnotation(Column.class)) ||
                        nonNull(field.getAnnotation(Id.class)) ||
                        nonNull(field.getAnnotation(JsonbColumn.class)) ||
                        !modifiers.contains(Modifier.STATIC)) {
                    columns.add(extractColumn(field));
                    seenFields.add(fieldName);
                }
            }
        }
    }

    /**
     * Check if a class has any Suprim-annotated fields.
     */
    private boolean hasSuprimAnnotatedFields(TypeElement classElement) {
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                if (nonNull(field.getAnnotation(Column.class)) ||
                        nonNull(field.getAnnotation(Id.class)) ||
                        nonNull(field.getAnnotation(JsonbColumn.class))) {
                    return true;
                }
            }
        }
        return false;
    }

    private ColumnMetadata extractColumn(VariableElement field) {
        Column columnAnnot = field.getAnnotation(Column.class);
        String columnName = nonNull(columnAnnot) && !columnAnnot.name().isEmpty()
                ? columnAnnot.name()
                : Casey.toSnakeCase(field.getSimpleName().toString());

        String javaType = field.asType().toString();
        String sqlType = resolveSqlType(columnAnnot, javaType);

        Id idAnnot = field.getAnnotation(Id.class);
        boolean isPrimaryKey = nonNull(idAnnot);
        boolean isJsonb = nonNull(field.getAnnotation(JsonbColumn.class));
        boolean isComparable = COMPARABLE_TYPES.contains(javaType);
        boolean isString = "java.lang.String".equals(javaType);

        // Extract generation strategy from @Id
        GenerationType generationType = GenerationType.NONE;
        String sequenceName = "";
        String generatorClass = null;
        if (nonNull(idAnnot)) {
            generationType = idAnnot.strategy();
            sequenceName = idAnnot.sequence();
            generatorClass = extractGeneratorClass(field);
        }

        return new ColumnMetadata(
                field.getSimpleName().toString(),
                columnName,
                javaType,
                sqlType,
                isPrimaryKey,
                isComparable,
                isString,
                isJsonb,
                generationType,
                sequenceName,
                generatorClass
        );
    }

    /**
     * Extract a generator class name from @Id annotation.
     * Uses AnnotationMirror because Class values can't be accessed directly at compile time.
     */
    private String extractGeneratorClass(VariableElement field) {
        for (var annotationMirror : field.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(Id.class.getName())) {
                for (var entry : annotationMirror.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("generator")) {
                        return entry.getValue().getValue().toString();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolve the SQL type from annotation or infer from the Java type.
     */
    private String resolveSqlType(Column columnAnnot, String javaType) {
        if (nonNull(columnAnnot)) {
            // Custom definition takes highest priority
            if (!columnAnnot.definition().isEmpty()) {
                return columnAnnot.definition();
            }

            SqlType sqlType = columnAnnot.type();
            if (nonNull(sqlType) && !sqlType.isAuto()) {
                return buildSqlTypeString(sqlType, columnAnnot);
            }
        }
        // Fall back to inference
        return inferSqlType(javaType);
    }

    /**
     * Build SQL type string with length/precision/scale if applicable.
     */
    private String buildSqlTypeString(SqlType sqlType, Column columnAnnot) {
        String base = sqlType.getSql();

        // Handle length for VARCHAR, CHAR, BIT, VARBIT
        if (sqlType.supportsLength()) {
            int length = columnAnnot.length();
            if (length > 0) {
                return base + "(" + length + ")";
            } else if (length == 0 && sqlType == SqlType.VARCHAR) {
                // Default VARCHAR to 255 if no length specified
                return base + "(255)";
            } else if (length == -1) {
                // Explicit unbounded
                return base;
            }
        }

        // Handle precision/scale for NUMERIC, DECIMAL
        if (sqlType.supportsPrecision()) {
            int precision = columnAnnot.precision();
            int scale = columnAnnot.scale();
            if (precision > 0) {
                if (scale > 0) {
                    return base + "(" + precision + ", " + scale + ")";
                }
                return base + "(" + precision + ")";
            }
        }

        return base;
    }

    private String inferSqlType(String javaType) {
        return switch (javaType) {
            case "java.lang.String" -> "VARCHAR(255)";
            case "java.lang.Long", "long" -> "BIGINT";
            case "java.lang.Integer", "int" -> "INTEGER";
            case "java.lang.Short", "short" -> "SMALLINT";
            case "java.lang.Boolean", "boolean" -> "BOOLEAN";
            case "java.lang.Double", "double" -> "DOUBLE PRECISION";
            case "java.lang.Float", "float" -> "REAL";
            case "java.math.BigDecimal" -> "NUMERIC";
            case "java.time.LocalDateTime", "java.sql.Timestamp" -> "TIMESTAMP";
            case "java.time.LocalDate", "java.sql.Date" -> "DATE";
            case "java.time.LocalTime", "java.sql.Time" -> "TIME";
            case "java.time.OffsetDateTime", "java.time.ZonedDateTime" -> "TIMESTAMPTZ";
            case "java.util.UUID" -> "UUID";
            default -> "TEXT";
        };
    }

    // ==================== RELATIONSHIP EXTRACTION ====================

    /**
     * Extract relationship metadata from a field if it has a relationship annotation.
     */
    private RelationshipMetadata extractRelationship(VariableElement field, String ownerTableName) {
        String fieldName = field.getSimpleName().toString();

        // @HasOne
        HasOne hasOne = field.getAnnotation(HasOne.class);
        if (nonNull(hasOne)) {
            EntityInfo related = extractEntityClass(field, HasOne.class.getName(), "entity");
            String fk = hasOne.foreignKey().isEmpty()
                    ? Casey.toSnakeCase(stripTableSuffix(ownerTableName)) + "_id"
                    : hasOne.foreignKey();

            // Check for ofMany annotations
            sant1ago.dev.suprim.annotation.relationship.LatestOfMany latestOfMany = field.getAnnotation(sant1ago.dev.suprim.annotation.relationship.LatestOfMany.class);
            sant1ago.dev.suprim.annotation.relationship.OldestOfMany oldestOfMany = field.getAnnotation(sant1ago.dev.suprim.annotation.relationship.OldestOfMany.class);
            sant1ago.dev.suprim.annotation.relationship.OfMany ofMany = field.getAnnotation(sant1ago.dev.suprim.annotation.relationship.OfMany.class);

            RelationType type = RelationType.HAS_ONE;
            String orderColumn = null;
            String aggregateColumn = null;
            String aggregateFunction = null;

            if (Objects.nonNull(latestOfMany)) {
                type = RelationType.LATEST_OF_MANY;
                orderColumn = latestOfMany.column();
            } else if (Objects.nonNull(oldestOfMany)) {
                type = RelationType.OLDEST_OF_MANY;
                orderColumn = oldestOfMany.column();
            } else if (Objects.nonNull(ofMany)) {
                type = RelationType.OF_MANY;
                aggregateColumn = ofMany.column();
                aggregateFunction = ofMany.aggregate();
            }

            return new RelationshipMetadata(
                    fieldName,
                    type,
                    related.qualifiedName(),
                    related.simpleName(),
                    fk,
                    "",
                    hasOne.localKey(),
                    "id",
                    "",
                    List.of(),
                    false,
                    hasOne.noForeignKey(),
                    Arrays.asList(hasOne.cascade()),
                    hasOne.fetch(),
                    null, null,  // through fields
                    null, null, null,  // morph fields
                    false, List.of("updated_at"),  // touch fields
                    orderColumn, aggregateColumn, aggregateFunction,  // ofMany fields
                    hasOne.withDefault(),
                    Arrays.asList(hasOne.defaultAttributes())
            );
        }

        // @HasMany
        HasMany hasMany = field.getAnnotation(HasMany.class);
        if (nonNull(hasMany)) {
            EntityInfo related = extractEntityClass(field, HasMany.class.getName(), "entity");
            String fk = hasMany.foreignKey().isEmpty()
                    ? Casey.toSnakeCase(stripTableSuffix(ownerTableName)) + "_id"
                    : hasMany.foreignKey();
            return new RelationshipMetadata(
                    fieldName,
                    RelationType.HAS_MANY,
                    related.qualifiedName(),
                    related.simpleName(),
                    fk,
                    "",
                    hasMany.localKey(),
                    "id",
                    "",
                    List.of(),
                    false,
                    hasMany.noForeignKey(),
                    Arrays.asList(hasMany.cascade()),
                    hasMany.fetch(),
                    null, null,  // through fields
                    null, null, null  // morph fields
            );
        }

        // @BelongsTo
        BelongsTo belongsTo = field.getAnnotation(BelongsTo.class);
        if (nonNull(belongsTo)) {
            EntityInfo related = extractEntityClass(field, BelongsTo.class.getName(), "entity");
            String fk = belongsTo.foreignKey().isEmpty()
                    ? Casey.toSnakeCase(related.simpleName()) + "_id"
                    : belongsTo.foreignKey();
            return new RelationshipMetadata(
                    fieldName,
                    RelationType.BELONGS_TO,
                    related.qualifiedName(),
                    related.simpleName(),
                    fk,
                    "",
                    "id",
                    belongsTo.ownerKey(),
                    "",
                    List.of(),
                    false,
                    belongsTo.noForeignKey(),
                    List.of(), // BelongsTo doesn't cascade
                    belongsTo.fetch(),
                    null, null,  // through fields
                    null, null, null,  // morph fields
                    belongsTo.touch(),  // touch parent timestamps
                    Arrays.asList(belongsTo.touches()),  // touch columns
                    null, null, null,  // ofMany fields
                    belongsTo.withDefault(),
                    Arrays.asList(belongsTo.defaultAttributes())
            );
        }

        // @BelongsToMany
        BelongsToMany belongsToMany = field.getAnnotation(BelongsToMany.class);
        if (nonNull(belongsToMany)) {
            EntityInfo related = extractEntityClass(field, BelongsToMany.class.getName(), "entity");
            String ownerSnake = Casey.toSnakeCase(stripTableSuffix(ownerTableName));
            String relatedSnake = Casey.toSnakeCase(related.simpleName());

            // Pivot table: alphabetically sorted entity names
            String pivotTable = belongsToMany.table().isEmpty()
                    ? derivePivotTableName(ownerSnake, relatedSnake)
                    : belongsToMany.table();

            String foreignPivotKey = belongsToMany.foreignPivotKey().isEmpty()
                    ? ownerSnake + "_id"
                    : belongsToMany.foreignPivotKey();

            String relatedPivotKey = belongsToMany.relatedPivotKey().isEmpty()
                    ? relatedSnake + "_id"
                    : belongsToMany.relatedPivotKey();

            return new RelationshipMetadata(
                    fieldName,
                    RelationType.BELONGS_TO_MANY,
                    related.qualifiedName(),
                    related.simpleName(),
                    foreignPivotKey,
                    relatedPivotKey,
                    belongsToMany.localKey(),
                    belongsToMany.relatedKey(),
                    pivotTable,
                    Arrays.asList(belongsToMany.withPivot()),
                    belongsToMany.withTimestamps(),
                    false,
                    Arrays.asList(belongsToMany.cascade()),
                    belongsToMany.fetch(),
                    null, null,  // through fields
                    null, null, null  // morph fields
            );
        }

        // @HasOneThrough
        HasOneThrough hasOneThrough = field.getAnnotation(HasOneThrough.class);
        if (nonNull(hasOneThrough)) {
            EntityInfo related = extractEntityClass(field, HasOneThrough.class.getName(), "entity");
            EntityInfo through = extractEntityClass(field, HasOneThrough.class.getName(), "through");
            String ownerSnake = Casey.toSnakeCase(stripTableSuffix(ownerTableName));
            String throughSnake = Casey.toSnakeCase(through.simpleName());

            // firstKey: FK on intermediate table pointing to this entity
            String firstKey = hasOneThrough.firstKey().isEmpty()
                    ? ownerSnake + "_id"
                    : hasOneThrough.firstKey();

            // secondKey: FK on final table pointing to intermediate entity
            String secondKey = hasOneThrough.secondKey().isEmpty()
                    ? throughSnake + "_id"
                    : hasOneThrough.secondKey();

            return new RelationshipMetadata(
                    fieldName,
                    RelationType.HAS_ONE_THROUGH,
                    related.qualifiedName(),
                    related.simpleName(),
                    firstKey,          // foreignKey = firstKey
                    secondKey,         // relatedPivotKey = secondKey
                    hasOneThrough.localKey(),
                    hasOneThrough.secondLocalKey(),  // relatedKey = secondLocalKey
                    "",                // no pivot table
                    List.of(),
                    false,
                    false,
                    List.of(),
                    hasOneThrough.fetch(),
                    through.qualifiedName(),
                    through.simpleName(),
                    null, null, null  // morph fields
            );
        }

        // @HasManyThrough
        HasManyThrough hasManyThrough = field.getAnnotation(HasManyThrough.class);
        if (nonNull(hasManyThrough)) {
            EntityInfo related = extractEntityClass(field, HasManyThrough.class.getName(), "entity");
            EntityInfo through = extractEntityClass(field, HasManyThrough.class.getName(), "through");
            String ownerSnake = Casey.toSnakeCase(stripTableSuffix(ownerTableName));
            String throughSnake = Casey.toSnakeCase(through.simpleName());

            // firstKey: FK on intermediate table pointing to this entity
            String firstKey = hasManyThrough.firstKey().isEmpty()
                    ? ownerSnake + "_id"
                    : hasManyThrough.firstKey();

            // secondKey: FK on final table pointing to intermediate entity
            String secondKey = hasManyThrough.secondKey().isEmpty()
                    ? throughSnake + "_id"
                    : hasManyThrough.secondKey();

            return new RelationshipMetadata(
                    fieldName,
                    RelationType.HAS_MANY_THROUGH,
                    related.qualifiedName(),
                    related.simpleName(),
                    firstKey,          // foreignKey = firstKey
                    secondKey,         // relatedPivotKey = secondKey
                    hasManyThrough.localKey(),
                    hasManyThrough.secondLocalKey(),  // relatedKey = secondLocalKey
                    "",                // no pivot table
                    List.of(),
                    false,
                    false,
                    List.of(),
                    hasManyThrough.fetch(),
                    through.qualifiedName(),
                    through.simpleName(),
                    null,  // morphName
                    null,  // morphTypeColumn
                    null   // morphIdColumn
            );
        }

        // @MorphOne
        MorphOne morphOne = field.getAnnotation(MorphOne.class);
        if (nonNull(morphOne)) {
            return extractMorphOne(field, morphOne, ownerTableName);
        }

        // @MorphMany
        MorphMany morphMany = field.getAnnotation(MorphMany.class);
        if (nonNull(morphMany)) {
            return extractMorphMany(field, morphMany, ownerTableName);
        }

        // @MorphTo
        MorphTo morphTo = field.getAnnotation(MorphTo.class);
        if (nonNull(morphTo)) {
            return extractMorphTo(field, morphTo);
        }

        // @MorphToMany
        MorphToMany morphToMany = field.getAnnotation(MorphToMany.class);
        if (nonNull(morphToMany)) {
            return extractMorphToMany(field, morphToMany, ownerTableName);
        }

        // @MorphedByMany
        MorphedByMany morphedByMany = field.getAnnotation(MorphedByMany.class);
        if (nonNull(morphedByMany)) {
            return extractMorphedByMany(field, morphedByMany, ownerTableName);
        }

        return null;
    }

    /**
     * Extract MorphOne relationship metadata.
     */
    private RelationshipMetadata extractMorphOne(VariableElement field, MorphOne annotation, String ownerTableName) {
        EntityInfo related = extractEntityClass(field, MorphOne.class.getName(), "entity");
        String morphName = annotation.name();
        String morphTypeColumn = annotation.type().isEmpty() ? morphName + "_type" : annotation.type();
        String morphIdColumn = annotation.id().isEmpty() ? morphName + "_id" : annotation.id();

        // Type value is simple class name of owner entity
        String ownerEntityName = stripTableSuffix(ownerTableName);
        String typeValue = capitalize(ownerEntityName);

        return new RelationshipMetadata(
                field.getSimpleName().toString(),
                RelationType.MORPH_ONE,
                related.qualifiedName(),
                related.simpleName(),
                morphIdColumn,         // foreignKey
                "",                    // relatedPivotKey (unused)
                annotation.localKey(),
                "id",                  // relatedKey
                "",                    // no pivot table
                List.of(),
                false,
                false,
                Arrays.asList(annotation.cascade()),
                annotation.fetch(),
                null,                  // throughEntityClass
                null,                  // throughEntitySimpleName
                morphName,
                morphTypeColumn,
                morphIdColumn
        );
    }

    /**
     * Extract MorphMany relationship metadata.
     */
    private RelationshipMetadata extractMorphMany(VariableElement field, MorphMany annotation, String ownerTableName) {
        EntityInfo related = extractEntityClass(field, MorphMany.class.getName(), "entity");
        String morphName = annotation.name();
        String morphTypeColumn = annotation.type().isEmpty() ? morphName + "_type" : annotation.type();
        String morphIdColumn = annotation.id().isEmpty() ? morphName + "_id" : annotation.id();

        // Type value is simple class name of owner entity
        String ownerEntityName = stripTableSuffix(ownerTableName);
        String typeValue = capitalize(ownerEntityName);

        return new RelationshipMetadata(
                field.getSimpleName().toString(),
                RelationType.MORPH_MANY,
                related.qualifiedName(),
                related.simpleName(),
                morphIdColumn,         // foreignKey
                "",                    // relatedPivotKey (unused)
                annotation.localKey(),
                "id",                  // relatedKey
                "",                    // no pivot table
                List.of(),
                false,
                false,
                Arrays.asList(annotation.cascade()),
                annotation.fetch(),
                null,                  // throughEntityClass
                null,                  // throughEntitySimpleName
                morphName,
                morphTypeColumn,
                morphIdColumn
        );
    }

    /**
     * Extract MorphTo relationship metadata.
     */
    private RelationshipMetadata extractMorphTo(VariableElement field, MorphTo annotation) {
        String morphName = annotation.name();
        String morphTypeColumn = annotation.type().isEmpty() ? morphName + "_type" : annotation.type();
        String morphIdColumn = annotation.id().isEmpty() ? morphName + "_id" : annotation.id();

        return new RelationshipMetadata(
                field.getSimpleName().toString(),
                RelationType.MORPH_TO,
                "java.lang.Object",    // Related type is resolved at runtime
                "Object",
                morphIdColumn,         // foreignKey
                "",                    // relatedPivotKey (unused)
                "id",                  // localKey (unused for MorphTo)
                "id",                  // relatedKey
                "",                    // no pivot table
                List.of(),
                false,
                false,
                List.of(),
                annotation.fetch(),
                null,                  // throughEntityClass
                null,                  // throughEntitySimpleName
                morphName,
                morphTypeColumn,
                morphIdColumn
        );
    }

    /**
     * Extract MorphToMany relationship metadata.
     */
    private RelationshipMetadata extractMorphToMany(VariableElement field, MorphToMany annotation, String ownerTableName) {
        EntityInfo related = extractEntityClass(field, MorphToMany.class.getName(), "entity");
        String morphName = annotation.name();
        String morphTypeColumn = annotation.type().isEmpty() ? morphName + "_type" : annotation.type();

        // Foreign key can be specified via id() or foreignPivotKey()
        String morphIdColumn = !annotation.foreignPivotKey().isEmpty()
                ? annotation.foreignPivotKey()
                : (!annotation.id().isEmpty() ? annotation.id() : morphName + "_id");

        String relatedSnake = Casey.toSnakeCase(related.simpleName());
        String relatedPivotKey = annotation.relatedPivotKey().isEmpty()
                ? relatedSnake + "_id"
                : annotation.relatedPivotKey();

        // Pivot table defaults to pluralized morph name
        String pivotTable = annotation.table().isEmpty()
                ? morphName + "s"
                : annotation.table();

        // Type value is simple class name of owner entity
        String ownerEntityName = stripTableSuffix(ownerTableName);
        String typeValue = capitalize(ownerEntityName);

        return new RelationshipMetadata(
                field.getSimpleName().toString(),
                RelationType.MORPH_TO_MANY,
                related.qualifiedName(),
                related.simpleName(),
                morphIdColumn,         // foreignKey (morphable_id in pivot)
                relatedPivotKey,       // relatedPivotKey (related entity FK in pivot)
                annotation.localKey(),
                annotation.relatedKey(),
                pivotTable,
                Arrays.asList(annotation.withPivot()),
                annotation.withTimestamps(),
                false,
                Arrays.asList(annotation.cascade()),
                annotation.fetch(),
                null,                  // throughEntityClass
                null,                  // throughEntitySimpleName
                morphName,
                morphTypeColumn,
                morphIdColumn
        );
    }

    /**
     * Extract MorphedByMany relationship metadata (inverse of MorphToMany).
     */
    private RelationshipMetadata extractMorphedByMany(VariableElement field, MorphedByMany annotation, String ownerTableName) {
        EntityInfo related = extractEntityClass(field, MorphedByMany.class.getName(), "entity");
        String morphName = annotation.name();
        String morphTypeColumn = annotation.morphTypeColumn().isEmpty() ? morphName + "_type" : annotation.morphTypeColumn();
        String morphIdColumn = annotation.relatedPivotKey().isEmpty() ? morphName + "_id" : annotation.relatedPivotKey();

        String ownerSnake = Casey.toSnakeCase(stripTableSuffix(ownerTableName));
        String foreignPivotKey = annotation.foreignPivotKey().isEmpty()
                ? ownerSnake + "_id"
                : annotation.foreignPivotKey();

        // Pivot table defaults to pluralized morph name
        String pivotTable = annotation.table().isEmpty()
                ? morphName + "s"
                : annotation.table();

        // Type value is simple class name of related entity (Post, Video, etc)
        // This is stored in the morphTypeColumn to filter by entity type

        return new RelationshipMetadata(
                field.getSimpleName().toString(),
                RelationType.MORPHED_BY_MANY,
                related.qualifiedName(),
                related.simpleName(),
                foreignPivotKey,       // foreignKey (tag_id in pivot)
                morphIdColumn,         // relatedPivotKey (taggable_id in pivot)
                annotation.localKey(),
                annotation.relatedKey(),
                pivotTable,
                Arrays.asList(annotation.withPivot()),
                annotation.withTimestamps(),
                false,                 // noForeignKey
                Arrays.asList(annotation.cascade()),
                annotation.fetch(),
                null,                  // throughEntityClass
                null,                  // throughEntitySimpleName
                morphName,             // morphName
                morphTypeColumn,       // morphTypeColumn
                morphIdColumn          // morphIdColumn
        );
    }

    /**
     * Capitalize first letter of a string.
     */
    private String capitalize(String str) {
        if (Objects.isNull(str) || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Extract the entity class from a relationship annotation using AnnotationMirror.
     * Direct Class access fails at compile time, so we use reflection on the mirror.
     */
    private EntityInfo extractEntityClass(VariableElement field, String annotationName, String attributeName) {
        for (var annotationMirror : field.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(annotationName)) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                        annotationMirror.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals(attributeName)) {
                        String qualifiedName = entry.getValue().getValue().toString();
                        String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
                        return new EntityInfo(qualifiedName, simpleName);
                    }
                }
            }
        }
        // Fallback - should not happen if annotation is correctly defined
        return new EntityInfo("Unknown", "Unknown");
    }

    /**
     * Strip trailing 's' from table name to get singular entity name.
     * users -> user, posts -> post
     */
    private String stripTableSuffix(String tableName) {
        if (tableName.endsWith("s") && tableName.length() > 1) {
            return tableName.substring(0, tableName.length() - 1);
        }
        return tableName;
    }

    /**
     * Derive pivot table name from two entity names (alphabetically sorted).
     * user + role -> role_user (r < u)
     */
    private String derivePivotTableName(String entity1, String entity2) {
        return entity1.compareTo(entity2) < 0
                ? entity1 + "_" + entity2
                : entity2 + "_" + entity1;
    }

    /**
     * Helper record for extracted entity class info.
     */
    private record EntityInfo(String qualifiedName, String simpleName) {}
}
