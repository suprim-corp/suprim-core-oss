package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.dialect.DialectRegistry;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.query.EagerLoadSpec;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.jdbc.eager.EagerLoader;
import sant1ago.dev.suprim.jdbc.event.*;
import sant1ago.dev.suprim.jdbc.exception.*;

import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.Column;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Lightweight query executor for Suprim queries.
 *
 * <pre>{@code
 * // Create executor (once, at app startup)
 * SuprimExecutor executor = SuprimExecutor.create(dataSource);
 *
 * // Simple queries (auto-commit)
 * List<User> users = executor.query(
 *     Suprim.select(User_.ID, User_.EMAIL).from(User_.TABLE).build(),
 *     rs -> new User(rs.getLong("id"), rs.getString("email"))
 * );
 *
 * int count = executor.execute(
 *     Suprim.insertInto(User_.TABLE).column(User_.EMAIL, "test@example.com").build()
 * );
 *
 * // Transactions
 * executor.transaction(tx -> {
 *     tx.execute(insertQuery);
 *     tx.execute(updateQuery);
 *     // auto-commit on success, rollback on exception
 * });
 *
 * // Exception handling
 * try {
 *     executor.execute(insertQuery);
 * } catch (UniqueConstraintException e) {
 *     // Handle duplicate
 * } catch (ForeignKeyException e) {
 *     // Handle missing reference
 * } catch (SuprimException e) {
 *     // Handle other errors
 * }
 * }</pre>
 */
public final class SuprimExecutor {

    private final DataSource dataSource;
    private final EventDispatcher dispatcher;
    private final String connectionName;
    private volatile SqlDialect dialect;

    // Lazy-initialized internal helpers
    private volatile PaginationHelper paginationHelper;
    private volatile ChunkProcessor chunkProcessor;

    private SuprimExecutor(DataSource dataSource) {
        this(dataSource, new EventDispatcher(), "default", null);
    }

    private SuprimExecutor(DataSource dataSource, EventDispatcher dispatcher, String connectionName, SqlDialect dialect) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.dispatcher = dispatcher;
        this.connectionName = connectionName;
        this.dialect = dialect;
    }

    /**
     * Get the SQL dialect, auto-detecting from connection if not explicitly set.
     *
     * @param connection the connection for auto-detection (if needed)
     * @return the SQL dialect
     */
    private SqlDialect getDialect(Connection connection) {
        if (Objects.nonNull(dialect)) {
            return dialect;
        }
        // Auto-detect and cache
        dialect = DialectRegistry.detect(connection);
        return dialect;
    }

    /**
     * Safely quote an identifier, only if it requires quoting.
     * Simple alphanumeric/underscore identifiers are left unquoted to preserve
     * database case-folding behavior (important for H2 which uppercases unquoted identifiers).
     *
     * @param identifier the identifier to potentially quote
     * @param dialect    the SQL dialect for quoting
     * @return the identifier, quoted only if necessary
     */
    private static String safeQuoteIdentifier(String identifier, SqlDialect dialect) {
        if (Objects.isNull(identifier) || identifier.isEmpty()) {
            return identifier;
        }
        // Only quote if identifier contains special characters, spaces, or starts with digit
        // Simple alphanumeric with underscores are safe unquoted and preserve case-folding
        if (identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return identifier; // Safe, no quoting needed
        }
        return dialect.quoteIdentifier(identifier);
    }

    /**
     * Create a new SuprimExecutor with the given DataSource.
     *
     * @param dataSource the DataSource to use for connections
     * @return a new SuprimExecutor instance
     */
    public static SuprimExecutor create(DataSource dataSource) {
        return new SuprimExecutor(dataSource);
    }

    /**
     * Create a builder for configuring SuprimExecutor with listeners.
     *
     * <pre>{@code
     * SuprimExecutor executor = SuprimExecutor.builder(dataSource)
     *     .connectionName("primary")
     *     .onQuery(e -> log.info("SQL: {} [{}ms]", e.sql(), e.durationMs()))
     *     .onSlowQuery(100, e -> log.warn("Slow: {}", e.sql()))
     *     .build();
     * }</pre>
     *
     * @param dataSource the DataSource to use
     * @return a new Builder
     */
    public static Builder builder(DataSource dataSource) {
        return new Builder(dataSource);
    }

    /**
     * Fluent builder for configuring SuprimExecutor with listeners and settings.
     */
    public static final class Builder {
        private final DataSource dataSource;
        private final EventDispatcher dispatcher = new EventDispatcher();
        private String connectionName = "default";
        private SqlDialect dialect;

        private Builder(DataSource dataSource) {
            this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        }

        /**
         * Set the SQL dialect explicitly.
         * If not set, the dialect is auto-detected from the first connection.
         *
         * @param dialect the SQL dialect to use
         * @return this builder
         */
        public Builder dialect(SqlDialect dialect) {
            this.dialect = dialect;
            return this;
        }

        /**
         * Set the connection name for event identification.
         *
         * @param name the connection/datasource name
         * @return this builder
         */
        public Builder connectionName(String name) {
            this.connectionName = name;
            return this;
        }

        /**
         * Add a listener that fires after every successful query.
         *
         * @param handler the handler to invoke
         * @return this builder
         */
        public Builder onQuery(Consumer<QueryEvent> handler) {
            dispatcher.addQueryListener(QueryListener.onQuery(handler));
            return this;
        }

        /**
         * Add a listener for slow queries exceeding threshold.
         *
         * @param thresholdMs minimum duration in milliseconds to trigger
         * @param handler     the handler to invoke
         * @return this builder
         */
        public Builder onSlowQuery(long thresholdMs, Consumer<QueryEvent> handler) {
            dispatcher.addQueryListener(QueryListener.onSlowQuery(thresholdMs, handler));
            return this;
        }

        /**
         * Add a listener for query errors.
         *
         * @param handler the handler to invoke
         * @return this builder
         */
        public Builder onQueryError(Consumer<QueryEvent> handler) {
            dispatcher.addQueryListener(QueryListener.onQueryError(handler));
            return this;
        }

        /**
         * Add a custom query listener.
         *
         * @param listener the listener to add
         * @return this builder
         */
        public Builder addQueryListener(QueryListener listener) {
            dispatcher.addQueryListener(listener);
            return this;
        }

        /**
         * Add a transaction listener.
         *
         * @param listener the listener to add
         * @return this builder
         */
        public Builder addTransactionListener(TransactionListener listener) {
            dispatcher.addTransactionListener(listener);
            return this;
        }

        /**
         * Build the configured SuprimExecutor instance.
         *
         * @return new SuprimExecutor with configured listeners
         */
        public SuprimExecutor build() {
            return new SuprimExecutor(dataSource, dispatcher, connectionName, dialect);
        }
    }

    // ============ Listener Management ============

    /**
     * Add a query listener at runtime.
     *
     * @param listener the listener to add
     */
    public void addQueryListener(QueryListener listener) {
        dispatcher.addQueryListener(listener);
    }

    /**
     * Remove a query listener.
     *
     * @param listener the listener to remove
     * @return true if the listener was found and removed
     */
    public boolean removeQueryListener(QueryListener listener) {
        return dispatcher.removeQueryListener(listener);
    }

    /**
     * Add a transaction listener at runtime.
     *
     * @param listener the listener to add
     */
    public void addTransactionListener(TransactionListener listener) {
        dispatcher.addTransactionListener(listener);
    }

    /**
     * Remove a transaction listener.
     *
     * @param listener the listener to remove
     * @return true if the listener was found and removed
     */
    public boolean removeTransactionListener(TransactionListener listener) {
        return dispatcher.removeTransactionListener(listener);
    }

    // Package-private for Transaction class
    EventDispatcher getDispatcher() {
        return dispatcher;
    }

    String getConnectionName() {
        return connectionName;
    }

    /**
     * Execute a SELECT query and map results using the provided RowMapper.
     *
     * @param queryResult the query result from Suprim builders
     * @param mapper      the row mapper to convert ResultSet rows to objects
     * @param <T>         the type of objects to return
     * @return list of mapped objects
     * @throws QueryException      if query execution fails
     * @throws ConnectionException if connection cannot be obtained
     * @throws MappingException    if row mapping fails
     */
    public <T> List<T> query(QueryResult queryResult, RowMapper<T> mapper) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        // Create and fire before the event
        QueryEvent beforeEvent = QueryEvent.before(converted.sql(), converted.parameters(), connectionName);
        dispatcher.fireBeforeQuery(beforeEvent);

        long startNanos = System.nanoTime();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(converted.sql())) {

            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    try {
                        results.add(mapper.map(rs));
                    } catch (SQLException e) {
                        throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
                    } catch (SuprimException e) {
                        throw e;
                    } catch (Exception e) {
                        throw MappingException.builder()
                                .message("Row mapping failed")
                                .cause(e)
                                .build();
                    }
                }

                // Fire success event
                long durationNanos = System.nanoTime() - startNanos;
                dispatcher.fireAfterQuery(beforeEvent.completed(durationNanos, results.size()));

                // Autoload eager relations if specified
                if (queryResult.hasEagerLoads() && !results.isEmpty()) {
                    EagerLoader loader = new EagerLoader(this);
                    loader.loadRelations(results, queryResult.eagerLoads());
                }

                return results;
            }
        } catch (SQLException e) {
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireQueryError(beforeEvent.failed(durationNanos, e));
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
        } catch (SuprimException e) {
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireQueryError(beforeEvent.failed(durationNanos, e));
            throw e;
        }
    }

    /**
     * Execute a SELECT query and return a single optional result.
     *
     * @param queryResult the query result from Suprim builders
     * @param mapper      the row mapper to convert the ResultSet row to an object
     * @param <T>         the type of object to return
     * @return optional containing the result, or empty if no rows found
     * @throws NonUniqueResultException if more than one row is returned
     * @throws QueryException           if query execution fails
     * @throws ConnectionException      if connection cannot be obtained
     */
    public <T> Optional<T> queryOne(QueryResult queryResult, RowMapper<T> mapper) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        // Create and fire before event
        QueryEvent beforeEvent = QueryEvent.before(converted.sql(), converted.parameters(), connectionName);
        dispatcher.fireBeforeQuery(beforeEvent);

        long startNanos = System.nanoTime();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(converted.sql())) {

            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // Fire success event (0 rows)
                    long durationNanos = System.nanoTime() - startNanos;
                    dispatcher.fireAfterQuery(beforeEvent.completed(durationNanos, 0));
                    return Optional.empty();
                }
                T result = mapper.map(rs);
                if (rs.next()) {
                    throw NonUniqueResultException.forQuery(converted.sql(), converted.parameters());
                }

                // Fire success event (1 row)
                long durationNanos = System.nanoTime() - startNanos;
                dispatcher.fireAfterQuery(beforeEvent.completed(durationNanos, 1));

                // Auto-load eager relations if specified
                if (queryResult.hasEagerLoads() && Objects.nonNull(result)) {
                    EagerLoader loader = new EagerLoader(this);
                    loader.loadRelations(List.of(result), queryResult.eagerLoads());
                }

                return Optional.of(result);
            }
        } catch (SQLException e) {
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireQueryError(beforeEvent.failed(durationNanos, e));
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
        } catch (SuprimException e) {
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireQueryError(beforeEvent.failed(durationNanos, e));
            throw e;
        }
    }

    /**
     * Execute a SELECT query and return exactly one result.
     *
     * @param queryResult the query result from Suprim builders
     * @param mapper      the row mapper to convert the ResultSet row to an object
     * @param <T>         the type of object to return
     * @return the single result
     * @throws NoResultException        if no rows are returned
     * @throws NonUniqueResultException if more than one row is returned
     * @throws QueryException           if query execution fails
     * @throws ConnectionException      if connection cannot be obtained
     */
    public <T> T queryOneRequired(QueryResult queryResult, RowMapper<T> mapper) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        // Create and fire before event
        QueryEvent beforeEvent = QueryEvent.before(converted.sql(), converted.parameters(), connectionName);
        dispatcher.fireBeforeQuery(beforeEvent);

        long startNanos = System.nanoTime();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(converted.sql())) {

            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw NoResultException.forQuery(converted.sql(), converted.parameters());
                }
                T result = mapper.map(rs);
                if (rs.next()) {
                    throw NonUniqueResultException.forQuery(converted.sql(), converted.parameters());
                }

                // Fire success event
                long durationNanos = System.nanoTime() - startNanos;
                dispatcher.fireAfterQuery(beforeEvent.completed(durationNanos, 1));

                return result;
            }
        } catch (SQLException e) {
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireQueryError(beforeEvent.failed(durationNanos, e));
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
        } catch (SuprimException e) {
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireQueryError(beforeEvent.failed(durationNanos, e));
            throw e;
        }
    }

    /**
     * Execute an INSERT, UPDATE, or DELETE statement.
     *
     * @param queryResult the query result from Suprim builders
     * @return the number of affected rows
     * @throws ExecutionException           if statement execution fails
     * @throws ConstraintViolationException if a constraint is violated
     * @throws ConnectionException          if connection cannot be obtained
     */
    public int execute(QueryResult queryResult) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        // Create and fire before event
        QueryEvent beforeEvent = QueryEvent.before(converted.sql(), converted.parameters(), connectionName);
        dispatcher.fireBeforeQuery(beforeEvent);

        long startNanos = System.nanoTime();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(converted.sql())) {

            setParameters(ps, converted.parameters());
            int affected = ps.executeUpdate();

            // Fire success event
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireAfterQuery(beforeEvent.completed(durationNanos, affected));

            return affected;
        } catch (SQLException e) {
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireQueryError(beforeEvent.failed(durationNanos, e));
            throw ExceptionTranslator.translateExecution(converted.sql(), converted.parameters(), e);
        }
    }

    /**
     * Execute multiple statements within a transaction.
     * Auto-commits on success, rolls back on exception.
     *
     * <pre>{@code
     * executor.transaction(tx -> {
     *     tx.execute(insertOrder);
     *     tx.execute(updateInventory);
     *     List<Order> orders = tx.query(selectOrders, Order.class);
     * });
     * }</pre>
     *
     * @param action the action to execute within the transaction
     * @throws TransactionException if transaction management fails
     * @throws SuprimException      if a database error occurs within the transaction
     */
    public void transaction(Consumer<Transaction> action) {
        Connection conn = null;
        TransactionEvent beginEvent = null;
        long startNanos = System.nanoTime();

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // Fire BEGIN event
            beginEvent = TransactionEvent.begin(connectionName);
            dispatcher.fireTransactionEvent(beginEvent);

            Transaction tx = new Transaction(conn, dispatcher, connectionName, beginEvent);
            action.accept(tx);

            conn.commit();

            // Fire COMMIT event
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireTransactionEvent(beginEvent.commit(durationNanos));
        } catch (SQLException e) {
            rollbackQuietly(conn);

            // Fire ROLLBACK event
            if (Objects.nonNull(beginEvent)) {
                long durationNanos = System.nanoTime() - startNanos;
                dispatcher.fireTransactionEvent(beginEvent.rollback(durationNanos));
            }

            throw TransactionException.fromSQLException(e);
        } catch (SuprimException e) {
            rollbackQuietly(conn);

            // Fire ROLLBACK event
            if (Objects.nonNull(beginEvent)) {
                long durationNanos = System.nanoTime() - startNanos;
                dispatcher.fireTransactionEvent(beginEvent.rollback(durationNanos));
            }

            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);

            // Fire ROLLBACK event
            if (Objects.nonNull(beginEvent)) {
                long durationNanos = System.nanoTime() - startNanos;
                dispatcher.fireTransactionEvent(beginEvent.rollback(durationNanos));
            }

            throw TransactionException.builder()
                    .message("Transaction failed")
                    .cause(e)
                    .build();
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Execute multiple statements within a transaction and return a result.
     *
     * <pre>{@code
     * Long orderId = executor.transactionWithResult(tx -> {
     *     tx.execute(insertOrder);
     *     return tx.queryOne(selectLastId, rs -> rs.getLong(1)).orElseThrow();
     * });
     * }</pre>
     *
     * @param action the action to execute within the transaction
     * @param <T>    the type of result to return
     * @return the result from the action
     * @throws TransactionException if transaction management fails
     * @throws SuprimException      if a database error occurs within the transaction
     */
    public <T> T transactionWithResult(TransactionFunction<T> action) {
        Connection conn = null;
        TransactionEvent beginEvent = null;
        long startNanos = System.nanoTime();

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // Fire BEGIN event
            beginEvent = TransactionEvent.begin(connectionName);
            dispatcher.fireTransactionEvent(beginEvent);

            Transaction tx = new Transaction(conn, dispatcher, connectionName, beginEvent);
            T result = action.apply(tx);

            conn.commit();

            // Fire COMMIT event
            long durationNanos = System.nanoTime() - startNanos;
            dispatcher.fireTransactionEvent(beginEvent.commit(durationNanos));

            return result;
        } catch (SQLException e) {
            rollbackQuietly(conn);

            // Fire ROLLBACK event
            if (Objects.nonNull(beginEvent)) {
                long durationNanos = System.nanoTime() - startNanos;
                dispatcher.fireTransactionEvent(beginEvent.rollback(durationNanos));
            }

            throw TransactionException.fromSQLException(e);
        } catch (SuprimException e) {
            rollbackQuietly(conn);

            // Fire ROLLBACK event
            if (Objects.nonNull(beginEvent)) {
                long durationNanos = System.nanoTime() - startNanos;
                dispatcher.fireTransactionEvent(beginEvent.rollback(durationNanos));
            }

            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);

            // Fire ROLLBACK event
            if (Objects.nonNull(beginEvent)) {
                long durationNanos = System.nanoTime() - startNanos;
                dispatcher.fireTransactionEvent(beginEvent.rollback(durationNanos));
            }

            throw TransactionException.builder()
                    .message("Transaction failed")
                    .cause(e)
                    .build();
        } finally {
            closeQuietly(conn);
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw ConnectionException.fromSQLException(e);
        }
    }

    private void setParameters(PreparedStatement ps, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            ps.setObject(i + 1, parameters[i]);
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (Objects.nonNull(conn)) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // Ignore rollback errors
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (Objects.nonNull(conn)) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
                // Ignore close errors
            }
        }
    }

    // ============ Internal Helpers ============

    /**
     * Package-private connection accessor for internal helpers.
     */
    Connection getConnectionInternal() throws SQLException {
        return getConnection();
    }

    private PaginationHelper getPaginationHelper() {
        if (Objects.isNull(paginationHelper)) {
            synchronized (this) {
                if (Objects.isNull(paginationHelper)) {
                    paginationHelper = new PaginationHelper(this);
                }
            }
        }
        return paginationHelper;
    }

    private ChunkProcessor getChunkProcessor() {
        if (Objects.isNull(chunkProcessor)) {
            synchronized (this) {
                if (Objects.isNull(chunkProcessor)) {
                    chunkProcessor = new ChunkProcessor(this);
                }
            }
        }
        return chunkProcessor;
    }

    /**
     * Functional interface for transactions that return a value.
     *
     * @param <T> the type of result
     */
    @FunctionalInterface
    public interface TransactionFunction<T> {
        T apply(Transaction tx) throws Exception;
    }

    /**
     * Lazily load relations on already-queried entities.
     * Useful for loading relations conditionally after the initial query.
     *
     * <pre>{@code
     * List<User> users = executor.query(selectQuery, EntityMapper.of(User.class));
     * // Later, conditionally load posts
     * if (includeRelations) {
     *     executor.loadMissing(users, User_.POSTS, User_.PROFILE);
     * }
     * }</pre>
     *
     * @param entities  the entities to load relations for
     * @param relations the relations to load
     * @param <T>       the entity type
     */
    @SafeVarargs
    public final <T> void loadMissing(List<T> entities, Relation<T, ?>... relations) {
        if (entities.isEmpty() || relations.length == 0) {
            return;
        }

        List<EagerLoadSpec> specs = new ArrayList<>();
        for (Relation<T, ?> relation : relations) {
            specs.add(EagerLoadSpec.of(relation));
        }

        EagerLoader loader = new EagerLoader(this);
        loader.loadRelations(entities, specs);
    }

    /**
     * Get a RelationshipManager for executing relationship mutations within a transaction.
     * Use this to perform associate, save, attach, sync, and other relationship operations.
     *
     * <pre>{@code
     * executor.transaction(tx -> {
     *     RelationshipManager rm = tx.relationships();
     *     rm.associate(post, Post_.AUTHOR, user);
     *     rm.save(user, User_.POSTS, newPost);
     *     rm.attach(user, User_.ROLES, roleId);
     * });
     * }</pre>
     *
     * @param tx the transaction context
     * @return RelationshipManager instance for the transaction
     */
    public static RelationshipManager relationships(Transaction tx) {
        return new RelationshipManager(tx);
    }

    // ==================== PAGINATION ====================

    /**
     * Execute a query with pagination metadata.
     *
     * <pre>{@code
     * PaginatedResult<User> users = executor.paginate(
     *     Suprim.select().from(User_.TABLE).where(User_.IS_ACTIVE.eq(true)),
     *     1, 20, User.class
     * );
     * users.getData();        // List<User>
     * users.getTotal();       // 100
     * users.hasMorePages();   // true
     * }</pre>
     *
     * @param builder   the select builder (without limit/offset)
     * @param page      page number (1-based)
     * @param perPage   items per page
     * @param entityClass the entity class for mapping
     * @param <T>       entity type
     * @return paginated result with data and metadata
     */
    public <T> PaginatedResult<T> paginate(SelectBuilder builder, int page, int perPage, Class<T> entityClass) {
        return paginate(builder, page, perPage, EntityMapper.of(entityClass));
    }

    /**
     * Execute a query with pagination metadata using custom mapper.
     */
    public <T> PaginatedResult<T> paginate(SelectBuilder builder, int page, int perPage, RowMapper<T> mapper) {
        return getPaginationHelper().paginate(builder, page, perPage, mapper);
    }

    /**
     * Execute cursor-based pagination for efficient large dataset traversal.
     *
     * <pre>{@code
     * CursorResult<User> result = executor.cursorPaginate(
     *     Suprim.select().from(User_.TABLE),
     *     null, 20, User.class, User_.ID
     * );
     * // Next page
     * CursorResult<User> next = executor.cursorPaginate(query, result.getNextCursor(), 20, ...);
     * }</pre>
     *
     * @param builder     the select builder
     * @param cursor      cursor from previous page (null for first page)
     * @param perPage     items per page
     * @param entityClass entity class
     * @param cursorColumn column to use for cursor (typically ID)
     * @param <T>         entity type
     * @return cursor result with data and cursors
     */
    public <T, V> CursorResult<T> cursorPaginate(SelectBuilder builder, String cursor, int perPage, Class<T> entityClass, Column<T, V> cursorColumn) {
        return cursorPaginate(builder, cursor, perPage, EntityMapper.of(entityClass), cursorColumn);
    }

    /**
     * Execute true keyset (cursor-based) pagination.
     * Uses WHERE column > lastValue instead of OFFSET for O(1) performance on large datasets.
     *
     * <p>Unlike offset pagination which degrades linearly (OFFSET 100000 must skip 100k rows),
     * keyset pagination maintains constant performance regardless of page depth.</p>
     *
     * <p><strong>Note:</strong> Keyset pagination only supports forward navigation.
     * For bidirectional navigation, use offset pagination or implement seek-based pagination.</p>
     */
    public <T, V> CursorResult<T> cursorPaginate(SelectBuilder builder, String cursor, int perPage, RowMapper<T> mapper, Column<T, V> cursorColumn) {
        return getPaginationHelper().cursorPaginate(builder, cursor, perPage, mapper, cursorColumn);
    }

    /**
     * Count total rows for a query (without pagination).
     */
    public long count(SelectBuilder builder) {
        return getPaginationHelper().count(builder);
    }

    // ==================== CHUNKING ====================

    /**
     * Process query results in chunks to avoid memory issues with large datasets.
     *
     * <pre>{@code
     * executor.chunk(
     *     Suprim.select().from(User_.TABLE),
     *     1000, User.class,
     *     users -> {
     *         for (User user : users) {
     *             processUser(user);
     *         }
     *         return true; // continue chunking
     *     }
     * );
     * }</pre>
     *
     * @param builder     the select builder
     * @param chunkSize   number of rows per chunk
     * @param entityClass entity class
     * @param processor   function to process each chunk, return false to stop
     * @param <T>         entity type
     * @return total number of processed rows
     */
    public <T> long chunk(SelectBuilder builder, int chunkSize, Class<T> entityClass, Function<List<T>, Boolean> processor) {
        return chunk(builder, chunkSize, EntityMapper.of(entityClass), processor);
    }

    /**
     * Process query results in chunks with custom mapper.
     */
    public <T> long chunk(SelectBuilder builder, int chunkSize, RowMapper<T> mapper, Function<List<T>, Boolean> processor) {
        return getChunkProcessor().chunk(builder, chunkSize, mapper, processor);
    }

    /**
     * Process query results in chunks by ID using keyset pagination (safe for updates during iteration).
     * Uses WHERE id > lastId instead of OFFSET for O(1) performance on large datasets.
     *
     * @param builder      the select builder
     * @param chunkSize    number of rows per chunk
     * @param entityClass  entity class
     * @param idColumn     the ID column for ordering and keyset
     * @param processor    function to process each chunk, return false to stop
     * @param <T>          entity type
     * @param <V>          ID column type
     * @return total number of processed rows
     */
    public <T, V> long chunkById(SelectBuilder builder, int chunkSize, Class<T> entityClass, Column<T, V> idColumn, Function<List<T>, Boolean> processor) {
        return chunkById(builder, chunkSize, EntityMapper.of(entityClass), idColumn, processor);
    }

    /**
     * Process query results in chunks by ID with custom mapper using keyset pagination.
     * Uses WHERE id > lastId instead of OFFSET for O(1) performance on large datasets.
     */
    public <T, V> long chunkById(SelectBuilder builder, int chunkSize, RowMapper<T> mapper, Column<T, V> idColumn, Function<List<T>, Boolean> processor) {
        return getChunkProcessor().chunkById(builder, chunkSize, mapper, idColumn, processor);
    }

    /**
     * Create a lazy stream for memory-efficient iteration.
     *
     * <pre>{@code
     * try (Stream<User> stream = executor.lazy(query, User.class)) {
     *     stream.filter(u -> u.isActive()).forEach(this::process);
     * }
     * }</pre>
     *
     * @param queryResult the query result
     * @param entityClass entity class
     * @param <T>         entity type
     * @return lazy stream (must be closed after use)
     */
    public <T> Stream<T> lazy(QueryResult queryResult, Class<T> entityClass) {
        return lazy(queryResult, EntityMapper.of(entityClass));
    }

    /**
     * Create a lazy stream with custom mapper.
     */
    public <T> Stream<T> lazy(QueryResult queryResult, RowMapper<T> mapper) {
        return getChunkProcessor().lazy(queryResult, mapper);
    }

    // ==================== QUERY RESULT METHODS ====================

    /**
     * Get first result or empty Optional.
     *
     * <pre>{@code
     * Optional<User> user = executor.first(query, User.class);
     * }</pre>
     */
    public <T> Optional<T> first(QueryResult queryResult, Class<T> entityClass) {
        return queryOne(queryResult, EntityMapper.of(entityClass));
    }

    /**
     * Get first result or throw NoResultException.
     *
     * <pre>{@code
     * User user = executor.firstOrFail(query, User.class);
     * }</pre>
     */
    public <T> T firstOrFail(QueryResult queryResult, Class<T> entityClass) {
        return queryOneRequired(queryResult, EntityMapper.of(entityClass));
    }

    /**
     * Find an entity by its primary key ID.
     * Uses reflection to extract table name and ID column from entity annotations.
     *
     * <pre>{@code
     * Optional<User> user = executor.findById(User.class, 1L);
     * }</pre>
     *
     * @param entityClass the entity class (must have @Entity/@Table and @Id annotations)
     * @param id          the primary key value
     * @param <T>         the entity type
     * @return optional containing the entity, or empty if not found
     * @throws IllegalArgumentException if entityClass is null, id is null, or class lacks required annotations
     * @throws QueryException           if query execution fails
     */
    public <T> Optional<T> findById(Class<T> entityClass, Object id) {
        if (Objects.isNull(entityClass)) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }
        if (Objects.isNull(id)) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(entityClass);

        try (Connection conn = getConnection()) {
            SqlDialect sqlDialect = getDialect(conn);

            // Build a qualified table name with safe identifier quoting (quotes only when needed)
            String tableName = Objects.nonNull(meta.schema()) && !meta.schema().isEmpty()
                    ? safeQuoteIdentifier(meta.schema(), sqlDialect) + "." + safeQuoteIdentifier(meta.tableName(), sqlDialect)
                    : safeQuoteIdentifier(meta.tableName(), sqlDialect);

            // Build a parameterized query with SQL:2008 standard FETCH FIRST (more portable)
            String sql = "SELECT * FROM " + tableName + " WHERE " + safeQuoteIdentifier(meta.idColumn(), sqlDialect) + " = ? FETCH FIRST 1 ROWS ONLY";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setObject(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(EntityMapper.of(entityClass).map(rs));
                }
            } catch (SQLException e) {
                throw ExceptionTranslator.translateQuery(sql, new Object[]{id}, e);
            }
        } catch (SQLException e) {
            throw ConnectionException.fromSQLException(e);
        }
    }

    /**
     * Find an entity by its primary key ID, or throw if not found.
     *
     * <pre>{@code
     * User user = executor.findByIdOrFail(User.class, 1L);
     * }</pre>
     *
     * @param entityClass the entity class (must have @Entity/@Table and @Id annotations)
     * @param id          the primary key value
     * @param <T>         the entity type
     * @return the entity
     * @throws IllegalArgumentException if entityClass is null, id is null, or class lacks required annotations
     * @throws NoResultException        if no entity found with the given ID
     * @throws QueryException           if query execution fails
     */
    public <T> T findByIdOrFail(Class<T> entityClass, Object id) {
        return findById(entityClass, id)
                .orElseThrow(() -> NoResultException.builder()
                        .message("No " + entityClass.getSimpleName() + " found with ID: " + id)
                        .build());
    }

    /**
     * Get exactly one result, throw if 0 or >1.
     *
     * <pre>{@code
     * User user = executor.sole(query, User.class);
     * }</pre>
     */
    public <T> T sole(QueryResult queryResult, Class<T> entityClass) {
        return queryOneRequired(queryResult, EntityMapper.of(entityClass));
    }

    /**
     * Get a single column value from first row.
     *
     * <pre>{@code
     * Optional<String> email = executor.value(query, "email");
     * }</pre>
     */
    public <T> Optional<T> value(QueryResult queryResult, String columnName, Class<T> type) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(converted.sql())) {

            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    T value = type.cast(rs.getObject(columnName));
                    return Optional.ofNullable(value);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
        }
    }

    /**
     * Get a column as list.
     *
     * <pre>{@code
     * List<String> emails = executor.pluck(query, "email", String.class);
     * }</pre>
     */
    public <T> List<T> pluck(QueryResult queryResult, String columnName, Class<T> type) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(converted.sql())) {

            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    T value = type.cast(rs.getObject(columnName));
                    results.add(value);
                }
                return results;
            }
        } catch (SQLException e) {
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
        }
    }

    /**
     * Get a column as map keyed by another column.
     *
     * <pre>{@code
     * Map<Long, String> emailsById = executor.pluck(query, "email", "id", String.class, Long.class);
     * }</pre>
     */
    public <K, V> Map<K, V> pluck(QueryResult queryResult, String valueColumn, String keyColumn, Class<V> valueType, Class<K> keyType) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(converted.sql())) {

            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                Map<K, V> results = new LinkedHashMap<>();
                while (rs.next()) {
                    K key = keyType.cast(rs.getObject(keyColumn));
                    V value = valueType.cast(rs.getObject(valueColumn));
                    results.put(key, value);
                }
                return results;
            }
        } catch (SQLException e) {
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
        }
    }
}
