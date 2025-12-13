package sant1ago.dev.suprim.jdbc.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe event dispatcher with copy-on-write listener lists.
 *
 * <p>Designed for zero overhead when no listeners are registered.
 * All dispatch methods check for empty lists before iteration.</p>
 *
 * <p>Exceptions in listeners are caught and logged, never propagated
 * to avoid interrupting query execution.</p>
 */
public final class EventDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(EventDispatcher.class);

    private final List<QueryListener> queryListeners = new CopyOnWriteArrayList<>();
    private final List<TransactionListener> transactionListeners = new CopyOnWriteArrayList<>();

    // ============ Query Listener Registration ============

    /**
     * Add a query listener.
     *
     * @param listener the listener to add (null ignored)
     */
    public void addQueryListener(QueryListener listener) {
        if (Objects.nonNull(listener)) {
            queryListeners.add(listener);
        }
    }

    /**
     * Remove a query listener.
     *
     * @param listener the listener to remove
     * @return true if listener was found and removed
     */
    public boolean removeQueryListener(QueryListener listener) {
        return queryListeners.remove(listener);
    }

    // ============ Transaction Listener Registration ============

    /**
     * Add a transaction listener.
     *
     * @param listener the listener to add (null ignored)
     */
    public void addTransactionListener(TransactionListener listener) {
        if (Objects.nonNull(listener)) {
            transactionListeners.add(listener);
        }
    }

    /**
     * Remove a transaction listener.
     *
     * @param listener the listener to remove
     * @return true if listener was found and removed
     */
    public boolean removeTransactionListener(TransactionListener listener) {
        return transactionListeners.remove(listener);
    }

    // ============ Query Event Dispatch ============

    /**
     * Fire beforeQuery event to all listeners.
     *
     * @param event the query event
     */
    public void fireBeforeQuery(QueryEvent event) {
        if (queryListeners.isEmpty()) return;
        for (QueryListener listener : queryListeners) {
            try {
                listener.beforeQuery(event);
            } catch (Exception e) {
                LOG.warn("QueryListener.beforeQuery threw exception", e);
            }
        }
    }

    /**
     * Fire afterQuery event to all listeners.
     *
     * @param event the query event with timing info
     */
    public void fireAfterQuery(QueryEvent event) {
        if (queryListeners.isEmpty()) return;
        for (QueryListener listener : queryListeners) {
            try {
                listener.afterQuery(event);
            } catch (Exception e) {
                LOG.warn("QueryListener.afterQuery threw exception", e);
            }
        }
    }

    /**
     * Fire onError event to all listeners.
     *
     * @param event the query event with error info
     */
    public void fireQueryError(QueryEvent event) {
        if (queryListeners.isEmpty()) return;
        for (QueryListener listener : queryListeners) {
            try {
                listener.onError(event);
            } catch (Exception e) {
                LOG.warn("QueryListener.onError threw exception", e);
            }
        }
    }

    // ============ Transaction Event Dispatch ============

    /**
     * Fire transaction event to all listeners.
     *
     * @param event the transaction event
     */
    public void fireTransactionEvent(TransactionEvent event) {
        if (transactionListeners.isEmpty()) return;
        for (TransactionListener listener : transactionListeners) {
            try {
                switch (event.type()) {
                    case BEGIN -> listener.onBegin(event);
                    case COMMIT -> listener.onCommit(event);
                    case ROLLBACK -> listener.onRollback(event);
                    case SAVEPOINT_CREATED -> listener.onSavepointCreated(event);
                    case SAVEPOINT_RELEASED -> listener.onSavepointReleased(event);
                    case SAVEPOINT_ROLLBACK -> listener.onSavepointRollback(event);
                }
            } catch (Exception e) {
                LOG.warn("TransactionListener threw exception for {}", event.type(), e);
            }
        }
    }

    // ============ Utility ============

    /**
     * Check if any query listeners are registered.
     *
     * @return true if at least one listener exists
     */
    public boolean hasQueryListeners() {
        return !queryListeners.isEmpty();
    }

    /**
     * Check if any transaction listeners are registered.
     *
     * @return true if at least one listener exists
     */
    public boolean hasTransactionListeners() {
        return !transactionListeners.isEmpty();
    }

    /**
     * Remove all listeners.
     */
    public void clearAll() {
        queryListeners.clear();
        transactionListeners.clear();
    }

    /**
     * Get count of query listeners.
     *
     * @return number of registered query listeners
     */
    public int queryListenerCount() {
        return queryListeners.size();
    }

    /**
     * Get count of transaction listeners.
     *
     * @return number of registered transaction listeners
     */
    public int transactionListenerCount() {
        return transactionListeners.size();
    }
}
