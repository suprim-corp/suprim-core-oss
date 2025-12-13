package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.query.Suprim;
import sant1ago.dev.suprim.core.type.Table;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ChunkProcessor.
 */
class ChunkProcessorTest {

    private JdbcDataSource dataSource;
    private SuprimExecutor executor;
    private Connection setupConnection;

    // Table and Column definitions for test entity (H2 stores unquoted as uppercase)
    private static final Table<UserEntity> USER_TABLE = Table.of("USERS", UserEntity.class);
    private static final sant1ago.dev.suprim.core.type.Column<UserEntity, Long> USER_ID =
            new sant1ago.dev.suprim.core.type.Column<>(USER_TABLE, "ID", Long.class, null);
    private static final sant1ago.dev.suprim.core.type.Column<UserEntity, String> USER_EMAIL =
            new sant1ago.dev.suprim.core.type.Column<>(USER_TABLE, "EMAIL", String.class, null);

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:chunk_test;DB_CLOSE_DELAY=-1");

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

    // ==================== chunk() tests ====================

    @Nested
    @DisplayName("chunk()")
    class ChunkTests {

        @Test
        @DisplayName("processes all chunks")
        void chunk_processesAllChunks() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);
            List<List<UserEntity>> allChunks = new ArrayList<>();

            long total = executor.chunk(builder, 10, UserEntity.class, chunk -> {
                allChunks.add(new ArrayList<>(chunk));
                return true; // continue
            });

            assertEquals(25, total);
            assertEquals(3, allChunks.size()); // 10 + 10 + 5
            assertEquals(10, allChunks.get(0).size());
            assertEquals(10, allChunks.get(1).size());
            assertEquals(5, allChunks.get(2).size());
        }

        @Test
        @DisplayName("stops when processor returns false")
        void chunk_stopsWhenProcessorReturnsFalse() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);
            AtomicInteger chunkCount = new AtomicInteger(0);

            long total = executor.chunk(builder, 10, UserEntity.class, chunk -> {
                chunkCount.incrementAndGet();
                return false; // stop after first chunk
            });

            assertEquals(10, total); // Only processed first chunk
            assertEquals(1, chunkCount.get());
        }

        @Test
        @DisplayName("handles empty result")
        void chunk_emptyResult_returnsZero() throws Exception {
            // Clear all data
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DELETE FROM users");
            }

            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);
            AtomicInteger chunkCount = new AtomicInteger(0);

            long total = executor.chunk(builder, 10, UserEntity.class, chunk -> {
                chunkCount.incrementAndGet();
                return true;
            });

            assertEquals(0, total);
            assertEquals(0, chunkCount.get());
        }

        @Test
        @DisplayName("handles invalid chunk size")
        void chunk_invalidChunkSize_defaultsTo1000() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);
            List<Integer> chunkSizes = new ArrayList<>();

            executor.chunk(builder, 0, UserEntity.class, chunk -> {
                chunkSizes.add(chunk.size());
                return true;
            });

            // With default chunk size of 1000, all 25 rows should be in one chunk
            assertEquals(1, chunkSizes.size());
            assertEquals(25, chunkSizes.get(0));
        }

        @Test
        @DisplayName("works with custom RowMapper")
        void chunk_withCustomMapper_mapsCorrectly() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);
            List<String> allEmails = new ArrayList<>();

            executor.chunk(builder, 10, rs -> rs.getString("email"), chunk -> {
                allEmails.addAll(chunk);
                return true;
            });

            assertEquals(25, allEmails.size());
            assertTrue(allEmails.get(0).contains("@example.com"));
        }
    }

    // ==================== chunkById() tests ====================

    @Nested
    @DisplayName("chunkById()")
    class ChunkByIdTests {

        @Test
        @DisplayName("processes all chunks using keyset pagination")
        void chunkById_processesAllChunks() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);
            List<List<UserEntity>> allChunks = new ArrayList<>();

            long total = executor.chunkById(builder, 10, UserEntity.class, USER_ID, chunk -> {
                allChunks.add(new ArrayList<>(chunk));
                return true;
            });

            assertEquals(25, total);
            assertEquals(3, allChunks.size());
        }

        @Test
        @DisplayName("stops when processor returns false")
        void chunkById_stopsWhenProcessorReturnsFalse() {
            SelectBuilder builder = Suprim.selectAll().from(USER_TABLE);
            AtomicInteger chunkCount = new AtomicInteger(0);

            long total = executor.chunkById(builder, 10, UserEntity.class, USER_ID, chunk -> {
                chunkCount.incrementAndGet();
                return false;
            });

            assertEquals(10, total);
            assertEquals(1, chunkCount.get());
        }
    }

    // ==================== lazy() tests ====================

    @Nested
    @DisplayName("lazy()")
    class LazyTests {

        @Test
        @DisplayName("returns lazy stream of all rows")
        void lazy_returnsAllRows() {
            QueryResult query = Suprim.selectAll().from(USER_TABLE).build();

            try (Stream<UserEntity> stream = executor.lazy(query, UserEntity.class)) {
                long count = stream.count();
                assertEquals(25, count);
            }
        }

        @Test
        @DisplayName("stream can be filtered")
        void lazy_canBeFiltered() {
            QueryResult query = Suprim.selectAll().from(USER_TABLE).build();

            try (Stream<UserEntity> stream = executor.lazy(query, UserEntity.class)) {
                long count = stream
                        .filter(u -> u.getId() <= 10)
                        .count();
                assertEquals(10, count);
            }
        }

        @Test
        @DisplayName("stream can be mapped")
        void lazy_canBeMapped() {
            QueryResult query = Suprim.selectAll().from(USER_TABLE).build();

            try (Stream<UserEntity> stream = executor.lazy(query, UserEntity.class)) {
                List<String> emails = stream
                        .map(UserEntity::getEmail)
                        .limit(5)
                        .toList();

                assertEquals(5, emails.size());
                assertTrue(emails.get(0).contains("@example.com"));
            }
        }

        @Test
        @DisplayName("stream resources are closed")
        void lazy_resourcesAreClosed() {
            QueryResult query = Suprim.selectAll().from(USER_TABLE).build();

            // Just verify it doesn't throw when closed
            Stream<UserEntity> stream = executor.lazy(query, UserEntity.class);
            stream.limit(1).forEach(u -> {});
            assertDoesNotThrow(stream::close);
        }

        @Test
        @DisplayName("works with custom RowMapper")
        void lazy_withCustomMapper_mapsCorrectly() {
            QueryResult query = Suprim.selectAll().from(USER_TABLE).build();

            try (Stream<String> stream = executor.lazy(query, rs -> rs.getString("email"))) {
                List<String> emails = stream.limit(3).toList();
                assertEquals(3, emails.size());
            }
        }

        @Test
        @DisplayName("handles empty result")
        void lazy_emptyResult_returnsEmptyStream() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DELETE FROM users");
            }

            QueryResult query = Suprim.selectAll().from(USER_TABLE).build();

            try (Stream<UserEntity> stream = executor.lazy(query, UserEntity.class)) {
                assertEquals(0, stream.count());
            }
        }

        @Test
        @DisplayName("processes rows one at a time (memory efficient)")
        void lazy_processesRowsOneAtATime() {
            QueryResult query = Suprim.selectAll().from(USER_TABLE).build();
            AtomicInteger processedCount = new AtomicInteger(0);

            try (Stream<UserEntity> stream = executor.lazy(query, UserEntity.class)) {
                stream.peek(u -> processedCount.incrementAndGet())
                        .limit(5)
                        .forEach(u -> {});
            }

            // Should only process 5 rows due to limit
            assertEquals(5, processedCount.get());
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
