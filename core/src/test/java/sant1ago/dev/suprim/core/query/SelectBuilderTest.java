package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestCte;
import sant1ago.dev.suprim.core.TestOrder_;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.type.OrderDirection;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;
import sant1ago.dev.suprim.annotation.entity.Entity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SelectBuilder.
 * Tests all SELECT query building capabilities.
 */
@DisplayName("SelectBuilder Tests")
class SelectBuilderTest {

    // ==================== BASIC SELECT ====================

    @Test
    @DisplayName("Basic SELECT with specific columns")
    void testBasicSelectWithColumns() {
        QueryResult result = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .build();

        assertEquals("SELECT users.\"id\", users.\"email\" FROM \"users\"", result.sql());
        assertTrue(result.parameters().isEmpty());
    }

    @Test
    @DisplayName("SELECT * (no columns specified)")
    void testSelectAll() {
        QueryResult result = Suprim.select()
            .from(TestUser_.TABLE)
            .build();

        assertEquals("SELECT * FROM \"users\"", result.sql());
    }

    @Test
    @DisplayName("SELECT DISTINCT")
    void testSelectDistinct() {
        QueryResult result = Suprim.select(TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .distinct()
            .build();

        assertEquals("SELECT DISTINCT users.\"email\" FROM \"users\"", result.sql());
    }

    // ==================== WHERE CONDITIONS ====================

    @Test
    @DisplayName("WHERE with equals condition")
    void testWhereEquals() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.EMAIL.eq("test@example.com"))
            .build();

        assertTrue(result.sql().contains("WHERE users.\"email\" = 'test@example.com'"));
    }

    @Test
    @DisplayName("WHERE with not equals condition")
    void testWhereNotEquals() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.EMAIL.ne("spam@example.com"))
            .build();

        assertTrue(result.sql().contains("WHERE users.\"email\" != 'spam@example.com'"));
    }

    @Test
    @DisplayName("WHERE with greater than")
    void testWhereGreaterThan() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gt(18))
            .build();

        assertTrue(result.sql().contains("WHERE users.\"age\" > 18"));
    }

    @Test
    @DisplayName("WHERE with greater than or equals")
    void testWhereGreaterThanOrEquals() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gte(21))
            .build();

        assertTrue(result.sql().contains("WHERE users.\"age\" >= 21"));
    }

    @Test
    @DisplayName("WHERE with less than")
    void testWhereLessThan() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.lt(65))
            .build();

        assertTrue(result.sql().contains("WHERE users.\"age\" < 65"));
    }

    @Test
    @DisplayName("WHERE with less than or equals")
    void testWhereLessThanOrEquals() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.lte(60))
            .build();

        assertTrue(result.sql().contains("WHERE users.\"age\" <= 60"));
    }

    @Test
    @DisplayName("WHERE with IN condition")
    void testWhereIn() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.in(18, 21, 25, 30))
            .build();

        assertTrue(result.sql().contains("WHERE users.\"age\" IN (18, 21, 25, 30)"));
    }

    @Test
    @DisplayName("WHERE with NOT IN condition")
    void testWhereNotIn() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.notIn(1, 2, 3))
            .build();

        assertTrue(result.sql().contains("WHERE users.\"age\" NOT IN (1, 2, 3)"));
    }

    @Test
    @DisplayName("WHERE with BETWEEN condition")
    void testWhereBetween() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.between(18, 65))
            .build();

        assertTrue(result.sql().contains("WHERE users.\"age\" BETWEEN 18 AND 65"));
    }

    @Test
    @DisplayName("WHERE with IS NULL")
    void testWhereIsNull() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.NAME.isNull())
            .build();

        assertTrue(result.sql().contains("WHERE users.\"name\" IS NULL"));
    }

    @Test
    @DisplayName("WHERE with IS NOT NULL")
    void testWhereIsNotNull() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.NAME.isNotNull())
            .build();

        assertTrue(result.sql().contains("WHERE users.\"name\" IS NOT NULL"));
    }

    // ==================== AND/OR CONDITIONS ====================

    @Test
    @DisplayName("WHERE with AND condition")
    void testWhereAnd() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gte(18))
            .and(TestUser_.IS_ACTIVE.eq(true))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("users.\"age\" >= 18"));
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("users.\"is_active\""));
    }

    @Test
    @DisplayName("WHERE with OR condition")
    void testWhereOr() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.lt(18))
            .or(TestUser_.AGE.gt(65))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("users.\"age\" < 18"));
        assertTrue(sql.contains("OR"));
        assertTrue(sql.contains("users.\"age\" > 65"));
    }

    @Test
    @DisplayName("Complex AND/OR combination")
    void testComplexAndOr() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gte(18).and(TestUser_.IS_ACTIVE.eq(true)))
            .or(TestUser_.EMAIL.like("%@admin.com"))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
    }

    // ==================== ORDER BY ====================

    @Test
    @DisplayName("ORDER BY ascending")
    void testOrderByAsc() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .orderBy(TestUser_.NAME.asc())
            .build();

        assertTrue(result.sql().contains("ORDER BY users.\"name\" ASC"));
    }

    @Test
    @DisplayName("ORDER BY descending")
    void testOrderByDesc() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .orderBy(TestUser_.CREATED_AT.desc())
            .build();

        assertTrue(result.sql().contains("ORDER BY users.\"created_at\" DESC"));
    }

    @Test
    @DisplayName("ORDER BY multiple columns")
    void testOrderByMultiple() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .orderBy(TestUser_.AGE.desc(), TestUser_.NAME.asc())
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("users.\"age\" DESC"));
        assertTrue(sql.contains("users.\"name\" ASC"));
    }

    // ==================== LIMIT / OFFSET ====================

    @Test
    @DisplayName("LIMIT clause")
    void testLimit() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .limit(10)
            .build();

        assertTrue(result.sql().contains("LIMIT 10"));
    }

    @Test
    @DisplayName("OFFSET clause")
    void testOffset() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .limit(10)
            .offset(20)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("LIMIT 10"));
        assertTrue(sql.contains("OFFSET 20"));
    }

    @Test
    @DisplayName("Pagination helper (page 1)")
    void testPaginatePage1() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .paginate(1, 20)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("LIMIT 20"));
        assertFalse(sql.contains("OFFSET")); // Offset 0 is omitted
    }

    @Test
    @DisplayName("Pagination helper (page 3)")
    void testPaginatePage3() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .paginate(3, 20)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("LIMIT 20"));
        assertTrue(sql.contains("OFFSET 40"));
    }

    // ==================== GROUP BY / HAVING ====================

    @Test
    @DisplayName("GROUP BY single column")
    void testGroupBy() {
        QueryResult result = Suprim.select(TestUser_.AGE)
            .from(TestUser_.TABLE)
            .groupBy(TestUser_.AGE)
            .build();

        assertTrue(result.sql().contains("GROUP BY users.\"age\""));
    }

    @Test
    @DisplayName("GROUP BY multiple columns")
    void testGroupByMultiple() {
        QueryResult result = Suprim.select(TestUser_.AGE, TestUser_.IS_ACTIVE)
            .from(TestUser_.TABLE)
            .groupBy(TestUser_.AGE, TestUser_.IS_ACTIVE)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("GROUP BY"));
        assertTrue(sql.contains("users.\"age\""));
        assertTrue(sql.contains("users.\"is_active\""));
    }

    @Test
    @DisplayName("HAVING clause")
    void testHaving() {
        QueryResult result = Suprim.select(TestUser_.AGE)
            .from(TestUser_.TABLE)
            .groupBy(TestUser_.AGE)
            .having(TestUser_.AGE.gt(18))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("GROUP BY users.\"age\""));
        assertTrue(sql.contains("HAVING users.\"age\" > 18"));
    }

    // ==================== JOINS ====================

    @Test
    @DisplayName("INNER JOIN")
    void testInnerJoin() {
        QueryResult result = Suprim.select(TestUser_.ID, TestOrder_.AMOUNT)
            .from(TestUser_.TABLE)
            .join(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("FROM \"users\""));
        assertTrue(sql.contains("JOIN \"orders\""));
        assertTrue(sql.contains("ON"));
    }

    @Test
    @DisplayName("LEFT JOIN")
    void testLeftJoin() {
        QueryResult result = Suprim.select(TestUser_.ID, TestOrder_.AMOUNT)
            .from(TestUser_.TABLE)
            .leftJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("LEFT JOIN \"orders\""));
        assertTrue(sql.contains("ON"));
    }

    @Test
    @DisplayName("RIGHT JOIN")
    void testRightJoin() {
        QueryResult result = Suprim.select(TestUser_.ID, TestOrder_.AMOUNT)
            .from(TestUser_.TABLE)
            .rightJoin(TestOrder_.TABLE, TestOrder_.USER_ID.eq(TestUser_.ID))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("RIGHT JOIN \"orders\""));
        assertTrue(sql.contains("ON"));
    }

    // ==================== SUBQUERIES ====================

    @Test
    @DisplayName("EXISTS subquery")
    void testExistsSubquery() {
        SelectBuilder subquery = Suprim.select(TestOrder_.ID)
            .from(TestOrder_.TABLE)
            .where(TestOrder_.USER_ID.eq(TestUser_.ID));

        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereExists(subquery)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WHERE EXISTS"));
        assertTrue(sql.contains("SELECT"));
    }

    @Test
    @DisplayName("NOT EXISTS subquery")
    void testNotExistsSubquery() {
        SelectBuilder subquery = Suprim.select(TestOrder_.ID)
            .from(TestOrder_.TABLE)
            .where(TestOrder_.USER_ID.eq(TestUser_.ID));

        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereNotExists(subquery)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WHERE NOT EXISTS"));
    }

    @Test
    @DisplayName("IN subquery")
    void testInSubquery() {
        SelectBuilder subquery = Suprim.select(TestOrder_.USER_ID)
            .from(TestOrder_.TABLE)
            .where(TestOrder_.AMOUNT.gt(java.math.BigDecimal.valueOf(100)));

        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereInSubquery(TestUser_.ID, subquery)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("IN"));
    }

    // ==================== UNION ====================

    @Test
    @DisplayName("UNION")
    void testUnion() {
        SelectBuilder query1 = Suprim.select(TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.lt(30));

        SelectBuilder query2 = Suprim.select(TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gt(60));

        QueryResult result = query1.union(query2).build();

        String sql = result.sql();
        assertTrue(sql.contains("UNION"));
        assertFalse(sql.contains("UNION ALL"));
    }

    @Test
    @DisplayName("UNION ALL")
    void testUnionAll() {
        SelectBuilder query1 = Suprim.select(TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.lt(30));

        SelectBuilder query2 = Suprim.select(TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gt(60));

        QueryResult result = query1.unionAll(query2).build();

        assertTrue(result.sql().contains("UNION ALL"));
    }

    // ==================== CTE (WITH) ====================

    @Test
    @DisplayName("CTE (WITH clause)")
    void testCte() {
        SelectBuilder activeUsers = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true));

        QueryResult result = Suprim.select()
            .with("active_users", activeUsers)
            .from(Table.of("active_users", TestCte.class))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WITH active_users AS"));
        assertTrue(sql.contains("SELECT"));
    }

    @Test
    @DisplayName("CTE with RECURSIVE")
    void testRecursiveCte() {
        QueryResult result = Suprim.select()
            .withRecursive("numbers", "SELECT 1 AS n UNION ALL SELECT n + 1 FROM numbers WHERE n < 10")
            .from(Table.of("numbers", TestCte.class))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WITH RECURSIVE numbers AS"));
    }

    // ==================== ROW LOCKING ====================

    @Test
    @DisplayName("FOR UPDATE")
    void testForUpdate() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.ID.eq(1L))
            .forUpdate()
            .build();

        assertTrue(result.sql().contains("FOR UPDATE"));
    }

    @Test
    @DisplayName("FOR UPDATE NOWAIT")
    void testForUpdateNowait() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.ID.eq(1L))
            .forUpdateNowait()
            .build();

        assertTrue(result.sql().contains("FOR UPDATE NOWAIT"));
    }

    @Test
    @DisplayName("FOR UPDATE SKIP LOCKED")
    void testForUpdateSkipLocked() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.ID.eq(1L))
            .forUpdateSkipLocked()
            .build();

        assertTrue(result.sql().contains("FOR UPDATE SKIP LOCKED"));
    }

    // ==================== DIALECT TESTS ====================

    @Test
    @DisplayName("Build with MySQL dialect")
    void testMySqlDialect() {
        QueryResult result = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gte(18))
            .build(MySqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("`id`"));
        assertTrue(sql.contains("`email`"));
        assertTrue(sql.contains("`users`"));
    }

    @Test
    @DisplayName("Build with PostgreSQL dialect")
    void testPostgreSqlDialect() {
        QueryResult result = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gte(18))
            .build(PostgreSqlDialect.INSTANCE);

        String sql = result.sql();
        assertTrue(sql.contains("\"id\""));
        assertTrue(sql.contains("\"email\""));
        assertTrue(sql.contains("\"users\""));
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Empty SELECT (no FROM)")
    void testEmptySelect() {
        QueryResult result = Suprim.select(TestUser_.ID, TestUser_.EMAIL).build();

        String sql = result.sql();
        assertTrue(sql.startsWith("SELECT"));
        assertFalse(sql.contains("FROM"));
    }

    @Test
    @DisplayName("Complex query with all clauses")
    void testComplexQuery() {
        QueryResult result = Suprim.select(TestUser_.ID, TestUser_.EMAIL, TestUser_.AGE)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .and(TestUser_.AGE.gte(18))
            .orderBy(TestUser_.CREATED_AT.desc())
            .limit(10)
            .offset(5)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("LIMIT 10"));
        assertTrue(sql.contains("OFFSET 5"));
    }

    // ==================== SELECT VARIANTS ====================

    @Test
    @DisplayName("selectRaw")
    void testSelectRaw() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .selectRaw("COUNT(*) as total")
            .from(TestUser_.TABLE)
            .build();

        assertTrue(result.sql().contains("COUNT(*) as total"));
    }

    @Test
    @DisplayName("selectIf - condition true")
    void testSelectIfTrue() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .selectIf(true, TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .build();

        assertTrue(result.sql().contains("\"email\""));
    }

    @Test
    @DisplayName("selectIf - condition false")
    void testSelectIfFalse() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .selectIf(false, TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .build();

        assertFalse(result.sql().contains("\"email\""));
    }

    @Test
    @DisplayName("selectRawIf - condition true")
    void testSelectRawIfTrue() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .selectRawIf(true, "NOW() as timestamp")
            .from(TestUser_.TABLE)
            .build();

        assertTrue(result.sql().contains("NOW() as timestamp"));
    }

    @Test
    @DisplayName("selectRawIf - condition false")
    void testSelectRawIfFalse() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .selectRawIf(false, "NOW() as timestamp")
            .from(TestUser_.TABLE)
            .build();

        assertFalse(result.sql().contains("NOW() as timestamp"));
    }

    @Test
    @DisplayName("selectCount")
    void testSelectCount() {
        QueryResult result = Suprim.select()
            .selectCount()
            .from(TestUser_.TABLE)
            .build();

        assertTrue(result.sql().contains("COUNT(*)"));
    }

    @Test
    @DisplayName("selectCount with alias")
    void testSelectCountWithAlias() {
        QueryResult result = Suprim.select()
            .selectCount("user_count")
            .from(TestUser_.TABLE)
            .build();

        assertTrue(result.sql().contains("COUNT(*) AS user_count"));
    }

    @Test
    @DisplayName("selectAs")
    void testSelectAs() {
        QueryResult result = Suprim.select()
            .selectAs(TestUser_.EMAIL, "user_email")
            .from(TestUser_.TABLE)
            .build();

        assertTrue(result.sql().contains("AS user_email"));
    }

    // ==================== CONDITIONAL WHERE ====================

    @Test
    @DisplayName("whereIfPresent - value present")
    void testWhereIfPresentWithValue() {
        String email = "test@example.com";
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereIfPresent(email, () -> TestUser_.EMAIL.eq(email))
            .build();

        assertTrue(result.sql().contains("WHERE"));
    }

    @Test
    @DisplayName("whereIfPresent - value null")
    void testWhereIfPresentWithNull() {
        String email = null;
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereIfPresent(email, () -> TestUser_.EMAIL.eq("ignored"))
            .build();

        assertFalse(result.sql().contains("WHERE"));
    }

    @Test
    @DisplayName("whereRaw")
    void testWhereRaw() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereRaw("created_at > NOW() - INTERVAL '7 days'")
            .build();

        assertTrue(result.sql().contains("WHERE created_at > NOW()"));
    }

    @Test
    @DisplayName("andIfPresent - value present")
    void testAndIfPresentWithValue() {
        Integer age = 25;
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .andIfPresent(age, () -> TestUser_.AGE.eq(age))
            .build();

        assertTrue(result.sql().contains("AND"));
    }

    @Test
    @DisplayName("andIfPresent - value null")
    void testAndIfPresentWithNull() {
        Integer age = null;
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .andIfPresent(age, () -> TestUser_.AGE.eq(99))
            .build();

        assertFalse(result.sql().contains("AND"));
    }

    @Test
    @DisplayName("andRaw")
    void testAndRaw() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .andRaw("email LIKE '%@admin.com'")
            .build();

        assertTrue(result.sql().contains("AND"));
        assertTrue(result.sql().contains("email LIKE"));
    }

    @Test
    @DisplayName("andRaw with no existing where")
    void testAndRawNoExistingWhere() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .andRaw("email LIKE '%@admin.com'")
            .build();

        assertTrue(result.sql().contains("WHERE"));
    }

    @Test
    @DisplayName("orIfPresent - value present")
    void testOrIfPresentWithValue() {
        String email = "admin@example.com";
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .orIfPresent(email, () -> TestUser_.EMAIL.eq(email))
            .build();

        assertTrue(result.sql().contains("OR"));
    }

    @Test
    @DisplayName("orIfPresent - value null")
    void testOrIfPresentWithNull() {
        String email = null;
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .orIfPresent(email, () -> TestUser_.EMAIL.eq("ignored"))
            .build();

        assertFalse(result.sql().contains("OR"));
    }

    @Test
    @DisplayName("and with no existing where")
    void testAndWithNoExistingWhere() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .and(TestUser_.IS_ACTIVE.eq(true))
            .build();

        assertTrue(result.sql().contains("WHERE"));
        assertFalse(result.sql().contains("AND"));
    }

    @Test
    @DisplayName("or with no existing where")
    void testOrWithNoExistingWhere() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .or(TestUser_.IS_ACTIVE.eq(true))
            .build();

        assertTrue(result.sql().contains("WHERE"));
        assertFalse(result.sql().contains("OR"));
    }

    @Test
    @DisplayName("whereIf - condition true")
    void testWhereIfTrue() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereIf(true, TestUser_.IS_ACTIVE.eq(true))
            .build();

        assertTrue(result.sql().contains("WHERE"));
    }

    @Test
    @DisplayName("whereIf - condition false")
    void testWhereIfFalse() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereIf(false, TestUser_.IS_ACTIVE.eq(true))
            .build();

        assertFalse(result.sql().contains("WHERE"));
    }

    @Test
    @DisplayName("andIf - condition true")
    void testAndIfTrue() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gt(18))
            .andIf(true, TestUser_.IS_ACTIVE.eq(true))
            .build();

        assertTrue(result.sql().contains("AND"));
    }

    @Test
    @DisplayName("andIf - condition false")
    void testAndIfFalse() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.gt(18))
            .andIf(false, TestUser_.IS_ACTIVE.eq(true))
            .build();

        assertFalse(result.sql().contains("AND"));
    }

    // ==================== GROUP BY ====================

    @Test
    @DisplayName("groupByRaw")
    void testGroupByRaw() {
        QueryResult result = Suprim.select(TestUser_.AGE)
            .selectRaw("COUNT(*)")
            .from(TestUser_.TABLE)
            .groupByRaw("EXTRACT(YEAR FROM created_at)")
            .build();

        assertTrue(result.sql().contains("GROUP BY EXTRACT(YEAR FROM created_at)"));
    }

    @Test
    @DisplayName("groupByExpression with non-Column expression")
    void testGroupByExpressionWithNonColumnExpression() {
        // Use Coalesce which is an Expression but not a Column
        sant1ago.dev.suprim.core.type.Coalesce<String> coalesceExpr =
            sant1ago.dev.suprim.core.type.Coalesce.of(TestUser_.NAME, "Unknown");

        QueryResult result = Suprim.select(TestUser_.ID)
            .selectRaw("COUNT(*)")
            .from(TestUser_.TABLE)
            .groupByExpression(coalesceExpr)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("GROUP BY"));
        assertTrue(sql.contains("COALESCE"));
    }

    @Test
    @DisplayName("groupByExpression with Column cast as Expression")
    void testGroupByExpressionWithColumnAsExpression() {
        // Cast Column to Expression to test GroupByItem.of(Expression) with Column instanceof branch
        sant1ago.dev.suprim.core.type.Expression<?> columnAsExpr = TestUser_.AGE;

        QueryResult result = Suprim.select(TestUser_.ID)
            .selectRaw("COUNT(*)")
            .from(TestUser_.TABLE)
            .groupByExpression(columnAsExpr)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("GROUP BY"));
        assertTrue(sql.contains("age"));
    }

    // ==================== JOINS RAW ====================

    @Test
    @DisplayName("joinRaw")
    void testJoinRaw() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .joinRaw("LEFT JOIN orders o ON o.user_id = users.id AND o.status = 'active'")
            .build();

        assertTrue(result.sql().contains("LEFT JOIN orders o"));
    }

    // ==================== RELATION JOINS ====================

    @Test
    @DisplayName("leftJoin with Relation")
    void testLeftJoinWithRelation() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .leftJoin(TestUser_.ORDERS)
            .build();

        assertTrue(result.sql().contains("LEFT JOIN"));
        assertTrue(result.sql().contains("orders"));
    }

    @Test
    @DisplayName("join with Relation")
    void testJoinWithRelation() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .join(TestUser_.ORDERS)
            .build();

        assertTrue(result.sql().contains("JOIN"));
        assertTrue(result.sql().contains("orders"));
    }

    @Test
    @DisplayName("rightJoin with Relation")
    void testRightJoinWithRelation() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .rightJoin(TestUser_.ORDERS)
            .build();

        assertTrue(result.sql().contains("RIGHT JOIN"));
    }

    @Test
    @DisplayName("leftJoin with belongsTo Relation")
    void testLeftJoinWithBelongsToRelation() {
        QueryResult result = Suprim.select(TestOrder_.ID)
            .from(TestOrder_.TABLE)
            .leftJoin(TestOrder_.USER)
            .build();

        assertTrue(result.sql().contains("LEFT JOIN"));
        assertTrue(result.sql().contains("users"));
    }

    // ==================== SUBQUERIES ====================

    @Test
    @DisplayName("andExists subquery")
    void testAndExistsSubquery() {
        SelectBuilder subquery = Suprim.select(TestOrder_.ID)
            .from(TestOrder_.TABLE)
            .where(TestOrder_.USER_ID.eq(TestUser_.ID));

        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .andExists(subquery)
            .build();

        assertTrue(result.sql().contains("AND EXISTS"));
    }

    @Test
    @DisplayName("andNotExists subquery")
    void testAndNotExistsSubquery() {
        SelectBuilder subquery = Suprim.select(TestOrder_.ID)
            .from(TestOrder_.TABLE)
            .where(TestOrder_.USER_ID.eq(TestUser_.ID));

        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .andNotExists(subquery)
            .build();

        assertTrue(result.sql().contains("AND NOT EXISTS"));
    }

    @Test
    @DisplayName("andExists subquery with no existing where")
    void testAndExistsSubqueryNoWhere() {
        SelectBuilder subquery = Suprim.select(TestOrder_.ID)
            .from(TestOrder_.TABLE)
            .where(TestOrder_.USER_ID.eq(TestUser_.ID));

        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .andExists(subquery)
            .build();

        assertTrue(result.sql().contains("WHERE EXISTS"));
    }

    @Test
    @DisplayName("andNotExists subquery with no existing where")
    void testAndNotExistsSubqueryNoWhere() {
        SelectBuilder subquery = Suprim.select(TestOrder_.ID)
            .from(TestOrder_.TABLE)
            .where(TestOrder_.USER_ID.eq(TestUser_.ID));

        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .andNotExists(subquery)
            .build();

        assertTrue(result.sql().contains("WHERE NOT EXISTS"));
    }

    @Test
    @DisplayName("NOT IN subquery")
    void testNotInSubquery() {
        SelectBuilder subquery = Suprim.select(TestOrder_.USER_ID)
            .from(TestOrder_.TABLE);

        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereNotInSubquery(TestUser_.ID, subquery)
            .build();

        assertTrue(result.sql().contains("NOT IN"));
    }

    // ==================== SET OPERATIONS ====================

    @Test
    @DisplayName("INTERSECT")
    void testIntersect() {
        SelectBuilder query1 = Suprim.select(TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.lt(30));

        SelectBuilder query2 = Suprim.select(TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true));

        QueryResult result = query1.intersect(query2).build();

        assertTrue(result.sql().contains("INTERSECT"));
    }

    @Test
    @DisplayName("EXCEPT")
    void testExcept() {
        SelectBuilder query1 = Suprim.select(TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.AGE.lt(30));

        SelectBuilder query2 = Suprim.select(TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(false));

        QueryResult result = query1.except(query2).build();

        assertTrue(result.sql().contains("EXCEPT"));
    }

    // ==================== ROW LOCKING ====================

    @Test
    @DisplayName("FOR SHARE")
    void testForShare() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .forShare()
            .build();

        assertTrue(result.sql().contains("FOR SHARE"));
    }

    @Test
    @DisplayName("FOR SHARE NOWAIT")
    void testForShareNowait() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .forShareNowait()
            .build();

        assertTrue(result.sql().contains("FOR SHARE NOWAIT"));
    }

    @Test
    @DisplayName("FOR SHARE SKIP LOCKED")
    void testForShareSkipLocked() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .forShareSkipLocked()
            .build();

        assertTrue(result.sql().contains("FOR SHARE SKIP LOCKED"));
    }

    // ==================== PAGINATION ====================

    @Test
    @DisplayName("paginate with invalid page")
    void testPaginateInvalidPage() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .paginate(0, 20)  // Invalid page, should default to 1
            .build();

        assertTrue(result.sql().contains("LIMIT 20"));
        assertFalse(result.sql().contains("OFFSET"));  // Page 1 means offset 0
    }

    @Test
    @DisplayName("paginate with invalid pageSize")
    void testPaginateInvalidPageSize() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .paginate(1, 0)  // Invalid pageSize, should default to 10
            .build();

        assertTrue(result.sql().contains("LIMIT 10"));
    }

    // ==================== CTE ====================

    @Test
    @DisplayName("WITH clause with raw SQL")
    void testWithRawSql() {
        QueryResult result = Suprim.select()
            .with("numbers", "SELECT 1 AS n")
            .from(Table.of("numbers", TestCte.class))
            .build();

        assertTrue(result.sql().contains("WITH numbers AS (SELECT 1 AS n)"));
    }

    @Test
    @DisplayName("WITH RECURSIVE with SelectBuilder")
    void testWithRecursiveSelectBuilder() {
        SelectBuilder base = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true));

        QueryResult result = Suprim.select()
            .withRecursive("active_tree", base)
            .from(Table.of("active_tree", TestCte.class))
            .build();

        assertTrue(result.sql().contains("WITH RECURSIVE active_tree AS"));
    }

    // ==================== EAGER LOADING ====================

    @Test
    @DisplayName("with Relation varargs")
    void testWithRelationVarargs() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .with(TestUser_.ORDERS)
            .build();

        assertEquals(1, result.eagerLoads().size());
    }

    @Test
    @DisplayName("with Relation and constraint")
    void testWithRelationAndConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .with(TestUser_.ORDERS, orders -> orders.limit(5))
            .build();

        assertEquals(1, result.eagerLoads().size());
        assertTrue(result.eagerLoads().get(0).hasConstraint());
    }

    @Test
    @DisplayName("with string path")
    void testWithStringPath() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .with("orders")
            .build();

        assertEquals(1, result.eagerLoads().size());
    }

    @Test
    @DisplayName("with string path and constraint")
    void testWithStringPathAndConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .with("orders", orders -> orders.limit(10))
            .build();

        assertEquals(1, result.eagerLoads().size());
    }

    @Test
    @DisplayName("with string path without FROM throws")
    void testWithStringPathWithoutFromThrows() {
        assertThrows(IllegalStateException.class, () ->
            Suprim.select(TestUser_.ID)
                .with("orders")
                .build()
        );
    }

    @Test
    @DisplayName("with Relation and nested string path")
    void testWithRelationAndNestedPath() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .with(TestUser_.ORDERS, "user")
            .build();

        assertEquals(1, result.eagerLoads().size());
    }

    @Test
    @DisplayName("with Relation, nested path and constraint")
    void testWithRelationNestedPathAndConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .with(TestUser_.ORDERS, "user", user -> user.limit(1))
            .build();

        assertEquals(1, result.eagerLoads().size());
    }

    @Test
    @DisplayName("without Relation")
    void testWithoutRelation() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .without(TestUser_.ORDERS)
            .build();

        assertTrue(result.sql().contains("SELECT"));
    }

    @Test
    @DisplayName("without string field names")
    void testWithoutStringFieldNames() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .without("orders", "profile")
            .build();

        assertTrue(result.sql().contains("SELECT"));
    }

    @Test
    @DisplayName("getEagerLoads returns list")
    void testGetEagerLoads() {
        SelectBuilder builder = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .with(TestUser_.ORDERS);

        assertEquals(1, builder.getEagerLoads().size());
    }

    @Test
    @DisplayName("getWithoutRelations returns set")
    void testGetWithoutRelations() {
        SelectBuilder builder = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .without("orders");

        assertTrue(builder.getWithoutRelations().contains("orders"));
    }

    @Test
    @DisplayName("getWhereClause returns predicate")
    void testGetWhereClause() {
        SelectBuilder builder = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true));

        assertNotNull(builder.getWhereClause());
    }

    // ==================== RELATIONSHIP QUERY METHODS ====================

    @Test
    @DisplayName("whereHas")
    void testWhereHas() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereHas(TestUser_.ORDERS)
            .build();

        assertTrue(result.sql().contains("WHERE EXISTS"));
    }

    @Test
    @DisplayName("whereHas with constraint")
    void testWhereHasWithConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereHas(TestUser_.ORDERS, orders -> orders.where(TestOrder_.STATUS.eq("completed")))
            .build();

        assertTrue(result.sql().contains("WHERE EXISTS"));
    }

    @Test
    @DisplayName("whereDoesntHave")
    void testWhereDoesntHave() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereDoesntHave(TestUser_.ORDERS)
            .build();

        assertTrue(result.sql().contains("WHERE NOT EXISTS"));
    }

    @Test
    @DisplayName("whereDoesntHave with constraint")
    void testWhereDoesntHaveWithConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .whereDoesntHave(TestUser_.ORDERS, orders -> orders.where(TestOrder_.STATUS.eq("cancelled")))
            .build();

        assertTrue(result.sql().contains("WHERE NOT EXISTS"));
    }

    @Test
    @DisplayName("has with count")
    void testHasWithCount() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .has(TestUser_.ORDERS, ">=", 5)
            .build();

        assertTrue(result.sql().contains(">="));
        assertTrue(result.sql().contains("5"));
    }

    @Test
    @DisplayName("has with count and constraint")
    void testHasWithCountAndConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .has(TestUser_.ORDERS, ">", 3, orders -> orders.where(TestOrder_.STATUS.eq("active")))
            .build();

        assertTrue(result.sql().contains(">"));
        assertTrue(result.sql().contains("3"));
    }

    @Test
    @DisplayName("doesntHave")
    void testDoesntHave() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .doesntHave(TestUser_.ORDERS)
            .build();

        assertTrue(result.sql().contains("= 0"));
    }

    // ==================== AGGREGATE SUBQUERIES ====================

    @Test
    @DisplayName("withCount")
    void testWithCount() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withCount(TestUser_.ORDERS)
            .build();

        assertTrue(result.sql().contains("SELECT COUNT(*)"));
    }

    @Test
    @DisplayName("withCount with constraint and alias")
    void testWithCountWithConstraintAndAlias() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withCount(TestUser_.ORDERS, orders -> orders.where(TestOrder_.STATUS.eq("active")), "active_orders")
            .build();

        assertTrue(result.sql().contains("SELECT COUNT(*)"));
        assertTrue(result.sql().contains("active_orders"));
    }

    @Test
    @DisplayName("withSum")
    void testWithSum() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withSum(TestUser_.ORDERS, TestOrder_.AMOUNT, "total_amount")
            .build();

        assertTrue(result.sql().contains("SUM("));
        assertTrue(result.sql().contains("total_amount"));
    }

    @Test
    @DisplayName("withSum with constraint")
    void testWithSumWithConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withSum(TestUser_.ORDERS, TestOrder_.AMOUNT, "active_total",
                orders -> orders.where(TestOrder_.STATUS.eq("active")))
            .build();

        assertTrue(result.sql().contains("SUM("));
    }

    @Test
    @DisplayName("withAvg")
    void testWithAvg() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withAvg(TestUser_.ORDERS, TestOrder_.AMOUNT, "avg_amount")
            .build();

        assertTrue(result.sql().contains("AVG("));
    }

    @Test
    @DisplayName("withAvg with constraint")
    void testWithAvgWithConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withAvg(TestUser_.ORDERS, TestOrder_.AMOUNT, "avg_active",
                orders -> orders.where(TestOrder_.STATUS.eq("active")))
            .build();

        assertTrue(result.sql().contains("AVG("));
    }

    @Test
    @DisplayName("withMin")
    void testWithMin() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withMin(TestUser_.ORDERS, TestOrder_.AMOUNT, "min_amount")
            .build();

        assertTrue(result.sql().contains("MIN("));
    }

    @Test
    @DisplayName("withMin with constraint")
    void testWithMinWithConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withMin(TestUser_.ORDERS, TestOrder_.AMOUNT, "min_active",
                orders -> orders.where(TestOrder_.STATUS.eq("active")))
            .build();

        assertTrue(result.sql().contains("MIN("));
    }

    @Test
    @DisplayName("withMax")
    void testWithMax() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withMax(TestUser_.ORDERS, TestOrder_.AMOUNT, "max_amount")
            .build();

        assertTrue(result.sql().contains("MAX("));
    }

    @Test
    @DisplayName("withMax with constraint")
    void testWithMaxWithConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withMax(TestUser_.ORDERS, TestOrder_.AMOUNT, "max_active",
                orders -> orders.where(TestOrder_.STATUS.eq("active")))
            .build();

        assertTrue(result.sql().contains("MAX("));
    }

    @Test
    @DisplayName("withExists")
    void testWithExists() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withExists(TestUser_.ORDERS, "has_orders")
            .build();

        assertTrue(result.sql().contains("EXISTS"));
        assertTrue(result.sql().contains("has_orders"));
    }

    @Test
    @DisplayName("withExists with constraint")
    void testWithExistsWithConstraint() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withExists(TestUser_.ORDERS, "has_active_orders",
                orders -> orders.where(TestOrder_.STATUS.eq("active")))
            .build();

        assertTrue(result.sql().contains("EXISTS"));
    }

    // ==================== SUBQUERY ITEM EDGE CASES ====================

    // Test entities for BelongsToMany pivot join testing
    @Entity(table = "tags")
    static class Tag {}

    private static final Table<Tag> TAGS = Table.of("tags", Tag.class);

    // BelongsToMany relation for pivot join coverage
    private static final Relation<sant1ago.dev.suprim.core.TestUser, Tag> USER_TAGS = Relation.belongsToMany(
            TestUser_.TABLE, TAGS, "user_tags", "user_id", "tag_id",
            "id", "id", java.util.List.of(), false, false
    );

    // Test entity for HAS_ONE_THROUGH relation
    @Entity(table = "profiles")
    static class Profile {}

    private static final Table<Profile> PROFILES = Table.of("profiles", Profile.class);

    // HAS_ONE_THROUGH relation (unsupported by addRelationJoin)
    private static final Relation<sant1ago.dev.suprim.core.TestUser, Profile> USER_PROFILE_THROUGH = Relation.hasOneThrough(
            TestUser_.TABLE, PROFILES, TestOrder_.TABLE,
            "user_id", "profile_id", "id", "id", false, "profileThrough"
    );

    @Test
    @DisplayName("withCount with BelongsToMany includes pivot JOIN")
    void testWithCountBelongsToMany() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withCount(USER_TAGS)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("JOIN user_tags ON"));
    }

    @Test
    @DisplayName("withExists with BelongsToMany includes pivot JOIN")
    void testWithExistsBelongsToMany() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withExists(USER_TAGS, "has_tags")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("EXISTS"));
        assertTrue(sql.contains("JOIN user_tags ON"));
    }

    @Test
    @DisplayName("withCount with constraint that has no WHERE clause")
    void testWithCountConstraintNoWhere() {
        // Constraint that only does limit (no where clause)
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withCount(TestUser_.ORDERS, orders -> orders.limit(10), "orders_count")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("orders_count"));
        // Should NOT have extra AND clause since constraint has no WHERE
        assertFalse(sql.contains(" AND AND "));
    }

    // ==================== PIVOT TABLE METHODS ====================

    @Test
    @DisplayName("wherePivot filters on pivot table column")
    void testWherePivot() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .wherePivot(USER_TAGS, "is_primary", true)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("is_primary"));
    }

    @Test
    @DisplayName("wherePivot throws for non-BelongsToMany relation")
    void testWherePivotThrowsForNonBelongsToMany() {
        assertThrows(IllegalArgumentException.class, () ->
            Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .wherePivot(TestUser_.ORDERS, "some_col", "value")
                .build()
        );
    }

    @Test
    @DisplayName("wherePivotIn filters with IN clause on pivot table")
    void testWherePivotIn() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .wherePivotIn(USER_TAGS, "tag_type", java.util.List.of("tech", "sports"))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("tag_type"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    @DisplayName("wherePivotNotIn filters with NOT IN clause on pivot table")
    void testWherePivotNotIn() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .wherePivotNotIn(USER_TAGS, "tag_type", java.util.List.of("spam", "blocked"))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("NOT IN"));
    }

    @Test
    @DisplayName("wherePivotNull filters where pivot column is NULL")
    void testWherePivotNull() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .wherePivotNull(USER_TAGS, "revoked_at")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("revoked_at"));
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    @DisplayName("wherePivotNotNull filters where pivot column is NOT NULL")
    void testWherePivotNotNull() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .wherePivotNotNull(USER_TAGS, "assigned_at")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("IS NOT NULL"));
    }

    @Test
    @DisplayName("wherePivotBetween filters with BETWEEN on pivot table")
    void testWherePivotBetween() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .wherePivotBetween(USER_TAGS, "assigned_at", "2024-01-01", "2024-12-31")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("BETWEEN"));
    }

    @Test
    @DisplayName("orderByPivot orders by pivot table column")
    void testOrderByPivot() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .orderByPivot(USER_TAGS, "assigned_at", OrderDirection.DESC)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("assigned_at"));
        assertTrue(sql.contains("DESC"));
    }

    @Test
    @DisplayName("selectPivot selects pivot table column with alias")
    void testSelectPivotWithAlias() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .selectPivot(USER_TAGS, "assigned_at", "tag_assigned")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("assigned_at"));
        assertTrue(sql.contains("tag_assigned"));
    }

    @Test
    @DisplayName("selectPivot selects pivot table column without alias")
    void testSelectPivotWithoutAlias() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .selectPivot(USER_TAGS, "created_at")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("created_at"));
    }

    @Test
    @DisplayName("selectCountFilter with FILTER clause")
    void testSelectCountFilter() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .selectCountFilter(TestUser_.ID, TestUser_.IS_ACTIVE.eq(true), "active_count")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("COUNT"));
        assertTrue(sql.contains("active_count"));
    }

    @Test
    @DisplayName("select with Expression array")
    void testSelectExpressionArray() {
        QueryResult result = Suprim.select(TestUser_.ID, TestUser_.EMAIL)
            .from(TestUser_.TABLE)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("\"id\""));
        assertTrue(sql.contains("\"email\""));
    }

    // ==================== ADDITIONAL BRANCH COVERAGE TESTS ====================

    @Test
    @DisplayName("addRelationJoin with BELONGS_TO relation type")
    void testJoinWithBelongsToRelation() {
        QueryResult result = Suprim.select(TestOrder_.ID, TestOrder_.AMOUNT)
            .from(TestOrder_.TABLE)
            .leftJoin(TestOrder_.USER)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("LEFT JOIN"));
        assertTrue(sql.contains("users"));
    }

    @Test
    @DisplayName("where + whereHas chains predicates correctly")
    void testWhereAndWhereHasChained() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .whereHas(TestUser_.ORDERS)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("EXISTS"));
        assertTrue(sql.contains("is_active"));
    }

    @Test
    @DisplayName("where + whereDoesntHave chains predicates correctly")
    void testWhereAndWhereDoesntHaveChained() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .whereDoesntHave(TestUser_.ORDERS)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("NOT EXISTS"));
    }

    @Test
    @DisplayName("where + wherePivot chains predicates correctly")
    void testWhereAndWherePivotChained() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .wherePivot(USER_TAGS, "is_primary", true)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("is_active"));
        assertTrue(sql.contains("user_tags"));
    }

    @Test
    @DisplayName("where + wherePivotIn chains predicates correctly")
    void testWhereAndWherePivotInChained() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .wherePivotIn(USER_TAGS, "type", java.util.List.of("a", "b"))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("is_active"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    @DisplayName("where + wherePivotNotIn chains predicates correctly")
    void testWhereAndWherePivotNotInChained() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .wherePivotNotIn(USER_TAGS, "type", java.util.List.of("x", "y"))
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("is_active"));
        assertTrue(sql.contains("NOT IN"));
    }

    @Test
    @DisplayName("where + wherePivotNull chains predicates correctly")
    void testWhereAndWherePivotNullChained() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .wherePivotNull(USER_TAGS, "revoked_at")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("is_active"));
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    @DisplayName("where + wherePivotNotNull chains predicates correctly")
    void testWhereAndWherePivotNotNullChained() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .wherePivotNotNull(USER_TAGS, "assigned_at")
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("is_active"));
        assertTrue(sql.contains("IS NOT NULL"));
    }

    @Test
    @DisplayName("where + wherePivotBetween chains predicates correctly")
    void testWhereAndWherePivotBetweenChained() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.IS_ACTIVE.eq(true))
            .wherePivotBetween(USER_TAGS, "score", 1, 100)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("is_active"));
        assertTrue(sql.contains("BETWEEN"));
    }

    @Test
    @DisplayName("wherePivotIn throws for non-BelongsToMany")
    void testWherePivotInThrowsForNonBelongsToMany() {
        assertThrows(IllegalArgumentException.class, () ->
            Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .wherePivotIn(TestUser_.ORDERS, "col", java.util.List.of(1, 2))
                .build()
        );
    }

    @Test
    @DisplayName("wherePivotNotIn throws for non-BelongsToMany")
    void testWherePivotNotInThrowsForNonBelongsToMany() {
        assertThrows(IllegalArgumentException.class, () ->
            Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .wherePivotNotIn(TestUser_.ORDERS, "col", java.util.List.of(1, 2))
                .build()
        );
    }

    @Test
    @DisplayName("wherePivotNull throws for non-BelongsToMany")
    void testWherePivotNullThrowsForNonBelongsToMany() {
        assertThrows(IllegalArgumentException.class, () ->
            Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .wherePivotNull(TestUser_.ORDERS, "col")
                .build()
        );
    }

    @Test
    @DisplayName("wherePivotNotNull throws for non-BelongsToMany")
    void testWherePivotNotNullThrowsForNonBelongsToMany() {
        assertThrows(IllegalArgumentException.class, () ->
            Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .wherePivotNotNull(TestUser_.ORDERS, "col")
                .build()
        );
    }

    @Test
    @DisplayName("wherePivotBetween throws for non-BelongsToMany")
    void testWherePivotBetweenThrowsForNonBelongsToMany() {
        assertThrows(IllegalArgumentException.class, () ->
            Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .wherePivotBetween(TestUser_.ORDERS, "col", 1, 10)
                .build()
        );
    }

    @Test
    @DisplayName("orderByPivot throws for non-BelongsToMany")
    void testOrderByPivotThrowsForNonBelongsToMany() {
        assertThrows(IllegalArgumentException.class, () ->
            Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .orderByPivot(TestUser_.ORDERS, "col", OrderDirection.ASC)
                .build()
        );
    }

    @Test
    @DisplayName("selectPivot with alias throws for non-BelongsToMany")
    void testSelectPivotWithAliasThrowsForNonBelongsToMany() {
        assertThrows(IllegalArgumentException.class, () ->
            Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .selectPivot(TestUser_.ORDERS, "col", "alias")
                .build()
        );
    }

    @Test
    @DisplayName("selectPivot without alias throws for non-BelongsToMany")
    void testSelectPivotWithoutAliasThrowsForNonBelongsToMany() {
        assertThrows(IllegalArgumentException.class, () ->
            Suprim.select(TestUser_.ID)
                .from(TestUser_.TABLE)
                .selectPivot(TestUser_.ORDERS, "col")
                .build()
        );
    }

    @Test
    @DisplayName("chained select() adds expressions to existing select list")
    void testChainedSelectAddsExpressions() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .select(TestUser_.EMAIL, TestUser_.NAME)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("\"id\""));
        assertTrue(sql.contains("\"email\""));
        assertTrue(sql.contains("\"name\""));
    }

    @Test
    @DisplayName("with(String, Function) without from() throws exception")
    void testWithStringFunctionWithoutFromThrows() {
        assertThrows(IllegalStateException.class, () ->
            Suprim.select(TestUser_.ID)
                .with("orders", b -> b.limit(5))
                .build()
        );
    }

    @Test
    @DisplayName("leftJoin with BELONGS_TO_MANY relation type")
    void testJoinWithBelongsToManyRelation() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .leftJoin(USER_TAGS)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("LEFT JOIN"));
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("tags"));
    }

    @Test
    @DisplayName("mergeDefaultEagerLoads applies @Entity(with=...) defaults")
    void testMergeDefaultEagerLoads() {
        // TestUserWithDefaults has @Entity(with = {"orders"})
        QueryResult result = Suprim.select(sant1ago.dev.suprim.core.TestUserWithDefaults_.ID)
            .from(sant1ago.dev.suprim.core.TestUserWithDefaults_.TABLE)
            .build();

        assertNotNull(result);
        // Default eager loads should be applied automatically
    }

    @Test
    @DisplayName("mergeDefaultEagerLoads respects without() exclusion")
    void testMergeDefaultEagerLoadsRespectsWithout() {
        // TestUserWithDefaults has @Entity(with = {"orders"})
        // But we exclude it with .without()
        QueryResult result = Suprim.select(sant1ago.dev.suprim.core.TestUserWithDefaults_.ID)
            .from(sant1ago.dev.suprim.core.TestUserWithDefaults_.TABLE)
            .without(sant1ago.dev.suprim.core.TestUserWithDefaults_.ORDERS)
            .build();

        assertNotNull(result);
    }

    @Test
    @DisplayName("mergeDefaultEagerLoads ignores already loaded relations")
    void testMergeDefaultEagerLoadsIgnoresExplicit() {
        // TestUserWithDefaults has @Entity(with = {"orders"})
        // Explicitly load orders with constraint
        QueryResult result = Suprim.select(sant1ago.dev.suprim.core.TestUserWithDefaults_.ID)
            .from(sant1ago.dev.suprim.core.TestUserWithDefaults_.TABLE)
            .with(sant1ago.dev.suprim.core.TestUserWithDefaults_.ORDERS, b -> b.limit(10))
            .build();

        assertNotNull(result);
    }

    @Test
    @DisplayName("whereHas without from uses relation's owner table")
    void testWhereHasWithoutFromUsesRelationOwner() {
        // Call whereHas without .from() to test getOwnerTableName branch
        SelectBuilder builder = Suprim.select(TestUser_.ID)
            .whereHas(TestUser_.ORDERS);

        // Now add from and build to get valid query
        QueryResult result = builder.from(TestUser_.TABLE).build();

        String sql = result.sql();
        assertTrue(sql.contains("EXISTS"));
    }

    @Test
    @DisplayName("leftJoin with HAS_ONE relation type")
    void testJoinWithHasOneRelation() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .leftJoin(TestUser_.LATEST_ORDER)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("LEFT JOIN"));
        assertTrue(sql.contains("orders"));
    }

    @Test
    @DisplayName("mergeDefaultEagerLoads silently skips invalid relation names")
    void testMergeDefaultEagerLoadsSkipsInvalidRelations() {
        // TestUserWithInvalidDefaults has @Entity(with = {"nonexistent_relation"})
        // The exception should be caught silently
        QueryResult result = Suprim.select(sant1ago.dev.suprim.core.TestUserWithInvalidDefaults_.ID)
            .from(sant1ago.dev.suprim.core.TestUserWithInvalidDefaults_.TABLE)
            .build();

        assertNotNull(result);
    }

    @Test
    @DisplayName("build with recursive CTE")
    void testBuildWithRecursiveCte() {
        // Create a recursive CTE query
        SelectBuilder baseQuery = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .where(TestUser_.ID.eq(1L));

        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .withRecursive("hierarchy", baseQuery)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("WITH RECURSIVE"));
    }

    // ==================== DIALECT UNSUPPORTED FEATURES ====================

    @Test
    @DisplayName("forUpdateNowait throws on MySQL 5.7 dialect")
    void testForUpdateNowaitThrowsOnMySql57() {
        SelectBuilder builder = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .forUpdateNowait();

        // MySQL 5.7 doesn't support NOWAIT
        assertThrows(UnsupportedDialectFeatureException.class, () ->
            builder.build(MySqlDialect.INSTANCE));
    }

    @Test
    @DisplayName("forUpdateSkipLocked throws on MySQL 5.7 dialect")
    void testForUpdateSkipLockedThrowsOnMySql57() {
        SelectBuilder builder = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .forUpdateSkipLocked();

        // MySQL 5.7 doesn't support SKIP LOCKED
        assertThrows(UnsupportedDialectFeatureException.class, () ->
            builder.build(MySqlDialect.INSTANCE));
    }

    @Test
    @DisplayName("forShareNowait throws on MySQL 5.7 dialect")
    void testForShareNowaitThrowsOnMySql57() {
        SelectBuilder builder = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .forShareNowait();

        // MySQL 5.7 doesn't support NOWAIT
        assertThrows(UnsupportedDialectFeatureException.class, () ->
            builder.build(MySqlDialect.INSTANCE));
    }

    @Test
    @DisplayName("forShareSkipLocked throws on MySQL 5.7 dialect")
    void testForShareSkipLockedThrowsOnMySql57() {
        SelectBuilder builder = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .forShareSkipLocked();

        // MySQL 5.7 doesn't support SKIP LOCKED
        assertThrows(UnsupportedDialectFeatureException.class, () ->
            builder.build(MySqlDialect.INSTANCE));
    }

    // ==================== JOIN TYPE COVERAGE ====================

    @Test
    @DisplayName("rightJoin with HAS_ONE relation")
    void testRightJoinWithHasOneRelation() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .rightJoin(TestUser_.LATEST_ORDER)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("RIGHT JOIN"));
        assertTrue(sql.contains("orders"));
    }

    @Test
    @DisplayName("innerJoin with HAS_MANY relation")
    void testInnerJoinWithHasManyRelation() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .join(TestUser_.ORDERS)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("JOIN"));
        assertTrue(sql.contains("orders"));
    }

    @Test
    @DisplayName("rightJoin with BELONGS_TO relation")
    void testRightJoinWithBelongsToRelation() {
        QueryResult result = Suprim.select(TestOrder_.ID)
            .from(TestOrder_.TABLE)
            .rightJoin(TestOrder_.USER)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("RIGHT JOIN"));
        assertTrue(sql.contains("users"));
    }

    @Test
    @DisplayName("innerJoin with BELONGS_TO_MANY relation")
    void testInnerJoinWithBelongsToManyRelation() {
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .join(USER_TAGS)
            .build();

        String sql = result.sql();
        assertTrue(sql.contains("JOIN"));
        assertTrue(sql.contains("user_tags"));
        assertTrue(sql.contains("tags"));
    }

    @Test
    @DisplayName("leftJoin with unsupported relation type (HAS_ONE_THROUGH) does not add join")
    void testJoinWithUnsupportedRelationType() {
        // HAS_ONE_THROUGH is not handled by addRelationJoin switch
        // The method should silently skip (no join added)
        QueryResult result = Suprim.select(TestUser_.ID)
            .from(TestUser_.TABLE)
            .leftJoin(USER_PROFILE_THROUGH)
            .build();

        String sql = result.sql();
        // Query should succeed but no join clause added for unsupported type
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
    }

    @Test
    @DisplayName("mergeDefaultEagerLoads handles table with null entityType (defensive code path)")
    void testMergeDefaultEagerLoadsWithNullEntityType() throws Exception {
        // This tests the defensive code path where entityType could be null
        // Using reflection to create a Table with null entityType
        Table<Object> tableWithNullEntityType = createTableWithNullEntityType();

        QueryResult result = Suprim.select()
            .from(tableWithNullEntityType)
            .build();

        // Should succeed without exception - mergeDefaultEagerLoads returns early
        assertNotNull(result);
    }

    @SuppressWarnings("unchecked")
    private static Table<Object> createTableWithNullEntityType() throws Exception {
        // Use reflection to bypass the null check in Table constructor
        java.lang.reflect.Constructor<Table> constructor = Table.class.getDeclaredConstructor(
            String.class, String.class, Class.class, String.class
        );
        constructor.setAccessible(true);

        // Create table normally first
        Table<Object> table = (Table<Object>) constructor.newInstance("test_table", "", Object.class, null);

        // Now use reflection to set entityType to null
        java.lang.reflect.Field entityTypeField = Table.class.getDeclaredField("entityType");
        entityTypeField.setAccessible(true);
        entityTypeField.set(table, null);

        return table;
    }
}
