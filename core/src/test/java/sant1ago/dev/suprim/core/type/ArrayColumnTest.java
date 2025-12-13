package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ArrayColumn and ArrayLiteral.
 */
class ArrayColumnTest {

    @Entity(table = "users")
    static class User {}

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final ArrayColumn<User, String> TAGS = new ArrayColumn<>(USERS, "tags", String.class, "TEXT[]");
    private static final ArrayColumn<User, Integer> SCORES = new ArrayColumn<>(USERS, "scores", Integer.class, "INTEGER[]");

    // ==================== ArrayLiteral Tests ====================

    @Test
    void testArrayLiteralGetValueType() {
        ArrayColumn.ArrayLiteral<String> literal = new ArrayColumn.ArrayLiteral<>(List.of("a", "b"), String.class);
        assertEquals(List.class, literal.getValueType());
    }

    @Test
    void testArrayLiteralEmptyArray() {
        // Tests values.isEmpty() = true branch
        ArrayColumn.ArrayLiteral<String> literal = new ArrayColumn.ArrayLiteral<>(List.of(), String.class);
        assertEquals("ARRAY[]", literal.toSql(PostgreSqlDialect.INSTANCE));
    }

    @Test
    void testArrayLiteralSingleStringElement() {
        // Tests String instanceof branch with single element (i > 0 = false)
        ArrayColumn.ArrayLiteral<String> literal = new ArrayColumn.ArrayLiteral<>(List.of("test"), String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("ARRAY['test']", sql);
    }

    @Test
    void testArrayLiteralMultipleStringElements() {
        // Tests i > 0 = true branch and String instanceof
        ArrayColumn.ArrayLiteral<String> literal = new ArrayColumn.ArrayLiteral<>(List.of("a", "b", "c"), String.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("ARRAY['a', 'b', 'c']", sql);
    }

    @Test
    void testArrayLiteralNonStringElements() {
        // Tests instanceof String = false branch
        ArrayColumn.ArrayLiteral<Integer> literal = new ArrayColumn.ArrayLiteral<>(List.of(1, 2, 3), Integer.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("ARRAY[1, 2, 3]", sql);
    }

    @Test
    void testArrayLiteralSingleNonStringElement() {
        // Single non-string element
        ArrayColumn.ArrayLiteral<Integer> literal = new ArrayColumn.ArrayLiteral<>(List.of(42), Integer.class);
        String sql = literal.toSql(PostgreSqlDialect.INSTANCE);
        assertEquals("ARRAY[42]", sql);
    }

    @Test
    void testArrayLiteralRecordAccessors() {
        List<String> values = List.of("x", "y");
        ArrayColumn.ArrayLiteral<String> literal = new ArrayColumn.ArrayLiteral<>(values, String.class);
        assertEquals(values, literal.values());
        assertEquals(String.class, literal.elementType());
    }

    // ==================== ArrayColumn Tests ====================

    @Test
    void testGetElementType() {
        assertEquals(String.class, TAGS.getElementType());
        assertEquals(Integer.class, SCORES.getElementType());
    }

    @Test
    void testContains() {
        Predicate predicate = TAGS.contains("admin");
        assertNotNull(predicate);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("@>"));
        assertTrue(sql.contains("ARRAY['admin']"));
    }

    @Test
    void testContainsAll() {
        Predicate predicate = TAGS.containsAll("admin", "user", "guest");
        assertNotNull(predicate);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("@>"));
        assertTrue(sql.contains("ARRAY['admin', 'user', 'guest']"));
    }

    @Test
    void testContainedBy() {
        Predicate predicate = TAGS.containedBy("admin", "user");
        assertNotNull(predicate);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("<@"));
    }

    @Test
    void testOverlapVarargs() {
        Predicate predicate = TAGS.overlap("tag1", "tag2");
        assertNotNull(predicate);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("&&"));
    }

    @Test
    void testOverlapList() {
        Predicate predicate = TAGS.overlap(List.of("tag1", "tag2", "tag3"));
        assertNotNull(predicate);
        String sql = predicate.toSql(PostgreSqlDialect.INSTANCE);
        assertTrue(sql.contains("&&"));
    }
}
