package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.query.Suprim;
import sant1ago.dev.suprim.core.type.Table;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PaginationHelper.
 */
class PaginationHelperTest {

    private JdbcDataSource dataSource;
    private SuprimExecutor executor;
    private Connection setupConnection;

    // Table and Column definitions for test entity (H2 stores unquoted as uppercase)
    private static final Table<UserEntity> USER_TABLE = Table.of("USERS", UserEntity.class);
    private static final sant1ago.dev.suprim.core.type.Column<UserEntity, Long> USER_ID =
            new sant1ago.dev.suprim.core.type.Column<>(USER_TABLE, "ID", Long.class, null);
    private static final sant1ago.dev.suprim.core.type.Column<UserEntity, String> USER_EMAIL =
            new sant1ago.dev.suprim.core.type.Column<>(USER_TABLE, "EMAIL", String.class, null);
    private static final sant1ago.dev.suprim.core.type.Column<UserEntity, String> USER_NAME =
            new sant1ago.dev.suprim.core.type.Column<>(USER_TABLE, "NAME", String.class, null);

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:pagination_test;DB_CLOSE_DELAY=-1");

        setupConnection = dataSource.getConnection();
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL,
                    name VARCHAR(100)
                )
                """);
        }

        executor = SuprimExecutor.create(dataSource);

        // Insert test data
        for (int i = 1; i <= 25; i++) {
            insertUser("user" + i + "@example.com", "User " + i);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
        }
        setupConnection.close();
    }

    // ==================== paginate() tests ====================

    @Nested
    @DisplayName("paginate()")
    class PaginateTests {

        @Test
        @DisplayName("returns first page with correct metadata")
        void paginate_firstPage_returnsCorrectData() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            PaginatedResult<UserEntity> result = executor.paginate(builder, 1, 10, UserEntity.class);

            assertEquals(10, result.getData().size());
            assertEquals(1, result.getCurrentPage());
            assertEquals(10, result.getPerPage());
            assertEquals(25, result.getTotal());
            assertEquals(3, result.getLastPage());
            assertTrue(result.hasMorePages());
        }

        @Test
        @DisplayName("returns middle page with correct data")
        void paginate_middlePage_returnsCorrectData() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            PaginatedResult<UserEntity> result = executor.paginate(builder, 2, 10, UserEntity.class);

            assertEquals(10, result.getData().size());
            assertEquals(2, result.getCurrentPage());
            assertTrue(result.hasMorePages());
        }

        @Test
        @DisplayName("returns last page with partial data")
        void paginate_lastPage_returnsPartialData() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            PaginatedResult<UserEntity> result = executor.paginate(builder, 3, 10, UserEntity.class);

            assertEquals(5, result.getData().size()); // 25 total, page 3 with 10 per page = 5 remaining
            assertEquals(3, result.getCurrentPage());
            assertFalse(result.hasMorePages());
        }

        @Test
        @DisplayName("handles page beyond last page")
        void paginate_beyondLastPage_returnsEmptyData() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            PaginatedResult<UserEntity> result = executor.paginate(builder, 10, 10, UserEntity.class);

            assertTrue(result.getData().isEmpty());
            assertEquals(10, result.getCurrentPage());
            assertEquals(25, result.getTotal());
        }

        @Test
        @DisplayName("handles invalid page number (< 1)")
        void paginate_invalidPage_defaultsToFirstPage() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            PaginatedResult<UserEntity> result = executor.paginate(builder, 0, 10, UserEntity.class);

            assertEquals(1, result.getCurrentPage());
            assertEquals(10, result.getData().size());
        }

        @Test
        @DisplayName("handles invalid perPage (< 1)")
        void paginate_invalidPerPage_defaultsToTen() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            PaginatedResult<UserEntity> result = executor.paginate(builder, 1, 0, UserEntity.class);

            assertEquals(10, result.getPerPage());
        }

        @Test
        @DisplayName("works with custom RowMapper")
        void paginate_withCustomMapper_mapsCorrectly() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            PaginatedResult<String> result = executor.paginate(builder, 1, 5,
                    rs -> rs.getString("email"));

            assertEquals(5, result.getData().size());
            assertTrue(result.getData().get(0).contains("@example.com"));
        }
    }

    // ==================== cursorPaginate() tests ====================

    @Nested
    @DisplayName("cursorPaginate()")
    class CursorPaginateTests {

        @Test
        @DisplayName("returns first page without cursor")
        void cursorPaginate_firstPage_returnsData() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            CursorResult<UserEntity> result = executor.cursorPaginate(
                    builder, null, 10, UserEntity.class, USER_ID);

            assertEquals(10, result.getData().size());
            assertNotNull(result.getNextCursor());
        }

        @Test
        @DisplayName("handles invalid perPage")
        void cursorPaginate_invalidPerPage_defaultsToTen() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            CursorResult<UserEntity> result = executor.cursorPaginate(
                    builder, null, 0, UserEntity.class, USER_ID);

            assertEquals(10, result.getPerPage());
        }
    }

    // ==================== count() tests ====================

    @Nested
    @DisplayName("count()")
    class CountTests {

        @Test
        @DisplayName("returns correct total count")
        void count_allRows_returnsCorrectCount() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            long count = executor.count(builder);

            assertEquals(25, count);
        }

        @Test
        @DisplayName("returns zero for empty result")
        void count_noRows_returnsZero() throws Exception {
            // Clear all data
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DELETE FROM users");
            }

            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);

            long count = executor.count(builder);

            assertEquals(0, count);
        }

        @Test
        @DisplayName("works with WHERE clause")
        void count_withFilter_returnsFilteredCount() {
            SelectBuilder builder = Suprim.selectAll()
                    .from(USER_TABLE)
                    .where(USER_ID.lte(10L));

            long count = executor.count(builder);

            assertEquals(10, count);
        }
    }

    // ==================== Helper Methods ====================

    private void insertUser(String email, String name) throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("INSERT INTO users (email, name) VALUES ('" + email + "', '" + name + "')");
        }
    }

    // ==================== Test Entity ====================

    @Entity(table = "users")
    public static class UserEntity {
        @Id
        @Column(name = "id", type = SqlType.BIGINT)
        private Long id;

        @Column(name = "email", type = SqlType.VARCHAR)
        private String email;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        public UserEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
