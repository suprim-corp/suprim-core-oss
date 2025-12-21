package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.query.QueryResult;
import sant1ago.dev.suprim.jdbc.event.*;
import sant1ago.dev.suprim.jdbc.exception.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Transaction context for executing multiple statements within a single transaction.
 *
 * <pre>{@code
 * executor.transaction(tx -> {
 *     // Execute insert
 *     tx.execute(Suprim.insertInto(Order_.TABLE)
 *         .column(Order_.USER_ID, userId)
 *         .build());
 *
 *     // Execute update
 *     tx.execute(Suprim.update(User_.TABLE)
 *         .set(User_.BALANCE, newBalance)
 *         .where(User_.ID.eq(userId))
 *         .build());
 *
 *     // Query within transaction
 *     List<Order> orders = tx.query(selectQuery, rs -> new Order(...));
 *
 *     // Partial rollback with savepoints
 *     Savepoint sp = tx.savepoint("before_risky_operation");
 *     try {
 *         tx.execute(riskyQuery);
 *     } catch (SuprimException e) {
 *         tx.rollbackTo(sp);
 *         tx.execute(fallbackQuery);
 *     }
 * });
 * }</pre>
 *
 * <p>Transactions are managed by {@link SuprimExecutor} - do not commit or rollback directly.
 */
public final class Transaction {

    private final Connection connection;
    private final EventDispatcher dispatcher;
    private final String connectionName;
    private final TransactionEvent beginEvent;

    /**
     * Legacy constructor for backward compatibility.
     */
    Transaction(Connection connection) {
        this(connection, new EventDispatcher(), "default", null);
    }

    /**
     * Constructor with event support.
     */
    Transaction(Connection connection, EventDispatcher dispatcher, String connectionName, TransactionEvent beginEvent) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        this.dispatcher = Objects.nonNull(dispatcher) ? dispatcher : new EventDispatcher();
        this.connectionName = Objects.nonNull(connectionName) ? connectionName : "default";
        this.beginEvent = beginEvent;
    }

    /**
     * Execute a SELECT query and map results using the provided RowMapper.
     *
     * @param queryResult the query result from Suprim builders
     * @param mapper      the row mapper to convert ResultSet rows to objects
     * @param <T>         the type of objects to return
     * @return list of mapped objects
     * @throws QueryException   if query execution fails
     * @throws MappingException if row mapping fails
     */
    public <T> List<T> query(QueryResult queryResult, RowMapper<T> mapper) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        // Create and fire before event
        QueryEvent beforeEvent = QueryEvent.before(converted.sql(), converted.parameters(), connectionName);
        dispatcher.fireBeforeQuery(beforeEvent);

        long startNanos = System.nanoTime();

        try (PreparedStatement ps = connection.prepareStatement(converted.sql())) {
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
     */
    public <T> Optional<T> queryOne(QueryResult queryResult, RowMapper<T> mapper) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        try (PreparedStatement ps = connection.prepareStatement(converted.sql())) {
            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                T result = mapper.map(rs);
                if (rs.next()) {
                    throw NonUniqueResultException.forQuery(converted.sql(), converted.parameters());
                }
                return Optional.of(result);
            }
        } catch (SQLException e) {
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
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
     */
    public <T> T queryOneRequired(QueryResult queryResult, RowMapper<T> mapper) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        try (PreparedStatement ps = connection.prepareStatement(converted.sql())) {
            setParameters(ps, converted.parameters());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw NoResultException.forQuery(converted.sql(), converted.parameters());
                }
                T result = mapper.map(rs);
                if (rs.next()) {
                    throw NonUniqueResultException.forQuery(converted.sql(), converted.parameters());
                }
                return result;
            }
        } catch (SQLException e) {
            throw ExceptionTranslator.translateQuery(converted.sql(), converted.parameters(), e);
        }
    }

    /**
     * Execute an INSERT, UPDATE, or DELETE statement.
     *
     * @param queryResult the query result from Suprim builders
     * @return the number of affected rows
     * @throws ExecutionException           if statement execution fails
     * @throws ConstraintViolationException if a constraint is violated
     */
    public int execute(QueryResult queryResult) {
        SqlParameterConverter.Result converted = SqlParameterConverter.convert(queryResult);

        // Create and fire before event
        QueryEvent beforeEvent = QueryEvent.before(converted.sql(), converted.parameters(), connectionName);
        dispatcher.fireBeforeQuery(beforeEvent);

        long startNanos = System.nanoTime();

        try (PreparedStatement ps = connection.prepareStatement(converted.sql())) {
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
     * Create a savepoint that can be used to partially rollback.
     *
     * @param name the savepoint name
     * @return the created savepoint
     * @throws SavepointException if savepoint creation fails
     */
    public Savepoint savepoint(String name) {
        try {
            Savepoint sp = connection.setSavepoint(name);

            // Fire savepoint created event
            if (Objects.nonNull(beginEvent)) {
                dispatcher.fireTransactionEvent(beginEvent.savepointCreated(name));
            }

            return sp;
        } catch (SQLException e) {
            throw SavepointException.createFailed(name, e);
        }
    }

    /**
     * Rollback to a previously created savepoint.
     *
     * @param savepoint the savepoint to rollback to
     * @throws SavepointException if rollback fails
     */
    public void rollbackTo(Savepoint savepoint) {
        try {
            String name = getSavepointName(savepoint);
            connection.rollback(savepoint);

            // Fire savepoint rollback event
            if (Objects.nonNull(beginEvent)) {
                dispatcher.fireTransactionEvent(beginEvent.savepointRollback(name));
            }
        } catch (SQLException e) {
            String name = getSavepointName(savepoint);
            throw SavepointException.rollbackFailed(name, e);
        }
    }

    /**
     * Release a savepoint (frees database resources).
     *
     * @param savepoint the savepoint to release
     * @throws SavepointException if release fails
     */
    public void releaseSavepoint(Savepoint savepoint) {
        try {
            String name = getSavepointName(savepoint);
            connection.releaseSavepoint(savepoint);

            // Fire savepoint released event
            if (Objects.nonNull(beginEvent)) {
                dispatcher.fireTransactionEvent(beginEvent.savepointReleased(name));
            }
        } catch (SQLException e) {
            String name = getSavepointName(savepoint);
            throw SavepointException.releaseFailed(name, e);
        }
    }

    /**
     * Get the underlying connection for advanced use cases.
     * Use with caution - do not commit, rollback, or close the connection directly.
     *
     * @return the underlying JDBC connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Get a RelationshipManager for executing relationship mutations.
     *
     * <pre>{@code
     * executor.transaction(tx -> {
     *     RelationshipManager rm = tx.relationships();
     *     rm.associate(post, Post_.AUTHOR, user);
     *     rm.save(user, User_.POSTS, newPost);
     * });
     * }</pre>
     *
     * @return RelationshipManager instance for this transaction
     */
    public RelationshipManager relationships() {
        return new RelationshipManager(this);
    }

    // ==================== ENTITY PERSISTENCE ====================

    /**
     * Save an entity with automatic ID generation based on @Id strategy.
     *
     * <pre>{@code
     * executor.transaction(tx -> {
     *     User user = new User();
     *     user.setEmail("test@example.com");
     *
     *     User saved = tx.save(user);  // ID generated automatically
     *     System.out.println(saved.getId());
     * });
     * }</pre>
     *
     * <p>Supports all ID generation strategies:
     * <ul>
     *   <li>{@code UUID} / {@code UUID_V7} - Application generates before insert</li>
     *   <li>{@code IDENTITY} - Database generates (SERIAL/AUTO_INCREMENT)</li>
     *   <li>{@code UUID_DB} - Database generates (gen_random_uuid())</li>
     *   <li>{@code SEQUENCE} - Database sequence</li>
     *   <li>Custom generator - Your own IdGenerator implementation</li>
     * </ul>
     *
     * @param entity the entity to save
     * @param <T> entity type
     * @return the saved entity with ID set
     * @throws PersistenceException if save fails or ID cannot be generated
     */
    public <T> T save(T entity) {
        return save(entity, PostgreSqlDialect.INSTANCE);
    }

    /**
     * Save an entity with automatic ID generation using specified dialect.
     *
     * @param entity the entity to save
     * @param dialect the SQL dialect to use
     * @param <T> entity type
     * @return the saved entity with ID set
     * @throws PersistenceException if save fails or ID cannot be generated
     */
    public <T> T save(T entity, SqlDialect dialect) {
        return EntityPersistence.save(entity, connection, dialect);
    }

    /**
     * Save multiple entities in efficient batch INSERT operations.
     * IDs are generated and set on all entities.
     *
     * <pre>{@code
     * executor.transaction(tx -> {
     *     List<User> users = List.of(user1, user2, user3);
     *     List<User> saved = tx.saveAll(users);
     *     // All entities now have IDs set
     * });
     * }</pre>
     *
     * @param entities list of entities to save
     * @param <T>      entity type
     * @return the saved entities with IDs set
     * @throws PersistenceException if batch save fails
     */
    public <T> List<T> saveAll(List<T> entities) {
        return saveAll(entities, PostgreSqlDialect.INSTANCE);
    }

    /**
     * Save multiple entities with specified dialect.
     *
     * @param entities list of entities to save
     * @param dialect  the SQL dialect to use
     * @param <T>      entity type
     * @return the saved entities with IDs set
     */
    public <T> List<T> saveAll(List<T> entities, SqlDialect dialect) {
        if (Objects.isNull(entities) || entities.isEmpty()) {
            return new ArrayList<>();
        }
        return BatchPersistence.saveAll(entities, connection, dialect);
    }

    /**
     * Save multiple entities with custom batch size.
     *
     * @param entities  list of entities to save
     * @param batchSize maximum entities per batch (1-1000)
     * @param dialect   the SQL dialect to use
     * @param <T>       entity type
     * @return the saved entities with IDs set
     */
    public <T> List<T> saveAll(List<T> entities, int batchSize, SqlDialect dialect) {
        if (Objects.isNull(entities) || entities.isEmpty()) {
            return new ArrayList<>();
        }
        return BatchPersistence.saveAll(entities, connection, dialect, batchSize);
    }

    // ==================== UPSERT ====================

    /**
     * Upsert a single entity (INSERT ... ON CONFLICT DO UPDATE).
     *
     * <pre>{@code
     * executor.transaction(tx -> {
     *     User user = new User();
     *     user.setEmail("test@example.com");
     *     User saved = tx.upsert(user, new String[]{"email"});
     * });
     * }</pre>
     *
     * @param entity          the entity to upsert
     * @param conflictColumns columns that define the conflict
     * @param <T>             entity type
     * @return the upserted entity
     */
    public <T> T upsert(T entity, String[] conflictColumns) {
        return upsert(entity, conflictColumns, null, PostgreSqlDialect.INSTANCE);
    }

    /**
     * Upsert a single entity with specific columns to update.
     *
     * @param entity          the entity to upsert
     * @param conflictColumns columns that define the conflict
     * @param updateColumns   columns to update on conflict (null = all non-conflict)
     * @param dialect         the SQL dialect
     * @param <T>             entity type
     * @return the upserted entity
     */
    public <T> T upsert(T entity, String[] conflictColumns, String[] updateColumns, SqlDialect dialect) {
        return UpsertPersistence.upsert(entity, connection, dialect, conflictColumns, updateColumns);
    }

    /**
     * Upsert multiple entities in batch.
     *
     * @param entities        list of entities to upsert
     * @param conflictColumns columns that define the conflict
     * @param <T>             entity type
     * @return the upserted entities
     */
    public <T> List<T> upsertAll(List<T> entities, String[] conflictColumns) {
        return upsertAll(entities, conflictColumns, null, PostgreSqlDialect.INSTANCE);
    }

    /**
     * Upsert multiple entities with specific columns to update.
     *
     * @param entities        list of entities to upsert
     * @param conflictColumns columns that define the conflict
     * @param updateColumns   columns to update on conflict (null = all non-conflict)
     * @param dialect         the SQL dialect
     * @param <T>             entity type
     * @return the upserted entities
     */
    public <T> List<T> upsertAll(List<T> entities, String[] conflictColumns, String[] updateColumns, SqlDialect dialect) {
        if (Objects.isNull(entities) || entities.isEmpty()) {
            return new ArrayList<>();
        }
        return UpsertPersistence.upsertAll(entities, connection, dialect, conflictColumns, updateColumns);
    }

    private void setParameters(PreparedStatement ps, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            if (param instanceof Enum<?> enumValue) {
                ps.setString(i + 1, enumValue.name());
            } else {
                ps.setObject(i + 1, param);
            }
        }
    }

    private String getSavepointName(Savepoint savepoint) {
        try {
            return savepoint.getSavepointName();
        } catch (SQLException e) {
            return "unknown";
        }
    }
}
