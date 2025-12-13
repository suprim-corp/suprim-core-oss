package sant1ago.dev.suprim.core.util;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.IdGenerator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IdMetadata and IdMetadata.Info.
 */
class IdMetadataTest {

    // ==================== Test Entities ====================

    static class EntityWithUuidV4 {
        @Id(strategy = GenerationType.UUID_V4)
        private UUID id;
    }

    static class EntityWithUuidV7 {
        @Id(strategy = GenerationType.UUID_V7)
        private UUID id;
    }

    static class EntityWithStringUuidV7 {
        @Id(strategy = GenerationType.UUID_V7)
        private String id;
    }

    static class EntityWithIdentity {
        @Id(strategy = GenerationType.IDENTITY)
        private Long id;
    }

    static class EntityWithSequence {
        @Id(strategy = GenerationType.SEQUENCE)
        private Long id;
    }

    static class EntityWithManualId {
        @Id(strategy = GenerationType.NONE)
        private String id;
    }

    static class EntityWithCustomColumn {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "custom_id_column")
        private UUID id;
    }

    static class EntityWithoutId {
        private String name;
    }

    static class CustomIdGenerator implements IdGenerator<String> {
        @Override
        public String generate() {
            return "custom-" + System.currentTimeMillis();
        }
    }

    static class EntityWithCustomGenerator {
        @Id(generator = CustomIdGenerator.class)
        private String id;
    }

    static class ParentEntity {
        @Id(strategy = GenerationType.UUID_V7)
        private UUID id;
    }

    static class ChildEntity extends ParentEntity {
        private String name;
    }

    static class EntityWithEmptyColumnName {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "")
        private UUID myCustomId;
    }

    static class BrokenIdGenerator implements IdGenerator<String> {
        public BrokenIdGenerator(String requiredArg) {
            // No default constructor - will fail instantiation
        }
        @Override
        public String generate() {
            return "broken";
        }
    }

    static class EntityWithBrokenGenerator {
        @Id(generator = BrokenIdGenerator.class)
        private String id;
    }

    // ==================== Info Record Tests ====================

    @Test
    void testInfoIsApplicationGeneratedForUuidV4() {
        IdMetadata.Info info = IdMetadata.get(EntityWithUuidV4.class);
        assertTrue(info.isApplicationGenerated());
        assertFalse(info.isDatabaseGenerated());
        assertFalse(info.isManual());
    }

    @Test
    void testInfoIsApplicationGeneratedForUuidV7() {
        IdMetadata.Info info = IdMetadata.get(EntityWithUuidV7.class);
        assertTrue(info.isApplicationGenerated());
        assertFalse(info.isDatabaseGenerated());
        assertFalse(info.isManual());
    }

    @Test
    void testInfoIsDatabaseGeneratedForIdentity() {
        IdMetadata.Info info = IdMetadata.get(EntityWithIdentity.class);
        assertFalse(info.isApplicationGenerated());
        assertTrue(info.isDatabaseGenerated());
        assertFalse(info.isManual());
    }

    @Test
    void testInfoIsDatabaseGeneratedForSequence() {
        IdMetadata.Info info = IdMetadata.get(EntityWithSequence.class);
        assertFalse(info.isApplicationGenerated());
        assertTrue(info.isDatabaseGenerated());
        assertFalse(info.isManual());
    }

    @Test
    void testInfoIsManualForNone() {
        IdMetadata.Info info = IdMetadata.get(EntityWithManualId.class);
        assertFalse(info.isApplicationGenerated());
        assertFalse(info.isDatabaseGenerated());
        assertTrue(info.isManual());
    }

    @Test
    void testInfoHasCustomGenerator() {
        IdMetadata.Info info = IdMetadata.get(EntityWithCustomGenerator.class);
        assertTrue(info.hasCustomGenerator());
        assertTrue(info.isApplicationGenerated());
        assertFalse(info.isManual());
    }

    @Test
    void testInfoNoCustomGenerator() {
        IdMetadata.Info info = IdMetadata.get(EntityWithUuidV7.class);
        assertFalse(info.hasCustomGenerator());
    }

    // ==================== Metadata Extraction Tests ====================

    @Test
    void testGetReturnsNullForEntityWithoutId() {
        IdMetadata.Info info = IdMetadata.get(EntityWithoutId.class);
        assertNull(info);
    }

    @Test
    void testGetExtractsFieldName() {
        IdMetadata.Info info = IdMetadata.get(EntityWithUuidV7.class);
        assertEquals("id", info.fieldName());
    }

    @Test
    void testGetExtractsDefaultColumnName() {
        IdMetadata.Info info = IdMetadata.get(EntityWithUuidV7.class);
        assertEquals("id", info.columnName());
    }

    @Test
    void testGetExtractsCustomColumnName() {
        IdMetadata.Info info = IdMetadata.get(EntityWithCustomColumn.class);
        assertEquals("custom_id_column", info.columnName());
    }

    @Test
    void testGetExtractsFieldType() {
        IdMetadata.Info info = IdMetadata.get(EntityWithUuidV7.class);
        assertEquals(UUID.class, info.fieldType());
    }

    @Test
    void testGetExtractsStrategy() {
        IdMetadata.Info info = IdMetadata.get(EntityWithUuidV7.class);
        assertEquals(GenerationType.UUID_V7, info.strategy());
    }

    @Test
    void testGetExtractsFromSuperclass() {
        IdMetadata.Info info = IdMetadata.get(ChildEntity.class);
        assertNotNull(info);
        assertEquals("id", info.fieldName());
        assertEquals(GenerationType.UUID_V7, info.strategy());
    }

    @Test
    void testGetCachesResult() {
        IdMetadata.Info info1 = IdMetadata.get(EntityWithUuidV7.class);
        IdMetadata.Info info2 = IdMetadata.get(EntityWithUuidV7.class);
        assertSame(info1, info2);
    }

    // ==================== ID Generation Tests ====================

    @Test
    void testGenerateIdWithUuidV4() {
        IdMetadata.Info info = IdMetadata.get(EntityWithUuidV4.class);
        Object id = IdMetadata.generateId(info);
        assertInstanceOf(UUID.class, id);
        assertEquals(4, ((UUID) id).version());
    }

    @Test
    void testGenerateIdWithUuidV7() {
        IdMetadata.Info info = IdMetadata.get(EntityWithUuidV7.class);
        Object id = IdMetadata.generateId(info);
        assertInstanceOf(UUID.class, id);
        assertEquals(7, ((UUID) id).version());
    }

    @Test
    void testGenerateIdWithStringFieldReturnsString() {
        IdMetadata.Info info = IdMetadata.get(EntityWithStringUuidV7.class);
        Object id = IdMetadata.generateId(info);
        assertInstanceOf(String.class, id);
    }

    @Test
    void testGenerateIdWithCustomGenerator() {
        IdMetadata.Info info = IdMetadata.get(EntityWithCustomGenerator.class);
        Object id = IdMetadata.generateId(info);
        assertInstanceOf(String.class, id);
        assertTrue(((String) id).startsWith("custom-"));
    }

    @Test
    void testGenerateIdThrowsForNonGeneratableStrategy() {
        IdMetadata.Info info = IdMetadata.get(EntityWithIdentity.class);
        assertThrows(IllegalStateException.class, () -> IdMetadata.generateId(info));
    }

    @Test
    void testGenerateIdThrowsForManualStrategy() {
        IdMetadata.Info info = IdMetadata.get(EntityWithManualId.class);
        assertThrows(IllegalStateException.class, () -> IdMetadata.generateId(info));
    }

    @Test
    void testGetUsesSnakeCaseForEmptyColumnName() {
        IdMetadata.Info info = IdMetadata.get(EntityWithEmptyColumnName.class);
        assertEquals("my_custom_id", info.columnName());
    }

    @Test
    void testGenerateIdThrowsForBrokenGenerator() {
        IdMetadata.Info info = IdMetadata.get(EntityWithBrokenGenerator.class);
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> IdMetadata.generateId(info)
        );
        assertTrue(ex.getMessage().contains("Cannot instantiate ID generator"));
    }

    @Test
    void testGetReturnsNullForInterface() {
        // Interface.getSuperclass() returns null, hitting the null branch
        IdMetadata.Info info = IdMetadata.get(Runnable.class);
        assertNull(info);
    }
}
