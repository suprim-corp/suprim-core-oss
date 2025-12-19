package sant1ago.dev.suprim.jdbc.eager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;
import sant1ago.dev.suprim.jdbc.DefaultModelRegistry;
import sant1ago.dev.suprim.jdbc.ReflectionUtils;
import sant1ago.dev.suprim.jdbc.exception.MappingException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RelationPopulatorTest {

    // Test entity tables
    private static final Table<User> USERS = Table.of("users", User.class);
    private static final Table<Post> POSTS = Table.of("posts", Post.class);
    private static final Table<Comment> COMMENTS = Table.of("comments", Comment.class);
    private static final Table<Profile> PROFILES = Table.of("profiles", Profile.class);
    private static final Table<Role> ROLES = Table.of("roles", Role.class);
    private static final Table<Tag> TAGS = Table.of("tags", Tag.class);

    @BeforeEach
    void setUp() {
        ReflectionUtils.clearCaches();
    }

    @AfterEach
    void tearDown() {
        ReflectionUtils.clearCaches();
    }

    // ==================== populate() - Empty/Null Cases ====================

    @Test
    void populate_emptyParents_doesNothing() {
        List<User> parents = Collections.emptyList();
        List<Post> related = List.of(createPost(1L, 1L));
        Relation<User, Post> relation = Relation.hasMany(USERS, POSTS, "user_id", "id", false, false, "posts");

        // Should not throw
        RelationPopulator.populate(parents, related, relation);
    }

    @Test
    void populate_nullFieldName_doesNothing() {
        User user = createUser(1L);
        List<User> parents = List.of(user);
        List<Post> related = List.of(createPost(1L, 1L));

        // Relation with null fieldName
        Relation<User, Post> relation = Relation.hasMany(USERS, POSTS, "user_id", "id", false, false);

        // Should not throw and should not modify parent
        RelationPopulator.populate(parents, related, relation);
    }

    // ==================== populate() - Single Relations (HasOne, BelongsTo) ====================

    @Test
    void populate_hasOne_setsSingleRelatedEntity() {
        User user = createUser(1L);
        Profile profile = createProfile(1L, 1L);

        List<User> parents = List.of(user);
        List<Profile> related = List.of(profile);
        Relation<User, Profile> relation = Relation.hasOne(USERS, PROFILES, "userId", "id", false, false, "profile");

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getProfile());
        assertEquals(1L, user.getProfile().getId());
    }

    @Test
    void populate_hasOne_multipleParents_matchesCorrectly() {
        User user1 = createUser(1L);
        User user2 = createUser(2L);
        Profile profile1 = createProfile(1L, 1L);
        Profile profile2 = createProfile(2L, 2L);

        List<User> parents = List.of(user1, user2);
        List<Profile> related = List.of(profile1, profile2);
        Relation<User, Profile> relation = Relation.hasOne(USERS, PROFILES, "userId", "id", false, false, "profile");

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user1.getProfile());
        assertEquals(1L, user1.getProfile().getUserId());
        assertNotNull(user2.getProfile());
        assertEquals(2L, user2.getProfile().getUserId());
    }

    @Test
    void populate_hasOne_noMatchingRelated_setsNull() {
        User user = createUser(1L);
        Profile profile = createProfile(1L, 99L); // Different userId

        List<User> parents = List.of(user);
        List<Profile> related = List.of(profile);
        Relation<User, Profile> relation = Relation.hasOne(USERS, PROFILES, "userId", "id", false, false, "profile");

        RelationPopulator.populate(parents, related, relation);

        assertNull(user.getProfile());
    }

    @Test
    void populate_belongsTo_setsParentEntity() {
        Post post = createPost(1L, 1L);
        User user = createUser(1L);

        List<Post> parents = List.of(post);
        List<User> related = List.of(user);
        Relation<Post, User> relation = Relation.belongsTo(POSTS, USERS, "userId", "id", false, false, "author");

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(post.getAuthor());
        assertEquals(1L, post.getAuthor().getId());
    }

    @Test
    void populate_belongsTo_multipleParents_matchesCorrectly() {
        Post post1 = createPost(1L, 1L);
        Post post2 = createPost(2L, 2L);
        User user1 = createUser(1L);
        User user2 = createUser(2L);

        List<Post> parents = List.of(post1, post2);
        List<User> related = List.of(user1, user2);
        Relation<Post, User> relation = Relation.belongsTo(POSTS, USERS, "userId", "id", false, false, "author");

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(post1.getAuthor());
        assertEquals(1L, post1.getAuthor().getId());
        assertNotNull(post2.getAuthor());
        assertEquals(2L, post2.getAuthor().getId());
    }

    // ==================== populate() - Collection Relations (HasMany, BelongsToMany) ====================

    @Test
    void populate_hasMany_setsListOfRelatedEntities() {
        User user = createUser(1L);
        Post post1 = createPost(1L, 1L);
        Post post2 = createPost(2L, 1L);

        List<User> parents = List.of(user);
        List<Post> related = List.of(post1, post2);
        Relation<User, Post> relation = Relation.hasMany(USERS, POSTS, "userId", "id", false, false, "posts");

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getPosts());
        assertEquals(2, user.getPosts().size());
    }

    @Test
    void populate_hasMany_multipleParents_groupsCorrectly() {
        User user1 = createUser(1L);
        User user2 = createUser(2L);
        Post post1 = createPost(1L, 1L);
        Post post2 = createPost(2L, 1L);
        Post post3 = createPost(3L, 2L);

        List<User> parents = List.of(user1, user2);
        List<Post> related = List.of(post1, post2, post3);
        Relation<User, Post> relation = Relation.hasMany(USERS, POSTS, "userId", "id", false, false, "posts");

        RelationPopulator.populate(parents, related, relation);

        assertEquals(2, user1.getPosts().size());
        assertEquals(1, user2.getPosts().size());
    }

    @Test
    void populate_hasMany_noMatchingRelated_setsEmptyList() {
        User user = createUser(1L);
        Post post = createPost(1L, 99L); // Different userId

        List<User> parents = List.of(user);
        List<Post> related = List.of(post);
        Relation<User, Post> relation = Relation.hasMany(USERS, POSTS, "userId", "id", false, false, "posts");

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getPosts());
        assertTrue(user.getPosts().isEmpty());
    }

    @Test
    void populate_belongsToMany_matchesByRelatedKey() {
        // NOTE: BelongsToMany is properly handled by EagerLoader which queries pivot table.
        // RelationPopulator alone can only match when parent.localKey == related.relatedKey.
        // This test verifies the matching behavior without pivot table context.
        User user = createUser(1L);
        Role role1 = createRole(1L); // Will match because role.id == user.id
        Role role2 = createRole(2L); // Won't match because role.id != user.id

        List<User> parents = List.of(user);
        List<Role> related = List.of(role1, role2);
        Relation<User, Role> relation = Relation.belongsToMany(
                USERS, ROLES, "user_roles", "user_id", "role_id", "id", "id",
                List.of(), false, false, "roles"
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getRoles());
        // Only role with id=1 matches user with id=1 (without pivot table context)
        assertEquals(1, user.getRoles().size());
        assertEquals(1L, user.getRoles().get(0).getId());
    }

    @Test
    void populate_collectionField_isSet_populatesAsSet() {
        // NOTE: BelongsToMany is properly handled by EagerLoader which queries pivot table.
        // This test verifies Set field type is honored even with partial matching.
        User user = createUser(1L);
        Tag tag1 = createTag(1L); // Will match because tag.id == user.id

        List<User> parents = List.of(user);
        List<Tag> related = List.of(tag1);
        Relation<User, Tag> relation = Relation.belongsToMany(
                USERS, TAGS, "user_tags", "user_id", "tag_id", "id", "id",
                List.of(), false, false, "tags"
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getTags());
        assertTrue(user.getTags() instanceof Set);
        assertEquals(1, user.getTags().size());
    }

    // ==================== populate() - Default Model Support ====================

    @Test
    void populate_hasOne_withDefault_createsDefaultInstance() {
        User user = createUser(1L);

        List<User> parents = List.of(user);
        List<Profile> related = Collections.emptyList(); // No related entities
        Relation<User, Profile> relation = Relation.hasOne(
                USERS, PROFILES, "userId", "id", false, false, "profile",
                true, List.of() // withDefault=true
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getProfile());
        assertTrue(DefaultModelRegistry.isDefault(user.getProfile()));
    }

    @Test
    void populate_hasOne_withDefaultAttributes_appliesAttributes() {
        User user = createUser(1L);

        List<User> parents = List.of(user);
        List<Profile> related = Collections.emptyList();
        Relation<User, Profile> relation = Relation.hasOne(
                USERS, PROFILES, "userId", "id", false, false, "profile",
                true, List.of("bio=Default Bio") // withDefault=true with attributes
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getProfile());
        assertEquals("Default Bio", user.getProfile().getBio());
        assertTrue(DefaultModelRegistry.isDefault(user.getProfile()));
    }

    @Test
    void populate_belongsTo_withDefault_createsDefaultInstance() {
        Post post = createPost(1L, null); // No userId

        List<Post> parents = List.of(post);
        List<User> related = Collections.emptyList();
        Relation<Post, User> relation = Relation.belongsTo(
                POSTS, USERS, "userId", "id", false, false, "author",
                false, List.of("updated_at"), true, List.of() // withDefault=true
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(post.getAuthor());
        assertTrue(DefaultModelRegistry.isDefault(post.getAuthor()));
    }

    // ==================== populate() - Through Relations ====================

    @Test
    void populate_hasOneThrough_setsSingleRelatedEntity() {
        Country country = createCountry(1L);
        Post post = createPost(1L, 1L);
        // Simulate post being accessible through user
        post.setUserId(1L); // Through user's country_id

        List<Country> parents = List.of(country);
        List<Post> related = List.of(post);
        Relation<Country, Post> relation = Relation.hasOneThrough(
                Table.of("countries", Country.class),
                POSTS,
                USERS,
                "country_id", // firstKey on users
                "user_id",    // secondKey on posts
                "id",         // localKey on countries
                "id",         // secondLocalKey on users
                false,
                "latestPost"
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(country.getLatestPost());
    }

    @Test
    void populate_hasManyThrough_matchesByRelatedKey() {
        // NOTE: HasManyThrough is properly handled by EagerLoader which queries through table.
        // RelationPopulator alone can only match when parent.localKey == related.relatedKey.
        Country country = createCountry(1L);
        Post post1 = createPost(1L, 1L); // Will match because post.id == country.id
        Post post2 = createPost(2L, 1L); // Won't match because post.id != country.id

        List<Country> parents = List.of(country);
        List<Post> related = List.of(post1, post2);
        Relation<Country, Post> relation = Relation.hasManyThrough(
                Table.of("countries", Country.class),
                POSTS,
                USERS,
                "country_id",
                "user_id",
                "id",
                "id",
                false,
                "posts"
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(country.getPosts());
        // Only post with id=1 matches country with id=1 (without through table context)
        assertEquals(1, country.getPosts().size());
        assertEquals(1L, country.getPosts().get(0).getId());
    }

    // ==================== populate() - OfMany Relations ====================

    @Test
    void populate_latestOfMany_setsSingleRelatedEntity() {
        User user = createUser(1L);
        Post latestPost = createPost(1L, 1L);

        List<User> parents = List.of(user);
        List<Post> related = List.of(latestPost);
        Relation<User, Post> relation = Relation.latestOfMany(
                USERS, POSTS, "userId", "id", "created_at", false, "latestPost"
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getLatestPost());
        assertEquals(1L, user.getLatestPost().getId());
    }

    @Test
    void populate_oldestOfMany_setsSingleRelatedEntity() {
        User user = createUser(1L);
        Post oldestPost = createPost(1L, 1L);

        List<User> parents = List.of(user);
        List<Post> related = List.of(oldestPost);
        Relation<User, Post> relation = Relation.oldestOfMany(
                USERS, POSTS, "userId", "id", "created_at", false, "oldestPost"
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getOldestPost());
        assertEquals(1L, user.getOldestPost().getId());
    }

    @Test
    void populate_ofMany_setsSingleRelatedEntity() {
        User user = createUser(1L);
        Post highestPricePost = createPost(1L, 1L);

        List<User> parents = List.of(user);
        List<Post> related = List.of(highestPricePost);
        Relation<User, Post> relation = Relation.ofMany(
                USERS, POSTS, "userId", "id", "price", "MAX", false, "highestPricePost"
        );

        RelationPopulator.populate(parents, related, relation);

        assertNotNull(user.getHighestPricePost());
    }

    // ==================== populate() - Polymorphic Relations ====================

    @Test
    void populate_morphOne_withNullFieldName_noOp() {
        // Morph relations without fieldName return early from populate
        User user = createUser(1L);
        Comment comment = createComment(1L, 1L);

        List<User> parents = List.of(user);
        List<Comment> related = List.of(comment);
        Relation<User, Comment> relation = Relation.morphOne(
                USERS, COMMENTS, "commentable", "commentable_type", "commentable_id", "id", false
        );

        // Should not throw - returns early due to null fieldName
        RelationPopulator.populate(parents, related, relation);
    }

    @Test
    void populate_morphMany_withNullFieldName_noOp() {
        // Morph relations without fieldName return early from populate
        User user = createUser(1L);
        Comment comment = createComment(1L, 1L);

        List<User> parents = List.of(user);
        List<Comment> related = List.of(comment);
        Relation<User, Comment> relation = Relation.morphMany(
                USERS, COMMENTS, "commentable", "commentable_type", "commentable_id", "id", false
        );

        // Should not throw - returns early due to null fieldName
        RelationPopulator.populate(parents, related, relation);
    }

    // ==================== populate() - Error Cases ====================

    @Test
    void populate_nonExistentField_throwsMappingException() {
        User user = createUser(1L);
        Profile profile = createProfile(1L, 1L);

        List<User> parents = List.of(user);
        List<Profile> related = List.of(profile);
        // Field name that doesn't exist on User
        Relation<User, Profile> relation = Relation.hasOne(
                USERS, PROFILES, "userId", "id", false, false, "nonExistentField"
        );

        ReflectionUtils.setStrictMode(true);
        try {
            assertThrows(MappingException.class, () ->
                    RelationPopulator.populate(parents, related, relation));
        } finally {
            ReflectionUtils.setStrictMode(false);
        }
    }

    @Test
    void populate_nullKeyValue_skipsRelated() {
        User user = createUser(1L);
        Profile profile = createProfile(1L, null); // Null userId

        List<User> parents = List.of(user);
        List<Profile> related = List.of(profile);
        Relation<User, Profile> relation = Relation.hasOne(USERS, PROFILES, "userId", "id", false, false, "profile");

        RelationPopulator.populate(parents, related, relation);

        assertNull(user.getProfile());
    }

    @Test
    void populate_nullParentKeyValue_skipsParent() {
        User user = createUser(null); // Null ID
        Profile profile = createProfile(1L, 1L);

        List<User> parents = List.of(user);
        List<Profile> related = List.of(profile);
        Relation<User, Profile> relation = Relation.hasOne(USERS, PROFILES, "userId", "id", false, false, "profile");

        RelationPopulator.populate(parents, related, relation);

        assertNull(user.getProfile());
    }

    // ==================== Helper Methods ====================

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Post createPost(Long id, Long userId) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(userId);
        return post;
    }

    private Comment createComment(Long id, Long postId) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setPostId(postId);
        return comment;
    }

    private Profile createProfile(Long id, Long userId) {
        Profile profile = new Profile();
        profile.setId(id);
        profile.setUserId(userId);
        return profile;
    }

    private Role createRole(Long id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }

    private Tag createTag(Long id) {
        Tag tag = new Tag();
        tag.setId(id);
        return tag;
    }

    private Country createCountry(Long id) {
        Country country = new Country();
        country.setId(id);
        return country;
    }

    // ==================== Test Entities ====================

    @Entity(table = "users")
    public static class User {
        @Column(name = "id")
        private Long id;

        private Profile profile;
        private List<Post> posts = new ArrayList<>();
        private List<Role> roles = new ArrayList<>();
        private Set<Tag> tags = new HashSet<>();
        private Post latestPost;
        private Post oldestPost;
        private Post highestPricePost;

        public User() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Profile getProfile() { return profile; }
        public void setProfile(Profile profile) { this.profile = profile; }
        public List<Post> getPosts() { return posts; }
        public void setPosts(List<Post> posts) { this.posts = posts; }
        public List<Role> getRoles() { return roles; }
        public void setRoles(List<Role> roles) { this.roles = roles; }
        public Set<Tag> getTags() { return tags; }
        public void setTags(Set<Tag> tags) { this.tags = tags; }
        public Post getLatestPost() { return latestPost; }
        public void setLatestPost(Post latestPost) { this.latestPost = latestPost; }
        public Post getOldestPost() { return oldestPost; }
        public void setOldestPost(Post oldestPost) { this.oldestPost = oldestPost; }
        public Post getHighestPricePost() { return highestPricePost; }
        public void setHighestPricePost(Post highestPricePost) { this.highestPricePost = highestPricePost; }
    }

    @Entity(table = "posts")
    public static class Post {
        @Column(name = "id")
        private Long id;
        @Column(name = "user_id")
        private Long userId;

        private User author;
        private List<Comment> comments = new ArrayList<>();

        public Post() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public User getAuthor() { return author; }
        public void setAuthor(User author) { this.author = author; }
        public List<Comment> getComments() { return comments; }
        public void setComments(List<Comment> comments) { this.comments = comments; }
    }

    @Entity(table = "comments")
    public static class Comment {
        @Column(name = "id")
        private Long id;
        @Column(name = "post_id")
        private Long postId;

        public Comment() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPostId() { return postId; }
        public void setPostId(Long postId) { this.postId = postId; }
    }

    @Entity(table = "profiles")
    public static class Profile {
        @Column(name = "id")
        private Long id;
        @Column(name = "user_id")
        private Long userId;
        @Column(name = "bio")
        private String bio;

        public Profile() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
    }

    @Entity(table = "roles")
    public static class Role {
        @Column(name = "id")
        private Long id;

        public Role() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    @Entity(table = "tags")
    public static class Tag {
        @Column(name = "id")
        private Long id;

        public Tag() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    @Entity(table = "countries")
    public static class Country {
        @Column(name = "id")
        private Long id;

        private Post latestPost;
        private List<Post> posts = new ArrayList<>();

        public Country() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Post getLatestPost() { return latestPost; }
        public void setLatestPost(Post latestPost) { this.latestPost = latestPost; }
        public List<Post> getPosts() { return posts; }
        public void setPosts(List<Post> posts) { this.posts = posts; }
    }
}
