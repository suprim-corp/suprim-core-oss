package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MorphTypeRegistry.
 */
class MorphTypeRegistryTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        MorphTypeRegistry.clear();
    }

    @Test
    void testRegisterAndResolve() {
        MorphTypeRegistry.register("User", String.class);

        Class<?> resolved = MorphTypeRegistry.resolve("User");
        assertEquals(String.class, resolved);
    }

    @Test
    void testResolveFullyQualifiedClassName() {
        // Test resolving by fully qualified class name (not registered alias)
        Class<?> resolved = MorphTypeRegistry.resolve("java.lang.Integer");
        assertEquals(Integer.class, resolved);
    }

    @Test
    void testResolveUnknownTypeThrows() {
        // Test ClassNotFoundException branch
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MorphTypeRegistry.resolve("NonExistentClass"));
        assertTrue(ex.getMessage().contains("Unknown morph type"));
    }

    @Test
    void testGetTypeAliasRegistered() {
        MorphTypeRegistry.register("Post", Long.class);

        String alias = MorphTypeRegistry.getTypeAlias(Long.class);
        assertEquals("Post", alias);
    }

    @Test
    void testGetTypeAliasNotRegistered() {
        // Returns simple class name if not registered
        String alias = MorphTypeRegistry.getTypeAlias(Double.class);
        assertEquals("Double", alias);
    }

    @Test
    void testIsRegistered() {
        assertFalse(MorphTypeRegistry.isRegistered("Comment"));

        MorphTypeRegistry.register("Comment", Float.class);
        assertTrue(MorphTypeRegistry.isRegistered("Comment"));
    }

    @Test
    void testClear() {
        MorphTypeRegistry.register("Test", Boolean.class);
        assertTrue(MorphTypeRegistry.isRegistered("Test"));

        MorphTypeRegistry.clear();
        assertFalse(MorphTypeRegistry.isRegistered("Test"));
    }

    @Test
    void testGetRegisteredTypes() {
        MorphTypeRegistry.register("A", String.class);
        MorphTypeRegistry.register("B", Integer.class);

        Map<String, Class<?>> types = MorphTypeRegistry.getRegisteredTypes();
        assertEquals(2, types.size());
        assertEquals(String.class, types.get("A"));
        assertEquals(Integer.class, types.get("B"));
    }

    @Test
    void testConstructorCoverage() throws Exception {
        // Cover the default constructor via reflection
        Constructor<MorphTypeRegistry> ctor = MorphTypeRegistry.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        MorphTypeRegistry instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
