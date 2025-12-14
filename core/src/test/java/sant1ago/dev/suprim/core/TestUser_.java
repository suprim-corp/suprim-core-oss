package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.core.type.ComparableColumn;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.StringColumn;
import sant1ago.dev.suprim.core.type.Table;
import sant1ago.dev.suprim.core.type.Column;

import java.time.LocalDateTime;

/**
 * Metamodel for TestUser entity - used in query builder tests.
 */
public final class TestUser_ {

    public static final Table<TestUser> TABLE = Table.of("users", TestUser.class);

    public static final ComparableColumn<TestUser, Long> ID =
        new ComparableColumn<>(TABLE, "id", Long.class, "BIGINT");

    public static final StringColumn<TestUser> EMAIL =
        new StringColumn<>(TABLE, "email", "VARCHAR(255)");

    public static final StringColumn<TestUser> NAME =
        new StringColumn<>(TABLE, "name", "VARCHAR(255)");

    public static final ComparableColumn<TestUser, Integer> AGE =
        new ComparableColumn<>(TABLE, "age", Integer.class, "INTEGER");

    public static final Column<TestUser, Boolean> IS_ACTIVE =
        new Column<>(TABLE, "is_active", Boolean.class, "BOOLEAN");

    public static final ComparableColumn<TestUser, LocalDateTime> CREATED_AT =
        new ComparableColumn<>(TABLE, "created_at", LocalDateTime.class, "TIMESTAMP");

    // Relations - used by PathResolver tests
    public static final Relation<TestUser, TestOrder> ORDERS = Relation.hasMany(
        TABLE, TestOrder_.TABLE, "user_id", "id", false, false, "orders"
    );

    // HAS_ONE relation for coverage testing
    public static final Relation<TestUser, TestOrder> LATEST_ORDER = Relation.hasOne(
        TABLE, TestOrder_.TABLE, "user_id", "id", false, false, "latestOrder"
    );

    private TestUser_() {
        // Prevent instantiation
    }
}
