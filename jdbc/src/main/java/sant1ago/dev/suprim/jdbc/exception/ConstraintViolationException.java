package sant1ago.dev.suprim.jdbc.exception;

import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exception thrown when a database constraint is violated.
 *
 * <p>This is the base class for specific constraint violations:
 * <ul>
 *   <li>{@link UniqueConstraintException} - Unique/primary key violations</li>
 *   <li>{@link ForeignKeyException} - Foreign key violations</li>
 *   <li>{@link CheckConstraintException} - Check constraint violations</li>
 *   <li>{@link NotNullException} - NOT NULL constraint violations</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     executor.execute(insertQuery);
 * } catch (UniqueConstraintException e) {
 *     log.warn("Duplicate: constraint={}, column={}", e.getConstraintName(), e.getColumnName());
 * } catch (ForeignKeyException e) {
 *     log.warn("Invalid reference: constraint={}", e.getConstraintName());
 * } catch (ConstraintViolationException e) {
 *     log.warn("Constraint violated: {}", e.getConstraintName());
 * }
 * }</pre>
 */
public class ConstraintViolationException extends ExecutionException {

    private final String constraintName;
    private final String tableName;
    private final String columnName;
    private final ConstraintType constraintType;

    protected ConstraintViolationException(Builder builder) {
        super(builder);
        this.constraintName = builder.constraintName;
        this.tableName = builder.tableName;
        this.columnName = builder.columnName;
        this.constraintType = builder.constraintType;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create appropriate constraint exception from SQLException.
     * Analyzes SQL state and error message to determine constraint type.
     */
    public static ConstraintViolationException fromSQLException(String sql, SQLException cause) {
        String sqlState = cause.getSQLState();
        String message = cause.getMessage();

        // SQL State 23xxx is integrity constraint violation
        if (Objects.nonNull(sqlState) && sqlState.startsWith("23")) {
            return switch (sqlState) {
                case "23505", "23001" -> UniqueConstraintException.fromSQLException(sql, cause);
                case "23503" -> ForeignKeyException.fromSQLException(sql, cause);
                case "23514", "23513" -> CheckConstraintException.fromSQLException(sql, cause);
                case "23502" -> NotNullException.fromSQLException(sql, cause);
                default -> parseFromMessage(sql, message, cause);
            };
        }

        return parseFromMessage(sql, message, cause);
    }

    private static ConstraintViolationException parseFromMessage(String sql, String message, SQLException cause) {
        String lowerMessage = message.toLowerCase();

        // Unique constraint patterns - check each separately
        if (lowerMessage.contains("unique")) {
            return UniqueConstraintException.fromSQLException(sql, cause);
        }
        if (lowerMessage.contains("duplicate")) {
            return UniqueConstraintException.fromSQLException(sql, cause);
        }
        if (lowerMessage.contains("primary key")) {
            return UniqueConstraintException.fromSQLException(sql, cause);
        }

        // Foreign key patterns
        if (lowerMessage.contains("foreign key")) {
            return ForeignKeyException.fromSQLException(sql, cause);
        }
        if (lowerMessage.contains("referential")) {
            return ForeignKeyException.fromSQLException(sql, cause);
        }

        // Check constraint
        if (lowerMessage.contains("check constraint")) {
            return CheckConstraintException.fromSQLException(sql, cause);
        }

        // Not null patterns
        if (lowerMessage.contains("not null")) {
            return NotNullException.fromSQLException(sql, cause);
        }
        if (lowerMessage.contains("cannot be null")) {
            return NotNullException.fromSQLException(sql, cause);
        }

        // Generic constraint violation
        return builder()
                .message("Constraint violation")
                .sql(sql)
                .cause(cause)
                .constraintType(ConstraintType.UNKNOWN)
                .constraintName(extractConstraintName(message))
                .build();
    }

    /**
     * Extract constraint name from error message.
     * Different databases format this differently.
     */
    protected static String extractConstraintName(String message) {
        if (Objects.isNull(message)) {
            return null;
        }

        // PostgreSQL: "duplicate key value violates unique constraint \"users_email_key\""
        // H2: "Unique index or primary key violation: \"PUBLIC.USERS_EMAIL_KEY\""
        // MySQL: "Duplicate entry '...' for key 'users.email'"

        // Try quoted constraint name patterns
        Pattern[] patterns = {
                Pattern.compile("constraint [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE),
                Pattern.compile("violates [a-z]+ constraint [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE),
                Pattern.compile("for key [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE),
                Pattern.compile("index or primary key violation: [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    /**
     * Extract table name from error message.
     */
    protected static String extractTableName(String message) {
        if (Objects.isNull(message)) {
            return null;
        }

        // Try common patterns
        Pattern[] patterns = {
                Pattern.compile("table [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE),
                Pattern.compile("on [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE),
                Pattern.compile("into [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    /**
     * Extract column name from error message.
     */
    protected static String extractColumnName(String message) {
        if (Objects.isNull(message)) {
            return null;
        }

        // Try common patterns
        Pattern[] patterns = {
                Pattern.compile("column [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE),
                Pattern.compile("field [\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\(([^)]+)\\)=", Pattern.CASE_INSENSITIVE) // PostgreSQL: (email)=value
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    /**
     * Get the constraint name that was violated.
     */
    public String getConstraintName() {
        return constraintName;
    }

    /**
     * Get the table name involved in the violation.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Get the column name involved in the violation.
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Get the type of constraint that was violated.
     */
    public ConstraintType getConstraintType() {
        return constraintType;
    }

    /**
     * Types of database constraints.
     */
    public enum ConstraintType {
        /** Primary key or unique constraint */
        UNIQUE,
        /** Foreign key constraint */
        FOREIGN_KEY,
        /** Check constraint */
        CHECK,
        /** NOT NULL constraint */
        NOT_NULL,
        /** Unknown constraint type */
        UNKNOWN
    }

    public static class Builder extends ExecutionException.Builder {
        private String constraintName;
        private String tableName;
        private String columnName;
        private ConstraintType constraintType = ConstraintType.UNKNOWN;

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
        public Builder parameters(Object[] parameters) {
            super.parameters(parameters);
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            super.cause(cause);
            return this;
        }

        public Builder constraintName(String constraintName) {
            this.constraintName = constraintName;
            return this;
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public Builder constraintType(ConstraintType constraintType) {
            this.constraintType = constraintType;
            return this;
        }

        @Override
        public ConstraintViolationException build() {
            this.category(ErrorCategory.INTEGRITY_CONSTRAINT);
            return new ConstraintViolationException(this);
        }
    }
}
