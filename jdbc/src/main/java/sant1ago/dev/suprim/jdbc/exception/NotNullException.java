package sant1ago.dev.suprim.jdbc.exception;

import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exception thrown when a NOT NULL constraint is violated.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>Inserting NULL into a required column</li>
 *   <li>Updating a required column to NULL</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     executor.execute(insertUserQuery);
 * } catch (NotNullException e) {
 *     throw new ValidationException("Field '" + e.getColumnName() + "' is required");
 * }
 * }</pre>
 */
public class NotNullException extends ConstraintViolationException {

    protected NotNullException(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static NotNullException fromSQLException(String sql, SQLException cause) {
        String message = cause.getMessage();

        return builder()
                .message("NOT NULL constraint violation")
                .sql(sql)
                .cause(cause)
                .constraintName(extractConstraintName(message))
                .tableName(extractTableNameFromNotNull(message))
                .columnName(extractColumnNameFromNotNull(message))
                .build();
    }

    private static String extractTableNameFromNotNull(String message) {
        if (Objects.isNull(message)) {
            return null;
        }

        // PostgreSQL: "null value in column \"email\" of relation \"users\""
        Pattern pattern = Pattern.compile("relation [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return extractTableName(message);
    }

    private static String extractColumnNameFromNotNull(String message) {
        if (Objects.isNull(message)) {
            return null;
        }

        // PostgreSQL: "null value in column \"email\""
        // H2: "NULL not allowed for column \"EMAIL\""
        // MySQL: "Column 'email' cannot be null"
        Pattern[] patterns = {
                Pattern.compile("column [\"'`]([^\"'`]+)[\"'`]", Pattern.CASE_INSENSITIVE),
                Pattern.compile("Column '([^']+)'", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return extractColumnName(message);
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
        public NotNullException build() {
            this.category(ErrorCategory.INTEGRITY_CONSTRAINT);
            this.constraintType(ConstraintType.NOT_NULL);
            return new NotNullException(this);
        }
    }
}
