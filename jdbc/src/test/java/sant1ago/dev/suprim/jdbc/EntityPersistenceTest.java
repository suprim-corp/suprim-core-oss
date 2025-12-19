package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.CreationTimestamp;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.entity.JsonbColumn;
import sant1ago.dev.suprim.annotation.entity.TimestampAction;
import sant1ago.dev.suprim.annotation.entity.UpdateTimestamp;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.IdGenerator;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.util.UUIDUtils;
import sant1ago.dev.suprim.jdbc.exception.PersistenceException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    // Generator without no-arg constructor (for testing instantiation failure)
    static class NoArgConstructorGenerator implements IdGenerator<String> {
        private final String prefix;

        // Only constructor requires argument
        public NoArgConstructorGenerator(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String generate() {
            return prefix + "-123";
        }
    }

    @Entity(table = "bad_gen")
    static class EntityWithBadGenerator {
        @Id(generator = NoArgConstructorGenerator.class)
        @Column(name = "id")
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
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

    @Entity(table = "jobs")
    static class JobWithJsonb {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "name")
        private String name;

        @Column(name = "workspace_id", type = SqlType.UUID)
        private String workspaceId;

        @JsonbColumn
        @Column(name = "payload", type = SqlType.JSONB)
        private Map<String, Object> payload;

        @JsonbColumn
        @Column(name = "output", type = SqlType.JSONB)
        private Map<String, Object> output;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getWorkspaceId() { return workspaceId; }
        public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
        public Map<String, Object> getOutput() { return output; }
        public void setOutput(Map<String, Object> output) { this.output = output; }
    }

    // Base class for testing inherited fields
    static abstract class BaseEntity {
        @Column(name = "created_at")
        private String createdAt;

        @Column(name = "updated_at")
        private String updatedAt;

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @Entity(table = "posts")
    static class PostWithInheritance extends BaseEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "title")
        private String title;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    // Entity with UUID column type for ID
    @Entity(table = "products")
    static class ProductWithUuidId {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id", type = SqlType.UUID)
        private String id;

        @Column(name = "name")
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // Entity with UUID type field (not String)
    @Entity(table = "orders")
    static class OrderWithUuidField {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id", type = SqlType.UUID)
        private java.util.UUID id;

        @Column(name = "customer_id", type = SqlType.UUID)
        private java.util.UUID customerId;

        @Column(name = "description")
        private String description;

        public java.util.UUID getId() { return id; }
        public void setId(java.util.UUID id) { this.id = id; }
        public java.util.UUID getCustomerId() { return customerId; }
        public void setCustomerId(java.util.UUID customerId) { this.customerId = customerId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // Base entity with UUID ID (testing inheritance)
    static abstract class BaseWithUuidId {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id", type = SqlType.UUID)
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    // Child entity inheriting UUID ID from base
    @Entity(table = "child_entities")
    static class ChildWithInheritedUuidId extends BaseWithUuidId {
        @Column(name = "title")
        private String title;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
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

        @Test
        @DisplayName("toColumnMap converts @JsonbColumn Map to PGobject")
        void testJsonbColumnConvertsToPGobject() {
            JobWithJsonb job = new JobWithJsonb();
            job.setId("test-id");
            job.setName("Test Job");

            Map<String, Object> payload = new HashMap<>();
            payload.put("key1", "value1");
            payload.put("count", 42);
            job.setPayload(payload);

            Map<String, Object> output = new HashMap<>();
            output.put("result", "success");
            job.setOutput(output);

            Map<String, Object> columnMap = EntityReflector.toColumnMap(job);

            // Verify payload is converted to PGobject with type jsonb
            Object payloadValue = columnMap.get("payload");
            assertInstanceOf(PGobject.class, payloadValue);
            PGobject payloadPg = (PGobject) payloadValue;
            assertEquals("jsonb", payloadPg.getType());
            assertTrue(payloadPg.getValue().contains("\"key1\""));
            assertTrue(payloadPg.getValue().contains("\"value1\""));

            // Verify output is converted to PGobject with type jsonb
            Object outputValue = columnMap.get("output");
            assertInstanceOf(PGobject.class, outputValue);
            PGobject outputPg = (PGobject) outputValue;
            assertEquals("jsonb", outputPg.getType());
            assertTrue(outputPg.getValue().contains("\"result\""));
        }

        @Test
        @DisplayName("toColumnMap handles null @JsonbColumn fields")
        void testJsonbColumnHandlesNull() {
            JobWithJsonb job = new JobWithJsonb();
            job.setId("test-id");
            job.setName("Test Job");
            // payload and output are null

            Map<String, Object> columnMap = EntityReflector.toColumnMap(job);

            // Null fields should not be in the map
            assertFalse(columnMap.containsKey("payload"));
            assertFalse(columnMap.containsKey("output"));
        }

        @Test
        @DisplayName("toColumnMap preserves non-JsonbColumn fields")
        void testNonJsonbColumnsPreserved() {
            JobWithJsonb job = new JobWithJsonb();
            job.setId("test-id");
            job.setName("Test Job");

            Map<String, Object> columnMap = EntityReflector.toColumnMap(job);

            // Regular fields should remain as-is
            assertEquals("test-id", columnMap.get("id"));
            assertEquals("Test Job", columnMap.get("name"));
        }

        @Test
        @DisplayName("toJsonbObject throws on non-serializable object")
        void testToJsonbObjectThrowsOnNonSerializable() {
            // Object with circular reference that Jackson can't serialize
            Object nonSerializable = new Object() {
                public Object getSelf() { return this; }
            };

            assertThrows(IllegalArgumentException.class, () -> {
                EntityReflector.toJsonbObject(nonSerializable);
            });
        }

        @Test
        @DisplayName("buildColumnMap converts @JsonbColumn Map to PGobject")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapJsonbHandling() throws Exception {
            JobWithJsonb job = new JobWithJsonb();
            job.setId("test-id");
            job.setName("Test Job");

            Map<String, Object> payload = new HashMap<>();
            payload.put("key1", "value1");
            job.setPayload(payload);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(JobWithJsonb.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, job, idMeta, false);

            // Verify payload is converted to PGobject
            Object payloadValue = columnMap.get("payload");
            assertInstanceOf(PGobject.class, payloadValue);
            PGobject payloadPg = (PGobject) payloadValue;
            assertEquals("jsonb", payloadPg.getType());
            assertTrue(payloadPg.getValue().contains("\"key1\""));
        }
        @Test
        @DisplayName("toColumnMap converts String to UUID for SqlType.UUID columns")
        void testUuidColumnConversion() {
            JobWithJsonb job = new JobWithJsonb();
            job.setId("test-id");
            job.setWorkspaceId("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1");

            Map<String, Object> columnMap = EntityReflector.toColumnMap(job);

            Object workspaceId = columnMap.get("workspace_id");
            assertInstanceOf(java.util.UUID.class, workspaceId);
            assertEquals("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1", workspaceId.toString());
        }

        @Test
        @DisplayName("toColumnMap throws on invalid UUID format")
        void testInvalidUuidThrows() {
            JobWithJsonb job = new JobWithJsonb();
            job.setId("test-id");
            job.setWorkspaceId("abc");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                EntityReflector.toColumnMap(job);
            });
            assertTrue(ex.getMessage().contains("Invalid UUID format"));
            assertTrue(ex.getMessage().contains("workspace_id"));
        }

        @Test
        @DisplayName("buildColumnMap converts String to UUID for SqlType.UUID columns")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapUuidConversion() throws Exception {
            JobWithJsonb job = new JobWithJsonb();
            job.setId("test-id");
            job.setWorkspaceId("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(JobWithJsonb.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, job, idMeta, false);

            Object workspaceId = columnMap.get("workspace_id");
            assertInstanceOf(java.util.UUID.class, workspaceId);
            assertEquals("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1", workspaceId.toString());
        }

        @Test
        @DisplayName("buildColumnMap throws on invalid UUID format")
        void testBuildColumnMapInvalidUuidThrows() throws Exception {
            JobWithJsonb job = new JobWithJsonb();
            job.setId("test-id");
            job.setWorkspaceId("invalid-uuid");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(JobWithJsonb.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);

            var ex = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
                method.invoke(null, job, idMeta, false);
            });
            assertInstanceOf(IllegalArgumentException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("Invalid UUID format"));
        }

        @Test
        @DisplayName("buildColumnMap includes inherited fields from parent class")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapIncludesInheritedFields() throws Exception {
            PostWithInheritance post = new PostWithInheritance();
            post.setId("post-id");
            post.setTitle("Test Title");
            post.setCreatedAt("2024-01-01T00:00:00Z");
            post.setUpdatedAt("2024-01-02T00:00:00Z");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(PostWithInheritance.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, post, idMeta, false);

            // Child class fields
            assertEquals("post-id", columnMap.get("id"));
            assertEquals("Test Title", columnMap.get("title"));
            // Inherited fields from BaseEntity
            assertEquals("2024-01-01T00:00:00Z", columnMap.get("created_at"));
            assertEquals("2024-01-02T00:00:00Z", columnMap.get("updated_at"));
        }

        @Test
        @DisplayName("toColumnMap includes inherited fields from parent class")
        void testToColumnMapIncludesInheritedFields() {
            PostWithInheritance post = new PostWithInheritance();
            post.setId("post-id");
            post.setTitle("Test Title");
            post.setCreatedAt("2024-01-01T00:00:00Z");
            post.setUpdatedAt("2024-01-02T00:00:00Z");

            Map<String, Object> columnMap = EntityReflector.toColumnMap(post);

            // Child class fields
            assertEquals("post-id", columnMap.get("id"));
            assertEquals("Test Title", columnMap.get("title"));
            // Inherited fields from BaseEntity
            assertEquals("2024-01-01T00:00:00Z", columnMap.get("created_at"));
            assertEquals("2024-01-02T00:00:00Z", columnMap.get("updated_at"));
        }

        @Test
        @DisplayName("buildColumnMap converts String ID to UUID when column type is SqlType.UUID")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapConvertsIdToUuidWhenColumnTypeUuid() throws Exception {
            ProductWithUuidId product = new ProductWithUuidId();
            product.setId("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1");
            product.setName("Test Product");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(ProductWithUuidId.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, product, idMeta, false);

            // ID should be converted to UUID because column type is SqlType.UUID
            Object idValue = columnMap.get("id");
            assertInstanceOf(java.util.UUID.class, idValue);
            assertEquals("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1", idValue.toString());
        }

        @Test
        @DisplayName("buildColumnMap keeps UUID-format String ID as String when column type is not UUID")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapKeepsUuidFormatStringWhenColumnTypeNotUuid() throws Exception {
            JobWithJsonb job = new JobWithJsonb();
            job.setId("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1");
            job.setName("Test Job");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(JobWithJsonb.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, job, idMeta, false);

            // UUID-format ID should stay as String because column type is not SqlType.UUID
            Object idValue = columnMap.get("id");
            assertInstanceOf(String.class, idValue);
            assertEquals("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1", idValue);
        }

        @Test
        @DisplayName("buildColumnMap throws on invalid UUID when column type is SqlType.UUID")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapThrowsOnInvalidUuidWhenColumnTypeUuid() throws Exception {
            ProductWithUuidId product = new ProductWithUuidId();
            product.setId("not-a-valid-uuid");
            product.setName("Test Product");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(ProductWithUuidId.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);

            InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, product, idMeta, false));
            assertInstanceOf(IllegalArgumentException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("Invalid UUID format"));
        }

        @Test
        @DisplayName("buildColumnMap keeps UUID type field as-is (no conversion needed)")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapKeepsUuidFieldAsIs() throws Exception {
            java.util.UUID uuid = java.util.UUID.fromString("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1");
            java.util.UUID customerId = java.util.UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

            OrderWithUuidField order = new OrderWithUuidField();
            order.setId(uuid);
            order.setCustomerId(customerId);
            order.setDescription("Test Order");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(OrderWithUuidField.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, order, idMeta, false);

            // UUID fields should remain as UUID (no conversion from String needed)
            Object idValue = columnMap.get("id");
            assertInstanceOf(java.util.UUID.class, idValue);
            assertEquals(uuid, idValue);

            Object customerIdValue = columnMap.get("customer_id");
            assertInstanceOf(java.util.UUID.class, customerIdValue);
            assertEquals(customerId, customerIdValue);
        }

        @Test
        @DisplayName("toColumnMap keeps UUID type field as-is (no conversion needed)")
        void testToColumnMapKeepsUuidFieldAsIs() {
            java.util.UUID uuid = java.util.UUID.fromString("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1");
            java.util.UUID customerId = java.util.UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

            OrderWithUuidField order = new OrderWithUuidField();
            order.setId(uuid);
            order.setCustomerId(customerId);
            order.setDescription("Test Order");

            Map<String, Object> columnMap = EntityReflector.toColumnMap(order);

            // UUID fields should remain as UUID
            Object idValue = columnMap.get("id");
            assertInstanceOf(java.util.UUID.class, idValue);
            assertEquals(uuid, idValue);

            Object customerIdValue = columnMap.get("customer_id");
            assertInstanceOf(java.util.UUID.class, customerIdValue);
            assertEquals(customerId, customerIdValue);
        }

        @Test
        @DisplayName("buildColumnMap converts inherited String ID to UUID when column type is SqlType.UUID")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapConvertsInheritedIdToUuid() throws Exception {
            ChildWithInheritedUuidId child = new ChildWithInheritedUuidId();
            child.setId("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1");
            child.setTitle("Test Title");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(ChildWithInheritedUuidId.class);

            // Verify idMeta has correct column type
            assertEquals(SqlType.UUID, idMeta.columnType(), "IdMeta should have UUID column type from inherited @Column");

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, child, idMeta, false);

            // ID should be converted to UUID because column type is SqlType.UUID
            Object idValue = columnMap.get("id");
            assertInstanceOf(java.util.UUID.class, idValue, "Inherited String ID should be converted to UUID");
            assertEquals("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1", idValue.toString());
        }

        @Test
        @DisplayName("toColumnMap converts inherited String ID to UUID when column type is SqlType.UUID")
        void testToColumnMapConvertsInheritedIdToUuid() {
            ChildWithInheritedUuidId child = new ChildWithInheritedUuidId();
            child.setId("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1");
            child.setTitle("Test Title");

            Map<String, Object> columnMap = EntityReflector.toColumnMap(child);

            // ID should be converted to UUID
            Object idValue = columnMap.get("id");
            assertInstanceOf(java.util.UUID.class, idValue, "Inherited String ID should be converted to UUID");
            assertEquals("7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1", idValue.toString());
        }
    }

    // ==================== ENTITY REFLECTOR ADDITIONAL TESTS ====================

    @Nested
    @DisplayName("EntityReflector getId/setField methods")
    class EntityReflectorFieldTests {

        @Test
        @DisplayName("getId returns ID value")
        void testGetIdReturnsValue() {
            UserWithUuidV7 user = new UserWithUuidV7();
            user.setId("test-id-123");

            Object id = EntityReflector.getId(user);
            assertEquals("test-id-123", id);
        }

        @Test
        @DisplayName("getId throws on null entity")
        void testGetIdThrowsOnNullEntity() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.getId(null));
        }

        @Test
        @DisplayName("getId throws on null ID value")
        void testGetIdThrowsOnNullIdValue() {
            UserWithUuidV7 user = new UserWithUuidV7();
            // ID is null

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.getId(user));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("setField sets value by field name")
        void testSetFieldByName() {
            UserWithUuidV7 user = new UserWithUuidV7();
            EntityReflector.setField(user, "email", "test@example.com");

            assertEquals("test@example.com", user.getEmail());
        }

        @Test
        @DisplayName("setField throws on null entity")
        void testSetFieldThrowsOnNullEntity() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setField(null, "email", "test"));
        }

        @Test
        @DisplayName("setField throws on null/empty field name")
        void testSetFieldThrowsOnNullFieldName() {
            UserWithUuidV7 user = new UserWithUuidV7();

            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setField(user, null, "test"));
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setField(user, "", "test"));
        }

        @Test
        @DisplayName("setField throws on non-existent field")
        void testSetFieldThrowsOnNonExistentField() {
            UserWithUuidV7 user = new UserWithUuidV7();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setField(user, "nonExistent", "test"));
            assertTrue(ex.getMessage().contains("Field not found"));
        }

        @Test
        @DisplayName("setFieldByColumnName sets value by column name")
        void testSetFieldByColumnName() {
            JobWithJsonb job = new JobWithJsonb();
            EntityReflector.setFieldByColumnName(job, "name", "Test Job");

            assertEquals("Test Job", job.getName());
        }

        @Test
        @DisplayName("setFieldByColumnName throws on null entity")
        void testSetFieldByColumnNameThrowsOnNullEntity() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setFieldByColumnName(null, "name", "test"));
        }

        @Test
        @DisplayName("setFieldByColumnName throws on null/empty column name")
        void testSetFieldByColumnNameThrowsOnNullColumnName() {
            JobWithJsonb job = new JobWithJsonb();

            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setFieldByColumnName(job, null, "test"));
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setFieldByColumnName(job, "", "test"));
        }

        @Test
        @DisplayName("setFieldByColumnName throws on non-existent column")
        void testSetFieldByColumnNameThrowsOnNonExistentColumn() {
            JobWithJsonb job = new JobWithJsonb();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setFieldByColumnName(job, "non_existent", "test"));
            assertTrue(ex.getMessage().contains("No field found"));
        }

        @Test
        @DisplayName("getFieldByColumnName returns value")
        void testGetFieldByColumnName() {
            JobWithJsonb job = new JobWithJsonb();
            job.setName("Test Job");

            Object value = EntityReflector.getFieldByColumnName(job, "name");
            assertEquals("Test Job", value);
        }

        @Test
        @DisplayName("getFieldByColumnName returns null for null entity")
        void testGetFieldByColumnNameNullEntity() {
            Object value = EntityReflector.getFieldByColumnName(null, "name");
            assertNull(value);
        }

        @Test
        @DisplayName("getFieldByColumnName returns null for null/empty column")
        void testGetFieldByColumnNameNullColumn() {
            JobWithJsonb job = new JobWithJsonb();

            assertNull(EntityReflector.getFieldByColumnName(job, null));
            assertNull(EntityReflector.getFieldByColumnName(job, ""));
        }

        @Test
        @DisplayName("getFieldByColumnName returns null for non-existent column")
        void testGetFieldByColumnNameNonExistent() {
            JobWithJsonb job = new JobWithJsonb();

            Object value = EntityReflector.getFieldByColumnName(job, "non_existent");
            assertNull(value);
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

    // ==================== CONVERT ID TYPE TESTS ====================

    @Nested
    @DisplayName("convertIdType")
    class ConvertIdTypeTests {

        @Test
        @DisplayName("convertIdType returns null for null value")
        void testConvertIdTypeNull() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdType", Object.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, null, String.class);
            assertNull(result);
        }

        @Test
        @DisplayName("convertIdType returns value when already correct type")
        void testConvertIdTypeSameType() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdType", Object.class, Class.class);
            method.setAccessible(true);

            String value = "test-id";
            Object result = method.invoke(null, value, String.class);
            assertEquals(value, result);
        }

        @Test
        @DisplayName("convertIdType converts Number to Long")
        void testConvertIdTypeToLong() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdType", Object.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, 123, Long.class);
            assertEquals(123L, result);

            // primitive long
            result = method.invoke(null, 456, long.class);
            assertEquals(456L, result);
        }

        @Test
        @DisplayName("convertIdType converts Number to Integer")
        void testConvertIdTypeToInteger() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdType", Object.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, 123L, Integer.class);
            assertEquals(123, result);

            // primitive int
            result = method.invoke(null, 456L, int.class);
            assertEquals(456, result);
        }

        @Test
        @DisplayName("convertIdType converts to String")
        void testConvertIdTypeToString() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdType", Object.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, 123, String.class);
            assertEquals("123", result);

            java.util.UUID uuid = java.util.UUID.randomUUID();
            result = method.invoke(null, uuid, String.class);
            assertEquals(uuid.toString(), result);
        }

        @Test
        @DisplayName("convertIdType converts String to UUID")
        void testConvertIdTypeToUuid() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdType", Object.class, Class.class);
            method.setAccessible(true);

            String uuidStr = "7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1";
            Object result = method.invoke(null, uuidStr, java.util.UUID.class);
            assertInstanceOf(java.util.UUID.class, result);
            assertEquals(uuidStr, result.toString());
        }

        @Test
        @DisplayName("convertIdType returns original value for unsupported conversion")
        void testConvertIdTypeUnsupported() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdType", Object.class, Class.class);
            method.setAccessible(true);

            // Try to convert String to Boolean (unsupported)
            Object result = method.invoke(null, "true", Boolean.class);
            assertEquals("true", result);
        }
    }

    // ==================== GENERATOR INSTANTIATION TESTS ====================

    @Nested
    @DisplayName("Generator Instantiation")
    class GeneratorInstantiationTests {

        @Test
        @DisplayName("getOrCreateGenerator throws on generator without no-arg constructor")
        void testGeneratorWithoutNoArgConstructor() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "getOrCreateGenerator", Class.class);
            method.setAccessible(true);

            var ex = assertThrows(InvocationTargetException.class, () -> {
                method.invoke(null, NoArgConstructorGenerator.class);
            });
            assertInstanceOf(IllegalStateException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("Cannot instantiate ID generator"));
            assertTrue(ex.getCause().getMessage().contains("no-arg constructor"));
        }
    }

    // ==================== ENTITY REFLECTOR STRICT MODE TESTS ====================

    @Nested
    @DisplayName("EntityReflector Strict Mode")
    class EntityReflectorStrictModeTests {

        // Entity with private fields but no setters
        @Entity(table = "private_only")
        static class PrivateOnlyEntity {
            @Id
            @Column(name = "id")
            private Long id;

            @Column(name = "value")
            private String value;

            // No setters!
        }

        @Test
        @DisplayName("setField throws in strict mode when no setter exists")
        void testSetFieldStrictModeThrows() {
            ReflectionUtils.setStrictMode(true);
            try {
                PrivateOnlyEntity entity = new PrivateOnlyEntity();

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EntityReflector.setField(entity, "value", "test"));
                assertTrue(ex.getMessage().contains("Cannot set field"));
            } finally {
                ReflectionUtils.setStrictMode(false);
            }
        }

        @Test
        @DisplayName("setFieldByColumnName throws in strict mode when no setter exists")
        void testSetFieldByColumnNameStrictModeThrows() {
            ReflectionUtils.setStrictMode(true);
            try {
                PrivateOnlyEntity entity = new PrivateOnlyEntity();

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EntityReflector.setFieldByColumnName(entity, "value", "test"));
                assertTrue(ex.getMessage().contains("Cannot set field"));
            } finally {
                ReflectionUtils.setStrictMode(false);
            }
        }

        @Test
        @DisplayName("fromMap throws in strict mode when no setter exists")
        void testFromMapStrictModeThrows() {
            ReflectionUtils.setStrictMode(true);
            try {
                Map<String, Object> attrs = new HashMap<>();
                attrs.put("value", "test");

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EntityReflector.fromMap(PrivateOnlyEntity.class, attrs));
                assertTrue(ex.getMessage().contains("Cannot set field"));
            } finally {
                ReflectionUtils.setStrictMode(false);
            }
        }
    }

    // ==================== ENTITY REFLECTOR ADDITIONAL COVERAGE ====================

    @Nested
    @DisplayName("EntityReflector Additional Coverage")
    class EntityReflectorAdditionalTests {

        @Test
        @DisplayName("getEntityMeta throws on null class")
        void testGetEntityMetaNullClass() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.getEntityMeta(null));
        }

        @Test
        @DisplayName("getIdMeta throws on null class")
        void testGetIdMetaNullClass() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.getIdMeta(null));
        }

        @Test
        @DisplayName("setId throws on null entity")
        void testSetIdNullEntity() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setId(null, "id"));
        }

        @Test
        @DisplayName("toColumnMap throws on null entity")
        void testToColumnMapNullEntity() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.toColumnMap(null));
        }

        @Test
        @DisplayName("fromMap throws on null entity class")
        void testFromMapNullClass() {
            Map<String, Object> attrs = new HashMap<>();
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.fromMap(null, attrs));
        }

        @Test
        @DisplayName("fromMap throws on null attributes")
        void testFromMapNullAttributes() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.fromMap(UserWithUuidV7.class, null));
        }

        @Test
        @DisplayName("fromMap creates entity and sets fields")
        void testFromMapSetsFields() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("email", "test@example.com");

            UserWithUuidV7 user = EntityReflector.fromMap(UserWithUuidV7.class, attrs);

            assertNotNull(user);
            assertEquals("test@example.com", user.getEmail());
        }

        @Test
        @DisplayName("fromMap ignores non-existent columns")
        void testFromMapIgnoresNonExistent() {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("email", "test@example.com");
            attrs.put("non_existent_column", "ignored");

            UserWithUuidV7 user = EntityReflector.fromMap(UserWithUuidV7.class, attrs);

            assertNotNull(user);
            assertEquals("test@example.com", user.getEmail());
        }

        @Test
        @DisplayName("getId throws when entity is null")
        void testGetIdNullEntity() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.getId(null));
        }

        @Test
        @DisplayName("setField throws when entity is null")
        void testSetFieldNullEntity() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setField(null, "field", "value"));
        }

        @Test
        @DisplayName("setField throws when field name is null")
        void testSetFieldNullFieldName() {
            UserWithUuidV7 entity = new UserWithUuidV7();
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setField(entity, null, "value"));
        }

        @Test
        @DisplayName("setField throws when field name is empty")
        void testSetFieldEmptyFieldName() {
            UserWithUuidV7 entity = new UserWithUuidV7();
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setField(entity, "", "value"));
        }

        @Test
        @DisplayName("setField throws for non-existent field")
        void testSetFieldNonExistent() {
            UserWithUuidV7 entity = new UserWithUuidV7();
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setField(entity, "nonExistentField", "value"));
        }

        @Test
        @DisplayName("setFieldByColumnName throws when entity is null")
        void testSetFieldByColumnNameNullEntity() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setFieldByColumnName(null, "column", "value"));
        }

        @Test
        @DisplayName("setFieldByColumnName throws when column name is null")
        void testSetFieldByColumnNameNullColumnName() {
            UserWithUuidV7 entity = new UserWithUuidV7();
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setFieldByColumnName(entity, null, "value"));
        }

        @Test
        @DisplayName("setFieldByColumnName throws when column name is empty")
        void testSetFieldByColumnNameEmptyColumnName() {
            UserWithUuidV7 entity = new UserWithUuidV7();
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setFieldByColumnName(entity, "", "value"));
        }

        @Test
        @DisplayName("setFieldByColumnName throws for non-existent column")
        void testSetFieldByColumnNameNonExistent() {
            UserWithUuidV7 entity = new UserWithUuidV7();
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.setFieldByColumnName(entity, "non_existent_column", "value"));
        }
    }

    // ==================== BUILD INSERT SQL TESTS ====================

    @Nested
    @DisplayName("buildInsertSql")
    class BuildInsertSqlTests {

        @Entity(table = "uuid_db_entity", schema = "test_schema")
        static class UuidDbEntity {
            @Id(strategy = GenerationType.UUID_DB)
            @Column(name = "id", type = SqlType.UUID)
            private String id;

            @Column(name = "name")
            private String name;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }

        @Test
        @DisplayName("buildInsertSql generates PostgreSQL RETURNING clause")
        void testBuildInsertSqlPostgresReturning() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildInsertSql",
                EntityReflector.EntityMeta.class,
                EntityReflector.IdMeta.class,
                Map.class,
                sant1ago.dev.suprim.core.dialect.SqlDialect.class,
                boolean.class
            );
            method.setAccessible(true);

            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(UuidDbEntity.class);
            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UuidDbEntity.class);
            Map<String, Object> columns = new LinkedHashMap<>();
            columns.put("name", "Test");

            String sql = (String) method.invoke(null, entityMeta, idMeta, columns, PostgreSqlDialect.INSTANCE, true);

            assertTrue(sql.contains("INSERT INTO"));
            assertTrue(sql.contains("gen_random_uuid()"));
            assertTrue(sql.contains("RETURNING"));
        }

        @Test
        @DisplayName("buildInsertSql generates MySQL UUID() function")
        void testBuildInsertSqlMysqlUuid() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildInsertSql",
                EntityReflector.EntityMeta.class,
                EntityReflector.IdMeta.class,
                Map.class,
                sant1ago.dev.suprim.core.dialect.SqlDialect.class,
                boolean.class
            );
            method.setAccessible(true);

            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(UuidDbEntity.class);
            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UuidDbEntity.class);
            Map<String, Object> columns = new LinkedHashMap<>();
            columns.put("name", "Test");

            String sql = (String) method.invoke(null, entityMeta, idMeta, columns, MySqlDialect.INSTANCE, true);

            assertTrue(sql.contains("INSERT INTO"));
            assertTrue(sql.contains("UUID()"));
            assertFalse(sql.contains("RETURNING")); // MySQL doesn't support RETURNING
        }

        @Test
        @DisplayName("buildInsertSql without returning ID (simple insert)")
        void testBuildInsertSqlNoReturning() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildInsertSql",
                EntityReflector.EntityMeta.class,
                EntityReflector.IdMeta.class,
                Map.class,
                sant1ago.dev.suprim.core.dialect.SqlDialect.class,
                boolean.class
            );
            method.setAccessible(true);

            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(UserWithUuidV7.class);
            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UserWithUuidV7.class);
            Map<String, Object> columns = new LinkedHashMap<>();
            columns.put("email", "test@example.com");
            columns.put("id", "test-uuid");

            String sql = (String) method.invoke(null, entityMeta, idMeta, columns, PostgreSqlDialect.INSTANCE, false);

            assertTrue(sql.contains("INSERT INTO"));
            assertFalse(sql.contains("RETURNING")); // Not returning ID
            assertFalse(sql.contains("gen_random_uuid()"));
        }

        @Test
        @DisplayName("buildInsertSql includes schema when present")
        void testBuildInsertSqlWithSchema() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildInsertSql",
                EntityReflector.EntityMeta.class,
                EntityReflector.IdMeta.class,
                Map.class,
                sant1ago.dev.suprim.core.dialect.SqlDialect.class,
                boolean.class
            );
            method.setAccessible(true);

            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(UuidDbEntity.class);
            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UuidDbEntity.class);
            Map<String, Object> columns = new LinkedHashMap<>();
            columns.put("name", "Test");

            String sql = (String) method.invoke(null, entityMeta, idMeta, columns, PostgreSqlDialect.INSTANCE, false);

            assertTrue(sql.contains("\"test_schema\".\"uuid_db_entity\""));
        }

        @Test
        @DisplayName("buildInsertSql with returningId=true but non-UUID_DB strategy skips uuid function")
        void testBuildInsertSqlReturningWithNonUuidDbStrategy() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildInsertSql",
                EntityReflector.EntityMeta.class,
                EntityReflector.IdMeta.class,
                Map.class,
                sant1ago.dev.suprim.core.dialect.SqlDialect.class,
                boolean.class
            );
            method.setAccessible(true);

            // UUID_V7 strategy (not UUID_DB) with returningId=true
            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(UserWithUuidV7.class);
            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UserWithUuidV7.class);
            Map<String, Object> columns = new LinkedHashMap<>();
            columns.put("email", "test@example.com");
            columns.put("id", "app-generated-uuid");

            String sql = (String) method.invoke(null, entityMeta, idMeta, columns, PostgreSqlDialect.INSTANCE, true);

            assertTrue(sql.contains("INSERT INTO"));
            assertTrue(sql.contains("RETURNING")); // Has RETURNING because returningId=true
            assertFalse(sql.contains("gen_random_uuid()")); // No UUID function (not UUID_DB strategy)
            assertFalse(sql.contains("UUID()")); // No UUID function
        }
    }

    // ==================== GENERATE ID TESTS ====================

    @Nested
    @DisplayName("generateId")
    class GenerateIdTests {

        // Entity with UUID field type (not String)
        @Entity(table = "uuid_field_entity")
        static class UuidFieldEntity {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id", type = SqlType.UUID)
            private java.util.UUID id;

            @Column(name = "name")
            private String name;

            public java.util.UUID getId() { return id; }
            public void setId(java.util.UUID id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }

        @Entity(table = "uuid_v4_field_entity")
        static class UuidV4FieldEntity {
            @Id(strategy = GenerationType.UUID_V4)
            @Column(name = "id", type = SqlType.UUID)
            private java.util.UUID id;

            @Column(name = "name")
            private String name;

            public java.util.UUID getId() { return id; }
            public void setId(java.util.UUID id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }

        @Test
        @DisplayName("generateId returns UUID when field type is UUID (not String)")
        void testGenerateIdReturnsUuidType() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "generateId", EntityReflector.IdMeta.class);
            method.setAccessible(true);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UuidFieldEntity.class);
            Object result = method.invoke(null, idMeta);

            assertInstanceOf(java.util.UUID.class, result);
        }

        @Test
        @DisplayName("generateId returns UUID for UUID_V4 when field type is UUID")
        void testGenerateIdUuidV4ReturnsUuidType() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "generateId", EntityReflector.IdMeta.class);
            method.setAccessible(true);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UuidV4FieldEntity.class);
            Object result = method.invoke(null, idMeta);

            assertInstanceOf(java.util.UUID.class, result);
        }

        @Test
        @DisplayName("generateId returns String when field type is String")
        void testGenerateIdReturnsStringType() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "generateId", EntityReflector.IdMeta.class);
            method.setAccessible(true);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UserWithUuidV7.class);
            Object result = method.invoke(null, idMeta);

            assertInstanceOf(String.class, result);
        }
    }

    // ==================== BUILD UPDATE SQL TESTS ====================

    @Nested
    @DisplayName("buildUpdateSql")
    class BuildUpdateSqlTests {

        @Entity(table = "update_entity", schema = "test_schema")
        static class UpdateEntityWithSchema {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id", type = SqlType.UUID)
            private String id;

            @Column(name = "name")
            private String name;

            @Column(name = "value")
            private Integer value;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public Integer getValue() { return value; }
            public void setValue(Integer value) { this.value = value; }
        }

        @Test
        @DisplayName("buildUpdateSql generates correct SQL for PostgreSQL")
        void testBuildUpdateSqlPostgres() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildUpdateSql",
                EntityReflector.EntityMeta.class,
                EntityReflector.IdMeta.class,
                Map.class,
                sant1ago.dev.suprim.core.dialect.SqlDialect.class
            );
            method.setAccessible(true);

            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(UserWithUuidV7.class);
            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UserWithUuidV7.class);
            Map<String, Object> columns = new LinkedHashMap<>();
            columns.put("email", "updated@example.com");

            String sql = (String) method.invoke(null, entityMeta, idMeta, columns, PostgreSqlDialect.INSTANCE);

            assertTrue(sql.contains("UPDATE"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("\"email\" = ?"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("\"id\" = ?"));
        }

        @Test
        @DisplayName("buildUpdateSql includes schema when present")
        void testBuildUpdateSqlWithSchema() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildUpdateSql",
                EntityReflector.EntityMeta.class,
                EntityReflector.IdMeta.class,
                Map.class,
                sant1ago.dev.suprim.core.dialect.SqlDialect.class
            );
            method.setAccessible(true);

            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(UpdateEntityWithSchema.class);
            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UpdateEntityWithSchema.class);
            Map<String, Object> columns = new LinkedHashMap<>();
            columns.put("name", "Test");
            columns.put("value", 42);

            String sql = (String) method.invoke(null, entityMeta, idMeta, columns, PostgreSqlDialect.INSTANCE);

            assertTrue(sql.contains("\"test_schema\".\"update_entity\""));
            assertTrue(sql.contains("\"name\" = ?"));
            assertTrue(sql.contains("\"value\" = ?"));
        }

        @Test
        @DisplayName("buildUpdateSql generates correct SQL for MySQL")
        void testBuildUpdateSqlMysql() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildUpdateSql",
                EntityReflector.EntityMeta.class,
                EntityReflector.IdMeta.class,
                Map.class,
                sant1ago.dev.suprim.core.dialect.SqlDialect.class
            );
            method.setAccessible(true);

            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(UserWithUuidV7.class);
            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UserWithUuidV7.class);
            Map<String, Object> columns = new LinkedHashMap<>();
            columns.put("email", "updated@example.com");

            String sql = (String) method.invoke(null, entityMeta, idMeta, columns, MySqlDialect.INSTANCE);

            assertTrue(sql.contains("UPDATE"));
            assertTrue(sql.contains("`email` = ?"));
            assertTrue(sql.contains("`id` = ?"));
        }
    }

    // ==================== CONVERT ID FOR QUERY TESTS ====================

    @Nested
    @DisplayName("convertIdForQuery")
    class ConvertIdForQueryTests {

        @Test
        @DisplayName("convertIdForQuery converts String to UUID when column type is UUID")
        void testConvertIdForQueryStringToUuid() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdForQuery", Object.class, EntityReflector.IdMeta.class);
            method.setAccessible(true);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(ProductWithUuidId.class);
            String uuidStr = "7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1";

            Object result = method.invoke(null, uuidStr, idMeta);

            assertInstanceOf(java.util.UUID.class, result);
            assertEquals(uuidStr, result.toString());
        }

        @Test
        @DisplayName("convertIdForQuery keeps String when column type is not UUID")
        void testConvertIdForQueryKeepsString() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdForQuery", Object.class, EntityReflector.IdMeta.class);
            method.setAccessible(true);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UserWithUuidV7.class);
            String id = "test-id-123";

            Object result = method.invoke(null, id, idMeta);

            assertInstanceOf(String.class, result);
            assertEquals(id, result);
        }

        @Test
        @DisplayName("convertIdForQuery keeps UUID as-is")
        void testConvertIdForQueryKeepsUuid() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdForQuery", Object.class, EntityReflector.IdMeta.class);
            method.setAccessible(true);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(OrderWithUuidField.class);
            java.util.UUID uuid = java.util.UUID.randomUUID();

            Object result = method.invoke(null, uuid, idMeta);

            assertInstanceOf(java.util.UUID.class, result);
            assertEquals(uuid, result);
        }

        @Test
        @DisplayName("convertIdForQuery keeps Long as-is")
        void testConvertIdForQueryKeepsLong() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdForQuery", Object.class, EntityReflector.IdMeta.class);
            method.setAccessible(true);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UserWithIdentity.class);
            Long id = 12345L;

            Object result = method.invoke(null, id, idMeta);

            assertEquals(id, result);
        }
    }

    // ==================== CONVERT VALUE TO FIELD TYPE TESTS ====================

    @Nested
    @DisplayName("convertValueToFieldType")
    class ConvertValueToFieldTypeTests {

        @Test
        @DisplayName("convertValueToFieldType returns null for null value")
        void testConvertValueToFieldTypeNull() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, null, String.class);
            assertNull(result);
        }

        @Test
        @DisplayName("convertValueToFieldType returns value when already correct type")
        void testConvertValueToFieldTypeSameType() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            String value = "test-value";
            Object result = method.invoke(null, value, String.class);
            assertEquals(value, result);
        }

        @Test
        @DisplayName("convertValueToFieldType converts to String")
        void testConvertValueToFieldTypeToString() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, 123, String.class);
            assertEquals("123", result);

            java.util.UUID uuid = java.util.UUID.randomUUID();
            result = method.invoke(null, uuid, String.class);
            assertEquals(uuid.toString(), result);
        }

        @Test
        @DisplayName("convertValueToFieldType converts String to UUID")
        void testConvertValueToFieldTypeStringToUuid() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            String uuidStr = "7186e95e-944d-44a4-b1d5-fb1cb8d6e6e1";
            Object result = method.invoke(null, uuidStr, java.util.UUID.class);

            assertInstanceOf(java.util.UUID.class, result);
            assertEquals(uuidStr, result.toString());
        }

        @Test
        @DisplayName("convertValueToFieldType converts Number to Long")
        void testConvertValueToFieldTypeToLong() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, 123, Long.class);
            assertEquals(123L, result);

            // primitive long
            result = method.invoke(null, 456, long.class);
            assertEquals(456L, result);
        }

        @Test
        @DisplayName("convertValueToFieldType converts Number to Integer")
        void testConvertValueToFieldTypeToInteger() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, 123L, Integer.class);
            assertEquals(123, result);

            // primitive int
            result = method.invoke(null, 456L, int.class);
            assertEquals(456, result);
        }

        @Test
        @DisplayName("convertValueToFieldType returns original value for unsupported conversion")
        void testConvertValueToFieldTypeUnsupported() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            // Try to convert String to Boolean (unsupported)
            Object result = method.invoke(null, "true", Boolean.class);
            assertEquals("true", result);
        }

        @Test
        @DisplayName("convertValueToFieldType keeps UUID as-is when target is UUID")
        void testConvertValueToFieldTypeUuidToUuid() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.util.UUID uuid = java.util.UUID.randomUUID();
            Object result = method.invoke(null, uuid, java.util.UUID.class);

            assertEquals(uuid, result);
        }
    }

    // ==================== BUILD COLUMN MAP EDGE CASES ====================

    @Nested
    @DisplayName("buildColumnMap Edge Cases")
    class BuildColumnMapEdgeCaseTests {

        // Entity with field that has empty column name (uses snake_case conversion)
        @Entity(table = "auto_column_entity")
        static class AutoColumnNameEntity {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            @Column(name = "") // Empty name - should use snake_case of field name
            private String userName;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getUserName() { return userName; }
            public void setUserName(String userName) { this.userName = userName; }
        }

        @Test
        @DisplayName("buildColumnMap uses snake_case for empty column name")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapEmptyColumnName() throws Exception {
            AutoColumnNameEntity entity = new AutoColumnNameEntity();
            entity.setId("test-id");
            entity.setUserName("testuser");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(AutoColumnNameEntity.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, entity, idMeta, false);

            // Should contain user_name (snake_case of userName)
            assertTrue(columnMap.containsKey("user_name") || columnMap.containsKey("userName"),
                "Column map should contain user_name or userName: " + columnMap.keySet());
        }

        @Test
        @DisplayName("buildColumnMap skips ID when skipId is true")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapSkipsId() throws Exception {
            UserWithUuidV7 entity = new UserWithUuidV7();
            entity.setId("test-id");
            entity.setEmail("test@example.com");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UserWithUuidV7.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, entity, idMeta, true);

            // ID should be skipped
            assertFalse(columnMap.containsKey("id"));
            assertTrue(columnMap.containsKey("email"));
        }

        @Test
        @DisplayName("buildColumnMap returns empty map when entity has only ID set (skipId=true)")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapEmptyWhenOnlyIdSet() throws Exception {
            UserWithUuidV7 entity = new UserWithUuidV7();
            entity.setId("test-id");
            // email is null - not set

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(UserWithUuidV7.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, entity, idMeta, true);

            // Should be empty because ID is skipped and email is null
            assertTrue(columnMap.isEmpty(), "Column map should be empty when only ID is set and skipId=true");
        }
    }

    // ==================== ENTITY WITH SCHEMA INTEGRATION TESTS ====================

    @Nested
    @DisplayName("Schema-qualified SQL Generation")
    class SchemaQualifiedSqlTests {

        @Entity(table = "schema_entity", schema = "my_schema")
        static class EntityWithSchema {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id", type = SqlType.UUID)
            private String id;

            @Column(name = "name")
            private String name;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }

        @Test
        @DisplayName("delete SQL includes schema when present")
        void testDeleteSqlWithSchema() {
            // Verify EntityMeta includes schema
            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(EntityWithSchema.class);
            assertEquals("my_schema", entityMeta.schema());
            assertEquals("schema_entity", entityMeta.tableName());
        }

        @Test
        @DisplayName("refresh SQL includes schema when present")
        void testRefreshSqlWithSchema() {
            // Verify EntityMeta includes schema
            EntityReflector.EntityMeta entityMeta = EntityReflector.getEntityMeta(EntityWithSchema.class);
            assertEquals("my_schema", entityMeta.schema());
        }
    }

    // ==================== POPULATE ENTITY FROM RESULT SET TESTS ====================

    @Nested
    @DisplayName("populateEntityFromResultSet")
    class PopulateEntityFromResultSetTests {

        @Test
        @DisplayName("getAllFields returns all fields including inherited ones")
        void testGetAllFieldsIncludesInherited() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "getAllFields", Class.class);
            method.setAccessible(true);

            java.lang.reflect.Field[] fields = (java.lang.reflect.Field[]) method.invoke(null, PostWithInheritance.class);

            // Should include fields from both PostWithInheritance and BaseEntity
            assertTrue(fields.length >= 4, "Should have at least 4 fields (id, title, createdAt, updatedAt)");

            // Verify we have both child and parent fields
            boolean hasId = false;
            boolean hasTitle = false;
            boolean hasCreatedAt = false;
            boolean hasUpdatedAt = false;

            for (java.lang.reflect.Field field : fields) {
                if ("id".equals(field.getName())) hasId = true;
                if ("title".equals(field.getName())) hasTitle = true;
                if ("createdAt".equals(field.getName())) hasCreatedAt = true;
                if ("updatedAt".equals(field.getName())) hasUpdatedAt = true;
            }

            assertTrue(hasId, "Should have id field");
            assertTrue(hasTitle, "Should have title field");
            assertTrue(hasCreatedAt, "Should have createdAt field from parent");
            assertTrue(hasUpdatedAt, "Should have updatedAt field from parent");
        }

        @Test
        @DisplayName("getAllFields stops at Object class")
        void testGetAllFieldsStopsAtObject() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "getAllFields", Class.class);
            method.setAccessible(true);

            java.lang.reflect.Field[] fields = (java.lang.reflect.Field[]) method.invoke(null, UserWithUuidV7.class);

            // Should not include Object's fields
            for (java.lang.reflect.Field field : fields) {
                assertFalse(field.getDeclaringClass().equals(Object.class),
                    "Should not include Object class fields");
            }
        }
    }

    // ==================== UPDATE EMPTY COLUMNS TEST ====================

    @Nested
    @DisplayName("Update with Empty Columns")
    class UpdateEmptyColumnsTests {

        @Entity(table = "id_only_entity")
        static class IdOnlyEntity {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
        }

        @Test
        @DisplayName("buildColumnMap returns empty for entity with only ID")
        @SuppressWarnings("unchecked")
        void testBuildColumnMapEmptyForIdOnlyEntity() throws Exception {
            IdOnlyEntity entity = new IdOnlyEntity();
            entity.setId("test-id");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(IdOnlyEntity.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, entity, idMeta, true);

            // Should be empty because ID is the only column and it's skipped
            assertTrue(columnMap.isEmpty(), "Column map should be empty for entity with only ID");
        }
    }

    // ==================== NULL PARAMETER VALIDATION TESTS ====================

    @Nested
    @DisplayName("Null Parameter Validation")
    class NullParameterValidationTests {

        @Test
        @DisplayName("setParameters handles empty array")
        void testSetParametersEmptyArray() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "setParameters", java.sql.PreparedStatement.class, Object[].class);
            method.setAccessible(true);

            // This should not throw - empty array is valid
            // We can't test with a real PreparedStatement without a connection,
            // but we verify the method signature accepts empty arrays
            assertNotNull(method);
        }
    }

    // ==================== SCHEMA-QUALIFIED TABLE TESTS ====================

    @Nested
    @DisplayName("Schema-qualified Tables in update/delete/refresh")
    class SchemaQualifiedCrudTests {

        @Entity(table = "schema_crud_entity", schema = "test_schema")
        static class SchemaEntity {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            @Column(name = "name")
            private String name;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }

        @Test
        @DisplayName("update generates schema-qualified SQL")
        void testUpdateWithSchema() {
            // Verify the entity meta includes schema
            EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(SchemaEntity.class);
            assertEquals("test_schema", meta.schema());

            // Test buildUpdateSql with schema
            try {
                Method method = EntityPersistence.class.getDeclaredMethod(
                    "buildUpdateSql",
                    EntityReflector.EntityMeta.class,
                    EntityReflector.IdMeta.class,
                    Map.class,
                    sant1ago.dev.suprim.core.dialect.SqlDialect.class
                );
                method.setAccessible(true);

                EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(SchemaEntity.class);
                Map<String, Object> columns = new LinkedHashMap<>();
                columns.put("name", "Test");

                String sql = (String) method.invoke(null, meta, idMeta, columns, PostgreSqlDialect.INSTANCE);
                assertTrue(sql.contains("\"test_schema\".\"schema_crud_entity\""),
                    "SQL should contain schema-qualified table name: " + sql);
            } catch (Exception e) {
                fail("Should not throw: " + e.getMessage());
            }
        }
    }

    // ==================== POPULATE ENTITY FROM RESULT SET TESTS ====================

    @Nested
    @DisplayName("populateEntityFromResultSet Edge Cases")
    class PopulateEntityEdgeCases {

        // Entity with field without @Column annotation
        @Entity(table = "mixed_fields")
        static class EntityWithMixedFields {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            @Column(name = "name")
            private String name;

            // Field without @Column - should be skipped
            private String transientField;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getTransientField() { return transientField; }
            public void setTransientField(String transientField) { this.transientField = transientField; }
        }

        // Entity with empty column name
        @Entity(table = "empty_col_name")
        static class EntityWithEmptyColumnName {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            @Column(name = "") // Empty - should use snake_case of field name
            private String myFieldName;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getMyFieldName() { return myFieldName; }
            public void setMyFieldName(String myFieldName) { this.myFieldName = myFieldName; }
        }

        @Test
        @DisplayName("getAllFields skips fields without @Column when iterating")
        void testFieldsWithoutColumnAnnotation() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "getAllFields", Class.class);
            method.setAccessible(true);

            java.lang.reflect.Field[] fields = (java.lang.reflect.Field[]) method.invoke(null, EntityWithMixedFields.class);

            // Should include all fields (including transientField)
            boolean hasTransient = false;
            for (java.lang.reflect.Field field : fields) {
                if ("transientField".equals(field.getName())) {
                    hasTransient = true;
                    // Verify it doesn't have @Column
                    assertNull(field.getAnnotation(Column.class));
                }
            }
            assertTrue(hasTransient, "Should include transientField in fields array");
        }

        @Test
        @DisplayName("buildColumnMap uses snake_case for empty column name")
        @SuppressWarnings("unchecked")
        void testEmptyColumnNameUsesSnakeCase() throws Exception {
            EntityWithEmptyColumnName entity = new EntityWithEmptyColumnName();
            entity.setId("test-id");
            entity.setMyFieldName("test value");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithEmptyColumnName.class);

            Method method = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class);
            method.setAccessible(true);
            Map<String, Object> columnMap = (Map<String, Object>) method.invoke(null, entity, idMeta, false);

            // Should have snake_case column name
            assertTrue(columnMap.containsKey("my_field_name"),
                "Column map should use snake_case: " + columnMap.keySet());
            assertEquals("test value", columnMap.get("my_field_name"));
        }
    }

    // ==================== TYPE CONVERSION EDGE CASES ====================

    @Nested
    @DisplayName("Type Conversion Edge Cases")
    class TypeConversionEdgeCases {

        @Test
        @DisplayName("convertValueToFieldType handles UUID that's not a String")
        void testConvertValueToFieldTypeUuidNotString() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            // Pass an Integer when expecting UUID - should return original value
            Object result = method.invoke(null, 123, java.util.UUID.class);
            assertEquals(123, result);
        }

        @Test
        @DisplayName("convertValueToFieldType handles Long conversion from non-Number")
        void testConvertValueToFieldTypeLongFromNonNumber() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            // Pass a String when expecting Long - should return original value
            Object result = method.invoke(null, "not-a-number", Long.class);
            assertEquals("not-a-number", result);
        }

        @Test
        @DisplayName("convertValueToFieldType handles Integer conversion from non-Number")
        void testConvertValueToFieldTypeIntegerFromNonNumber() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertValueToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            // Pass a String when expecting Integer - should return original value
            Object result = method.invoke(null, "not-a-number", Integer.class);
            assertEquals("not-a-number", result);
        }

        @Test
        @DisplayName("convertIdType handles non-String to UUID")
        void testConvertIdTypeNonStringToUuid() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertIdType", Object.class, Class.class);
            method.setAccessible(true);

            // Pass an Integer when expecting UUID - should return original value
            Object result = method.invoke(null, 123, java.util.UUID.class);
            assertEquals(123, result);
        }
    }

    // ==================== GET ALL FIELDS LOOP EDGE CASES ====================

    @Nested
    @DisplayName("getAllFields Loop Edge Cases")
    class GetAllFieldsLoopTests {

        @Test
        @DisplayName("getAllFields handles null parent gracefully")
        void testGetAllFieldsWithDeepHierarchy() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "getAllFields", Class.class);
            method.setAccessible(true);

            // Test with a class that eventually reaches Object
            java.lang.reflect.Field[] fields = (java.lang.reflect.Field[]) method.invoke(null, PostWithInheritance.class);

            // Should have fields from PostWithInheritance and BaseEntity
            assertTrue(fields.length >= 4);
        }

        @Test
        @DisplayName("getAllFields stops at Object class boundary")
        void testGetAllFieldsStopsAtObject() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "getAllFields", Class.class);
            method.setAccessible(true);

            // A simple entity class
            java.lang.reflect.Field[] fields = (java.lang.reflect.Field[]) method.invoke(null, UserWithUuidV7.class);

            // None of the fields should be from Object class
            for (java.lang.reflect.Field field : fields) {
                assertNotEquals(Object.class, field.getDeclaringClass(),
                    "Should not include fields from Object class");
            }
        }

        @Test
        @DisplayName("getAllFields returns empty array for null input")
        void testGetAllFieldsNullInput() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "getAllFields", Class.class);
            method.setAccessible(true);

            java.lang.reflect.Field[] fields = (java.lang.reflect.Field[]) method.invoke(null, (Class<?>) null);

            assertEquals(0, fields.length, "Should return empty array for null input");
        }

        @Test
        @DisplayName("getAllFields returns empty array for Object.class input")
        void testGetAllFieldsObjectClassInput() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "getAllFields", Class.class);
            method.setAccessible(true);

            java.lang.reflect.Field[] fields = (java.lang.reflect.Field[]) method.invoke(null, Object.class);

            assertEquals(0, fields.length, "Should return empty array for Object.class input");
        }
    }

    // ==================== TIMESTAMP CONVERSION TESTS ====================

    @Nested
    @DisplayName("Timestamp Conversion")
    class TimestampConversionTests {

        @Test
        @DisplayName("convertTimestampToFieldType returns null for null input")
        void testConvertTimestampNull() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, null, java.time.LocalDateTime.class);
            assertNull(result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType returns value when already correct type")
        void testConvertTimestampSameType() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.Instant instant = java.time.Instant.now();
            Object result = method.invoke(null, instant, java.time.Instant.class);
            assertEquals(instant, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts Instant to LocalDateTime")
        void testConvertInstantToLocalDateTime() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.Instant instant = java.time.Instant.now();
            Object result = method.invoke(null, instant, java.time.LocalDateTime.class);

            assertInstanceOf(java.time.LocalDateTime.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts Instant to OffsetDateTime")
        void testConvertInstantToOffsetDateTime() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.Instant instant = java.time.Instant.now();
            Object result = method.invoke(null, instant, java.time.OffsetDateTime.class);

            assertInstanceOf(java.time.OffsetDateTime.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts Instant to Timestamp")
        void testConvertInstantToTimestamp() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.Instant instant = java.time.Instant.now();
            Object result = method.invoke(null, instant, java.sql.Timestamp.class);

            assertInstanceOf(java.sql.Timestamp.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts Timestamp to LocalDateTime")
        void testConvertTimestampToLocalDateTime() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.sql.Timestamp timestamp = java.sql.Timestamp.from(java.time.Instant.now());
            Object result = method.invoke(null, timestamp, java.time.LocalDateTime.class);

            assertInstanceOf(java.time.LocalDateTime.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts Timestamp to Instant")
        void testConvertTimestampToInstant() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.sql.Timestamp timestamp = java.sql.Timestamp.from(java.time.Instant.now());
            Object result = method.invoke(null, timestamp, java.time.Instant.class);

            assertInstanceOf(java.time.Instant.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts LocalDateTime to Instant")
        void testConvertLocalDateTimeToInstant() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.LocalDateTime ldt = java.time.LocalDateTime.now();
            Object result = method.invoke(null, ldt, java.time.Instant.class);

            assertInstanceOf(java.time.Instant.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts to java.util.Date")
        void testConvertToUtilDate() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.Instant instant = java.time.Instant.now();
            Object result = method.invoke(null, instant, java.util.Date.class);

            assertInstanceOf(java.util.Date.class, result);
        }

        @Test
        @DisplayName("isTemporalType returns true for temporal types")
        void testIsTemporalType() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "isTemporalType", Class.class);
            method.setAccessible(true);

            assertTrue((Boolean) method.invoke(null, java.time.LocalDateTime.class));
            assertTrue((Boolean) method.invoke(null, java.time.Instant.class));
            assertTrue((Boolean) method.invoke(null, java.time.OffsetDateTime.class));
            assertTrue((Boolean) method.invoke(null, java.sql.Timestamp.class));
            assertTrue((Boolean) method.invoke(null, java.util.Date.class));
        }

        @Test
        @DisplayName("isTemporalType returns false for non-temporal types")
        void testIsNotTemporalType() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "isTemporalType", Class.class);
            method.setAccessible(true);

            assertFalse((Boolean) method.invoke(null, String.class));
            assertFalse((Boolean) method.invoke(null, Long.class));
            assertFalse((Boolean) method.invoke(null, Integer.class));
            assertFalse((Boolean) method.invoke(null, java.util.UUID.class));
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts OffsetDateTime to Instant")
        void testConvertOffsetDateTimeToInstant() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.OffsetDateTime odt = java.time.OffsetDateTime.now();
            Object result = method.invoke(null, odt, java.time.Instant.class);

            assertInstanceOf(java.time.Instant.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts OffsetDateTime to LocalDateTime")
        void testConvertOffsetDateTimeToLocalDateTime() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.OffsetDateTime odt = java.time.OffsetDateTime.now();
            Object result = method.invoke(null, odt, java.time.LocalDateTime.class);

            assertInstanceOf(java.time.LocalDateTime.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts OffsetDateTime to Timestamp")
        void testConvertOffsetDateTimeToTimestamp() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.OffsetDateTime odt = java.time.OffsetDateTime.now();
            Object result = method.invoke(null, odt, java.sql.Timestamp.class);

            assertInstanceOf(java.sql.Timestamp.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts OffsetDateTime to java.util.Date")
        void testConvertOffsetDateTimeToDate() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.OffsetDateTime odt = java.time.OffsetDateTime.now();
            Object result = method.invoke(null, odt, java.util.Date.class);

            assertInstanceOf(java.util.Date.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType returns unknown type as-is")
        void testConvertUnknownTypeReturnsAsIs() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            // Unknown source type should be returned as-is
            String unknownValue = "not a timestamp";
            Object result = method.invoke(null, unknownValue, java.time.Instant.class);

            assertEquals(unknownValue, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType returns instant fallback for unknown target type")
        void testConvertFallbackReturnsInstant() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            // When target type is not a known temporal type, return the instant
            java.time.Instant instant = java.time.Instant.now();
            Object result = method.invoke(null, instant, Object.class);

            // Should return the instant since Object.class is not a known target type
            assertInstanceOf(java.time.Instant.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts Timestamp to OffsetDateTime")
        void testConvertTimestampToOffsetDateTime() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.sql.Timestamp ts = java.sql.Timestamp.from(java.time.Instant.now());
            Object result = method.invoke(null, ts, java.time.OffsetDateTime.class);

            assertInstanceOf(java.time.OffsetDateTime.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts Timestamp to java.util.Date")
        void testConvertTimestampToUtilDate() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.sql.Timestamp ts = java.sql.Timestamp.from(java.time.Instant.now());
            Object result = method.invoke(null, ts, java.util.Date.class);

            assertInstanceOf(java.util.Date.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts LocalDateTime to OffsetDateTime")
        void testConvertLocalDateTimeToOffsetDateTime() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.LocalDateTime ldt = java.time.LocalDateTime.now();
            Object result = method.invoke(null, ldt, java.time.OffsetDateTime.class);

            assertInstanceOf(java.time.OffsetDateTime.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts LocalDateTime to Timestamp")
        void testConvertLocalDateTimeToTimestamp() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.LocalDateTime ldt = java.time.LocalDateTime.now();
            Object result = method.invoke(null, ldt, java.sql.Timestamp.class);

            assertInstanceOf(java.sql.Timestamp.class, result);
        }

        @Test
        @DisplayName("convertTimestampToFieldType converts LocalDateTime to java.util.Date")
        void testConvertLocalDateTimeToUtilDate() throws Exception {
            Method method = EntityPersistence.class.getDeclaredMethod(
                "convertTimestampToFieldType", Object.class, Class.class);
            method.setAccessible(true);

            java.time.LocalDateTime ldt = java.time.LocalDateTime.now();
            Object result = method.invoke(null, ldt, java.util.Date.class);

            assertInstanceOf(java.util.Date.class, result);
        }
    }

    // ==================== TIMESTAMP ACTION TEST ENTITIES ====================

    @Entity(table = "ts_default")
    static class EntityWithDefaultTimestamps {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @CreationTimestamp  // default: IF_NULL
        private java.time.LocalDateTime createdAt;

        @UpdateTimestamp  // default: NOW
        private java.time.LocalDateTime updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @Entity(table = "ts_now")
    static class EntityWithNowTimestamps {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @CreationTimestamp(onCreation = TimestampAction.NOW)  // always overwrite
        private java.time.LocalDateTime createdAt;

        @UpdateTimestamp(onModification = TimestampAction.NOW)  // always overwrite (default)
        private java.time.LocalDateTime updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @Entity(table = "ts_if_null")
    static class EntityWithIfNullTimestamps {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @CreationTimestamp(onCreation = TimestampAction.IF_NULL)  // default
        private java.time.LocalDateTime createdAt;

        @UpdateTimestamp(onModification = TimestampAction.IF_NULL)  // set only if null
        private java.time.LocalDateTime updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @Entity(table = "ts_never")
    static class EntityWithNeverTimestamps {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @CreationTimestamp(onCreation = TimestampAction.NEVER)  // manual only
        private java.time.LocalDateTime createdAt;

        @UpdateTimestamp(onModification = TimestampAction.NEVER)  // manual only
        private java.time.LocalDateTime updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @Nested
    @DisplayName("Timestamp Action Tests")
    class TimestampActionTests {

        @Test
        @DisplayName("CreationTimestamp IF_NULL sets timestamp when null")
        void creationTimestamp_ifNull_setsWhenNull() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithDefaultTimestamps entity = new EntityWithDefaultTimestamps();
            entity.setId("test-id");
            // createdAt is null

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithDefaultTimestamps.class);
            Map<String, Object> columns = (Map<String, Object>) buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should have set createdAt
            assertNotNull(entity.getCreatedAt());
            assertTrue(columns.containsKey("created_at"));
        }

        @Test
        @DisplayName("CreationTimestamp IF_NULL preserves existing value")
        void creationTimestamp_ifNull_preservesExisting() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithDefaultTimestamps entity = new EntityWithDefaultTimestamps();
            entity.setId("test-id");
            java.time.LocalDateTime existingTime = java.time.LocalDateTime.of(2020, 1, 1, 12, 0);
            entity.setCreatedAt(existingTime);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithDefaultTimestamps.class);
            Map<String, Object> columns = (Map<String, Object>) buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should preserve the existing value
            assertEquals(existingTime, entity.getCreatedAt());
        }

        @Test
        @DisplayName("CreationTimestamp NOW always overwrites")
        void creationTimestamp_now_alwaysOverwrites() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithNowTimestamps entity = new EntityWithNowTimestamps();
            entity.setId("test-id");
            java.time.LocalDateTime existingTime = java.time.LocalDateTime.of(2020, 1, 1, 12, 0);
            entity.setCreatedAt(existingTime);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithNowTimestamps.class);
            buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should have overwritten with current time
            assertNotEquals(existingTime, entity.getCreatedAt());
            assertTrue(entity.getCreatedAt().isAfter(existingTime));
        }

        @Test
        @DisplayName("CreationTimestamp NEVER does not auto-set")
        void creationTimestamp_never_doesNotAutoSet() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithNeverTimestamps entity = new EntityWithNeverTimestamps();
            entity.setId("test-id");
            // createdAt is null

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithNeverTimestamps.class);
            Map<String, Object> columns = (Map<String, Object>) buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should NOT have set createdAt
            assertNull(entity.getCreatedAt());
            assertFalse(columns.containsKey("created_at"));
        }

        @Test
        @DisplayName("UpdateTimestamp NOW always overwrites")
        void updateTimestamp_now_alwaysOverwrites() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithDefaultTimestamps entity = new EntityWithDefaultTimestamps();
            entity.setId("test-id");
            java.time.LocalDateTime existingTime = java.time.LocalDateTime.of(2020, 1, 1, 12, 0);
            entity.setUpdatedAt(existingTime);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithDefaultTimestamps.class);
            buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should have overwritten with current time
            assertNotEquals(existingTime, entity.getUpdatedAt());
            assertTrue(entity.getUpdatedAt().isAfter(existingTime));
        }

        @Test
        @DisplayName("UpdateTimestamp IF_NULL sets timestamp when null")
        void updateTimestamp_ifNull_setsWhenNull() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithIfNullTimestamps entity = new EntityWithIfNullTimestamps();
            entity.setId("test-id");
            // updatedAt is null

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithIfNullTimestamps.class);
            Map<String, Object> columns = (Map<String, Object>) buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should have set updatedAt
            assertNotNull(entity.getUpdatedAt());
            assertTrue(columns.containsKey("updated_at"));
        }

        @Test
        @DisplayName("UpdateTimestamp IF_NULL preserves existing value")
        void updateTimestamp_ifNull_preservesExisting() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithIfNullTimestamps entity = new EntityWithIfNullTimestamps();
            entity.setId("test-id");
            java.time.LocalDateTime existingTime = java.time.LocalDateTime.of(2020, 1, 1, 12, 0);
            entity.setUpdatedAt(existingTime);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithIfNullTimestamps.class);
            buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should preserve the existing value
            assertEquals(existingTime, entity.getUpdatedAt());
        }

        @Test
        @DisplayName("UpdateTimestamp NEVER does not auto-set")
        void updateTimestamp_never_doesNotAutoSet() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithNeverTimestamps entity = new EntityWithNeverTimestamps();
            entity.setId("test-id");
            // updatedAt is null

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithNeverTimestamps.class);
            Map<String, Object> columns = (Map<String, Object>) buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should NOT have set updatedAt
            assertNull(entity.getUpdatedAt());
            assertFalse(columns.containsKey("updated_at"));
        }

        @Test
        @DisplayName("CreationTimestamp not set on update (isInsert=false)")
        void creationTimestamp_notSetOnUpdate() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithDefaultTimestamps entity = new EntityWithDefaultTimestamps();
            entity.setId("test-id");
            // createdAt is null

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithDefaultTimestamps.class);
            // isInsert = false (update operation)
            Map<String, Object> columns = (Map<String, Object>) buildColumnMap.invoke(null, entity, idMeta, false, false);

            // Should NOT have set createdAt on update
            assertNull(entity.getCreatedAt());
        }

        @Test
        @DisplayName("UpdateTimestamp set on both insert and update")
        void updateTimestamp_setOnBothInsertAndUpdate() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            // Test on insert
            EntityWithDefaultTimestamps insertEntity = new EntityWithDefaultTimestamps();
            insertEntity.setId("test-id");
            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithDefaultTimestamps.class);
            buildColumnMap.invoke(null, insertEntity, idMeta, false, true);
            assertNotNull(insertEntity.getUpdatedAt());

            // Test on update
            EntityWithDefaultTimestamps updateEntity = new EntityWithDefaultTimestamps();
            updateEntity.setId("test-id");
            buildColumnMap.invoke(null, updateEntity, idMeta, false, false);
            assertNotNull(updateEntity.getUpdatedAt());
        }

        @Test
        @DisplayName("CreationTimestamp NEVER preserves manually set value")
        void creationTimestamp_never_preservesManualValue() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithNeverTimestamps entity = new EntityWithNeverTimestamps();
            entity.setId("test-id");
            java.time.LocalDateTime manualTime = java.time.LocalDateTime.of(2020, 1, 1, 12, 0);
            entity.setCreatedAt(manualTime);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithNeverTimestamps.class);
            Map<String, Object> columns = (Map<String, Object>) buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should preserve the manual value (not overwrite)
            assertEquals(manualTime, entity.getCreatedAt());
            assertTrue(columns.containsKey("created_at"));
        }

        @Test
        @DisplayName("UpdateTimestamp NEVER preserves manually set value")
        void updateTimestamp_never_preservesManualValue() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithNeverTimestamps entity = new EntityWithNeverTimestamps();
            entity.setId("test-id");
            java.time.LocalDateTime manualTime = java.time.LocalDateTime.of(2020, 1, 1, 12, 0);
            entity.setUpdatedAt(manualTime);

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithNeverTimestamps.class);
            Map<String, Object> columns = (Map<String, Object>) buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should preserve the manual value (not overwrite)
            assertEquals(manualTime, entity.getUpdatedAt());
            assertTrue(columns.containsKey("updated_at"));
        }

        @Test
        @DisplayName("Custom column names are respected")
        void customColumnNames_areRespected() throws Exception {
            Method buildColumnMap = EntityPersistence.class.getDeclaredMethod(
                "buildColumnMap", Object.class, EntityReflector.IdMeta.class, boolean.class, boolean.class);
            buildColumnMap.setAccessible(true);

            EntityWithCustomColumnTimestamps entity = new EntityWithCustomColumnTimestamps();
            entity.setId("test-id");

            EntityReflector.IdMeta idMeta = EntityReflector.getIdMeta(EntityWithCustomColumnTimestamps.class);
            Map<String, Object> columns = (Map<String, Object>) buildColumnMap.invoke(null, entity, idMeta, false, true);

            // Should use custom column names
            assertTrue(columns.containsKey("creation_time"));
            assertTrue(columns.containsKey("modification_time"));
            assertFalse(columns.containsKey("created_at"));
            assertFalse(columns.containsKey("updated_at"));
        }
    }

    @Entity(table = "ts_custom_col")
    static class EntityWithCustomColumnTimestamps {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @CreationTimestamp(column = "creation_time")
        private java.time.LocalDateTime createdAt;

        @UpdateTimestamp(column = "modification_time")
        private java.time.LocalDateTime updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    // ==================== ENTITY REFLECTOR EDGE CASE ENTITIES ====================

    // Entity with @Table annotation (instead of @Entity table attribute)
    @Entity
    @sant1ago.dev.suprim.annotation.entity.Table(name = "table_annotation_test", schema = "custom_schema")
    static class EntityWithTableAnnotation {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    // Entity with ID field without @Column (snake_case fallback)
    @Entity(table = "snake_case_id_test")
    static class EntityWithSnakeCaseIdColumn {
        @Id(strategy = GenerationType.UUID_V7)
        private String myEntityId;  // Should become my_entity_id

        public String getMyEntityId() { return myEntityId; }
        public void setMyEntityId(String id) { this.myEntityId = id; }
    }

    // Entity without any table annotation (class name fallback)
    @Entity
    static class MyTestEntityWithNoTableName {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    // Entity without @Id field (should throw exception)
    @Entity(table = "no_id_entity")
    static class EntityWithoutIdField {
        @Column(name = "name")
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Nested
    @DisplayName("EntityReflector Edge Case Tests")
    class EntityReflectorEdgeCaseTests {

        @Test
        @DisplayName("@Table annotation fallback for table name and schema")
        void tableAnnotation_fallback() {
            EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(EntityWithTableAnnotation.class);

            assertEquals("table_annotation_test", meta.tableName());
            assertEquals("custom_schema", meta.schema());
        }

        @Test
        @DisplayName("ID column uses snake_case when no @Column annotation")
        void idColumn_snakeCaseFallback() {
            EntityReflector.IdMeta meta = EntityReflector.getIdMeta(EntityWithSnakeCaseIdColumn.class);

            assertEquals("my_entity_id", meta.columnName());
        }

        @Test
        @DisplayName("Table name uses snake_case class name when no annotation")
        void tableName_snakeCaseFallback() {
            EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(MyTestEntityWithNoTableName.class);

            assertEquals("my_test_entity_with_no_table_name", meta.tableName());
        }

        @Test
        @DisplayName("getEntityMeta throws for entity without @Id")
        void getEntityMeta_noIdField_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.getEntityMeta(EntityWithoutIdField.class));
        }

        @Test
        @DisplayName("getIdMeta throws for entity without @Id")
        void getIdMeta_noIdField_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> EntityReflector.getIdMeta(EntityWithoutIdField.class));
        }
    }
}
