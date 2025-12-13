package sant1ago.dev.suprim.core.type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * SQL function builder for type-safe database function calls.
 *
 * <pre>{@code
 * // Date/Time functions
 * Fn.now()                                    // NOW()
 * Fn.currentDate()                            // CURRENT_DATE
 * Fn.currentTime()                            // CURRENT_TIME
 *
 * // Random
 * Fn.random()                                 // RANDOM() or RAND()
 *
 * // String functions
 * Fn.upper(User_.NAME)                        // UPPER("name")
 * Fn.lower(User_.EMAIL)                       // LOWER("email")
 * Fn.concat(User_.FIRST, " ", User_.LAST)     // CONCAT("first", ' ', "last")
 * Fn.coalesce(User_.NICKNAME, "Anonymous")    // COALESCE("nickname", 'Anonymous')
 *
 * // Custom functions
 * Fn.call("my_function", arg1, arg2)          // my_function(arg1, arg2)
 * Fn.<BigDecimal>call("calculate_tax", amount) // Type-safe custom function
 *
 * // Raw expressions
 * Fn.raw("EXTRACT(YEAR FROM ?)", User_.CREATED_AT)
 * Fn.raw("? AT TIME ZONE 'UTC'", User_.CREATED_AT)
 *
 * // Usage in queries
 * Suprim.select(User_.ID, Fn.now().as("current_time"))
 *     .from(User_.TABLE)
 *     .where(User_.CREATED_AT.lt(Fn.now()))
 *     .build(dialect);
 *
 * Suprim.insertInto(User_.TABLE)
 *     .column(User_.CREATED_AT, Fn.now())
 *     .build(dialect);
 * }</pre>
 */
public final class Fn {

    private Fn() {
        // Static factory class
    }

    // ==================== DATE/TIME FUNCTIONS ====================

    /**
     * NOW() - current date and time.
     * <pre>{@code
     * Fn.now() // NOW()
     * }</pre>
     */
    public static SqlFunction<LocalDateTime> now() {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.NOW, LocalDateTime.class);
    }

    /**
     * CURRENT_DATE - current date only.
     */
    public static SqlFunction<LocalDate> currentDate() {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.CURRENT_DATE, LocalDate.class);
    }

    /**
     * CURRENT_TIME - current time only.
     */
    public static SqlFunction<LocalTime> currentTime() {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.CURRENT_TIME, LocalTime.class);
    }

    /**
     * CURRENT_TIMESTAMP - current timestamp.
     */
    public static SqlFunction<LocalDateTime> currentTimestamp() {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.CURRENT_TIMESTAMP, LocalDateTime.class);
    }

    // ==================== RANDOM ====================

    /**
     * RANDOM() / RAND() - random number between 0 and 1.
     * Uses RANDOM() for PostgreSQL, RAND() for MySQL/MariaDB.
     */
    public static SqlFunction<Double> random() {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.RANDOM, Double.class);
    }

    // ==================== STRING FUNCTIONS ====================

    /**
     * UPPER(column) - convert to uppercase.
     */
    public static SqlFunction<String> upper(Expression<String> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.UPPER, String.class, column);
    }

    /**
     * UPPER(value) - convert string to uppercase.
     */
    public static SqlFunction<String> upper(String value) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.UPPER, String.class, value);
    }

    /**
     * LOWER(column) - convert to lowercase.
     */
    public static SqlFunction<String> lower(Expression<String> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.LOWER, String.class, column);
    }

    /**
     * LOWER(value) - convert string to lowercase.
     */
    public static SqlFunction<String> lower(String value) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.LOWER, String.class, value);
    }

    /**
     * LENGTH(column) - string length.
     */
    public static SqlFunction<Integer> length(Expression<String> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.LENGTH, Integer.class, column);
    }

    /**
     * TRIM(column) - trim whitespace from both ends.
     */
    public static SqlFunction<String> trim(Expression<String> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.TRIM, String.class, column);
    }

    /**
     * LTRIM(column) - trim whitespace from left.
     */
    public static SqlFunction<String> ltrim(Expression<String> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.LTRIM, String.class, column);
    }

    /**
     * RTRIM(column) - trim whitespace from right.
     */
    public static SqlFunction<String> rtrim(Expression<String> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.RTRIM, String.class, column);
    }

    /**
     * CONCAT(args...) - concatenate strings.
     * <pre>{@code
     * Fn.concat(User_.FIRST_NAME, " ", User_.LAST_NAME)
     * // CONCAT("first_name", ' ', "last_name")
     * }</pre>
     */
    public static SqlFunction<String> concat(Object... args) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.CONCAT, String.class, args);
    }

    /**
     * SUBSTRING(column, start, length) - extract substring.
     * Note: PostgreSQL uses 1-based indexing.
     */
    public static SqlFunction<String> substring(Expression<String> column, int start, int length) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.SUBSTRING, String.class, column, start, length);
    }

    /**
     * REPLACE(column, from, to) - replace occurrences.
     */
    public static SqlFunction<String> replace(Expression<String> column, String from, String to) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.REPLACE, String.class, column, from, to);
    }

    /**
     * REVERSE(column) - reverse string.
     */
    public static SqlFunction<String> reverse(Expression<String> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.REVERSE, String.class, column);
    }

    // ==================== MATH FUNCTIONS ====================

    /**
     * ABS(column) - absolute value.
     */
    public static <T extends Number> SqlFunction<T> abs(Expression<T> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.ABS, column.getValueType(), column);
    }

    /**
     * CEIL(column) - round up to nearest integer.
     */
    public static SqlFunction<Long> ceil(Expression<? extends Number> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.CEIL, Long.class, column);
    }

    /**
     * FLOOR(column) - round down to nearest integer.
     */
    public static SqlFunction<Long> floor(Expression<? extends Number> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.FLOOR, Long.class, column);
    }

    /**
     * ROUND(column) - round to nearest integer.
     */
    public static SqlFunction<Long> round(Expression<? extends Number> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.ROUND, Long.class, column);
    }

    /**
     * ROUND(column, decimals) - round to specified decimal places.
     */
    public static SqlFunction<BigDecimal> round(Expression<? extends Number> column, int decimals) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.ROUND, BigDecimal.class, column, decimals);
    }

    /**
     * SQRT(column) - square root.
     */
    public static SqlFunction<Double> sqrt(Expression<? extends Number> column) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.SQRT, Double.class, column);
    }

    /**
     * POWER(base, exponent) - raise to power.
     */
    public static SqlFunction<Double> power(Expression<? extends Number> base, Number exponent) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.POWER, Double.class, base, exponent);
    }

    /**
     * MOD(dividend, divisor) - modulo operation.
     */
    public static SqlFunction<Long> mod(Expression<? extends Number> dividend, Number divisor) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.MOD, Long.class, dividend, divisor);
    }

    // ==================== NULL HANDLING ====================

    /**
     * COALESCE(args...) - return first non-null value.
     * <pre>{@code
     * Fn.coalesce(User_.NICKNAME, User_.NAME, "Anonymous")
     * // COALESCE("nickname", "name", 'Anonymous')
     * }</pre>
     */
    public static <T> SqlFunction<T> coalesce(Expression<T> first, Object... rest) {
        Object[] args = new Object[rest.length + 1];
        args[0] = first;
        System.arraycopy(rest, 0, args, 1, rest.length);
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.COALESCE, first.getValueType(), args);
    }

    /**
     * NULLIF(value1, value2) - return NULL if equal.
     */
    public static <T> SqlFunction<T> nullIf(Expression<T> value1, T value2) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.NULLIF, value1.getValueType(), value1, value2);
    }

    /**
     * IFNULL(column, default) - return default if NULL.
     * PostgreSQL uses COALESCE, MySQL uses IFNULL.
     */
    public static <T> SqlFunction<T> ifNull(Expression<T> column, T defaultValue) {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.IFNULL, column.getValueType(), column, defaultValue);
    }

    // ==================== UUID ====================

    /**
     * Generate random UUID.
     * <ul>
     *   <li>PostgreSQL 13+: gen_random_uuid() (built-in)</li>
     *   <li>PostgreSQL &lt; 13: requires CREATE EXTENSION pgcrypto;</li>
     *   <li>MySQL: UUID() (built-in, returns string format)</li>
     * </ul>
     */
    public static SqlFunction<UUID> uuid() {
        return SqlFunction.builtin(SqlFunction.BuiltinFunction.GEN_RANDOM_UUID, UUID.class);
    }

    // ==================== CUSTOM FUNCTION CALL ====================

    /**
     * Call a custom database function.
     * <pre>{@code
     * Fn.call("my_function", arg1, arg2)
     * Fn.<BigDecimal>call("calculate_tax", Order_.TOTAL)
     * }</pre>
     *
     * @param name function name
     * @param args function arguments (can be Expression, String, Number, etc.)
     * @return function expression
     */
    public static SqlFunction<Object> call(String name, Object... args) {
        return SqlFunction.custom(name, Object.class, args);
    }

    /**
     * Call a custom database function with explicit return type.
     * <pre>{@code
     * Fn.<BigDecimal>callTyped(BigDecimal.class, "calculate_tax", Order_.TOTAL)
     * }</pre>
     */
    public static <T> SqlFunction<T> callTyped(Class<T> returnType, String name, Object... args) {
        return SqlFunction.custom(name, returnType, args);
    }

    // ==================== RAW EXPRESSION ====================

    /**
     * Raw SQL expression with placeholders.
     * Use ? as placeholder for arguments.
     * <pre>{@code
     * Fn.raw("EXTRACT(YEAR FROM ?)", User_.CREATED_AT)
     * Fn.raw("? AT TIME ZONE 'UTC'", User_.CREATED_AT)
     * Fn.raw("CASE WHEN ? > 100 THEN 'high' ELSE 'low' END", Order_.TOTAL)
     * }</pre>
     *
     * @param sql SQL expression with ? placeholders
     * @param args arguments to substitute for placeholders
     * @return raw SQL function expression
     */
    public static SqlFunction<Object> raw(String sql, Object... args) {
        return SqlFunction.raw(sql, Object.class, args);
    }

    /**
     * Raw SQL expression with explicit return type.
     * <pre>{@code
     * Fn.<Integer>rawTyped(Integer.class, "EXTRACT(YEAR FROM ?)", User_.CREATED_AT)
     * }</pre>
     */
    public static <T> SqlFunction<T> rawTyped(Class<T> returnType, String sql, Object... args) {
        return SqlFunction.raw(sql, returnType, args);
    }
}
