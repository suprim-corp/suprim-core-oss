package sant1ago.dev.suprim.core.query;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EagerLoadSpec record.
 */
class EagerLoadSpecTest {

    @Entity(table = "users")
    static class User {}

    @Entity(table = "posts")
    static class Post {}

    @Entity(table = "comments")
    static class Comment {}

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final Table<Post> POSTS = Table.of("posts", Post.class);
    private static final Table<Comment> COMMENTS = Table.of("comments", Comment.class);

    private static final Relation<User, Post> USER_POSTS = Relation.hasMany(
            USERS, POSTS, "user_id", "id", false, false
    );
    private static final Relation<Post, Comment> POST_COMMENTS = Relation.hasMany(
            POSTS, COMMENTS, "post_id", "id", false, false
    );

    // ==================== Factory Methods ====================

    @Test
    void testOfWithRelation() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);

        assertEquals(USER_POSTS, spec.relation());
        assertNull(spec.constraint());
        assertNotNull(spec.nested());
        assertTrue(spec.nested().isEmpty());
    }

    @Test
    void testOfWithRelationAndConstraint() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS, b -> b.limit(5));

        assertEquals(USER_POSTS, spec.relation());
        assertNotNull(spec.constraint());
        assertTrue(spec.nested().isEmpty());
    }

    // ==================== with() methods ====================

    @Test
    void testWithNestedRelation() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS)
                .with(POST_COMMENTS);

        assertEquals(USER_POSTS, spec.relation());
        assertEquals(1, spec.nested().size());
        assertEquals(POST_COMMENTS, spec.nested().get(0).relation());
    }

    @Test
    void testWithNestedRelationAndConstraint() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS)
                .with(POST_COMMENTS, b -> b.limit(10));

        assertEquals(USER_POSTS, spec.relation());
        assertEquals(1, spec.nested().size());
        assertNotNull(spec.nested().get(0).constraint());
    }

    @Test
    void testWithMultipleNestedRelations() {
        Relation<Post, User> postAuthor = Relation.belongsTo(POSTS, USERS, "user_id", "id", false, false);

        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS)
                .with(POST_COMMENTS)
                .with(postAuthor);

        assertEquals(2, spec.nested().size());
    }

    // ==================== hasConstraint() ====================

    @Test
    void testHasConstraintTrue() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS, b -> b.limit(5));
        assertTrue(spec.hasConstraint());
    }

    @Test
    void testHasConstraintFalse() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);
        assertFalse(spec.hasConstraint());
    }

    // ==================== hasNested() ====================

    @Test
    void testHasNestedTrue() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS)
                .with(POST_COMMENTS);
        assertTrue(spec.hasNested());
    }

    @Test
    void testHasNestedFalse() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);
        assertFalse(spec.hasNested());
    }

    // ==================== toString() ====================

    @Test
    void testToStringBasic() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);
        String str = spec.toString();

        assertTrue(str.contains("EagerLoadSpec"));
        assertTrue(str.contains("posts"));
        assertFalse(str.contains("constrained"));
        assertFalse(str.contains("nested="));
    }

    @Test
    void testToStringWithConstraint() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS, b -> b.limit(5));
        String str = spec.toString();

        assertTrue(str.contains("constrained=true"));
    }

    @Test
    void testToStringWithNested() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS)
                .with(POST_COMMENTS);
        String str = spec.toString();

        assertTrue(str.contains("nested=1"));
    }

    @Test
    void testToStringWithConstraintAndNested() {
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS, b -> b.limit(5))
                .with(POST_COMMENTS);
        String str = spec.toString();

        assertTrue(str.contains("constrained=true"));
        assertTrue(str.contains("nested=1"));
    }

    // ==================== Record Accessor Methods ====================

    @Test
    void testRecordAccessors() {
        List<EagerLoadSpec> nestedList = new ArrayList<>();
        nestedList.add(EagerLoadSpec.of(POST_COMMENTS));

        EagerLoadSpec spec = new EagerLoadSpec(USER_POSTS, b -> b.limit(5), nestedList);

        assertEquals(USER_POSTS, spec.relation());
        assertNotNull(spec.constraint());
        assertEquals(1, spec.nested().size());
    }
}
