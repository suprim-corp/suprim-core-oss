package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QueryResult record.
 */
class QueryResultTest {

    @Entity(table = "users")
    static class User {}

    @Entity(table = "posts")
    static class Post {}

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final Table<Post> POSTS = Table.of("posts", Post.class);
    private static final Relation<User, Post> USER_POSTS = Relation.hasMany(
            USERS, POSTS, "user_id", "id", false, false
    );

    // ==================== Constructors ====================

    @Test
    void testCanonicalConstructor() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "value1");
        params.put("p2", 42);
        List<EagerLoadSpec> eagerLoads = List.of(EagerLoadSpec.of(USER_POSTS));

        QueryResult result = new QueryResult("SELECT * FROM users", params, eagerLoads);

        assertEquals("SELECT * FROM users", result.sql());
        assertEquals(2, result.parameters().size());
        assertEquals(1, result.eagerLoads().size());
    }

    @Test
    void testTwoParamConstructor() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "value");

        QueryResult result = new QueryResult("SELECT * FROM users", params);

        assertEquals("SELECT * FROM users", result.sql());
        assertEquals(1, result.parameters().size());
        assertTrue(result.eagerLoads().isEmpty());
    }

    // ==================== parameters() ====================

    @Test
    void testParametersReturnsUnmodifiable() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "value");

        QueryResult result = new QueryResult("SELECT * FROM users", params);

        assertThrows(UnsupportedOperationException.class, () ->
                result.parameters().put("p2", "new")
        );
    }

    // ==================== eagerLoads() ====================

    @Test
    void testEagerLoadsReturnsUnmodifiable() {
        List<EagerLoadSpec> eagerLoads = new ArrayList<>();
        eagerLoads.add(EagerLoadSpec.of(USER_POSTS));

        QueryResult result = new QueryResult("SELECT * FROM users", Map.of(), eagerLoads);

        assertThrows(UnsupportedOperationException.class, () ->
                result.eagerLoads().add(EagerLoadSpec.of(USER_POSTS))
        );
    }

    // ==================== hasEagerLoads() ====================

    @Test
    void testHasEagerLoadsTrue() {
        List<EagerLoadSpec> eagerLoads = List.of(EagerLoadSpec.of(USER_POSTS));
        QueryResult result = new QueryResult("SELECT * FROM users", Map.of(), eagerLoads);

        assertTrue(result.hasEagerLoads());
    }

    @Test
    void testHasEagerLoadsFalseEmpty() {
        QueryResult result = new QueryResult("SELECT * FROM users", Map.of(), new ArrayList<>());
        assertFalse(result.hasEagerLoads());
    }

    @Test
    void testHasEagerLoadsFalseFromTwoParamConstructor() {
        // Test with two-param constructor (returns empty list)
        QueryResult result = new QueryResult("SELECT * FROM users", Map.of());
        assertFalse(result.hasEagerLoads());
    }

    @Test
    void testHasEagerLoadsFalseWithNullEagerLoads() {
        // Test with null eagerLoads passed to canonical constructor
        QueryResult result = new QueryResult("SELECT * FROM users", Map.of(), null);
        assertFalse(result.hasEagerLoads());
    }

    // ==================== parameterValues() ====================

    @Test
    void testParameterValues() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "value1");
        params.put("p2", 42);
        params.put("p3", true);

        QueryResult result = new QueryResult("SELECT * FROM users", params);
        Object[] values = result.parameterValues();

        assertEquals(3, values.length);
        assertEquals("value1", values[0]);
        assertEquals(42, values[1]);
        assertEquals(true, values[2]);
    }

    @Test
    void testParameterValuesEmpty() {
        QueryResult result = new QueryResult("SELECT * FROM users", Map.of());
        Object[] values = result.parameterValues();

        assertEquals(0, values.length);
    }

    // ==================== hasParameters() ====================

    @Test
    void testHasParametersTrue() {
        Map<String, Object> params = Map.of("p1", "value");
        QueryResult result = new QueryResult("SELECT * FROM users", params);

        assertTrue(result.hasParameters());
    }

    @Test
    void testHasParametersFalse() {
        QueryResult result = new QueryResult("SELECT * FROM users", Map.of());
        assertFalse(result.hasParameters());
    }

    // ==================== toString() ====================

    @Test
    void testToStringBasic() {
        Map<String, Object> params = Map.of("p1", "value");
        QueryResult result = new QueryResult("SELECT * FROM users", params);

        String str = result.toString();
        assertTrue(str.contains("QueryResult"));
        assertTrue(str.contains("SELECT * FROM users"));
        assertTrue(str.contains("p1"));
        assertFalse(str.contains("eagerLoads="));
    }

    @Test
    void testToStringWithEagerLoads() {
        List<EagerLoadSpec> eagerLoads = List.of(EagerLoadSpec.of(USER_POSTS));
        QueryResult result = new QueryResult("SELECT * FROM users", Map.of(), eagerLoads);

        String str = result.toString();
        assertTrue(str.contains("eagerLoads=1"));
    }

    @Test
    void testToStringWithEmptyParams() {
        QueryResult result = new QueryResult("SELECT 1", Map.of());
        String str = result.toString();

        assertTrue(str.contains("QueryResult"));
        assertTrue(str.contains("SELECT 1"));
    }
}
