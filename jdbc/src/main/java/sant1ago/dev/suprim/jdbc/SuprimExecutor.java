package sant1ago.dev.suprim.jdbc;

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
import java.util.stream.StreamSupport;

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

    private SuprimExecutor(DataSource dataSource) {
        this(dataSource, new EventDispatcher(), "default");
    }

    private SuprimExecutor(DataSource dataSource, EventDispatcher dispatcher, String connectionName) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.dispatcher = dispatcher;
        this.connectionName = connectionName;
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

        private Builder(DataSource dataSource) {
            this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
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
            return new SuprimExecutor(dataSource, dispatcher, connectionName);
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
     * @return true if listener was found and removed
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
     * @return true if listener was found and removed
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

        // Create and fire before event
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
        if (page < 1) page = 1;
        if (perPage < 1) perPage = 10;

        // Count total
        long total = count(builder);

        // Get page data
        QueryResult dataQuery = builder.paginate(page, perPage).build();
        List<T> data = query(dataQuery, mapper);

        return PaginatedResult.of(data, page, perPage, total);
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
        if (perPage < 1) perPage = 10;

        // Decode cursor to get last seen value
        String decodedCursor = CursorResult.decodeCursor(cursor);
        V lastValue = null;

        if (Objects.nonNull(decodedCursor) && !decodedCursor.isEmpty()) {
            lastValue = parseCursorValue(decodedCursor, cursorColumn.getValueType());
        }

        // Build query with keyset condition (WHERE column > lastValue)
        SelectBuilder queryBuilder = builder.orderBy(cursorColumn.asc());
        if (Objects.nonNull(lastValue)) {
            queryBuilder = queryBuilder.where(cursorColumn.gt(lastValue));
        }
        QueryResult dataQuery = queryBuilder.limit(perPage + 1).build();

        List<T> results = query(dataQuery, mapper);

        // Determine if there are more pages
        boolean hasMore = results.size() > perPage;
        List<T> data = hasMore ? results.subList(0, perPage) : results;

        // Build next cursor from last item's column value
        String nextCursor = null;
        if (hasMore && !data.isEmpty()) {
            T lastItem = data.get(data.size() - 1);
            Object lastColValue = EntityReflector.getFieldByColumnName(lastItem, cursorColumn.getName());
            nextCursor = CursorResult.encodeCursor(lastColValue);
        }

        // Keyset pagination doesn't support backward navigation efficiently
        // (would need to reverse sort and use < operator)
        return CursorResult.of(data, nextCursor, null, perPage);
    }

    /**
     * Parse cursor value string to appropriate type with type safety.
     *
     * @param value      the string value to parse
     * @param targetType the target class type
     * @param <V>        the value type
     * @return parsed value of type V
     */
    private <V> V parseCursorValue(String value, Class<V> targetType) {
        if (Objects.isNull(value)) return null;

        Object result;
        try {
            if (targetType == Long.class || targetType == long.class) {
                result = Long.parseLong(value);
            } else if (targetType == Integer.class || targetType == int.class) {
                result = Integer.parseInt(value);
            } else if (targetType == Double.class || targetType == double.class) {
                result = Double.parseDouble(value);
            } else if (targetType == Float.class || targetType == float.class) {
                result = Float.parseFloat(value);
            } else if (targetType == String.class) {
                result = value;
            } else if (targetType == java.util.UUID.class) {
                result = java.util.UUID.fromString(value);
            } else if (targetType == java.time.LocalDateTime.class) {
                result = java.time.LocalDateTime.parse(value);
            } else if (targetType == java.time.LocalDate.class) {
                result = java.time.LocalDate.parse(value);
            } else if (targetType == java.time.Instant.class) {
                result = java.time.Instant.parse(value);
            } else {
                // Fallback: try to use as-is (String)
                result = value;
            }
            return targetType.cast(result);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot cast cursor value '" + value + "' to " + targetType.getSimpleName(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse cursor value '" + value + "' as " + targetType.getSimpleName(), e);
        }
    }

    /**
     * Count total rows for a query (without pagination).
     */
    public long count(SelectBuilder builder) {
        // Build the original query to get SQL and parameters
        QueryResult original = builder.build();
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(original);

        // Wrap the original query in a count query
        String countSql = "SELECT COUNT(*) FROM (" + converted.sql() + ") AS count_query";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {

            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw ExceptionTranslator.translateQuery(countSql, converted.parameters(), e);
        }
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
        if (chunkSize < 1) chunkSize = 1000;

        long totalProcessed = 0;
        int offset = 0;

        while (true) {
            QueryResult chunkQuery = builder.limit(chunkSize).offset(offset).build();
            List<T> chunk = query(chunkQuery, mapper);

            if (chunk.isEmpty()) {
                break;
            }

            totalProcessed += chunk.size();

            Boolean shouldContinue = processor.apply(chunk);
            if (Boolean.FALSE.equals(shouldContinue)) {
                break;
            }

            if (chunk.size() < chunkSize) {
                break; // Last chunk
            }

            offset += chunkSize;
        }

        return totalProcessed;
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
        if (chunkSize < 1) chunkSize = 1000;

        long totalProcessed = 0;
        V lastId = null;
        Class<V> idType = idColumn.getValueType();

        while (true) {
            // Build query with keyset condition (WHERE id > lastId)
            SelectBuilder queryBuilder = builder.orderBy(idColumn.asc());
            if (Objects.nonNull(lastId)) {
                queryBuilder = queryBuilder.where(idColumn.gt(lastId));
            }
            QueryResult chunkQuery = queryBuilder.limit(chunkSize).build();

            List<T> chunk = query(chunkQuery, mapper);

            if (chunk.isEmpty()) {
                break;
            }

            totalProcessed += chunk.size();

            Boolean shouldContinue = processor.apply(chunk);
            if (Boolean.FALSE.equals(shouldContinue)) {
                break;
            }

            if (chunk.size() < chunkSize) {
                break; // Last chunk
            }

            // Get last ID for next iteration (keyset) - type-safe cast via Class.cast()
            T lastItem = chunk.get(chunk.size() - 1);
            Object lastIdValue = EntityReflector.getFieldByColumnName(lastItem, idColumn.getName());
            lastId = idType.cast(lastIdValue);
        }

        return totalProcessed;
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
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    converted.sql(),
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY
            );
            ps.setFetchSize(100); // Stream in batches

            setParameters(ps, converted.parameters());
            ResultSet rs = ps.executeQuery();

            Iterator<T> iterator = new Iterator<>() {
                private boolean hasNext;
                private boolean nextChecked = false;

                @Override
                public boolean hasNext() {
                    if (!nextChecked) {
                        try {
                            hasNext = rs.next();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        nextChecked = true;
                    }
                    return hasNext;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    nextChecked = false;
                    try {
                        return mapper.map(rs);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
            return StreamSupport.stream(spliterator, false)
                    .onClose(() -> {
                        try {
                            rs.close();
                            ps.close();
                            conn.close();
                        } catch (SQLException ignored) {
                        }
                    });
        } catch (SQLException e) {
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
        }
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

        // Build qualified table name (unquoted for portability across databases)
        String tableName = Objects.nonNull(meta.schema()) && !meta.schema().isEmpty()
                ? meta.schema() + "." + meta.tableName()
                : meta.tableName();

        // Build parameterized query (unquoted column for portability, FETCH FIRST for SQL standard)
        String sql = "SELECT * FROM " + tableName + " WHERE " + meta.idColumn() + " = ? FETCH FIRST 1 ROWS ONLY";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
