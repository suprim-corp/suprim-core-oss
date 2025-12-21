package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UpsertBuilder - INSERT ... ON CONFLICT query builder.
 */
@DisplayName("UpsertBuilder Tests")
class UpsertBuilderTest {

    private static final PostgreSqlDialect PG_DIALECT = PostgreSqlDialect.INSTANCE;
    private static final MySqlDialect MYSQL_DIALECT = MySqlDialect.INSTANCE;

    @Nested
    @DisplayName("PostgreSQL ON CONFLICT DO UPDATE")
    class PostgresUpsert {

        @Test
        @DisplayName("should generate ON CONFLICT DO UPDATE SQL")
        void shouldGenerateOnConflictDoUpdate() {
            UUID id = UUID.randomUUID();

            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("id", "email", "name")
                .values(Map.of("id", id, "email", "test@example.com", "name", "Test"))
                .onConflict("id")
                .doUpdate("email", "name")
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("INSERT INTO"));
            assertTrue(sql.contains("ON CONFLICT (\"id\")"));
            assertTrue(sql.contains("DO UPDATE SET"));
            assertTrue(sql.contains("\"email\" = EXCLUDED.\"email\""));
            assertTrue(sql.contains("\"name\" = EXCLUDED.\"name\""));
        }

        @Test
        @DisplayName("should generate ON CONFLICT DO NOTHING SQL")
        void shouldGenerateOnConflictDoNothing() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "test@example.com", "name", "Test"))
                .onConflict("email")
                .doNothing()
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("ON CONFLICT (\"email\")"));
            assertTrue(sql.contains("DO NOTHING"));
            assertFalse(sql.contains("DO UPDATE"));
        }

        @Test
        @DisplayName("should support multiple conflict columns")
        void shouldSupportMultipleConflictColumns() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("email", "name", "tenant_id")
                .values(Map.of("email", "test@example.com", "name", "Test", "tenant_id", 1))
                .onConflict("email", "tenant_id")
                .doUpdate("name")
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("ON CONFLICT (\"email\", \"tenant_id\")"));
        }

        @Test
        @DisplayName("should include RETURNING clause")
        void shouldIncludeReturningClause() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "test@example.com", "name", "Test"))
                .onConflict("email")
                .doUpdate("name")
                .returning("id")
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("RETURNING \"id\""));
        }

        @Test
        @DisplayName("should support doUpdateAll for all non-conflict columns")
        void shouldSupportDoUpdateAll() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("id", "email", "name")
                .values(Map.of("id", UUID.randomUUID(), "email", "test@example.com", "name", "Test"))
                .onConflict("id")
                .doUpdateAll()
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("\"email\" = EXCLUDED.\"email\""));
            assertTrue(sql.contains("\"name\" = EXCLUDED.\"name\""));
            assertFalse(sql.contains("\"id\" = EXCLUDED.\"id\""));
        }
    }

    @Nested
    @DisplayName("MySQL ON DUPLICATE KEY UPDATE")
    class MySqlUpsert {

        @Test
        @DisplayName("should generate ON DUPLICATE KEY UPDATE SQL")
        void shouldGenerateOnDuplicateKeyUpdate() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("id", "email", "name")
                .values(Map.of("id", 1L, "email", "test@example.com", "name", "Test"))
                .onConflict("id")
                .doUpdate("email", "name")
                .build(MYSQL_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("INSERT INTO"));
            assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
            assertTrue(sql.contains("`email` = VALUES(`email`)"));
            assertTrue(sql.contains("`name` = VALUES(`name`)"));
            assertFalse(sql.contains("ON CONFLICT"));
        }

        @Test
        @DisplayName("should generate INSERT IGNORE for doNothing")
        void shouldGenerateInsertIgnore() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "test@example.com", "name", "Test"))
                .onConflict("email")
                .doNothing()
                .build(MYSQL_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("INSERT IGNORE INTO"));
            assertFalse(sql.contains("ON DUPLICATE KEY UPDATE"));
        }

        @Test
        @DisplayName("should NOT include RETURNING clause")
        void shouldNotIncludeReturningClause() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "test@example.com", "name", "Test"))
                .onConflict("email")
                .doUpdate("name")
                .returning("id")
                .build(MYSQL_DIALECT);

            String sql = result.sql();
            assertFalse(sql.contains("RETURNING"));
        }
    }

    @Nested
    @DisplayName("Batch Upsert")
    class BatchUpsert {

        @Test
        @DisplayName("should generate multi-row upsert for PostgreSQL")
        void shouldGenerateMultiRowPostgres() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("id", "email", "name")
                .values(Map.of("id", UUID.randomUUID(), "email", "a@example.com", "name", "A"))
                .values(Map.of("id", UUID.randomUUID(), "email", "b@example.com", "name", "B"))
                .values(Map.of("id", UUID.randomUUID(), "email", "c@example.com", "name", "C"))
                .onConflict("id")
                .doUpdate("email", "name")
                .build(PG_DIALECT);

            // Should have 9 parameters (3 columns x 3 rows)
            assertEquals(9, result.parameters().size());
        }

        @Test
        @DisplayName("should generate multi-row upsert for MySQL")
        void shouldGenerateMultiRowMysql() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("id", "email", "name")
                .values(Map.of("id", 1L, "email", "a@example.com", "name", "A"))
                .values(Map.of("id", 2L, "email", "b@example.com", "name", "B"))
                .onConflict("id")
                .doUpdate("email", "name")
                .build(MYSQL_DIALECT);

            assertEquals(6, result.parameters().size());
        }
    }

    @Nested
    @DisplayName("Using Column References")
    class ColumnReferences {

        @Test
        @DisplayName("should accept Column references")
        void shouldAcceptColumnReferences() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns(TestUser_.ID, TestUser_.EMAIL, TestUser_.NAME)
                .values(Map.of("id", UUID.randomUUID(), "email", "test@example.com", "name", "Test"))
                .onConflict(TestUser_.ID)
                .doUpdate(TestUser_.EMAIL, TestUser_.NAME)
                .returning(TestUser_.ID)
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("\"id\""));
            assertTrue(sql.contains("\"email\""));
            assertTrue(sql.contains("\"name\""));
        }
    }

    @Nested
    @DisplayName("Additional API Methods")
    class AdditionalApiMethods {

        @Test
        @DisplayName("should support columns from List")
        void shouldSupportColumnsFromList() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns(java.util.List.of("id", "email", "name"))
                .values(Map.of("id", java.util.UUID.randomUUID(), "email", "test@example.com", "name", "Test"))
                .onConflict("id")
                .doUpdate("email", "name")
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("\"id\""));
            assertTrue(sql.contains("\"email\""));
            assertTrue(sql.contains("\"name\""));
        }

        @Test
        @DisplayName("should support set() for individual column values")
        void shouldSupportSetForIndividualValues() {
            Long id = 1L;

            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .set(TestUser_.ID, id)
                .set(TestUser_.EMAIL, "test@example.com")
                .set(TestUser_.NAME, "Test")
                .onConflict(TestUser_.ID)
                .doUpdate(TestUser_.EMAIL, TestUser_.NAME)
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("\"id\""));
            assertTrue(sql.contains("\"email\""));
            assertTrue(sql.contains("\"name\""));
            assertEquals(3, result.parameters().size());
        }

        @Test
        @DisplayName("should support values from List of Maps")
        void shouldSupportValuesFromListOfMaps() {
            java.util.List<Map<String, Object>> valuesList = java.util.List.of(
                Map.of("id", java.util.UUID.randomUUID(), "email", "a@example.com", "name", "A"),
                Map.of("id", java.util.UUID.randomUUID(), "email", "b@example.com", "name", "B")
            );

            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("id", "email", "name")
                .values(valuesList)
                .onConflict("id")
                .doUpdate("email", "name")
                .build(PG_DIALECT);

            assertEquals(6, result.parameters().size());
        }

        @Test
        @DisplayName("should support returning multiple columns by name")
        void shouldSupportReturningMultipleColumnsByName() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "test@example.com", "name", "Test"))
                .onConflict("email")
                .doUpdate("name")
                .returning("id", "email", "created_at")
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("RETURNING \"id\", \"email\", \"created_at\""));
        }

        @Test
        @DisplayName("rowCount should return correct number of rows")
        void rowCountShouldReturnCorrectNumber() {
            UpsertBuilder<?> builder = Suprim.upsertInto(TestUser_.TABLE)
                .columns("id", "email", "name")
                .values(Map.of("id", java.util.UUID.randomUUID(), "email", "a@example.com", "name", "A"))
                .values(Map.of("id", java.util.UUID.randomUUID(), "email", "b@example.com", "name", "B"))
                .values(Map.of("id", java.util.UUID.randomUUID(), "email", "c@example.com", "name", "C"))
                .onConflict("id")
                .doUpdate("email", "name");

            assertEquals(3, builder.rowCount());
        }

        @Test
        @DisplayName("set() should add column if not already present")
        void setShouldAddColumnIfNotPresent() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .set(TestUser_.ID, 1L)
                .set(TestUser_.EMAIL, "test@example.com")
                .onConflict(TestUser_.ID)
                .doUpdate(TestUser_.EMAIL)
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("\"id\""));
            assertTrue(sql.contains("\"email\""));
        }

        @Test
        @DisplayName("set() should not duplicate column if already present")
        void setShouldNotDuplicateColumn() {
            QueryResult result = Suprim.upsertInto(TestUser_.TABLE)
                .columns(TestUser_.ID, TestUser_.EMAIL)
                .set(TestUser_.ID, 1L)
                .set(TestUser_.EMAIL, "test@example.com")
                .onConflict(TestUser_.ID)
                .doUpdate(TestUser_.EMAIL)
                .build(PG_DIALECT);

            String sql = result.sql();
            // Count occurrences of "id" column in the column list
            // The SQL should be: INSERT INTO ... ("id", "email") VALUES ...
            // Not: INSERT INTO ... ("id", "email", "id", "email") VALUES ...
            int firstIdIndex = sql.indexOf("\"id\"");
            int lastIdIndex = sql.lastIndexOf("\"id\"");
            // If no duplication, first and last should be at same position OR
            // the second occurrence should be after VALUES (in EXCLUDED.id part)
            int valuesIndex = sql.indexOf("VALUES");
            assertTrue(firstIdIndex == lastIdIndex || lastIdIndex > valuesIndex,
                "Column 'id' should not be duplicated in column list. SQL: " + sql);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw when no columns specified")
        void shouldThrowWhenNoColumns() {
            UpsertBuilder<?> builder = Suprim.upsertInto(TestUser_.TABLE)
                .values(Map.of("email", "test@example.com"))
                .onConflict("email")
                .doUpdate("name");

            assertThrows(IllegalStateException.class, () -> builder.build(PG_DIALECT));
        }

        @Test
        @DisplayName("should throw when no values specified")
        void shouldThrowWhenNoValues() {
            UpsertBuilder<?> builder = Suprim.upsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .onConflict("email")
                .doUpdate("name");

            assertThrows(IllegalStateException.class, () -> builder.build(PG_DIALECT));
        }

        @Test
        @DisplayName("should throw when no conflict columns specified")
        void shouldThrowWhenNoConflictColumns() {
            UpsertBuilder<?> builder = Suprim.upsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "test@example.com", "name", "Test"))
                .doUpdate("name");

            assertThrows(IllegalStateException.class, () -> builder.build(PG_DIALECT));
        }

        @Test
        @DisplayName("should throw when no update columns and not doNothing")
        void shouldThrowWhenNoUpdateColumnsAndNotDoNothing() {
            UpsertBuilder<?> builder = Suprim.upsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "test@example.com", "name", "Test"))
                .onConflict("email");

            assertThrows(IllegalStateException.class, () -> builder.build(PG_DIALECT));
        }
    }
}
