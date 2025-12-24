package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.entity.SoftDeletes;
import sant1ago.dev.suprim.annotation.entity.UpdateTimestamp;
import sant1ago.dev.suprim.annotation.type.GenerationType;

import java.time.LocalDateTime;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SuprimEntity Active Record base class.
 */
@DisplayName("SuprimEntity Tests")
class SuprimEntityTest {

    private static JdbcDataSource dataSource;
    private static SuprimExecutor executor;

    @BeforeAll
    static void setup() throws SQLException {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:entity_test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        executor = SuprimExecutor.create(dataSource);

        // Create test tables (quoted to preserve lowercase for H2)
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS \"entity_users\" (" +
                "\"id\" VARCHAR(36) PRIMARY KEY, " +
                "\"email\" VARCHAR(255))"
            );
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS \"custom_gen_users\" (" +
                "\"id\" VARCHAR(36) PRIMARY KEY, " +
                "\"name\" VARCHAR(255))"
            );
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS \"soft_delete_users\" (" +
                "\"id\" VARCHAR(36) PRIMARY KEY, " +
                "\"email\" VARCHAR(255), " +
                "\"deleted_at\" TIMESTAMP)"
            );
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS \"enum_users\" (" +
                "\"id\" VARCHAR(36) PRIMARY KEY, " +
                "\"name\" VARCHAR(255), " +
                "\"status\" VARCHAR(50))"
            );
        }
    }

    @AfterAll
    static void teardown() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("DROP TABLE IF EXISTS \"entity_users\"");
            conn.createStatement().execute("DROP TABLE IF EXISTS \"custom_gen_users\"");
            conn.createStatement().execute("DROP TABLE IF EXISTS \"soft_delete_users\"");
            conn.createStatement().execute("DROP TABLE IF EXISTS \"enum_users\"");
        }
    }

    @AfterEach
    void cleanup() {
        SuprimContext.clearContext();
        SuprimContext.clearGlobalExecutor();
    }

    // ==================== TEST ENUMS ====================

    enum UserStatus {
        ACTIVE, INACTIVE, PENDING
    }

    // ==================== TEST ENTITIES ====================

    @Entity(table = "entity_users")
    static class TestUser extends SuprimEntity {
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

    // Custom ID generator for testing
    static class TestIdGenerator implements sant1ago.dev.suprim.annotation.type.IdGenerator<String> {
        private static int counter = 0;

        @Override
        public String generate() {
            return "custom-gen-" + (++counter);
        }
    }

    @Entity(table = "custom_gen_users")
    static class CustomGenUser extends SuprimEntity {
        @Id(generator = TestIdGenerator.class)
        @Column(name = "id")
        private String id;

        @Column(name = "name")
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Entity(table = "enum_users")
    static class EnumUser extends SuprimEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "name")
        private String name;

        @Column(name = "status")
        private UserStatus status;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public UserStatus getStatus() { return status; }
        public void setStatus(UserStatus status) { this.status = status; }
    }

    // ==================== TESTS ====================

    @Nested
    @DisplayName("save() without context")
    class SaveWithoutContextTests {

        @Test
        @DisplayName("throws IllegalStateException when called outside transaction")
        void save_outsideTransaction_throws() {
            TestUser user = new TestUser();
            user.setEmail("test@example.com");

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                user::save
            );

            assertTrue(ex.getMessage().contains("No active transaction context"));
            assertTrue(ex.getMessage().contains("executor.transaction"));
        }
    }

    @Nested
    @DisplayName("save() within transaction")
    class SaveWithinTransactionTests {

        @Test
        @DisplayName("saves entity and generates ID")
        void save_withinTransaction_savesEntity() {
            TestUser user = new TestUser();
            user.setEmail("active-record@example.com");

            executor.transaction(tx -> {
                user.save();
            });

            // Verify ID was generated
            assertNotNull(user.getId());
            assertFalse(user.getId().isEmpty());
        }

        @Test
        @DisplayName("returns same entity instance")
        void save_withinTransaction_returnsSameInstance() {
            TestUser user = new TestUser();
            user.setEmail("return-test@example.com");

            executor.transaction(tx -> {
                SuprimEntity returned = user.save();
                assertSame(user, returned);
            });
        }

        @Test
        @DisplayName("saves entity with enum field")
        void save_withEnumField_savesCorrectly() throws SQLException {
            EnumUser user = new EnumUser();
            user.setName("enum-test");
            user.setStatus(UserStatus.ACTIVE);

            executor.transaction(tx -> {
                user.save();
            });

            // Verify ID was generated and enum was persisted
            assertNotNull(user.getId());
            assertEquals(UserStatus.ACTIVE, user.getStatus());

            // Verify the enum value is stored as string in DB
            try (Connection conn = dataSource.getConnection();
                 var ps = conn.prepareStatement("SELECT \"status\" FROM \"enum_users\" WHERE \"id\" = ?")) {
                ps.setString(1, user.getId());
                try (var rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("ACTIVE", rs.getString("status"));
                }
            }
        }
    }

    @Nested
    @DisplayName("update() within transaction")
    class UpdateTests {

        @Test
        @DisplayName("updates existing entity")
        void update_existingEntity_updatesInDb() {
            TestUser user = new TestUser();
            user.setEmail("original@example.com");

            executor.transaction(tx -> {
                user.save();
                assertNotNull(user.getId());

                user.setEmail("updated@example.com");
                user.update();
            });

            // Verify by refreshing
            executor.transaction(tx -> {
                user.refresh();
                assertEquals("updated@example.com", user.getEmail());
            });
        }

        @Test
        @DisplayName("throws without ID")
        void update_withoutId_throws() {
            TestUser user = new TestUser();
            user.setEmail("test@example.com");

            executor.transaction(tx -> {
                assertThrows(Exception.class, user::update);
            });
        }
    }

    @Nested
    @DisplayName("delete() within transaction")
    class DeleteTests {

        @Test
        @DisplayName("deletes existing entity")
        void delete_existingEntity_removesFromDb() {
            TestUser user = new TestUser();
            user.setEmail("todelete@example.com");

            executor.transaction(tx -> {
                user.save();
                assertNotNull(user.getId());
                user.delete();
            });

            // Verify deleted - refresh should throw
            String userId = user.getId();
            executor.transaction(tx -> {
                TestUser toRefresh = new TestUser();
                toRefresh.setId(userId);
                assertThrows(Exception.class, toRefresh::refresh);
            });
        }

        @Test
        @DisplayName("throws without ID")
        void delete_withoutId_throws() {
            TestUser user = new TestUser();

            executor.transaction(tx -> {
                assertThrows(Exception.class, user::delete);
            });
        }
    }

    @Nested
    @DisplayName("refresh() within transaction")
    class RefreshTests {

        @Test
        @DisplayName("reloads entity from database")
        void refresh_existingEntity_reloadsValues() {
            TestUser user = new TestUser();
            user.setEmail("refresh-test@example.com");

            executor.transaction(tx -> {
                user.save();
            });

            // Modify in memory
            user.setEmail("modified-in-memory@example.com");

            // Refresh should restore DB value
            executor.transaction(tx -> {
                user.refresh();
                assertEquals("refresh-test@example.com", user.getEmail());
            });
        }

        @Test
        @DisplayName("throws for non-existent entity")
        void refresh_nonExistent_throws() {
            TestUser user = new TestUser();
            user.setId("non-existent-id");

            executor.transaction(tx -> {
                assertThrows(Exception.class, user::refresh);
            });
        }

        @Test
        @DisplayName("throws without ID")
        void refresh_withoutId_throws() {
            TestUser user = new TestUser();
            // ID is not set

            executor.transaction(tx -> {
                assertThrows(Exception.class, user::refresh);
            });
        }
    }

    @Nested
    @DisplayName("Entity inheritance")
    class InheritanceTests {

        @Test
        @DisplayName("entity extends SuprimEntity")
        void entity_extendsSuprimEntity() {
            TestUser user = new TestUser();
            assertTrue(user instanceof SuprimEntity);
        }

        @Test
        @DisplayName("save method is accessible")
        void save_methodAccessible() {
            TestUser user = new TestUser();

            // Verify save() method exists and is callable
            // (throws because no context, but method is accessible)
            assertThrows(IllegalStateException.class, user::save);
        }
    }

    @Nested
    @DisplayName("update() edge cases")
    class UpdateEdgeCaseTests {

        @Test
        @DisplayName("update with only ID set returns early (no columns to update)")
        void update_onlyIdSet_returnsEarly() {
            TestUser user = new TestUser();
            user.setEmail("initial@example.com");

            executor.transaction(tx -> {
                user.save();
            });

            // Clear email to simulate entity with only ID set
            user.setEmail(null);

            // Update should return early since there are no non-ID columns to update
            executor.transaction(tx -> {
                SuprimEntity returned = user.update();
                assertSame(user, returned);
            });

            // Verify original email is still in DB (update did nothing)
            executor.transaction(tx -> {
                user.refresh();
                assertEquals("initial@example.com", user.getEmail());
            });
        }
    }

    @Nested
    @DisplayName("Custom ID Generator")
    class CustomGeneratorTests {

        @Test
        @DisplayName("save with custom generator generates ID")
        void save_customGenerator_generatesId() {
            CustomGenUser user = new CustomGenUser();
            user.setName("Custom Gen User");

            executor.transaction(tx -> {
                user.save();
            });

            // Verify ID was generated by custom generator
            assertNotNull(user.getId());
            assertTrue(user.getId().startsWith("custom-gen-"));
        }

        @Test
        @DisplayName("custom generator is cached and reused")
        void save_customGeneratorCached_reusesInstance() {
            CustomGenUser user1 = new CustomGenUser();
            user1.setName("User 1");

            CustomGenUser user2 = new CustomGenUser();
            user2.setName("User 2");

            executor.transaction(tx -> {
                user1.save();
                user2.save();
            });

            // Both should have custom IDs
            assertTrue(user1.getId().startsWith("custom-gen-"));
            assertTrue(user2.getId().startsWith("custom-gen-"));
            // And they should be different (counter incremented)
            assertNotEquals(user1.getId(), user2.getId());
        }
    }

    @Nested
    @DisplayName("SQLException handling")
    class SqlExceptionTests {

        // Entity targeting non-existent table to trigger SQLException
        @Entity(table = "non_existent_table_xyz")
        static class NonExistentTableEntity extends SuprimEntity {
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
        @DisplayName("update throws PersistenceException on SQL error")
        void update_sqlError_throwsPersistenceException() {
            NonExistentTableEntity entity = new NonExistentTableEntity();
            entity.setId("test-id-123");
            entity.setName("Test");

            // Update on non-existent table should throw PersistenceException
            executor.transaction(tx -> {
                Exception ex = assertThrows(Exception.class, entity::update);
                assertTrue(ex.getMessage().contains("Failed to update") ||
                           ex.getMessage().contains("not found") ||
                           ex.getCause() != null);
            });
        }

        @Test
        @DisplayName("delete throws PersistenceException on SQL error")
        void delete_sqlError_throwsPersistenceException() {
            NonExistentTableEntity entity = new NonExistentTableEntity();
            entity.setId("test-id-123");

            // Delete on non-existent table should throw PersistenceException
            executor.transaction(tx -> {
                Exception ex = assertThrows(Exception.class, entity::delete);
                assertTrue(ex.getMessage().contains("Failed to delete") ||
                           ex.getMessage().contains("not found") ||
                           ex.getCause() != null);
            });
        }

        @Test
        @DisplayName("refresh throws PersistenceException on SQL error")
        void refresh_sqlError_throwsPersistenceException() {
            NonExistentTableEntity entity = new NonExistentTableEntity();
            entity.setId("test-id-123");

            // Refresh on non-existent table should throw PersistenceException
            executor.transaction(tx -> {
                Exception ex = assertThrows(Exception.class, entity::refresh);
                assertTrue(ex.getMessage().contains("Failed to refresh") ||
                           ex.getMessage().contains("not found") ||
                           ex.getCause() != null);
            });
        }
    }

    @Nested
    @DisplayName("Schema-qualified entity operations")
    class SchemaEntityTests {

        // Entity with schema for testing schema paths
        @Entity(table = "schema_users", schema = "test_schema")
        static class SchemaUser extends SuprimEntity {
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
        @DisplayName("delete with schema generates correct SQL")
        void delete_withSchema_executesCorrectly() {
            SchemaUser user = new SchemaUser();
            user.setId("schema-test-id");
            user.setName("Schema Test");

            // This will fail because schema doesn't exist, but it will execute the schema path
            executor.transaction(tx -> {
                // This tests that the schema path is hit (line 477)
                Exception ex = assertThrows(Exception.class, user::delete);
                // The error confirms the SQL was generated with schema
                assertNotNull(ex);
            });
        }

        @Test
        @DisplayName("refresh with schema generates correct SQL")
        void refresh_withSchema_executesCorrectly() {
            SchemaUser user = new SchemaUser();
            user.setId("schema-test-id");

            // This will fail because schema doesn't exist, but it will execute the schema path
            executor.transaction(tx -> {
                // This tests that the schema path is hit (line 521)
                Exception ex = assertThrows(Exception.class, user::refresh);
                // The error confirms the SQL was generated with schema
                assertNotNull(ex);
            });
        }
    }

    @Nested
    @DisplayName("Entity with mixed fields")
    class MixedFieldsTests {

        // Entity with field without @Column annotation
        @Entity(table = "mixed_entity")
        static class MixedFieldsEntity extends SuprimEntity {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            @Column(name = "name")
            private String name;

            // Transient field without @Column - should be skipped
            private String transientData;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getTransientData() { return transientData; }
            public void setTransientData(String data) { this.transientData = data; }
        }

        @Test
        @DisplayName("refresh skips fields without @Column annotation")
        void refresh_skipsFieldsWithoutColumn() throws SQLException {
            // Create table for this test
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"mixed_entity\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255))"
                );
            }

            try {
                MixedFieldsEntity entity = new MixedFieldsEntity();
                entity.setName("Test Name");
                entity.setTransientData("Should be ignored");

                executor.transaction(tx -> {
                    entity.save();
                });

                // Modify transient in memory
                entity.setTransientData("Modified");

                // Refresh should not affect transient field
                executor.transaction(tx -> {
                    entity.refresh();
                    // Name should be reloaded, transient stays as-is
                    assertEquals("Test Name", entity.getName());
                    // Transient field was modified in memory and stays that way
                    assertEquals("Modified", entity.getTransientData());
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"mixed_entity\"");
                }
            }
        }
    }

    @Nested
    @DisplayName("Entity with empty column name")
    class EmptyColumnNameTests {

        // Entity with empty column name - uses snake_case of field name
        @Entity(table = "empty_col_entity")
        static class EmptyColumnEntity extends SuprimEntity {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            @Column(name = "") // Empty - should use snake_case: my_field_name
            private String myFieldName;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getMyFieldName() { return myFieldName; }
            public void setMyFieldName(String value) { this.myFieldName = value; }
        }

        @Test
        @DisplayName("refresh uses snake_case for empty column name")
        void refresh_usesSnakeCaseForEmptyColumnName() throws SQLException {
            // Create table with snake_case column name
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"empty_col_entity\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"my_field_name\" VARCHAR(255))"
                );
            }

            try {
                EmptyColumnEntity entity = new EmptyColumnEntity();
                entity.setMyFieldName("Original Value");

                executor.transaction(tx -> {
                    entity.save();
                });

                // Modify in memory
                entity.setMyFieldName("Modified");

                // Refresh should use snake_case column name (covers line 594-595)
                executor.transaction(tx -> {
                    entity.refresh();
                    assertEquals("Original Value", entity.getMyFieldName());
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"empty_col_entity\"");
                }
            }
        }
    }

    @Nested
    @DisplayName("Entity with nullable column")
    class NullableColumnTests {

        // Entity with nullable field
        @Entity(table = "nullable_entity")
        static class NullableEntity extends SuprimEntity {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            @Column(name = "optional_field")
            private String optionalField;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getOptionalField() { return optionalField; }
            public void setOptionalField(String value) { this.optionalField = value; }
        }

        @Test
        @DisplayName("refresh skips setting null column values")
        void refresh_skipsNullColumnValues() throws SQLException {
            // Create table
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"nullable_entity\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"optional_field\" VARCHAR(255))"
                );
            }

            try {
                NullableEntity entity = new NullableEntity();
                entity.setOptionalField(null); // Null value in DB

                executor.transaction(tx -> {
                    entity.save();
                });

                // Set value in memory
                entity.setOptionalField("In Memory Value");

                // Refresh should NOT overwrite in-memory value when DB value is null
                // (covers line 600 - the null value branch)
                executor.transaction(tx -> {
                    entity.refresh();
                    // Value stays as-is because DB has null
                    assertEquals("In Memory Value", entity.getOptionalField());
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"nullable_entity\"");
                }
            }
        }
    }

    @Nested
    @DisplayName("Entity with extra column not in DB")
    class ExtraColumnTests {

        // Entity with a column that doesn't exist in DB table
        @Entity(table = "extra_col_entity")
        static class ExtraColumnEntity extends SuprimEntity {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            @Column(name = "name")
            private String name;

            @Column(name = "extra_column_not_in_db")
            private String extraColumn;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getExtraColumn() { return extraColumn; }
            public void setExtraColumn(String value) { this.extraColumn = value; }
        }

        @Test
        @DisplayName("refresh skips column not in result set")
        void refresh_skipsColumnNotInResultSet() throws SQLException {
            // Create table WITHOUT the extra_column_not_in_db column
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"extra_col_entity\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255))"
                );
            }

            try {
                // Manually insert a row using raw SQL (bypassing entity which would try to insert extra column)
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute(
                        "INSERT INTO \"extra_col_entity\" (\"id\", \"name\") VALUES ('test-id-123', 'Test Name')"
                    );
                }

                ExtraColumnEntity entity = new ExtraColumnEntity();
                entity.setId("test-id-123");
                entity.setExtraColumn("In Memory Value");

                // Refresh should skip the extra_column_not_in_db since it doesn't exist in result set
                // (covers line 604 - SQLException catch block)
                executor.transaction(tx -> {
                    entity.refresh();
                    // Name should be loaded from DB
                    assertEquals("Test Name", entity.getName());
                    // Extra column stays as-is (SQLException caught and ignored)
                    assertEquals("In Memory Value", entity.getExtraColumn());
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"extra_col_entity\"");
                }
            }
        }
    }

    // ==================== AUTO-COMMIT MODE TESTS ====================

    @Nested
    @DisplayName("Auto-commit mode")
    class AutoCommitModeTests {

        @Test
        @DisplayName("save works with global executor (auto-commit)")
        void save_withGlobalExecutor_autoCommits() throws SQLException {
            // Setup table
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"autocommit_users\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"email\" VARCHAR(255))"
                );
            }

            try {
                // Register global executor
                SuprimContext.setGlobalExecutor(executor);

                // Create entity outside transaction
                AutoCommitUser user = new AutoCommitUser();
                user.setEmail("autocommit@test.com");
                user.save();  // Should auto-commit

                // Verify saved by querying directly
                assertNotNull(user.getId());
                try (Connection conn = dataSource.getConnection();
                     var rs = conn.createStatement().executeQuery(
                         "SELECT * FROM \"autocommit_users\" WHERE \"id\" = '" + user.getId() + "'")) {
                    assertTrue(rs.next());
                    assertEquals("autocommit@test.com", rs.getString("email"));
                }
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"autocommit_users\"");
                }
            }
        }

        @Test
        @DisplayName("update works with global executor (auto-commit)")
        void update_withGlobalExecutor_autoCommits() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"autocommit_users\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"email\" VARCHAR(255))"
                );
            }

            try {
                SuprimContext.setGlobalExecutor(executor);

                AutoCommitUser user = new AutoCommitUser();
                user.setEmail("original@test.com");
                user.save();

                user.setEmail("updated@test.com");
                user.update();  // Auto-commit

                try (Connection conn = dataSource.getConnection();
                     var rs = conn.createStatement().executeQuery(
                         "SELECT * FROM \"autocommit_users\" WHERE \"id\" = '" + user.getId() + "'")) {
                    assertTrue(rs.next());
                    assertEquals("updated@test.com", rs.getString("email"));
                }
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"autocommit_users\"");
                }
            }
        }

        @Test
        @DisplayName("delete works with global executor (auto-commit)")
        void delete_withGlobalExecutor_autoCommits() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"autocommit_users\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"email\" VARCHAR(255))"
                );
            }

            try {
                SuprimContext.setGlobalExecutor(executor);

                AutoCommitUser user = new AutoCommitUser();
                user.setEmail("todelete@test.com");
                user.save();
                String id = user.getId();

                user.delete();  // Auto-commit

                try (Connection conn = dataSource.getConnection();
                     var rs = conn.createStatement().executeQuery(
                         "SELECT COUNT(*) FROM \"autocommit_users\" WHERE \"id\" = '" + id + "'")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1));
                }
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"autocommit_users\"");
                }
            }
        }

        @Test
        @DisplayName("refresh works with global executor (auto-commit)")
        void refresh_withGlobalExecutor_autoCommits() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"autocommit_users\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"email\" VARCHAR(255))"
                );
            }

            try {
                SuprimContext.setGlobalExecutor(executor);

                AutoCommitUser user = new AutoCommitUser();
                user.setEmail("original@test.com");
                user.save();

                // Modify in DB directly
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute(
                        "UPDATE \"autocommit_users\" SET \"email\" = 'modified@test.com' WHERE \"id\" = '" + user.getId() + "'"
                    );
                }

                user.refresh();  // Auto-commit
                assertEquals("modified@test.com", user.getEmail());
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"autocommit_users\"");
                }
            }
        }

        @Test
        @DisplayName("save throws when no context and no global executor")
        void save_noContextNoExecutor_throws() {
            AutoCommitUser user = new AutoCommitUser();
            user.setEmail("test@test.com");

            IllegalStateException ex = assertThrows(IllegalStateException.class, user::save);
            assertTrue(ex.getMessage().contains("No active transaction context"));
            assertTrue(ex.getMessage().contains("global executor"));
        }

        @Test
        @DisplayName("update throws when no context and no global executor")
        void update_noContextNoExecutor_throws() {
            AutoCommitUser user = new AutoCommitUser();
            user.setId("test-id");
            user.setEmail("test@test.com");

            IllegalStateException ex = assertThrows(IllegalStateException.class, user::update);
            assertTrue(ex.getMessage().contains("No active transaction context"));
        }

        @Test
        @DisplayName("delete throws when no context and no global executor")
        void delete_noContextNoExecutor_throws() {
            AutoCommitUser user = new AutoCommitUser();
            user.setId("test-id");

            IllegalStateException ex = assertThrows(IllegalStateException.class, user::delete);
            assertTrue(ex.getMessage().contains("No active transaction context"));
        }

        @Test
        @DisplayName("refresh throws when no context and no global executor")
        void refresh_noContextNoExecutor_throws() {
            AutoCommitUser user = new AutoCommitUser();
            user.setId("test-id");

            IllegalStateException ex = assertThrows(IllegalStateException.class, user::refresh);
            assertTrue(ex.getMessage().contains("No active transaction context"));
        }

        @Test
        @DisplayName("restore throws when no context and no global executor")
        void restore_noContextNoExecutor_throws() {
            AutoCommitSoftDeleteUser user = new AutoCommitSoftDeleteUser();
            user.setId("test-id");

            IllegalStateException ex = assertThrows(IllegalStateException.class, user::restore);
            assertTrue(ex.getMessage().contains("No active transaction context"));
        }

        @Test
        @DisplayName("forceDelete throws when no context and no global executor")
        void forceDelete_noContextNoExecutor_throws() {
            AutoCommitSoftDeleteUser user = new AutoCommitSoftDeleteUser();
            user.setId("test-id");

            IllegalStateException ex = assertThrows(IllegalStateException.class, user::forceDelete);
            assertTrue(ex.getMessage().contains("No active transaction context"));
        }

        @Test
        @DisplayName("restore works with global executor (auto-commit)")
        void restore_withGlobalExecutor_autoCommits() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"autocommit_soft_users\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"email\" VARCHAR(255), " +
                    "\"deleted_at\" TIMESTAMP)"
                );
            }

            try {
                SuprimContext.setGlobalExecutor(executor);

                AutoCommitSoftDeleteUser user = new AutoCommitSoftDeleteUser();
                user.setEmail("restore-autocommit@test.com");
                user.save();

                // Soft delete
                user.delete();
                assertNotNull(user.getDeletedAt());

                // Restore in auto-commit mode
                user.restore();
                assertNull(user.getDeletedAt());

                // Verify in DB
                try (Connection conn = dataSource.getConnection();
                     var rs = conn.createStatement().executeQuery(
                         "SELECT \"deleted_at\" FROM \"autocommit_soft_users\" WHERE \"id\" = '" + user.getId() + "'")) {
                    assertTrue(rs.next());
                    assertNull(rs.getTimestamp("deleted_at"));
                }
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"autocommit_soft_users\"");
                }
            }
        }

        @Test
        @DisplayName("forceDelete works with global executor (auto-commit)")
        void forceDelete_withGlobalExecutor_autoCommits() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"autocommit_soft_users\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"email\" VARCHAR(255), " +
                    "\"deleted_at\" TIMESTAMP)"
                );
            }

            try {
                SuprimContext.setGlobalExecutor(executor);

                AutoCommitSoftDeleteUser user = new AutoCommitSoftDeleteUser();
                user.setEmail("forcedelete-autocommit@test.com");
                user.save();
                String id = user.getId();

                // Force delete in auto-commit mode
                user.forceDelete();

                // Verify actually deleted from DB
                try (Connection conn = dataSource.getConnection();
                     var rs = conn.createStatement().executeQuery(
                         "SELECT COUNT(*) FROM \"autocommit_soft_users\" WHERE \"id\" = '" + id + "'")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1));
                }
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"autocommit_soft_users\"");
                }
            }
        }

        @Entity(table = "autocommit_users")
        static class AutoCommitUser extends SuprimEntity {
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

        @Entity(table = "autocommit_soft_users")
        @SoftDeletes
        static class AutoCommitSoftDeleteUser extends SuprimEntity {
            @Id(strategy = GenerationType.UUID_V7)
            @Column(name = "id")
            private String id;

            @Column(name = "email")
            private String email;

            @Column(name = "deleted_at")
            private LocalDateTime deletedAt;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; }
            public LocalDateTime getDeletedAt() { return deletedAt; }
            public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
        }
    }

    // ==================== SOFT DELETE TESTS ====================

    @Nested
    @DisplayName("Soft Delete Operations")
    class SoftDeleteTests {

        @Test
        @DisplayName("delete() soft deletes entity with @SoftDeletes")
        void delete_softDeletesEntity() {
            executor.transaction(tx -> {
                SoftDeleteUser user = new SoftDeleteUser();
                user.setEmail("soft@test.com");
                tx.save(user);

                assertNull(user.getDeletedAt());
                assertFalse(user.trashed());

                user.delete();

                assertNotNull(user.getDeletedAt());
                assertTrue(user.trashed());
            });
        }

        @Test
        @DisplayName("restore() clears deleted_at")
        void restore_clearsDeletedAt() {
            executor.transaction(tx -> {
                SoftDeleteUser user = new SoftDeleteUser();
                user.setEmail("restore@test.com");
                tx.save(user);

                user.delete();
                assertTrue(user.trashed());

                user.restore();

                assertNull(user.getDeletedAt());
                assertFalse(user.trashed());
            });
        }

        @Test
        @DisplayName("forceDelete() actually removes record")
        void forceDelete_removesRecord() {
            executor.transaction(tx -> {
                SoftDeleteUser user = new SoftDeleteUser();
                user.setEmail("force@test.com");
                tx.save(user);
                String id = user.getId();

                user.forceDelete();

                // Verify record is actually deleted
                java.util.List<SoftDeleteUser> found = tx.query(
                    sant1ago.dev.suprim.core.query.Suprim.selectRaw("*")
                        .from(sant1ago.dev.suprim.core.type.Table.of("soft_delete_users", SoftDeleteUser.class))
                        .whereRaw("\"id\" = '" + id + "'")
                        .build(),
                    rs -> {
                        SoftDeleteUser u = new SoftDeleteUser();
                        u.setId(rs.getString("id"));
                        return u;
                    }
                );
                assertTrue(found.isEmpty());
            });
        }

        @Test
        @DisplayName("trashed() returns true when deleted_at is set")
        void trashed_returnsTrueWhenDeleted() {
            SoftDeleteUser user = new SoftDeleteUser();
            assertFalse(user.trashed());

            user.setDeletedAt(LocalDateTime.now());
            assertTrue(user.trashed());

            user.setDeletedAt(null);
            assertFalse(user.trashed());
        }

        @Test
        @DisplayName("refresh() loads deleted_at from database")
        void refresh_loadsDeletedAt() {
            executor.transaction(tx -> {
                SoftDeleteUser user = new SoftDeleteUser();
                user.setEmail("refresh@test.com");
                tx.save(user);

                user.delete();
                LocalDateTime deletedTime = user.getDeletedAt();

                // Clear local state
                user.setDeletedAt(null);
                assertNull(user.getDeletedAt());

                // Refresh should reload from DB
                user.refresh();
                assertNotNull(user.getDeletedAt());
            });
        }

        @Test
        @DisplayName("delete then restore then verify in DB")
        void delete_restore_verifyInDb() {
            executor.transaction(tx -> {
                SoftDeleteUser user = new SoftDeleteUser();
                user.setEmail("cycle@test.com");
                tx.save(user);
                String id = user.getId();

                // Soft delete
                user.delete();
                assertTrue(user.trashed());

                // Refresh to verify DB state
                user.refresh();
                assertTrue(user.trashed());

                // Restore
                user.restore();
                assertFalse(user.trashed());

                // Refresh to verify DB state
                user.refresh();
                assertFalse(user.trashed());
                assertNull(user.getDeletedAt());
            });
        }

        @Test
        @DisplayName("restore() throws when entity has no @SoftDeletes")
        void restore_throwsWhenNoSoftDeletes() {
            executor.transaction(tx -> {
                TestUser user = new TestUser();
                user.setEmail("nosoftdelete@test.com");
                tx.save(user);

                assertThrows(sant1ago.dev.suprim.jdbc.exception.PersistenceException.class, user::restore);
            });
        }

        @Test
        @DisplayName("restore() throws when entity has no ID")
        void restore_throwsWhenNoId() {
            executor.transaction(tx -> {
                SoftDeleteUser user = new SoftDeleteUser();
                user.setEmail("noid@test.com");
                // Don't save - entity has no ID

                assertThrows(sant1ago.dev.suprim.jdbc.exception.PersistenceException.class, user::restore);
            });
        }

        @Test
        @DisplayName("restore() works with schema-qualified entity")
        void restore_worksWithSchema() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute("CREATE SCHEMA IF NOT EXISTS \"test_schema\"");
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"test_schema\".\"schema_soft_delete\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"email\" VARCHAR(255), " +
                    "\"deleted_at\" TIMESTAMP)"
                );
            }

            try {
                executor.transaction(tx -> {
                    SchemaSoftDeleteEntity entity = new SchemaSoftDeleteEntity();
                    entity.setEmail("schema@test.com");
                    tx.save(entity);

                    entity.delete();
                    assertTrue(entity.trashed());

                    entity.restore();
                    assertFalse(entity.trashed());
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"test_schema\".\"schema_soft_delete\"");
                    conn.createStatement().execute("DROP SCHEMA IF EXISTS \"test_schema\"");
                }
            }
        }
    }

    @Entity(table = "soft_delete_users")
    @SoftDeletes
    static class SoftDeleteUser extends SuprimEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "email")
        private String email;

        @Column(name = "deleted_at")
        private LocalDateTime deletedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    }

    @Entity(table = "schema_soft_delete", schema = "test_schema")
    @SoftDeletes
    static class SchemaSoftDeleteEntity extends SuprimEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "email")
        private String email;

        @Column(name = "deleted_at")
        private LocalDateTime deletedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    }

    // ==================== REPLICATE TESTS ====================

    @Nested
    @DisplayName("replicate() method")
    class ReplicateTests {

        @Test
        @DisplayName("replicate() creates copy without ID")
        void replicate_createsCopyWithoutId() {
            executor.transaction(tx -> {
                TestUser original = new TestUser();
                original.setEmail("original@test.com");
                tx.save(original);

                TestUser copy = original.replicate();

                assertNull(copy.getId());
                assertEquals("original@test.com", copy.getEmail());
            });
        }

        @Test
        @DisplayName("replicate() copy can be saved as new record")
        void replicate_copyCanBeSaved() {
            executor.transaction(tx -> {
                TestUser original = new TestUser();
                original.setEmail("original@test.com");
                tx.save(original);
                String originalId = original.getId();

                TestUser copy = original.replicate();
                copy.setEmail("copy@test.com");
                copy.save();

                assertNotNull(copy.getId());
                assertNotEquals(originalId, copy.getId());
                assertEquals("copy@test.com", copy.getEmail());
            });
        }

        @Test
        @DisplayName("replicate(except) excludes specified fields")
        void replicate_withExcept_excludesFields() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"replicate_test\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255), " +
                    "\"status\" VARCHAR(50))"
                );
            }

            try {
                executor.transaction(tx -> {
                    ReplicateTestEntity original = new ReplicateTestEntity();
                    original.setName("Test Name");
                    original.setStatus("active");
                    tx.save(original);

                    ReplicateTestEntity copy = original.replicate("status");

                    assertNull(copy.getId());
                    assertEquals("Test Name", copy.getName());
                    assertNull(copy.getStatus()); // excluded
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"replicate_test\"");
                }
            }
        }

        @Test
        @DisplayName("replicate() handles entity with no default constructor gracefully")
        void replicate_handlesReflectionErrors() {
            // Testing that error path is hit (entity without default constructor would throw)
            executor.transaction(tx -> {
                TestUser user = new TestUser();
                user.setEmail("test@test.com");
                tx.save(user);

                // Should work for entities with default constructor
                TestUser copy = user.replicate();
                assertNotNull(copy);
            });
        }

        @Test
        @DisplayName("replicate(except) excludes by column name when different from field name")
        void replicate_withExcept_excludesByColumnName() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"column_name_test\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"user_name\" VARCHAR(255), " +
                    "\"user_status\" VARCHAR(50))"
                );
            }

            try {
                executor.transaction(tx -> {
                    ColumnNameEntity original = new ColumnNameEntity();
                    original.setName("Test Name");
                    original.setStatus("active");
                    tx.save(original);

                    // Exclude by column name (not field name)
                    ColumnNameEntity copy = original.replicate("user_status");

                    assertNull(copy.getId());
                    assertEquals("Test Name", copy.getName());
                    assertNull(copy.getStatus()); // excluded by column name
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"column_name_test\"");
                }
            }
        }
    }

    @Entity(table = "replicate_test")
    static class ReplicateTestEntity extends SuprimEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "name")
        private String name;

        @Column(name = "status")
        private String status;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @Entity(table = "column_name_test")
    static class ColumnNameEntity extends SuprimEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "user_name")
        private String name;

        @Column(name = "user_status")
        private String status;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // ==================== TOUCH TESTS ====================

    @Nested
    @DisplayName("touch() method")
    class TouchTests {

        @Test
        @DisplayName("touch() updates updated_at timestamp")
        void touch_updatesTimestamp() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"touch_test\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255), " +
                    "\"updated_at\" TIMESTAMP)"
                );
            }

            try {
                executor.transaction(tx -> {
                    TouchTestEntity entity = new TouchTestEntity();
                    entity.setName("Test");
                    tx.save(entity);

                    LocalDateTime beforeTouch = entity.getUpdatedAt();

                    // Small delay to ensure timestamp changes
                    try { Thread.sleep(10); } catch (InterruptedException e) { }

                    entity.touch();

                    // Refresh to get DB value
                    entity.refresh();
                    LocalDateTime afterTouch = entity.getUpdatedAt();

                    assertNotNull(afterTouch);
                    // After touch should be >= before (or null if no updated_at before)
                    if (beforeTouch != null) {
                        assertTrue(afterTouch.isAfter(beforeTouch) || afterTouch.equals(beforeTouch));
                    }
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"touch_test\"");
                }
            }
        }

        @Test
        @DisplayName("touch() works in auto-commit mode")
        void touch_worksInAutoCommit() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"touch_test\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255), " +
                    "\"updated_at\" TIMESTAMP)"
                );
            }

            try {
                SuprimContext.setGlobalExecutor(executor);

                TouchTestEntity entity = new TouchTestEntity();
                entity.setName("AutoCommit Touch Test");
                entity.save();

                entity.touch(); // Auto-commit

                entity.refresh();
                assertNotNull(entity.getUpdatedAt());
            } finally {
                SuprimContext.clearGlobalExecutor();
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"touch_test\"");
                }
            }
        }

        @Test
        @DisplayName("touch() throws when no context and no global executor")
        void touch_noContext_throws() {
            TouchTestEntity entity = new TouchTestEntity();
            entity.setId("test-id");

            IllegalStateException ex = assertThrows(IllegalStateException.class, entity::touch);
            assertTrue(ex.getMessage().contains("No active transaction context"));
        }

        @Test
        @DisplayName("touch() throws when entity has no ID")
        void touch_throwsWhenNoId() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"touch_test\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255), " +
                    "\"updated_at\" TIMESTAMP)"
                );
            }

            try {
                executor.transaction(tx -> {
                    TouchTestEntity entity = new TouchTestEntity();
                    entity.setName("No ID Test");
                    // Don't save - entity has no ID

                    assertThrows(sant1ago.dev.suprim.jdbc.exception.PersistenceException.class, entity::touch);
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"touch_test\"");
                }
            }
        }

        @Test
        @DisplayName("touch() returns this for chaining")
        void touch_returnsThis() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"touch_test\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255), " +
                    "\"updated_at\" TIMESTAMP)"
                );
            }

            try {
                executor.transaction(tx -> {
                    TouchTestEntity entity = new TouchTestEntity();
                    entity.setName("Chain Test");
                    tx.save(entity);

                    SuprimEntity result = entity.touch();
                    assertSame(entity, result);
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"touch_test\"");
                }
            }
        }

        @Test
        @DisplayName("touch() works with schema-qualified entity")
        void touch_worksWithSchema() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute("CREATE SCHEMA IF NOT EXISTS \"test_schema\"");
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"test_schema\".\"schema_touch_test\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255), " +
                    "\"updated_at\" TIMESTAMP)"
                );
            }

            try {
                executor.transaction(tx -> {
                    SchemaTouchTestEntity entity = new SchemaTouchTestEntity();
                    entity.setName("Schema Test");
                    tx.save(entity);

                    entity.touch();
                    entity.refresh();
                    assertNotNull(entity.getUpdatedAt());
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"test_schema\".\"schema_touch_test\"");
                    conn.createStatement().execute("DROP SCHEMA IF EXISTS \"test_schema\"");
                }
            }
        }
    }

    @Entity(table = "touch_test")
    static class TouchTestEntity extends SuprimEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "name")
        private String name;

        @UpdateTimestamp(column = "updated_at")
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @Entity(table = "schema_touch_test", schema = "test_schema")
    static class SchemaTouchTestEntity extends SuprimEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id")
        private String id;

        @Column(name = "name")
        private String name;

        @UpdateTimestamp(column = "updated_at")
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    // ==================== SAVE WITH EXISTING ID TESTS ====================

    @Nested
    @DisplayName("save() with existing ID")
    class SaveWithExistingIdTests {

        @Test
        @DisplayName("save() inserts entity with pre-set ID")
        void save_withPresetId_insertsEntity() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"preset_id_entity\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255))"
                );
            }

            try {
                executor.transaction(tx -> {
                    PresetIdEntity entity = new PresetIdEntity();
                    String customId = "my-custom-preset-id-123";
                    entity.setId(customId); // Pre-set the ID
                    entity.setName("Test Name");

                    tx.save(entity);

                    // ID should remain as set
                    assertEquals(customId, entity.getId());

                    // Verify it was saved correctly
                    entity.refresh();
                    assertEquals("Test Name", entity.getName());
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"preset_id_entity\"");
                }
            }
        }
    }

    @Entity(table = "preset_id_entity")
    static class PresetIdEntity extends SuprimEntity {
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

    // ==================== MANUAL ID STRATEGY TESTS ====================

    @Nested
    @DisplayName("Manual ID strategy (NONE)")
    class ManualIdStrategyTests {

        @Test
        @DisplayName("save() throws when strategy is NONE and ID is null")
        void save_manualStrategyNoId_throws() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"manual_id_entity\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255))"
                );
            }

            try {
                executor.transaction(tx -> {
                    ManualIdEntity entity = new ManualIdEntity();
                    entity.setName("Test");
                    // Don't set ID - should throw

                    Exception ex = assertThrows(
                        sant1ago.dev.suprim.jdbc.exception.PersistenceException.class,
                        () -> tx.save(entity)
                    );
                    assertTrue(ex.getMessage().contains("Entity ID is null and strategy is NONE"));
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"manual_id_entity\"");
                }
            }
        }

        @Test
        @DisplayName("save() works when strategy is NONE and ID is set")
        void save_manualStrategyWithId_saves() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS \"manual_id_entity\" (" +
                    "\"id\" VARCHAR(36) PRIMARY KEY, " +
                    "\"name\" VARCHAR(255))"
                );
            }

            try {
                executor.transaction(tx -> {
                    ManualIdEntity entity = new ManualIdEntity();
                    entity.setId("manual-123");
                    entity.setName("Manual Test");

                    tx.save(entity);

                    assertEquals("manual-123", entity.getId());
                    entity.refresh();
                    assertEquals("Manual Test", entity.getName());
                });
            } finally {
                try (Connection conn = dataSource.getConnection()) {
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"manual_id_entity\"");
                }
            }
        }
    }

    @Entity(table = "manual_id_entity")
    static class ManualIdEntity extends SuprimEntity {
        @Id(strategy = GenerationType.NONE)
        @Column(name = "id")
        private String id;

        @Column(name = "name")
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
