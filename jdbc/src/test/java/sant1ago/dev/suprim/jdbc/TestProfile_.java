package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.type.ComparableColumn;
import sant1ago.dev.suprim.core.type.StringColumn;
import sant1ago.dev.suprim.core.type.Table;

/**
 * Metamodel for TestProfile entity - used in relation tests.
 */
public final class TestProfile_ {

    public static final Table<TestProfile> TABLE = Table.of("profiles", TestProfile.class);

    public static final ComparableColumn<TestProfile, Long> ID =
        new ComparableColumn<>(TABLE, "id", Long.class, "BIGINT");

    public static final ComparableColumn<TestProfile, Long> USER_ID =
        new ComparableColumn<>(TABLE, "user_id", Long.class, "BIGINT");

    public static final StringColumn<TestProfile> BIO =
        new StringColumn<>(TABLE, "bio", "TEXT");

    private TestProfile_() {
        // Prevent instantiation
    }
}
