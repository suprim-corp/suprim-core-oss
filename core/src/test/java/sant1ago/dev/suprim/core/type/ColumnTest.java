package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Column types and operations.
 */
@DisplayName("Column Tests")
class ColumnTest {

    // ==================== BASIC COLUMN OPERATIONS ====================

    @Test
    @DisplayName("Column equals (eq)")
    void testColumnEquals() {
        Predicate predicate = TestUser_.EMAIL.eq("test@example.com");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"email\" = 'test@example.com'"));
    }

    @Test
    @DisplayName("Column not equals (ne)")
    void testColumnNotEquals() {
        Predicate predicate = TestUser_.EMAIL.ne("spam@example.com");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"email\" != 'spam@example.com'"));
    }

    @Test
    @DisplayName("Column IN list")
    void testColumnIn() {
        Predicate predicate = TestUser_.AGE.in(18, 21, 25, 30);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"age\" IN"));
        assertTrue(sql.contains("18, 21, 25, 30"));
    }

    @Test
    @DisplayName("Column NOT IN list")
    void testColumnNotIn() {
        Predicate predicate = TestUser_.AGE.notIn(1, 2, 3);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"age\" NOT IN"));
        assertTrue(sql.contains("1, 2, 3"));
    }

    @Test
    @DisplayName("Column IS NULL")
    void testColumnIsNull() {
        Predicate predicate = TestUser_.NAME.isNull();
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"name\" IS NULL"));
    }

    @Test
    @DisplayName("Column IS NOT NULL")
    void testColumnIsNotNull() {
        Predicate predicate = TestUser_.NAME.isNotNull();
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"name\" IS NOT NULL"));
    }

    @Test
    @DisplayName("Column aliasing with as()")
    void testColumnAlias() {
        AliasedColumn<?, ?> aliased = TestUser_.EMAIL.as("user_email");
        String sql = aliased.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"email\" AS user_email"));
    }

    // ==================== STRING COLUMN OPERATIONS ====================

    @Test
    @DisplayName("StringColumn LIKE")
    void testStringColumnLike() {
        Predicate predicate = TestUser_.EMAIL.like("%@example.com");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"email\" LIKE '%@example.com'"));
    }

    @Test
    @DisplayName("StringColumn ILIKE (case-insensitive)")
    void testStringColumnIlike() {
        Predicate predicate = TestUser_.EMAIL.ilike("%@EXAMPLE.COM");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"email\" ILIKE '%@EXAMPLE.COM'"));
    }

    @Test
    @DisplayName("StringColumn startsWith")
    void testStringColumnStartsWith() {
        Predicate predicate = TestUser_.NAME.startsWith("John");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"name\" LIKE 'John%'"));
    }

    @Test
    @DisplayName("StringColumn endsWith")
    void testStringColumnEndsWith() {
        Predicate predicate = TestUser_.NAME.endsWith("Doe");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"name\" LIKE '%Doe'"));
    }

    @Test
    @DisplayName("StringColumn contains")
    void testStringColumnContains() {
        Predicate predicate = TestUser_.NAME.contains("oh");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"name\" LIKE '%oh%'"));
    }

    @Test
    @DisplayName("StringColumn containsIgnoreCase")
    void testStringColumnContainsIgnoreCase() {
        Predicate predicate = TestUser_.NAME.containsIgnoreCase("JOHN");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"name\" ILIKE '%JOHN%'"));
    }

    // ==================== COMPARABLE COLUMN OPERATIONS ====================

    @Test
    @DisplayName("ComparableColumn greater than (gt)")
    void testComparableColumnGreaterThan() {
        Predicate predicate = TestUser_.AGE.gt(18);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"age\" > 18"));
    }

    @Test
    @DisplayName("ComparableColumn greater than or equals (gte)")
    void testComparableColumnGreaterThanOrEquals() {
        Predicate predicate = TestUser_.AGE.gte(21);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"age\" >= 21"));
    }

    @Test
    @DisplayName("ComparableColumn less than (lt)")
    void testComparableColumnLessThan() {
        Predicate predicate = TestUser_.AGE.lt(65);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"age\" < 65"));
    }

    @Test
    @DisplayName("ComparableColumn less than or equals (lte)")
    void testComparableColumnLessThanOrEquals() {
        Predicate predicate = TestUser_.AGE.lte(60);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"age\" <= 60"));
    }

    @Test
    @DisplayName("ComparableColumn between")
    void testComparableColumnBetween() {
        Predicate predicate = TestUser_.AGE.between(18, 65);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"age\" BETWEEN 18 AND 65"));
    }

    // ==================== ORDERING ====================

    @Test
    @DisplayName("Column ascending order")
    void testColumnAscendingOrder() {
        OrderSpec orderSpec = TestUser_.NAME.asc();
        String sql = orderSpec.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"name\" ASC"));
    }

    @Test
    @DisplayName("Column descending order")
    void testColumnDescendingOrder() {
        OrderSpec orderSpec = TestUser_.CREATED_AT.desc();
        String sql = orderSpec.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"created_at\" DESC"));
    }

    // ==================== COLUMN TO SQL ====================

    @Test
    @DisplayName("Column toSql() renders fully qualified name")
    void testColumnToSql() {
        String sql = TestUser_.EMAIL.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("users.\"email\"", sql);
    }

    @Test
    @DisplayName("Column getName() returns column name")
    void testColumnGetName() {
        assertEquals("email", TestUser_.EMAIL.getName());
        assertEquals("age", TestUser_.AGE.getName());
    }

    @Test
    @DisplayName("Column getValueType() returns correct type")
    void testColumnGetValueType() {
        assertEquals(String.class, TestUser_.EMAIL.getValueType());
        assertEquals(Integer.class, TestUser_.AGE.getValueType());
        assertEquals(Long.class, TestUser_.ID.getValueType());
    }

    @Test
    @DisplayName("Column getSqlType() returns SQL type")
    void testColumnGetSqlType() {
        assertEquals("VARCHAR(255)", TestUser_.EMAIL.getSqlType());
        assertEquals("INTEGER", TestUser_.AGE.getSqlType());
        assertEquals("BIGINT", TestUser_.ID.getSqlType());
    }

    // ==================== COLUMN COMPARISON ====================

    @Test
    @DisplayName("Column equals another column")
    void testColumnEqualsColumn() {
        // For JOIN conditions: users.id = orders.user_id
        Predicate predicate = TestUser_.ID.eq(TestUser_.ID);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"id\" = users.\"id\""));
    }

    @Test
    @DisplayName("Column not equals another column")
    void testColumnNotEqualsColumn() {
        Predicate predicate = TestUser_.ID.ne(TestUser_.ID);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"id\" != users.\"id\""));
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Column with null value in eq()")
    void testColumnEqNull() {
        Predicate predicate = TestUser_.NAME.eq((String) null);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"name\" = NULL"));
    }

    @Test
    @DisplayName("Column with empty string")
    void testColumnEqEmptyString() {
        Predicate predicate = TestUser_.NAME.eq("");
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"name\" = ''"));
    }

    @Test
    @DisplayName("Column IN empty list")
    void testColumnInEmptyList() {
        Predicate predicate = TestUser_.AGE.in(java.util.List.of());
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);

        assertTrue(sql.contains("users.\"age\" IN ()"));
    }

    // ==================== ALIASED COLUMN TESTS ====================

    @Test
    @DisplayName("AliasedColumn getValueType returns column value type")
    void testAliasedColumnGetValueType() {
        AliasedColumn<?, String> aliased = TestUser_.EMAIL.as("user_email");
        assertEquals(String.class, aliased.getValueType());
    }

    @Test
    @DisplayName("AliasedColumn record accessors")
    void testAliasedColumnRecordAccessors() {
        AliasedColumn<?, String> aliased = TestUser_.EMAIL.as("user_email");
        assertSame(TestUser_.EMAIL, aliased.column());
        assertEquals("user_email", aliased.alias());
    }

    // ==================== BASE COLUMN METHODS ====================

    @Test
    @DisplayName("Column.of() static factory method")
    void testColumnOfStaticFactory() {
        Column<?, Object> col = Column.of("custom_col", TestUser_.TABLE);
        assertNotNull(col);
        assertEquals("custom_col", col.getName());
    }

    @Test
    @DisplayName("Column getTable() returns owning table")
    void testColumnGetTable() {
        assertSame(TestUser_.TABLE, TestUser_.EMAIL.getTable());
        assertSame(TestUser_.TABLE, TestUser_.AGE.getTable());
    }

    @Test
    @DisplayName("Base Column gt/gte/lt/lte methods")
    void testBaseColumnComparisonMethods() {
        // Create a plain Column (not ComparableColumn) to test Column's own methods
        Column<?, Object> col = Column.of("score", TestUser_.TABLE);

        Predicate gtPred = col.gt(100);
        assertTrue(gtPred.toSql(PostgreSqlDialect.INSTANCE).contains("> 100"));

        Predicate gtePred = col.gte(50);
        assertTrue(gtePred.toSql(PostgreSqlDialect.INSTANCE).contains(">= 50"));

        Predicate ltPred = col.lt(200);
        assertTrue(ltPred.toSql(PostgreSqlDialect.INSTANCE).contains("< 200"));

        Predicate ltePred = col.lte(150);
        assertTrue(ltePred.toSql(PostgreSqlDialect.INSTANCE).contains("<= 150"));
    }
}
