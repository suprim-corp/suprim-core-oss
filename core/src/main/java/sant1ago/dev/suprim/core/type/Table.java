package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import java.util.Objects;

/**
 * Represents a database table with entity type information.
 *
 * @param <T> The entity class this table maps to
 */
public final class Table<T> {
    private final String name;
    private final String schema;
    private final Class<T> entityType;
    private final String alias;

    public Table(String name, String schema, Class<T> entityType) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.schema = schema; // schema can be null
        this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
        this.alias = null;
    }

    private Table(String name, String schema, Class<T> entityType, String alias) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.schema = schema; // schema can be null
        this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
        this.alias = alias; // alias can be null
    }

    /**
     * Factory method to create a table reference with just name and entity type.
     * Schema defaults to empty string.
     *
     * @param name table name
     * @param entityType entity class
     * @return new Table instance
     */
    public static <T> Table<T> of(String name, Class<T> entityType) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(entityType, "entityType cannot be null");
        return new Table<>(name, "", entityType);
    }

    /**
     * Factory method to create a table reference with name, schema, and entity type.
     *
     * @param name table name
     * @param schema schema name
     * @param entityType entity class
     * @return new Table instance
     */
    public static <T> Table<T> of(String name, String schema, Class<T> entityType) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(entityType, "entityType cannot be null");
        return new Table<>(name, schema, entityType);
    }

    /**
     * Create a new table reference with an alias.
     */
    public Table<T> as(String alias) {
        return new Table<>(name, schema, entityType, alias);
    }

    /**
     * Get a fully qualified table name (schema.table).
     */
    public String getQualifiedName() {
        if (Objects.isNull(schema) || schema.isEmpty()) {
            return name;
        }
        return schema + "." + name;
    }

    /**
     * Get SQL reference (alias if set, otherwise qualified name).
     */
    public String getSqlReference() {
        return Objects.nonNull(alias) ? alias : name;
    }

    /**
     * Render table for FROM clause with alias.
     */
    public String toSql(SqlDialect dialect) {
        String qualified = Objects.nonNull(schema) && !schema.isEmpty()
                ? dialect.quoteIdentifier(schema) + "." + dialect.quoteIdentifier(name)
                : dialect.quoteIdentifier(name);

        if (Objects.nonNull(alias)) {
            return qualified + " " + alias;
        }
        return qualified;
    }

    public String getName() {
        return name;
    }

    public String getSchema() {
        return schema;
    }

    public Class<T> getEntityType() {
        return entityType;
    }

    public String getAlias() {
        return alias;
    }
}
