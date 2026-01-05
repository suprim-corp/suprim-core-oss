package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.CreationTimestamp;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.entity.TimestampAction;
import sant1ago.dev.suprim.annotation.entity.UpdateTimestamp;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.IdGenerator;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.jdbc.exception.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for UpsertPersistence upsert (INSERT ON CONFLICT) operations.
 */
@DisplayName("UpsertPersistence Tests")
@ExtendWith(MockitoExtension.class)
class UpsertPersistenceTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    // ==================== TEST ENTITIES ====================

    @Entity(table = "users")
    static class UserWithUuidV7 {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "email")
        private String email;

        @Column(name = "name")
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Entity(table = "users_v4")
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

    @Entity(table = "users_uuid")
    static class UserWithUuidField {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private UUID id;

        @Column(name = "email")
        private String email;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
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

        @Column(name = "name")
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Entity(table = "users_identity_int")
    static class UserWithIdentityInt {
        @Id(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private Integer id;

        @Column(name = "email")
        private String email;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @Entity(table = "posts")
    static class PostWithTimestamps {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "title")
        private String title;

        @CreationTimestamp(column = "created_at", onCreation = TimestampAction.NOW)
        private LocalDateTime createdAt;

        @UpdateTimestamp(column = "updated_at", onModification = TimestampAction.NOW)
        private LocalDateTime updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @Entity(table = "posts_instant")
    static class PostWithInstantTimestamp {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "title")
        private String title;

        @CreationTimestamp(column = "created_at", onCreation = TimestampAction.IF_NULL)
        private Instant createdAt;

        @UpdateTimestamp(column = "updated_at", onModification = TimestampAction.IF_NULL)
        private Instant updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }

    static class UpsertCountingGenerator implements IdGenerator<String> {
        private static int counter = 0;

        @Override
        public String generate() {
            return "upsert-custom-" + (++counter);
        }
    }

    @Entity(table = "items")
    static class ItemWithCustomGenerator {
        @Id(generator = UpsertCountingGenerator.class)
        @Column(name = "id")
        private String id;

        @Column(name = "name")
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
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

    @Entity(table = "users_uuid_type", schema = "public")
    static class UserWithUuidSqlType {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id", type = SqlType.UUID)
        private String id;

        @Column(name = "ref_id", type = SqlType.UUID)
        private String refId;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getRefId() { return refId; }
        public void setRefId(String refId) { this.refId = refId; }
    }

    public enum Status { ACTIVE, INACTIVE }

    @Entity(table = "users_enum")
    static class UserWithEnum {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "status")
        private Status status;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
    }

    // ==================== NULL VALIDATION TESTS ====================

    @Nested
    @DisplayName("Null Validation - upsert()")
    class NullValidationUpsertTests {

        @Test
        @DisplayName("throws NullPointerException for null entity")
        void throwsForNullEntity() {
            assertThrows(NullPointerException.class, () ->
                UpsertPersistence.upsert(null, mockConnection, PostgreSqlDialect.INSTANCE,
                    new String[]{"id"}, null));
        }

        @Test
        @DisplayName("throws NullPointerException for null connection")
        void throwsForNullConnection() {
            UserWithUuidV7 user = new UserWithUuidV7();
            assertThrows(NullPointerException.class, () ->
                UpsertPersistence.upsert(user, null, PostgreSqlDialect.INSTANCE,
                    new String[]{"id"}, null));
        }

        @Test
        @DisplayName("throws NullPointerException for null dialect")
        void throwsForNullDialect() {
            UserWithUuidV7 user = new UserWithUuidV7();
            assertThrows(NullPointerException.class, () ->
                UpsertPersistence.upsert(user, mockConnection, null,
                    new String[]{"id"}, null));
        }

        @Test
        @DisplayName("throws NullPointerException for null conflictColumns")
        void throwsForNullConflictColumns() {
            UserWithUuidV7 user = new UserWithUuidV7();
            assertThrows(NullPointerException.class, () ->
                UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                    null, null));
        }

        @Test
        @DisplayName("throws IllegalArgumentException for empty conflictColumns")
        void throwsForEmptyConflictColumns() {
            UserWithUuidV7 user = new UserWithUuidV7();
            assertThrows(IllegalArgumentException.class, () ->
                UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                    new String[]{}, null));
        }
    }

    // ==================== NULL VALIDATION TESTS - upsertAll ====================

    @Nested
    @DisplayName("Null Validation - upsertAll()")
    class NullValidationUpsertAllTests {

        @Test
        @DisplayName("returns empty list for null entities")
        void returnsEmptyForNull() {
            List<UserWithUuidV7> result = UpsertPersistence.upsertAll(null, mockConnection,
                PostgreSqlDialect.INSTANCE, new String[]{"id"}, null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty entities")
        void returnsEmptyForEmpty() {
            List<UserWithUuidV7> result = UpsertPersistence.upsertAll(new ArrayList<>(), mockConnection,
                PostgreSqlDialect.INSTANCE, new String[]{"id"}, null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("throws NullPointerException for null connection")
        void throwsForNullConnection() {
            List<UserWithUuidV7> users = List.of(new UserWithUuidV7());
            assertThrows(NullPointerException.class, () ->
                UpsertPersistence.upsertAll(users, null, PostgreSqlDialect.INSTANCE,
                    new String[]{"id"}, null));
        }

        @Test
        @DisplayName("throws NullPointerException for null dialect")
        void throwsForNullDialect() {
            List<UserWithUuidV7> users = List.of(new UserWithUuidV7());
            assertThrows(NullPointerException.class, () ->
                UpsertPersistence.upsertAll(users, mockConnection, null,
                    new String[]{"id"}, null));
        }

        @Test
        @DisplayName("throws NullPointerException for null conflictColumns")
        void throwsForNullConflictColumns() {
            List<UserWithUuidV7> users = List.of(new UserWithUuidV7());
            assertThrows(NullPointerException.class, () ->
                UpsertPersistence.upsertAll(users, mockConnection, PostgreSqlDialect.INSTANCE,
                    null, null));
        }

        @Test
        @DisplayName("throws IllegalArgumentException for empty conflictColumns")
        void throwsForEmptyConflictColumns() {
            List<UserWithUuidV7> users = List.of(new UserWithUuidV7());
            assertThrows(IllegalArgumentException.class, () ->
                UpsertPersistence.upsertAll(users, mockConnection, PostgreSqlDialect.INSTANCE,
                    new String[]{}, null));
        }
    }

    // ==================== ID GENERATION TESTS ====================

    @Nested
    @DisplayName("ID Generation")
    class IdGenerationTests {

        @Test
        @DisplayName("generates UUID_V7 ID for entity without ID")
        void generatesUuidV7Id() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithUuidV7 user = new UserWithUuidV7();
            user.setEmail("test@example.com");

            assertNull(user.getId());

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            assertNotNull(user.getId());
            assertTrue(user.getId().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        }

        @Test
        @DisplayName("generates UUID_V4 ID for entity without ID")
        void generatesUuidV4Id() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithUuidV4 user = new UserWithUuidV4();
            user.setEmail("test@example.com");

            assertNull(user.getId());

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            assertNotNull(user.getId());
            assertTrue(user.getId().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        }

        @Test
        @DisplayName("preserves existing ID")
        void preservesExistingId() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            String existingId = UUID.randomUUID().toString();
            UserWithUuidV7 user = new UserWithUuidV7();
            user.setId(existingId);
            user.setEmail("test@example.com");

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            assertEquals(existingId, user.getId());
        }

        @Test
        @DisplayName("generates UUID for UUID type field")
        void generatesUuidForUuidField() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithUuidField user = new UserWithUuidField();
            user.setEmail("test@example.com");

            assertNull(user.getId());

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            assertNotNull(user.getId());
            assertInstanceOf(UUID.class, user.getId());
        }

        @Test
        @DisplayName("uses custom generator when specified")
        void usesCustomGenerator() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ItemWithCustomGenerator item = new ItemWithCustomGenerator();
            item.setName("Test Item");

            UpsertPersistence.upsert(item, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            assertNotNull(item.getId());
            assertTrue(item.getId().startsWith("upsert-custom-"));
        }
    }

    // ==================== DATABASE GENERATED ID TESTS ====================

    @Nested
    @DisplayName("Database Generated IDs")
    class DatabaseGeneratedIdTests {

        @Test
        @DisplayName("retrieves IDENTITY ID from RETURNING (PostgreSQL)")
        void retrievesIdentityIdPostgres() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getObject(1)).thenReturn(100L);

            UserWithIdentity user = new UserWithIdentity();
            user.setEmail("test@example.com");

            UserWithIdentity result = UpsertPersistence.upsert(user, mockConnection,
                PostgreSqlDialect.INSTANCE, new String[]{"email"}, new String[]{"name"});

            assertEquals(100L, result.getId());
        }

        @Test
        @DisplayName("retrieves Integer IDENTITY ID from RETURNING")
        void retrievesIntegerIdentityId() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getObject(1)).thenReturn(42L);

            UserWithIdentityInt user = new UserWithIdentityInt();
            user.setEmail("test@example.com");

            UserWithIdentityInt result = UpsertPersistence.upsert(user, mockConnection,
                PostgreSqlDialect.INSTANCE, new String[]{"email"}, null);

            assertEquals(42, result.getId());
        }

        @Test
        @DisplayName("handles MySQL without RETURNING")
        void handlesMysqlWithoutReturning() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithIdentity user = new UserWithIdentity();
            user.setEmail("test@example.com");

            UserWithIdentity result = UpsertPersistence.upsert(user, mockConnection,
                MySqlDialect.INSTANCE, new String[]{"email"}, new String[]{"name"});

            // MySQL doesn't support RETURNING, so ID remains null
            assertNull(result.getId());
        }
    }

    // ==================== SQL GENERATION TESTS ====================

    @Nested
    @DisplayName("SQL Generation - PostgreSQL")
    class PostgresqlSqlGenerationTests {

        @Test
        @DisplayName("generates ON CONFLICT DO UPDATE SET for specified columns")
        void generatesOnConflictDoUpdate() throws SQLException {
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithUuidV7 user = new UserWithUuidV7();
            user.setId(UUID.randomUUID().toString());
            user.setEmail("test@example.com");
            user.setName("Test");

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, new String[]{"email", "name"});

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("ON CONFLICT"));
            assertTrue(sql.contains("DO UPDATE SET"));
            assertTrue(sql.contains("EXCLUDED"));
        }

        @Test
        @DisplayName("generates ON CONFLICT DO NOTHING when all columns are conflict columns")
        void generatesOnConflictDoNothing() throws SQLException {
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Use entity with only id and email, both as conflict columns
            UserWithUuidV7 user = new UserWithUuidV7();
            user.setId(UUID.randomUUID().toString());
            user.setEmail("test@example.com");
            user.setName("Test");

            // Pass all non-id columns as conflict columns to trigger DO NOTHING
            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id", "email", "name"}, null);

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("ON CONFLICT"));
            assertTrue(sql.contains("DO NOTHING"));
        }

        @Test
        @DisplayName("updates all non-conflict columns when updateColumns is null")
        void updatesAllNonConflictColumns() throws SQLException {
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithUuidV7 user = new UserWithUuidV7();
            user.setId(UUID.randomUUID().toString());
            user.setEmail("test@example.com");
            user.setName("Test");

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("DO UPDATE SET"));
            // Should update email and name (non-conflict columns)
            assertTrue(sql.contains("\"email\"") || sql.contains("email"));
            assertTrue(sql.contains("\"name\"") || sql.contains("name"));
        }

        @Test
        @DisplayName("handles schema in table name")
        void handlesSchemaInTableName() throws SQLException {
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithUuidSqlType user = new UserWithUuidSqlType();
            user.setRefId(UUID.randomUUID().toString());

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("\"public\"") || sql.contains("public."));
        }
    }

    @Nested
    @DisplayName("SQL Generation - MySQL")
    class MysqlSqlGenerationTests {

        @Test
        @DisplayName("generates ON DUPLICATE KEY UPDATE")
        void generatesOnDuplicateKeyUpdate() throws SQLException {
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithUuidV7 user = new UserWithUuidV7();
            user.setId(UUID.randomUUID().toString());
            user.setEmail("test@example.com");
            user.setName("Test");

            UpsertPersistence.upsert(user, mockConnection, MySqlDialect.INSTANCE,
                new String[]{"id"}, new String[]{"email", "name"});

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
            assertTrue(sql.contains("VALUES("));
        }

        @Test
        @DisplayName("generates INSERT IGNORE when all columns are conflict columns")
        void generatesInsertIgnore() throws SQLException {
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Use entity with all columns as conflict columns (no update columns)
            UserWithUuidV7 user = new UserWithUuidV7();
            user.setId(UUID.randomUUID().toString());
            user.setEmail("test@example.com");
            user.setName("Test");

            // Pass all columns as conflict to trigger INSERT IGNORE (no updates)
            UpsertPersistence.upsert(user, mockConnection, MySqlDialect.INSTANCE,
                new String[]{"id", "email", "name"}, null);

            String sql = sqlCaptor.getValue();
            assertTrue(sql.contains("INSERT IGNORE INTO"));
        }
    }

    // ==================== TIMESTAMP TESTS ====================

    @Nested
    @DisplayName("Timestamp Handling")
    class TimestampTests {

        @Test
        @DisplayName("sets CreationTimestamp and UpdateTimestamp on upsert")
        void setsTimestamps() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            PostWithTimestamps post = new PostWithTimestamps();
            post.setTitle("Test Post");

            assertNull(post.getCreatedAt());
            assertNull(post.getUpdatedAt());

            UpsertPersistence.upsert(post, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            assertNotNull(post.getCreatedAt());
            assertNotNull(post.getUpdatedAt());
        }

        @Test
        @DisplayName("sets timestamps with IF_NULL action only when null")
        void setsTimestampsIfNull() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            Instant existingCreated = Instant.now().minusSeconds(3600);
            PostWithInstantTimestamp post = new PostWithInstantTimestamp();
            post.setTitle("Test Post");
            post.setCreatedAt(existingCreated);

            UpsertPersistence.upsert(post, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            // createdAt should remain unchanged (IF_NULL and value exists)
            assertEquals(existingCreated, post.getCreatedAt());
            // updatedAt should be set (was null)
            assertNotNull(post.getUpdatedAt());
        }
    }

    // ==================== BATCH UPSERT TESTS ====================

    @Nested
    @DisplayName("Batch Upsert - upsertAll()")
    class BatchUpsertTests {

        @Test
        @DisplayName("upserts multiple entities")
        void upsertsMultipleEntities() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(2);

            UserWithUuidV7 user1 = new UserWithUuidV7();
            user1.setEmail("a@example.com");
            user1.setName("User A");

            UserWithUuidV7 user2 = new UserWithUuidV7();
            user2.setEmail("b@example.com");
            user2.setName("User B");

            List<UserWithUuidV7> result = UpsertPersistence.upsertAll(List.of(user1, user2),
                mockConnection, PostgreSqlDialect.INSTANCE, new String[]{"id"}, null);

            assertEquals(2, result.size());
            assertNotNull(result.get(0).getId());
            assertNotNull(result.get(1).getId());
            assertNotEquals(result.get(0).getId(), result.get(1).getId());
        }

        @Test
        @DisplayName("generates IDs for all entities in batch")
        void generatesIdsForAllEntities() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(3);

            List<UserWithUuidV7> users = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                UserWithUuidV7 user = new UserWithUuidV7();
                user.setEmail("user" + i + "@example.com");
                users.add(user);
            }

            UpsertPersistence.upsertAll(users, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            for (UserWithUuidV7 user : users) {
                assertNotNull(user.getId());
            }
        }

        @Test
        @DisplayName("retrieves IDENTITY IDs from RETURNING for batch (PostgreSQL)")
        void retrievesIdentityIdsForBatchPostgres() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getObject(1)).thenReturn(100L, 101L);

            UserWithIdentity user1 = new UserWithIdentity();
            user1.setEmail("a@example.com");

            UserWithIdentity user2 = new UserWithIdentity();
            user2.setEmail("b@example.com");

            List<UserWithIdentity> result = UpsertPersistence.upsertAll(List.of(user1, user2),
                mockConnection, PostgreSqlDialect.INSTANCE, new String[]{"email"}, new String[]{"name"});

            assertEquals(2, result.size());
            assertEquals(100L, result.get(0).getId());
            assertEquals(101L, result.get(1).getId());
        }

        @Test
        @DisplayName("generates batch SQL with multiple value rows")
        void generatesBatchSqlWithMultipleRows() throws SQLException {
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            when(mockConnection.prepareStatement(sqlCaptor.capture())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(3);

            List<UserWithUuidV7> users = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                UserWithUuidV7 user = new UserWithUuidV7();
                user.setId(UUID.randomUUID().toString());
                user.setEmail("user" + i + "@example.com");
                users.add(user);
            }

            UpsertPersistence.upsertAll(users, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            String sql = sqlCaptor.getValue();
            // Should have 3 value placeholders
            int valueCount = sql.split("\\(\\?, \\?, \\?\\)").length - 1;
            assertEquals(3, valueCount);
        }
    }

    // ==================== ENUM HANDLING TESTS ====================

    @Nested
    @DisplayName("Enum Handling")
    class EnumHandlingTests {

        @Test
        @DisplayName("converts enum to string for parameter")
        void convertsEnumToString() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithEnum user = new UserWithEnum();
            user.setStatus(Status.ACTIVE);

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            // Verify setString was called for enum
            verify(mockPreparedStatement).setString(anyInt(), eq("ACTIVE"));
        }
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("wraps SQLException in PersistenceException for upsert")
        void wrapsSqlExceptionForUpsert() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

            UserWithUuidV7 user = new UserWithUuidV7();
            user.setEmail("test@example.com");

            PersistenceException ex = assertThrows(PersistenceException.class, () ->
                UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                    new String[]{"id"}, null));

            assertTrue(ex.getMessage().contains("Database error"));
        }

        @Test
        @DisplayName("wraps SQLException in PersistenceException for upsertAll")
        void wrapsSqlExceptionForUpsertAll() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Batch error"));

            List<UserWithUuidV7> users = List.of(new UserWithUuidV7());

            PersistenceException ex = assertThrows(PersistenceException.class, () ->
                UpsertPersistence.upsertAll(users, mockConnection, PostgreSqlDialect.INSTANCE,
                    new String[]{"id"}, null));

            assertTrue(ex.getMessage().contains("Batch error"));
        }
    }

    // ==================== ID TYPE CONVERSION TESTS ====================

    @Nested
    @DisplayName("ID Type Conversion")
    class IdTypeConversionTests {

        @Test
        @DisplayName("handles UUID string conversion for SqlType.UUID")
        void handlesUuidStringConversion() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            String uuidString = UUID.randomUUID().toString();
            UserWithUuidSqlType user = new UserWithUuidSqlType();
            user.setId(uuidString);
            user.setRefId(UUID.randomUUID().toString());

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            // Should convert String UUID to UUID object for SqlType.UUID columns
            verify(mockPreparedStatement, atLeastOnce()).setObject(anyInt(), any());
        }
    }

    // ==================== MANUAL ID STRATEGY TESTS ====================

    @Nested
    @DisplayName("Manual ID Strategy")
    class ManualIdStrategyTests {

        @Test
        @DisplayName("does not generate ID for manual strategy")
        void doesNotGenerateIdForManual() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithManualId user = new UserWithManualId();
            user.setId(42L);
            user.setEmail("test@example.com");

            UpsertPersistence.upsert(user, mockConnection, PostgreSqlDialect.INSTANCE,
                new String[]{"id"}, null);

            assertEquals(42L, user.getId());
        }
    }
}
