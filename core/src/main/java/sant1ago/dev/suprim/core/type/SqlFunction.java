package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Type-safe SQL function expression.
 * Supports built-in functions (NOW(), RANDOM(), etc.) and custom function calls.
 *
 * <pre>{@code
 * // Built-in functions
 * Fn.now()                           // NOW()
 * Fn.random()                        // RANDOM() or RAND() depending on dialect
 * Fn.upper(User_.NAME)               // UPPER("name")
 * Fn.coalesce(User_.NICKNAME, "N/A") // COALESCE("nickname", 'N/A')
 *
 * // Custom functions
 * Fn.call("my_func", arg1, arg2)     // my_func(arg1, arg2)
 *
 * // Raw expressions
 * Fn.raw("EXTRACT(YEAR FROM ?)", User_.CREATED_AT)
 * }</pre>
 *
 * @param <V> the return type of the function
 */
public final class SqlFunction<V> implements Expression<V> {

    private final String functionName;
    private final List<Object> arguments;
    private final Class<V> valueType;
    private final FunctionType type;
    private final String rawSql;
    private String alias;

    private SqlFunction(String functionName, List<Object> arguments, Class<V> valueType, FunctionType type, String rawSql) {
        this.functionName = functionName;
        this.arguments = nonNull(arguments) ? new ArrayList<>(arguments) : new ArrayList<>();
        this.valueType = Objects.requireNonNull(valueType, "valueType cannot be null");
        this.type = type;
        this.rawSql = rawSql;
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create a built-in function (dialect-aware).
     */
    static <V> SqlFunction<V> builtin(BuiltinFunction function, Class<V> valueType, Object... args) {
        return new SqlFunction<>(function.name(), List.of(args), valueType, FunctionType.BUILTIN, null);
    }

    /**
     * Create a custom function call.
     */
    static <V> SqlFunction<V> custom(String name, Class<V> valueType, Object... args) {
        return new SqlFunction<>(name, List.of(args), valueType, FunctionType.CUSTOM, null);
    }

    /**
     * Create a raw SQL expression with placeholders.
     */
    static <V> SqlFunction<V> raw(String sql, Class<V> valueType, Object... args) {
        return new SqlFunction<>(null, List.of(args), valueType, FunctionType.RAW, sql);
    }

    // ==================== MODIFIERS ====================

    /**
     * Add alias to function expression.
     *
     * @param alias the column alias
     * @return this function for chaining
     */
    public SqlFunction<V> as(String alias) {
        this.alias = alias;
        return this;
    }

    // ==================== COMPARISON OPERATORS ====================

    /**
     * Equals: function = value (for WHERE clause).
     */
    public Predicate eq(V value) {
        return new Predicate.SimplePredicate(this, Operator.EQUALS, new Literal<>(value, valueType));
    }

    /**
     * Equals another expression.
     */
    public Predicate eq(Expression<V> other) {
        return new Predicate.SimplePredicate(this, Operator.EQUALS, other);
    }

    /**
     * Not equals: function != value.
     */
    public Predicate ne(V value) {
        return new Predicate.SimplePredicate(this, Operator.NOT_EQUALS, new Literal<>(value, valueType));
    }

    /**
     * Greater than: function > value.
     */
    public Predicate gt(V value) {
        return new Predicate.SimplePredicate(this, Operator.GREATER_THAN, new Literal<>(value, valueType));
    }

    /**
     * Greater than or equals: function >= value.
     */
    public Predicate gte(V value) {
        return new Predicate.SimplePredicate(this, Operator.GREATER_THAN_OR_EQUALS, new Literal<>(value, valueType));
    }

    /**
     * Less than: function < value.
     */
    public Predicate lt(V value) {
        return new Predicate.SimplePredicate(this, Operator.LESS_THAN, new Literal<>(value, valueType));
    }

    /**
     * Less than or equals: function <= value.
     */
    public Predicate lte(V value) {
        return new Predicate.SimplePredicate(this, Operator.LESS_THAN_OR_EQUALS, new Literal<>(value, valueType));
    }

    // ==================== ORDERING ====================

    /**
     * Ascending order for ORDER BY clause.
     */
    public OrderSpec asc() {
        return OrderSpec.raw(this.toSql(sant1ago.dev.suprim.core.dialect.PostgreSqlDialect.INSTANCE) + " ASC");
    }

    /**
     * Descending order for ORDER BY clause.
     */
    public OrderSpec desc() {
        return OrderSpec.raw(this.toSql(sant1ago.dev.suprim.core.dialect.PostgreSqlDialect.INSTANCE) + " DESC");
    }

    // ==================== EXPRESSION ====================

    @Override
    public Class<V> getValueType() {
        return valueType;
    }

    @Override
    public String toSql(SqlDialect dialect) {
        StringBuilder sb = new StringBuilder();

        switch (type) {
            case BUILTIN -> sb.append(renderBuiltinFunction(dialect));
            case CUSTOM -> sb.append(renderCustomFunction(dialect));
            case RAW -> sb.append(renderRawExpression(dialect));
        }

        if (nonNull(alias)) {
            sb.append(" AS ").append(dialect.quoteIdentifier(alias));
        }

        return sb.toString();
    }

    private String renderBuiltinFunction(SqlDialect dialect) {
        BuiltinFunction fn = BuiltinFunction.valueOf(functionName);
        String fnName = fn.getSqlName(dialect);

        StringBuilder sb = new StringBuilder();
        sb.append(fnName).append("(");
        sb.append(renderArguments(dialect));
        sb.append(")");
        return sb.toString();
    }

    private String renderCustomFunction(SqlDialect dialect) {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append("(");
        sb.append(renderArguments(dialect));
        sb.append(")");
        return sb.toString();
    }

    private String renderRawExpression(SqlDialect dialect) {
        if (arguments.isEmpty()) {
            return rawSql;
        }

        // Replace ? placeholders with actual argument SQL
        String result = rawSql;
        for (Object arg : arguments) {
            String argSql = renderArgument(arg, dialect);
            result = result.replaceFirst("\\?", argSql);
        }
        return result;
    }

    private String renderArguments(SqlDialect dialect) {
        if (arguments.isEmpty()) {
            return "";
        }

        List<String> argStrings = new ArrayList<>();
        for (Object arg : arguments) {
            argStrings.add(renderArgument(arg, dialect));
        }
        return String.join(", ", argStrings);
    }

    private String renderArgument(Object arg, SqlDialect dialect) {
        if (isNull(arg)) {
            return dialect.nullLiteral();
        } else if (arg instanceof Expression<?> expr) {
            return expr.toSql(dialect);
        } else if (arg instanceof String s) {
            return dialect.quoteString(s);
        } else if (arg instanceof Number n) {
            return n.toString();
        } else if (arg instanceof Boolean b) {
            return dialect.formatBoolean(b);
        } else {
            return arg.toString();
        }
    }

    // ==================== GETTERS ====================

    public String getAlias() {
        return alias;
    }

    // ==================== ENUMS ====================

    enum FunctionType {
        BUILTIN,
        CUSTOM,
        RAW
    }

    /**
     * Built-in SQL functions with dialect-aware rendering.
     */
    public enum BuiltinFunction {
        // Date/Time functions
        NOW("NOW", "NOW"),
        CURRENT_DATE("CURRENT_DATE", "CURRENT_DATE"),
        CURRENT_TIME("CURRENT_TIME", "CURRENT_TIME"),
        CURRENT_TIMESTAMP("CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP"),

        // Random
        RANDOM("RANDOM", "RAND"),

        // String functions
        UPPER("UPPER", "UPPER"),
        LOWER("LOWER", "LOWER"),
        LENGTH("LENGTH", "LENGTH"),
        TRIM("TRIM", "TRIM"),
        LTRIM("LTRIM", "LTRIM"),
        RTRIM("RTRIM", "RTRIM"),
        CONCAT("CONCAT", "CONCAT"),
        SUBSTRING("SUBSTRING", "SUBSTRING"),
        REPLACE("REPLACE", "REPLACE"),
        REVERSE("REVERSE", "REVERSE"),

        // Math functions
        ABS("ABS", "ABS"),
        CEIL("CEIL", "CEIL"),
        FLOOR("FLOOR", "FLOOR"),
        ROUND("ROUND", "ROUND"),
        SQRT("SQRT", "SQRT"),
        POWER("POWER", "POWER"),
        MOD("MOD", "MOD"),

        // Null handling
        COALESCE("COALESCE", "COALESCE"),
        NULLIF("NULLIF", "NULLIF"),
        IFNULL("COALESCE", "IFNULL"),

        // UUID
        GEN_RANDOM_UUID("gen_random_uuid", "UUID");

        private final String postgresName;
        private final String mysqlName;

        BuiltinFunction(String postgresName, String mysqlName) {
            this.postgresName = postgresName;
            this.mysqlName = mysqlName;
        }

        public String getSqlName(SqlDialect dialect) {
            String dialectName = dialect.getName().toLowerCase();
            if (dialectName.contains("mysql") || dialectName.contains("mariadb")) {
                return mysqlName;
            }
            return postgresName;
        }
    }
}
