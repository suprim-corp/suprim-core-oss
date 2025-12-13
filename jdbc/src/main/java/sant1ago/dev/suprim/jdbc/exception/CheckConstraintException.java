package sant1ago.dev.suprim.jdbc.exception;

import java.sql.SQLException;

/**
 * Exception thrown when a CHECK constraint is violated.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>Value outside allowed range (e.g., age < 0)</li>
 *   <li>Value doesn't match pattern (e.g., invalid email format)</li>
 *   <li>Value fails custom validation rule</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     executor.execute(insertProductQuery);
 * } catch (CheckConstraintException e) {
 *     log.warn("Validation failed: constraint={}", e.getConstraintName());
 *     throw new ValidationException("Invalid product data");
 * }
 * }</pre>
 */
public class CheckConstraintException extends ConstraintViolationException {

    protected CheckConstraintException(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CheckConstraintException fromSQLException(String sql, SQLException cause) {
        String message = cause.getMessage();

        return builder()
                .message("Check constraint violation")
                .sql(sql)
                .cause(cause)
                .constraintName(extractConstraintName(message))
                .tableName(extractTableName(message))
                .columnName(extractColumnName(message))
                .build();
    }

    public static class Builder extends ConstraintViolationException.Builder {
        @Override
        public Builder message(String message) {
            super.message(message);
            return this;
        }

        @Override
        public Builder sql(String sql) {
            super.sql(sql);
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            super.cause(cause);
            return this;
        }

        @Override
        public Builder constraintName(String constraintName) {
            super.constraintName(constraintName);
            return this;
        }

        @Override
        public Builder tableName(String tableName) {
            super.tableName(tableName);
            return this;
        }

        @Override
        public Builder columnName(String columnName) {
            super.columnName(columnName);
            return this;
        }

        @Override
        public CheckConstraintException build() {
            this.category(ErrorCategory.INTEGRITY_CONSTRAINT);
            this.constraintType(ConstraintType.CHECK);
            return new CheckConstraintException(this);
        }
    }
}
