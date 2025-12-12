package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.core.type.ComparableColumn;
import sant1ago.dev.suprim.core.type.StringColumn;
import sant1ago.dev.suprim.core.type.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Metamodel for TestOrder entity.
 */
public final class TestOrder_ {

    public static final Table<TestOrder> TABLE = Table.of("orders", TestOrder.class);

    public static final ComparableColumn<TestOrder, Long> ID =
        new ComparableColumn<>(TABLE, "id", Long.class, "BIGINT");

    public static final ComparableColumn<TestOrder, Long> USER_ID =
        new ComparableColumn<>(TABLE, "user_id", Long.class, "BIGINT");

    public static final ComparableColumn<TestOrder, BigDecimal> AMOUNT =
        new ComparableColumn<>(TABLE, "amount", BigDecimal.class, "DECIMAL");

    public static final StringColumn<TestOrder> STATUS =
        new StringColumn<>(TABLE, "status", "VARCHAR(50)");

    public static final ComparableColumn<TestOrder, LocalDateTime> CREATED_AT =
        new ComparableColumn<>(TABLE, "created_at", LocalDateTime.class, "TIMESTAMP");

    private TestOrder_() {
        // Prevent instantiation
    }
}
