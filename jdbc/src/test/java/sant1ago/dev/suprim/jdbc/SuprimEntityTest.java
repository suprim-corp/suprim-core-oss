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

        // Create test table (quoted to preserve lowercase for H2)
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS \"entity_users\" (" +
                "\"id\" VARCHAR(36) PRIMARY KEY, " +
                "\"email\" VARCHAR(255))"
            );
        }
    }

    @AfterAll
    static void teardown() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("DROP TABLE IF EXISTS \"entity_users\"");
        }
    }

    @AfterEach
    void cleanup() {
        SuprimContext.clearContext();
    }

    // ==================== TEST ENTITY ====================

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
}
