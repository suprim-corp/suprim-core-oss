package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Entity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Relation class.
 */
class RelationTest {

    @Entity(table = "users")
    static class User {}

    @Entity(table = "posts")
    static class Post {}

    @Entity(table = "comments")
    static class Comment {}

    @Entity(table = "tags")
    static class Tag {}

    @Entity(table = "images")
    static class Image {}

    @Entity(table = "orders")
    static class Order {}

    @Entity(table = "bids")
    static class Bid {}

    private static final Table<User> USERS = Table.of("users", User.class);
    private static final Table<Post> POSTS = Table.of("posts", Post.class);
    private static final Table<Comment> COMMENTS = Table.of("comments", Comment.class);
    private static final Table<Tag> TAGS = Table.of("tags", Tag.class);
    private static final Table<Image> IMAGES = Table.of("images", Image.class);
    private static final Table<Order> ORDERS = Table.of("orders", Order.class);
    private static final Table<Bid> BIDS = Table.of("bids", Bid.class);

    // ==================== Factory Methods - hasOne ====================

    @Test
    void testHasOneWithFieldName() {
        Relation<User, Post> relation = Relation.hasOne(
                USERS, POSTS, "user_id", "id", false, true, "latestPost"
        );

        assertEquals(Relation.Type.HAS_ONE, relation.getType());
        assertEquals("user_id", relation.getForeignKey());
        assertEquals("id", relation.getLocalKey());
        assertEquals("latestPost", relation.getFieldName());
        assertFalse(relation.isNoForeignKey());
        assertTrue(relation.isEager());
    }

    @Test
    void testHasOneWithoutFieldName() {
        Relation<User, Post> relation = Relation.hasOne(
                USERS, POSTS, "user_id", "id", true, false
        );

        assertEquals(Relation.Type.HAS_ONE, relation.getType());
        assertTrue(relation.isNoForeignKey());
        assertNull(relation.getFieldName());
    }

    // ==================== Factory Methods - belongsTo ====================

    @Test
    void testBelongsToWithFieldName() {
        Relation<Post, User> relation = Relation.belongsTo(
                POSTS, USERS, "user_id", "id", false, true, "author"
        );

        assertEquals(Relation.Type.BELONGS_TO, relation.getType());
        assertEquals("user_id", relation.getForeignKey());
        assertEquals("id", relation.getRelatedKey());
        assertEquals("author", relation.getFieldName());
    }

    @Test
    void testBelongsToWithoutFieldName() {
        Relation<Post, User> relation = Relation.belongsTo(
                POSTS, USERS, "user_id", "id", false, false
        );

        assertEquals(Relation.Type.BELONGS_TO, relation.getType());
        assertNull(relation.getFieldName());
    }

    // ==================== Factory Methods - hasOneThrough ====================

    @Test
    void testHasOneThroughWithFieldName() {
        Relation<User, Comment> relation = Relation.hasOneThrough(
                USERS, COMMENTS, POSTS, "user_id", "post_id", "id", "id", true, "latestComment"
        );

        assertEquals(Relation.Type.HAS_ONE_THROUGH, relation.getType());
        assertEquals("user_id", relation.getFirstKey());
        assertEquals("post_id", relation.getSecondKey());
        assertEquals("id", relation.getLocalKey());
        assertEquals("id", relation.getSecondLocalKey());
        assertEquals("latestComment", relation.getFieldName());
        assertEquals(POSTS, relation.getThroughTable());
    }

    @Test
    void testHasOneThroughWithoutFieldName() {
        Relation<User, Comment> relation = Relation.hasOneThrough(
                USERS, COMMENTS, POSTS, "user_id", "post_id", "id", "id", false
        );

        assertNull(relation.getFieldName());
    }

    // ==================== Factory Methods - latestOfMany ====================

    @Test
    void testLatestOfManyWithFieldName() {
        Relation<User, Order> relation = Relation.latestOfMany(
                USERS, ORDERS, "user_id", "id", "created_at", true, "latestOrder"
        );

        assertEquals(Relation.Type.LATEST_OF_MANY, relation.getType());
        assertEquals("created_at", relation.getOrderColumn());
        assertEquals("latestOrder", relation.getFieldName());
    }

    @Test
    void testLatestOfManyWithoutFieldName() {
        Relation<User, Order> relation = Relation.latestOfMany(
                USERS, ORDERS, "user_id", "id", "created_at", false
        );

        assertNull(relation.getFieldName());
    }

    // ==================== Factory Methods - ofMany ====================

    @Test
    void testOfManyWithFieldName() {
        Relation<User, Bid> relation = Relation.ofMany(
                USERS, BIDS, "user_id", "id", "amount", "MAX", true, "highestBid"
        );

        assertEquals(Relation.Type.OF_MANY, relation.getType());
        assertEquals("amount", relation.getAggregateColumn());
        assertEquals("MAX", relation.getAggregateFunction());
        assertEquals("highestBid", relation.getFieldName());
    }

    @Test
    void testOfManyWithoutFieldName() {
        Relation<User, Bid> relation = Relation.ofMany(
                USERS, BIDS, "user_id", "id", "amount", "MIN", false
        );

        assertNull(relation.getFieldName());
    }

    // ==================== Getters ====================

    @Test
    void testGetOwnerTable() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        assertEquals(USERS, relation.getOwnerTable());
    }

    @Test
    void testGetForeignKey() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        assertEquals("user_id", relation.getForeignKey());
    }

    @Test
    void testGetLocalKey() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        assertEquals("id", relation.getLocalKey());
    }

    @Test
    void testGetRelatedKey() {
        Relation<Post, User> relation = Relation.belongsTo(POSTS, USERS, "user_id", "id", false, false);
        assertEquals("id", relation.getRelatedKey());
    }

    @Test
    void testGetPivotTable() {
        Relation<User, Tag> relation = Relation.belongsToMany(
                USERS, TAGS, "user_tag", "user_id", "tag_id", "id", "id", List.of(), false, false
        );
        assertEquals("user_tag", relation.getPivotTable());
    }

    @Test
    void testGetForeignPivotKey() {
        Relation<User, Tag> relation = Relation.belongsToMany(
                USERS, TAGS, "user_tag", "user_id", "tag_id", "id", "id", List.of(), false, false
        );
        assertEquals("user_id", relation.getForeignPivotKey());
    }

    @Test
    void testGetRelatedPivotKey() {
        Relation<User, Tag> relation = Relation.belongsToMany(
                USERS, TAGS, "user_tag", "user_id", "tag_id", "id", "id", List.of(), false, false
        );
        assertEquals("tag_id", relation.getRelatedPivotKey());
    }

    @Test
    void testIsNoForeignKey() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", true, false);
        assertTrue(relation.isNoForeignKey());
    }

    @Test
    void testGetFieldName() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false, "posts");
        assertEquals("posts", relation.getFieldName());
    }

    @Test
    void testShouldTouch() {
        Relation<Post, User> relation = Relation.belongsTo(
                POSTS, USERS, "user_id", "id", false, false, "author", true, List.of("updated_at")
        );
        assertTrue(relation.shouldTouch());
    }

    @Test
    void testHasDefault() {
        Relation<User, Post> relation = Relation.hasOne(
                USERS, POSTS, "user_id", "id", false, false, "post", true, List.of("title")
        );
        assertTrue(relation.hasDefault());
    }

    // ==================== isToMany ====================

    @Test
    void testIsToMany_HAS_MANY() {
        Relation<User, Post> relation = Relation.hasMany(USERS, POSTS, "user_id", "id", false, false);
        assertTrue(relation.isToMany());
    }

    @Test
    void testIsToMany_BELONGS_TO_MANY() {
        Relation<User, Tag> relation = Relation.belongsToMany(
                USERS, TAGS, "user_tag", "user_id", "tag_id", "id", "id", List.of(), false, false
        );
        assertTrue(relation.isToMany());
    }

    @Test
    void testIsToMany_HAS_MANY_THROUGH() {
        Relation<User, Comment> relation = Relation.hasManyThrough(
                USERS, COMMENTS, POSTS, "user_id", "post_id", "id", "id", false
        );
        assertTrue(relation.isToMany());
    }

    @Test
    void testIsToMany_MORPH_MANY() {
        Relation<Post, Comment> relation = Relation.morphMany(
                POSTS, COMMENTS, "commentable", "commentable_type", "commentable_id", "id", false
        );
        assertTrue(relation.isToMany());
    }

    @Test
    void testIsToMany_MORPH_TO_MANY() {
        Relation<Post, Tag> relation = Relation.morphToMany(
                POSTS, TAGS, "taggables", "taggable", "taggable_type", "taggable_id", "tag_id",
                "id", "id", List.of(), false, false
        );
        assertTrue(relation.isToMany());
    }

    @Test
    void testIsToMany_MORPHED_BY_MANY() {
        Relation<Tag, Post> relation = Relation.morphedByMany(
                TAGS, POSTS, "taggables", "taggable", "taggable_type", "taggable_id", "tag_id",
                "id", "id", List.of(), false, false
        );
        assertTrue(relation.isToMany());
    }

    @Test
    void testIsToMany_HAS_ONE_returns_false() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        assertFalse(relation.isToMany());
    }

    // ==================== usesPivotTable ====================

    @Test
    void testUsesPivotTable_BELONGS_TO_MANY() {
        Relation<User, Tag> relation = Relation.belongsToMany(
                USERS, TAGS, "user_tag", "user_id", "tag_id", "id", "id", List.of(), false, false
        );
        assertTrue(relation.usesPivotTable());
    }

    @Test
    void testUsesPivotTable_MORPH_TO_MANY() {
        Relation<Post, Tag> relation = Relation.morphToMany(
                POSTS, TAGS, "taggables", "taggable", "taggable_type", "taggable_id", "tag_id",
                "id", "id", List.of(), false, false
        );
        assertTrue(relation.usesPivotTable());
    }

    @Test
    void testUsesPivotTable_MORPHED_BY_MANY() {
        Relation<Tag, Post> relation = Relation.morphedByMany(
                TAGS, POSTS, "taggables", "taggable", "taggable_type", "taggable_id", "tag_id",
                "id", "id", List.of(), false, false
        );
        assertTrue(relation.usesPivotTable());
    }

    @Test
    void testUsesPivotTable_HAS_ONE_returns_false() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        assertFalse(relation.usesPivotTable());
    }

    // ==================== requiresLimitOne ====================

    @Test
    void testRequiresLimitOne_LATEST_OF_MANY() {
        Relation<User, Order> relation = Relation.latestOfMany(USERS, ORDERS, "user_id", "id", "created_at", false);
        assertTrue(relation.requiresLimitOne());
    }

    @Test
    void testRequiresLimitOne_OF_MANY() {
        Relation<User, Bid> relation = Relation.ofMany(USERS, BIDS, "user_id", "id", "amount", "MAX", false);
        assertTrue(relation.requiresLimitOne());
    }

    @Test
    void testRequiresLimitOne_HAS_ONE_returns_false() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        assertFalse(relation.requiresLimitOne());
    }

    // ==================== getOfManyOrderBy ====================

    @Test
    void testGetOfManyOrderBy_LATEST_OF_MANY() {
        Relation<User, Order> relation = Relation.latestOfMany(USERS, ORDERS, "user_id", "id", "created_at", false);
        String orderBy = relation.getOfManyOrderBy();
        assertTrue(orderBy.contains("orders.created_at DESC"));
    }

    @Test
    void testGetOfManyOrderBy_OLDEST_OF_MANY() {
        Relation<User, Order> relation = Relation.oldestOfMany(USERS, ORDERS, "user_id", "id", "created_at", false);
        String orderBy = relation.getOfManyOrderBy();
        assertTrue(orderBy.contains("orders.created_at ASC"));
    }

    @Test
    void testGetOfManyOrderBy_OF_MANY_MAX() {
        Relation<User, Bid> relation = Relation.ofMany(USERS, BIDS, "user_id", "id", "amount", "MAX", false);
        String orderBy = relation.getOfManyOrderBy();
        assertTrue(orderBy.contains("bids.amount DESC"));
    }

    @Test
    void testGetOfManyOrderBy_OF_MANY_MIN() {
        Relation<User, Bid> relation = Relation.ofMany(USERS, BIDS, "user_id", "id", "amount", "MIN", false);
        String orderBy = relation.getOfManyOrderBy();
        assertTrue(orderBy.contains("bids.amount ASC"));
    }

    @Test
    void testGetOfManyOrderBy_HAS_ONE_returns_null() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        assertNull(relation.getOfManyOrderBy());
    }

    // ==================== getExistsCondition ====================

    @Test
    void testGetExistsCondition_HAS_ONE() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        String condition = relation.getExistsCondition("u");
        assertEquals("posts.user_id = u.id", condition);
    }

    @Test
    void testGetExistsCondition_BELONGS_TO() {
        Relation<Post, User> relation = Relation.belongsTo(POSTS, USERS, "user_id", "id", false, false);
        String condition = relation.getExistsCondition("p");
        assertEquals("p.user_id = users.id", condition);
    }

    @Test
    void testGetExistsCondition_BELONGS_TO_MANY() {
        Relation<User, Tag> relation = Relation.belongsToMany(
                USERS, TAGS, "user_tag", "user_id", "tag_id", "id", "id", List.of(), false, false
        );
        String condition = relation.getExistsCondition("u");
        assertEquals("user_tag.user_id = u.id", condition);
    }

    @Test
    void testGetExistsCondition_HAS_ONE_THROUGH() {
        Relation<User, Comment> relation = Relation.hasOneThrough(
                USERS, COMMENTS, POSTS, "user_id", "post_id", "id", "id", false
        );
        String condition = relation.getExistsCondition("u");
        assertEquals("posts.user_id = u.id", condition);
    }

    @Test
    void testGetExistsCondition_MORPH_ONE() {
        Relation<User, Image> relation = Relation.morphOne(
                USERS, IMAGES, "imageable", "imageable_type", "imageable_id", "id", false
        );
        String condition = relation.getExistsCondition("u");
        assertTrue(condition.contains("images.imageable_id = u.id"));
        assertTrue(condition.contains("images.imageable_type = 'User'"));
    }

    @Test
    void testGetExistsCondition_MORPH_TO() {
        Relation<Image, Object> relation = Relation.morphTo(
                IMAGES, "imageable", "imageable_type", "imageable_id", false
        );
        String condition = relation.getExistsCondition("i");
        assertTrue(condition.contains("i.imageable_id"));
    }

    @Test
    void testGetExistsCondition_MORPH_TO_MANY() {
        Relation<Post, Tag> relation = Relation.morphToMany(
                POSTS, TAGS, "taggables", "taggable", "taggable_type", "taggable_id", "tag_id",
                "id", "id", List.of(), false, false
        );
        String condition = relation.getExistsCondition("p");
        assertTrue(condition.contains("taggables.taggable_id = p.id"));
        assertTrue(condition.contains("taggables.taggable_type = 'Post'"));
    }

    @Test
    void testGetExistsCondition_MORPHED_BY_MANY() {
        Relation<Tag, Post> relation = Relation.morphedByMany(
                TAGS, POSTS, "taggables", "taggable", "taggable_type", "taggable_id", "tag_id",
                "id", "id", List.of(), false, false
        );
        String condition = relation.getExistsCondition("t");
        assertTrue(condition.contains("taggables.tag_id = t.id"));
        assertTrue(condition.contains("taggables.taggable_type = 'Post'"));
    }

    @Test
    void testGetExistsCondition_LATEST_OF_MANY() {
        Relation<User, Order> relation = Relation.latestOfMany(USERS, ORDERS, "user_id", "id", "created_at", false);
        String condition = relation.getExistsCondition("u");
        assertEquals("orders.user_id = u.id", condition);
    }

    // ==================== getPivotJoinForExists ====================

    @Test
    void testGetPivotJoinForExists_BELONGS_TO_MANY() {
        Relation<User, Tag> relation = Relation.belongsToMany(
                USERS, TAGS, "user_tag", "user_id", "tag_id", "id", "id", List.of(), false, false
        );
        String join = relation.getPivotJoinForExists();
        assertTrue(join.contains("JOIN user_tag ON user_tag.tag_id = tags.id"));
    }

    @Test
    void testGetPivotJoinForExists_MORPH_TO_MANY() {
        Relation<Post, Tag> relation = Relation.morphToMany(
                POSTS, TAGS, "taggables", "taggable", "taggable_type", "taggable_id", "tag_id",
                "id", "id", List.of(), false, false
        );
        String join = relation.getPivotJoinForExists();
        assertTrue(join.contains("JOIN taggables ON taggables.tag_id = tags.id"));
    }

    @Test
    void testGetPivotJoinForExists_MORPHED_BY_MANY() {
        Relation<Tag, Post> relation = Relation.morphedByMany(
                TAGS, POSTS, "taggables", "taggable", "taggable_type", "taggable_id", "tag_id",
                "id", "id", List.of(), false, false
        );
        String join = relation.getPivotJoinForExists();
        assertTrue(join.contains("JOIN taggables ON taggables.taggable_id = posts.id"));
        assertTrue(join.contains("taggables.taggable_type = 'Post'"));
    }

    @Test
    void testGetPivotJoinForExists_HAS_ONE_THROUGH() {
        Relation<User, Comment> relation = Relation.hasOneThrough(
                USERS, COMMENTS, POSTS, "user_id", "post_id", "id", "id", false
        );
        String join = relation.getPivotJoinForExists();
        assertTrue(join.contains("JOIN posts ON comments.post_id = posts.id"));
    }

    @Test
    void testGetPivotJoinForExists_HAS_ONE_returns_null() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        assertNull(relation.getPivotJoinForExists());
    }

    // ==================== isThrough (Type method) ====================

    @Test
    void testIsThrough_HAS_ONE_THROUGH() {
        Relation<User, Comment> relation = Relation.hasOneThrough(
                USERS, COMMENTS, POSTS, "user_id", "post_id", "id", "id", false
        );
        assertTrue(relation.isThrough());
    }

    @Test
    void testIsThrough_HAS_MANY_THROUGH() {
        Relation<User, Comment> relation = Relation.hasManyThrough(
                USERS, COMMENTS, POSTS, "user_id", "post_id", "id", "id", false
        );
        assertTrue(relation.isThrough());
    }

    @Test
    void testIsThrough_HAS_ONE_returns_false() {
        Relation<User, Post> relation = Relation.hasOne(USERS, POSTS, "user_id", "id", false, false);
        assertFalse(relation.isThrough());
    }
}
