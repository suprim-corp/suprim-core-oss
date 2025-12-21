package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OrderSpec - ORDER BY clause specifications.
 */
@DisplayName("OrderSpec Tests")
class OrderSpecTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Creates OrderSpec with column and direction")
        void testColumnAndDirection() {
            OrderSpec spec = new OrderSpec(TestUser_.EMAIL, OrderDirection.ASC);

            assertEquals(TestUser_.EMAIL, spec.column());
            assertEquals(OrderDirection.ASC, spec.direction());
            assertNull(spec.rawSql());
        }

        @Test
        @DisplayName("Creates OrderSpec with full constructor")
        void testFullConstructor() {
            OrderSpec spec = new OrderSpec(TestUser_.EMAIL, OrderDirection.DESC, null);

            assertEquals(TestUser_.EMAIL, spec.column());
            assertEquals(OrderDirection.DESC, spec.direction());
            assertNull(spec.rawSql());
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("raw() creates raw SQL OrderSpec")
        void testRaw() {
            OrderSpec spec = OrderSpec.raw("RANDOM()");

            assertNull(spec.column());
            assertNull(spec.direction());
            assertEquals("RANDOM()", spec.rawSql());
        }

        @Test
        @DisplayName("of() with Column returns column-based OrderSpec")
        void testOfWithColumn() {
            OrderSpec spec = OrderSpec.of(TestUser_.EMAIL, OrderDirection.ASC);

            assertEquals(TestUser_.EMAIL, spec.column());
            assertEquals(OrderDirection.ASC, spec.direction());
            assertNull(spec.rawSql());
        }

        @Test
        @DisplayName("of() with non-Column Expression returns raw OrderSpec")
        void testOfWithExpression() {
            Expression<?> expr = Fn.upper(TestUser_.EMAIL);
            OrderSpec spec = OrderSpec.of(expr, OrderDirection.DESC);

            assertNull(spec.column());
            assertNull(spec.direction());
            assertNotNull(spec.rawSql());
            assertTrue(spec.rawSql().contains("DESC"));
        }
    }

    @Nested
    @DisplayName("toSql Tests")
    class ToSqlTests {

        @Test
        @DisplayName("toSql() renders column with direction")
        void testToSqlColumn() {
            OrderSpec spec = new OrderSpec(TestUser_.EMAIL, OrderDirection.ASC);
            String sql = spec.toSql(PostgreSqlDialect.INSTANCE);

            assertTrue(sql.contains("email"));
            assertTrue(sql.contains("ASC"));
        }

        @Test
        @DisplayName("toSql() renders raw SQL as-is")
        void testToSqlRaw() {
            OrderSpec spec = OrderSpec.raw("created_at DESC NULLS LAST");
            String sql = spec.toSql(PostgreSqlDialect.INSTANCE);

            assertEquals("created_at DESC NULLS LAST", sql);
        }

        @Test
        @DisplayName("toSql() with DESC direction")
        void testToSqlDesc() {
            OrderSpec spec = new OrderSpec(TestUser_.AGE, OrderDirection.DESC);
            String sql = spec.toSql(PostgreSqlDialect.INSTANCE);

            assertTrue(sql.contains("DESC"));
        }
    }
}
