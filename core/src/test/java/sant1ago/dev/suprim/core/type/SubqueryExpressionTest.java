package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.query.SelectBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SubqueryExpression and ExistsPredicate.
 */
class SubqueryExpressionTest {

    private static final SqlDialect DIALECT = PostgreSqlDialect.INSTANCE;

    @Entity(table = "users")
    static class User {}

    @Entity(table = "posts")
    static class Post {}

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final Table<Post> POSTS = Table.of("posts", Post.class);
    private static final ComparableColumn<User, Long> USER_ID =
            new ComparableColumn<>(USERS, "id", Long.class, "BIGINT");

    @Test
    void testGetValueType() {
        SelectBuilder subquery = new SelectBuilder(List.of(USER_ID)).from(USERS);
        SubqueryExpression<Long> expr = new SubqueryExpression<>(subquery, Long.class);
        assertEquals(Long.class, expr.getValueType());
    }

    @Test
    void testToSql() {
        SelectBuilder subquery = new SelectBuilder(List.of(USER_ID)).from(USERS);
        SubqueryExpression<Long> expr = new SubqueryExpression<>(subquery, Long.class);
        String sql = expr.toSql(DIALECT);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("users"));
    }

    @Test
    void testExists() {
        SelectBuilder subquery = new SelectBuilder(List.of()).from(USERS);
        Predicate predicate = SubqueryExpression.exists(subquery);
        assertInstanceOf(SubqueryExpression.ExistsPredicate.class, predicate);
        String sql = predicate.toSql(DIALECT);
        assertTrue(sql.startsWith("EXISTS ("));
        assertTrue(sql.contains("users"));
        assertTrue(sql.endsWith(")"));
    }

    @Test
    void testNotExists() {
        SelectBuilder subquery = new SelectBuilder(List.of()).from(USERS);
        Predicate predicate = SubqueryExpression.notExists(subquery);
        assertInstanceOf(SubqueryExpression.ExistsPredicate.class, predicate);
        String sql = predicate.toSql(DIALECT);
        assertTrue(sql.startsWith("NOT EXISTS ("));
        assertTrue(sql.contains("users"));
        assertTrue(sql.endsWith(")"));
    }

    @Test
    void testExistsPredicateNegatedFalse() {
        SelectBuilder subquery = new SelectBuilder(List.of()).from(POSTS);
        SubqueryExpression.ExistsPredicate predicate =
                new SubqueryExpression.ExistsPredicate(subquery, false);
        assertFalse(predicate.negated());
        String sql = predicate.toSql(DIALECT);
        assertTrue(sql.startsWith("EXISTS ("));
        assertFalse(sql.startsWith("NOT EXISTS"));
    }

    @Test
    void testExistsPredicateNegatedTrue() {
        SelectBuilder subquery = new SelectBuilder(List.of()).from(POSTS);
        SubqueryExpression.ExistsPredicate predicate =
                new SubqueryExpression.ExistsPredicate(subquery, true);
        assertTrue(predicate.negated());
        String sql = predicate.toSql(DIALECT);
        assertTrue(sql.startsWith("NOT EXISTS ("));
    }

    @Test
    void testSubqueryExpressionRecord() {
        SelectBuilder subquery = new SelectBuilder(List.of(USER_ID)).from(USERS);
        SubqueryExpression<Long> expr = new SubqueryExpression<>(subquery, Long.class);
        assertSame(subquery, expr.subquery());
        assertEquals(Long.class, expr.valueType());
    }
}
