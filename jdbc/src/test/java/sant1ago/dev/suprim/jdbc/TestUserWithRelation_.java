package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.type.ComparableColumn;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.StringColumn;
import sant1ago.dev.suprim.core.type.Table;

/**
 * Metamodel for TestUserWithRelation entity - used to test with() method coverage.
 */
public final class TestUserWithRelation_ {

    public static final Table<TestUserWithRelation> TABLE =
        Table.of("test_users_with_relation", TestUserWithRelation.class);

    public static final ComparableColumn<TestUserWithRelation, Long> ID =
        new ComparableColumn<>(TABLE, "id", Long.class, "BIGINT");

    public static final StringColumn<TestUserWithRelation> NAME =
        new StringColumn<>(TABLE, "name", "VARCHAR(255)");

    // HAS_ONE relation to profile
    public static final Relation<TestUserWithRelation, TestProfile> PROFILE = Relation.hasOne(
        TABLE, TestProfile_.TABLE, "user_id", "id", false, false, "profile"
    );

    private TestUserWithRelation_() {
        // Prevent instantiation
    }
}
