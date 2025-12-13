package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.type.TypeUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;

/**
 * Converts JDBC ResultSet values to Java types.
 * Handles primitives, wrappers, java.time types, enums, and JSON/JSONB.
 */
final class ResultSetTypeConverter {

    private ResultSetTypeConverter() {
        // Utility class
    }

    /**
     * Get value from ResultSet and convert to target type.
     *
     * @param rs    the ResultSet
     * @param index the column index (1-based)
     * @param type  the target Java type
     * @return the converted value, or default for primitives if null
     */
    static Object getValue(ResultSet rs, int index, Class<?> type) throws SQLException {
        Object value = rs.getObject(index);

        if (rs.wasNull()) {
            return getDefaultValue(type);
        }

        // Boolean conversion
        if (type == boolean.class || type == Boolean.class) {
            return convertToBoolean(value);
        }

        // Number conversions
        if (value instanceof Number number) {
            Object converted = convertNumber(number, type);
            if (Objects.nonNull(converted)) {
                return converted;
            }
        }

        // Date/Time conversions
        Object dateTime = convertDateTime(value, type);
        if (Objects.nonNull(dateTime)) {
            return dateTime;
        }

        // Enum conversion
        if (type.isEnum()) {
            return convertToEnum(value, type);
        }

        // JSON/JSONB to Map conversion
        if (Map.class.isAssignableFrom(type) || type == Object.class) {
            Object jsonResult = convertFromJson(value, type);
            if (Objects.nonNull(jsonResult)) {
                return jsonResult;
            }
        }

        return value;
    }

    /**
     * Get default value for a type (0 for numeric primitives, false for boolean, null for objects).
     */
    static Object getDefaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == double.class) return 0.0;
            if (type == float.class) return 0.0f;
            if (type == short.class) return (short) 0;
            if (type == byte.class) return (byte) 0;
            if (type == char.class) return '\0';
        }
        return null;
    }

    // ==================== NUMBER CONVERSION ====================

    private static Object convertNumber(Number number, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return number.intValue();
        }
        if (type == long.class || type == Long.class) {
            return number.longValue();
        }
        if (type == double.class || type == Double.class) {
            return number.doubleValue();
        }
        if (type == float.class || type == Float.class) {
            return number.floatValue();
        }
        if (type == short.class || type == Short.class) {
            return number.shortValue();
        }
        if (type == byte.class || type == Byte.class) {
            return number.byteValue();
        }
        return null;
    }

    // ==================== BOOLEAN CONVERSION ====================

    private static boolean convertToBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String s) {
            String lower = s.toLowerCase();
            return "true".equals(lower) || "t".equals(lower) || "yes".equals(lower)
                    || "y".equals(lower) || "1".equals(lower);
        }
        return false;
    }

    // ==================== DATE/TIME CONVERSION ====================

    private static Object convertDateTime(Object value, Class<?> type) {
        if (type == LocalDateTime.class) {
            return convertToLocalDateTime(value);
        }
        if (type == LocalDate.class) {
            return convertToLocalDate(value);
        }
        if (type == Instant.class) {
            return convertToInstant(value);
        }
        if (type == OffsetDateTime.class) {
            return convertToOffsetDateTime(value);
        }
        return null;
    }

    private static LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay();
        }
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }

    private static LocalDate convertToLocalDate(Object value) {
        if (value instanceof LocalDate ld) {
            return ld;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private static Instant convertToInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        return null;
    }

    private static OffsetDateTime convertToOffsetDateTime(Object value) {
        if (value instanceof OffsetDateTime odt) {
            return odt;
        }
        if (value instanceof Timestamp ts) {
            return OffsetDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof java.util.Date date) {
            return OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }

    // ==================== ENUM CONVERSION ====================

    private static <E extends Enum<E>> Object convertToEnum(Object value, Class<?> enumType) {
        if (enumType.isInstance(value)) {
            return value;
        }

        Class<E> enumClass = TypeUtils.cast(enumType);
        E[] constants = enumClass.getEnumConstants();

        if (value instanceof String s) {
            // Try case-insensitive match first
            for (E constant : constants) {
                if (constant.name().equalsIgnoreCase(s)) {
                    return constant;
                }
            }
            // Exact match fallback (will throw if not found)
            return Enum.valueOf(enumClass, s);
        }

        if (value instanceof Number n) {
            int ordinal = n.intValue();
            if (ordinal >= 0 && ordinal < constants.length) {
                return constants[ordinal];
            }
        }

        return null;
    }

    // ==================== JSON/JSONB CONVERSION ====================

    private static Object convertFromJson(Object value, Class<?> type) {
        String jsonString = extractJsonString(value);

        if (Objects.isNull(jsonString) || jsonString.isEmpty()) {
            return null;
        }

        return parseJson(jsonString, type);
    }

    private static String extractJsonString(Object value) {
        // Handle PostgreSQL PGobject (jsonb/json types)
        if (value.getClass().getName().equals("org.postgresql.util.PGobject")) {
            try {
                var getType = value.getClass().getMethod("getType");
                var getValue = value.getClass().getMethod("getValue");
                String pgType = (String) getType.invoke(value);
                if ("jsonb".equals(pgType) || "json".equals(pgType)) {
                    return (String) getValue.invoke(value);
                }
            } catch (Exception e) {
                return null;
            }
        } else if (value instanceof String s) {
            return s;
        }
        return null;
    }

    private static Object parseJson(String jsonString, Class<?> type) {
        // Try Jackson 3.x first
        try {
            var mapperClass = Class.forName("tools.jackson.databind.json.JsonMapper");
            var builderMethod = mapperClass.getMethod("builder");
            var builder = builderMethod.invoke(null);
            var buildMethod = builder.getClass().getMethod("build");
            var mapper = buildMethod.invoke(builder);
            var readValueMethod = mapper.getClass().getMethod("readValue", String.class, Class.class);
            return readValueMethod.invoke(mapper, jsonString, type);
        } catch (ClassNotFoundException e) {
            // Jackson 3.x not available, try Jackson 2.x
            return parseJsonWithJackson2(jsonString, type);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object parseJsonWithJackson2(String jsonString, Class<?> type) {
        try {
            var mapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            var mapper = mapperClass.getDeclaredConstructor().newInstance();
            var readValueMethod = mapperClass.getMethod("readValue", String.class, Class.class);
            return readValueMethod.invoke(mapper, jsonString, type);
        } catch (Exception e) {
            return null;
        }
    }
}
