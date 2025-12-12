package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * Column for PostgreSQL JSONB type with JSON-specific operators.
 *
 * @param <T> Table entity type
 */
public final class JsonbColumn<T> extends Column<T, String> {

    /**
     * Constructs a new JsonbColumn.
     *
     * @param table the table this column belongs to
     * @param name the column name
     * @param sqlType the SQL type
     */
    public JsonbColumn(Table<T> table, String name, String sqlType) {
        super(table, name, String.class, sqlType);
    }

    /**
     * JSONB contains: column @> '{"key": "value"}'
     *
     * @param jsonValue the JSON value to check
     * @return predicate for contains check
     */
    public Predicate jsonbContains(String jsonValue) {
        return new Predicate.SimplePredicate(this, Operator.JSONB_CONTAINS,
                new JsonLiteral(jsonValue));
    }

    /**
     * JSONB contained by: column &lt;@ '{"key": "value"}'
     *
     * @param jsonValue the JSON value to check
     * @return predicate for contained by check
     */
    public Predicate jsonbContainedBy(String jsonValue) {
        return new Predicate.SimplePredicate(this, Operator.JSONB_CONTAINED_BY,
                new JsonLiteral(jsonValue));
    }

    /**
     * JSONB key exists: column ? 'key'
     *
     * @param key the key to check
     * @return predicate for key exists check
     */
    public Predicate jsonbKeyExists(String key) {
        return new Predicate.SimplePredicate(this, Operator.JSONB_KEY_EXISTS,
                new Literal<>(key, String.class));
    }

    /**
     * JSONB arrow operator: column -> 'key' (returns JSON)
     *
     * @param key the JSON key
     * @return JsonPathExpression for the key
     */
    public JsonPathExpression arrow(String key) {
        return new JsonPathExpression(this, key, false);
    }

    /**
     * JSONB arrow text operator: column ->> 'key' (returns text)
     *
     * @param key the JSON key
     * @return JsonPathExpression for the key as text
     */
    public JsonPathExpression arrowText(String key) {
        return new JsonPathExpression(this, key, true);
    }

    /**
     * Navigate nested JSONB path: column->'key1'->'key2'->>'key3' (returns text).
     * <pre>{@code
     * // meta_data->'feedback'->>'type'
     * Message_.META_DATA.jsonPath("feedback", "type")
     *
     * // meta_data->'settings'->'notifications'->>'enabled'
     * User_.META_DATA.jsonPath("settings", "notifications", "enabled")
     * }</pre>
     *
     * @param keys path segments (last one extracted as text)
     * @return JsonPathExpression for further operations
     */
    public JsonPathExpression jsonPath(String... keys) {
        if (keys.length == 0) {
            throw new IllegalArgumentException("At least one path key is required");
        }
        return new JsonPathExpression(this, Arrays.asList(keys));
    }

    /**
     * Navigate nested JSONB path returning JSONB: column->'key1'->'key2'.
     * <pre>{@code
     * // meta_data->'feedback' (returns JSONB)
     * Message_.META_DATA.jsonPathJson("feedback")
     * }</pre>
     *
     * @param keys path segments (all returned as JSONB)
     * @return JsonPathExpression for further operations
     */
    public JsonPathExpression jsonPathJson(String... keys) {
        if (keys.length == 0) {
            throw new IllegalArgumentException("At least one path key is required");
        }
        return new JsonPathExpression(this, Arrays.asList(keys), false);
    }

    /**
     * Represents a JSON path expression for chaining.
     * Supports single key (->/'key') or nested path (->key1->key2->>key3).
     */
    public static final class JsonPathExpression implements Expression<String> {
        private final Column<?, ?> column;
        private final List<String> pathKeys;
        private final boolean lastAsText;
        private String alias;

        // Legacy single-key constructor
        JsonPathExpression(Column<?, ?> column, String path, boolean asText) {
            this.column = column;
            this.pathKeys = List.of(path);
            this.lastAsText = asText;
        }

        // New multi-key constructor (last key as text by default)
        JsonPathExpression(Column<?, ?> column, List<String> keys) {
            this.column = column;
            this.pathKeys = new ArrayList<>(keys);
            this.lastAsText = true;
        }

        // Multi-key constructor with explicit text/json flag
        JsonPathExpression(Column<?, ?> column, List<String> keys, boolean lastAsText) {
            this.column = column;
            this.pathKeys = new ArrayList<>(keys);
            this.lastAsText = lastAsText;
        }

        /**
         * Add alias to this expression.
         *
         * @param alias the alias name
         * @return this expression for chaining
         */
        public JsonPathExpression as(String alias) {
            this.alias = alias;
            return this;
        }

        /**
         * Equals predicate for JSON path expression.
         *
         * @param value the value to compare
         * @return predicate for equality check
         */
        public Predicate eq(String value) {
            return new Predicate.SimplePredicate(this, Operator.EQUALS, new Literal<>(value, String.class));
        }

        /**
         * IS NULL predicate for JSON path expression.
         *
         * @return predicate for null check
         */
        public Predicate isNull() {
            return new Predicate.SimplePredicate(this, Operator.IS_NULL, null);
        }

        /**
         * IS NOT NULL predicate for JSON path expression.
         *
         * @return predicate for not null check
         */
        public Predicate isNotNull() {
            return new Predicate.SimplePredicate(this, Operator.IS_NOT_NULL, null);
        }

        @Override
        public Class<String> getValueType() {
            return String.class;
        }

        @Override
        public String toSql(SqlDialect dialect) {
            // Build nested path expression using dialect-specific JSON syntax
            String result = column.toSql(dialect);

            for (int i = 0; i < pathKeys.size(); i++) {
                boolean isLast = (i == pathKeys.size() - 1);
                boolean asText = isLast && lastAsText;
                result = dialect.jsonExtract(result, pathKeys.get(i), asText);
            }

            if (nonNull(alias)) {
                result = result + " AS " + alias;
            }

            return result;
        }
    }

    /**
     * JSON literal for JSONB operators.
     * PostgreSQL: 'value'::jsonb
     * MySQL: 'value' (no cast needed, JSON_CONTAINS handles it)
     *
     * @param json the JSON string value
     */
    public record JsonLiteral(String json) implements Expression<String> {
        @Override
        public Class<String> getValueType() {
            return String.class;
        }

        @Override
        public String toSql(SqlDialect dialect) {
            String quoted = dialect.quoteString(json);
            // PostgreSQL needs explicit ::jsonb cast
            if (dialect.capabilities().supportsJsonb()) {
                return quoted + "::jsonb";
            }
            // MySQL JSON functions handle type conversion internally
            return quoted;
        }
    }
}
