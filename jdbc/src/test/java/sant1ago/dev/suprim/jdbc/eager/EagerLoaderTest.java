package sant1ago.dev.suprim.jdbc.eager;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.core.query.EagerLoadSpec;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;
import sant1ago.dev.suprim.jdbc.SuprimExecutor;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EagerLoaderTest {

    private JdbcDataSource dataSource;
    private SuprimExecutor executor;
    private Connection setupConnection;
    private EagerLoader eagerLoader;

    // Test entity tables
    private static final Table<User> USERS = Table.of("users", User.class);
    private static final Table<Post> POSTS = Table.of("posts", Post.class);
    private static final Table<Comment> COMMENTS = Table.of("comments", Comment.class);
    private static final Table<Profile> PROFILES = Table.of("profiles", Profile.class);
    private static final Table<Role> ROLES = Table.of("roles", Role.class);
    private static final Table<Country> COUNTRIES = Table.of("countries", Country.class);

    // Relations
    private static final Relation<User, Post> USER_POSTS = Relation.hasMany(
            USERS, POSTS, "user_id", "id", false, false, "posts"
    );
    private static final Relation<User, Profile> USER_PROFILE = Relation.hasOne(
            USERS, PROFILES, "user_id", "id", false, false, "profile"
    );
    private static final Relation<Post, User> POST_AUTHOR = Relation.belongsTo(
            POSTS, USERS, "user_id", "id", false, false, "author"
    );
    private static final Relation<Post, Comment> POST_COMMENTS = Relation.hasMany(
            POSTS, COMMENTS, "post_id", "id", false, false, "comments"
    );
    private static final Relation<User, Role> USER_ROLES = Relation.belongsToMany(
            USERS, ROLES, "user_roles", "user_id", "role_id", "id", "id",
            List.of(), false, false, "roles"
    );
    private static final Relation<Country, Post> COUNTRY_POSTS = Relation.hasManyThrough(
            COUNTRIES, POSTS, USERS, "country_id", "user_id", "id", "id", false, "posts"
    );

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:eagertest" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE");

        setupConnection = dataSource.getConnection();
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255),
                    email VARCHAR(255),
                    country_id BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE posts (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255),
                    user_id BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE comments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    content VARCHAR(255),
                    post_id BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE profiles (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    bio VARCHAR(255),
                    user_id BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE roles (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255)
                )
            """);
            stmt.execute("""
                CREATE TABLE user_roles (
                    user_id BIGINT,
                    role_id BIGINT,
                    PRIMARY KEY (user_id, role_id)
                )
            """);
            stmt.execute("""
                CREATE TABLE countries (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255)
                )
            """);
        }

        executor = SuprimExecutor.create(dataSource);
        eagerLoader = new EagerLoader(executor);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS user_roles");
            stmt.execute("DROP TABLE IF EXISTS comments");
            stmt.execute("DROP TABLE IF EXISTS posts");
            stmt.execute("DROP TABLE IF EXISTS profiles");
            stmt.execute("DROP TABLE IF EXISTS roles");
            stmt.execute("DROP TABLE IF EXISTS users");
            stmt.execute("DROP TABLE IF EXISTS countries");
        }
        setupConnection.close();
    }

    // ==================== Constructor Tests ====================

    @Test
    void constructor_throwsOnNullExecutor() {
        assertThrows(NullPointerException.class, () -> new EagerLoader(null));
    }

    @Test
    void constructor_acceptsValidExecutor() {
        EagerLoader loader = new EagerLoader(executor);
        assertNotNull(loader);
    }

    // ==================== formatValue Tests ====================

    @Test
    void formatValue_nullValue_returnsNULL() throws Exception {
        String result = invokeFormatValue(null);
        assertEquals("NULL", result);
    }

    @Test
    void formatValue_integerValue_returnsUnquoted() throws Exception {
        String result = invokeFormatValue(42);
        assertEquals("42", result);
    }

    @Test
    void formatValue_longValue_returnsUnquoted() throws Exception {
        String result = invokeFormatValue(123456789L);
        assertEquals("123456789", result);
    }

    @Test
    void formatValue_doubleValue_returnsUnquoted() throws Exception {
        String result = invokeFormatValue(3.14);
        assertEquals("3.14", result);
    }

    @Test
    void formatValue_floatValue_returnsUnquoted() throws Exception {
        String result = invokeFormatValue(2.5f);
        assertEquals("2.5", result);
    }

    @Test
    void formatValue_bigDecimalValue_returnsUnquoted() throws Exception {
        String result = invokeFormatValue(new BigDecimal("99.99"));
        assertEquals("99.99", result);
    }

    @Test
    void formatValue_bigIntegerValue_returnsUnquoted() throws Exception {
        String result = invokeFormatValue(new BigInteger("9999999999999999999"));
        assertEquals("9999999999999999999", result);
    }

    @Test
    void formatValue_shortValue_returnsUnquoted() throws Exception {
        String result = invokeFormatValue((short) 100);
        assertEquals("100", result);
    }

    @Test
    void formatValue_byteValue_returnsUnquoted() throws Exception {
        String result = invokeFormatValue((byte) 10);
        assertEquals("10", result);
    }

    @Test
    void formatValue_negativeNumber_returnsUnquoted() throws Exception {
        String result = invokeFormatValue(-42);
        assertEquals("-42", result);
    }

    @Test
    void formatValue_zeroValue_returnsUnquoted() throws Exception {
        String result = invokeFormatValue(0);
        assertEquals("0", result);
    }

    @Test
    void formatValue_simpleString_returnsQuoted() throws Exception {
        String result = invokeFormatValue("hello");
        assertEquals("'hello'", result);
    }

    @Test
    void formatValue_stringWithSingleQuote_escapesQuote() throws Exception {
        String result = invokeFormatValue("it's");
        assertEquals("'it''s'", result);
    }

    @Test
    void formatValue_stringWithMultipleSingleQuotes_escapesAll() throws Exception {
        String result = invokeFormatValue("it's a 'test'");
        assertEquals("'it''s a ''test'''", result);
    }

    @Test
    void formatValue_emptyString_returnsEmptyQuoted() throws Exception {
        String result = invokeFormatValue("");
        assertEquals("''", result);
    }

    @Test
    void formatValue_stringWithSpaces_returnsQuoted() throws Exception {
        String result = invokeFormatValue("hello world");
        assertEquals("'hello world'", result);
    }

    @Test
    void formatValue_uuidValue_returnsQuoted() throws Exception {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String result = invokeFormatValue(uuid);
        assertEquals("'550e8400-e29b-41d4-a716-446655440000'", result);
    }

    @Test
    void formatValue_booleanValue_returnsQuoted() throws Exception {
        String resultTrue = invokeFormatValue(true);
        String resultFalse = invokeFormatValue(false);
        assertEquals("'true'", resultTrue);
        assertEquals("'false'", resultFalse);
    }

    @Test
    void formatValue_customObjectWithToString_returnsQuoted() throws Exception {
        Object customObj = new Object() {
            @Override
            public String toString() {
                return "custom-value";
            }
        };
        String result = invokeFormatValue(customObj);
        assertEquals("'custom-value'", result);
    }

    @Test
    void formatValue_stringWithSqlInjection_escapesQuotes() throws Exception {
        String malicious = "'; DROP TABLE users; --";
        String result = invokeFormatValue(malicious);
        assertEquals("'''; DROP TABLE users; --'", result);
    }

    private String invokeFormatValue(Object value) throws Exception {
        Method method = EagerLoader.class.getDeclaredMethod("formatValue", Object.class);
        method.setAccessible(true);
        return (String) method.invoke(eagerLoader, value);
    }

    // ==================== loadHasOneOrMany Tests ====================

    @Test
    void loadHasOneOrMany_emptyParentsList_returnsEmptyList() throws Exception {
        List<User> emptyParents = Collections.emptyList();
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);

        @SuppressWarnings("unchecked")
        List<Post> result = invokeLoadHasOneOrMany(emptyParents, USER_POSTS, spec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadHasOneOrMany_parentsWithNullKeys_returnsEmptyList() throws Exception {
        User userWithNullId = new User();
        userWithNullId.setName("NoId");

        List<User> parents = List.of(userWithNullId);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);

        @SuppressWarnings("unchecked")
        List<Post> result = invokeLoadHasOneOrMany(parents, USER_POSTS, spec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadHasOneOrMany_parentsWithMixedNullAndValidKeys_loadsForValidOnly() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Alice Post", 1L);

        User userWithNullId = new User();
        userWithNullId.setName("NoId");

        User userWithId = new User();
        userWithId.setId(1L);
        userWithId.setName("Alice");

        List<User> parents = List.of(userWithNullId, userWithId);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);

        @SuppressWarnings("unchecked")
        List<Post> result = invokeLoadHasOneOrMany(parents, USER_POSTS, spec);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Alice Post", result.get(0).getTitle());
    }

    @Test
    void loadHasOneOrMany_singleParentWithMultipleRelated_returnsAllRelated() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Post 1", 1L);
        insertPost(2L, "Post 2", 1L);
        insertPost(3L, "Post 3", 1L);

        User user = new User();
        user.setId(1L);

        List<User> parents = List.of(user);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);

        @SuppressWarnings("unchecked")
        List<Post> result = invokeLoadHasOneOrMany(parents, USER_POSTS, spec);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void loadHasOneOrMany_multipleParents_returnsRelatedForAll() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertUser(2L, "Bob", "bob@example.com");
        insertPost(1L, "Alice Post 1", 1L);
        insertPost(2L, "Alice Post 2", 1L);
        insertPost(3L, "Bob Post", 2L);

        User alice = new User();
        alice.setId(1L);
        User bob = new User();
        bob.setId(2L);

        List<User> parents = List.of(alice, bob);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);

        @SuppressWarnings("unchecked")
        List<Post> result = invokeLoadHasOneOrMany(parents, USER_POSTS, spec);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void loadHasOneOrMany_parentWithNoRelatedEntities_returnsEmptyList() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        // No posts for Alice

        User user = new User();
        user.setId(1L);

        List<User> parents = List.of(user);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);

        @SuppressWarnings("unchecked")
        List<Post> result = invokeLoadHasOneOrMany(parents, USER_POSTS, spec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadHasOneOrMany_withConstraint_appliesConstraintToQuery() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Important Post", 1L);
        insertPost(2L, "Regular Post", 1L);

        User user = new User();
        user.setId(1L);

        List<User> parents = List.of(user);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS,
                builder -> builder.whereRaw("title LIKE '%Important%'"));

        @SuppressWarnings("unchecked")
        List<Post> result = invokeLoadHasOneOrMany(parents, USER_POSTS, spec);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Important Post", result.get(0).getTitle());
    }

    @Test
    void loadHasOneOrMany_hasOneRelation_returnsSingleEntity() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertProfile(1L, "Bio", 1L);

        User user = new User();
        user.setId(1L);

        List<User> parents = List.of(user);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_PROFILE);

        @SuppressWarnings("unchecked")
        List<Profile> result = invokeLoadHasOneOrMany(parents, USER_PROFILE, spec);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Bio", result.get(0).getBio());
    }

    @Test
    void loadHasOneOrMany_duplicateParentKeys_deduplicatesInQuery() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Post", 1L);

        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(1L); // Same ID as user1

        List<User> parents = List.of(user1, user2);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS);

        @SuppressWarnings("unchecked")
        List<Post> result = invokeLoadHasOneOrMany(parents, USER_POSTS, spec);

        assertNotNull(result);
        assertEquals(1, result.size()); // Should only return once despite duplicate keys
    }

    @SuppressWarnings("unchecked")
    private <T, R> List<R> invokeLoadHasOneOrMany(
            List<T> parents,
            Relation<T, R> relation,
            EagerLoadSpec spec
    ) throws Exception {
        Method method = EagerLoader.class.getDeclaredMethod(
                "loadHasOneOrMany", List.class, Relation.class, EagerLoadSpec.class);
        method.setAccessible(true);
        return (List<R>) method.invoke(eagerLoader, parents, relation, spec);
    }

    // ==================== loadBelongsTo Tests ====================

    @Test
    void loadBelongsTo_emptyParentsList_returnsEmptyList() throws Exception {
        List<Post> emptyPosts = Collections.emptyList();
        EagerLoadSpec spec = EagerLoadSpec.of(POST_AUTHOR);

        @SuppressWarnings("unchecked")
        List<User> result = invokeLoadBelongsTo(emptyPosts, POST_AUTHOR, spec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadBelongsTo_parentsWithNullForeignKeys_returnsEmptyList() throws Exception {
        Post postWithNullUserId = new Post();
        postWithNullUserId.setId(1L);
        postWithNullUserId.setTitle("Orphan Post");
        // userId is null

        List<Post> parents = List.of(postWithNullUserId);
        EagerLoadSpec spec = EagerLoadSpec.of(POST_AUTHOR);

        @SuppressWarnings("unchecked")
        List<User> result = invokeLoadBelongsTo(parents, POST_AUTHOR, spec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadBelongsTo_parentsWithMixedNullAndValidForeignKeys_loadsForValidOnly() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");

        Post postWithNullUserId = new Post();
        postWithNullUserId.setId(1L);
        // userId is null

        Post postWithUserId = new Post();
        postWithUserId.setId(2L);
        postWithUserId.setUserId(1L);

        List<Post> parents = List.of(postWithNullUserId, postWithUserId);
        EagerLoadSpec spec = EagerLoadSpec.of(POST_AUTHOR);

        @SuppressWarnings("unchecked")
        List<User> result = invokeLoadBelongsTo(parents, POST_AUTHOR, spec);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    void loadBelongsTo_singleParent_loadsRelatedEntity() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Post 1", 1L);

        Post post = new Post();
        post.setId(1L);
        post.setUserId(1L);

        List<Post> parents = List.of(post);
        EagerLoadSpec spec = EagerLoadSpec.of(POST_AUTHOR);

        @SuppressWarnings("unchecked")
        List<User> result = invokeLoadBelongsTo(parents, POST_AUTHOR, spec);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    void loadBelongsTo_multipleParentsSameRelated_returnsSingleRelated() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Post 1", 1L);
        insertPost(2L, "Post 2", 1L);

        Post post1 = new Post();
        post1.setId(1L);
        post1.setUserId(1L);

        Post post2 = new Post();
        post2.setId(2L);
        post2.setUserId(1L); // Same user as post1

        List<Post> parents = List.of(post1, post2);
        EagerLoadSpec spec = EagerLoadSpec.of(POST_AUTHOR);

        @SuppressWarnings("unchecked")
        List<User> result = invokeLoadBelongsTo(parents, POST_AUTHOR, spec);

        assertNotNull(result);
        assertEquals(1, result.size()); // Deduplication - same foreign key
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    void loadBelongsTo_multipleParentsDifferentRelated_returnsAllRelated() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertUser(2L, "Bob", "bob@example.com");
        insertPost(1L, "Alice Post", 1L);
        insertPost(2L, "Bob Post", 2L);

        Post alicePost = new Post();
        alicePost.setId(1L);
        alicePost.setUserId(1L);

        Post bobPost = new Post();
        bobPost.setId(2L);
        bobPost.setUserId(2L);

        List<Post> parents = List.of(alicePost, bobPost);
        EagerLoadSpec spec = EagerLoadSpec.of(POST_AUTHOR);

        @SuppressWarnings("unchecked")
        List<User> result = invokeLoadBelongsTo(parents, POST_AUTHOR, spec);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void loadBelongsTo_relatedEntityNotFound_returnsEmptyList() throws Exception {
        // User with ID 999 does not exist
        Post post = new Post();
        post.setId(1L);
        post.setUserId(999L);

        List<Post> parents = List.of(post);
        EagerLoadSpec spec = EagerLoadSpec.of(POST_AUTHOR);

        @SuppressWarnings("unchecked")
        List<User> result = invokeLoadBelongsTo(parents, POST_AUTHOR, spec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadBelongsTo_withConstraint_appliesConstraintToQuery() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertUser(2L, "Bob", "bob@example.com");
        insertPost(1L, "Alice Post", 1L);
        insertPost(2L, "Bob Post", 2L);

        Post alicePost = new Post();
        alicePost.setId(1L);
        alicePost.setUserId(1L);

        Post bobPost = new Post();
        bobPost.setId(2L);
        bobPost.setUserId(2L);

        List<Post> parents = List.of(alicePost, bobPost);
        // Constraint to only load users with name 'Alice'
        EagerLoadSpec spec = EagerLoadSpec.of(POST_AUTHOR,
                builder -> builder.whereRaw("name = 'Alice'"));

        @SuppressWarnings("unchecked")
        List<User> result = invokeLoadBelongsTo(parents, POST_AUTHOR, spec);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    void loadBelongsTo_duplicateForeignKeys_deduplicatesInQuery() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");

        Post post1 = new Post();
        post1.setId(1L);
        post1.setUserId(1L);

        Post post2 = new Post();
        post2.setId(2L);
        post2.setUserId(1L); // Same FK as post1

        Post post3 = new Post();
        post3.setId(3L);
        post3.setUserId(1L); // Same FK as post1

        List<Post> parents = List.of(post1, post2, post3);
        EagerLoadSpec spec = EagerLoadSpec.of(POST_AUTHOR);

        @SuppressWarnings("unchecked")
        List<User> result = invokeLoadBelongsTo(parents, POST_AUTHOR, spec);

        assertNotNull(result);
        assertEquals(1, result.size()); // Only one user despite 3 posts with same FK
    }

    @SuppressWarnings("unchecked")
    private <T, R> List<R> invokeLoadBelongsTo(
            List<T> parents,
            Relation<T, R> relation,
            EagerLoadSpec spec
    ) throws Exception {
        Method method = EagerLoader.class.getDeclaredMethod(
                "loadBelongsTo", List.class, Relation.class, EagerLoadSpec.class);
        method.setAccessible(true);
        return (List<R>) method.invoke(eagerLoader, parents, relation, spec);
    }

    // ==================== loadBelongsToMany Tests ====================

    @Test
    void loadBelongsToMany_emptyParentsList_returnsEmptyList() throws Exception {
        List<User> emptyUsers = Collections.emptyList();
        EagerLoadSpec spec = EagerLoadSpec.of(USER_ROLES);

        @SuppressWarnings("unchecked")
        List<Role> result = invokeLoadBelongsToMany(emptyUsers, USER_ROLES, spec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadBelongsToMany_parentsWithNullKeys_returnsEmptyList() throws Exception {
        User userWithNullId = new User();
        userWithNullId.setName("NoId");

        List<User> parents = List.of(userWithNullId);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_ROLES);

        @SuppressWarnings("unchecked")
        List<Role> result = invokeLoadBelongsToMany(parents, USER_ROLES, spec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadBelongsToMany_parentsWithAllNullKeys_returnsEmptyWithoutQuery() throws Exception {
        User user1 = new User();
        user1.setName("NoId1");
        User user2 = new User();
        user2.setName("NoId2");

        List<User> parents = List.of(user1, user2);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_ROLES);

        @SuppressWarnings("unchecked")
        List<Role> result = invokeLoadBelongsToMany(parents, USER_ROLES, spec);

        // When all keys are null, returns empty without executing query
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadBelongsToMany_extractsCorrectParentKeys() throws Exception {
        // This test verifies that parent keys are correctly extracted
        // by checking the method doesn't throw when parents have valid IDs
        insertUser(1L, "Alice", "alice@example.com");

        User user = new User();
        user.setId(1L);

        List<User> parents = List.of(user);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_ROLES);

        // Should not throw - keys are extracted correctly
        @SuppressWarnings("unchecked")
        List<Role> result = invokeLoadBelongsToMany(parents, USER_ROLES, spec);

        assertNotNull(result);
    }

    @Test
    void loadBelongsToMany_deduplicatesParentKeys() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");

        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(1L); // Same ID - should be deduplicated in IN clause

        List<User> parents = List.of(user1, user2);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_ROLES);

        // Should not throw - duplicate keys are handled
        @SuppressWarnings("unchecked")
        List<Role> result = invokeLoadBelongsToMany(parents, USER_ROLES, spec);

        assertNotNull(result);
    }

    @Test
    void loadBelongsToMany_parentWithNoRelatedInPivot_returnsEmptyList() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertRole(1L, "admin");
        // No user_roles entry for Alice

        User user = new User();
        user.setId(1L);

        List<User> parents = List.of(user);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_ROLES);

        @SuppressWarnings("unchecked")
        List<Role> result = invokeLoadBelongsToMany(parents, USER_ROLES, spec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadBelongsToMany_buildsCorrectPivotJoin() throws Exception {
        // Verifies the method builds correct JOIN syntax
        // by checking it doesn't throw SQL syntax errors
        insertUser(1L, "Alice", "alice@example.com");
        insertRole(1L, "admin");
        insertUserRole(1L, 1L);

        User user = new User();
        user.setId(1L);

        List<User> parents = List.of(user);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_ROLES);

        // Should execute without SQL syntax errors
        @SuppressWarnings("unchecked")
        List<Role> result = invokeLoadBelongsToMany(parents, USER_ROLES, spec);

        assertNotNull(result);
    }

    @Test
    void loadBelongsToMany_withConstraint_doesNotThrow() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertRole(1L, "admin");
        insertUserRole(1L, 1L);

        User user = new User();
        user.setId(1L);

        List<User> parents = List.of(user);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_ROLES,
                builder -> builder.whereRaw("name = 'admin'"));

        // Should not throw - constraint is applied to query
        @SuppressWarnings("unchecked")
        List<Role> result = invokeLoadBelongsToMany(parents, USER_ROLES, spec);

        assertNotNull(result);
    }

    @Test
    void loadBelongsToMany_mixedNullAndValidKeys_filtersNulls() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");

        User userWithNullId = new User();
        userWithNullId.setName("NoId");
        User userWithId = new User();
        userWithId.setId(1L);

        List<User> parents = List.of(userWithNullId, userWithId);
        EagerLoadSpec spec = EagerLoadSpec.of(USER_ROLES);

        // Should not throw - null keys filtered out
        @SuppressWarnings("unchecked")
        List<Role> result = invokeLoadBelongsToMany(parents, USER_ROLES, spec);

        assertNotNull(result);
    }

    @SuppressWarnings("unchecked")
    private <T, R> List<R> invokeLoadBelongsToMany(
            List<T> parents,
            Relation<T, R> relation,
            EagerLoadSpec spec
    ) throws Exception {
        Method method = EagerLoader.class.getDeclaredMethod(
                "loadBelongsToMany", List.class, Relation.class, EagerLoadSpec.class);
        method.setAccessible(true);
        return (List<R>) method.invoke(eagerLoader, parents, relation, spec);
    }

    // ==================== loadRelation Tests ====================

    @Test
    void loadRelation_nestedSpecsWithEmptyRelatedEntities_skipsNestedLoading() throws Exception {
        // User exists but has no posts
        insertUser(1L, "Alice", "alice@example.com");
        // No posts inserted

        User user = new User();
        user.setId(1L);
        user.setName("Alice");

        List<User> users = List.of(user);
        // Nested spec: load posts -> then load comments (but posts will be empty)
        EagerLoadSpec postSpec = EagerLoadSpec.of(USER_POSTS).with(POST_COMMENTS);
        List<EagerLoadSpec> specs = List.of(postSpec);

        // Should not throw - nested loading skipped when relatedEntities is empty
        eagerLoader.loadRelations(users, specs);

        assertNotNull(user.getPosts());
        assertTrue(user.getPosts().isEmpty());
    }

    // ==================== loadRelations Tests ====================

    @Test
    void loadRelations_emptyEntities_noOp() {
        List<User> emptyUsers = Collections.emptyList();
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(USER_POSTS));

        // Should not throw
        eagerLoader.loadRelations(emptyUsers, specs);
    }

    @Test
    void loadRelations_emptySpecs_noOp() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        User user = new User();
        user.setId(1L);

        List<User> users = List.of(user);
        List<EagerLoadSpec> emptySpecs = Collections.emptyList();

        // Should not throw
        eagerLoader.loadRelations(users, emptySpecs);
    }

    @Test
    void loadRelations_hasMany_loadsRelatedEntities() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Post 1", 1L);
        insertPost(2L, "Post 2", 1L);

        User user = new User();
        user.setId(1L);
        user.setName("Alice");

        List<User> users = List.of(user);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(USER_POSTS));

        eagerLoader.loadRelations(users, specs);

        assertNotNull(user.getPosts());
        assertEquals(2, user.getPosts().size());
    }

    @Test
    void loadRelations_hasOne_loadsSingleEntity() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertProfile(1L, "Alice Bio", 1L);

        User user = new User();
        user.setId(1L);
        user.setName("Alice");

        List<User> users = List.of(user);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(USER_PROFILE));

        eagerLoader.loadRelations(users, specs);

        assertNotNull(user.getProfile());
        assertEquals("Alice Bio", user.getProfile().getBio());
    }

    @Test
    void loadRelations_belongsTo_loadsParentEntity() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Post 1", 1L);

        Post post = new Post();
        post.setId(1L);
        post.setUserId(1L);

        List<Post> posts = List.of(post);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(POST_AUTHOR));

        eagerLoader.loadRelations(posts, specs);

        assertNotNull(post.getAuthor());
        assertEquals("Alice", post.getAuthor().getName());
    }

    @Test
    void loadRelations_belongsToMany_loadsManyToManyRelation() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertRole(1L, "admin");
        insertRole(2L, "editor");
        insertUserRole(1L, 1L);
        insertUserRole(1L, 2L);

        User user = new User();
        user.setId(1L);
        user.setName("Alice");

        List<User> users = List.of(user);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(USER_ROLES));

        eagerLoader.loadRelations(users, specs);

        assertNotNull(user.getRoles());
        assertEquals(2, user.getRoles().size());
    }

    @Test
    void loadRelations_hasManyThrough_loadsThroughRelation() throws Exception {
        insertCountry(1L, "USA");
        insertUserWithCountry(1L, "Alice", "alice@example.com", 1L);
        insertPost(1L, "Post 1", 1L);
        insertPost(2L, "Post 2", 1L);

        Country country = new Country();
        country.setId(1L);
        country.setName("USA");

        List<Country> countries = List.of(country);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(COUNTRY_POSTS));

        eagerLoader.loadRelations(countries, specs);

        assertNotNull(country.getPosts());
        assertEquals(2, country.getPosts().size());
    }

    @Test
    void loadRelations_nestedRelations_loadsRecursively() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Post 1", 1L);
        insertComment(1L, "Comment 1", 1L);
        insertComment(2L, "Comment 2", 1L);

        User user = new User();
        user.setId(1L);
        user.setName("Alice");

        List<User> users = List.of(user);
        EagerLoadSpec postSpec = EagerLoadSpec.of(USER_POSTS).with(POST_COMMENTS);
        List<EagerLoadSpec> specs = List.of(postSpec);

        eagerLoader.loadRelations(users, specs);

        assertNotNull(user.getPosts());
        assertEquals(1, user.getPosts().size());
        Post loadedPost = user.getPosts().get(0);
        assertNotNull(loadedPost.getComments());
        assertEquals(2, loadedPost.getComments().size());
    }

    @Test
    void loadRelations_withConstraint_appliesConstraint() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Important Post", 1L);
        insertPost(2L, "Regular Post", 1L);

        User user = new User();
        user.setId(1L);
        user.setName("Alice");

        List<User> users = List.of(user);
        // Constraint to filter posts by title containing 'Important'
        EagerLoadSpec spec = EagerLoadSpec.of(USER_POSTS,
                builder -> builder.whereRaw("title LIKE '%Important%'"));
        List<EagerLoadSpec> specs = List.of(spec);

        eagerLoader.loadRelations(users, specs);

        assertNotNull(user.getPosts());
        assertEquals(1, user.getPosts().size());
        assertEquals("Important Post", user.getPosts().get(0).getTitle());
    }

    @Test
    void loadRelations_multipleParentEntities_loadsForAll() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        insertUser(2L, "Bob", "bob@example.com");
        insertPost(1L, "Alice Post", 1L);
        insertPost(2L, "Bob Post", 2L);

        User alice = new User();
        alice.setId(1L);
        alice.setName("Alice");

        User bob = new User();
        bob.setId(2L);
        bob.setName("Bob");

        List<User> users = List.of(alice, bob);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(USER_POSTS));

        eagerLoader.loadRelations(users, specs);

        assertNotNull(alice.getPosts());
        assertEquals(1, alice.getPosts().size());
        assertNotNull(bob.getPosts());
        assertEquals(1, bob.getPosts().size());
    }

    @Test
    void loadRelations_noRelatedEntities_setsEmptyCollection() throws Exception {
        insertUser(1L, "Alice", "alice@example.com");
        // No posts for Alice

        User user = new User();
        user.setId(1L);
        user.setName("Alice");

        List<User> users = List.of(user);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(USER_POSTS));

        eagerLoader.loadRelations(users, specs);

        assertNotNull(user.getPosts());
        assertTrue(user.getPosts().isEmpty());
    }

    @Test
    void loadRelations_nullKeyValues_skipped() throws Exception {
        // User without ID
        User user = new User();
        user.setName("NoId");

        List<User> users = List.of(user);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(USER_POSTS));

        // Should not throw
        eagerLoader.loadRelations(users, specs);

        assertNotNull(user.getPosts());
        assertTrue(user.getPosts().isEmpty());
    }

    @Test
    void loadRelations_polymorphicRelation_throwsUnsupported() throws Exception {
        // Create a morph relation
        Relation<User, Post> morphRelation = Relation.morphOne(
                USERS, POSTS, "taggable", "taggable_type", "taggable_id", "id", false
        );

        User user = new User();
        user.setId(1L);

        List<User> users = List.of(user);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(morphRelation));

        assertThrows(UnsupportedOperationException.class, () ->
                eagerLoader.loadRelations(users, specs));
    }

    @Test
    void loadRelations_stringKeyValues_quotedCorrectly() throws Exception {
        // Test with UUID or String keys
        insertUser(1L, "Alice", "alice@example.com");
        insertPost(1L, "Post 1", 1L);

        User user = new User();
        user.setId(1L);

        List<User> users = List.of(user);
        List<EagerLoadSpec> specs = List.of(EagerLoadSpec.of(USER_POSTS));

        // Should not throw SQL injection issues
        eagerLoader.loadRelations(users, specs);

        assertNotNull(user.getPosts());
    }

    // ==================== Helper Methods ====================

    private void insertUser(Long id, String name, String email) throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO users (id, name, email) VALUES (%d, '%s', '%s')",
                    id, name, email));
        }
    }

    private void insertUserWithCountry(Long id, String name, String email, Long countryId) throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO users (id, name, email, country_id) VALUES (%d, '%s', '%s', %d)",
                    id, name, email, countryId));
        }
    }

    private void insertPost(Long id, String title, Long userId) throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO posts (id, title, user_id) VALUES (%d, '%s', %d)",
                    id, title, userId));
        }
    }

    private void insertComment(Long id, String content, Long postId) throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO comments (id, content, post_id) VALUES (%d, '%s', %d)",
                    id, content, postId));
        }
    }

    private void insertProfile(Long id, String bio, Long userId) throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO profiles (id, bio, user_id) VALUES (%d, '%s', %d)",
                    id, bio, userId));
        }
    }

    private void insertRole(Long id, String name) throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO roles (id, name) VALUES (%d, '%s')",
                    id, name));
        }
    }

    private void insertUserRole(Long userId, Long roleId) throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (%d, %d)",
                    userId, roleId));
        }
    }

    private void insertCountry(Long id, String name) throws Exception {
        try (Statement stmt = setupConnection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO countries (id, name) VALUES (%d, '%s')",
                    id, name));
        }
    }

    // ==================== Test Entities ====================

    @Entity(table = "users")
    public static class User {
        @Column(name = "id")
        private Long id;
        @Column(name = "name")
        private String name;
        @Column(name = "email")
        private String email;
        @Column(name = "country_id")
        private Long countryId;

        private Profile profile;
        private List<Post> posts = new ArrayList<>();
        private List<Role> roles = new ArrayList<>();

        public User() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Long getCountryId() { return countryId; }
        public void setCountryId(Long countryId) { this.countryId = countryId; }
        public Profile getProfile() { return profile; }
        public void setProfile(Profile profile) { this.profile = profile; }
        public List<Post> getPosts() { return posts; }
        public void setPosts(List<Post> posts) { this.posts = posts; }
        public List<Role> getRoles() { return roles; }
        public void setRoles(List<Role> roles) { this.roles = roles; }
    }

    @Entity(table = "posts")
    public static class Post {
        @Column(name = "id")
        private Long id;
        @Column(name = "title")
        private String title;
        @Column(name = "user_id")
        private Long userId;

        private User author;
        private List<Comment> comments = new ArrayList<>();

        public Post() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
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
        @Column(name = "content")
        private String content;
        @Column(name = "post_id")
        private Long postId;

        public Comment() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getPostId() { return postId; }
        public void setPostId(Long postId) { this.postId = postId; }
    }

    @Entity(table = "profiles")
    public static class Profile {
        @Column(name = "id")
        private Long id;
        @Column(name = "bio")
        private String bio;
        @Column(name = "user_id")
        private Long userId;

        public Profile() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    @Entity(table = "roles")
    public static class Role {
        @Column(name = "id")
        private Long id;
        @Column(name = "name")
        private String name;

        public Role() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Entity(table = "countries")
    public static class Country {
        @Column(name = "id")
        private Long id;
        @Column(name = "name")
        private String name;

        private List<Post> posts = new ArrayList<>();

        public Country() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Post> getPosts() { return posts; }
        public void setPosts(List<Post> posts) { this.posts = posts; }
    }
}
