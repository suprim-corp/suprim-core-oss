package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.type.ComparableColumn;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.StringColumn;
import sant1ago.dev.suprim.core.type.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;

/**
 * Tests for RelationshipManager to cover ORM relationship operations.
 */
@DisplayName("RelationshipManager Tests")
class RelationshipManagerTest {

    private Transaction mockTransaction;
    private RelationshipManager manager;

    // ==================== TEST ENTITIES ====================

    @Entity(table = "users")
    public static class User {
        @Id
        @Column(name = "id")
        private Long id;

        @Column(name = "name")
        private String name;

        public User() {}

        public User(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Entity(table = "posts")
    public static class Post {
        @Id
        @Column(name = "id")
        private Long id;

        @Column(name = "title")
        private String title;

        @Column(name = "author_id")
        private Long authorId;

        public Post() {}

        public Post(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
    }

    @Entity(table = "roles")
    public static class Role {
        @Id
        @Column(name = "id")
        private Long id;

        @Column(name = "name")
        private String name;

        public Role() {}

        public Role(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // ==================== METAMODELS ====================

    public static final class User_ {
        public static final Table<User> TABLE = Table.of("users", User.class);
        public static final ComparableColumn<User, Long> ID = new ComparableColumn<>(TABLE, "id", Long.class, "BIGINT");
        public static final StringColumn<User> NAME = new StringColumn<>(TABLE, "name", "VARCHAR(255)");

        // HAS_MANY relation to posts
        public static final Relation<User, Post> POSTS = Relation.hasMany(
            TABLE, Post_.TABLE, "author_id", "id", false, false, "posts"
        );

        // BELONGS_TO_MANY relation to roles (through user_roles pivot)
        public static final Relation<User, Role> ROLES = Relation.belongsToMany(
            TABLE, Role_.TABLE, "user_roles", "user_id", "role_id", "id", "id", List.of(), false, false
        );

        // BELONGS_TO_MANY relation with pivot timestamps enabled
        public static final Relation<User, Role> ROLES_WITH_TIMESTAMPS = Relation.belongsToMany(
            TABLE, Role_.TABLE, "user_roles", "user_id", "role_id", "id", "id", List.of(), true, false
        );

        private User_() {}
    }

    public static final class Post_ {
        public static final Table<Post> TABLE = Table.of("posts", Post.class);
        public static final ComparableColumn<Post, Long> ID = new ComparableColumn<>(TABLE, "id", Long.class, "BIGINT");
        public static final StringColumn<Post> TITLE = new StringColumn<>(TABLE, "title", "VARCHAR(255)");
        public static final ComparableColumn<Post, Long> AUTHOR_ID = new ComparableColumn<>(TABLE, "author_id", Long.class, "BIGINT");

        // BELONGS_TO relation to user
        public static final Relation<Post, User> AUTHOR = Relation.belongsTo(
            TABLE, User_.TABLE, "author_id", "id", false, false, "author"
        );

        private Post_() {}
    }

    public static final class Role_ {
        public static final Table<Role> TABLE = Table.of("roles", Role.class);
        public static final ComparableColumn<Role, Long> ID = new ComparableColumn<>(TABLE, "id", Long.class, "BIGINT");
        public static final StringColumn<Role> NAME = new StringColumn<>(TABLE, "name", "VARCHAR(255)");

        private Role_() {}
    }

    // ==================== SETUP ====================

    @BeforeEach
    void setUp() {
        mockTransaction = mock(Transaction.class);
        manager = new RelationshipManager(mockTransaction);
    }

    // ==================== CONSTRUCTOR TESTS ====================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor with null transaction throws NullPointerException")
        void constructor_nullTransaction_throwsException() {
            assertThrows(NullPointerException.class, () -> new RelationshipManager(null));
        }

        @Test
        @DisplayName("Constructor with valid transaction creates instance")
        void constructor_validTransaction_createsInstance() {
            RelationshipManager rm = new RelationshipManager(mockTransaction);
            assertNotNull(rm);
        }
    }

    // ==================== BELONGS_TO TESTS ====================

    @Nested
    @DisplayName("BelongsTo Relationship Tests")
    class BelongsToTests {

        @Test
        @DisplayName("associate updates foreign key on child entity")
        void associate_updatesFK() {
            Post post = new Post(1L, "Hello World");
            User user = new User(100L, "John");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.associate(post, Post_.AUTHOR, user);

            assertEquals(1, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("associate with wrong relation type throws exception")
        void associate_wrongRelationType_throwsException() {
            User user = new User(1L, "John");
            Post post = new Post(2L, "Test");

            // HAS_MANY is not BELONGS_TO
            assertThrows(IllegalArgumentException.class, () ->
                manager.associate(user, User_.POSTS, post));
        }

        @Test
        @DisplayName("dissociate clears foreign key on child entity")
        void dissociate_clearsFK() {
            Post post = new Post(1L, "Hello World");
            post.setAuthorId(100L);

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.dissociate(post, Post_.AUTHOR);

            assertEquals(1, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("dissociate with wrong relation type throws exception")
        void dissociate_wrongRelationType_throwsException() {
            User user = new User(1L, "John");

            assertThrows(IllegalArgumentException.class, () ->
                manager.dissociate(user, User_.POSTS));
        }
    }

    // ==================== HAS_ONE / HAS_MANY TESTS ====================

    @Nested
    @DisplayName("HasOne/HasMany Relationship Tests")
    class HasManyTests {

        @Test
        @DisplayName("save inserts child with FK set to parent ID")
        void save_insertsChildWithFK() {
            User user = new User(1L, "John");
            Post post = new Post();
            post.setTitle("New Post");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            Post result = manager.save(user, User_.POSTS, post);

            assertSame(post, result);
            assertEquals(1L, post.getAuthorId());
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("save with wrong relation type throws exception")
        void save_wrongRelationType_throwsException() {
            Post post = new Post(1L, "Test");
            User user = new User(2L, "John");

            // BELONGS_TO is not HAS_ONE or HAS_MANY
            assertThrows(IllegalArgumentException.class, () ->
                manager.save(post, Post_.AUTHOR, user));
        }

        @Test
        @DisplayName("saveMany inserts multiple children")
        void saveMany_insertsMultipleChildren() {
            User user = new User(1L, "John");
            Post post1 = new Post();
            post1.setTitle("Post 1");
            Post post2 = new Post();
            post2.setTitle("Post 2");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            List<Post> result = manager.saveMany(user, User_.POSTS, List.of(post1, post2));

            assertEquals(2, result.size());
            assertEquals(1L, post1.getAuthorId());
            assertEquals(1L, post2.getAuthorId());
            verify(mockTransaction, times(2)).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("create inserts entity from attribute map")
        void create_insertsFromMap() {
            User user = new User(1L, "John");
            Map<String, Object> attrs = Map.of("title", "New Post");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.create(user, User_.POSTS, attrs);

            assertEquals(1, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("createMany inserts multiple entities from attribute maps")
        void createMany_insertsMultiple() {
            User user = new User(1L, "John");
            List<Map<String, Object>> attrsList = List.of(
                Map.of("title", "Post 1"),
                Map.of("title", "Post 2")
            );

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.createMany(user, User_.POSTS, attrsList);

            assertEquals(2, result);
            verify(mockTransaction, times(2)).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("delete removes all related entities")
        void delete_removesAll() {
            User user = new User(1L, "John");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(3);

            int result = manager.delete(user, User_.POSTS);

            assertEquals(3, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("forceDelete is alias for delete")
        void forceDelete_sameAsDelete() {
            User user = new User(1L, "John");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(5);

            int result = manager.forceDelete(user, User_.POSTS);

            assertEquals(5, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }
    }

    // ==================== BELONGS_TO_MANY TESTS ====================

    @Nested
    @DisplayName("BelongsToMany Relationship Tests")
    class BelongsToManyTests {

        @Test
        @DisplayName("attach inserts into pivot table")
        void attach_insertsIntoPivot() {
            User user = new User(1L, "John");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.attach(user, User_.ROLES, 10L);

            assertEquals(1, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("attach with pivot attributes inserts additional columns")
        void attach_withPivotAttributes() {
            User user = new User(1L, "John");
            Map<String, Object> pivotAttrs = Map.of("assigned_by", 99L);

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.attach(user, User_.ROLES, 10L, pivotAttrs);

            assertEquals(1, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("attach with wrong relation type throws exception")
        void attach_wrongRelationType_throwsException() {
            User user = new User(1L, "John");

            assertThrows(IllegalArgumentException.class, () ->
                manager.attach(user, User_.POSTS, 10L));
        }

        @Test
        @DisplayName("detach removes from pivot table")
        void detach_removesFromPivot() {
            User user = new User(1L, "John");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.detach(user, User_.ROLES, 10L);

            assertEquals(1, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("detach all removes all from pivot table")
        void detachAll_removesAllFromPivot() {
            User user = new User(1L, "John");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(3);

            int result = manager.detach(user, User_.ROLES);

            assertEquals(3, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("sync adds missing and removes extra")
        void sync_addsAndRemoves() {
            User user = new User(1L, "John");

            // Mock getCurrentAttachments - returns current [10, 20]
            doAnswer(invocation -> List.of(10L, 20L))
                .when(mockTransaction).query(any(QueryResult.class), any());
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            // Desired: [20, 30] -> attach 30, detach 10
            SyncResult result = manager.sync(user, User_.ROLES, List.of(20L, 30L));

            assertEquals(1, result.attached());
            assertEquals(1, result.detached());
        }

        @Test
        @DisplayName("syncWithoutDetaching only adds missing")
        void syncWithoutDetaching_onlyAdds() {
            User user = new User(1L, "John");

            doAnswer(invocation -> List.of(10L))
                .when(mockTransaction).query(any(QueryResult.class), any());
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.syncWithoutDetaching(user, User_.ROLES, List.of(10L, 20L));

            assertEquals(1, result); // Only 20L attached (10L already exists)
        }

        @Test
        @DisplayName("toggle attaches missing and detaches existing")
        void toggle_attachesAndDetaches() {
            User user = new User(1L, "John");

            doAnswer(invocation -> List.of(10L))
                .when(mockTransaction).query(any(QueryResult.class), any());
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            // 10L exists, 20L doesn't -> detach 10, attach 20
            ToggleResult result = manager.toggle(user, User_.ROLES, List.of(10L, 20L));

            assertEquals(1, result.attached().size());
            assertEquals(1, result.detached().size());
            assertTrue(result.attached().contains(20L));
            assertTrue(result.detached().contains(10L));
        }

        @Test
        @DisplayName("updateExistingPivot updates pivot attributes")
        void updateExistingPivot_updatesAttributes() {
            User user = new User(1L, "John");
            Map<String, Object> attrs = Map.of("is_primary", true);

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.updateExistingPivot(user, User_.ROLES, 10L, attrs);

            assertEquals(1, result);
            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("attach with pivot timestamps adds created_at and updated_at")
        void attach_withTimestamps_addsTimestampColumns() {
            User user = new User(1L, "John");
            Role role = new Role(10L, "Admin");

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.attach(user, User_.ROLES_WITH_TIMESTAMPS, role);

            assertEquals(1, result);
            verify(mockTransaction).execute(argThat(query -> {
                String sql = query.sql();
                return sql.contains("created_at") && sql.contains("updated_at");
            }));
        }

        @Test
        @DisplayName("updateExistingPivot with timestamps updates updated_at")
        void updateExistingPivot_withTimestamps_updatesTimestamp() {
            User user = new User(1L, "John");
            Map<String, Object> attrs = Map.of("is_primary", true);

            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            int result = manager.updateExistingPivot(user, User_.ROLES_WITH_TIMESTAMPS, 10L, attrs);

            assertEquals(1, result);
            verify(mockTransaction).execute(argThat(query -> {
                String sql = query.sql();
                return sql.contains("updated_at");
            }));
        }
    }

    // ==================== TOUCH TIMESTAMPS TESTS ====================

    @Nested
    @DisplayName("Touch Timestamps Tests")
    class TouchTimestampsTests {

        @Test
        @DisplayName("touchTimestamps updates specified columns")
        void touchTimestamps_updatesColumns() {
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            manager.touchTimestamps(User_.TABLE, 1L, List.of("updated_at"));

            verify(mockTransaction).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("touchTimestamps with null list does nothing")
        void touchTimestamps_nullList_doesNothing() {
            manager.touchTimestamps(User_.TABLE, 1L, null);

            verify(mockTransaction, never()).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("touchTimestamps with empty list does nothing")
        void touchTimestamps_emptyList_doesNothing() {
            manager.touchTimestamps(User_.TABLE, 1L, List.of());

            verify(mockTransaction, never()).execute(any(QueryResult.class));
        }
    }

    // ==================== FIRST OR CREATE/NEW TESTS ====================

    @Nested
    @DisplayName("FirstOr Methods Tests")
    class FirstOrMethodsTests {

        @Test
        @DisplayName("firstOrCreate returns existing entity if found")
        @SuppressWarnings("unchecked")
        void firstOrCreate_existingEntity_returns() {
            User user = new User(1L, "John");
            Post existingPost = new Post(10L, "Existing");
            existingPost.setAuthorId(1L);

            when(mockTransaction.query(any(QueryResult.class), any())).thenReturn(List.of(existingPost));

            Post result = manager.firstOrCreate(user, User_.POSTS,
                Map.of("title", "Existing"), Map.of());

            assertEquals(existingPost, result);
        }

        @Test
        @DisplayName("firstOrCreate creates entity if not found")
        @SuppressWarnings("unchecked")
        void firstOrCreate_notFound_creates() {
            User user = new User(1L, "John");
            Post newPost = new Post(20L, "New Post");
            newPost.setAuthorId(1L);

            // First query returns empty, second returns the created entity
            when(mockTransaction.query(any(QueryResult.class), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(newPost));
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            Post result = manager.firstOrCreate(user, User_.POSTS,
                Map.of("title", "New Post"), Map.of("author_id", 1L));

            assertEquals(newPost.getTitle(), result.getTitle());
        }

        @Test
        @DisplayName("firstOrNew returns existing entity if found")
        @SuppressWarnings("unchecked")
        void firstOrNew_existingEntity_returns() {
            User user = new User(1L, "John");
            Post existingPost = new Post(10L, "Existing");
            existingPost.setAuthorId(1L);

            when(mockTransaction.query(any(QueryResult.class), any())).thenReturn(List.of(existingPost));

            Post result = manager.firstOrNew(user, User_.POSTS,
                Map.of("title", "Existing"), Map.of());

            assertEquals(existingPost, result);
        }

        @Test
        @DisplayName("firstOrNew returns new instance if not found (not persisted)")
        @SuppressWarnings("unchecked")
        void firstOrNew_notFound_returnsNewInstance() {
            User user = new User(1L, "John");

            when(mockTransaction.query(any(QueryResult.class), any())).thenReturn(List.of());

            Post result = manager.firstOrNew(user, User_.POSTS,
                Map.of("title", "New Title"), Map.of());

            assertNotNull(result);
            assertEquals("New Title", result.getTitle());
            assertEquals(1L, result.getAuthorId());
            // Not persisted - no execute called
            verify(mockTransaction, never()).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("updateOrCreate updates existing entity")
        @SuppressWarnings("unchecked")
        void updateOrCreate_existingEntity_updates() {
            User user = new User(1L, "John");
            Post existingPost = new Post(10L, "Old Title");
            existingPost.setAuthorId(1L);
            Post updatedPost = new Post(10L, "New Title");
            updatedPost.setAuthorId(1L);

            // First query finds existing, last query returns updated
            when(mockTransaction.query(any(QueryResult.class), any()))
                .thenReturn(List.of(existingPost))
                .thenReturn(List.of(updatedPost));
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            Post result = manager.updateOrCreate(user, User_.POSTS,
                Map.of("id", 10L), Map.of("title", "New Title"));

            assertEquals("New Title", result.getTitle());
        }

        @Test
        @DisplayName("firstOrCreate with null defaults creates entity")
        @SuppressWarnings("unchecked")
        void firstOrCreate_nullDefaults_creates() {
            User user = new User(1L, "John");
            Post newPost = new Post(20L, "New Post");
            newPost.setAuthorId(1L);

            when(mockTransaction.query(any(QueryResult.class), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(newPost));
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            Post result = manager.firstOrCreate(user, User_.POSTS,
                Map.of("title", "New Post"), null);

            assertEquals(newPost.getTitle(), result.getTitle());
        }

        @Test
        @DisplayName("firstOrNew with null defaults returns new instance")
        @SuppressWarnings("unchecked")
        void firstOrNew_nullDefaults_returnsNewInstance() {
            User user = new User(1L, "John");

            when(mockTransaction.query(any(QueryResult.class), any())).thenReturn(List.of());

            Post result = manager.firstOrNew(user, User_.POSTS,
                Map.of("title", "New Title"), null);

            assertNotNull(result);
            assertEquals("New Title", result.getTitle());
            assertEquals(1L, result.getAuthorId());
            verify(mockTransaction, never()).execute(any(QueryResult.class));
        }

        @Test
        @DisplayName("updateOrCreate with null values creates entity")
        @SuppressWarnings("unchecked")
        void updateOrCreate_nullValues_creates() {
            User user = new User(1L, "John");
            Post newPost = new Post(20L, "Created");
            newPost.setAuthorId(1L);

            when(mockTransaction.query(any(QueryResult.class), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(newPost));
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            Post result = manager.updateOrCreate(user, User_.POSTS,
                Map.of("title", "Created"), null);

            assertEquals("Created", result.getTitle());
        }

        @Test
        @DisplayName("updateOrCreate creates entity if not found")
        @SuppressWarnings("unchecked")
        void updateOrCreate_notFound_creates() {
            User user = new User(1L, "John");
            Post newPost = new Post(20L, "Created");
            newPost.setAuthorId(1L);

            when(mockTransaction.query(any(QueryResult.class), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(newPost));
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(1);

            Post result = manager.updateOrCreate(user, User_.POSTS,
                Map.of("title", "Created"), Map.of());

            assertEquals("Created", result.getTitle());
        }
    }

    // ==================== DELETE WITH CONSTRAINT TESTS ====================

    @Nested
    @DisplayName("Delete with Constraint Tests")
    class DeleteWithConstraintTests {

        @Test
        @DisplayName("delete with constraint deletes matching entities")
        @SuppressWarnings("unchecked")
        void delete_withConstraint_deletesMatching() {
            User user = new User(1L, "John");

            when(mockTransaction.query(any(QueryResult.class), any())).thenReturn(List.of(10L, 20L));
            when(mockTransaction.execute(any(QueryResult.class))).thenReturn(2);

            int result = manager.delete(user, User_.POSTS,
                builder -> builder.and(Post_.TITLE.like("%draft%")));

            assertEquals(2, result);
        }

        @Test
        @DisplayName("delete with constraint returns 0 if no matches")
        @SuppressWarnings("unchecked")
        void delete_withConstraint_noMatches_returnsZero() {
            User user = new User(1L, "John");

            when(mockTransaction.query(any(QueryResult.class), any())).thenReturn(List.of());

            int result = manager.delete(user, User_.POSTS,
                builder -> builder.and(Post_.TITLE.like("%nonexistent%")));

            assertEquals(0, result);
            // execute should not be called when no IDs to delete
            verify(mockTransaction, never()).execute(any(QueryResult.class));
        }
    }
}
