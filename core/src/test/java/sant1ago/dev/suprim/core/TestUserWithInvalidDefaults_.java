package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.core.type.ComparableColumn;
import sant1ago.dev.suprim.core.type.Table;

/**
 * Metamodel for TestUserWithInvalidDefaults entity.
 * Note: This entity has @Entity(with = {"nonexistent_relation"}) which doesn't exist here.
 */
public final class TestUserWithInvalidDefaults_ {

    public static final Table<TestUserWithInvalidDefaults> TABLE =
        Table.of("users_invalid", TestUserWithInvalidDefaults.class);

    public static final ComparableColumn<TestUserWithInvalidDefaults, Long> ID =
        new ComparableColumn<>(TABLE, "id", Long.class, "BIGINT");

    // No relations defined - "nonexistent_relation" doesn't exist!

    private TestUserWithInvalidDefaults_() {
        // Prevent instantiation
    }
}
