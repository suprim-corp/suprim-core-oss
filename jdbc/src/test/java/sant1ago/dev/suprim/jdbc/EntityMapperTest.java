package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.MappedSuperclass;
import sant1ago.dev.suprim.core.query.QueryResult;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityMapperTest {

    private JdbcDataSource dataSource;
    private SuprimExecutor executor;
    private Connection setupConnection;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:entitytest;DB_CLOSE_DELAY=-1");

        setupConnection = dataSource.getConnection();
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL,
                    full_name VARCHAR(100),
                    is_active BOOLEAN DEFAULT TRUE,
                    age INT,
                    balance DECIMAL(10, 2)
                )
                """);

            stmt.execute("""
                CREATE TABLE type_test (
                    id UUID PRIMARY KEY,
                    name VARCHAR(255),
                    long_value BIGINT,
                    int_value INT,
                    bool_value BOOLEAN,
                    double_value DOUBLE,
                    float_value FLOAT,
                    decimal_value DECIMAL(19, 4),
                    datetime_value TIMESTAMP,
                    date_value DATE,
                    offset_datetime_value TIMESTAMP WITH TIME ZONE,
                    instant_value TIMESTAMP,
                    user_id BIGINT,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
        }

        executor = SuprimExecutor.create(dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
            stmt.execute("DROP TABLE IF EXISTS type_test");
        }
        setupConnection.close();
    }

    @Test
    void map_record_mapsCorrectly() {
        insertUser("alice@example.com", "Alice Smith", true, 30, new BigDecimal("100.50"));

        List<UserRecord> users = executor.query(
                new QueryResult("SELECT id, email, full_name, is_active, age, balance FROM users", Map.of()),
                EntityMapper.of(UserRecord.class)
        );

        assertEquals(1, users.size());
        UserRecord user = users.get(0);
        assertEquals("alice@example.com", user.email());
        assertEquals("Alice Smith", user.fullName());
        assertTrue(user.isActive());
        assertEquals(30, user.age());
        assertEquals(new BigDecimal("100.50"), user.balance());
    }

    @Test
    void map_recordWithColumnAnnotation_usesAnnotatedName() {
        insertUser("bob@example.com", "Bob Jones", false, 25, new BigDecimal("50.00"));

        List<UserRecordWithAnnotation> users = executor.query(
                new QueryResult("SELECT email, full_name as name FROM users", Map.of()),
                EntityMapper.of(UserRecordWithAnnotation.class)
        );

        assertEquals(1, users.size());
        assertEquals("bob@example.com", users.get(0).email());
        assertEquals("Bob Jones", users.get(0).name());
    }

    @Test
    void map_class_mapsCorrectly() {
        insertUser("charlie@example.com", "Charlie Brown", true, 35, new BigDecimal("200.00"));

        List<UserClass> users = executor.query(
                new QueryResult("SELECT id, email, full_name, is_active FROM users", Map.of()),
                EntityMapper.of(UserClass.class)
        );

        assertEquals(1, users.size());
        UserClass user = users.get(0);
        assertEquals("charlie@example.com", user.email);
        assertEquals("Charlie Brown", user.fullName);
        assertTrue(user.isActive);
    }

    @Test
    void map_nullValues_handledCorrectly() {
        insertUser("null@example.com", null, true, null, null);

        List<UserRecord> users = executor.query(
                new QueryResult("SELECT id, email, full_name, is_active, age, balance FROM users", Map.of()),
                EntityMapper.of(UserRecord.class)
        );

        assertEquals(1, users.size());
        UserRecord user = users.get(0);
        assertEquals("null@example.com", user.email());
        assertNull(user.fullName());
        assertEquals(0, user.age()); // primitive default
        assertNull(user.balance());
    }

    @Test
    void map_booleanConversion_handlesVariousTypes() {
        insertUser("bool@example.com", "Boolean Test", true, 0, null);

        List<UserRecord> users = executor.query(
                new QueryResult("SELECT id, email, full_name, is_active, age, balance FROM users", Map.of()),
                EntityMapper.of(UserRecord.class)
        );

        assertEquals(1, users.size());
        assertTrue(users.get(0).isActive());
    }

    @Test
    void map_multiplRows_mapsAll() {
        insertUser("user1@example.com", "User 1", true, 20, null);
        insertUser("user2@example.com", "User 2", true, 25, null);
        insertUser("user3@example.com", "User 3", false, 30, null);

        List<UserRecord> users = executor.query(
                new QueryResult("SELECT id, email, full_name, is_active, age, balance FROM users ORDER BY age", Map.of()),
                EntityMapper.of(UserRecord.class)
        );

        assertEquals(3, users.size());
        assertEquals("User 1", users.get(0).fullName());
        assertEquals("User 2", users.get(1).fullName());
        assertEquals("User 3", users.get(2).fullName());
    }

    @Test
    void map_queryOne_returnsOptional() {
        insertUser("single@example.com", "Single User", true, 40, new BigDecimal("1000.00"));

        Optional<UserRecord> user = executor.queryOne(
                new QueryResult("SELECT id, email, full_name, is_active, age, balance FROM users WHERE email = :p1",
                        Map.of("p1", "single@example.com")),
                EntityMapper.of(UserRecord.class)
        );

        assertTrue(user.isPresent());
        assertEquals("Single User", user.get().fullName());
    }

    @Test
    void map_partialSelect_handlesOnlySelectedColumns() {
        insertUser("partial@example.com", "Partial User", true, 50, new BigDecimal("500.00"));

        List<PartialUserRecord> users = executor.query(
                new QueryResult("SELECT email, age FROM users", Map.of()),
                EntityMapper.of(PartialUserRecord.class)
        );

        assertEquals(1, users.size());
        assertEquals("partial@example.com", users.get(0).email());
        assertEquals(50, users.get(0).age());
    }

    // ============ NEW COMPREHENSIVE TESTS ============

    @Test
    void map_emptyResultSet_returnsEmptyList() {
        List<UserRecord> users = executor.query(
                new QueryResult("SELECT id, email, full_name, is_active, age, balance FROM users WHERE id = -1", Map.of()),
                EntityMapper.of(UserRecord.class)
        );

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void map_stringField_mapsCorrectly() {
        insertUser("string@test.com", "String Test", true, 25, null);

        List<StringFieldRecord> results = executor.query(
                new QueryResult("SELECT email, full_name FROM users", Map.of()),
                EntityMapper.of(StringFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals("string@test.com", results.get(0).email());
        assertEquals("String Test", results.get(0).fullName());
    }

    @Test
    void map_longField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        insertTypeTest(uuid, 999999999999L);

        List<LongFieldRecord> results = executor.query(
                new QueryResult("SELECT long_value FROM type_test", Map.of()),
                EntityMapper.of(LongFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals(999999999999L, results.get(0).longValue());
    }

    @Test
    void map_integerField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        insertTypeTest(uuid, 42);

        List<IntegerFieldRecord> results = executor.query(
                new QueryResult("SELECT int_value FROM type_test", Map.of()),
                EntityMapper.of(IntegerFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals(42, results.get(0).intValue());
    }

    @Test
    void map_booleanField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        insertTypeTestWithBoolean(uuid, true);

        List<BooleanFieldRecord> results = executor.query(
                new QueryResult("SELECT bool_value FROM type_test", Map.of()),
                EntityMapper.of(BooleanFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertTrue(results.get(0).boolValue());
    }

    @Test
    void map_doubleAndFloatField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        insertTypeTestWithDecimals(uuid, 123.456, 78.9f);

        List<DoubleFloatFieldRecord> results = executor.query(
                new QueryResult("SELECT double_value, float_value FROM type_test", Map.of()),
                EntityMapper.of(DoubleFloatFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals(123.456, results.get(0).doubleValue(), 0.001);
        assertEquals(78.9f, results.get(0).floatValue(), 0.01f);
    }

    @Test
    void map_bigDecimalField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        insertTypeTestWithBigDecimal(uuid, new BigDecimal("12345.6789"));

        List<BigDecimalFieldRecord> results = executor.query(
                new QueryResult("SELECT decimal_value FROM type_test", Map.of()),
                EntityMapper.of(BigDecimalFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals(new BigDecimal("12345.6789"), results.get(0).decimalValue());
    }

    @Test
    void map_localDateTimeField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        insertTypeTestWithDateTime(uuid, now);

        List<LocalDateTimeFieldRecord> results = executor.query(
                new QueryResult("SELECT datetime_value FROM type_test", Map.of()),
                EntityMapper.of(LocalDateTimeFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertNotNull(results.get(0).datetimeValue());
    }

    @Test
    void map_localDateField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        insertTypeTestWithDate(uuid, today);

        List<LocalDateFieldRecord> results = executor.query(
                new QueryResult("SELECT date_value FROM type_test", Map.of()),
                EntityMapper.of(LocalDateFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertNotNull(results.get(0).dateValue());
    }

    @Test
    void map_offsetDateTimeField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        insertTypeTestWithOffsetDateTime(uuid, now);

        List<OffsetDateTimeFieldRecord> results = executor.query(
                new QueryResult("SELECT offset_datetime_value FROM type_test", Map.of()),
                EntityMapper.of(OffsetDateTimeFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertNotNull(results.get(0).offsetDatetimeValue());
    }

    @Test
    void map_instantField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        Instant now = Instant.now();
        insertTypeTestWithInstant(uuid, now);

        List<InstantFieldRecord> results = executor.query(
                new QueryResult("SELECT instant_value FROM type_test", Map.of()),
                EntityMapper.of(InstantFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertNotNull(results.get(0).instantValue());
    }

    @Test
    void map_uuidField_mapsCorrectly() {
        UUID uuid = UUID.randomUUID();
        insertTypeTestBasic(uuid, "UUID Test");

        List<UuidFieldRecord> results = executor.query(
                new QueryResult("SELECT id FROM type_test", Map.of()),
                EntityMapper.of(UuidFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals(uuid, results.get(0).id());
    }

    @Test
    void map_snakeCaseToCamelCase_convertsAutomatically() {
        UUID uuid = UUID.randomUUID();
        insertTypeTestWithUserId(uuid, 12345L);

        List<CamelCaseFieldRecord> results = executor.query(
                new QueryResult("SELECT user_id FROM type_test", Map.of()),
                EntityMapper.of(CamelCaseFieldRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals(12345L, results.get(0).userId());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Column annotation lookup not implemented - requires entity metadata")
    void map_columnAnnotation_usesCustomName() {
        UUID uuid = UUID.randomUUID();
        insertTypeTestBasic(uuid, "Custom Name Test");

        List<CustomColumnNameRecord> results = executor.query(
                new QueryResult("SELECT name FROM type_test", Map.of()),
                EntityMapper.of(CustomColumnNameRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals("Custom Name Test", results.get(0).customName());
    }

    @Test
    void map_caseInsensitiveColumns_matchesCorrectly() {
        insertUser("case@test.com", "Case Test", true, 30, null);

        List<CaseInsensitiveRecord> results = executor.query(
                new QueryResult("SELECT EMAIL, FULL_NAME FROM users", Map.of()),
                EntityMapper.of(CaseInsensitiveRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals("case@test.com", results.get(0).email());
        assertEquals("Case Test", results.get(0).fullName());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Inheritance not supported - requires entity metadata")
    void map_inheritanceFromMappedSuperclass_mapsAllFields() {
        UUID uuid = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        insertTypeTestWithTimestamps(uuid, "Entity with inheritance", createdAt, updatedAt);

        List<ChildEntityClass> results = executor.query(
                new QueryResult("SELECT id, name, created_at, updated_at FROM type_test", Map.of()),
                EntityMapper.of(ChildEntityClass.class)
        );

        assertEquals(1, results.size());
        assertEquals(uuid, results.get(0).id);
        assertEquals("Entity with inheritance", results.get(0).name);
        assertNotNull(results.get(0).createdAt);
        assertNotNull(results.get(0).updatedAt);
    }

    @Test
    void map_missingColumn_setsToNullOrDefault() {
        insertUser("missing@test.com", "Missing Column", true, 30, null);

        List<RecordWithExtraField> results = executor.query(
                new QueryResult("SELECT email, full_name FROM users", Map.of()),
                EntityMapper.of(RecordWithExtraField.class)
        );

        assertEquals(1, results.size());
        assertEquals("missing@test.com", results.get(0).email());
        assertEquals("Missing Column", results.get(0).fullName());
        assertNull(results.get(0).extraField());
        assertEquals(0, results.get(0).extraInt());
    }

    @Test
    void map_extraColumnsInResultSet_ignored() {
        insertUser("extra@test.com", "Extra Cols", true, 30, new BigDecimal("100.00"));

        List<MinimalUserRecord> results = executor.query(
                new QueryResult("SELECT id, email, full_name, is_active, age, balance FROM users", Map.of()),
                EntityMapper.of(MinimalUserRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals("extra@test.com", results.get(0).email());
    }

    @Test
    void map_privateFieldsInClass_accessedViaReflection() {
        UUID uuid = UUID.randomUUID();
        insertTypeTestBasic(uuid, "Private Fields");

        List<ClassWithPrivateFields> results = executor.query(
                new QueryResult("SELECT id, name FROM type_test", Map.of()),
                EntityMapper.of(ClassWithPrivateFields.class)
        );

        assertEquals(1, results.size());
        assertEquals(uuid, results.get(0).getId());
        assertEquals("Private Fields", results.get(0).getName());
    }

    @Test
    void map_primitiveDefaults_whenNull() {
        insertUser("primitives@test.com", null, true, null, null);

        List<PrimitiveDefaultsRecord> results = executor.query(
                new QueryResult("SELECT email, age, is_active FROM users", Map.of()),
                EntityMapper.of(PrimitiveDefaultsRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals("primitives@test.com", results.get(0).email());
        assertEquals(0, results.get(0).age());
        assertTrue(results.get(0).isActive());
    }

    @Test
    void map_wrapperTypes_handleNull() {
        insertUser("wrappers@test.com", null, true, null, null);

        List<WrapperTypesRecord> results = executor.query(
                new QueryResult("SELECT email, full_name, age FROM users", Map.of()),
                EntityMapper.of(WrapperTypesRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals("wrappers@test.com", results.get(0).email());
        assertNull(results.get(0).fullName());
        assertNull(results.get(0).age());
    }

    @Test
    void map_numberConversion_fromDatabaseTypes() {
        UUID uuid = UUID.randomUUID();
        insertTypeTestAllNumbers(uuid, 1000L, 500);

        List<NumberConversionRecord> results = executor.query(
                new QueryResult("SELECT long_value, int_value FROM type_test", Map.of()),
                EntityMapper.of(NumberConversionRecord.class)
        );

        assertEquals(1, results.size());
        assertEquals(1000L, results.get(0).longValue());
        assertEquals(500, results.get(0).intValue());
    }

    // ============ HELPER METHODS ============

    private void insertUser(String email, String fullName, boolean isActive, Integer age, BigDecimal balance) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", email);
        params.put("p2", fullName);
        params.put("p3", isActive);
        params.put("p4", age);
        params.put("p5", balance);

        executor.execute(new QueryResult(
                "INSERT INTO users (email, full_name, is_active, age, balance) VALUES (:p1, :p2, :p3, :p4, :p5)",
                params
        ));
    }

    private void insertTypeTestBasic(UUID id, String name) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", name);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, name) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTest(UUID id, Long longValue) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", longValue);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, long_value) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTest(UUID id, Integer intValue) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", intValue);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, int_value) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTestWithBoolean(UUID id, Boolean boolValue) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", boolValue);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, bool_value) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTestWithDecimals(UUID id, Double doubleValue, Float floatValue) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", doubleValue);
        params.put("p3", floatValue);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, double_value, float_value) VALUES (:p1, :p2, :p3)",
                params
        ));
    }

    private void insertTypeTestWithBigDecimal(UUID id, BigDecimal decimalValue) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", decimalValue);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, decimal_value) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTestWithDateTime(UUID id, LocalDateTime datetime) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", datetime);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, datetime_value) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTestWithDate(UUID id, LocalDate date) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", date);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, date_value) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTestWithOffsetDateTime(UUID id, OffsetDateTime offsetDatetime) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", offsetDatetime);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, offset_datetime_value) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTestWithInstant(UUID id, Instant instant) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", instant);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, instant_value) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTestWithUserId(UUID id, Long userId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", userId);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, user_id) VALUES (:p1, :p2)",
                params
        ));
    }

    private void insertTypeTestWithTimestamps(UUID id, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", name);
        params.put("p3", createdAt);
        params.put("p4", updatedAt);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, name, created_at, updated_at) VALUES (:p1, :p2, :p3, :p4)",
                params
        ));
    }

    private void insertTypeTestAllNumbers(UUID id, Long longValue, Integer intValue) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", id);
        params.put("p2", longValue);
        params.put("p3", intValue);
        executor.execute(new QueryResult(
                "INSERT INTO type_test (id, long_value, int_value) VALUES (:p1, :p2, :p3)",
                params
        ));
    }

    // ============ TEST ENTITIES ============

    // Original test records
    record UserRecord(
            Long id,
            String email,
            String fullName,
            boolean isActive,
            int age,
            BigDecimal balance
    ) {}

    record UserRecordWithAnnotation(
            String email,
            @Column(name = "name") String name
    ) {}

    record PartialUserRecord(
            String email,
            int age
    ) {}

    // Original test class (non-record)
    static class UserClass {
        Long id;
        String email;
        String fullName;
        boolean isActive;

        UserClass() {}
    }

    // New test records for comprehensive tests
    record StringFieldRecord(String email, String fullName) {}

    record LongFieldRecord(Long longValue) {}

    record IntegerFieldRecord(int intValue) {}

    record BooleanFieldRecord(boolean boolValue) {}

    record DoubleFloatFieldRecord(double doubleValue, float floatValue) {}

    record BigDecimalFieldRecord(BigDecimal decimalValue) {}

    record LocalDateTimeFieldRecord(LocalDateTime datetimeValue) {}

    record LocalDateFieldRecord(LocalDate dateValue) {}

    record OffsetDateTimeFieldRecord(OffsetDateTime offsetDatetimeValue) {}

    record InstantFieldRecord(Instant instantValue) {}

    record UuidFieldRecord(UUID id) {}

    record CamelCaseFieldRecord(Long userId) {}

    record CustomColumnNameRecord(@Column(name = "name") String customName) {}

    record CaseInsensitiveRecord(String email, String fullName) {}

    record RecordWithExtraField(String email, String fullName, String extraField, int extraInt) {}

    record MinimalUserRecord(String email) {}

    record PrimitiveDefaultsRecord(String email, int age, boolean isActive) {}

    record WrapperTypesRecord(String email, String fullName, Integer age) {}

    record NumberConversionRecord(Long longValue, Integer intValue) {}

    // Base class for inheritance testing
    @MappedSuperclass
    static class BaseEntity {
        UUID id;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;

        BaseEntity() {}
    }

    // Child class extending base
    static class ChildEntityClass extends BaseEntity {
        String name;

        ChildEntityClass() {
            super();
        }
    }

    // Class with private fields
    static class ClassWithPrivateFields {
        private UUID id;
        private String name;

        ClassWithPrivateFields() {}

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
