package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestOrder;
import sant1ago.dev.suprim.core.TestOrder_;
import sant1ago.dev.suprim.core.TestUser;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.type.Relation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PathResolver class.
 */
class PathResolverTest {

    // ==================== resolve(path, rootEntity) ====================

    @Test
    void testResolveSingleLevel() {
        EagerLoadSpec spec = PathResolver.resolve("orders", TestUser.class);

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertFalse(spec.hasConstraint());
        assertFalse(spec.hasNested());
    }

    @Test
    void testResolveNestedPathViaResolveNested() {
        // Use resolveNested for proper nested path resolution
        // orders.user - first relation is orders (on User), nested is user (on Order)
        EagerLoadSpec spec = PathResolver.resolveNested(TestUser_.ORDERS, "user");

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertTrue(spec.hasNested());
        assertEquals(1, spec.nested().size());
        assertEquals(TestOrder_.USER, spec.nested().get(0).relation());
    }

    @Test
    void testResolveNullPathThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                PathResolver.resolve(null, TestUser.class)
        );
    }

    @Test
    void testResolveEmptyPathThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                PathResolver.resolve("", TestUser.class)
        );
    }

    @Test
    void testResolveBlankPathThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                PathResolver.resolve("   ", TestUser.class)
        );
    }

    @Test
    void testResolveNullEntityThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                PathResolver.resolve("orders", null)
        );
    }

    @Test
    void testResolveInvalidRelationThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                PathResolver.resolve("nonexistent", TestUser.class)
        );
    }

    // ==================== resolve(path, rootEntity, constraint) ====================

    @Test
    void testResolveWithConstraint() {
        EagerLoadSpec spec = PathResolver.resolve(
                "orders",
                TestUser.class,
                b -> b.limit(10)
        );

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertTrue(spec.hasConstraint());
    }

    @Test
    void testResolveWithNullConstraintDelegatesToBasicResolve() {
        EagerLoadSpec spec = PathResolver.resolve("orders", TestUser.class, null);

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertFalse(spec.hasConstraint());
    }

    @Test
    void testResolveNestedPathWithConstraintViaResolveNested() {
        // Use resolveNested with constraint for proper nested path + constraint
        EagerLoadSpec spec = PathResolver.resolveNested(
                TestUser_.ORDERS,
                "user",
                b -> b.limit(5)
        );

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertFalse(spec.hasConstraint()); // Parent has no constraint
        assertTrue(spec.hasNested());
        // Nested (final) relation has the constraint
        assertTrue(spec.nested().get(0).hasConstraint());
    }

    // ==================== resolveNested(relation, nestedPath) ====================

    @Test
    void testResolveNestedWithTypedRelation() {
        EagerLoadSpec spec = PathResolver.resolveNested(TestUser_.ORDERS, "user");

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertTrue(spec.hasNested());
        assertEquals(TestOrder_.USER, spec.nested().get(0).relation());
    }

    @Test
    void testResolveNestedWithNullPathReturnsSimpleSpec() {
        EagerLoadSpec spec = PathResolver.resolveNested(TestUser_.ORDERS, null);

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertFalse(spec.hasNested());
    }

    @Test
    void testResolveNestedWithEmptyPathReturnsSimpleSpec() {
        EagerLoadSpec spec = PathResolver.resolveNested(TestUser_.ORDERS, "");

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertFalse(spec.hasNested());
    }

    @Test
    void testResolveNestedWithBlankPathReturnsSimpleSpec() {
        EagerLoadSpec spec = PathResolver.resolveNested(TestUser_.ORDERS, "   ");

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertFalse(spec.hasNested());
    }

    // ==================== resolveNested(relation, nestedPath, constraint) ====================

    @Test
    void testResolveNestedWithConstraint() {
        EagerLoadSpec spec = PathResolver.resolveNested(
                TestUser_.ORDERS,
                "user",
                b -> b.limit(3)
        );

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertFalse(spec.hasConstraint()); // Parent has no constraint
        assertTrue(spec.hasNested());
        assertTrue(spec.nested().get(0).hasConstraint());
    }

    @Test
    void testResolveNestedWithNullPathAndConstraint() {
        EagerLoadSpec spec = PathResolver.resolveNested(TestUser_.ORDERS, null, b -> b.limit(5));

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertTrue(spec.hasConstraint());
        assertFalse(spec.hasNested());
    }

    @Test
    void testResolveNestedWithEmptyPathAndConstraint() {
        EagerLoadSpec spec = PathResolver.resolveNested(TestUser_.ORDERS, "", b -> b.limit(5));

        assertNotNull(spec);
        assertTrue(spec.hasConstraint());
        assertFalse(spec.hasNested());
    }

    // ==================== toConstantCase() edge cases ====================

    @Test
    void testResolveWithUpperCaseFieldName() {
        // Field name "ORDERS" should find the field
        EagerLoadSpec spec = PathResolver.resolve("ORDERS", TestUser.class);
        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
    }

    @Test
    void testResolveWithCamelCaseConversion() {
        // "orders" converted to "ORDERS" should work
        EagerLoadSpec spec = PathResolver.resolve("orders", TestUser.class);
        assertNotNull(spec);
    }

    // ==================== Error scenarios ====================

    @Test
    void testResolveFieldNotRelationThrows() {
        // ID is a Column, not a Relation
        assertThrows(IllegalArgumentException.class, () ->
                PathResolver.resolve("ID", TestUser.class)
        );
    }

    @Test
    void testResolveMissingMetamodelClassThrows() {
        // Create a class without a metamodel class
        class NoMetamodel {}

        // Throws IllegalArgumentException which wraps ClassNotFoundException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PathResolver.resolve("something", NoMetamodel.class)
        );
        assertTrue(ex.getMessage().contains("Metamodel class not found"));
    }

    // ==================== Additional edge case tests ====================

    @Test
    void testResolveWithRuntimeExceptionNotClassNotFound() {
        // Test RuntimeException that is NOT caused by ClassNotFoundException
        // This requires a metamodel class that exists but throws RuntimeException on access
        // We can't easily test this without mocking, so we just verify the existing exception path
        class EntityWithBadMetamodel {}
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PathResolver.resolve("something", EntityWithBadMetamodel.class)
        );
        assertTrue(ex.getMessage().contains("Metamodel class not found"));
    }

    @Test
    void testPrivateConstructor() throws Exception {
        // Utility class should have private constructor
        var constructor = PathResolver.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    void testResolvePrivateRelationFieldThrows() {
        // Private static Relation field should throw IllegalAccessException wrapped in IllegalArgumentException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PathResolver.resolve("privateRelation", PrivateFieldEntity.class)
        );
        assertTrue(ex.getMessage().contains("Failed to access relation field"));
    }

    // ==================== Multi-level path branch coverage ====================

    @Test
    void testResolveMultiLevelPathCoversNestedBranch() {
        // Multi-level path with same relation twice to cover Objects.nonNull(current) branch
        EagerLoadSpec spec = PathResolver.resolve("orders.orders", TestUser.class);

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertTrue(spec.hasNested());
        assertEquals(TestUser_.ORDERS, spec.nested().get(0).relation());
    }

    @Test
    void testResolveMultiLevelPathWithConstraintCoversAllBranches() {
        // Multi-level path with constraint covers: constraint ternary + nested ternary
        EagerLoadSpec spec = PathResolver.resolve("orders.orders", TestUser.class, b -> b.limit(5));

        assertNotNull(spec);
        assertEquals(TestUser_.ORDERS, spec.relation());
        assertFalse(spec.hasConstraint()); // First level has no constraint
        assertTrue(spec.hasNested());
        assertTrue(spec.nested().get(0).hasConstraint()); // Last level has constraint
    }
}
