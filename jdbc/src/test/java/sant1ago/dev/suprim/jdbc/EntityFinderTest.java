package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.type.SqlType;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the EntityFinder internal helper class.
 */
@DisplayName("EntityFinder - Internal Entity Finder Tests")
class EntityFinderTest {

    private JdbcDataSource dataSource;
    private SuprimExecutor executor;
    private Connection setupConnection;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:entityfindertest;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");

        setupConnection = dataSource.getConnection();
        try (Statement stmt = setupConnection.createStatement()) {
            // DATABASE_TO_LOWER=TRUE makes H2 case-insensitive
            stmt.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL,
                    name VARCHAR(100),
                    status VARCHAR(50)
                )
                """);

            // Table with UUID ID
            stmt.execute("""
                CREATE TABLE products (
                    id UUID PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    price DECIMAL(10, 2)
                )
                """);

            // Table with INTEGER ID
            stmt.execute("""
                CREATE TABLE categories (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL
                )
                """);

            // Table with schema (simulated with H2)
            stmt.execute("CREATE SCHEMA IF NOT EXISTS app");
            stmt.execute("""
                CREATE TABLE app.orders (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    order_number VARCHAR(50) NOT NULL,
                    total DECIMAL(10, 2)
                )
                """);
        }

        executor = SuprimExecutor.create(dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
            stmt.execute("DROP TABLE IF EXISTS products");
            stmt.execute("DROP TABLE IF EXISTS categories");
            stmt.execute("DROP TABLE IF EXISTS app.orders");
            stmt.execute("DROP SCHEMA IF EXISTS app");
        }
        setupConnection.close();
    }

    // ==================== findAll TESTS ====================

    @Nested
    @DisplayName("findAll() - Offset-Based Pagination")
    class FindAllTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                for (int i = 1; i <= 10; i++) {
                    stmt.execute(String.format(
                        "INSERT INTO users (email, name, status) VALUES ('user%d@test.com', 'User%d', 'active')",
                        i, i));
                }
            }
        }

        @Test
        @DisplayName("findAll() returns results with limit and offset")
        void findAll_returnsResultsWithLimitAndOffset() {
            List<UserEntity> users = EntityFinder.findAll(executor, UserEntity.class, 5, 0);

            assertEquals(5, users.size());
        }

        @Test
        @DisplayName("findAll() respects offset parameter")
        void findAll_respectsOffsetParameter() {
            List<UserEntity> users = EntityFinder.findAll(executor, UserEntity.class, 5, 5);

            assertEquals(5, users.size());
            // Should start from User6 (id=6)
        }

        @Test
        @DisplayName("findAll() with offset exceeding data returns empty")
        void findAll_offsetExceedingData_returnsEmpty() {
            List<UserEntity> users = EntityFinder.findAll(executor, UserEntity.class, 5, 100);

            assertTrue(users.isEmpty());
        }

        @Test
        @DisplayName("findAll() with limit 0 returns empty")
        void findAll_limitZero_returnsEmpty() {
            List<UserEntity> users = EntityFinder.findAll(executor, UserEntity.class, 0, 0);

            assertTrue(users.isEmpty());
        }

        @Test
        @DisplayName("findAll() throws IllegalArgumentException when entity class is null")
        void findAll_nullEntityClass_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityFinder.findAll(executor, null, 10, 0));
        }

        @Test
        @DisplayName("findAll() orders results by ID")
        void findAll_ordersResultsById() {
            List<UserEntity> users = EntityFinder.findAll(executor, UserEntity.class, 10, 0);

            for (int i = 0; i < users.size() - 1; i++) {
                assertTrue(users.get(i).getId() < users.get(i + 1).getId());
            }
        }
    }

    // ==================== findAllWithCursor TESTS ====================

    @Nested
    @DisplayName("findAllWithCursor() - Cursor-Based Pagination")
    class FindAllWithCursorTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                for (int i = 1; i <= 10; i++) {
                    stmt.execute(String.format(
                        "INSERT INTO users (email, name, status) VALUES ('user%d@test.com', 'User%d', 'active')",
                        i, i));
                }
            }
        }

        @Test
        @DisplayName("findAllWithCursor() first page returns correct results")
        void findAllWithCursor_firstPage_returnsCorrectResults() {
            CursorResult<UserEntity> result = EntityFinder.findAllWithCursor(
                executor, UserEntity.class, null, 5);

            assertEquals(5, result.getData().size());
            assertTrue(result.hasMorePages());
            assertNotNull(result.getNextCursor());
            assertFalse(result.hasPreviousPages());
            assertNull(result.getPreviousCursor());
        }

        @Test
        @DisplayName("findAllWithCursor() subsequent page uses cursor")
        void findAllWithCursor_subsequentPage_usesCursor() {
            CursorResult<UserEntity> firstPage = EntityFinder.findAllWithCursor(
                executor, UserEntity.class, null, 5);

            CursorResult<UserEntity> secondPage = EntityFinder.findAllWithCursor(
                executor, UserEntity.class, firstPage.getNextCursor(), 5);

            assertEquals(5, secondPage.getData().size());
            assertFalse(secondPage.hasMorePages());
            assertNotNull(secondPage.getPreviousCursor());
        }

        @Test
        @DisplayName("findAllWithCursor() last page has no more pages")
        void findAllWithCursor_lastPage_hasNoMorePages() {
            CursorResult<UserEntity> result = EntityFinder.findAllWithCursor(
                executor, UserEntity.class, null, 20);

            assertEquals(10, result.getData().size());
            assertFalse(result.hasMorePages());
            assertNull(result.getNextCursor());
        }

        @Test
        @DisplayName("findAllWithCursor() throws IllegalArgumentException when entity class is null")
        void findAllWithCursor_nullEntityClass_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityFinder.findAllWithCursor(executor, null, null, 10));
        }

        @Test
        @DisplayName("findAllWithCursor() with empty table returns empty result")
        void findAllWithCursor_emptyTable_returnsEmptyResult() throws Exception {
            // Clear the table
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DELETE FROM users");
            }

            CursorResult<UserEntity> result = EntityFinder.findAllWithCursor(
                executor, UserEntity.class, null, 10);

            assertTrue(result.isEmpty());
            assertFalse(result.hasMorePages());
        }

        @Test
        @DisplayName("findAllWithCursor() returns correct perPage value")
        void findAllWithCursor_returnsCorrectPerPage() {
            CursorResult<UserEntity> result = EntityFinder.findAllWithCursor(
                executor, UserEntity.class, null, 7);

            assertEquals(7, result.getPerPage());
        }
    }

    // ==================== UUID ID TYPE TESTS ====================

    @Nested
    @DisplayName("UUID ID Type Support")
    class UuidIdTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                for (int i = 1; i <= 5; i++) {
                    UUID id = UUID.randomUUID();
                    stmt.execute(String.format(
                        "INSERT INTO products (id, name, price) VALUES ('%s', 'Product%d', %d.99)",
                        id.toString(), i, i * 10));
                }
            }
        }

        @Test
        @DisplayName("findAll() works with UUID primary key")
        void findAll_worksWithUuidPrimaryKey() {
            List<ProductEntity> products = EntityFinder.findAll(executor, ProductEntity.class, 10, 0);

            assertEquals(5, products.size());
            assertNotNull(products.get(0).getId());
            assertInstanceOf(UUID.class, products.get(0).getId());
        }

        @Test
        @DisplayName("findAllWithCursor() works with UUID primary key")
        void findAllWithCursor_worksWithUuidPrimaryKey() {
            CursorResult<ProductEntity> result = EntityFinder.findAllWithCursor(
                executor, ProductEntity.class, null, 3);

            assertEquals(3, result.getData().size());
            assertTrue(result.hasMorePages());
            assertNotNull(result.getNextCursor());
        }

        @Test
        @DisplayName("findAllWithCursor() cursor navigation works with UUID")
        void findAllWithCursor_cursorNavigationWorksWithUuid() {
            CursorResult<ProductEntity> firstPage = EntityFinder.findAllWithCursor(
                executor, ProductEntity.class, null, 3);

            CursorResult<ProductEntity> secondPage = EntityFinder.findAllWithCursor(
                executor, ProductEntity.class, firstPage.getNextCursor(), 3);

            assertEquals(2, secondPage.getData().size());
            assertFalse(secondPage.hasMorePages());
        }
    }

    // ==================== INTEGER ID TYPE TESTS ====================

    @Nested
    @DisplayName("Integer ID Type Support")
    class IntegerIdTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                for (int i = 1; i <= 5; i++) {
                    stmt.execute(String.format(
                        "INSERT INTO categories (name) VALUES ('Category%d')", i));
                }
            }
        }

        @Test
        @DisplayName("findAll() works with Integer primary key")
        void findAll_worksWithIntegerPrimaryKey() {
            List<CategoryEntity> categories = EntityFinder.findAll(
                executor, CategoryEntity.class, 10, 0);

            assertEquals(5, categories.size());
        }

        @Test
        @DisplayName("findAllWithCursor() works with Integer primary key")
        void findAllWithCursor_worksWithIntegerPrimaryKey() {
            CursorResult<CategoryEntity> result = EntityFinder.findAllWithCursor(
                executor, CategoryEntity.class, null, 3);

            assertEquals(3, result.getData().size());
            assertTrue(result.hasMorePages());
        }
    }

    // ==================== SCHEMA SUPPORT TESTS ====================

    @Nested
    @DisplayName("Schema Support")
    class SchemaTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                for (int i = 1; i <= 5; i++) {
                    stmt.execute(String.format(
                        "INSERT INTO app.orders (order_number, total) VALUES ('ORD%d', %d.00)",
                        i, i * 100));
                }
            }
        }

        @Test
        @DisplayName("findAll() works with schema-qualified table")
        void findAll_worksWithSchemaQualifiedTable() {
            List<OrderEntity> orders = EntityFinder.findAll(executor, OrderEntity.class, 10, 0);

            assertEquals(5, orders.size());
        }

        @Test
        @DisplayName("findAllWithCursor() works with schema-qualified table")
        void findAllWithCursor_worksWithSchemaQualifiedTable() {
            CursorResult<OrderEntity> result = EntityFinder.findAllWithCursor(
                executor, OrderEntity.class, null, 3);

            assertEquals(3, result.getData().size());
            assertTrue(result.hasMorePages());
        }
    }

    // ==================== CURSOR ENCODING/DECODING TESTS ====================

    @Nested
    @DisplayName("Cursor Encoding and Decoding")
    class CursorEncodingTests {

        @Test
        @DisplayName("encodeCursor() encodes value to base64")
        void encodeCursor_encodesToBase64() {
            String encoded = CursorResult.encodeCursor(12345L);

            assertNotNull(encoded);
            assertFalse(encoded.isEmpty());
        }

        @Test
        @DisplayName("decodeCursor() decodes base64 back to value")
        void decodeCursor_decodesBackToValue() {
            String encoded = CursorResult.encodeCursor(12345L);
            String decoded = CursorResult.decodeCursor(encoded);

            assertEquals("12345", decoded);
        }

        @Test
        @DisplayName("encodeCursor() with null returns null")
        void encodeCursor_nullValue_returnsNull() {
            String encoded = CursorResult.encodeCursor(null);

            assertNull(encoded);
        }

        @Test
        @DisplayName("decodeCursor() with null returns null")
        void decodeCursor_nullValue_returnsNull() {
            String decoded = CursorResult.decodeCursor(null);

            assertNull(decoded);
        }

        @Test
        @DisplayName("decodeCursor() with empty string returns null")
        void decodeCursor_emptyString_returnsNull() {
            String decoded = CursorResult.decodeCursor("");

            assertNull(decoded);
        }

        @Test
        @DisplayName("decodeCursor() with invalid base64 returns null")
        void decodeCursor_invalidBase64_returnsNull() {
            String decoded = CursorResult.decodeCursor("not-valid-base64!!!");

            assertNull(decoded);
        }

        @Test
        @DisplayName("encodeCursor() and decodeCursor() work with UUID")
        void encodeDecodeCursor_worksWithUuid() {
            UUID uuid = UUID.randomUUID();
            String encoded = CursorResult.encodeCursor(uuid);
            String decoded = CursorResult.decodeCursor(encoded);

            assertEquals(uuid.toString(), decoded);
        }
    }

    // ==================== CURSORRESULT METHODS TESTS ====================

    @Nested
    @DisplayName("CursorResult Methods")
    class CursorResultMethodsTests {

        @Test
        @DisplayName("CursorResult.of() creates instance with correct values")
        void of_createsInstanceWithCorrectValues() {
            List<String> data = List.of("a", "b", "c");
            CursorResult<String> result = CursorResult.of(data, "next", "prev", 10);

            assertEquals(data, result.getData());
            assertEquals("next", result.getNextCursor());
            assertEquals("prev", result.getPreviousCursor());
            assertEquals(10, result.getPerPage());
        }

        @Test
        @DisplayName("hasMorePages() returns true when nextCursor exists")
        void hasMorePages_returnsTrueWhenNextCursorExists() {
            CursorResult<String> result = CursorResult.of(List.of("a"), "next", null, 10);

            assertTrue(result.hasMorePages());
        }

        @Test
        @DisplayName("hasMorePages() returns false when nextCursor is null")
        void hasMorePages_returnsFalseWhenNextCursorIsNull() {
            CursorResult<String> result = CursorResult.of(List.of("a"), null, null, 10);

            assertFalse(result.hasMorePages());
        }

        @Test
        @DisplayName("hasPreviousPages() returns true when previousCursor exists")
        void hasPreviousPages_returnsTrueWhenPreviousCursorExists() {
            CursorResult<String> result = CursorResult.of(List.of("a"), null, "prev", 10);

            assertTrue(result.hasPreviousPages());
        }

        @Test
        @DisplayName("hasPreviousPages() returns false when previousCursor is null")
        void hasPreviousPages_returnsFalseWhenPreviousCursorIsNull() {
            CursorResult<String> result = CursorResult.of(List.of("a"), null, null, 10);

            assertFalse(result.hasPreviousPages());
        }

        @Test
        @DisplayName("isEmpty() returns true when data is empty")
        void isEmpty_returnsTrueWhenDataIsEmpty() {
            CursorResult<String> result = CursorResult.of(List.of(), null, null, 10);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("isEmpty() returns false when data is not empty")
        void isEmpty_returnsFalseWhenDataIsNotEmpty() {
            CursorResult<String> result = CursorResult.of(List.of("a"), null, null, 10);

            assertFalse(result.isEmpty());
        }
    }

    // ==================== TEST ENTITIES ====================

    @Entity(table = "users")
    public static class UserEntity {
        @Id
        @Column(name = "id", type = SqlType.BIGINT)
        private Long id;

        @Column(name = "email", type = SqlType.VARCHAR)
        private String email;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        @Column(name = "status", type = SqlType.VARCHAR)
        private String status;

        public UserEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @Entity(table = "products")
    public static class ProductEntity {
        @Id
        @Column(name = "id", type = SqlType.UUID)
        private UUID id;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        @Column(name = "price", type = SqlType.DOUBLE_PRECISION)
        private Double price;

        public ProductEntity() {}

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }

    @Entity(table = "categories")
    public static class CategoryEntity {
        @Id
        @Column(name = "id", type = SqlType.INTEGER)
        private Integer id;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        public CategoryEntity() {}

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Entity(table = "orders", schema = "app")
    public static class OrderEntity {
        @Id
        @Column(name = "id", type = SqlType.BIGINT)
        private Long id;

        @Column(name = "order_number", type = SqlType.VARCHAR)
        private String orderNumber;

        @Column(name = "total", type = SqlType.DOUBLE_PRECISION)
        private Double total;

        public OrderEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        public Double getTotal() { return total; }
        public void setTotal(Double total) { this.total = total; }
    }
}
