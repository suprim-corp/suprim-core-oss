package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.dialect.SqlDialect;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Objects;

/**
 * Thread-local context holder for Active Record pattern.
 *
 * <p>Provides persistence context (connection + dialect) to entities
 * calling {@code save()} within a transaction. Context is set by
 * {@link SuprimExecutor} and should not be manipulated directly.
 *
 * <p><b>Auto-commit mode:</b> Register executor globally to enable
 * {@code entity.save()} outside explicit transactions:
 *
 * <pre>{@code
 * // At app startup - register globally
 * SuprimContext.setGlobalExecutor(executor);
 *
 * // Now save() works anywhere (auto-commit)
 * User user = new User();
 * user.setEmail("test@example.com");
 * user.save();  // Auto-commits immediately
 *
 * // Explicit transaction still works (for atomic multi-ops)
 * executor.transaction(tx -> {
 *     tx.save(user);
 *     tx.save(profile);  // Both commit together
 * });
 * }</pre>
 */
public final class SuprimContext {

    private static final ThreadLocal<TransactionContext> CONTEXT = new ThreadLocal<>();
    private static volatile SuprimExecutor globalExecutor;

    private SuprimContext() {
        // Utility class
    }

    /**
     * Register a global executor for auto-commit mode.
     *
     * <p>Once set, entities can call {@code save()}, {@code update()},
     * {@code delete()} outside explicit transactions. Each operation
     * will auto-commit immediately.
     *
     * <p>Call this once at application startup:
     * <pre>{@code
     * SuprimExecutor executor = SuprimExecutor.create(dataSource);
     * SuprimContext.setGlobalExecutor(executor);
     * }</pre>
     *
     * @param executor the executor to use for auto-commit operations
     */
    public static void setGlobalExecutor(SuprimExecutor executor) {
        globalExecutor = Objects.requireNonNull(executor, "Executor cannot be null");
    }

    /**
     * Get the global executor for auto-commit operations.
     *
     * @return the global executor, or null if not set
     */
    static SuprimExecutor getGlobalExecutor() {
        return globalExecutor;
    }

    /**
     * Check if a global executor is registered.
     *
     * @return true if global executor is set
     */
    public static boolean hasGlobalExecutor() {
        return Objects.nonNull(globalExecutor);
    }

    /**
     * Clear the global executor (useful for testing).
     */
    public static void clearGlobalExecutor() {
        globalExecutor = null;
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
