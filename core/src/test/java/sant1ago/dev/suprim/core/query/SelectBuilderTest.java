package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.TestCte;
import sant1ago.dev.suprim.core.TestOrder_;
import sant1ago.dev.suprim.core.TestUser_;
import sant1ago.dev.suprim.core.dialect.MySqlDialect;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.type.Table;

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
}
