package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import java.sql.Connection;
import java.util.Objects;

/**
 * Thread-local context holder for Active Record pattern.
 *
 * <p>Provides persistence context (connection + dialect) to entities
 * calling {@code save()} within a transaction. Context is set by
 * {@link SuprimExecutor} and should not be manipulated directly.
 *
 * <pre>{@code
 * executor.transaction(tx -> {
 *     User user = new User();
 *     user.setEmail("test@example.com");
 *     user.save();  // Uses context set by executor
 * });
 * }</pre>
 */
public final class SuprimContext {

    private static final ThreadLocal<TransactionContext> CONTEXT = new ThreadLocal<>();

    private SuprimContext() {
        // Utility class
    }

    /**
     * Context data holder.
     */
    record TransactionContext(
        Connection connection,
        SqlDialect dialect
    ) {
        TransactionContext {
            Objects.requireNonNull(connection, "Connection cannot be null");
            Objects.requireNonNull(dialect, "Dialect cannot be null");
        }
    }

    /**
     * Set the current thread's persistence context.
     * Package-private - called by SuprimExecutor.
     *
     * @param connection the database connection
     * @param dialect the SQL dialect
     */
    static void setContext(Connection connection, SqlDialect dialect) {
        CONTEXT.set(new TransactionContext(connection, dialect));
    }

    /**
     * Clear the current thread's persistence context.
     * Package-private - called by SuprimExecutor.
     */
    static void clearContext() {
        CONTEXT.remove();
    }

    /**
     * Check if a transaction context is currently set.
     *
     * @return true if context exists
     */
    public static boolean hasContext() {
        return Objects.nonNull(CONTEXT.get());
    }

    /**
     * Get the current thread's connection.
     *
     * @return current connection
     * @throws IllegalStateException if not in transaction context
     */
    public static Connection getConnection() {
        TransactionContext ctx = CONTEXT.get();
        if (Objects.isNull(ctx)) {
            throw new IllegalStateException(
                "No active transaction context. " +
                "Entity.save() must be called within executor.transaction(tx -> {...})"
            );
        }
        return ctx.connection();
    }

    /**
     * Get the current thread's SQL dialect.
     *
     * @return current dialect
     * @throws IllegalStateException if not in transaction context
     */
    public static SqlDialect getDialect() {
        TransactionContext ctx = CONTEXT.get();
        if (Objects.isNull(ctx)) {
            throw new IllegalStateException(
                "No active transaction context. " +
                "Entity.save() must be called within executor.transaction(tx -> {...})"
            );
        }
        return ctx.dialect();
    }
}
