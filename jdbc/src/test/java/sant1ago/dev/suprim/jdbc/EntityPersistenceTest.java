package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.IdGenerator;
import sant1ago.dev.suprim.core.util.UUIDUtils;
import sant1ago.dev.suprim.jdbc.exception.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EntityPersistence and ID generation strategies.
 */
@DisplayName("Entity Persistence Tests")
class EntityPersistenceTest {

    // ==================== TEST ENTITIES ====================

    @Entity(table = "users_uuid")
    static class UserWithUuidV7 {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "email")
        private String email;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @Entity(table = "users_uuid4")
    static class UserWithUuidV4 {
        @Id(strategy = GenerationType.UUID_V4)
        @Column(name = "id")
        private String id;

        @Column(name = "email")
        private String email;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @Entity(table = "users_identity")
    static class UserWithIdentity {
        @Id(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private Long id;

        @Column(name = "email")
        private String email;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @Entity(table = "users_manual")
    static class UserWithManualId {
        @Id
        @Column(name = "id")
        private Long id;

        @Column(name = "email")
        private String email;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    static class CountingGenerator implements IdGenerator<String> {
        private static int counter = 0;

        @Override
        public String generate() {
            return "custom-" + (++counter);
        }
    }

    @Entity(table = "users_custom")
    static class UserWithCustomGenerator {
        @Id(generator = CountingGenerator.class)
        @Column(name = "id")
        private String id;

        @Column(name = "email")
        private String email;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // ==================== ID META TESTS ====================

    @Nested
    @DisplayName("IdMeta Extraction")
    class IdMetaTests {

        @Test
        @DisplayName("Extract UUID_V7 strategy")
        void testUuidV7Strategy() {
            EntityReflector.IdMeta meta = EntityReflector.getIdMeta(UserWithUuidV7.class);

            assertEquals("id", meta.fieldName());
            assertEquals("id", meta.columnName());
            assertEquals(String.class, meta.fieldType());
            assertEquals(GenerationType.UUID_V7, meta.strategy());
            assertTrue(meta.isApplicationGenerated());
            assertFalse(meta.isDatabaseGenerated());
            assertFalse(meta.isManual());
        }

        @Test
        @DisplayName("Extract UUID strategy")
        void testUuidStrategy() {
            EntityReflector.IdMeta meta = EntityReflector.getIdMeta(UserWithUuidV4.class);

            assertEquals(GenerationType.UUID_V4, meta.strategy());
            assertTrue(meta.isApplicationGenerated());
        }

        @Test
        @DisplayName("Extract IDENTITY strategy")
        void testIdentityStrategy() {
            EntityReflector.IdMeta meta = EntityReflector.getIdMeta(UserWithIdentity.class);

            assertEquals(GenerationType.IDENTITY, meta.strategy());
            assertFalse(meta.isApplicationGenerated());
            assertTrue(meta.isDatabaseGenerated());
        }

        @Test
        @DisplayName("Extract NONE (manual) strategy")
        void testManualStrategy() {
            EntityReflector.IdMeta meta = EntityReflector.getIdMeta(UserWithManualId.class);

            assertEquals(GenerationType.NONE, meta.strategy());
            assertFalse(meta.isApplicationGenerated());
            assertFalse(meta.isDatabaseGenerated());
            assertTrue(meta.isManual());
        }

        @Test
        @DisplayName("Extract custom generator")
        void testCustomGenerator() {
            EntityReflector.IdMeta meta = EntityReflector.getIdMeta(UserWithCustomGenerator.class);

            assertTrue(meta.hasCustomGenerator());
            assertTrue(meta.isApplicationGenerated());
            assertEquals(CountingGenerator.class, meta.generatorClass());
        }
    }

    // ==================== UUID GENERATION TESTS ====================

    @Nested
    @DisplayName("UUID Generation")
    class UuidGenerationTests {

        @Test
        @DisplayName("UUID v7 format is valid")
        void testUuidV7Format() {
            String uuid = UUIDUtils.v7().toString();

            assertNotNull(uuid);
            assertEquals(36, uuid.length());  // UUID format: 8-4-4-4-12
            assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        }

        @Test
        @DisplayName("UUID v7 is time-ordered")
        void testUuidV7IsTimeOrdered() {
            String uuid1 = UUIDUtils.v7().toString();
            String uuid2 = UUIDUtils.v7().toString();
            String uuid3 = UUIDUtils.v7().toString();

            // UUID v7 should be naturally sortable by creation time
            assertTrue(uuid1.compareTo(uuid2) <= 0);
            assertTrue(uuid2.compareTo(uuid3) <= 0);
        }

        @Test
        @DisplayName("UUID v4 format is valid")
        void testUuidV4Format() {
            String uuid = UUIDUtils.v4().toString();

            assertNotNull(uuid);
            assertEquals(36, uuid.length());
            assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        }

        @Test
        @DisplayName("UUID v4 is random (unique)")
        void testUuidV4IsUnique() {
            String uuid1 = UUIDUtils.v4().toString();
            String uuid2 = UUIDUtils.v4().toString();

            assertNotEquals(uuid1, uuid2);
        }
    }

    // ==================== ENTITY REFLECTOR TESTS ====================

    @Nested
    @DisplayName("EntityReflector ID Operations")
    class EntityReflectorTests {

        @Test
        @DisplayName("getIdOrNull returns null when not set")
        void testGetIdOrNullReturnsNull() {
            UserWithUuidV7 user = new UserWithUuidV7();

            Object id = EntityReflector.getIdOrNull(user);

            assertNull(id);
        }

        @Test
        @DisplayName("getIdOrNull returns value when set")
        void testGetIdOrNullReturnsValue() {
            UserWithUuidV7 user = new UserWithUuidV7();
            user.setId("test-id-123");

            Object id = EntityReflector.getIdOrNull(user);

            assertEquals("test-id-123", id);
        }

        @Test
        @DisplayName("setId sets the ID value")
        void testSetId() {
            UserWithUuidV7 user = new UserWithUuidV7();

            EntityReflector.setId(user, "new-id-456");

            assertEquals("new-id-456", user.getId());
        }
    }

    // ==================== PERSISTENCE EXCEPTION TESTS ====================

    @Nested
    @DisplayName("PersistenceException")
    class PersistenceExceptionTests {

        @Test
        @DisplayName("Exception contains entity class")
        void testExceptionContainsEntityClass() {
            PersistenceException ex = new PersistenceException(
                "Test error",
                UserWithUuidV7.class
            );

            assertEquals(UserWithUuidV7.class, ex.getEntityClass());
            assertTrue(ex.getMessage().contains("Test error"));
        }

        @Test
        @DisplayName("Exception with cause")
        void testExceptionWithCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            PersistenceException ex = new PersistenceException(
                "Wrapper error",
                UserWithManualId.class,
                cause
            );

            assertEquals(cause, ex.getCause());
        }
    }
}
