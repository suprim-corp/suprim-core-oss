package sant1ago.dev.suprim.core.query.select;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestOrder_;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.query.Suprim;
import sant1ago.dev.suprim.core.type.OrderDirection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for mixin interface default methods.
 * Verifies that default methods in mixin interfaces work correctly
 * when implemented by SelectBuilder.
 */
@DisplayName("Mixin Interface Tests")
class MixinInterfaceTest {

    @Nested
    @DisplayName("WhereClauseSupport")
    class WhereClauseSupportTests {

        @Test
        @DisplayName("where() adds WHERE clause")
        void testWhere() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com"));

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("WHERE"));
        }

        @Test
        @DisplayName("and() chains conditions")
        void testAnd() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com"))
                    .and(TestUser_.AGE.gte(18));

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("AND"));
        }

        @Test
        @DisplayName("or() chains conditions")
        void testOr() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com"))
                    .or(TestUser_.EMAIL.eq("other@example.com"));

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("OR"));
        }

        @Test
        @DisplayName("whereRaw() adds raw SQL")
        void testWhereRaw() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .whereRaw("1 = 1");

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("1 = 1"));
        }

        @Test
        @DisplayName("where with isNull() checks for NULL")
        void testWhereIsNull() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.isNull());

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("IS NULL"));
        }

        @Test
        @DisplayName("where with isNotNull() checks for NOT NULL")
        void testWhereIsNotNull() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.isNotNull());

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("IS NOT NULL"));
        }

        @Test
        @DisplayName("whereIf() conditionally adds clause")
        void testWhereIfTrue() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .whereIf(true, TestUser_.AGE.gte(18));

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("WHERE"));
        }

        @Test
        @DisplayName("whereIf() skips when false")
        void testWhereIfFalse() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .whereIf(false, TestUser_.AGE.gte(18));

            QueryResult result = builder.build();
            assertFalse(result.sql().contains("WHERE"));
        }
    }

    @Nested
    @DisplayName("JoinClauseSupport")
    class JoinClauseSupportTests {

        @Test
        @DisplayName("join() adds INNER JOIN")
        void testJoin() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .join(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID));

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("JOIN"));
        }

        @Test
        @DisplayName("leftJoin() adds LEFT JOIN")
        void testLeftJoin() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .leftJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID));

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("LEFT JOIN"));
        }

        @Test
        @DisplayName("rightJoin() adds RIGHT JOIN")
        void testRightJoin() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .rightJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID));

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("RIGHT JOIN"));
        }

        @Test
        @DisplayName("joinRaw() adds raw JOIN")
        void testJoinRaw() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .joinRaw("CROSS JOIN \"orders\"");

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("CROSS JOIN"));
        }
    }

    @Nested
    @DisplayName("GroupByHavingSupport")
    class GroupByHavingSupportTests {

        @Test
        @DisplayName("groupBy() adds GROUP BY")
        void testGroupBy() {
            SelectBuilder builder = Suprim.select(TestUser_.AGE)
                    .from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE);

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("GROUP BY"));
        }

        @Test
        @DisplayName("groupByRaw() adds raw GROUP BY")
        void testGroupByRaw() {
            SelectBuilder builder = Suprim.select(TestUser_.AGE)
                    .from(TestUser_.TABLE)
                    .groupByRaw("EXTRACT(YEAR FROM created_at)");

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("GROUP BY"));
        }

        @Test
        @DisplayName("having() adds HAVING")
        void testHaving() {
            SelectBuilder builder = Suprim.select(TestUser_.AGE)
                    .from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE)
                    .having(TestUser_.AGE.gte(18));

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("HAVING"));
        }

        @Test
        @DisplayName("havingRaw() adds raw HAVING")
        void testHavingRaw() {
            SelectBuilder builder = Suprim.select(TestUser_.AGE)
                    .from(TestUser_.TABLE)
                    .groupBy(TestUser_.AGE)
                    .havingRaw("COUNT(*) > 5");

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("HAVING"));
            assertTrue(result.sql().contains("COUNT(*)"));
        }
    }

    @Nested
    @DisplayName("SubquerySupport")
    class SubquerySupportTests {

        @Test
        @DisplayName("whereExists() adds EXISTS subquery")
        void testWhereExists() {
            SelectBuilder subquery = Suprim.select(TestOrder_.ID)
                    .from(TestOrder_.TABLE)
                    .whereRaw("orders.user_id = users.id");

            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .whereExists(subquery);

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("EXISTS"));
        }

        @Test
        @DisplayName("whereNotExists() adds NOT EXISTS subquery")
        void testWhereNotExists() {
            SelectBuilder subquery = Suprim.select(TestOrder_.ID)
                    .from(TestOrder_.TABLE)
                    .whereRaw("orders.user_id = users.id");

            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .whereNotExists(subquery);

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("NOT EXISTS"));
        }
    }

    @Nested
    @DisplayName("SetOperationSupport")
    class SetOperationSupportTests {

        @Test
        @DisplayName("union() combines queries")
        void testUnion() {
            SelectBuilder builder1 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            SelectBuilder builder2 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);

            builder1.union(builder2);
            QueryResult result = builder1.build();

            assertTrue(result.sql().contains("UNION"));
        }

        @Test
        @DisplayName("unionAll() combines with duplicates")
        void testUnionAll() {
            SelectBuilder builder1 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            SelectBuilder builder2 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);

            builder1.unionAll(builder2);
            QueryResult result = builder1.build();

            assertTrue(result.sql().contains("UNION ALL"));
        }

        @Test
        @DisplayName("intersect() returns common rows")
        void testIntersect() {
            SelectBuilder builder1 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            SelectBuilder builder2 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);

            builder1.intersect(builder2);
            QueryResult result = builder1.build();

            assertTrue(result.sql().contains("INTERSECT"));
        }

        @Test
        @DisplayName("except() returns difference")
        void testExcept() {
            SelectBuilder builder1 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            SelectBuilder builder2 = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);

            builder1.except(builder2);
            QueryResult result = builder1.build();

            assertTrue(result.sql().contains("EXCEPT"));
        }
    }

    @Nested
    @DisplayName("LockingSupport")
    class LockingSupportTests {

        @Test
        @DisplayName("forUpdate() adds FOR UPDATE")
        void testForUpdate() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .forUpdate();

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("FOR UPDATE"));
        }

        @Test
        @DisplayName("forShare() adds FOR SHARE")
        void testForShare() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .forShare();

            QueryResult result = builder.build();
            assertTrue(result.sql().contains("FOR SHARE"));
        }
    }

    @Nested
    @DisplayName("SoftDeleteSupport")
    class SoftDeleteSupportTests {

        @Test
        @DisplayName("withTrashed() sets scope")
        void testWithTrashed() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .withTrashed();

            assertEquals(SelectBuilder.SoftDeleteScope.WITH_TRASHED, builder.getSoftDeleteScope());
        }

        @Test
        @DisplayName("onlyTrashed() sets scope")
        void testOnlyTrashed() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .onlyTrashed();

            assertEquals(SelectBuilder.SoftDeleteScope.ONLY_TRASHED, builder.getSoftDeleteScope());
        }
    }

    @Nested
    @DisplayName("EagerLoadSupport")
    class EagerLoadSupportTests {

        @Test
        @DisplayName("with() adds eager load")
        void testWith() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .with(TestUser_.ORDERS);

            assertFalse(builder.getEagerLoads().isEmpty());
        }

        @Test
        @DisplayName("without() excludes relation")
        void testWithout() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .without("orders");

            assertTrue(builder.getWithoutRelations().contains("orders"));
        }
    }

    @Nested
    @DisplayName("SelectBuilderCore")
    class SelectBuilderCoreTests {

        @Test
        @DisplayName("self() returns SelectBuilder")
        void testSelf() {
            SelectBuilder builder = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);
            assertSame(builder, builder.self());
        }

        @Test
        @DisplayName("nextParamName() generates unique names")
        void testNextParamName() {
            SelectBuilder builder = Suprim.select(TestUser_.ID).from(TestUser_.TABLE);

            String p1 = builder.nextParamName();
            String p2 = builder.nextParamName();

            assertNotEquals(p1, p2);
            assertTrue(p1.startsWith("p"));
            assertTrue(p2.startsWith("p"));
        }

        @Test
        @DisplayName("whereClause() returns current WHERE")
        void testWhereClause() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE)
                    .where(TestUser_.EMAIL.eq("test@example.com"));

            assertNotNull(builder.whereClause());
        }

        @Test
        @DisplayName("fromTable() returns FROM table")
        void testFromTable() {
            SelectBuilder builder = Suprim.select(TestUser_.ID)
                    .from(TestUser_.TABLE);

            assertEquals(TestUser_.TABLE, builder.fromTable());
        }
    }
}
