package sant1ago.dev.suprim.jdbc.exception;

import sant1ago.dev.suprim.jdbc.SuprimException;

import java.util.Objects;

/**
 * Exception thrown when entity mapping fails.
 *
 * <p>Common causes:
 * <ul>
 *   <li>No suitable constructor found</li>
 *   <li>Field type mismatch</li>
 *   <li>Missing required column in ResultSet</li>
 *   <li>Reflection access errors</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     List<User> users = executor.query(selectQuery, EntityMapper.of(User.class));
 * } catch (MappingException e) {
 *     log.error("Failed to map to {}: field={}, type={}",
 *         e.getTargetClass(), e.getFieldName(), e.getMappingErrorType());
 * }
 * }</pre>
 */
public class MappingException extends SuprimException {

    private final Class<?> targetClass;
    private final String fieldName;
    private final String columnName;
    private final MappingErrorType errorType;

    protected MappingException(Builder builder) {
        super(builder);
        this.targetClass = builder.targetClass;
        this.fieldName = builder.fieldName;
        this.columnName = builder.columnName;
        this.errorType = builder.errorType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MappingException noConstructor(Class<?> targetClass) {
        return builder()
                .message("No suitable constructor found")
                .targetClass(targetClass)
                .errorType(MappingErrorType.NO_CONSTRUCTOR)
                .build();
    }

    public static MappingException noNoArgConstructor(Class<?> targetClass) {
        return builder()
                .message("Class must have a no-arg constructor")
                .targetClass(targetClass)
                .errorType(MappingErrorType.NO_NO_ARG_CONSTRUCTOR)
                .build();
    }

    public static MappingException cannotCreateInstance(Class<?> targetClass, Throwable cause) {
        return builder()
                .message("Failed to create instance")
                .targetClass(targetClass)
                .errorType(MappingErrorType.INSTANTIATION_FAILED)
                .cause(cause)
                .build();
    }

    public static MappingException fieldAccessError(Class<?> targetClass, String fieldName, Throwable cause) {
        return builder()
                .message("Failed to access field")
                .targetClass(targetClass)
                .fieldName(fieldName)
                .errorType(MappingErrorType.FIELD_ACCESS_ERROR)
                .cause(cause)
                .build();
    }

    public static MappingException typeMismatch(Class<?> targetClass, String fieldName,
                                                 Class<?> expectedType, Class<?> actualType) {
        return builder()
                .message("Type mismatch: expected " + expectedType.getSimpleName()
                        + " but got " + actualType.getSimpleName())
                .targetClass(targetClass)
                .fieldName(fieldName)
                .errorType(MappingErrorType.TYPE_MISMATCH)
                .build();
    }

    public static MappingException columnNotFound(Class<?> targetClass, String columnName) {
        return builder()
                .message("Column not found in ResultSet")
                .targetClass(targetClass)
                .columnName(columnName)
                .errorType(MappingErrorType.COLUMN_NOT_FOUND)
                .build();
    }

    public static MappingException conversionFailed(Class<?> targetClass, String columnName,
                                                     Class<?> targetType, Throwable cause) {
        return builder()
                .message("Failed to convert value to " + targetType.getSimpleName())
                .targetClass(targetClass)
                .columnName(columnName)
                .errorType(MappingErrorType.CONVERSION_FAILED)
                .cause(cause)
                .build();
    }

    public static MappingException recordConstructorFailed(Class<?> targetClass, Throwable cause) {
        return builder()
                .message("Failed to invoke record constructor")
                .targetClass(targetClass)
                .errorType(MappingErrorType.RECORD_CONSTRUCTOR_FAILED)
                .cause(cause)
                .build();
    }

    /**
     * Get the target class that mapping was attempted for.
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }

    /**
     * Get the field name involved in the mapping error.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Get the column name involved in the mapping error.
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Get the type of mapping error.
     */
    public MappingErrorType getMappingErrorType() {
        return errorType;
    }

    /**
     * Types of mapping errors.
     */
    public enum MappingErrorType {
        /** No suitable constructor found */
        NO_CONSTRUCTOR,
        /** Class needs no-arg constructor */
        NO_NO_ARG_CONSTRUCTOR,
        /** Failed to create instance */
        INSTANTIATION_FAILED,
        /** Failed to access field */
        FIELD_ACCESS_ERROR,
        /** Type mismatch between ResultSet and field */
        TYPE_MISMATCH,
        /** Required column not found in ResultSet */
        COLUMN_NOT_FOUND,
        /** Failed to convert ResultSet value to field type */
        CONVERSION_FAILED,
        /** Record canonical constructor invocation failed */
        RECORD_CONSTRUCTOR_FAILED,
        /** Unknown mapping error */
        UNKNOWN
    }

    public static class Builder extends SuprimException.Builder {
        private Class<?> targetClass;
        private String fieldName;
        private String columnName;
        private MappingErrorType errorType = MappingErrorType.UNKNOWN;

        @Override
        public Builder message(String message) {
            super.message(message);
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            super.cause(cause);
            return this;
        }

        public Builder targetClass(Class<?> targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public Builder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public Builder errorType(MappingErrorType errorType) {
            this.errorType = errorType;
            return this;
        }

        @Override
        protected String buildMessage() {
            StringBuilder sb = new StringBuilder();
            if (Objects.nonNull(message)) {
                sb.append(message);
            }
            if (Objects.nonNull(targetClass)) {
                sb.append(" [class: ").append(targetClass.getSimpleName()).append("]");
            }
            if (Objects.nonNull(fieldName)) {
                sb.append(" [field: ").append(fieldName).append("]");
            }
            if (Objects.nonNull(columnName)) {
                sb.append(" [column: ").append(columnName).append("]");
            }
            return sb.toString();
        }

        @Override
        public MappingException build() {
            return new MappingException(this);
        }
    }
}
