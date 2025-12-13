package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Relation.Type enum methods and Builder null handling.
 */
class RelationTypeTest {

    @Entity(table = "users")
    static class User {}

    @Entity(table = "roles")
    static class Role {}

    @Entity(table = "posts")
    static class Post {}

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final Table<Role> ROLES = Table.of("roles", Role.class);
    private static final Table<Post> POSTS = Table.of("posts", Post.class);

    @Test
    void testIsThrough() {
        assertTrue(Relation.Type.HAS_ONE_THROUGH.isThrough());
        assertTrue(Relation.Type.HAS_MANY_THROUGH.isThrough());

        assertFalse(Relation.Type.HAS_ONE.isThrough());
        assertFalse(Relation.Type.HAS_MANY.isThrough());
        assertFalse(Relation.Type.BELONGS_TO.isThrough());
        assertFalse(Relation.Type.BELONGS_TO_MANY.isThrough());
        assertFalse(Relation.Type.MORPH_ONE.isThrough());
        assertFalse(Relation.Type.MORPH_MANY.isThrough());
        assertFalse(Relation.Type.MORPH_TO.isThrough());
        assertFalse(Relation.Type.MORPH_TO_MANY.isThrough());
        assertFalse(Relation.Type.MORPHED_BY_MANY.isThrough());
        assertFalse(Relation.Type.LATEST_OF_MANY.isThrough());
        assertFalse(Relation.Type.OLDEST_OF_MANY.isThrough());
        assertFalse(Relation.Type.OF_MANY.isThrough());
    }

    @Test
    void testIsToMany() {
        assertTrue(Relation.Type.HAS_MANY.isToMany());
        assertTrue(Relation.Type.BELONGS_TO_MANY.isToMany());
        assertTrue(Relation.Type.HAS_MANY_THROUGH.isToMany());
        assertTrue(Relation.Type.MORPH_MANY.isToMany());
        assertTrue(Relation.Type.MORPH_TO_MANY.isToMany());
        assertTrue(Relation.Type.MORPHED_BY_MANY.isToMany());

        assertFalse(Relation.Type.HAS_ONE.isToMany());
        assertFalse(Relation.Type.BELONGS_TO.isToMany());
        assertFalse(Relation.Type.HAS_ONE_THROUGH.isToMany());
        assertFalse(Relation.Type.MORPH_ONE.isToMany());
        assertFalse(Relation.Type.MORPH_TO.isToMany());
        assertFalse(Relation.Type.LATEST_OF_MANY.isToMany());
        assertFalse(Relation.Type.OLDEST_OF_MANY.isToMany());
        assertFalse(Relation.Type.OF_MANY.isToMany());
    }

    @Test
    void testIsMorphic() {
        assertTrue(Relation.Type.MORPH_ONE.isMorphic());
        assertTrue(Relation.Type.MORPH_MANY.isMorphic());
        assertTrue(Relation.Type.MORPH_TO.isMorphic());
        assertTrue(Relation.Type.MORPH_TO_MANY.isMorphic());
        assertTrue(Relation.Type.MORPHED_BY_MANY.isMorphic());

        assertFalse(Relation.Type.HAS_ONE.isMorphic());
        assertFalse(Relation.Type.HAS_MANY.isMorphic());
        assertFalse(Relation.Type.BELONGS_TO.isMorphic());
        assertFalse(Relation.Type.BELONGS_TO_MANY.isMorphic());
        assertFalse(Relation.Type.HAS_ONE_THROUGH.isMorphic());
        assertFalse(Relation.Type.HAS_MANY_THROUGH.isMorphic());
        assertFalse(Relation.Type.LATEST_OF_MANY.isMorphic());
        assertFalse(Relation.Type.OLDEST_OF_MANY.isMorphic());
        assertFalse(Relation.Type.OF_MANY.isMorphic());
    }

    @Test
    void testIsOfMany() {
        assertTrue(Relation.Type.LATEST_OF_MANY.isOfMany());
        assertTrue(Relation.Type.OLDEST_OF_MANY.isOfMany());
        assertTrue(Relation.Type.OF_MANY.isOfMany());

        assertFalse(Relation.Type.HAS_ONE.isOfMany());
        assertFalse(Relation.Type.HAS_MANY.isOfMany());
        assertFalse(Relation.Type.BELONGS_TO.isOfMany());
        assertFalse(Relation.Type.BELONGS_TO_MANY.isOfMany());
        assertFalse(Relation.Type.HAS_ONE_THROUGH.isOfMany());
        assertFalse(Relation.Type.HAS_MANY_THROUGH.isOfMany());
        assertFalse(Relation.Type.MORPH_ONE.isOfMany());
        assertFalse(Relation.Type.MORPH_MANY.isOfMany());
        assertFalse(Relation.Type.MORPH_TO.isOfMany());
        assertFalse(Relation.Type.MORPH_TO_MANY.isOfMany());
        assertFalse(Relation.Type.MORPHED_BY_MANY.isOfMany());
    }

    @Test
    void testAllEnumValues() {
        assertEquals(14, Relation.Type.values().length);
        assertNotNull(Relation.Type.valueOf("HAS_ONE"));
        assertNotNull(Relation.Type.valueOf("HAS_MANY"));
        assertNotNull(Relation.Type.valueOf("BELONGS_TO"));
        assertNotNull(Relation.Type.valueOf("BELONGS_TO_MANY"));
        assertNotNull(Relation.Type.valueOf("HAS_ONE_THROUGH"));
        assertNotNull(Relation.Type.valueOf("HAS_MANY_THROUGH"));
        assertNotNull(Relation.Type.valueOf("MORPH_ONE"));
        assertNotNull(Relation.Type.valueOf("MORPH_MANY"));
        assertNotNull(Relation.Type.valueOf("MORPH_TO"));
        assertNotNull(Relation.Type.valueOf("MORPH_TO_MANY"));
        assertNotNull(Relation.Type.valueOf("MORPHED_BY_MANY"));
        assertNotNull(Relation.Type.valueOf("LATEST_OF_MANY"));
        assertNotNull(Relation.Type.valueOf("OLDEST_OF_MANY"));
        assertNotNull(Relation.Type.valueOf("OF_MANY"));
    }

    // ==================== Builder Null Branch Tests ====================

    @Test
    void testBelongsToManyWithNullPivotColumns() {
        Relation<User, Role> relation = Relation.belongsToMany(
                USERS, ROLES, "role_user", "user_id", "role_id",
                "id", "id", null, false, false
        );
        assertNotNull(relation.getPivotColumns());
        assertTrue(relation.getPivotColumns().isEmpty());
    }

    @Test
    void testBelongsToWithNullTouchColumns() {
        Relation<Post, User> relation = Relation.belongsTo(
                POSTS, USERS, "user_id", "id", false, false,
                "author", true, null
        );
        assertNotNull(relation.getTouchColumns());
        assertEquals(List.of("updated_at"), relation.getTouchColumns());
    }

    @Test
    void testHasOneWithNullDefaultAttributes() {
        Relation<User, Post> relation = Relation.hasOne(
                USERS, POSTS, "user_id", "id", false, false,
                "latestPost", true, null
        );
        assertNotNull(relation.getDefaultAttributes());
        assertTrue(relation.getDefaultAttributes().isEmpty());
    }

    @Test
    void testBelongsToWithNullDefaultAttributes() {
        Relation<Post, User> relation = Relation.belongsTo(
                POSTS, USERS, "user_id", "id", false, false,
                "author", false, List.of(), true, null
        );
        assertNotNull(relation.getDefaultAttributes());
        assertTrue(relation.getDefaultAttributes().isEmpty());
    }

    // ==================== Through Relationship Tests ====================

    @Test
    void testHasOneThrough() {
        Relation<User, Post> relation = Relation.hasOneThrough(
                USERS, POSTS, ROLES,
                "user_id", "role_id", "id", "id", false, "latestPost"
        );
        assertEquals(Relation.Type.HAS_ONE_THROUGH, relation.getType());
        assertEquals(ROLES, relation.getThroughTable());
        assertEquals("user_id", relation.getFirstKey());
        assertEquals("role_id", relation.getSecondKey());
        assertEquals("id", relation.getSecondLocalKey());
        assertTrue(relation.isThrough());
    }

    @Test
    void testHasManyThrough() {
        Relation<User, Post> relation = Relation.hasManyThrough(
                USERS, POSTS, ROLES,
                "user_id", "role_id", "id", "id", true
        );
        assertEquals(Relation.Type.HAS_MANY_THROUGH, relation.getType());
        assertTrue(relation.isThrough());
        assertTrue(relation.isEager());
    }

    // ==================== Morph Relationship Tests ====================

    @Test
    void testMorphOne() {
        Relation<User, Post> relation = Relation.morphOne(
                USERS, POSTS, "imageable", "imageable_type", "imageable_id", "id", false
        );
        assertEquals(Relation.Type.MORPH_ONE, relation.getType());
        assertEquals("imageable", relation.getMorphName());
        assertEquals("imageable_type", relation.getMorphTypeColumn());
        assertEquals("imageable_id", relation.getMorphIdColumn());
        assertTrue(relation.isMorphic());
    }

    @Test
    void testMorphMany() {
        Relation<User, Post> relation = Relation.morphMany(
                USERS, POSTS, "commentable", "commentable_type", "commentable_id", "id", true
        );
        assertEquals(Relation.Type.MORPH_MANY, relation.getType());
        assertTrue(relation.isMorphic());
    }

    @Test
    void testMorphTo() {
        Relation<Post, Object> relation = Relation.morphTo(
                POSTS, "commentable", "commentable_type", "commentable_id", false
        );
        assertEquals(Relation.Type.MORPH_TO, relation.getType());
        assertTrue(relation.isMorphic());
    }

    @Test
    void testMorphToMany() {
        Relation<User, Role> relation = Relation.morphToMany(
                USERS, ROLES, "taggables", "taggable", "taggable_type", "taggable_id",
                "role_id", "id", "id", List.of(), false, false
        );
        assertEquals(Relation.Type.MORPH_TO_MANY, relation.getType());
        assertTrue(relation.isMorphic());
    }

    @Test
    void testMorphedByMany() {
        Relation<Role, User> relation = Relation.morphedByMany(
                ROLES, USERS, "taggables", "taggable", "taggable_type", "taggable_id",
                "role_id", "id", "id", List.of(), false, false
        );
        assertEquals(Relation.Type.MORPHED_BY_MANY, relation.getType());
        assertTrue(relation.isMorphic());
    }

    // ==================== OfMany Relationship Tests ====================

    @Test
    void testLatestOfMany() {
        Relation<User, Post> relation = Relation.latestOfMany(
                USERS, POSTS, "user_id", "id", "created_at", false, "latestPost"
        );
        assertEquals(Relation.Type.LATEST_OF_MANY, relation.getType());
        assertEquals("created_at", relation.getOrderColumn());
        assertTrue(relation.isOfMany());
    }

    @Test
    void testOldestOfMany() {
        Relation<User, Post> relation = Relation.oldestOfMany(
                USERS, POSTS, "user_id", "id", "created_at", true
        );
        assertEquals(Relation.Type.OLDEST_OF_MANY, relation.getType());
        assertTrue(relation.isOfMany());
    }

    @Test
    void testOfMany() {
        Relation<User, Post> relation = Relation.ofMany(
                USERS, POSTS, "user_id", "id", "amount", "MAX", false, "highestBid"
        );
        assertEquals(Relation.Type.OF_MANY, relation.getType());
        assertEquals("amount", relation.getAggregateColumn());
        assertEquals("MAX", relation.getAggregateFunction());
        assertTrue(relation.isOfMany());
    }

    // ==================== PivotColumns non-null branch ====================

    @Test
    void testBelongsToManyWithPivotColumns() {
        Relation<User, Role> relation = Relation.belongsToMany(
                USERS, ROLES, "role_user", "user_id", "role_id",
                "id", "id", List.of("created_at", "expires_at"), true, false
        );
        assertEquals(List.of("created_at", "expires_at"), relation.getPivotColumns());
        assertTrue(relation.hasPivotTimestamps());
    }
}
