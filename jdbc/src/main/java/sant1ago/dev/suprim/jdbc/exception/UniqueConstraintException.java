package sant1ago.dev.suprim.jdbc.exception;

import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exception thrown when a unique or primary key constraint is violated.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>Inserting a duplicate primary key</li>
 *   <li>Inserting a duplicate value in a unique column</li>
 *   <li>Updating to a value that already exists</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     executor.execute(insertUserQuery);
 * } catch (UniqueConstraintException e) {
 *     if ("email".equals(e.getColumnName())) {
 *         throw new EmailAlreadyExistsException(email);
 *     }
 *     throw e;
 * }
 * }</pre>
 */
public class UniqueConstraintException extends ConstraintViolationException {

    private final Object duplicateValue;

    protected UniqueConstraintException(Builder builder) {
        super(builder);
        this.duplicateValue = builder.duplicateValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static UniqueConstraintException fromSQLException(String sql, SQLException cause) {
        String message = cause.getMessage();

        return builder()
                .message("Unique constraint violation")
                .sql(sql)
                .cause(cause)
                .constraintType(ConstraintType.UNIQUE)
                .constraintName(extractConstraintName(message))
                .tableName(extractTableName(message))
                .columnName(extractColumnName(message))
                .duplicateValue(extractDuplicateValue(message))
                .build();
    }

    /**
     * Extract the duplicate value from error message.
     */
    private static Object extractDuplicateValue(String message) {
        if (Objects.isNull(message)) {
            return null;
        }

        // MySQL: "Duplicate entry 'value' for key..."
        Pattern mysqlPattern = Pattern.compile("Duplicate entry '([^']+)'", Pattern.CASE_INSENSITIVE);
        Matcher matcher = mysqlPattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // PostgreSQL: "Key (email)=(test@example.com) already exists"
        Pattern pgPattern = Pattern.compile("\\)=\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        matcher = pgPattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Get the duplicate value that caused the violation, if available.
     */
    public Object getDuplicateValue() {
        return duplicateValue;
    }

    public static class Builder extends ConstraintViolationException.Builder {
        private Object duplicateValue;

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
        public Builder constraintType(ConstraintType constraintType) {
            super.constraintType(constraintType);
            return this;
        }

        public Builder duplicateValue(Object duplicateValue) {
            this.duplicateValue = duplicateValue;
            return this;
        }

        @Override
        public UniqueConstraintException build() {
            this.category(ErrorCategory.INTEGRITY_CONSTRAINT);
            this.constraintType(ConstraintType.UNIQUE);
            return new UniqueConstraintException(this);
        }
    }
}
