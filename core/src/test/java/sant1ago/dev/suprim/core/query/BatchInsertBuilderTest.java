package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestUser;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BatchInsertBuilder - multi-row INSERT query builder.
 */
@DisplayName("BatchInsertBuilder Tests")
class BatchInsertBuilderTest {

    private static final PostgreSqlDialect PG_DIALECT = PostgreSqlDialect.INSTANCE;
    private static final MySqlDialect MYSQL_DIALECT = MySqlDialect.INSTANCE;

    @Nested
    @DisplayName("Basic Batch Insert")
    class BasicBatchInsert {

        @Test
        @DisplayName("should generate multi-value INSERT SQL")
        void shouldGenerateMultiValueInsert() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            QueryResult result = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("id", "email", "name")
                .values(Map.of("id", id1, "email", "john@example.com", "name", "John"))
                .values(Map.of("id", id2, "email", "jane@example.com", "name", "Jane"))
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("INSERT INTO"));
            assertTrue(sql.contains("VALUES"));
            // Should have two value groups
            assertEquals(2, countOccurrences(sql, "(:p"));
        }

        @Test
        @DisplayName("should generate INSERT with RETURNING for PostgreSQL")
        void shouldGenerateInsertWithReturning() {
            QueryResult result = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "john@example.com", "name", "John"))
                .values(Map.of("email", "jane@example.com", "name", "Jane"))
                .returning("id")
                .build(PG_DIALECT);

            String sql = result.sql();
            assertTrue(sql.contains("RETURNING \"id\""));
        }

        @Test
        @DisplayName("should NOT include RETURNING for MySQL")
        void shouldNotIncludeReturningForMysql() {
            QueryResult result = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "john@example.com", "name", "John"))
                .returning("id")
                .build(MYSQL_DIALECT);

            String sql = result.sql();
            assertFalse(sql.contains("RETURNING"));
        }

        @Test
        @DisplayName("should use proper identifier quoting per dialect")
        void shouldQuoteIdentifiersByDialect() {
            QueryResult pgResult = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email")
                .values(Map.of("email", "test@example.com"))
                .build(PG_DIALECT);

            QueryResult mysqlResult = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email")
                .values(Map.of("email", "test@example.com"))
                .build(MYSQL_DIALECT);

            assertTrue(pgResult.sql().contains("\"email\""));
            assertTrue(mysqlResult.sql().contains("`email`"));
        }
    }

    @Nested
    @DisplayName("Parameter Handling")
    class ParameterHandling {

        @Test
        @DisplayName("should collect all parameters in correct order")
        void shouldCollectParametersInOrder() {
            QueryResult result = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "john@example.com", "name", "John"))
                .values(Map.of("email", "jane@example.com", "name", "Jane"))
                .build(PG_DIALECT);

            Map<String, Object> params = result.parameters();
            assertEquals(4, params.size());
        }

        @Test
        @DisplayName("should handle null values")
        void shouldHandleNullValues() {
            QueryResult result = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(Map.of("email", "john@example.com", "name", "John"))
                .build(PG_DIALECT);

            assertNotNull(result.sql());
            assertFalse(result.parameters().isEmpty());
        }
    }

    @Nested
    @DisplayName("Using Column References")
    class ColumnReferences {

        @Test
        @DisplayName("should accept Column references for columns")
        void shouldAcceptColumnReferences() {
            QueryResult result = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns(TestUser_.EMAIL, TestUser_.NAME)
                .values(Map.of("email", "test@example.com", "name", "Test"))
                .build(PG_DIALECT);

            assertTrue(result.sql().contains("\"email\""));
            assertTrue(result.sql().contains("\"name\""));
        }

        @Test
        @DisplayName("should accept Column references for returning")
        void shouldAcceptColumnReferencesForReturning() {
            QueryResult result = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns(TestUser_.EMAIL)
                .values(Map.of("email", "test@example.com"))
                .returning(TestUser_.ID)
                .build(PG_DIALECT);

            assertTrue(result.sql().contains("RETURNING \"id\""));
        }
    }

    @Nested
    @DisplayName("Batch Values API")
    class BatchValuesApi {

        @Test
        @DisplayName("should accept list of value maps")
        void shouldAcceptListOfValueMaps() {
            List<Map<String, Object>> rows = List.of(
                Map.of("email", "a@example.com", "name", "A"),
                Map.of("email", "b@example.com", "name", "B"),
                Map.of("email", "c@example.com", "name", "C")
            );

            QueryResult result = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email", "name")
                .values(rows)
                .build(PG_DIALECT);

            // Should have 6 parameters (2 per row x 3 rows)
            assertEquals(6, result.parameters().size());
        }

        @Test
        @DisplayName("should return correct row count")
        void shouldReturnCorrectRowCount() {
            BatchInsertBuilder<TestUser> builder = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email")
                .values(Map.of("email", "a@example.com"))
                .values(Map.of("email", "b@example.com"))
                .values(Map.of("email", "c@example.com"));

            assertEquals(3, builder.rowCount());
        }

        @Test
        @DisplayName("should return column names")
        void shouldReturnColumnNames() {
            BatchInsertBuilder<TestUser> builder = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email", "name", "age");

            List<String> columns = builder.getColumnNames();
            assertEquals(3, columns.size());
            assertTrue(columns.contains("email"));
            assertTrue(columns.contains("name"));
            assertTrue(columns.contains("age"));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw when no columns specified")
        void shouldThrowWhenNoColumns() {
            BatchInsertBuilder<TestUser> builder = Suprim.batchInsertInto(TestUser_.TABLE)
                .values(Map.of("email", "test@example.com"));

            assertThrows(IllegalStateException.class, () -> builder.build(PG_DIALECT));
        }

        @Test
        @DisplayName("should throw when no values specified")
        void shouldThrowWhenNoValues() {
            BatchInsertBuilder<TestUser> builder = Suprim.batchInsertInto(TestUser_.TABLE)
                .columns("email");

            assertThrows(IllegalStateException.class, () -> builder.build(PG_DIALECT));
        }
    }

    private static int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
