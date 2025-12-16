package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.JsonbColumn;
import sant1ago.dev.suprim.core.type.StringColumn;
import sant1ago.dev.suprim.core.type.Table;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for InsertBuilder.
 */
@DisplayName("InsertBuilder Tests")
class InsertBuilderTest {

    @Test
    @DisplayName("Single column insert")
    void testSingleColumnInsert() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("(\"email\")"));
        assertTrue(sql.contains("VALUES"));
        assertEquals(1, result.parameters().size());
    }

    @Test
    @DisplayName("Multi-column insert")
    void testMultiColumnInsert() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .column(TestUser_.AGE, 25)
            .column(TestUser_.IS_ACTIVE, true)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("(\"email\", \"name\", \"age\", \"is_active\")"));
        assertTrue(sql.contains("VALUES"));
        assertEquals(4, result.parameters().size());
    }

    @Test
    @DisplayName("Insert with RETURNING clause (PostgreSQL)")
    void testInsertWithReturning() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .returning(TestUser_.ID, TestUser_.CREATED_AT)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("RETURNING \"id\", \"created_at\""));
    }

    @Test
    @DisplayName("Insert with all data types")
    void testInsertWithAllTypes() {
        LocalDateTime now = LocalDateTime.now();

        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .column(TestUser_.AGE, 30)
            .column(TestUser_.IS_ACTIVE, true)
            .column(TestUser_.CREATED_AT, now)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("(\"email\", \"name\", \"age\", \"is_active\", \"created_at\")"));
        assertEquals(5, result.parameters().size());
    }

    @Test
    @DisplayName("Build with MySQL dialect")
    void testInsertWithMySqlDialect() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .build(MySqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO `users`"));
        assertTrue(sql.contains("(`email`, `name`)"));
    }

    @Test
    @DisplayName("Build with PostgreSQL dialect")
    void testInsertWithPostgreSqlDialect() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John Doe")
            .build(PostgreSqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertTrue(sql.contains("(\"email\", \"name\")"));
    }

    @Test
    @DisplayName("RETURNING clause with MySQL throws exception")
    void testReturningWithMySqlThrowsException() {
        assertThrows(UnsupportedDialectFeatureException.class, () -> {
            Suprim.insertInto(TestUser_.TABLE)
                .column(TestUser_.EMAIL, "test@example.com")
                .returning(TestUser_.ID)
                .build(MySqlDialect.INSTANCE);
        });
    }

    @Test
    @DisplayName("Insert without columns throws exception")
    void testInsertWithoutColumnsThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            Suprim.insertInto(TestUser_.TABLE).build();
        });
    }

    @Test
    @DisplayName("Insert with null values")
    void testInsertWithNullValues() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, (String) null)
            .column(TestUser_.AGE, (Integer) null)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"users\""));
        assertEquals(3, result.parameters().size());
        assertTrue(result.parameters().values().stream().anyMatch(v -> v == null));
    }

    @Test
    @DisplayName("Parameter names are sequential")
    void testParameterNamesAreSequential() {
        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, "test@example.com")
            .column(TestUser_.NAME, "John")
            .column(TestUser_.AGE, 25)
            .build();

        assertTrue(result.parameters().containsKey("p1"));
        assertTrue(result.parameters().containsKey("p2"));
        assertTrue(result.parameters().containsKey("p3"));
    }

    @Test
    @DisplayName("Insert preserves parameter values")
    void testInsertPreservesParameterValues() {
        String email = "test@example.com";
        String name = "John Doe";
        Integer age = 25;

        QueryResult result = Suprim.insertInto(TestUser_.TABLE)
            .column(TestUser_.EMAIL, email)
            .column(TestUser_.NAME, name)
            .column(TestUser_.AGE, age)
            .build();

        assertTrue(result.parameters().containsValue(email));
        assertTrue(result.parameters().containsValue(name));
        assertTrue(result.parameters().containsValue(age));
    }

    // ==================== AUTO-GENERATED ID TESTS ====================

    // Entity with UUID auto-generation
    static class UuidEntity {
        @Id(strategy = GenerationType.UUID_V7)
        private UUID id;
        private String name;
    }

    private static final Table<UuidEntity> UUID_TABLE = Table.of("uuid_entities", UuidEntity.class);
    private static final Column<UuidEntity, UUID> UUID_ID = new Column<>(UUID_TABLE, "id", UUID.class, "UUID");
    private static final StringColumn<UuidEntity> UUID_NAME = new StringColumn<>(UUID_TABLE, "name", "VARCHAR(255)");

    @Test
    @DisplayName("Insert auto-generates UUID when ID column not specified")
    void testInsertAutoGeneratesUuid() {
        QueryResult result = Suprim.insertInto(UUID_TABLE)
            .column(UUID_NAME, "Test Name")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"uuid_entities\""));
        // Should have 2 parameters: generated UUID + name
        assertEquals(2, result.parameters().size());
        // First parameter should be a UUID
        Object firstParam = result.parameters().values().iterator().next();
        assertTrue(firstParam instanceof UUID, "First parameter should be auto-generated UUID");
    }

    @Test
    @DisplayName("Insert does not auto-generate UUID when ID column is specified")
    void testInsertDoesNotAutoGenerateWhenIdProvided() {
        UUID providedId = UUID.randomUUID();
        QueryResult result = Suprim.insertInto(UUID_TABLE)
            .column(UUID_ID, providedId)
            .column(UUID_NAME, "Test Name")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"uuid_entities\""));
        // Should have 2 parameters: provided UUID + name
        assertEquals(2, result.parameters().size());
        // Should contain the provided UUID
        assertTrue(result.parameters().containsValue(providedId));
    }

    @Test
    @DisplayName("hasIdColumn returns true when ID column exists")
    void testHasIdColumnWhenExists() {
        UUID providedId = UUID.randomUUID();
        QueryResult result = Suprim.insertInto(UUID_TABLE)
            .column(UUID_ID, providedId)
            .build();

        // Only 1 parameter - the provided ID
        assertEquals(1, result.parameters().size());
        assertTrue(result.parameters().containsValue(providedId));
    }

    // Entity with IDENTITY (database-generated) - should NOT auto-generate
    static class IdentityEntity {
        @Id(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;
    }

    private static final Table<IdentityEntity> IDENTITY_TABLE = Table.of("identity_entities", IdentityEntity.class);
    private static final StringColumn<IdentityEntity> IDENTITY_NAME = new StringColumn<>(IDENTITY_TABLE, "name", "VARCHAR(255)");

    @Test
    @DisplayName("Insert with IDENTITY strategy does NOT auto-generate ID")
    void testInsertWithIdentityDoesNotAutoGenerate() {
        QueryResult result = Suprim.insertInto(IDENTITY_TABLE)
            .column(IDENTITY_NAME, "Test Name")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("INSERT INTO \"identity_entities\""));
        // Should have only 1 parameter - the name (no ID auto-generated)
        assertEquals(1, result.parameters().size());
        assertTrue(result.parameters().containsValue("Test Name"));
    }

    // ==================== JSONB CAST Tests ====================

    // Entity for JSONB tests (no ID auto-generation)
    static class JsonEntity {
        private String name;
        private String payload;
    }

    private static final Table<JsonEntity> JSON_TABLE = Table.of("json_entities", JsonEntity.class);
    private static final StringColumn<JsonEntity> JSON_NAME = new StringColumn<>(JSON_TABLE, "name", "VARCHAR(255)");
    private static final JsonbColumn<JsonEntity> JSON_PAYLOAD = new JsonbColumn<>(JSON_TABLE, "payload", "JSONB");

    @Test
    @DisplayName("Insert with JsonbColumn wraps param with CAST for PostgreSQL")
    void testInsertWithJsonbColumnPostgres() {
        QueryResult result = Suprim.insertInto(JSON_TABLE)
            .column(JSON_NAME, "Test")
            .column(JSON_PAYLOAD, "{\"key\": \"value\"}")
            .build(PostgreSqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("CAST(:p2 AS jsonb)"), "PostgreSQL should wrap JSONB param with CAST: " + sql);
    }

    @Test
    @DisplayName("Insert with JsonbColumn no CAST for MySQL")
    void testInsertWithJsonbColumnMySql() {
        QueryResult result = Suprim.insertInto(JSON_TABLE)
            .column(JSON_NAME, "Test")
            .column(JSON_PAYLOAD, "{\"key\": \"value\"}")
            .build(MySqlDialect.INSTANCE);

        String sql = result.sql();
        assertFalse(sql.contains("CAST("), "MySQL should not wrap with CAST: " + sql);
        assertTrue(sql.contains(":p2"), "MySQL should use plain param: " + sql);
    }
}
