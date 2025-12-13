package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.core.type.ComparableColumn;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.StringColumn;
import sant1ago.dev.suprim.core.type.Table;

/**
 * Metamodel for TestUserWithDefaults entity with default eager loads.
 */
public final class TestUserWithDefaults_ {

    public static final Table<TestUserWithDefaults> TABLE = Table.of("users_with_defaults", TestUserWithDefaults.class);

    public static final ComparableColumn<TestUserWithDefaults, Long> ID =
        new ComparableColumn<>(TABLE, "id", Long.class, "BIGINT");

    public static final StringColumn<TestUserWithDefaults> EMAIL =
        new StringColumn<>(TABLE, "email", "VARCHAR(255)");

    // Has many relation for default eager loading test
    public static final Relation<TestUserWithDefaults, TestOrder> ORDERS = Relation.hasMany(
        TABLE, TestOrder_.TABLE, "user_id", "id", false, false, "orders"
    );

    private TestUserWithDefaults_() {
        // Prevent instantiation
    }
}
