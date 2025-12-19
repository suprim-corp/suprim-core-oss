package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.casey.Casey;
import sant1ago.dev.suprim.jdbc.exception.MappingException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared reflection utilities for field access and name conversion.
 * Thread-safe with cached lookups for performance.
 *
 * <p>Access strategy (in order of preference):
 * <ol>
 *   <li>Record component accessors (for records)</li>
 *   <li>Public getter/setter methods (getX, setX, isX)</li>
 *   <li>Public fields</li>
 *   <li>Private field access via setAccessible (with warning, unless strict mode)</li>
 * </ol>
 *
 * <p>Strict mode can be enabled to disable private field fallback:
 * {@code ReflectionUtils.setStrictMode(true)}
 */
public final class ReflectionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionUtils.class);

    private static final Map<String, MethodHandle> GETTER_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, MethodHandle> SETTER_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandles.Lookup> PRIVATE_LOOKUP_CACHE = new ConcurrentHashMap<>();

    /**
     * Strict mode flag. When true, disables private field fallback.
     */
    private static volatile boolean strictMode = false;

    /**
     * Track fields that have already logged warnings to avoid log spam.
     */
    private static final Map<String, Boolean> WARNED_FIELDS = new ConcurrentHashMap<>();

    private ReflectionUtils() {
        // Utility class
    }

    /**
     * Get a private lookup for the given class.
     * Uses privateLookupIn for better access in Java 9+ module system.
     */
    private static MethodHandles.Lookup getPrivateLookup(Class<?> clazz) {
        return PRIVATE_LOOKUP_CACHE.computeIfAbsent(clazz, c -> {
            try {
                return MethodHandles.privateLookupIn(c, MethodHandles.lookup());
            } catch (IllegalAccessException e) {
                return MethodHandles.lookup();
            }
        });
    }

    /**
     * Enable or disable strict mode.
     * In strict mode, private field access is not allowed and will throw exceptions.
     *
     * @param strict true to enable strict mode
     */
    public static void setStrictMode(boolean strict) {
        strictMode = strict;
    }

    /**
     * Check if strict mode is enabled.
     *
     * @return true if strict mode is enabled
     */
    public static boolean isStrictMode() {
        return strictMode;
    }

    // ==================== GETTERS ====================

    /**
     * Get a field value from an entity using safe reflection.
     * Tries in order: record accessor, getter method, public field, private field (with warning).
     *
     * @param entity    the entity instance
     * @param fieldName the field name (can be snake_case or camelCase)
     * @return the field value, or null if not accessible
     */
    public static Object getFieldValue(Object entity, String fieldName) {
        if (Objects.isNull(entity) || Objects.isNull(fieldName)) {
            return null;
        }

        Class<?> clazz = entity.getClass();

        // Try with original field name
        Object value = getFieldValueInternal(entity, clazz, fieldName);
        if (Objects.nonNull(value)) {
            return value;
        }

        // Try converting snake_case to camelCase
        String camelCaseFieldName = Casey.toCamelCase(fieldName);
        if (!camelCaseFieldName.equals(fieldName)) {
            value = getFieldValueInternal(entity, clazz, camelCaseFieldName);
            if (Objects.nonNull(value)) {
                return value;
            }
        }

        // Try converting camelCase to snake_case
        String snakeCaseFieldName = Casey.toSnakeCase(fieldName);
        if (!snakeCaseFieldName.equals(fieldName)) {
            value = getFieldValueInternal(entity, clazz, snakeCaseFieldName);
        }

        return value;
    }

    private static Object getFieldValueInternal(Object entity, Class<?> clazz, String fieldName) {
        String cacheKey = clazz.getName() + "#get#" + fieldName;

        MethodHandle getter = GETTER_CACHE.computeIfAbsent(cacheKey, k -> findGetter(clazz, fieldName));

        if (Objects.isNull(getter)) {
            return null;
        }

        try {
            return getter.invoke(entity);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Find a getter for the field. Tries in order:
     * 1. Record component accessor
     * 2. Public getter (getX, isX)
     * 3. Public field
     * 4. Private field (with warning, unless strict mode)
     */
    private static MethodHandle findGetter(Class<?> clazz, String fieldName) {
        // 1. Try record component accessor
        if (clazz.isRecord()) {
            MethodHandle handle = findRecordAccessor(clazz, fieldName);
            if (Objects.nonNull(handle)) {
                return handle;
            }
        }

        // 2. Try public getter methods
        MethodHandle handle = findGetterMethod(clazz, fieldName);
        if (Objects.nonNull(handle)) {
            return handle;
        }

        // 3. Try public field
        handle = findPublicFieldGetter(clazz, fieldName);
        if (Objects.nonNull(handle)) {
            return handle;
        }

        // 4. Fallback to private field (with warning)
        return findPrivateFieldGetter(clazz, fieldName);
    }

    private static MethodHandle findRecordAccessor(Class<?> clazz, String fieldName) {
        try {
            for (RecordComponent component : clazz.getRecordComponents()) {
                if (component.getName().equals(fieldName)) {
                    Method accessor = component.getAccessor();
                    return getPrivateLookup(clazz).unreflect(accessor);
                }
            }
        } catch (Exception e) {
            // Not a record or accessor not found
        }
        return null;
    }

    private static MethodHandle findGetterMethod(Class<?> clazz, String fieldName) {
        String capitalizedName = capitalize(fieldName);

        // Try getX()
        try {
            Method getter = clazz.getMethod("get" + capitalizedName);
            return getPrivateLookup(clazz).unreflect(getter);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // Try isX() for booleans
        }

        // Try isX()
        try {
            Method getter = clazz.getMethod("is" + capitalizedName);
            return getPrivateLookup(clazz).unreflect(getter);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // No getter found
        }

        // Try direct method name (for record-style accessors on non-records)
        try {
            Method getter = clazz.getMethod(fieldName);
            if (getter.getParameterCount() == 0) {
                return getPrivateLookup(clazz).unreflect(getter);
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // No direct accessor
        }

        return null;
    }

    private static MethodHandle findPublicFieldGetter(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            return getPrivateLookup(clazz).unreflectGetter(field);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private static MethodHandle findPrivateFieldGetter(Class<?> clazz, String fieldName) {
        if (strictMode) {
            return null;
        }

        try {
            Field field = findDeclaredField(clazz, fieldName);
            if (Objects.isNull(field)) {
                return null;
            }

            logPrivateAccessWarning(clazz, fieldName, "getter");

            field.setAccessible(true);
            return getPrivateLookup(clazz).unreflectGetter(field);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== SETTERS ====================

    /**
     * Set a field value on an entity using safe reflection.
     * Tries in order: public setter method, public field, private field (with warning).
     *
     * @param entity    the entity instance
     * @param fieldName the field name (can be snake_case or camelCase)
     * @param value     the value to set
     * @return true if value was set successfully, false otherwise
     */
    public static boolean setFieldValue(Object entity, String fieldName, Object value) {
        if (Objects.isNull(entity) || Objects.isNull(fieldName)) {
            return false;
        }

        Class<?> clazz = entity.getClass();

        // Try with original field name
        if (setFieldValueInternal(entity, clazz, fieldName, value)) {
            return true;
        }

        // Try converting snake_case to camelCase
        String camelCaseFieldName = Casey.toCamelCase(fieldName);
        if (!camelCaseFieldName.equals(fieldName)) {
            if (setFieldValueInternal(entity, clazz, camelCaseFieldName, value)) {
                return true;
            }
        }

        // Try converting camelCase to snake_case
        String snakeCaseFieldName = Casey.toSnakeCase(fieldName);
        if (!snakeCaseFieldName.equals(fieldName)) {
            return setFieldValueInternal(entity, clazz, snakeCaseFieldName, value);
        }

        return false;
    }

    private static boolean setFieldValueInternal(Object entity, Class<?> clazz, String fieldName, Object value) {
        String cacheKey = clazz.getName() + "#set#" + fieldName + "#" + (Objects.isNull(value) ? "null" : value.getClass().getName());

        MethodHandle setter = SETTER_CACHE.computeIfAbsent(cacheKey, k -> findSetter(clazz, fieldName, value));

        if (Objects.isNull(setter)) {
            return false;
        }

        try {
            Object convertedValue = convertValueIfNeeded(clazz, fieldName, value);
            setter.invoke(entity, convertedValue);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Convert value if type conversion is needed (UUID↔String, cross-classloader).
     */
    private static Object convertValueIfNeeded(Class<?> clazz, String fieldName, Object value) {
        if (Objects.isNull(value)) {
            return null;
        }

        Field field = findField(clazz, fieldName);
        if (Objects.isNull(field)) {
            return value;
        }

        Class<?> fieldType = field.getType();
        Class<?> valueType = value.getClass();

        // String → UUID
        if (fieldType == UUID.class && valueType == String.class) {
            return UUID.fromString((String) value);
        }
        // UUID → String
        if (fieldType == String.class && valueType == UUID.class) {
            return value.toString();
        }

        // Cross-classloader conversion: same class name but different Class objects
        if (isSameClassDifferentLoader(fieldType, valueType)) {
            return convertCrossClassloader(value, fieldType);
        }

        return value;
    }

    /**
     * Convert an object from one classloader to the expected type from another classloader.
     * Creates a new instance of the target type and copies all field values.
     * Handles enums by matching by name.
     */
    private static Object convertCrossClassloader(Object source, Class<?> targetType) {
        try {
            // Handle enums: find matching constant by name
            if (targetType.isEnum() && source.getClass().isEnum()) {
                return findEnumByName(targetType, ((Enum<?>) source).name());
            }

            Object target = targetType.getDeclaredConstructor().newInstance();

            // Copy all fields from source to target
            for (Field sourceField : getAllFields(source.getClass())) {
                String fieldName = sourceField.getName();
                Object fieldValue = getFieldValue(source, fieldName);
                if (Objects.nonNull(fieldValue)) {
                    setFieldValueDirect(target, fieldName, fieldValue);
                }
            }

            return target;
        } catch (Exception e) {
            LOGGER.warn("Suprim: Failed to convert cross-classloader object {} to {}: {}",
                    source.getClass().getName(), targetType.getName(), e.getMessage());
            // Return original value - let the caller handle any ClassCastException
            return source;
        }
    }

    /**
     * Find enum constant by name from target enum class.
     */
    private static Object findEnumByName(Class<?> enumType, String name) {
        Object[] constants = enumType.getEnumConstants();
        if (Objects.isNull(constants)) {
            return null;
        }
        for (Object constant : constants) {
            if (((Enum<?>) constant).name().equals(name)) {
                return constant;
            }
        }
        return null;
    }

    /**
     * Get all fields from a class including inherited fields.
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (Objects.nonNull(current) && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                // Skip synthetic and static fields
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Set field value directly using Field.set() - bypasses type checking.
     * Used for cross-classloader field copying where types have same name.
     */
    private static void setFieldValueDirect(Object target, String fieldName, Object value) {
        try {
            Field field = findDeclaredField(target.getClass(), fieldName);
            if (Objects.nonNull(field)) {
                field.setAccessible(true);
                // For nested objects with same-class-different-loader, recurse
                Class<?> fieldType = field.getType();
                Class<?> valueType = value.getClass();
                if (isSameClassDifferentLoader(fieldType, valueType)) {
                    value = convertCrossClassloader(value, fieldType);
                }
                field.set(target, value);
            }
        } catch (Exception e) {
            // Silently skip - best effort copy
            LOGGER.debug("Suprim: Could not copy field {} to target: {}", fieldName, e.getMessage());
        }
    }

    /**
     * Find a setter for the field. Tries in order:
     * 1. Public setter (setX)
     * 2. Public field
     * 3. Private field (with warning, unless strict mode)
     */
    private static MethodHandle findSetter(Class<?> clazz, String fieldName, Object value) {
        // 1. Try public setter method
        MethodHandle handle = findSetterMethod(clazz, fieldName, value);
        if (Objects.nonNull(handle)) {
            return handle;
        }

        // 2. Try public field
        handle = findPublicFieldSetter(clazz, fieldName);
        if (Objects.nonNull(handle)) {
            return handle;
        }

        // 3. Fallback to private field (with warning)
        return findPrivateFieldSetter(clazz, fieldName);
    }

    private static MethodHandle findSetterMethod(Class<?> clazz, String fieldName, Object value) {
        String capitalizedName = capitalize(fieldName);
        String setterName = "set" + capitalizedName;

        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                Class<?> valueType = Objects.isNull(value) ? null : value.getClass();

                // Check type compatibility: null, assignable, boxed, numeric widening, UUID/String, same-class-different-loader
                boolean typeMatch = Objects.isNull(value) ||
                    paramType.isAssignableFrom(valueType) ||
                    isBoxedMatch(paramType, valueType) ||
                    isNumericWideningMatch(paramType, valueType) ||
                    isUuidStringMatch(paramType, valueType) ||
                    isSameClassDifferentLoader(paramType, valueType);

                if (!typeMatch) {
                    // Throw error on type mismatch - strict mode
                    throw MappingException.builder()
                        .message("Type mismatch for " + clazz.getSimpleName() + "." + setterName +
                            " - expected: " + paramType.getName() + ", got: " + valueType.getName())
                        .build();
                }

                try {
                    return getPrivateLookup(clazz).unreflect(method);
                } catch (IllegalAccessException e) {
                    LOGGER.debug("Suprim: privateLookup failed for {}.{}: {}", clazz.getSimpleName(), setterName, e.getMessage());
                    try {
                        return MethodHandles.publicLookup().unreflect(method);
                    } catch (IllegalAccessException ex) {
                        LOGGER.debug("Suprim: publicLookup also failed for {}.{}: {}", clazz.getSimpleName(), setterName, ex.getMessage());
                    }
                }
            }
        }
        return null;
    }

    private static MethodHandle findPublicFieldSetter(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            return getPrivateLookup(clazz).unreflectSetter(field);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private static MethodHandle findPrivateFieldSetter(Class<?> clazz, String fieldName) {
        if (strictMode) {
            return null;
        }

        try {
            Field field = findDeclaredField(clazz, fieldName);
            if (Objects.isNull(field)) {
                return null;
            }

            logPrivateAccessWarning(clazz, fieldName, "setter");

            field.setAccessible(true);

            // Try MethodHandle first
            try {
                return getPrivateLookup(clazz).unreflectSetter(field);
            } catch (IllegalAccessException e) {
                // Fallback: wrap Field.set() for cross-module access in Java 9+
                return createFieldSetter(field);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Create a MethodHandle wrapper around Field.set() for cross-module access.
     * Works across module boundaries where unreflectSetter() fails.
     */
    private static MethodHandle createFieldSetter(Field field) {
        try {
            MethodHandle setter = MethodHandles.lookup().findVirtual(
                Field.class, "set",
                MethodType.methodType(void.class, Object.class, Object.class)
            );
            return setter.bindTo(field);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== FIELD LOOKUP ====================

    /**
     * Find a field on a class by name, checking cache first.
     * Searches declared fields and superclass.
     * Note: This returns the Field but does NOT make it accessible.
     *
     * @param clazz     the class to search
     * @param fieldName the field name to find
     * @return the Field, or null if not found
     */
    public static Field findField(Class<?> clazz, String fieldName) {
        if (Objects.isNull(clazz) || Objects.isNull(fieldName)) {
            return null;
        }

        String cacheKey = clazz.getName() + "#field#" + fieldName;
        return FIELD_CACHE.computeIfAbsent(cacheKey, k -> findDeclaredField(clazz, fieldName));
    }

    private static Field findDeclaredField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superclass = clazz.getSuperclass();
            if (Objects.nonNull(superclass) && superclass != Object.class) {
                return findDeclaredField(superclass, fieldName);
            }
            return null;
        }
    }

    // ==================== UTILITIES ====================

    private static void logPrivateAccessWarning(Class<?> clazz, String fieldName, String accessType) {
        String warnKey = clazz.getName() + "#" + fieldName + "#" + accessType;
        if (Objects.isNull(WARNED_FIELDS.putIfAbsent(warnKey, Boolean.TRUE))) {
            LOGGER.warn("Suprim: Using private field access for {}.{} ({}). " +
                            "Consider adding a public {} method for better compatibility. " +
                            "Enable strict mode with ReflectionUtils.setStrictMode(true) to disallow this.",
                    clazz.getSimpleName(), fieldName, accessType, accessType);
        }
    }

    private static boolean isBoxedMatch(Class<?> paramType, Class<?> valueType) {
        if (paramType.isPrimitive()) {
            return (paramType == int.class && valueType == Integer.class) ||
                    (paramType == long.class && valueType == Long.class) ||
                    (paramType == double.class && valueType == Double.class) ||
                    (paramType == float.class && valueType == Float.class) ||
                    (paramType == boolean.class && valueType == Boolean.class) ||
                    (paramType == short.class && valueType == Short.class) ||
                    (paramType == byte.class && valueType == Byte.class) ||
                    (paramType == char.class && valueType == Character.class);
        }
        return false;
    }

    /**
     * Check if value type can be widened to param type (numeric widening).
     * Allows: byte→short→int→long, float→double, int→long (common DB driver variance)
     */
    private static boolean isNumericWideningMatch(Class<?> paramType, Class<?> valueType) {
        // Long param accepts Integer (common: DB returns Integer, field is Long)
        if ((paramType == Long.class || paramType == long.class) &&
            (valueType == Integer.class || valueType == int.class)) {
            return true;
        }
        // Double param accepts Float
        if ((paramType == Double.class || paramType == double.class) &&
            (valueType == Float.class || valueType == float.class)) {
            return true;
        }
        // Integer param accepts Short/Byte
        if ((paramType == Integer.class || paramType == int.class) &&
            (valueType == Short.class || valueType == short.class ||
             valueType == Byte.class || valueType == byte.class)) {
            return true;
        }
        // Long param accepts Short/Byte
        if ((paramType == Long.class || paramType == long.class) &&
            (valueType == Short.class || valueType == short.class ||
             valueType == Byte.class || valueType == byte.class)) {
            return true;
        }
        return false;
    }

    /**
     * Check if types are UUID ↔ String (bidirectional conversion allowed).
     */
    private static boolean isUuidStringMatch(Class<?> paramType, Class<?> valueType) {
        return (paramType == UUID.class && valueType == String.class) ||
               (paramType == String.class && valueType == UUID.class);
    }

    /**
     * Check if classes have the same name but were loaded by different classloaders.
     * This happens with Spring DevTools, hot-reload, or multi-module applications.
     */
    private static boolean isSameClassDifferentLoader(Class<?> paramType, Class<?> valueType) {
        if (paramType == valueType) {
            return false; // Same class object, already handled by isAssignableFrom
        }
        return paramType.getName().equals(valueType.getName());
    }

    private static String capitalize(String str) {
        if (Objects.isNull(str) || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Clear all caches. Useful for testing.
     */
    public static void clearCaches() {
        GETTER_CACHE.clear();
        SETTER_CACHE.clear();
        FIELD_CACHE.clear();
        WARNED_FIELDS.clear();
        PRIVATE_LOOKUP_CACHE.clear();
    }
}
