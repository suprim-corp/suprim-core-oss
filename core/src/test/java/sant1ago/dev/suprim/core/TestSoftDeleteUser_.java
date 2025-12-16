package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.ComparableColumn;
import sant1ago.dev.suprim.core.type.StringColumn;
import sant1ago.dev.suprim.core.type.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metamodel for TestSoftDeleteUser entity - used in soft delete tests.
 */
public final class TestSoftDeleteUser_ {

    public static final Table<TestSoftDeleteUser> TABLE = Table.of("soft_delete_users", TestSoftDeleteUser.class);

    public static final Column<TestSoftDeleteUser, UUID> ID =
        new Column<>(TABLE, "id", UUID.class, "UUID");

    public static final StringColumn<TestSoftDeleteUser> EMAIL =
        new StringColumn<>(TABLE, "email", "VARCHAR(255)");

    public static final StringColumn<TestSoftDeleteUser> NAME =
        new StringColumn<>(TABLE, "name", "VARCHAR(255)");

    public static final Column<TestSoftDeleteUser, Boolean> IS_ACTIVE =
        new Column<>(TABLE, "is_active", Boolean.class, "BOOLEAN");

    public static final ComparableColumn<TestSoftDeleteUser, LocalDateTime> DELETED_AT =
        new ComparableColumn<>(TABLE, "deleted_at", LocalDateTime.class, "TIMESTAMP");

    public static final ComparableColumn<TestSoftDeleteUser, LocalDateTime> CREATED_AT =
        new ComparableColumn<>(TABLE, "created_at", LocalDateTime.class, "TIMESTAMP");

    private TestSoftDeleteUser_() {
        // Prevent instantiation
    }
}
