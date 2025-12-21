package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestOrder_;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.type.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EXISTS predicate support and closure-based grouping.
 * Covers:
 * - Predicate.orExists(), orNotExists(), andExists(), andNotExists()
 * - Suprim.exists(), notExists() static factories
 * - SelectBuilder.orExists(), orNotExists()
 * - Closure-based grouping: where(Function), and(Function), or(Function)
 */
@DisplayName("EXISTS Predicate and Closure Grouping Tests")
class ExistsPredicateTest {

    private final SqlDialect dialect = PostgreSqlDialect.INSTANCE;

    // ==================== PREDICATE EXISTS METHODS ====================

    @Nested
    @DisplayName("Predicate EXISTS Methods")
    class PredicateExistsMethods {

        @Test
        @DisplayName("orExists() combines predicate with EXISTS using OR")
        void orExists_combinesWithOr() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE)
                .whereRaw("orders.user_id = users.id");

            Predicate predicate = TestUser_.IS_ACTIVE.eq(true).orExists(subquery);
            String sql = predicate.toSql(dialect);

            assertTrue(sql.contains("OR EXISTS"), "Should contain OR EXISTS");
            assertTrue(sql.startsWith("("), "Should be wrapped in parentheses");
        }

        @Test
        @DisplayName("orNotExists() combines predicate with NOT EXISTS using OR")
        void orNotExists_combinesWithOrNot() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE)
                .whereRaw("orders.user_id = users.id");

            Predicate predicate = TestUser_.IS_ACTIVE.eq(true).orNotExists(subquery);
            String sql = predicate.toSql(dialect);

            assertTrue(sql.contains("OR NOT EXISTS"), "Should contain OR NOT EXISTS");
        }

        @Test
        @DisplayName("andExists() combines predicate with EXISTS using AND")
        void andExists_combinesWithAnd() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE)
                .whereRaw("orders.user_id = users.id");

            Predicate predicate = TestUser_.IS_ACTIVE.eq(true).andExists(subquery);
            String sql = predicate.toSql(dialect);

            assertTrue(sql.contains("AND EXISTS"), "Should contain AND EXISTS");
        }

        @Test
        @DisplayName("andNotExists() combines predicate with NOT EXISTS using AND")
        void andNotExists_combinesWithAndNot() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE)
                .whereRaw("orders.user_id = users.id");

            Predicate predicate = TestUser_.IS_ACTIVE.eq(true).andNotExists(subquery);
            String sql = predicate.toSql(dialect);

            assertTrue(sql.contains("AND NOT EXISTS"), "Should contain AND NOT EXISTS");
        }
    }

    // ==================== SUPRIM STATIC FACTORIES ====================

    @Nested
    @DisplayName("Suprim Static Factory Methods")
    class SuprimStaticFactories {

        @Test
        @DisplayName("Suprim.exists() creates EXISTS predicate")
        void exists_createsExistsPredicate() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE);

            Predicate predicate = Suprim.exists(subquery);
            String sql = predicate.toSql(dialect);

            assertTrue(sql.startsWith("EXISTS"), "Should start with EXISTS");
            assertTrue(sql.contains("SELECT 1"), "Should contain subquery");
        }

        @Test
        @DisplayName("Suprim.notExists() creates NOT EXISTS predicate")
        void notExists_createsNotExistsPredicate() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE);

            Predicate predicate = Suprim.notExists(subquery);
            String sql = predicate.toSql(dialect);

            assertTrue(sql.startsWith("NOT EXISTS"), "Should start with NOT EXISTS");
        }

        @Test
        @DisplayName("Suprim.exists() can be combined with .or()")
        void exists_combinableWithOr() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE)
                .whereRaw("orders.user_id = users.id");

            Predicate predicate = TestUser_.NAME.eq("Admin")
                .or(Suprim.exists(subquery));

            String sql = predicate.toSql(dialect);
            assertTrue(sql.contains("OR EXISTS"), "Should contain OR EXISTS");
        }
    }

    // ==================== SELECT BUILDER EXISTS METHODS ====================

    @Nested
    @DisplayName("SelectBuilder EXISTS Methods")
    class SelectBuilderExistsMethods {

        @Test
        @DisplayName("orExists() appends OR EXISTS to WHERE clause")
        void orExists_appendsToWhere() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE)
                .whereRaw("orders.user_id = users.id");

            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .where(TestUser_.IS_ACTIVE.eq(true))
                .orExists(subquery)
                .build(dialect);

            assertTrue(result.sql().contains("OR EXISTS"), "Should contain OR EXISTS");
        }

        @Test
        @DisplayName("orNotExists() appends OR NOT EXISTS to WHERE clause")
        void orNotExists_appendsToWhere() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE);

            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .where(TestUser_.IS_ACTIVE.eq(true))
                .orNotExists(subquery)
                .build(dialect);

            assertTrue(result.sql().contains("OR NOT EXISTS"), "Should contain OR NOT EXISTS");
        }

        @Test
        @DisplayName("orExists() with no prior WHERE sets EXISTS as WHERE")
        void orExists_noPriorWhere_setsAsWhere() {
            SelectBuilder subquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE);

            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .orExists(subquery)
                .build(dialect);

            assertTrue(result.sql().contains("WHERE EXISTS"), "Should have WHERE EXISTS");
        }
    }

    // ==================== CLOSURE-BASED GROUPING ====================

    @Nested
    @DisplayName("Closure-Based Grouping")
    class ClosureBasedGrouping {

        @Test
        @DisplayName("where(Function) creates grouped condition")
        void where_function_createsGroup() {
            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .where(q -> q.where(TestUser_.NAME.eq("Admin")).or(TestUser_.NAME.eq("Mod")))
                .build(dialect);

            String sql = result.sql();
            // The group should be parenthesized by CompositePredicate
            assertTrue(sql.contains("WHERE ("), "Should have WHERE with grouped conditions");
            assertTrue(sql.contains("OR"), "Should contain OR");
        }

        @Test
        @DisplayName("and(Function) appends grouped AND condition")
        void and_function_appendsGroupedAnd() {
            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .where(TestUser_.IS_ACTIVE.eq(true))
                .and(q -> q.where(TestUser_.NAME.eq("Admin")).or(TestUser_.NAME.eq("Mod")))
                .build(dialect);

            String sql = result.sql();
            assertTrue(sql.contains("AND ("), "Should have AND with grouped conditions");
        }

        @Test
        @DisplayName("or(Function) appends grouped OR condition")
        void or_function_appendsGroupedOr() {
            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .where(TestUser_.IS_ACTIVE.eq(true))
                .or(q -> q.where(TestUser_.AGE.gt(30)).and(TestUser_.NAME.eq("Senior")))
                .build(dialect);

            String sql = result.sql();
            assertTrue(sql.contains("OR ("), "Should have OR with grouped conditions");
        }

        @Test
        @DisplayName("Closure with orExists() generates correct SQL")
        void closure_withOrExists_generatesCorrectSql() {
            SelectBuilder modelsSubquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE)
                .whereRaw("orders.user_id = users.id");

            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .where(TestUser_.IS_ACTIVE.eq(true))
                .and(q -> q.where(TestUser_.NAME.ne("Guest")).orExists(modelsSubquery))
                .build(dialect);

            String sql = result.sql();
            assertTrue(sql.contains("AND ("), "Should have AND with grouped conditions");
            assertTrue(sql.contains("OR EXISTS"), "Should contain OR EXISTS inside group");
        }
    }

    // ==================== COMPLEX SCENARIOS ====================

    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexScenarios {

        @Test
        @DisplayName("Original use case: SIMPLE type OR EXISTS attached models")
        void originalUseCase_simpleTypeOrExists() {
            SelectBuilder attachedModelsSubquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE)
                .whereRaw("orders.user_id = users.id")
                .andRaw("orders.\"amount\" > 0");

            // Using Predicate.orExists()
            QueryResult result = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
                .from(TestUser_.TABLE)
                .where(TestUser_.IS_ACTIVE.eq(true))
                .and(TestUser_.NAME.ne("SIMPLE").orExists(attachedModelsSubquery))
                .build(dialect);

            String sql = result.sql();
            assertTrue(sql.contains("AND ("), "Condition should be grouped with AND");
            assertTrue(sql.contains("OR EXISTS"), "Should contain OR EXISTS");
            assertTrue(sql.contains("SELECT 1 FROM"), "Should have EXISTS subquery");
        }

        @Test
        @DisplayName("Nested closures maintain correct precedence")
        void nestedClosures_correctPrecedence() {
            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .where(TestUser_.IS_ACTIVE.eq(true))
                .and(q -> q
                    .where(TestUser_.AGE.gte(18))
                    .and(q2 -> q2
                        .where(TestUser_.NAME.eq("Admin"))
                        .or(TestUser_.NAME.eq("Mod"))
                    )
                )
                .build(dialect);

            String sql = result.sql();
            assertNotNull(sql);
            assertTrue(sql.contains("WHERE"), "Should have WHERE clause");
        }

        @Test
        @DisplayName("Multiple EXISTS with different operators")
        void multipleExists_differentOperators() {
            SelectBuilder ordersSubquery = Suprim.selectRaw("1")
                .from(TestOrder_.TABLE)
                .whereRaw("orders.user_id = users.id");

            QueryResult result = Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .whereExists(ordersSubquery)
                .orNotExists(ordersSubquery)
                .build(dialect);

            String sql = result.sql();
            assertTrue(sql.contains("EXISTS"), "Should contain EXISTS");
            assertTrue(sql.contains("OR NOT EXISTS"), "Should contain OR NOT EXISTS");
        }
    }
}
