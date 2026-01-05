package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestOrder_;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SelectQueryRenderer - SQL generation from SelectBuilder state.
 */
@DisplayName("SelectQueryRenderer Tests")
class SelectQueryRendererTest {

    @Nested
    @DisplayName("Basic Rendering")
    class BasicRenderingTests {

        @Test
        @DisplayName("Renders simple SELECT")
        void testRenderSimpleSelect() {
            SelectBuilder builder = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
                    .from(TestUser_.TABLE);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("SELECT"));
            assertTrue(result.sql().contains("FROM"));
        }

        @Test
        @DisplayName("Renders SELECT *")
        void testRenderSelectAll() {
            SelectBuilder builder = Suprim.select().from(TestUser_.TABLE);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("SELECT *"));
        }

        @Test
        @DisplayName("Renders DISTINCT")
        void testRenderDistinct() {
            SelectBuilder builder = Suprim.select(TestUser_.EMAIL)
                    .from(TestUser_.TABLE)
                    .distinct();

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("SELECT DISTINCT"));
        }
    }

    @Nested
    @DisplayName("WHERE Clause Rendering")
    class WhereClauseTests {

        @Test
        @DisplayName("Renders WHERE clause")
        void testRenderWhereClause() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com"));

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("WHERE"));
            assertFalse(result.parameters().isEmpty());
        }

        @Test
        @DisplayName("Renders AND conditions")
        void testRenderAndConditions() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com"))
                    .and(TestUser_.AGE.gte(18));

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("AND"));
        }
    }

    @Nested
    @DisplayName("JOIN Rendering")
    class JoinTests {

        @Test
        @DisplayName("Renders LEFT JOIN")
        void testRenderLeftJoin() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .leftJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID));

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("LEFT JOIN"));
            assertTrue(result.sql().contains("ON"));
        }

        @Test
        @DisplayName("Renders INNER JOIN")
        void testRenderInnerJoin() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .join(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID));

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("JOIN"));
        }
    }

    @Nested
    @DisplayName("ORDER BY Rendering")
    class OrderByTests {

        @Test
        @DisplayName("Renders ORDER BY")
        void testRenderOrderBy() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .orderBy(TestUser_.CREATED_AT.desc());

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("ORDER BY"));
        }

        @Test
        @DisplayName("Renders multiple ORDER BY")
        void testRenderMultipleOrderBy() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .orderBy(TestUser_.AGE.desc(), TestUser_.EMAIL.asc());

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("ORDER BY"));
            assertTrue(result.sql().contains(","));
        }
    }

    @Nested
    @DisplayName("GROUP BY / HAVING Rendering")
    class GroupByHavingTests {

        @Test
        @DisplayName("Renders GROUP BY")
        void testRenderGroupBy() {
            SelectBuilder builder = Suprim.select(TestUser_.AGE)
                    .from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("GROUP BY"));
        }

        @Test
        @DisplayName("Renders HAVING")
        void testRenderHaving() {
            SelectBuilder builder = Suprim.select(TestUser_.AGE)
                    .from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE)
                    .havingRaw("COUNT(*) > 5");

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("HAVING"));
        }
    }

    @Nested
    @DisplayName("LIMIT / OFFSET Rendering")
    class LimitOffsetTests {

        @Test
        @DisplayName("Renders LIMIT")
        void testRenderLimit() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .limit(10);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("LIMIT 10"));
        }

        @Test
        @DisplayName("Renders OFFSET")
        void testRenderOffset() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .limit(10)
                    .offset(20);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("OFFSET 20"));
        }

        @Test
        @DisplayName("Does not render OFFSET 0")
        void testNoOffsetZero() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .limit(10)
                    .offset(0);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertFalse(result.sql().contains("OFFSET"));
        }
    }

    @Nested
    @DisplayName("SET Operations Rendering")
    class SetOperationsTests {

        @Test
        @DisplayName("Renders UNION")
        void testRenderUnion() {
            SelectBuilder builder1 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            SelectBuilder builder2 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);

            builder1.union(builder2);

            QueryResult result = SelectQueryRenderer.render(builder1, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("UNION"));
        }

        @Test
        @DisplayName("Renders UNION ALL")
        void testRenderUnionAll() {
            SelectBuilder builder1 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            SelectBuilder builder2 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);

            builder1.unionAll(builder2);

            QueryResult result = SelectQueryRenderer.render(builder1, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("UNION ALL"));
        }
    }

    @Nested
    @DisplayName("CTE Rendering")
    class CteTests {

        @Test
        @DisplayName("Renders WITH clause")
        void testRenderCte() {
            SelectBuilder cte = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            SelectBuilder builder = Suprim.select()
                    .with("active_users", cte)
                    .from(TestUser_.TABLE);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("WITH"));
        }

        @Test
        @DisplayName("Renders WITH RECURSIVE")
        void testRenderRecursiveCte() {
            SelectBuilder builder = Suprim.select()
                    .withRecursive("tree", "SELECT 1")
                    .from(TestUser_.TABLE);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("WITH RECURSIVE"));
        }
    }

    @Nested
    @DisplayName("Locking Rendering")
    class LockingTests {

        @Test
        @DisplayName("Renders FOR UPDATE")
        void testRenderForUpdate() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .forUpdate();

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("FOR UPDATE"));
        }

        @Test
        @DisplayName("Renders FOR SHARE")
        void testRenderForShare() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .forShare();

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.sql().contains("FOR SHARE"));
        }
    }

    @Nested
    @DisplayName("Dialect-specific Rendering")
    class DialectTests {

        @Test
        @DisplayName("PostgreSQL dialect renders correctly")
        void testPostgresDialect() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com"));

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertNotNull(result.sql());
            assertTrue(result.sql().contains("\""));  // PostgreSQL quotes
        }

        @Test
        @DisplayName("MySQL dialect renders correctly")
        void testMySqlDialect() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com"));

            QueryResult result = SelectQueryRenderer.render(builder, MySqlDialect.INSTANCE);

            assertNotNull(result.sql());
            assertTrue(result.sql().contains("`"));  // MySQL backticks
        }
    }

    @Nested
    @DisplayName("Parameter Handling")
    class ParameterTests {

        @Test
        @DisplayName("Collects WHERE parameters")
        void testCollectsWhereParams() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com"))
                    .and(TestUser_.AGE.gte(18));

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertEquals(2, result.parameters().size());
        }

        @Test
        @DisplayName("Returns empty params for no conditions")
        void testEmptyParamsForNoConditions() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertTrue(result.parameters().isEmpty());
        }
    }

    @Nested
    @DisplayName("Eager Load Metadata")
    class EagerLoadTests {

        @Test
        @DisplayName("Returns soft delete scope")
        void testReturnsSoftDeleteScope() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE);

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertEquals(SelectBuilder.SoftDeleteScope.DEFAULT, result.softDeleteScope());
        }

        @Test
        @DisplayName("Returns WITH_TRASHED scope")
        void testReturnsWithTrashedScope() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .withTrashed();

            QueryResult result = SelectQueryRenderer.render(builder, PostgreSqlDialect.INSTANCE);

            assertEquals(SelectBuilder.SoftDeleteScope.WITH_TRASHED, result.softDeleteScope());
        }
    }
}
