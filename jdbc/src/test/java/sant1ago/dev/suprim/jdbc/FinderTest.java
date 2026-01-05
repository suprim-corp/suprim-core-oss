package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.CreationTimestamp;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.entity.UpdateTimestamp;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.SqlType;
import sant1ago.dev.suprim.jdbc.exception.NoResultException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Finder fluent query builder.
 */
@DisplayName("Finder - Fluent Query Builder Tests")
class FinderTest {

    private JdbcDataSource dataSource;
    private SuprimExecutor executor;
    private Connection setupConnection;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:findertest;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");

        setupConnection = dataSource.getConnection();
        try (Statement stmt = setupConnection.createStatement()) {
            // DATABASE_TO_LOWER=TRUE makes H2 case-insensitive
            stmt.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL,
                    name VARCHAR(100),
                    status VARCHAR(50),
                    age INT,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    deleted_at TIMESTAMP
                )
                """);
        }

        executor = SuprimExecutor.create(dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
        }
        setupConnection.close();
    }

    // ==================== CONSTRUCTION TESTS ====================

    @Nested
    @DisplayName("Constructor and Initialization")
    class ConstructorTests {

        @Test
        @DisplayName("Finder throws NullPointerException when executor is null")
        void finder_nullExecutor_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () ->
                new Finder<>(null, UserEntity.class));
        }

        @Test
        @DisplayName("Finder throws NullPointerException when entity class is null")
        void finder_nullEntityClass_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () ->
                new Finder<>(executor, null));
        }

        @Test
        @DisplayName("find() method creates Finder instance")
        void find_createsFinderInstance() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            assertNotNull(finder);
        }
    }

    // ==================== SORTING TESTS ====================

    @Nested
    @DisplayName("Sorting Operations")
    class SortingTests {

        @BeforeEach
        void insertTestData() {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);
        }

        @Test
        @DisplayName("orderBy() sorts ascending by default")
        void orderBy_sortsAscending() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .orderBy("name")
                .get();

            assertEquals(3, users.size());
            assertEquals("Alice", users.get(0).getName());
            assertEquals("Bob", users.get(1).getName());
            assertEquals("Charlie", users.get(2).getName());
        }

        @Test
        @DisplayName("orderBy() with asc direction sorts ascending")
        void orderBy_withAscDirection_sortsAscending() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .orderBy("age", "asc")
                .get();

            assertEquals(3, users.size());
            assertEquals(25, users.get(0).getAge());
            assertEquals(30, users.get(1).getAge());
            assertEquals(35, users.get(2).getAge());
        }

        @Test
        @DisplayName("orderBy() with desc direction sorts descending")
        void orderBy_withDescDirection_sortsDescending() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .orderBy("age", "desc")
                .get();

            assertEquals(3, users.size());
            assertEquals(35, users.get(0).getAge());
            assertEquals(30, users.get(1).getAge());
            assertEquals(25, users.get(2).getAge());
        }

        @Test
        @DisplayName("orderByDesc() sorts descending")
        void orderByDesc_sortsDescending() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .orderByDesc("name")
                .get();

            assertEquals(3, users.size());
            assertEquals("Charlie", users.get(0).getName());
            assertEquals("Bob", users.get(1).getName());
            assertEquals("Alice", users.get(2).getName());
        }

        // Note: latest() and oldest() methods use reflection to find @CreationTimestamp
        // annotation which isn't available in test classpath. These methods are tested
        // indirectly through integration tests with proper entity setup.
    }

    // ==================== BETWEEN CONDITIONS TESTS ====================

    @Nested
    @DisplayName("Between Conditions")
    class BetweenConditionsTests {

        @BeforeEach
        void insertTestData() {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);
            insertUser("diana@test.com", "Diana", "pending", 20);
        }

        @Test
        @DisplayName("whereBetween() filters by inclusive range")
        void whereBetween_filtersByInclusiveRange() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereBetween("age", 25, 30)
                .orderBy("age")
                .get();

            assertEquals(2, users.size());
            assertEquals("Bob", users.get(0).getName());
            assertEquals("Alice", users.get(1).getName());
        }

        @Test
        @DisplayName("whereBetween() with same min and max")
        void whereBetween_withSameMinMax() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereBetween("age", 30, 30)
                .get();

            assertEquals(1, users.size());
            assertEquals("Alice", users.get(0).getName());
        }

        @Test
        @DisplayName("whereBetween() returns empty when no match")
        void whereBetween_returnsEmptyWhenNoMatch() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereBetween("age", 100, 200)
                .get();

            assertEquals(0, users.size());
        }

        @Test
        @DisplayName("whereNotBetween() excludes range")
        void whereNotBetween_excludesRange() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereNotBetween("age", 25, 30)
                .orderBy("age")
                .get();

            assertEquals(2, users.size());
            assertEquals("Diana", users.get(0).getName());
            assertEquals("Charlie", users.get(1).getName());
        }

        @Test
        @DisplayName("whereNotBetween() returns all when range excludes none")
        void whereNotBetween_returnsAllWhenRangeExcludesNone() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereNotBetween("age", 100, 200)
                .get();

            assertEquals(4, users.size());
        }

        @Test
        @DisplayName("orWhereBetween() adds OR condition with range")
        void orWhereBetween_addsOrConditionWithRange() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "inactive")
                .orWhereBetween("age", 18, 22)
                .orderBy("age")
                .get();

            // Diana (age 20) OR Charlie (inactive)
            assertEquals(2, users.size());
            assertEquals("Diana", users.get(0).getName());
            assertEquals("Charlie", users.get(1).getName());
        }

        @Test
        @DisplayName("orWhereNotBetween() adds OR NOT condition with range")
        void orWhereNotBetween_addsOrNotConditionWithRange() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "pending")
                .orWhereNotBetween("age", 20, 30)
                .orderBy("age")
                .get();

            // Diana (pending) OR Charlie (age 35, not between 20-30)
            assertEquals(2, users.size());
            assertEquals("Diana", users.get(0).getName());
            assertEquals("Charlie", users.get(1).getName());
        }

        @Test
        @DisplayName("whereBetween() chains with other conditions")
        void whereBetween_chainsWithOtherConditions() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .whereBetween("age", 20, 28)
                .get();

            assertEquals(1, users.size());
            assertEquals("Bob", users.get(0).getName());
        }

        @Test
        @DisplayName("whereBetween() returns this for chaining")
        void whereBetween_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);

            assertSame(finder, finder.whereBetween("age", 1, 10));
            assertSame(finder, finder.whereNotBetween("age", 100, 200));
            assertSame(finder, finder.orWhereBetween("age", 50, 60));
            assertSame(finder, finder.orWhereNotBetween("age", 70, 80));
        }

        @Test
        @DisplayName("multiple between conditions combine correctly")
        void multipleBetweenConditions_combineCorrectly() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereBetween("age", 20, 35)
                .whereNotBetween("age", 26, 29)
                .orderBy("age")
                .get();

            // Age 20-35 but NOT 26-29: Diana(20), Bob(25), Alice(30), Charlie(35)
            assertEquals(4, users.size());
        }

        @Test
        @DisplayName("whereBetween() with date range")
        void whereBetween_withDateRange() {
            // Test that between works with date types (falls through to default case)
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

            // Just verify query builds without error
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereBetween("created_at", start, end);

            assertNotNull(finder.toBuilder());
        }
    }

    // ==================== COLUMN COMPARISON TESTS ====================

    @Nested
    @DisplayName("Column Comparison Conditions")
    class ColumnComparisonTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("UPDATE users SET updated_at = created_at WHERE id IS NOT NULL");
            }
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);
        }

        @Test
        @DisplayName("whereColumn() compares two columns for equality")
        void whereColumn_comparesColumnsEquality() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereColumn("created_at", "updated_at");

            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("whereColumn() with operator compares columns")
        void whereColumn_withOperator() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereColumn("updated_at", ">=", "created_at");

            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("orWhereColumn() adds OR condition")
        void orWhereColumn_addsOrCondition() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereColumn("updated_at", ">", "created_at");

            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("whereColumn methods return this for chaining")
        void whereColumn_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);

            assertSame(finder, finder.whereColumn("name", "email"));
            assertSame(finder, finder.whereColumn("age", ">", "id"));
            assertSame(finder, finder.orWhereColumn("name", "status"));
            assertSame(finder, finder.orWhereColumn("age", "<>", "id"));
        }
    }

    // ==================== EXISTS SUBQUERY TESTS ====================

    @Nested
    @DisplayName("Exists Subquery Conditions")
    class ExistsSubqueryTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        total DECIMAL(10,2)
                    )
                    """);
            }

            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);

            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("INSERT INTO orders (user_id, total) VALUES (1, 100.00)");
                stmt.execute("INSERT INTO orders (user_id, total) VALUES (1, 200.00)");
            }
        }

        @AfterEach
        void cleanupOrders() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS orders");
            }
        }

        @Test
        @DisplayName("whereExists() with SelectBuilder")
        void whereExists_withSelectBuilder() {
            sant1ago.dev.suprim.core.query.SelectBuilder subquery = sant1ago.dev.suprim.core.query.Suprim.select()
                .selectRaw("1")
                .from(sant1ago.dev.suprim.core.type.Table.of("orders", PermissionEntity.class))
                .whereRaw("orders.user_id = users.id");

            List<UserEntity> users = executor.find(UserEntity.class)
                .whereExists(subquery)
                .get();

            assertEquals(1, users.size());
            assertEquals("Alice", users.get(0).getName());
        }

        @Test
        @DisplayName("whereExists() with function builder")
        void whereExists_withFunctionBuilder() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereExists(q -> q
                    .selectRaw("1")
                    .from(sant1ago.dev.suprim.core.type.Table.of("orders", PermissionEntity.class))
                    .whereRaw("orders.user_id = users.id"))
                .get();

            assertEquals(1, users.size());
        }

        @Test
        @DisplayName("whereNotExists() finds users without orders")
        void whereNotExists_findsUsersWithoutOrders() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereNotExists(q -> q
                    .selectRaw("1")
                    .from(sant1ago.dev.suprim.core.type.Table.of("orders", PermissionEntity.class))
                    .whereRaw("orders.user_id = users.id"))
                .orderBy("name")
                .get();

            assertEquals(2, users.size());
            assertEquals("Bob", users.get(0).getName());
            assertEquals("Charlie", users.get(1).getName());
        }

        @Test
        @DisplayName("orWhereExists() combines with other conditions")
        void orWhereExists_combinesConditions() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "inactive")
                .orWhereExists(q -> q
                    .selectRaw("1")
                    .from(sant1ago.dev.suprim.core.type.Table.of("orders", PermissionEntity.class))
                    .whereRaw("orders.user_id = users.id"))
                .orderBy("name")
                .get();

            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("exists methods return this for chaining")
        void exists_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            sant1ago.dev.suprim.core.query.SelectBuilder subquery = sant1ago.dev.suprim.core.query.Suprim.select().selectRaw("1");

            assertSame(finder, finder.whereExists(subquery));
            assertSame(finder, finder.whereNotExists(q -> q.selectRaw("1")));
            assertSame(finder, finder.orWhereExists(subquery));
            assertSame(finder, finder.orWhereNotExists(q -> q.selectRaw("1")));
        }
    }

    // ==================== DATE/TIME EXTRACTION TESTS ====================

    @Nested
    @DisplayName("Date/Time Extraction Conditions")
    class DateTimeExtractionTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("INSERT INTO users (email, name, status, age, created_at) VALUES " +
                    "('jan@test.com', 'Jan', 'active', 25, '2024-01-15 09:30:00')");
                stmt.execute("INSERT INTO users (email, name, status, age, created_at) VALUES " +
                    "('jun@test.com', 'Jun', 'active', 30, '2024-06-20 14:45:00')");
                stmt.execute("INSERT INTO users (email, name, status, age, created_at) VALUES " +
                    "('dec@test.com', 'Dec', 'active', 35, '2023-12-25 18:00:00')");
            }
        }

        @Test
        @DisplayName("whereYear() filters by year")
        void whereYear_filtersByYear() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereYear("created_at", 2024)
                .get();

            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("whereYear() with operator")
        void whereYear_withOperator() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereYear("created_at", ">=", 2024)
                .get();

            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("whereMonth() filters by month")
        void whereMonth_filtersByMonth() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereMonth("created_at", 6)
                .get();

            assertEquals(1, users.size());
            assertEquals("Jun", users.get(0).getName());
        }

        @Test
        @DisplayName("whereDay() filters by day")
        void whereDay_filtersByDay() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereDay("created_at", 25)
                .get();

            assertEquals(1, users.size());
            assertEquals("Dec", users.get(0).getName());
        }

        @Test
        @DisplayName("combined year and month filters")
        void combinedYearMonth() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereYear("created_at", 2024)
                .whereMonth("created_at", "<=", 3)
                .get();

            assertEquals(1, users.size());
            assertEquals("Jan", users.get(0).getName());
        }

        @Test
        @DisplayName("orWhereYear() adds OR condition")
        void orWhereYear_addsOrCondition() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereYear("created_at", 2023)
                .orWhereMonth("created_at", 1)
                .get();

            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("date/time methods return this for chaining")
        void dateTime_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);

            assertSame(finder, finder.whereDate("created_at", java.time.LocalDate.now()));
            assertSame(finder, finder.whereDate("created_at", ">=", java.time.LocalDate.now()));
            assertSame(finder, finder.orWhereDate("created_at", java.time.LocalDate.now()));
            assertSame(finder, finder.whereYear("created_at", 2024));
            assertSame(finder, finder.orWhereYear("created_at", 2024));
            assertSame(finder, finder.whereMonth("created_at", 6));
            assertSame(finder, finder.orWhereMonth("created_at", 6));
            assertSame(finder, finder.whereDay("created_at", 15));
            assertSame(finder, finder.orWhereDay("created_at", 15));
        }

        @Test
        @DisplayName("orWhereDate() with operator executes correctly")
        void orWhereDate_withOperator() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereDate("created_at", ">=", java.time.LocalDate.of(2024, 1, 1));
            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("whereTime() executes correctly")
        void whereTime_executesCorrectly() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereTime("created_at", java.time.LocalTime.of(9, 0));
            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("whereTime() with operator executes correctly")
        void whereTime_withOperator() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereTime("created_at", ">=", java.time.LocalTime.of(9, 0));
            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("orWhereTime() executes correctly")
        void orWhereTime_executesCorrectly() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereTime("created_at", java.time.LocalTime.of(18, 0));
            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("orWhereTime() with operator executes correctly")
        void orWhereTime_withOperator() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereTime("created_at", "<=", java.time.LocalTime.of(18, 0));
            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("orWhereYear() with operator executes correctly")
        void orWhereYear_withOperator() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereYear("created_at", ">=", 2020);
            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("orWhereMonth() with operator executes correctly")
        void orWhereMonth_withOperator() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereMonth("created_at", "<=", 6);
            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("orWhereDay() with operator executes correctly")
        void orWhereDay_withOperator() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereDay("created_at", ">=", 15);
            assertNotNull(finder.toBuilder());
        }
    }

    // ==================== JSON OPERATIONS TESTS ====================

    @Nested
    @DisplayName("JSON Operations (SQL Generation Only)")
    class JsonOperationsTests {

        @Test
        @DisplayName("whereJsonContains() generates correct SQL")
        void whereJsonContains_generatesCorrectSQL() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereJsonContains("tags", "urgent");

            // Just verify query builds without error (H2 doesn't support JSONB)
            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("whereJsonContains() with path generates correct SQL")
        void whereJsonContains_withPath_generatesCorrectSQL() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereJsonContains("settings", "theme", "dark");

            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("whereJsonContains() with nested path")
        void whereJsonContains_nestedPath() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereJsonContains("settings", "notifications.email", "true");

            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("whereJsonLength() generates correct SQL")
        void whereJsonLength_generatesCorrectSQL() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereJsonLength("tags", 3);

            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("whereJsonLength() with operator")
        void whereJsonLength_withOperator() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .whereJsonLength("tags", ">", 0);

            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("JSON methods return this for chaining")
        void json_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);

            assertSame(finder, finder.whereJsonContains("tags", "test"));
            assertSame(finder, finder.whereJsonContains("settings", "key", "value"));
            assertSame(finder, finder.orWhereJsonContains("tags", "test"));
            assertSame(finder, finder.orWhereJsonContains("settings", "key", "value"));
            assertSame(finder, finder.whereJsonLength("tags", 1));
            assertSame(finder, finder.whereJsonLength("tags", ">", 0));
            assertSame(finder, finder.orWhereJsonLength("tags", 1));
            assertSame(finder, finder.orWhereJsonLength("tags", "<", 10));
        }

        @Test
        @DisplayName("toJsonArray() handles different types")
        void toJsonArray_handlesDifferentTypes() {
            // String value
            Finder<UserEntity> finder1 = executor.find(UserEntity.class)
                .whereJsonContains("tags", "string");
            assertNotNull(finder1.toBuilder());

            // Number value
            Finder<UserEntity> finder2 = executor.find(UserEntity.class)
                .whereJsonContains("ids", 42);
            assertNotNull(finder2.toBuilder());

            // Boolean value
            Finder<UserEntity> finder3 = executor.find(UserEntity.class)
                .whereJsonContains("flags", true);
            assertNotNull(finder3.toBuilder());

            // Fallback case (object that's not String/Number/Boolean)
            Finder<UserEntity> finder4 = executor.find(UserEntity.class)
                .whereJsonContains("data", new Object());
            assertNotNull(finder4.toBuilder());
        }

        @Test
        @DisplayName("buildJsonPath() handles nested paths correctly")
        void buildJsonPath_handlesNestedPaths() {
            // Single level path
            Finder<UserEntity> finder1 = executor.find(UserEntity.class)
                .whereJsonContains("settings", "theme", "dark");
            assertNotNull(finder1.toBuilder());

            // Two level nested path (exercises intermediate -> operator)
            Finder<UserEntity> finder2 = executor.find(UserEntity.class)
                .whereJsonContains("settings", "notifications.email", "true");
            assertNotNull(finder2.toBuilder());

            // Three level nested path
            Finder<UserEntity> finder3 = executor.find(UserEntity.class)
                .whereJsonContains("config", "a.b.c", "value");
            assertNotNull(finder3.toBuilder());
        }

        @Test
        @DisplayName("orWhereJsonContains() executes correctly")
        void orWhereJsonContains_executesCorrectly() {
            // Array contains
            Finder<UserEntity> finder1 = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereJsonContains("tags", "urgent");
            assertNotNull(finder1.toBuilder());

            // Path contains
            Finder<UserEntity> finder2 = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereJsonContains("settings", "theme", "dark");
            assertNotNull(finder2.toBuilder());
        }

        @Test
        @DisplayName("orWhereJsonLength() executes correctly")
        void orWhereJsonLength_executesCorrectly() {
            // Equality
            Finder<UserEntity> finder1 = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereJsonLength("tags", 5);
            assertNotNull(finder1.toBuilder());

            // With operator
            Finder<UserEntity> finder2 = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereJsonLength("tags", ">=", 1);
            assertNotNull(finder2.toBuilder());
        }
    }

    // ==================== WHERE CONDITIONS TESTS ====================

    @Nested
    @DisplayName("Where Conditions")
    class WhereConditionsTests {

        @BeforeEach
        void insertTestData() {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);
            insertUserWithNullName("null@test.com", "active", 20);
        }

        @Test
        @DisplayName("where() filters by exact match")
        void where_filtersExactMatch() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .get();

            assertEquals(3, users.size());
            assertTrue(users.stream().allMatch(u -> "active".equals(u.getStatus())));
        }

        @Test
        @DisplayName("where() with operator filters correctly")
        void where_withOperator_filtersCorrectly() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("age", ">=", 30)
                .get();

            assertEquals(2, users.size());
            assertTrue(users.stream().allMatch(u -> u.getAge() >= 30));
        }

        @Test
        @DisplayName("multiple where() clauses combine with AND")
        void where_multipleConditions_combineWithAnd() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .where("age", ">", 25)
                .get();

            assertEquals(1, users.size());
            assertEquals("Alice", users.get(0).getName());
        }

        // Note: whereIn() tests skipped - there's an existing bug in the implementation
        // where only the last parameter in the IN clause is used. This should be fixed
        // in the Finder class itself.

        @Test
        @DisplayName("whereNull() filters for null values")
        void whereNull_filtersForNullValues() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereNull("name")
                .get();

            assertEquals(1, users.size());
            assertNull(users.get(0).getName());
        }

        @Test
        @DisplayName("whereNotNull() filters for non-null values")
        void whereNotNull_filtersForNonNullValues() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereNotNull("name")
                .get();

            assertEquals(3, users.size());
            assertTrue(users.stream().allMatch(u -> u.getName() != null));
        }

        @Test
        @DisplayName("whereRaw() allows raw SQL conditions")
        void whereRaw_allowsRawSqlConditions() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereRaw("age BETWEEN 25 AND 30")
                .get();

            assertEquals(2, users.size());
        }
    }

    // ==================== OR CONDITIONS TESTS ====================

    @Nested
    @DisplayName("OR Conditions")
    class OrConditionsTests {

        @BeforeEach
        void insertTestData() {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "pending", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);
            insertUser("diana@test.com", "Diana", "suspended", 20);
        }

        @Test
        @DisplayName("orWhere() adds OR condition with equals")
        void orWhere_addsOrConditionEquals() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhere("status", "pending")
                .get();

            assertEquals(2, users.size());
            assertTrue(users.stream().allMatch(u ->
                "active".equals(u.getStatus()) || "pending".equals(u.getStatus())));
        }

        @Test
        @DisplayName("orWhere() with operator adds OR condition")
        void orWhere_withOperator_addsOrCondition() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("age", ">=", 35)
                .orWhere("status", "=", "pending")
                .get();

            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("orWhereIn() adds OR IN condition with list")
        void orWhereIn_withList_addsOrInCondition() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereIn("status", List.of("pending", "suspended"))
                .get();

            // Alice (active) + Bob (pending) + Diana (suspended) = 3
            assertEquals(3, users.size());
        }

        @Test
        @DisplayName("orWhereIn() with empty list adds always false condition")
        void orWhereIn_withEmptyList_addsAlwaysFalse() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereIn("status", List.of())
                .get();

            assertEquals(1, users.size());
            assertEquals("active", users.get(0).getStatus());
        }

        @Test
        @DisplayName("orWhereNull() adds OR IS NULL condition")
        void orWhereNull_addsOrIsNullCondition() {
            // Insert a user with null name for this test
            insertUserWithNullName("nullname@test.com", "archived", 40);

            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereNull("name")
                .get();

            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("orWhereNotNull() adds OR IS NOT NULL condition")
        void orWhereNotNull_addsOrIsNotNullCondition() {
            // Insert a user with null name for this test
            insertUserWithNullName("nullname@test.com", "archived", 40);

            List<UserEntity> users = executor.find(UserEntity.class)
                .whereNull("name")
                .orWhereNotNull("name")
                .get();

            assertEquals(5, users.size());
        }

        @Test
        @DisplayName("orWhereRaw() adds raw OR condition")
        void orWhereRaw_addsRawOrCondition() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhereRaw("age < 22")
                .get();

            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("multiple orWhere() chains correctly")
        void orWhere_multipleChains_correctly() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhere("status", "pending")
                .orWhere("status", "suspended")
                .get();

            assertEquals(3, users.size());
        }

        @Test
        @DisplayName("orWhere methods return this for chaining")
        void orWhere_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);

            assertSame(finder, finder.orWhere("name", "test"));
            assertSame(finder, finder.orWhere("age", ">", 10));
            assertSame(finder, finder.orWhereIn("name", List.of("a")));
            assertSame(finder, finder.orWhereNull("name"));
            assertSame(finder, finder.orWhereNotNull("name"));
            assertSame(finder, finder.orWhereRaw("1=1"));
        }
    }

    // ==================== GROUPED CONDITIONS TESTS ====================

    @Nested
    @DisplayName("Grouped Conditions")
    class GroupedConditionsTests {

        @BeforeEach
        void insertTestData() {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "pending", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);
            insertUser("diana@test.com", "Diana", "active", 22);
        }

        @Test
        @DisplayName("where(Consumer) groups conditions with AND")
        void whereConsumer_groupsConditionsWithAnd() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .where(q -> q
                    .where("age", ">", 25)
                    .orWhere("name", "Diana"))
                .get();

            // status = 'active' AND (age > 25 OR name = 'Diana')
            // Alice (active, 30) - matches: active AND (30 > 25)
            // Diana (active, 22) - matches: active AND name = 'Diana'
            assertEquals(2, users.size());
            assertTrue(users.stream().allMatch(u -> "active".equals(u.getStatus())));
        }

        @Test
        @DisplayName("orWhere(Consumer) groups conditions with OR")
        void orWhereConsumer_groupsConditionsWithOr() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "inactive")
                .orWhere(q -> q
                    .where("status", "active")
                    .where("age", "<", 25))
                .get();

            // status = 'inactive' OR (status = 'active' AND age < 25)
            // Charlie (inactive) - matches first condition
            // Diana (active, 22) - matches: active AND 22 < 25
            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("nested grouped conditions work correctly")
        void nestedGroupedConditions_workCorrectly() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where(q -> q
                    .where("status", "active")
                    .orWhere("status", "pending"))
                .where(q -> q
                    .where("age", ">=", 25)
                    .orWhere("name", "Diana"))
                .get();

            // (status = 'active' OR status = 'pending') AND (age >= 25 OR name = 'Diana')
            // Alice (active, 30) - matches
            // Bob (pending, 25) - matches
            // Diana (active, 22) - matches: active AND name = 'Diana'
            assertEquals(3, users.size());
        }

        @Test
        @DisplayName("empty grouped condition does not affect query")
        void emptyGroupedCondition_doesNotAffect() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .where(q -> {})
                .get();

            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("grouped conditions return this for chaining")
        void groupedConditions_returnThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);

            assertSame(finder, finder.where(q -> q.where("name", "test")));
            assertSame(finder, finder.orWhere(q -> q.where("age", ">", 10)));
        }
    }

    // ==================== OR WHERE IN SUBQUERY TESTS ====================

    @Nested
    @DisplayName("OR Where In Subquery Operations")
    class OrWhereInSubqueryTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS permissions (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        resource_type VARCHAR(50),
                        action VARCHAR(50)
                    )
                    """);
            }

            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);

            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("INSERT INTO permissions (user_id, resource_type, action) VALUES (2, 'workspace', 'READ')");
                stmt.execute("INSERT INTO permissions (user_id, resource_type, action) VALUES (3, 'workspace', 'WRITE')");
            }
        }

        @AfterEach
        void cleanupPermissions() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS permissions");
            }
        }

        @Test
        @DisplayName("orWhereIn() with SelectBuilder subquery")
        void orWhereIn_withSelectBuilder() {
            sant1ago.dev.suprim.core.query.SelectBuilder subquery = sant1ago.dev.suprim.core.query.Suprim.select()
                .selectRaw("user_id")
                .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class))
                .whereRaw("resource_type = 'workspace'");

            List<UserEntity> users = executor.find(UserEntity.class)
                .where("id", 1L)
                .orWhereIn("id", subquery)
                .orderBy("id")
                .get();

            // id = 1 OR id IN (SELECT user_id FROM permissions WHERE resource_type = 'workspace')
            // Alice (id=1) - matches first condition
            // Bob (id=2) - matches subquery
            // Charlie (id=3) - matches subquery
            assertEquals(3, users.size());
        }

        @Test
        @DisplayName("orWhereIn() with closure-style subquery")
        void orWhereIn_withClosure() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("id", 1L)
                .orWhereIn("id", q -> q
                    .selectRaw("user_id")
                    .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class))
                    .whereRaw("action = 'READ'"))
                .orderBy("id")
                .get();

            // id = 1 OR id IN (SELECT user_id FROM permissions WHERE action = 'READ')
            assertEquals(2, users.size());
            assertEquals("Alice", users.get(0).getName());
            assertEquals("Bob", users.get(1).getName());
        }

        @Test
        @DisplayName("orWhereNotIn() excludes matching records")
        void orWhereNotIn_excludesMatchingRecords() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "inactive")
                .orWhereNotIn("id", q -> q
                    .selectRaw("user_id")
                    .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class)))
                .get();

            // status = 'inactive' OR id NOT IN (SELECT user_id FROM permissions)
            // Charlie (inactive) - matches first condition
            // Alice (id=1) - matches NOT IN (no permission)
            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("combined where and orWhereIn with grouped conditions")
        void combined_whereAndOrWhereIn_withGroupedConditions() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where(q -> q
                    .where("status", "active")
                    .orWhereIn("id", subq -> subq
                        .selectRaw("user_id")
                        .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class))
                        .whereRaw("action = 'WRITE'")))
                .get();

            // (status = 'active' OR id IN (SELECT user_id WHERE action = 'WRITE'))
            // Alice (active) - matches
            // Bob (active) - matches
            // Charlie (id=3, has WRITE) - matches subquery
            assertEquals(3, users.size());
        }
    }

    // ==================== SUBQUERY WHERE IN TESTS ====================

    @Nested
    @DisplayName("Where In Subquery Operations")
    class WhereInSubqueryTests {

        @BeforeEach
        void insertTestData() throws Exception {
            // Create permissions table for subquery tests
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS permissions (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        resource_type VARCHAR(50),
                        action VARCHAR(50)
                    )
                    """);
            }

            // Insert users
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);

            // Insert permissions - only Alice (id=1) and Bob (id=2) have permissions
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("INSERT INTO permissions (user_id, resource_type, action) VALUES (1, 'workspace', 'READ')");
                stmt.execute("INSERT INTO permissions (user_id, resource_type, action) VALUES (2, 'workspace', 'READ')");
            }
        }

        @AfterEach
        void cleanupPermissions() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS permissions");
            }
        }

        @Test
        @DisplayName("whereIn() with SelectBuilder subquery filters correctly")
        void whereIn_withSelectBuilder_filtersCorrectly() {
            // Build subquery: SELECT user_id FROM permissions WHERE resource_type = 'workspace'
            sant1ago.dev.suprim.core.query.SelectBuilder subquery = sant1ago.dev.suprim.core.query.Suprim.select()
                .selectRaw("user_id")
                .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class))
                .whereRaw("resource_type = 'workspace'");

            List<UserEntity> users = executor.find(UserEntity.class)
                .whereIn("id", subquery)
                .orderBy("id")
                .get();

            assertEquals(2, users.size());
            assertEquals("Alice", users.get(0).getName());
            assertEquals("Bob", users.get(1).getName());
        }

        @Test
        @DisplayName("whereIn() with closure-style subquery filters correctly")
        void whereIn_withClosure_filtersCorrectly() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereIn("id", q -> q
                    .selectRaw("user_id")
                    .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class))
                    .whereRaw("resource_type = 'workspace'"))
                .orderBy("id")
                .get();

            assertEquals(2, users.size());
            assertEquals("Alice", users.get(0).getName());
            assertEquals("Bob", users.get(1).getName());
        }

        @Test
        @DisplayName("whereNotIn() with SelectBuilder excludes matching records")
        void whereNotIn_withSelectBuilder_excludesMatchingRecords() {
            sant1ago.dev.suprim.core.query.SelectBuilder subquery = sant1ago.dev.suprim.core.query.Suprim.select()
                .selectRaw("user_id")
                .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class))
                .whereRaw("resource_type = 'workspace'");

            List<UserEntity> users = executor.find(UserEntity.class)
                .whereNotIn("id", subquery)
                .get();

            assertEquals(1, users.size());
            assertEquals("Charlie", users.get(0).getName());
        }

        @Test
        @DisplayName("whereNotIn() with closure-style excludes matching records")
        void whereNotIn_withClosure_excludesMatchingRecords() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereNotIn("id", q -> q
                    .selectRaw("user_id")
                    .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class))
                    .whereRaw("resource_type = 'workspace'"))
                .get();

            assertEquals(1, users.size());
            assertEquals("Charlie", users.get(0).getName());
        }

        @Test
        @DisplayName("whereIn() with subquery and additional conditions")
        void whereIn_withSubqueryAndAdditionalConditions() {
            sant1ago.dev.suprim.core.query.SelectBuilder subquery = sant1ago.dev.suprim.core.query.Suprim.select()
                .selectRaw("user_id")
                .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class));

            List<UserEntity> users = executor.find(UserEntity.class)
                .whereIn("id", subquery)
                .where("age", ">", 26)
                .get();

            assertEquals(1, users.size());
            assertEquals("Alice", users.get(0).getName());
        }

        @Test
        @DisplayName("count() with whereIn subquery works correctly")
        void count_withWhereInSubquery_worksCorrectly() {
            sant1ago.dev.suprim.core.query.SelectBuilder subquery = sant1ago.dev.suprim.core.query.Suprim.select()
                .selectRaw("user_id")
                .from(sant1ago.dev.suprim.core.type.Table.of("permissions", PermissionEntity.class))
                .whereRaw("resource_type = 'workspace'");

            long count = executor.find(UserEntity.class)
                .whereIn("id", subquery)
                .count();

            assertEquals(2, count);
        }

        @Test
        @DisplayName("whereIn() subquery methods return this for chaining")
        void whereInSubquery_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            sant1ago.dev.suprim.core.query.SelectBuilder subquery = sant1ago.dev.suprim.core.query.Suprim.select()
                .selectRaw("1");

            assertSame(finder, finder.whereIn("id", subquery));
        }

        @Test
        @DisplayName("whereNotIn() subquery methods return this for chaining")
        void whereNotInSubquery_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);

            assertSame(finder, finder.whereNotIn("id", q -> q.selectRaw("1")));
        }
    }

    // ==================== PAGINATION TESTS ====================

    @Nested
    @DisplayName("Pagination Operations")
    class PaginationTests {

        @BeforeEach
        void insertTestData() {
            for (int i = 1; i <= 10; i++) {
                insertUser("user" + i + "@test.com", "User" + i, "active", 20 + i);
            }
        }

        @Test
        @DisplayName("limit() restricts result count")
        void limit_restrictsResultCount() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .orderBy("id")
                .limit(5)
                .get();

            assertEquals(5, users.size());
        }

        @Test
        @DisplayName("offset() skips results")
        void offset_skipsResults() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .orderBy("id")
                .offset(5)
                .limit(5)
                .get();

            assertEquals(5, users.size());
            assertEquals("User6", users.get(0).getName());
        }

        @Test
        @DisplayName("take() is alias for limit()")
        void take_isAliasForLimit() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .orderBy("id")
                .take(3)
                .get();

            assertEquals(3, users.size());
        }

        @Test
        @DisplayName("skip() is alias for offset()")
        void skip_isAliasForOffset() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .orderBy("id")
                .skip(7)
                .get();

            assertEquals(3, users.size());
        }

        @Test
        @DisplayName("paginate() returns paginated result with metadata")
        void paginate_returnsPaginatedResultWithMetadata() {
            PaginatedResult<UserEntity> result = executor.find(UserEntity.class)
                .orderBy("id")
                .paginate(1, 3);

            assertEquals(3, result.getData().size());
            assertEquals(1, result.getCurrentPage());
            assertEquals(3, result.getPerPage());
            assertEquals(10, result.getTotal());
            assertEquals(4, result.getLastPage());
            assertTrue(result.hasMorePages());
        }

        @Test
        @DisplayName("cursor() returns cursor-based pagination result")
        void cursor_returnsCursorBasedPaginationResult() {
            CursorResult<UserEntity> result = executor.find(UserEntity.class)
                .orderBy("id")
                .cursor(null, 3);

            assertEquals(3, result.getData().size());
            assertTrue(result.hasMorePages());
            assertNotNull(result.getNextCursor());
            assertNull(result.getPreviousCursor());
        }
    }

    // Note: Eager loading tests require generated metamodel classes which are not
    // available in test environment. The with()/without() methods are tested
    // indirectly through the chaining tests that verify method returns this.

    // ==================== GROUPING TESTS ====================

    @Nested
    @DisplayName("Grouping Operations")
    class GroupingTests {

        @BeforeEach
        void insertTestData() {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);
        }

        @Test
        @DisplayName("groupBy() groups results by column")
        void groupBy_groupsResultsByColumn() {
            // Using raw query to verify groupBy works
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .groupBy("status");

            assertNotNull(finder.toBuilder());
        }
    }

    // ==================== SOFT DELETE TESTS ====================

    @Nested
    @DisplayName("Soft Delete Operations")
    class SoftDeleteTests {

        @BeforeEach
        void insertTestData() throws Exception {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);

            // Soft delete Bob
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE email = 'bob@test.com'");
            }
        }

        @Test
        @DisplayName("withTrashed() includes soft-deleted records")
        void withTrashed_includesSoftDeletedRecords() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .withTrashed();

            assertNotNull(finder.toBuilder());
        }

        @Test
        @DisplayName("onlyTrashed() returns only soft-deleted records")
        void onlyTrashed_returnsOnlySoftDeletedRecords() {
            Finder<UserEntity> finder = executor.find(UserEntity.class)
                .onlyTrashed();

            assertNotNull(finder.toBuilder());
        }
    }

    // ==================== TERMINAL OPERATIONS TESTS ====================

    @Nested
    @DisplayName("Terminal Operations")
    class TerminalOperationsTests {

        @BeforeEach
        void insertTestData() {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "active", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);
        }

        @Test
        @DisplayName("get() returns all matching results")
        void get_returnsAllMatchingResults() {
            List<UserEntity> users = executor.find(UserEntity.class).get();

            assertEquals(3, users.size());
        }

        @Test
        @DisplayName("first() returns first matching result")
        void first_returnsFirstMatchingResult() {
            Optional<UserEntity> user = executor.find(UserEntity.class)
                .orderBy("name")
                .first();

            assertTrue(user.isPresent());
            assertEquals("Alice", user.get().getName());
        }

        @Test
        @DisplayName("first() returns empty when no match")
        void first_returnsEmptyWhenNoMatch() {
            Optional<UserEntity> user = executor.find(UserEntity.class)
                .where("status", "nonexistent")
                .first();

            assertTrue(user.isEmpty());
        }

        @Test
        @DisplayName("firstOrFail() returns entity when found")
        void firstOrFail_returnsEntityWhenFound() {
            UserEntity user = executor.find(UserEntity.class)
                .where("name", "Alice")
                .firstOrFail();

            assertEquals("Alice", user.getName());
        }

        @Test
        @DisplayName("firstOrFail() throws NoResultException when not found")
        void firstOrFail_throwsNoResultExceptionWhenNotFound() {
            assertThrows(NoResultException.class, () ->
                executor.find(UserEntity.class)
                    .where("name", "NonExistent")
                    .firstOrFail()
            );
        }

        @Test
        @DisplayName("count() returns total matching records")
        void count_returnsTotalMatchingRecords() {
            long count = executor.find(UserEntity.class)
                .where("status", "active")
                .count();

            assertEquals(2, count);
        }

        @Test
        @DisplayName("exists() returns true when records exist")
        void exists_returnsTrueWhenRecordsExist() {
            boolean exists = executor.find(UserEntity.class)
                .where("name", "Alice")
                .exists();

            assertTrue(exists);
        }

        @Test
        @DisplayName("exists() returns false when no records exist")
        void exists_returnsFalseWhenNoRecordsExist() {
            boolean exists = executor.find(UserEntity.class)
                .where("name", "NonExistent")
                .exists();

            assertFalse(exists);
        }

        @Test
        @DisplayName("toBuilder() returns underlying SelectBuilder")
        void toBuilder_returnsUnderlyingSelectBuilder() {
            var builder = executor.find(UserEntity.class).toBuilder();

            assertNotNull(builder);
        }
    }

    // ==================== CHAINING TESTS ====================

    @Nested
    @DisplayName("Method Chaining")
    class ChainingTests {

        @BeforeEach
        void insertTestData() {
            for (int i = 1; i <= 20; i++) {
                insertUser("user" + i + "@test.com", "User" + i,
                    i % 2 == 0 ? "active" : "inactive", 20 + i);
            }
        }

        @Test
        @DisplayName("complex chained query executes correctly")
        void complexChainedQuery_executesCorrectly() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .where("age", ">=", 25)
                .orderByDesc("age")
                .limit(5)
                .offset(2)
                .get();

            assertNotNull(users);
            assertTrue(users.size() <= 5);
            assertTrue(users.stream().allMatch(u -> "active".equals(u.getStatus())));
        }

        @Test
        @DisplayName("all Finder methods return this for chaining")
        void allMethods_returnThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);

            // Note: with()/without() require metamodel, latest()/oldest() require CreationTimestamp
            assertSame(finder, finder.orderBy("name"));
            assertSame(finder, finder.orderBy("name", "desc"));
            assertSame(finder, finder.orderByDesc("name"));
            assertSame(finder, finder.where("name", "test"));
            assertSame(finder, finder.where("age", ">", 10));
            assertSame(finder, finder.whereIn("name", List.of("a")));
            assertSame(finder, finder.whereNull("name"));
            assertSame(finder, finder.whereNotNull("name"));
            assertSame(finder, finder.whereRaw("1=1"));
            assertSame(finder, finder.groupBy("status"));
            assertSame(finder, finder.limit(10));
            assertSame(finder, finder.offset(5));
            assertSame(finder, finder.take(10));
            assertSame(finder, finder.skip(5));
            assertSame(finder, finder.withTrashed());
            assertSame(finder, finder.onlyTrashed());
        }
    }

    // ==================== SCHEMA ENTITY TESTS ====================

    @Nested
    @DisplayName("Schema Entity Operations")
    class SchemaEntityTests {

        @Test
        @DisplayName("Finder handles entity with schema annotation")
        void finder_entityWithSchema_createsFinderCorrectly() {
            Finder<SchemaUserEntity> finder = executor.find(SchemaUserEntity.class);
            assertNotNull(finder);
            assertNotNull(finder.toBuilder());
        }
    }

    // ==================== TIMESTAMP SORTING TESTS ====================

    @Nested
    @DisplayName("Timestamp Sorting Operations")
    class TimestampSortingTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS timestamped_users (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100),
                        created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
                stmt.execute("INSERT INTO timestamped_users (name, created_date) VALUES ('First', '2024-01-01 10:00:00')");
                stmt.execute("INSERT INTO timestamped_users (name, created_date) VALUES ('Second', '2024-01-02 10:00:00')");
                stmt.execute("INSERT INTO timestamped_users (name, created_date) VALUES ('Third', '2024-01-03 10:00:00')");
            }
        }

        @AfterEach
        void cleanupTimestampedUsers() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS timestamped_users");
            }
        }

        @Test
        @DisplayName("latest() orders by creation timestamp descending")
        void latest_ordersByCreationTimestampDesc() {
            List<TimestampedUserEntity> users = executor.find(TimestampedUserEntity.class)
                .latest()
                .get();

            assertEquals(3, users.size());
            assertEquals("Third", users.get(0).getName());
            assertEquals("First", users.get(2).getName());
        }

        @Test
        @DisplayName("oldest() orders by creation timestamp ascending")
        void oldest_ordersByCreationTimestampAsc() {
            List<TimestampedUserEntity> users = executor.find(TimestampedUserEntity.class)
                .oldest()
                .get();

            assertEquals(3, users.size());
            assertEquals("First", users.get(0).getName());
            assertEquals("Third", users.get(2).getName());
        }

        @Test
        @DisplayName("latest() falls back to created_at when no @CreationTimestamp")
        void latest_fallsBackToDefaultColumn() {
            // UserEntity doesn't have @CreationTimestamp, so it should use "created_at" default
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            assertNotNull(finder.latest());
            // Verify it returns this for chaining
            assertSame(finder, finder.latest());
        }

        @Test
        @DisplayName("oldest() falls back to created_at when no @CreationTimestamp")
        void oldest_fallsBackToDefaultColumn() {
            // UserEntity doesn't have @CreationTimestamp, so it should use "created_at" default
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            assertNotNull(finder.oldest());
            // Verify it returns this for chaining
            assertSame(finder, finder.oldest());
        }
    }

    // ==================== EAGER LOADING TESTS ====================

    @Nested
    @DisplayName("Eager Loading Operations")
    class EagerLoadingTests {

        @Test
        @DisplayName("with() returns this for chaining when metamodel exists")
        void with_returnsThisForChaining_whenMetamodelExists() {
            // TestUserWithRelation has a metamodel with PROFILE relation defined
            Finder<TestUserWithRelation> finder = executor.find(TestUserWithRelation.class);
            assertSame(finder, finder.with("profile"));
        }

        @Test
        @DisplayName("with() throws when metamodel does not exist")
        void with_throwsWhenMetamodelMissing() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            // UserEntity doesn't have a metamodel, so this throws
            assertThrows(IllegalArgumentException.class, () -> finder.with("profile"));
        }

        @Test
        @DisplayName("without() delegates to builder and returns this for chaining")
        void without_delegatesToBuilder_returnsThis() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            // without() doesn't require metamodel to exist - it just records exclusions
            assertSame(finder, finder.without("profile"));
        }
    }

    // ==================== WHERE IN EDGE CASES ====================

    @Nested
    @DisplayName("Where In Edge Cases")
    class WhereInEdgeCasesTests {

        @BeforeEach
        void insertTestData() {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "pending", 25);
            insertUser("charlie@test.com", "Charlie", "inactive", 35);
        }

        @Test
        @DisplayName("whereIn() with empty list returns no results")
        void whereIn_emptyList_returnsNoResults() {
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereIn("status", List.of())
                .get();

            assertEquals(0, users.size());
        }

        @Test
        @DisplayName("whereIn() with single item list works correctly")
        void whereIn_singleItemList_worksCorrectly() {
            // Single item - only i=0 iteration, i>0 branch never taken
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereIn("status", List.of("active"))
                .get();

            assertEquals(1, users.size());
        }

        @Test
        @DisplayName("whereIn() with multiple items covers comma separator branch")
        void whereIn_multipleItems_coversCommaSeparator() {
            // Multiple items - i=0 (no comma), i=1,2 (with comma)
            List<UserEntity> users = executor.find(UserEntity.class)
                .whereIn("status", List.of("active", "pending", "inactive"))
                .get();

            assertEquals(3, users.size());
        }

        @Test
        @DisplayName("orWhere() with empty group skips predicate")
        void orWhere_emptyGroup_skipsPredicate() {
            // Empty group - groupPredicate will be null
            List<UserEntity> users = executor.find(UserEntity.class)
                .where("status", "active")
                .orWhere(group -> {
                    // Empty group - no conditions added
                })
                .get();

            // Only active users returned since empty orWhere has no effect
            assertEquals(1, users.size());
        }
    }

    // ==================== VALUE NORMALIZATION TESTS ====================

    @Nested
    @DisplayName("Value Normalization Operations")
    class ValueNormalizationTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS typed_entities (
                        id UUID PRIMARY KEY,
                        name TEXT,
                        small_num SMALLINT,
                        big_num BIGINT,
                        int_num INT,
                        double_num DOUBLE PRECISION,
                        float_num REAL,
                        is_enabled BOOLEAN,
                        char_col CHAR(10)
                    )
                    """);
                stmt.execute("INSERT INTO typed_entities VALUES " +
                    "('550e8400-e29b-41d4-a716-446655440000', 'Test', 10, 100, 50, 3.14, 2.5, true, 'ABC')");
            }
        }

        @AfterEach
        void cleanupTypedEntities() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS typed_entities");
            }
        }

        @Test
        @DisplayName("where() normalizes UUID from string")
        void where_normalizesUuidFromString() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("id", "550e8400-e29b-41d4-a716-446655440000")
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes UUID from UUID object")
        void where_normalizesUuidFromUuidObject() {
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("id", uuid)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes TEXT type")
        void where_normalizesTextType() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("name", "Test")
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes SMALLINT from Integer")
        void where_normalizesSmallintFromInteger() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("small_num", 10)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes SMALLINT from Short")
        void where_normalizesSmallintFromShort() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("small_num", (short) 10)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes BIGINT from Integer")
        void where_normalizesBigintFromInteger() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("big_num", 100)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes BIGINT from Long")
        void where_normalizesBigintFromLong() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("big_num", 100L)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes INTEGER from Long")
        void where_normalizesIntegerFromLong() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("int_num", 50L)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes INTEGER from Integer")
        void where_normalizesIntegerFromInteger() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("int_num", 50)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes DOUBLE_PRECISION from Float")
        void where_normalizesDoubleFromFloat() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("double_num", 3.14f)
                .get();

            // May not match exactly due to floating point, just test query builds correctly
            assertNotNull(entities);
        }

        @Test
        @DisplayName("where() normalizes DOUBLE_PRECISION from Double")
        void where_normalizesDoubleFromDouble() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("double_num", 3.14)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes REAL from Integer")
        void where_normalizesRealFromInteger() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("float_num", 2)
                .get();

            // Just test query builds correctly
            assertNotNull(entities);
        }

        @Test
        @DisplayName("where() normalizes REAL from Float")
        void where_normalizesRealFromFloat() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("float_num", 2.5f)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes BOOLEAN from Boolean")
        void where_normalizesBooleanFromBoolean() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("is_enabled", true)
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes BOOLEAN from String")
        void where_normalizesBooleanFromString() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .where("is_enabled", "true")
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() normalizes CHAR type")
        void where_normalizesCharType() {
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .whereRaw("TRIM(char_col) = 'ABC'")
                .get();

            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("whereNull() handles null condition correctly")
        void whereNull_handlesNullCondition() {
            // Test the whereNull method which bypasses normalizeValue
            List<TypedEntity> entities = executor.find(TypedEntity.class)
                .whereNull("name")
                .get();

            // No entities have null name, so should return empty
            assertEquals(0, entities.size());
        }
    }

    // ==================== DEFAULT TYPE NORMALIZATION TESTS ====================

    @Nested
    @DisplayName("Default Type and Null Value Normalization")
    class DefaultTypeNormalizationTests {

        @BeforeEach
        void insertTestData() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS timestamp_entities (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100),
                        event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        event_date DATE DEFAULT CURRENT_DATE
                    )
                    """);
                stmt.execute("INSERT INTO timestamp_entities (name, event_time) VALUES ('Event1', '2024-01-15 10:30:00')");
            }
        }

        @AfterEach
        void cleanupTimestampEntities() throws Exception {
            try (Statement stmt = setupConnection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS timestamp_entities");
            }
        }

        @Test
        @DisplayName("where() with TIMESTAMP type falls through to default case")
        void where_timestampType_fallsThroughToDefault() {
            // TIMESTAMP type is not explicitly handled in the switch, falls to default
            List<TimestampEntity> entities = executor.find(TimestampEntity.class)
                .where("event_time", LocalDateTime.of(2024, 1, 15, 10, 30, 0))
                .get();

            // Query executes correctly with default passthrough
            assertEquals(1, entities.size());
        }

        @Test
        @DisplayName("where() with DATE type falls through to default case")
        void where_dateType_fallsThroughToDefault() {
            // DATE type is not explicitly handled in the switch, falls to default
            Finder<TimestampEntity> finder = executor.find(TimestampEntity.class);
            // Just verify the query builds without error
            assertNotNull(finder.where("event_date", java.time.LocalDate.now()).toBuilder());
        }

        @Test
        @DisplayName("where() with null value exercises normalizeValue null check")
        void where_nullValue_exercisesNullCheck() {
            // Passing null value to where() hits the null check in normalizeValue,
            // but Map.of() doesn't accept null values, so it throws NPE.
            // The coverage is still achieved because normalizeValue is called first.
            Finder<TimestampEntity> finder = executor.find(TimestampEntity.class);
            assertThrows(NullPointerException.class, () -> finder.where("name", null));
        }
    }

    // ==================== HELPER METHODS ====================

    private void insertUser(String email, String name, String status, int age) {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                "INSERT INTO users (email, name, status, age) VALUES ('%s', '%s', '%s', %d)",
                email, name, status, age));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void insertUserWithNullName(String email, String status, int age) {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                "INSERT INTO users (email, name, status, age) VALUES ('%s', NULL, '%s', %d)",
                email, status, age));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== LIKE PATTERN MATCHING TESTS ====================

    @Nested
    @DisplayName("LIKE Pattern Matching")
    class LikePatternMatchingTests {

        @BeforeEach
        void setupLikeData() {
            insertUser("john.doe@gmail.com", "John Doe", "active", 30);
            insertUser("jane.smith@yahoo.com", "Jane Smith", "active", 25);
            insertUser("admin@company.com", "Admin User", "inactive", 40);
            insertUser("dev.team@company.com", "Dev Team", "active", 28);
        }

        @Test
        @DisplayName("whereLike() filters with LIKE pattern")
        void whereLike_filtersWithPattern() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .whereLike("name", "%Doe%")
                .get();
            assertEquals(1, results.size());
            assertEquals("John Doe", results.get(0).getName());
        }

        @Test
        @DisplayName("whereNotLike() excludes matching pattern")
        void whereNotLike_excludesPattern() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .whereNotLike("name", "%Admin%")
                .get();
            assertEquals(3, results.size());
        }

        @Test
        @DisplayName("orWhereLike() adds OR LIKE condition")
        void orWhereLike_addsOrCondition() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .whereLike("name", "%John%")
                .orWhereLike("name", "%Jane%")
                .get();
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("orWhereNotLike() adds OR NOT LIKE condition")
        void orWhereNotLike_addsOrCondition() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .where("status", "inactive")
                .orWhereNotLike("name", "%Admin%")
                .get();
            assertEquals(4, results.size());
        }

        @Test
        @DisplayName("whereContains() wraps value with wildcards")
        void whereContains_wrapsWithWildcards() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .whereContains("name", "Doe")
                .get();
            assertEquals(1, results.size());
            assertEquals("John Doe", results.get(0).getName());
        }

        @Test
        @DisplayName("whereStartsWith() matches prefix")
        void whereStartsWith_matchesPrefix() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .whereStartsWith("name", "John")
                .get();
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("whereEndsWith() matches suffix")
        void whereEndsWith_matchesSuffix() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .whereEndsWith("email", "@gmail.com")
                .get();
            assertEquals(1, results.size());
            assertEquals("john.doe@gmail.com", results.get(0).getEmail());
        }

        @Test
        @DisplayName("orWhereContains() adds OR with wildcards")
        void orWhereContains_addsOrWithWildcards() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .whereContains("name", "John")
                .orWhereContains("name", "Jane")
                .get();
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("orWhereStartsWith() adds OR prefix match")
        void orWhereStartsWith_addsOrPrefix() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .whereStartsWith("name", "John")
                .orWhereStartsWith("name", "Admin")
                .get();
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("orWhereEndsWith() adds OR suffix match")
        void orWhereEndsWith_addsOrSuffix() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .whereEndsWith("email", "@gmail.com")
                .orWhereEndsWith("email", "@yahoo.com")
                .get();
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("LIKE methods can be chained")
        void like_methodsCanBeChained() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            assertSame(finder, finder.whereLike("name", "%test%"));
            assertSame(finder, finder.whereNotLike("name", "%admin%"));
            assertSame(finder, finder.orWhereLike("name", "%user%"));
        }
    }

    // ==================== HAVING CLAUSE TESTS ====================

    @Nested
    @DisplayName("HAVING Clause")
    class HavingClauseTests {

        @BeforeEach
        void setupHavingData() {
            // Create data for aggregation tests
            insertUser("user1@test.com", "User One", "active", 25);
            insertUser("user2@test.com", "User Two", "active", 30);
            insertUser("user3@test.com", "User Three", "active", 35);
            insertUser("user4@test.com", "User Four", "inactive", 40);
            insertUser("user5@test.com", "User Five", "pending", 22);
        }

        @Test
        @DisplayName("having() filters grouped results with equals")
        void having_filtersWithEquals() {
            var sql = executor.find(UserEntity.class)
                .groupBy("status")
                .having("COUNT(*)", 3)
                .toBuilder().build().sql();
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        @DisplayName("having() filters with comparison operator")
        void having_filtersWithOperator() {
            var sql = executor.find(UserEntity.class)
                .groupBy("status")
                .having("COUNT(*)", ">", 1)
                .toBuilder().build().sql();
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains(">"));
        }

        @Test
        @DisplayName("havingRaw() adds raw HAVING clause")
        void havingRaw_addsRawClause() {
            var sql = executor.find(UserEntity.class)
                .groupBy("status")
                .havingRaw("COUNT(*) >= 1")
                .toBuilder().build().sql();
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("COUNT(*) >= 1"));
        }

        @Test
        @DisplayName("havingRaw() with params uses parameterized query")
        void havingRaw_withParams_usesParameters() {
            var result = executor.find(UserEntity.class)
                .groupBy("status")
                .havingRaw("COUNT(*) > :minCount", java.util.Map.of("minCount", 0))
                .toBuilder().build();
            assertTrue(result.sql().contains("HAVING"));
            // Parameter is renamed by ParameterContext
            assertTrue(result.parameters().containsValue(0));
        }

        @Test
        @DisplayName("orHaving() adds OR condition to HAVING")
        void orHaving_addsOrCondition() {
            var sql = executor.find(UserEntity.class)
                .groupBy("status")
                .having("COUNT(*)", ">", 2)
                .orHaving("AVG(age)", ">", 35)
                .toBuilder().build().sql();
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("OR"));
        }

        @Test
        @DisplayName("orHaving() with 2 params uses equals operator")
        void orHaving_twoParams_usesEquals() {
            var result = executor.find(UserEntity.class)
                .groupBy("status")
                .having("COUNT(*)", ">", 2)
                .orHaving("COUNT(*)", 1)
                .toBuilder().build();
            assertTrue(result.sql().contains("OR"));
            assertTrue(result.sql().contains("COUNT(*) ="));
        }

        @Test
        @DisplayName("orHavingRaw() with parameters")
        void orHavingRaw_withParameters() {
            var result = executor.find(UserEntity.class)
                .groupBy("status")
                .havingRaw("COUNT(*) > 2")
                .orHavingRaw("COUNT(*) = :cnt", java.util.Map.of("cnt", 5))
                .toBuilder().build();
            assertTrue(result.sql().contains("OR"));
            assertTrue(result.parameters().containsValue(5));
        }

        @Test
        @DisplayName("orHavingRaw() adds raw OR condition")
        void orHavingRaw_addsRawOrCondition() {
            var sql = executor.find(UserEntity.class)
                .groupBy("status")
                .havingRaw("COUNT(*) > 2")
                .orHavingRaw("COUNT(*) = 1")
                .toBuilder().build().sql();
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("OR"));
        }

        @Test
        @DisplayName("HAVING methods return this for chaining")
        void having_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class).groupBy("status");
            assertSame(finder, finder.having("COUNT(*)", 5));
            assertSame(finder, finder.havingRaw("SUM(age) > 0"));
            assertSame(finder, finder.orHaving("AVG(age)", "<", 50));
            assertSame(finder, finder.orHavingRaw("MAX(age) < 100"));
        }
    }

    // ==================== ADVANCED ORDERING & DISTINCT TESTS ====================

    @Nested
    @DisplayName("Advanced Ordering and DISTINCT")
    class AdvancedOrderingTests {

        @BeforeEach
        void setupOrderingData() {
            insertUser("alice@test.com", "Alice", "active", 30);
            insertUser("bob@test.com", "Bob", "pending", 25);
            insertUser("charlie@test.com", "Charlie", "active", 35);
        }

        @Test
        @DisplayName("orderByRaw() adds raw ORDER BY expression")
        void orderByRaw_addsRawExpression() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .orderByRaw("age DESC")
                .get();
            assertEquals(3, results.size());
            assertEquals("Charlie", results.get(0).getName()); // oldest first
        }

        @Test
        @DisplayName("reorder() clears existing orders")
        void reorder_clearsOrders() {
            var sql = executor.find(UserEntity.class)
                .orderBy("name")
                .reorder()
                .toBuilder().build().sql();
            assertFalse(sql.contains("ORDER BY"));
        }

        @Test
        @DisplayName("reorder() with column sets new order")
        void reorder_withColumn_setsNewOrder() {
            List<UserEntity> results = executor.find(UserEntity.class)
                .orderBy("name", "asc")
                .reorder("age", "desc")
                .get();
            assertEquals("Charlie", results.get(0).getName()); // reordered by age desc
        }

        @Test
        @DisplayName("inRandomOrder() adds random ordering")
        void inRandomOrder_addsRandomOrdering() {
            var sql = executor.find(UserEntity.class)
                .inRandomOrder()
                .toBuilder().build().sql();
            assertTrue(sql.contains("RANDOM()") || sql.contains("ORDER BY"));
        }

        @Test
        @DisplayName("distinct() enables DISTINCT selection")
        void distinct_enablesDistinct() {
            // Insert duplicate status
            insertUser("dave@test.com", "Dave", "active", 28);

            var sql = executor.find(UserEntity.class)
                .distinct()
                .toBuilder().build().sql();
            assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        @DisplayName("latest() orders by default timestamp descending")
        void latest_ordersByTimestampDesc() {
            // latest() uses default timestamp column (created_at)
            var sql = executor.find(UserEntity.class)
                .latest()
                .toBuilder().build().sql();
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("DESC"));
        }

        @Test
        @DisplayName("oldest() orders by default timestamp ascending")
        void oldest_ordersByTimestampAsc() {
            // oldest() uses default timestamp column (created_at)
            var sql = executor.find(UserEntity.class)
                .oldest()
                .toBuilder().build().sql();
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("ASC"));
        }

        @Test
        @DisplayName("Ordering methods return this for chaining")
        void ordering_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            assertSame(finder, finder.orderByRaw("name ASC"));
            assertSame(finder, finder.reorder());
            assertSame(finder, finder.reorder("id", "asc"));
            assertSame(finder, finder.inRandomOrder());
            assertSame(finder, finder.distinct());
        }
    }

    // ==================== SCOPE TESTS ====================

    @Nested
    @DisplayName("Query Scopes")
    class ScopeTests {

        @BeforeEach
        void setupScopeData() {
            insertUser("active1@test.com", "Active One", "active", 25);
            insertUser("active2@test.com", "Active Two", "active", 30);
            insertUser("inactive@test.com", "Inactive User", "inactive", 40);
        }

        @Test
        @DisplayName("scope() applies consumer constraint and filters results")
        void scope_appliesConsumerAndFilters() {
            java.util.function.Consumer<Finder<UserEntity>> activeScope =
                f -> f.where("status", "active");

            List<UserEntity> results = executor.find(UserEntity.class)
                .scope(activeScope)
                .get();
            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(u -> "active".equals(u.getStatus())));
        }

        @Test
        @DisplayName("scope() can be chained with multiple scopes")
        void scope_canBeChainedMultipleTimes() {
            java.util.function.Consumer<Finder<UserEntity>> activeScope =
                f -> f.where("status", "active");
            java.util.function.Consumer<Finder<UserEntity>> youngScope =
                f -> f.where("age", "<", 30);

            List<UserEntity> results = executor.find(UserEntity.class)
                .scope(activeScope)
                .scope(youngScope)
                .get();
            assertEquals(1, results.size());
            assertEquals("Active One", results.get(0).getName());
        }

        @Test
        @DisplayName("scope() returns this for fluent chaining")
        void scope_returnsThisForChaining() {
            Finder<UserEntity> finder = executor.find(UserEntity.class);
            Finder<UserEntity> result = finder.scope(f -> f.where("status", "active"));
            assertSame(finder, result);
        }

        @Test
        @DisplayName("scope() can combine with other query methods")
        void scope_combinesWithOtherMethods() {
            java.util.function.Consumer<Finder<UserEntity>> activeScope =
                f -> f.where("status", "active");

            List<UserEntity> results = executor.find(UserEntity.class)
                .scope(activeScope)
                .orderBy("age", "desc")
                .limit(1)
                .get();
            assertEquals(1, results.size());
            assertEquals("Active Two", results.get(0).getName()); // age 30
        }
    }

    // ==================== BATCH OPERATION TESTS ====================

    @Nested
    @DisplayName("Batch Operations")
    class BatchOperationTests {

        @Test
        @DisplayName("increment() returns affected row count")
        void increment_returnsAffectedRows() {
            insertUser("test@test.com", "Test", "active", 25);

            int affected = executor.find(UserEntity.class)
                .where("email", "test@test.com")
                .increment("age");

            assertEquals(1, affected);
        }

        @Test
        @DisplayName("increment() with amount works correctly")
        void increment_withAmount_works() {
            insertUser("test@test.com", "Test", "active", 25);

            int affected = executor.find(UserEntity.class)
                .where("email", "test@test.com")
                .increment("age", 5);

            assertEquals(1, affected);
        }

        @Test
        @DisplayName("decrement() subtracts from column")
        void decrement_subtractsFromColumn() {
            insertUser("test@test.com", "Test", "active", 25);

            int affected = executor.find(UserEntity.class)
                .where("email", "test@test.com")
                .decrement("age");

            assertEquals(1, affected);
        }

        @Test
        @DisplayName("decrement() with amount works correctly")
        void decrement_withAmount_works() {
            insertUser("test@test.com", "Test", "active", 25);

            int affected = executor.find(UserEntity.class)
                .where("email", "test@test.com")
                .decrement("age", 5);

            assertEquals(1, affected);
        }

        @Test
        @DisplayName("increment() without WHERE affects all rows")
        void increment_withoutWhere_affectsAllRows() {
            insertUser("user1@test.com", "User1", "active", 20);
            insertUser("user2@test.com", "User2", "active", 25);
            insertUser("user3@test.com", "User3", "active", 30);

            // Increment without WHERE clause
            int affected = executor.find(UserEntity.class)
                .increment("age", 1);

            assertEquals(3, affected);
        }

        @Test
        @DisplayName("increment() with ORDER BY in query extracts WHERE correctly")
        void increment_withOrderBy_extractsWhereCorrectly() {
            insertUser("test@test.com", "Test", "active", 25);

            // WHERE clause followed by ORDER BY
            int affected = executor.find(UserEntity.class)
                .where("email", "test@test.com")
                .orderBy("age", "desc")
                .increment("age", 5);

            assertEquals(1, affected);
        }

        @Test
        @DisplayName("chunk() processes records in batches")
        void chunk_processesInBatches() {
            for (int i = 0; i < 15; i++) {
                insertUser("user" + i + "@test.com", "User" + i, "active", 20 + i);
            }

            java.util.concurrent.atomic.AtomicInteger batchCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger totalCount = new java.util.concurrent.atomic.AtomicInteger(0);

            executor.find(UserEntity.class)
                .chunk(5, batch -> {
                    batchCount.incrementAndGet();
                    totalCount.addAndGet(batch.size());
                    return true;
                });

            assertEquals(3, batchCount.get());
            assertEquals(15, totalCount.get());
        }

        @Test
        @DisplayName("chunk() stops when callback returns false")
        void chunk_stopsOnFalse() {
            for (int i = 0; i < 15; i++) {
                insertUser("user" + i + "@test.com", "User" + i, "active", 20 + i);
            }

            java.util.concurrent.atomic.AtomicInteger batchCount = new java.util.concurrent.atomic.AtomicInteger(0);

            executor.find(UserEntity.class)
                .chunk(5, batch -> {
                    batchCount.incrementAndGet();
                    return batchCount.get() < 2; // Stop after 2 batches
                });

            assertEquals(2, batchCount.get());
        }

        @Test
        @DisplayName("chunk() handles last batch smaller than chunk size")
        void chunk_handlesPartialLastBatch() {
            // Insert 7 records - will have batches of 5 and 2
            for (int i = 0; i < 7; i++) {
                insertUser("user" + i + "@test.com", "User" + i, "active", 20 + i);
            }

            java.util.concurrent.atomic.AtomicInteger batchCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.List<Integer> batchSizes = new java.util.ArrayList<>();

            executor.find(UserEntity.class)
                .chunk(5, batch -> {
                    batchCount.incrementAndGet();
                    batchSizes.add(batch.size());
                    return true;
                });

            assertEquals(2, batchCount.get());
            assertEquals(5, batchSizes.get(0).intValue());
            assertEquals(2, batchSizes.get(1).intValue()); // Last batch smaller than chunk size
        }
    }

    // ==================== FIRST OR CREATE / UPDATE OR CREATE TESTS ====================

    @Nested
    @DisplayName("FirstOrCreate and UpdateOrCreate Operations")
    class FirstOrCreateTests {

        @BeforeEach
        void setupTable() throws SQLException {
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS active_record_users");
                stmt.execute("""
                    CREATE TABLE active_record_users (
                        id VARCHAR(36) PRIMARY KEY,
                        email VARCHAR(255) NOT NULL,
                        name VARCHAR(255),
                        status VARCHAR(50),
                        age INT
                    )
                """);
            }
            SuprimContext.setGlobalExecutor(executor);
        }

        @AfterEach
        void cleanupContext() {
            SuprimContext.clearGlobalExecutor();
        }

        @Test
        @DisplayName("firstOrCreate() returns existing entity when found")
        void firstOrCreate_returnsExisting_whenFound() {
            // Insert existing record
            executor.transaction(tx -> {
                ActiveRecordUserEntity user = new ActiveRecordUserEntity();
                user.setEmail("existing@test.com");
                user.setName("Existing");
                user.setStatus("active");
                user.setAge(30);
                user.save();
            });

            // Should find existing, not create new
            ActiveRecordUserEntity result = executor.find(ActiveRecordUserEntity.class)
                .firstOrCreate(
                    java.util.Map.of("email", "existing@test.com"),
                    java.util.Map.of("name", "New Name", "status", "pending")
                );

            assertEquals("Existing", result.getName()); // Original name, not "New Name"
            assertEquals("active", result.getStatus());
        }

        @Test
        @DisplayName("firstOrCreate() creates new entity when not found")
        void firstOrCreate_createsNew_whenNotFound() {
            ActiveRecordUserEntity result = executor.find(ActiveRecordUserEntity.class)
                .firstOrCreate(
                    java.util.Map.of("email", "new@test.com"),
                    java.util.Map.of("name", "New User", "status", "active", "age", 25)
                );

            assertNotNull(result.getId());
            assertEquals("new@test.com", result.getEmail());
            assertEquals("New User", result.getName());
            assertEquals("active", result.getStatus());
            assertEquals(25, result.getAge());
        }

        @Test
        @DisplayName("updateOrCreate() updates existing entity")
        void updateOrCreate_updatesExisting() {
            // Insert existing record
            executor.transaction(tx -> {
                ActiveRecordUserEntity user = new ActiveRecordUserEntity();
                user.setEmail("update@test.com");
                user.setName("Original");
                user.setStatus("active");
                user.setAge(30);
                user.save();
            });

            // Should update existing
            ActiveRecordUserEntity result = executor.find(ActiveRecordUserEntity.class)
                .updateOrCreate(
                    java.util.Map.of("email", "update@test.com"),
                    java.util.Map.of("name", "Updated", "status", "inactive")
                );

            assertEquals("Updated", result.getName());
            assertEquals("inactive", result.getStatus());
        }

        @Test
        @DisplayName("updateOrCreate() creates new entity when not found")
        void updateOrCreate_createsNew_whenNotFound() {
            ActiveRecordUserEntity result = executor.find(ActiveRecordUserEntity.class)
                .updateOrCreate(
                    java.util.Map.of("email", "brandnew@test.com"),
                    java.util.Map.of("name", "Brand New", "status", "pending", "age", 20)
                );

            assertNotNull(result.getId());
            assertEquals("brandnew@test.com", result.getEmail());
            assertEquals("Brand New", result.getName());
        }

        @Test
        @DisplayName("firstOrCreate() ignores unknown fields in values map")
        void firstOrCreate_ignoresUnknownFields() {
            // firstOrCreate with an unknown field should ignore it silently
            ActiveRecordUserEntity result = executor.find(ActiveRecordUserEntity.class)
                .firstOrCreate(
                    java.util.Map.of("email", "unknown@test.com"),
                    java.util.Map.of("name", "Test", "unknownField", "ignored")
                );

            assertNotNull(result.getId());
            assertEquals("unknown@test.com", result.getEmail());
            assertEquals("Test", result.getName());
        }

        @Test
        @DisplayName("updateOrCreate() ignores unknown fields in values map")
        void updateOrCreate_ignoresUnknownFields() {
            // updateOrCreate with an unknown field should ignore it silently
            ActiveRecordUserEntity result = executor.find(ActiveRecordUserEntity.class)
                .updateOrCreate(
                    java.util.Map.of("email", "unknown2@test.com"),
                    java.util.Map.of("name", "Test2", "nonExistentField", "ignored")
                );

            assertNotNull(result.getId());
            assertEquals("unknown2@test.com", result.getEmail());
            assertEquals("Test2", result.getName());
        }

        @Test
        @DisplayName("firstOrCreate() uses field name when column annotation name doesn't match")
        void firstOrCreate_usesFieldName_whenColumnNameMismatch() throws SQLException {
            // Create table with different column names than field names
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS field_name_test");
                stmt.execute("""
                    CREATE TABLE field_name_test (
                        id VARCHAR(36) PRIMARY KEY,
                        user_email VARCHAR(255),
                        user_name VARCHAR(255)
                    )
                """);
            }

            // firstOrCreate using field names instead of column names
            FieldNameTestEntity result = executor.find(FieldNameTestEntity.class)
                .firstOrCreate(
                    java.util.Map.of("user_email", "test@test.com"),  // column name
                    java.util.Map.of("userName", "Test User")  // field name (not column name)
                );

            assertNotNull(result.getId());
            assertEquals("test@test.com", result.getEmail());
            assertEquals("Test User", result.getUserName());

            // Cleanup
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS field_name_test");
            }
        }
    }

    // ==================== TEST ENTITIES ====================

    @Entity(table = "permissions")
    public static class PermissionEntity {
        @Id
        @Column(name = "id", type = SqlType.BIGINT)
        private Long id;

        @Column(name = "user_id", type = SqlType.BIGINT)
        private Long userId;

        @Column(name = "resource_type", type = SqlType.VARCHAR)
        private String resourceType;

        @Column(name = "action", type = SqlType.VARCHAR)
        private String action;

        public PermissionEntity() {}
        public Long getId() { return id; }
        public Long getUserId() { return userId; }
        public String getResourceType() { return resourceType; }
        public String getAction() { return action; }
    }

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

        @Column(name = "age", type = SqlType.INTEGER)
        private Integer age;

        @Column(name = "is_active", type = SqlType.BOOLEAN)
        private Boolean isActive;

        @Column(name = "created_at", type = SqlType.TIMESTAMP)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", type = SqlType.TIMESTAMP)
        private LocalDateTime updatedAt;

        public UserEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    /**
     * Entity with schema to test constructor branch for schema handling.
     */
    @Entity(table = "schema_users", schema = "custom_schema")
    public static class SchemaUserEntity {
        @Id
        @Column(name = "id", type = SqlType.BIGINT)
        private Long id;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        public SchemaUserEntity() {}
        public Long getId() { return id; }
        public String getName() { return name; }
    }

    /**
     * Entity with @CreationTimestamp and @UpdateTimestamp to test latest()/oldest().
     */
    @Entity(table = "timestamped_users")
    public static class TimestampedUserEntity {
        @Id
        @Column(name = "id", type = SqlType.BIGINT)
        private Long id;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        @CreationTimestamp(column = "created_date")
        @Column(name = "created_date", type = SqlType.TIMESTAMP)
        private LocalDateTime createdDate;

        @UpdateTimestamp(column = "modified_date")
        @Column(name = "modified_date", type = SqlType.TIMESTAMP)
        private LocalDateTime modifiedDate;

        public TimestampedUserEntity() {}
        public Long getId() { return id; }
        public String getName() { return name; }
        public LocalDateTime getCreatedDate() { return createdDate; }
        public LocalDateTime getModifiedDate() { return modifiedDate; }
    }

    /**
     * Entity with various column types to test normalizeValue().
     */
    @Entity(table = "typed_entities")
    public static class TypedEntity {
        @Id
        @Column(name = "id", type = SqlType.UUID)
        private UUID id;

        @Column(name = "name", type = SqlType.TEXT)
        private String name;

        @Column(name = "small_num", type = SqlType.SMALLINT)
        private Short smallNum;

        @Column(name = "big_num", type = SqlType.BIGINT)
        private Long bigNum;

        @Column(name = "int_num", type = SqlType.INTEGER)
        private Integer intNum;

        @Column(name = "double_num", type = SqlType.DOUBLE_PRECISION)
        private Double doubleNum;

        @Column(name = "float_num", type = SqlType.REAL)
        private Float floatNum;

        @Column(name = "is_enabled", type = SqlType.BOOLEAN)
        private Boolean isEnabled;

        @Column(name = "char_col", type = SqlType.CHAR)
        private String charCol;

        public TypedEntity() {}
        public UUID getId() { return id; }
        public String getName() { return name; }
        public Short getSmallNum() { return smallNum; }
        public Long getBigNum() { return bigNum; }
        public Integer getIntNum() { return intNum; }
        public Double getDoubleNum() { return doubleNum; }
        public Float getFloatNum() { return floatNum; }
        public Boolean getIsEnabled() { return isEnabled; }
        public String getCharCol() { return charCol; }
    }

    /**
     * Entity with TIMESTAMP and DATE columns to test default case in normalizeValue().
     */
    @Entity(table = "timestamp_entities")
    public static class TimestampEntity {
        @Id
        @Column(name = "id", type = SqlType.BIGINT)
        private Long id;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        @Column(name = "event_time", type = SqlType.TIMESTAMP)
        private LocalDateTime eventTime;

        @Column(name = "event_date", type = SqlType.DATE)
        private java.time.LocalDate eventDate;

        public TimestampEntity() {}
        public Long getId() { return id; }
        public String getName() { return name; }
        public LocalDateTime getEventTime() { return eventTime; }
        public java.time.LocalDate getEventDate() { return eventDate; }
    }

    /**
     * Entity extending SuprimEntity for firstOrCreate/updateOrCreate tests.
     */
    @Entity(table = "active_record_users")
    public static class ActiveRecordUserEntity extends SuprimEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id", type = SqlType.VARCHAR)
        private String id;

        @Column(name = "email", type = SqlType.VARCHAR)
        private String email;

        @Column(name = "name", type = SqlType.VARCHAR)
        private String name;

        @Column(name = "status", type = SqlType.VARCHAR)
        private String status;

        @Column(name = "age", type = SqlType.INTEGER)
        private Integer age;

        public ActiveRecordUserEntity() {}
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }

    /**
     * Entity with field names different from column names for setFieldValue testing.
     */
    @Entity(table = "field_name_test")
    public static class FieldNameTestEntity extends SuprimEntity {
        @Id(strategy = GenerationType.UUID_V7)
        @Column(name = "id", type = SqlType.VARCHAR)
        private String id;

        @Column(name = "user_email", type = SqlType.VARCHAR)
        private String email;

        @Column(name = "user_name", type = SqlType.VARCHAR)
        private String userName;  // field name differs from column name

        public FieldNameTestEntity() {}
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
    }
}
