package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.query.ParameterContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Predicate types and composition.
 */
@DisplayName("Predicate Tests")
class PredicateTest {

    // ==================== SIMPLE PREDICATE ====================

    @Test
    @DisplayName("SimplePredicate with EQUALS")
    void testSimplePredicateEquals() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.EMAIL,
            Operator.EQUALS,
            new Literal<>("test@example.com", String.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"email\" = 'test@example.com'"));
    }

    @Test
    @DisplayName("SimplePredicate with NOT_EQUALS")
    void testSimplePredicateNotEquals() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.NOT_EQUALS,
            new Literal<>(18, Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" != 18"));
    }

    @Test
    @DisplayName("SimplePredicate with GREATER_THAN")
    void testSimplePredicateGreaterThan() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.GREATER_THAN,
            new Literal<>(21, Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" > 21"));
    }

    @Test
    @DisplayName("SimplePredicate with IS_NULL")
    void testSimplePredicateIsNull() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.NAME,
            Operator.IS_NULL,
            null
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"name\" IS NULL"));
    }

    @Test
    @DisplayName("SimplePredicate with IS_NOT_NULL")
    void testSimplePredicateIsNotNull() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.EMAIL,
            Operator.IS_NOT_NULL,
            null
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"email\" IS NOT NULL"));
    }

    @Test
    @DisplayName("SimplePredicate with IN")
    void testSimplePredicateIn() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.IN,
            new ListLiteral<>(java.util.List.of(18, 21, 25), Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" IN (18, 21, 25)"));
    }

    @Test
    @DisplayName("SimplePredicate with NOT_IN")
    void testSimplePredicateNotIn() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.NOT_IN,
            new ListLiteral<>(java.util.List.of(1, 2, 3), Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" NOT IN (1, 2, 3)"));
    }

    @Test
    @DisplayName("SimplePredicate with BETWEEN")
    void testSimplePredicateBetween() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.AGE,
            Operator.BETWEEN,
            new ListLiteral<>(java.util.List.of(18, 65), Integer.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"age\" BETWEEN 18 AND 65"));
    }

    @Test
    @DisplayName("SimplePredicate with LIKE")
    void testSimplePredicateLike() {
        Predicate predicate = new Predicate.SimplePredicate(
            TestUser_.EMAIL,
            Operator.LIKE,
            new Literal<>("%@example.com", String.class)
        );

        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("users.\"email\" LIKE '%@example.com'"));
    }

    // ==================== COMPOSITE PREDICATE ====================

    @Test
    @DisplayName("CompositePredicate with AND")
    void testCompositePredicateAnd() {
        Predicate left = TestUser_.AGE.gte(18);
        Predicate right = TestUser_.IS_ACTIVE.eq(true);
        Predicate composite = new Predicate.CompositePredicate(left, LogicalOperator.AND, right);

        String sql = composite.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("users.\"age\" >= 18"));
        assertTrue(sql.contains("users.\"is_active\""));
    }

    @Test
    @DisplayName("CompositePredicate with OR")
    void testCompositePredicateOr() {
        Predicate left = TestUser_.AGE.lt(18);
        Predicate right = TestUser_.AGE.gt(65);
        Predicate composite = new Predicate.CompositePredicate(left, LogicalOperator.OR, right);

        String sql = composite.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("OR"));
        assertTrue(sql.contains("users.\"age\" < 18"));
        assertTrue(sql.contains("users.\"age\" > 65"));
    }

    @Test
    @DisplayName("Nested CompositePredicate")
    void testNestedCompositePredicate() {
        Predicate innerLeft = TestUser_.AGE.gte(18);
        Predicate innerRight = TestUser_.IS_ACTIVE.eq(true);
        Predicate inner = new Predicate.CompositePredicate(innerLeft, LogicalOperator.AND, innerRight);

        Predicate outer = new Predicate.CompositePredicate(
            inner,
            LogicalOperator.OR,
            TestUser_.EMAIL.like("%@admin.com")
        );

        String sql = outer.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
        assertTrue(sql.contains("("));
        assertTrue(sql.contains(")"));
    }

    // ==================== NOT PREDICATE ====================

    @Test
    @DisplayName("NotPredicate negates simple predicate")
    void testNotPredicate() {
        Predicate inner = TestUser_.IS_ACTIVE.eq(true);
        Predicate not = new Predicate.NotPredicate(inner);

        String sql = not.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("NOT ("));
        assertTrue(sql.contains("users.\"is_active\""));
    }

    @Test
    @DisplayName("NotPredicate negates composite predicate")
    void testNotPredicateComposite() {
        Predicate inner = TestUser_.AGE.gte(18).and(TestUser_.IS_ACTIVE.eq(true));
        Predicate not = new Predicate.NotPredicate(inner);

        String sql = not.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("NOT ("));
        assertTrue(sql.contains("AND"));
    }

    // ==================== RAW PREDICATE ====================

    @Test
    @DisplayName("RawPredicate renders raw SQL")
    void testRawPredicate() {
        Predicate raw = new Predicate.RawPredicate("created_at > NOW() - INTERVAL '7 days'");

        String sql = raw.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("created_at > NOW() - INTERVAL '7 days'", sql);
    }

    @Test
    @DisplayName("RawPredicate with complex SQL")
    void testRawPredicateComplex() {
        Predicate raw = new Predicate.RawPredicate(
            "EXTRACT(YEAR FROM created_at) = 2024 AND status IN ('active', 'pending')"
        );

        String sql = raw.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("EXTRACT"));
        assertTrue(sql.contains("status IN"));
    }

    // ==================== PREDICATE COMPOSITION METHODS ====================

    @Test
    @DisplayName("Predicate.and() composition")
    void testPredicateAndComposition() {
        Predicate p1 = TestUser_.AGE.gte(18);
        Predicate p2 = TestUser_.IS_ACTIVE.eq(true);
        Predicate composed = p1.and(p2);

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("users.\"age\" >= 18"));
        assertTrue(sql.contains("users.\"is_active\""));
    }

    @Test
    @DisplayName("Predicate.or() composition")
    void testPredicateOrComposition() {
        Predicate p1 = TestUser_.AGE.lt(18);
        Predicate p2 = TestUser_.AGE.gt(65);
        Predicate composed = p1.or(p2);

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("OR"));
    }

    @Test
    @DisplayName("Predicate.not() negation")
    void testPredicateNotNegation() {
        Predicate p = TestUser_.IS_ACTIVE.eq(true);
        Predicate negated = p.not();

        String sql = negated.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("NOT ("));
    }

    @Test
    @DisplayName("Chained predicate composition")
    void testChainedPredicateComposition() {
        Predicate composed = TestUser_.AGE.gte(18)
            .and(TestUser_.IS_ACTIVE.eq(true))
            .and(TestUser_.EMAIL.isNotNull());

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        // Should contain multiple AND operators
        int andCount = sql.split("AND").length - 1;
        assertTrue(andCount >= 2);
    }

    @Test
    @DisplayName("Complex predicate with mixed AND/OR")
    void testComplexPredicateMixedLogic() {
        Predicate composed = TestUser_.AGE.gte(18)
            .and(TestUser_.IS_ACTIVE.eq(true))
            .or(TestUser_.EMAIL.like("%@admin.com"));

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Empty RawPredicate")
    void testEmptyRawPredicate() {
        Predicate raw = new Predicate.RawPredicate("");
        String sql = raw.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("", sql);
    }

    @Test
    @DisplayName("Double negation")
    void testDoubleNegation() {
        Predicate p = TestUser_.IS_ACTIVE.eq(true);
        Predicate notNot = p.not().not();

        String sql = notNot.toSql(PostgreSqlDialect.INSTANCE);
        // Should have two NOT wrappers
        assertTrue(sql.contains("NOT (NOT ("));
    }

    @Test
    @DisplayName("Predicate with parentheses grouping")
    void testPredicateParenthesesGrouping() {
        // (age >= 18 AND is_active = true) OR (age >= 65)
        Predicate group1 = TestUser_.AGE.gte(18).and(TestUser_.IS_ACTIVE.eq(true));
        Predicate group2 = TestUser_.AGE.gte(65);
        Predicate composed = group1.or(group2);

        String sql = composed.toSql(PostgreSqlDialect.INSTANCE);
        // Check that parentheses are present for grouping
        assertTrue(sql.contains("("));
        assertTrue(sql.contains(")"));
    }

    // ==================== SimplePredicate Branch Coverage Tests ====================

    @Test
    @DisplayName("IN with null right expression")
    void testInWithNullRight() {
        Predicate predicate = new Predicate.SimplePredicate(
                TestUser_.AGE, Operator.IN, null
        );
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("IN ()"));
    }

    @Test
    @DisplayName("BETWEEN with wrong size ListLiteral")
    void testBetweenWithWrongSizeList() {
        // Single value list - should fallback to "BETWEEN ?"
        Predicate predicate = new Predicate.SimplePredicate(
                TestUser_.AGE, Operator.BETWEEN, new ListLiteral<>(List.of(10), Integer.class)
        );
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("BETWEEN ?"));
    }

    @Test
    @DisplayName("BETWEEN with non-ListLiteral right")
    void testBetweenWithNonListLiteral() {
        Predicate predicate = new Predicate.SimplePredicate(
                TestUser_.AGE, Operator.BETWEEN, new Literal<>(50, Integer.class)
        );
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("BETWEEN ?"));
    }

    @Test
    @DisplayName("ILIKE with null right - PostgreSQL")
    void testIlikeWithNullRightPostgres() {
        Predicate predicate = new Predicate.SimplePredicate(
                TestUser_.NAME, Operator.ILIKE, null
        );
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("ILIKE NULL"));
    }

    @Test
    @DisplayName("ILIKE with null right - MySQL")
    void testIlikeWithNullRightMysql() {
        Predicate predicate = new Predicate.SimplePredicate(
                TestUser_.NAME, Operator.ILIKE, null
        );
        String sql = predicate.toSql(MySqlDialect.INSTANCE);
        assertTrue(sql.contains("LOWER") && sql.contains("NULL"));
    }

    @Test
    @DisplayName("Array operator with MySQL throws exception")
    void testArrayOperatorWithMysqlThrows() {
        @Entity(table = "test")
        class TestEntity {}
        Table<TestEntity> table = Table.of("test", TestEntity.class);
        ArrayColumn<TestEntity, String> tags = new ArrayColumn<>(table, "tags", String.class, "TEXT[]");

        Predicate predicate = new Predicate.SimplePredicate(
                tags, Operator.ARRAY_CONTAINS, new ArrayColumn.ArrayLiteral<>(List.of("a"), String.class)
        );
        assertThrows(UnsupportedDialectFeatureException.class,
                () -> predicate.toSql(MySqlDialect.INSTANCE));
    }

    @Test
    @DisplayName("Array operator with null right - PostgreSQL")
    void testArrayOperatorWithNullRight() {
        @Entity(table = "test")
        class TestEntity {}
        Table<TestEntity> table = Table.of("test", TestEntity.class);
        ArrayColumn<TestEntity, String> tags = new ArrayColumn<>(table, "tags", String.class, "TEXT[]");

        Predicate predicate = new Predicate.SimplePredicate(
                tags, Operator.ARRAY_CONTAINS, null
        );
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("@> NULL"));
    }

    @Test
    @DisplayName("Default case with null right")
    void testDefaultCaseWithNullRight() {
        Predicate predicate = new Predicate.SimplePredicate(
                TestUser_.AGE, Operator.EQUALS, null
        );
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("= NULL"));
    }

    // ==================== PARAMETERIZED PREDICATE TESTS ====================

    @Nested
    @DisplayName("Parameterized Predicates")
    class ParameterizedPredicateTests {

        @Test
        @DisplayName("SimplePredicate parameterized EQUALS")
        void testParameterizedEquals() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.EMAIL, Operator.EQUALS, new Literal<>("test@example.com", String.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains(":p1"));
            assertEquals("test@example.com", params.getParameters().get("p1"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized IS_NULL")
        void testParameterizedIsNull() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.NAME, Operator.IS_NULL, null
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("IS NULL"));
            assertTrue(params.getParameters().isEmpty());
        }

        @Test
        @DisplayName("SimplePredicate parameterized IS_NOT_NULL")
        void testParameterizedIsNotNull() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.EMAIL, Operator.IS_NOT_NULL, null
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("IS NOT NULL"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized IN")
        void testParameterizedIn() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.AGE, Operator.IN, new ListLiteral<>(List.of(18, 21, 25), Integer.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("IN (:p1, :p2, :p3)"));
            assertEquals(18, params.getParameters().get("p1"));
            assertEquals(21, params.getParameters().get("p2"));
            assertEquals(25, params.getParameters().get("p3"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized IN with null")
        void testParameterizedInWithNull() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.AGE, Operator.IN, null
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("IN ()"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized BETWEEN")
        void testParameterizedBetween() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.AGE, Operator.BETWEEN, new ListLiteral<>(List.of(18, 65), Integer.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("BETWEEN :p1 AND :p2"));
            assertEquals(18, params.getParameters().get("p1"));
            assertEquals(65, params.getParameters().get("p2"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized BETWEEN with wrong size")
        void testParameterizedBetweenWrongSize() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.AGE, Operator.BETWEEN, new ListLiteral<>(List.of(10), Integer.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("BETWEEN ?"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized ILIKE - PostgreSQL")
        void testParameterizedIlikePostgres() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.NAME, Operator.ILIKE, new Literal<>("%test%", String.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("ILIKE :p1"));
            assertEquals("%test%", params.getParameters().get("p1"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized ILIKE - MySQL")
        void testParameterizedIlikeMysql() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.NAME, Operator.ILIKE, new Literal<>("%test%", String.class)
            );
            String sql = predicate.toSql(MySqlDialect.INSTANCE, params);
            assertTrue(sql.contains("LOWER"));
            assertTrue(sql.contains("LIKE"));
            assertTrue(sql.contains(":p1"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized ILIKE with null - PostgreSQL")
        void testParameterizedIlikeNullPostgres() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.NAME, Operator.ILIKE, null
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("ILIKE NULL"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized ILIKE with null - MySQL")
        void testParameterizedIlikeNullMysql() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.NAME, Operator.ILIKE, null
            );
            String sql = predicate.toSql(MySqlDialect.INSTANCE, params);
            assertTrue(sql.contains("LOWER") && sql.contains("NULL"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized ARRAY_CONTAINS - PostgreSQL")
        void testParameterizedArrayContainsPostgres() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            ArrayColumn<TestEntity, String> tags = new ArrayColumn<>(table, "tags", String.class, "TEXT[]");

            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    tags, Operator.ARRAY_CONTAINS, new ArrayColumn.ArrayLiteral<>(List.of("a"), String.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("@>"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized ARRAY_CONTAINS with null")
        void testParameterizedArrayContainsNull() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            ArrayColumn<TestEntity, String> tags = new ArrayColumn<>(table, "tags", String.class, "TEXT[]");

            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    tags, Operator.ARRAY_CONTAINS, null
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("@> NULL"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized ARRAY throws on MySQL")
        void testParameterizedArrayThrowsOnMysql() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            ArrayColumn<TestEntity, String> tags = new ArrayColumn<>(table, "tags", String.class, "TEXT[]");

            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    tags, Operator.ARRAY_CONTAINS, new ArrayColumn.ArrayLiteral<>(List.of("a"), String.class)
            );
            assertThrows(UnsupportedDialectFeatureException.class,
                    () -> predicate.toSql(MySqlDialect.INSTANCE, params));
        }

        @Test
        @DisplayName("SimplePredicate parameterized default case with null")
        void testParameterizedDefaultWithNull() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.AGE, Operator.EQUALS, null
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("= NULL"));
        }

        @Test
        @DisplayName("CompositePredicate parameterized AND")
        void testParameterizedCompositeAnd() {
            ParameterContext params = new ParameterContext();
            Predicate left = TestUser_.AGE.gte(18);
            Predicate right = TestUser_.IS_ACTIVE.eq(true);
            Predicate composite = new Predicate.CompositePredicate(left, LogicalOperator.AND, right);

            String sql = composite.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains(":p1"));
            assertTrue(sql.contains(":p2"));
            assertEquals(18, params.getParameters().get("p1"));
            assertEquals(true, params.getParameters().get("p2"));
        }

        @Test
        @DisplayName("NotPredicate parameterized")
        void testParameterizedNot() {
            ParameterContext params = new ParameterContext();
            Predicate inner = TestUser_.IS_ACTIVE.eq(true);
            Predicate not = new Predicate.NotPredicate(inner);

            String sql = not.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("NOT ("));
            assertTrue(sql.contains(":p1"));
            assertEquals(true, params.getParameters().get("p1"));
        }

        @Test
        @DisplayName("Default parameterized toSql delegates to non-parameterized")
        void testDefaultParameterizedDelegates() {
            ParameterContext params = new ParameterContext();
            Predicate raw = new Predicate.RawPredicate("custom_sql = 1");

            String sql = raw.toSql(PostgreSqlDialect.INSTANCE, params);
            assertEquals("custom_sql = 1", sql);
            assertTrue(params.getParameters().isEmpty());
        }

        @Test
        @DisplayName("SimplePredicate parameterized NOT_IN")
        void testParameterizedNotIn() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.AGE, Operator.NOT_IN, new ListLiteral<>(List.of(1, 2, 3), Integer.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("NOT IN (:p1, :p2, :p3)"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized JSONB_CONTAINS - PostgreSQL")
        void testParameterizedJsonbContainsPostgres() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            JsonbColumn<TestEntity> data = new JsonbColumn<>(table, "data", "JSONB");

            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    data, Operator.JSONB_CONTAINS, new JsonbColumn.JsonLiteral("{\"key\":\"value\"}")
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("@>"));
            assertTrue(sql.contains("CAST(:p1 AS jsonb)"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized JSONB_CONTAINS - MySQL (no jsonb)")
        void testParameterizedJsonbContainsMysql() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            JsonbColumn<TestEntity> data = new JsonbColumn<>(table, "data", "JSON");

            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    data, Operator.JSONB_CONTAINS, new Literal<>("{}", String.class)
            );
            String sql = predicate.toSql(MySqlDialect.INSTANCE, params);
            assertTrue(sql.contains("@>"));
            assertTrue(sql.contains(":p1"));
            assertFalse(sql.contains("jsonb"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized JSONB_KEY_EXISTS")
        void testParameterizedJsonbKeyExists() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            JsonbColumn<TestEntity> data = new JsonbColumn<>(table, "data", "JSONB");

            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    data, Operator.JSONB_KEY_EXISTS, new Literal<>("myKey", String.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("myKey") || sql.contains("?"));
        }

        @Test
        @DisplayName("SimplePredicate parameterized BETWEEN with non-ListLiteral")
        void testParameterizedBetweenNonListLiteral() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.AGE, Operator.BETWEEN, new Literal<>(50, Integer.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("BETWEEN ?"));
        }

        @Test
        @DisplayName("CompositePredicate parameterized OR")
        void testParameterizedCompositeOr() {
            ParameterContext params = new ParameterContext();
            Predicate left = TestUser_.AGE.lt(18);
            Predicate right = TestUser_.AGE.gt(65);
            Predicate composite = new Predicate.CompositePredicate(left, LogicalOperator.OR, right);

            String sql = composite.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("OR"));
            assertTrue(sql.contains(":p1"));
            assertTrue(sql.contains(":p2"));
        }

        @Test
        @DisplayName("JSONB_CONTAINS with Literal (extractJsonValue fallback to Literal)")
        void testJsonbContainsWithLiteralExtractJsonValue() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            JsonbColumn<TestEntity> data = new JsonbColumn<>(table, "data", "JSONB");

            ParameterContext params = new ParameterContext();
            // Use Literal instead of JsonLiteral to hit the instanceof Literal branch
            Predicate predicate = new Predicate.SimplePredicate(
                    data, Operator.JSONB_CONTAINS, new Literal<>("{\"test\":1}", String.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("@>"));
            assertEquals("{\"test\":1}", params.getParameters().get("p1"));
        }

        @Test
        @DisplayName("JSONB_CONTAINS with Column (extractJsonValue fallback to toSql)")
        void testJsonbContainsWithColumnExtractJsonValue() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            JsonbColumn<TestEntity> data = new JsonbColumn<>(table, "data", "JSONB");
            JsonbColumn<TestEntity> other = new JsonbColumn<>(table, "other", "JSONB");

            ParameterContext params = new ParameterContext();
            // Use Column expression to hit the else branch (toSql fallback)
            Predicate predicate = new Predicate.SimplePredicate(
                    data, Operator.JSONB_CONTAINS, other
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("@>"));
        }

        @Test
        @DisplayName("JSONB_KEY_EXISTS with Column (extractStringValue fallback)")
        void testJsonbKeyExistsWithColumnExtractStringValue() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            JsonbColumn<TestEntity> data = new JsonbColumn<>(table, "data", "JSONB");
            StringColumn<TestEntity> keyCol = new StringColumn<>(table, "key_col", "VARCHAR");

            ParameterContext params = new ParameterContext();
            // Use Column expression to hit the else branch
            Predicate predicate = new Predicate.SimplePredicate(
                    data, Operator.JSONB_KEY_EXISTS, keyCol
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("?"));
        }

        @Test
        @DisplayName("ARRAY_CONTAINED_BY parameterized")
        void testParameterizedArrayContainedBy() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            ArrayColumn<TestEntity, String> tags = new ArrayColumn<>(table, "tags", String.class, "TEXT[]");

            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    tags, Operator.ARRAY_CONTAINED_BY, new ArrayColumn.ArrayLiteral<>(List.of("a", "b"), String.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("<@"));
        }

        @Test
        @DisplayName("ARRAY_OVERLAP parameterized")
        void testParameterizedArrayOverlap() {
            @Entity(table = "test")
            class TestEntity {}
            Table<TestEntity> table = Table.of("test", TestEntity.class);
            ArrayColumn<TestEntity, String> tags = new ArrayColumn<>(table, "tags", String.class, "TEXT[]");

            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    tags, Operator.ARRAY_OVERLAP, new ArrayColumn.ArrayLiteral<>(List.of("x"), String.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("&&"));
        }

        @Test
        @DisplayName("GREATER_THAN parameterized (default branch)")
        void testParameterizedGreaterThan() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.AGE, Operator.GREATER_THAN, new Literal<>(21, Integer.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("> :p1"));
            assertEquals(21, params.getParameters().get("p1"));
        }

        @Test
        @DisplayName("LESS_THAN parameterized (default branch)")
        void testParameterizedLessThan() {
            ParameterContext params = new ParameterContext();
            Predicate predicate = new Predicate.SimplePredicate(
                    TestUser_.AGE, Operator.LESS_THAN, new Literal<>(65, Integer.class)
            );
            String sql = predicate.toSql(PostgreSqlDialect.INSTANCE, params);
            assertTrue(sql.contains("< :p1"));
            assertEquals(65, params.getParameters().get("p1"));
        }
    }
}
