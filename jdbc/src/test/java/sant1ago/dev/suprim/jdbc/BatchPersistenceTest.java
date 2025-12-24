package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.jdbc.exception.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for BatchPersistence batch insert operations.
 */
@DisplayName("BatchPersistence Tests")
@ExtendWith(MockitoExtension.class)
class BatchPersistenceTest {

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

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
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

    static class CountingGenerator implements IdGenerator<String> {
        private static int counter = 0;

        @Override
        public String generate() {
            return "custom-" + (++counter);
        }
    }

    @Entity(table = "items")
    static class ItemWithCustomGenerator {
        @Id(generator = CountingGenerator.class)
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

    // ==================== EMPTY/NULL INPUT TESTS ====================

    @Nested
    @DisplayName("Empty and Null Input Handling")
    class EmptyNullInputTests {

        @Test
        @DisplayName("saveAll returns empty list for null input")
        void saveAllReturnsEmptyForNull() {
            List<UserWithUuidV7> result = BatchPersistence.saveAll(null, mockConnection, PostgreSqlDialect.INSTANCE);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("saveAll returns empty list for empty input")
        void saveAllReturnsEmptyForEmpty() {
            List<UserWithUuidV7> result = BatchPersistence.saveAll(new ArrayList<>(), mockConnection, PostgreSqlDialect.INSTANCE);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("saveAll throws for null connection")
        void saveAllThrowsForNullConnection() {
            List<UserWithUuidV7> users = List.of(new UserWithUuidV7());

            assertThrows(NullPointerException.class, () ->
                BatchPersistence.saveAll(users, null, PostgreSqlDialect.INSTANCE));
        }

        @Test
        @DisplayName("saveAll throws for null dialect")
        void saveAllThrowsForNullDialect() {
            List<UserWithUuidV7> users = List.of(new UserWithUuidV7());

            assertThrows(NullPointerException.class, () ->
                BatchPersistence.saveAll(users, mockConnection, null));
        }
    }

    // ==================== ID GENERATION TESTS ====================

    @Nested
    @DisplayName("ID Generation")
    class IdGenerationTests {

        @Test
        @DisplayName("generates UUID_V7 IDs for entities without ID")
        void generatesUuidV7Ids() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(2);

            UserWithUuidV7 user1 = new UserWithUuidV7();
            user1.setEmail("a@example.com");

            UserWithUuidV7 user2 = new UserWithUuidV7();
            user2.setEmail("b@example.com");

            List<UserWithUuidV7> users = List.of(user1, user2);

            assertNull(user1.getId());
            assertNull(user2.getId());

            BatchPersistence.saveAll(users, mockConnection, PostgreSqlDialect.INSTANCE);

            // IDs should be generated
            assertNotNull(user1.getId());
            assertNotNull(user2.getId());
            assertNotEquals(user1.getId(), user2.getId());

            // Should be valid UUID format
            assertTrue(user1.getId().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        }

        @Test
        @DisplayName("preserves existing IDs")
        void preservesExistingIds() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            String existingId = UUID.randomUUID().toString();
            UserWithUuidV7 user = new UserWithUuidV7();
            user.setId(existingId);
            user.setEmail("test@example.com");

            BatchPersistence.saveAll(List.of(user), mockConnection, PostgreSqlDialect.INSTANCE);

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

            BatchPersistence.saveAll(List.of(user), mockConnection, PostgreSqlDialect.INSTANCE);

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

            BatchPersistence.saveAll(List.of(item), mockConnection, PostgreSqlDialect.INSTANCE);

            assertNotNull(item.getId());
            assertTrue(item.getId().startsWith("custom-"));
        }
    }

    // ==================== DATABASE GENERATED ID TESTS ====================

    @Nested
    @DisplayName("Database Generated IDs")
    class DatabaseGeneratedIdTests {

        @Test
        @DisplayName("retrieves IDENTITY IDs from RETURNING (PostgreSQL)")
        void retrievesIdentityIdsPostgres() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getObject(1)).thenReturn(100L, 101L);

            UserWithIdentity user1 = new UserWithIdentity();
            user1.setEmail("a@example.com");

            UserWithIdentity user2 = new UserWithIdentity();
            user2.setEmail("b@example.com");

            List<UserWithIdentity> result = BatchPersistence.saveAll(List.of(user1, user2), mockConnection, PostgreSqlDialect.INSTANCE);

            assertEquals(2, result.size());
            assertEquals(100L, result.get(0).getId());
            assertEquals(101L, result.get(1).getId());
        }

        @Test
        @DisplayName("retrieves IDENTITY IDs from GENERATED_KEYS (MySQL)")
        void retrievesIdentityIdsMysql() throws SQLException {
            when(mockConnection.prepareStatement(anyString(), eq(java.sql.Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(2);
            when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getObject(1)).thenReturn(1L, 2L);

            UserWithIdentity user1 = new UserWithIdentity();
            user1.setEmail("a@example.com");

            UserWithIdentity user2 = new UserWithIdentity();
            user2.setEmail("b@example.com");

            List<UserWithIdentity> result = BatchPersistence.saveAll(List.of(user1, user2), mockConnection, MySqlDialect.INSTANCE);

            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals(2L, result.get(1).getId());
        }
    }

    // ==================== TIMESTAMP TESTS ====================

    @Nested
    @DisplayName("Timestamp Handling")
    class TimestampTests {

        @Test
        @DisplayName("sets CreationTimestamp on save")
        void setsCreationTimestamp() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            PostWithTimestamps post = new PostWithTimestamps();
            post.setTitle("Test Post");

            assertNull(post.getCreatedAt());
            assertNull(post.getUpdatedAt());

            BatchPersistence.saveAll(List.of(post), mockConnection, PostgreSqlDialect.INSTANCE);

            assertNotNull(post.getCreatedAt());
            assertNotNull(post.getUpdatedAt());
        }
    }

    // ==================== BATCH SIZE TESTS ====================

    @Nested
    @DisplayName("Batch Size Handling")
    class BatchSizeTests {

        @Test
        @DisplayName("processes large batches in chunks")
        void processesLargeBatchesInChunks() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(100);

            // Create 150 entities to test chunking (default batch size is 500)
            List<UserWithUuidV7> users = new ArrayList<>();
            for (int i = 0; i < 150; i++) {
                UserWithUuidV7 user = new UserWithUuidV7();
                user.setEmail("user" + i + "@example.com");
                users.add(user);
            }

            List<UserWithUuidV7> result = BatchPersistence.saveAll(users, mockConnection, PostgreSqlDialect.INSTANCE, 100);

            assertEquals(150, result.size());
            // Should have been called twice (100 + 50)
            verify(mockPreparedStatement, times(2)).executeUpdate();
        }

        @Test
        @DisplayName("respects custom batch size")
        void respectsCustomBatchSize() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(10);

            List<UserWithUuidV7> users = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                UserWithUuidV7 user = new UserWithUuidV7();
                user.setEmail("user" + i + "@example.com");
                users.add(user);
            }

            BatchPersistence.saveAll(users, mockConnection, PostgreSqlDialect.INSTANCE, 10);

            // Should have been called 3 times (10 + 10 + 5)
            verify(mockPreparedStatement, times(3)).executeUpdate();
        }

        @Test
        @DisplayName("clamps batch size to max 1000")
        void clampsBatchSizeToMax() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            List<UserWithUuidV7> users = new ArrayList<>();
            for (int i = 0; i < 1500; i++) {
                UserWithUuidV7 user = new UserWithUuidV7();
                user.setEmail("user" + i + "@example.com");
                users.add(user);
            }

            // Request batch size of 2000 (should be clamped to 1000)
            BatchPersistence.saveAll(users, mockConnection, PostgreSqlDialect.INSTANCE, 2000);

            // Should have been called twice (1000 + 500)
            verify(mockPreparedStatement, times(2)).executeUpdate();
        }
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("wraps SQLException in PersistenceException")
        void wrapsSqlExceptionInPersistenceException() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

            UserWithUuidV7 user = new UserWithUuidV7();
            user.setEmail("test@example.com");

            PersistenceException ex = assertThrows(PersistenceException.class, () ->
                BatchPersistence.saveAll(List.of(user), mockConnection, PostgreSqlDialect.INSTANCE));

            assertTrue(ex.getMessage().contains("Database error"));
        }
    }

    // ==================== MANUAL ID TESTS ====================

    @Nested
    @DisplayName("Manual ID Strategy")
    class ManualIdTests {

        @Test
        @DisplayName("does not generate ID for manual strategy")
        void doesNotGenerateIdForManual() throws SQLException {
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            UserWithManualId user = new UserWithManualId();
            user.setId(42L);
            user.setEmail("test@example.com");

            BatchPersistence.saveAll(List.of(user), mockConnection, PostgreSqlDialect.INSTANCE);

            assertEquals(42L, user.getId());
        }
    }
}
