package sant1ago.dev.suprim.processor.model;

import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.IdGenerator;

import static java.util.Objects.nonNull;

/**
 * Metadata for a single column extracted from @Column annotated field.
 */
public record ColumnMetadata(
        String fieldName,
        String columnName,
        String javaType,
        String sqlType,
        boolean isPrimaryKey,
        boolean isComparable,
        boolean isString,
        boolean isJsonb,
        GenerationType generationType,
        String sequenceName,
        String generatorClass
) {
    /**
     * Constructor without generation info (backwards compatible).
     */
    public ColumnMetadata(
            String fieldName,
            String columnName,
            String javaType,
            String sqlType,
            boolean isPrimaryKey,
            boolean isComparable,
            boolean isString,
            boolean isJsonb
    ) {
        this(fieldName, columnName, javaType, sqlType, isPrimaryKey, isComparable, isString, isJsonb,
             GenerationType.NONE, "", null);
    }

    /**
     * Constructor without custom generator (backwards compatible).
     */
    public ColumnMetadata(
            String fieldName,
            String columnName,
            String javaType,
            String sqlType,
            boolean isPrimaryKey,
            boolean isComparable,
            boolean isString,
            boolean isJsonb,
            GenerationType generationType,
            String sequenceName
    ) {
        this(fieldName, columnName, javaType, sqlType, isPrimaryKey, isComparable, isString, isJsonb,
             generationType, sequenceName, null);
    }

    /**
     * Convert field name to UPPER_SNAKE_CASE for constant name.
     */
    public String getConstantName() {
        return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    /**
     * Check if this column has a custom generator.
     */
    public boolean hasCustomGenerator() {
        return nonNull(generatorClass) && !generatorClass.equals(IdGenerator.None.class.getName());
    }

    /**
     * Check if this column has a built-in generation strategy.
     */
    public boolean hasGenerationType() {
        return !generationType.isNone();
    }

    /**
     * Check if ID is application-generated (custom generator, UUID, UUID_V7).
     */
    public boolean isApplicationGenerated() {
        return hasCustomGenerator() || generationType.isApplicationGenerated();
    }

    /**
     * Check if ID is database-generated (IDENTITY, SEQUENCE, UUID_DB).
     */
    public boolean isDatabaseGenerated() {
        return !hasCustomGenerator() && generationType.isDatabaseGenerated();
    }
}
