package sant1ago.dev.suprim.annotation.entity;

/**
 * Defines the action to take when setting timestamp fields.
 *
 * <p>Used with {@link CreationTimestamp} and {@link UpdateTimestamp} to control
 * when and how timestamp values are automatically set.
 *
 * @see CreationTimestamp
 * @see UpdateTimestamp
 */
public enum TimestampAction {

    /**
     * Always set the field to the current timestamp.
     *
     * <p>This overwrites any existing value, ensuring the timestamp
     * always reflects the actual operation time.
     */
    NOW,

    /**
     * Set the field to the current timestamp only if it is null.
     *
     * <p>This preserves user-set values while providing a default
     * for fields that weren't explicitly set.
     */
    IF_NULL,

    /**
     * Never automatically set the field.
     *
     * <p>The field must be set manually by the application.
     * Useful when you want to control timestamps explicitly.
     */
    NEVER
}
