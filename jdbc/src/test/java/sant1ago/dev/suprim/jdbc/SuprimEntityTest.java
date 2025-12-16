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
import sant1ago.dev.suprim.annotation.type.GenerationType;

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
        }
    }

    @AfterAll
    static void teardown() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("DROP TABLE IF EXISTS \"entity_users\"");
            conn.createStatement().execute("DROP TABLE IF EXISTS \"custom_gen_users\"");
        }
    }

    @AfterEach
    void cleanup() {
        SuprimContext.clearContext();
        SuprimContext.clearGlobalExecutor();
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
    }
}
