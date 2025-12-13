package sant1ago.dev.suprim.jdbc.exception;

import sant1ago.dev.suprim.jdbc.SuprimException;

/**
 * Exception thrown when entity persistence operations fail.
 * This includes ID generation failures, missing required fields, and save errors.
 *
 * <pre>{@code
 * try {
 *     tx.save(entity);
 * } catch (PersistenceException e) {
 *     log.error("Failed to save {}: {}", e.getEntityClass().getSimpleName(), e.getMessage());
 * }
 * }</pre>
 */
public class PersistenceException extends SuprimException {

    private final Class<?> entityClass;

    /**
     * Create a persistence exception with entity class.
     *
     * @param message the error message
     * @param entityClass the entity class involved
     */
    public PersistenceException(String message, Class<?> entityClass) {
        super(message);
        this.entityClass = entityClass;
    }

    /**
     * Create a persistence exception with entity class and cause.
     *
     * @param message the error message
     * @param entityClass the entity class involved
     * @param cause the underlying cause
     */
    public PersistenceException(String message, Class<?> entityClass, Throwable cause) {
        super(message, cause);
        this.entityClass = entityClass;
    }

    /**
     * Get the entity class involved in the failed operation.
     *
     * @return the entity class, or null if not applicable
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }
}
