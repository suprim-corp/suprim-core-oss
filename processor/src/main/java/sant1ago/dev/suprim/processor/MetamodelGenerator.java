package sant1ago.dev.suprim.processor;

import com.squareup.javapoet.*;
import sant1ago.dev.suprim.annotation.type.FetchType;
import sant1ago.dev.suprim.processor.model.ColumnMetadata;
import sant1ago.dev.suprim.processor.model.EntityMetadata;
import sant1ago.dev.suprim.processor.model.RelationshipMetadata;
import sant1ago.dev.suprim.processor.model.RelationType;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Generates metamodel classes using JavaPoet.
 */
public class MetamodelGenerator {

    private final Filer filer;

    private static final ClassName TABLE_CLASS = ClassName.get("sant1ago.dev.suprim.core.type", "Table");
    private static final ClassName COLUMN_CLASS = ClassName.get("sant1ago.dev.suprim.core.type", "Column");
    private static final ClassName COMPARABLE_COLUMN_CLASS = ClassName.get("sant1ago.dev.suprim.core.type", "ComparableColumn");
    private static final ClassName STRING_COLUMN_CLASS = ClassName.get("sant1ago.dev.suprim.core.type", "StringColumn");
    private static final ClassName JSONB_COLUMN_CLASS = ClassName.get("sant1ago.dev.suprim.core.type", "JsonbColumn");
    private static final ClassName RELATION_CLASS = ClassName.get("sant1ago.dev.suprim.core.type", "Relation");
    private static final ClassName LIST_CLASS = ClassName.get("java.util", "List");

    public MetamodelGenerator(Filer filer) {
        this.filer = Objects.requireNonNull(filer, "filer cannot be null");
    }

    /**
     * Generate metamodel class for the given entity.
     */
    public void generate(EntityMetadata metadata) throws IOException {
        Objects.requireNonNull(metadata, "metadata cannot be null");
        TypeSpec.Builder metamodel = TypeSpec.classBuilder(metadata.getMetamodelClassName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", "sant1ago.dev.suprim.processor.SuprimProcessor")
                        .build())
                .addJavadoc("Generated metamodel for {@link $L}.\n", metadata.entityClassName())
                .addJavadoc("Provides type-safe column references for query building.\n");

        // Add TABLE constant
        metamodel.addField(generateTableField(metadata));

        // Add column fields
        for (ColumnMetadata col : metadata.columns()) {
            metamodel.addField(generateColumnField(metadata, col));
        }

        // Add relationship fields
        if (metadata.hasRelationships()) {
            for (RelationshipMetadata rel : metadata.relationships()) {
                metamodel.addField(generateRelationField(metadata, rel));
            }
        }

        // Private constructor to prevent instantiation
        metamodel.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        JavaFile javaFile = JavaFile.builder(metadata.packageName(), metamodel.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();

        javaFile.writeTo(filer);
    }

    private FieldSpec generateTableField(EntityMetadata metadata) {
        ClassName entityClass = ClassName.get(metadata.packageName(), metadata.entityClassName());

        return FieldSpec.builder(
                        ParameterizedTypeName.get(TABLE_CLASS, entityClass),
                        "TABLE",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL
                )
                .addJavadoc("Table reference for $L\n", metadata.entityClassName())
                .initializer("new $T($S, $S, $T.class)",
                        TABLE_CLASS,
                        metadata.tableName(),
                        metadata.schema(),
                        entityClass)
                .build();
    }

    private FieldSpec generateColumnField(EntityMetadata metadata, ColumnMetadata col) {
        ClassName entityClass = ClassName.get(metadata.packageName(), metadata.entityClassName());
        ClassName columnType = getColumnType(col);

        TypeName fieldType;
        CodeBlock initializer;

        if (col.isJsonb()) {
            // JsonbColumn<Entity>
            fieldType = ParameterizedTypeName.get(JSONB_COLUMN_CLASS, entityClass);
            initializer = CodeBlock.of("new $T(TABLE, $S, $S)",
                    JSONB_COLUMN_CLASS, col.columnName(), col.sqlType());
        } else if (col.isString()) {
            // StringColumn<Entity>
            fieldType = ParameterizedTypeName.get(STRING_COLUMN_CLASS, entityClass);
            initializer = CodeBlock.of("new $T(TABLE, $S, $S)",
                    STRING_COLUMN_CLASS, col.columnName(), col.sqlType());
        } else if (col.isComparable()) {
            // ComparableColumn<Entity, ValueType>
            ClassName valueClass = getValueClass(col.javaType());
            fieldType = ParameterizedTypeName.get(COMPARABLE_COLUMN_CLASS, entityClass, valueClass);
            initializer = CodeBlock.of("new $T(TABLE, $S, $T.class, $S)",
                    COMPARABLE_COLUMN_CLASS, col.columnName(), valueClass, col.sqlType());
        } else {
            // Column<Entity, ValueType>
            ClassName valueClass = getValueClass(col.javaType());
            fieldType = ParameterizedTypeName.get(COLUMN_CLASS, entityClass, valueClass);
            initializer = CodeBlock.of("new $T(TABLE, $S, $T.class, $S)",
                    COLUMN_CLASS, col.columnName(), valueClass, col.sqlType());
        }

        return FieldSpec.builder(fieldType, col.getConstantName(),
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Column: $L ($L)\n", col.columnName(), col.sqlType())
                .initializer(initializer)
                .build();
    }

    /**
     * Generate a relationship field.
     */
    private FieldSpec generateRelationField(EntityMetadata metadata, RelationshipMetadata rel) {
        ClassName ownerClass = ClassName.get(metadata.packageName(), metadata.entityClassName());
        ClassName relatedClass = getValueClass(rel.relatedEntityClass());

        // Relation<Owner, Related>
        TypeName fieldType = ParameterizedTypeName.get(RELATION_CLASS, ownerClass, relatedClass);

        CodeBlock initializer = switch (rel.relationType()) {
            case HAS_ONE -> CodeBlock.of(
                    "$T.hasOne(TABLE, $T.TABLE, $S, $S, $L, $L, $S, $L, $T.of($L))",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.foreignKey(),
                    rel.localKey(),
                    rel.noForeignKey(),
                    rel.isEager(),
                    rel.fieldName(),
                    rel.withDefault(),
                    ClassName.get("java.util", "List"),
                    String.join(", ", rel.defaultAttributes().stream()
                            .map(s -> "\"" + s + "\"")
                            .toList())
            );
            case HAS_MANY -> CodeBlock.of(
                    "$T.hasMany(TABLE, $T.TABLE, $S, $S, $L, $L, $S)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.foreignKey(),
                    rel.localKey(),
                    rel.noForeignKey(),
                    rel.isEager(),
                    rel.fieldName()
            );
            case BELONGS_TO -> CodeBlock.of(
                    "$T.belongsTo(TABLE, $T.TABLE, $S, $S, $L, $L, $S, $L, $T.of($L), $L, $T.of($L))",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.foreignKey(),
                    rel.relatedKey(),
                    rel.noForeignKey(),
                    rel.isEager(),
                    rel.fieldName(),
                    rel.shouldTouch(),
                    LIST_CLASS,
                    formatStringList(rel.touchColumns()),
                    rel.withDefault(),
                    LIST_CLASS,
                    formatStringList(rel.defaultAttributes())
            );
            case BELONGS_TO_MANY -> CodeBlock.of(
                    "$T.belongsToMany(TABLE, $T.TABLE, $S, $S, $S, $S, $S, $T.of($L), $L, $L, $S)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.pivotTable(),
                    rel.foreignKey(), // foreignPivotKey
                    rel.relatedPivotKey(),
                    rel.localKey(),
                    rel.relatedKey(),
                    LIST_CLASS,
                    formatStringList(rel.pivotColumns()),
                    rel.pivotTimestamps(),
                    rel.isEager(),
                    rel.fieldName()
            );
            case HAS_ONE_THROUGH -> CodeBlock.of(
                    "$T.hasOneThrough(TABLE, $T.TABLE, $T.TABLE, $S, $S, $S, $S, $L, $S)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    ClassName.get(getPackage(rel.throughEntityClass()), rel.getThroughMetamodelClassName()),
                    rel.foreignKey(),      // firstKey
                    rel.relatedPivotKey(), // secondKey
                    rel.localKey(),
                    rel.relatedKey(),      // secondLocalKey
                    rel.isEager(),
                    rel.fieldName()
            );
            case HAS_MANY_THROUGH -> CodeBlock.of(
                    "$T.hasManyThrough(TABLE, $T.TABLE, $T.TABLE, $S, $S, $S, $S, $L, $S)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    ClassName.get(getPackage(rel.throughEntityClass()), rel.getThroughMetamodelClassName()),
                    rel.foreignKey(),      // firstKey
                    rel.relatedPivotKey(), // secondKey
                    rel.localKey(),
                    rel.relatedKey(),      // secondLocalKey
                    rel.isEager(),
                    rel.fieldName()
            );
            case MORPH_ONE -> CodeBlock.of(
                    "$T.morphOne(TABLE, $T.TABLE, $S, $S, $S, $S, $L)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.morphName(),
                    rel.morphTypeColumn(),
                    rel.morphIdColumn(),
                    rel.localKey(),
                    rel.isEager()
            );
            case MORPH_MANY -> CodeBlock.of(
                    "$T.morphMany(TABLE, $T.TABLE, $S, $S, $S, $S, $L)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.morphName(),
                    rel.morphTypeColumn(),
                    rel.morphIdColumn(),
                    rel.localKey(),
                    rel.isEager()
            );
            case MORPH_TO -> CodeBlock.of(
                    "$T.morphTo(TABLE, $S, $S, $S, $L)",
                    RELATION_CLASS,
                    rel.morphName(),
                    rel.morphTypeColumn(),
                    rel.morphIdColumn(),
                    rel.isEager()
            );
            case MORPH_TO_MANY -> CodeBlock.of(
                    "$T.morphToMany(TABLE, $T.TABLE, $S, $S, $S, $S, $S, $S, $S, $T.of($L), $L, $L)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.pivotTable(),
                    rel.morphName(),
                    rel.morphTypeColumn(),
                    rel.morphIdColumn(),
                    rel.relatedPivotKey(),
                    rel.localKey(),
                    rel.relatedKey(),
                    LIST_CLASS,
                    formatStringList(rel.pivotColumns()),
                    rel.pivotTimestamps(),
                    rel.isEager()
            );
            case MORPHED_BY_MANY -> CodeBlock.of(
                    "$T.morphedByMany(TABLE, $T.TABLE, $S, $S, $S, $S, $S, $S, $S, $T.of($L), $L, $L)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.pivotTable(),
                    rel.morphName(),
                    rel.morphTypeColumn(),
                    rel.morphIdColumn(),
                    rel.foreignKey(),           // foreignPivotKey (tag_id)
                    rel.localKey(),
                    rel.relatedKey(),
                    LIST_CLASS,
                    formatStringList(rel.pivotColumns()),
                    rel.pivotTimestamps(),
                    rel.isEager()
            );
            case LATEST_OF_MANY -> CodeBlock.of(
                    "$T.latestOfMany(TABLE, $T.TABLE, $S, $S, $S, $L, $S)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.foreignKey(),
                    rel.localKey(),
                    rel.orderColumn(),
                    rel.isEager(),
                    rel.fieldName()
            );
            case OLDEST_OF_MANY -> CodeBlock.of(
                    "$T.oldestOfMany(TABLE, $T.TABLE, $S, $S, $S, $L, $S)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.foreignKey(),
                    rel.localKey(),
                    rel.orderColumn(),
                    rel.isEager(),
                    rel.fieldName()
            );
            case OF_MANY -> CodeBlock.of(
                    "$T.ofMany(TABLE, $T.TABLE, $S, $S, $S, $S, $L, $S)",
                    RELATION_CLASS,
                    ClassName.get(getPackage(rel.relatedEntityClass()), rel.getRelatedMetamodelClassName()),
                    rel.foreignKey(),
                    rel.localKey(),
                    rel.aggregateColumn(),
                    rel.aggregateFunction(),
                    rel.isEager(),
                    rel.fieldName()
            );
        };

        String javadoc = switch (rel.relationType()) {
            case HAS_ONE -> "HasOne relationship to $L\n";
            case HAS_MANY -> "HasMany relationship to $L\n";
            case BELONGS_TO -> "BelongsTo relationship to $L\n";
            case BELONGS_TO_MANY -> "BelongsToMany relationship to $L via $L\n";
            case HAS_ONE_THROUGH -> "HasOneThrough relationship to $L via $L\n";
            case HAS_MANY_THROUGH -> "HasManyThrough relationship to $L via $L\n";
            case MORPH_ONE -> "MorphOne relationship to $L as $L\n";
            case MORPH_MANY -> "MorphMany relationship to $L as $L\n";
            case MORPH_TO -> "MorphTo relationship: $L\n";
            case MORPH_TO_MANY -> "MorphToMany relationship to $L as $L via $L\n";
            case MORPHED_BY_MANY -> "MorphedByMany relationship to $L as $L via $L\n";
            case LATEST_OF_MANY -> "LatestOfMany relationship to $L (ORDER BY $L DESC LIMIT 1)\n";
            case OLDEST_OF_MANY -> "OldestOfMany relationship to $L (ORDER BY $L ASC LIMIT 1)\n";
            case OF_MANY -> "OfMany relationship to $L ($L($L))\n";
        };

        FieldSpec.Builder builder = FieldSpec.builder(fieldType, rel.getConstantName(),
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(initializer);

        if (rel.relationType() == RelationType.BELONGS_TO_MANY) {
            builder.addJavadoc(javadoc, rel.relatedEntitySimpleName(), rel.pivotTable());
        } else if (rel.isThrough()) {
            builder.addJavadoc(javadoc, rel.relatedEntitySimpleName(), rel.throughEntitySimpleName());
        } else if (rel.relationType() == RelationType.MORPH_ONE || rel.relationType() == RelationType.MORPH_MANY) {
            builder.addJavadoc(javadoc, rel.relatedEntitySimpleName(), rel.morphName());
        } else if (rel.relationType() == RelationType.MORPH_TO) {
            builder.addJavadoc(javadoc, rel.morphName());
        } else if (rel.relationType() == RelationType.MORPH_TO_MANY || rel.relationType() == RelationType.MORPHED_BY_MANY) {
            builder.addJavadoc(javadoc, rel.relatedEntitySimpleName(), rel.morphName(), rel.pivotTable());
        } else if (rel.relationType() == RelationType.LATEST_OF_MANY || rel.relationType() == RelationType.OLDEST_OF_MANY) {
            builder.addJavadoc(javadoc, rel.relatedEntitySimpleName(), rel.orderColumn());
        } else if (rel.relationType() == RelationType.OF_MANY) {
            builder.addJavadoc(javadoc, rel.relatedEntitySimpleName(), rel.aggregateFunction(), rel.aggregateColumn());
        } else {
            builder.addJavadoc(javadoc, rel.relatedEntitySimpleName());
        }

        return builder.build();
    }

    /**
     * Extract package name from fully qualified class name.
     */
    private String getPackage(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }

    /**
     * Format a list of strings for code generation.
     */
    private String formatStringList(List<String> list) {
        if (Objects.isNull(list) || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .map(s -> "\"" + s + "\"")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private ClassName getColumnType(ColumnMetadata col) {
        if (col.isJsonb()) {
            return JSONB_COLUMN_CLASS;
        } else if (col.isString()) {
            return STRING_COLUMN_CLASS;
        } else if (col.isComparable()) {
            return COMPARABLE_COLUMN_CLASS;
        } else {
            return COLUMN_CLASS;
        }
    }

    private ClassName getValueClass(String javaType) {
        return switch (javaType) {
            case "long" -> ClassName.get(Long.class);
            case "int" -> ClassName.get(Integer.class);
            case "short" -> ClassName.get(Short.class);
            case "byte" -> ClassName.get(Byte.class);
            case "double" -> ClassName.get(Double.class);
            case "float" -> ClassName.get(Float.class);
            case "boolean" -> ClassName.get(Boolean.class);
            case "char" -> ClassName.get(Character.class);
            default -> {
                // Handle fully qualified class names
                int lastDot = javaType.lastIndexOf('.');
                if (lastDot > 0) {
                    yield ClassName.get(javaType.substring(0, lastDot), javaType.substring(lastDot + 1));
                }
                yield ClassName.get("", javaType);
            }
        };
    }
}
