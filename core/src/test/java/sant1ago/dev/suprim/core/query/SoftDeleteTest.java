package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestSoftDeleteUser_;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for soft delete functionality.
 */
@DisplayName("Soft Delete Tests")
class SoftDeleteTest {

    // ==================== SELECT BUILDER SOFT DELETE SCOPE ====================

    @Nested
    @DisplayName("SelectBuilder Soft Delete Scope")
    class SelectBuilderSoftDeleteScopeTest {

        @Test
        @DisplayName("Default scope adds WHERE deleted_at IS NULL for @SoftDeletes entity")
        void testDefaultScopeExcludesSoftDeleted() {
            QueryResult result = Suprim.select(TestSoftDeleteUser_.ID, TestSoftDeleteUser_.EMAIL)
                .from(TestSoftDeleteUser_.TABLE)
                .build();

            assertTrue(result.sql().contains("WHERE"));
            assertTrue(result.sql().contains("\"deleted_at\" IS NULL"));
        }

        @Test
        @DisplayName("Default scope with existing WHERE adds AND deleted_at IS NULL")
        void testDefaultScopeWithExistingWhere() {
            QueryResult result = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE)
                .where(TestSoftDeleteUser_.IS_ACTIVE.eq(true))
                .build();

            String sql = result.sql();
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("\"is_active\" = :p1"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("\"deleted_at\" IS NULL"));
        }

        @Test
        @DisplayName("withTrashed() includes soft-deleted records")
        void testWithTrashedIncludesSoftDeleted() {
            QueryResult result = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE)
                .withTrashed()
                .build();

            // Should NOT have deleted_at filter
            assertFalse(result.sql().contains("deleted_at"));
            assertTrue(result.isWithTrashed());
        }

        @Test
        @DisplayName("onlyTrashed() returns only soft-deleted records")
        void testOnlyTrashedReturnsOnlyDeleted() {
            QueryResult result = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE)
                .onlyTrashed()
                .build();

            assertTrue(result.sql().contains("WHERE"));
            assertTrue(result.sql().contains("\"deleted_at\" IS NOT NULL"));
            assertTrue(result.isOnlyTrashed());
        }

        @Test
        @DisplayName("onlyTrashed() with existing WHERE adds AND deleted_at IS NOT NULL")
        void testOnlyTrashedWithExistingWhere() {
            QueryResult result = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE)
                .where(TestSoftDeleteUser_.IS_ACTIVE.eq(false))
                .onlyTrashed()
                .build();

            String sql = result.sql();
            assertTrue(sql.contains("\"is_active\" = :p1"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("\"deleted_at\" IS NOT NULL"));
        }

        @Test
        @DisplayName("Entity without @SoftDeletes has no automatic filter")
        void testEntityWithoutSoftDeletesNoFilter() {
            QueryResult result = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
                .from(TestUser_.TABLE)
                .build();

            // Should NOT have deleted_at filter
            assertFalse(result.sql().contains("deleted_at"));
        }

        @Test
        @DisplayName("getSoftDeleteScope returns correct scope")
        void testGetSoftDeleteScope() {
            SelectBuilder builder = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE);

            assertEquals(SelectBuilder.SoftDeleteScope.DEFAULT, builder.getSoftDeleteScope());

            builder.withTrashed();
            assertEquals(SelectBuilder.SoftDeleteScope.WITH_TRASHED, builder.getSoftDeleteScope());

            builder.onlyTrashed();
            assertEquals(SelectBuilder.SoftDeleteScope.ONLY_TRASHED, builder.getSoftDeleteScope());
        }

        @Test
        @DisplayName("getFromTable returns correct table")
        void testGetFromTable() {
            SelectBuilder builder = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE);

            assertEquals(TestSoftDeleteUser_.TABLE, builder.getFromTable());
        }

        @Test
        @DisplayName("getEntityType returns correct entity class")
        void testGetEntityType() {
            SelectBuilder builder = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE);

            assertEquals(sant1ago.dev.suprim.core.TestSoftDeleteUser.class, builder.getEntityType());
        }
    }

    // ==================== UPDATE BUILDER RAW METHODS ====================

    @Nested
    @DisplayName("UpdateBuilder Raw Methods")
    class UpdateBuilderRawMethodsTest {

        @Test
        @DisplayName("setRaw sets column to expression")
        void testSetRawWithExpression() {
            QueryResult result = Suprim.update(TestSoftDeleteUser_.TABLE)
                .setRaw("deleted_at", sant1ago.dev.suprim.core.type.Fn.now())
                .where(TestSoftDeleteUser_.ID.eq(java.util.UUID.randomUUID()))
                .build();

            assertTrue(result.sql().contains("SET \"deleted_at\" = NOW()"));
        }

        @Test
        @DisplayName("setNull sets column to NULL")
        void testSetNullSetsColumnToNull() {
            QueryResult result = Suprim.update(TestSoftDeleteUser_.TABLE)
                .setNull("deleted_at")
                .where(TestSoftDeleteUser_.ID.eq(java.util.UUID.randomUUID()))
                .build();

            assertTrue(result.sql().contains("SET \"deleted_at\" = NULL"));
        }

        @Test
        @DisplayName("whereRaw adds raw SQL condition")
        void testWhereRawAddsCondition() {
            QueryResult result = Suprim.update(TestSoftDeleteUser_.TABLE)
                .setNull("deleted_at")
                .whereRaw("deleted_at IS NOT NULL")
                .build();

            assertTrue(result.sql().contains("WHERE deleted_at IS NOT NULL"));
        }

        @Test
        @DisplayName("Multiple setRaw and setNull together")
        void testMultipleSetRawAndSetNull() {
            QueryResult result = Suprim.update(TestSoftDeleteUser_.TABLE)
                .setRaw("updated_at", sant1ago.dev.suprim.core.type.Fn.now())
                .setNull("deleted_at")
                .where(TestSoftDeleteUser_.ID.eq(java.util.UUID.randomUUID()))
                .build();

            String sql = result.sql();
            assertTrue(sql.contains("\"updated_at\" = NOW()"));
            assertTrue(sql.contains("\"deleted_at\" = NULL"));
        }
    }

    // ==================== SUPRIM BULK OPERATIONS ====================

    @Nested
    @DisplayName("Suprim Bulk Soft Delete/Restore")
    class SuprimBulkOperationsTest {

        @Test
        @DisplayName("softDelete creates UPDATE with NOW()")
        void testSoftDeleteCreatesUpdateWithNow() {
            QueryResult result = Suprim.softDelete(TestSoftDeleteUser_.TABLE, "deleted_at")
                .where(TestSoftDeleteUser_.IS_ACTIVE.eq(false))
                .build();

            String sql = result.sql();
            assertTrue(sql.startsWith("UPDATE"));
            assertTrue(sql.contains("SET \"deleted_at\" = NOW()"));
            assertTrue(sql.contains("WHERE"));
        }

        @Test
        @DisplayName("restore creates UPDATE with NULL")
        void testRestoreCreatesUpdateWithNull() {
            QueryResult result = Suprim.restore(TestSoftDeleteUser_.TABLE, "deleted_at")
                .whereRaw("deleted_at IS NOT NULL")
                .build();

            String sql = result.sql();
            assertTrue(sql.startsWith("UPDATE"));
            assertTrue(sql.contains("SET \"deleted_at\" = NULL"));
            assertTrue(sql.contains("WHERE deleted_at IS NOT NULL"));
        }

        @Test
        @DisplayName("softDelete with custom column name")
        void testSoftDeleteWithCustomColumn() {
            QueryResult result = Suprim.softDelete(TestSoftDeleteUser_.TABLE, "removed_at")
                .where(TestSoftDeleteUser_.IS_ACTIVE.eq(false))
                .build();

            assertTrue(result.sql().contains("SET \"removed_at\" = NOW()"));
        }

        @Test
        @DisplayName("restore with complex WHERE condition")
        void testRestoreWithComplexWhere() {
            QueryResult result = Suprim.restore(TestSoftDeleteUser_.TABLE, "deleted_at")
                .whereRaw("deleted_at > NOW() - INTERVAL '30 days'")
                .build();

            assertTrue(result.sql().contains("WHERE deleted_at > NOW() - INTERVAL '30 days'"));
        }
    }

    // ==================== QUERY RESULT SOFT DELETE METADATA ====================

    @Nested
    @DisplayName("QueryResult Soft Delete Metadata")
    class QueryResultSoftDeleteMetadataTest {

        @Test
        @DisplayName("Default QueryResult has DEFAULT scope")
        void testDefaultQueryResultScope() {
            QueryResult result = new QueryResult("SELECT 1", java.util.Collections.emptyMap());
            assertEquals(SelectBuilder.SoftDeleteScope.DEFAULT, result.softDeleteScope());
            assertFalse(result.isWithTrashed());
            assertFalse(result.isOnlyTrashed());
        }

        @Test
        @DisplayName("QueryResult from withTrashed has WITH_TRASHED scope")
        void testWithTrashedQueryResultScope() {
            QueryResult result = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE)
                .withTrashed()
                .build();

            assertEquals(SelectBuilder.SoftDeleteScope.WITH_TRASHED, result.softDeleteScope());
            assertTrue(result.isWithTrashed());
            assertFalse(result.isOnlyTrashed());
        }

        @Test
        @DisplayName("QueryResult from onlyTrashed has ONLY_TRASHED scope")
        void testOnlyTrashedQueryResultScope() {
            QueryResult result = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE)
                .onlyTrashed()
                .build();

            assertEquals(SelectBuilder.SoftDeleteScope.ONLY_TRASHED, result.softDeleteScope());
            assertFalse(result.isWithTrashed());
            assertTrue(result.isOnlyTrashed());
        }

        @Test
        @DisplayName("toString includes soft delete scope when not DEFAULT")
        void testToStringIncludesSoftDeleteScope() {
            QueryResult withTrashed = Suprim.select(TestSoftDeleteUser_.ID)
                .from(TestSoftDeleteUser_.TABLE)
                .withTrashed()
                .build();

            assertTrue(withTrashed.toString().contains("softDeleteScope=WITH_TRASHED"));

            QueryResult defaultScope = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .build();

            assertFalse(defaultScope.toString().contains("softDeleteScope"));
        }
    }

    // ==================== @SOFTDELETES ANNOTATION ====================

    @Nested
    @DisplayName("@SoftDeletes Annotation")
    class SoftDeletesAnnotationTest {

        @Test
        @DisplayName("Entity with @SoftDeletes has annotation")
        void testEntityHasSoftDeletesAnnotation() {
            assertTrue(sant1ago.dev.suprim.core.TestSoftDeleteUser.class
                .isAnnotationPresent(sant1ago.dev.suprim.annotation.entity.SoftDeletes.class));
        }

        @Test
        @DisplayName("Default column name is deleted_at")
        void testDefaultColumnName() {
            sant1ago.dev.suprim.annotation.entity.SoftDeletes annotation =
                sant1ago.dev.suprim.core.TestSoftDeleteUser.class
                    .getAnnotation(sant1ago.dev.suprim.annotation.entity.SoftDeletes.class);

            assertEquals("deleted_at", annotation.column());
        }
    }
}
