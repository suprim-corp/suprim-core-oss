package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.casey.Casey;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.Objects;
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
            setter.invoke(entity, value);
            return true;
        } catch (Throwable e) {
            return false;
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
                boolean typeMatch = Objects.isNull(value) || paramType.isAssignableFrom(valueType) || isBoxedMatch(paramType, valueType);

                if (!typeMatch) {
                    LOGGER.debug("Suprim: Type mismatch for {}.{} - paramType={}, valueType={}",
                        clazz.getSimpleName(), setterName, paramType.getName(),
                        valueType.getName()
                    );
                    continue;
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
            return getPrivateLookup(clazz).unreflectSetter(field);
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
