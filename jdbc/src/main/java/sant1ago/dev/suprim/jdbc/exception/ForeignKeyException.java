package sant1ago.dev.suprim.jdbc.exception;

import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exception thrown when a foreign key constraint is violated.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>Inserting a reference to non-existent parent record</li>
 *   <li>Deleting a parent record with existing child references</li>
 *   <li>Updating a foreign key to a non-existent value</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     executor.execute(insertOrderQuery);
 * } catch (ForeignKeyException e) {
 *     if (e.getReferencedTable().contains("user")) {
 *         throw new UserNotFoundException(userId);
 *     }
 *     throw e;
 * }
 * }</pre>
 */
public class ForeignKeyException extends ConstraintViolationException {

    private final String referencedTable;
    private final String referencedColumn;
    private final ForeignKeyViolationType violationType;

    protected ForeignKeyException(Builder builder) {
        super(builder);
        this.referencedTable = builder.referencedTable;
        this.referencedColumn = builder.referencedColumn;
        this.violationType = builder.violationType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ForeignKeyException fromSQLException(String sql, SQLException cause) {
        String message = cause.getMessage();
        ForeignKeyViolationType violationType = detectViolationType(message, sql);

        return builder()
                .message(violationType == ForeignKeyViolationType.PARENT_NOT_FOUND
                        ? "Referenced record does not exist"
                        : "Cannot delete/update - child records exist")
                .sql(sql)
                .cause(cause)
                .constraintName(extractConstraintName(message))
                .tableName(extractTableName(message))
                .columnName(extractColumnName(message))
                .referencedTable(extractReferencedTable(message))
                .referencedColumn(extractReferencedColumn(message))
                .violationType(violationType)
                .build();
    }

    private static ForeignKeyViolationType detectViolationType(String message, String sql) {
        if (Objects.isNull(message)) {
            return ForeignKeyViolationType.UNKNOWN;
        }

        String lowerMessage = message.toLowerCase();
        String lowerSql = Objects.nonNull(sql) ? sql.toLowerCase() : "";

        // Check if it's a DELETE or UPDATE on parent causing the issue
        if (lowerMessage.contains("still referenced") || lowerMessage.contains("child record found")
                || lowerMessage.contains("restrict")) {
            return ForeignKeyViolationType.CHILD_EXISTS;
        }

        // Check if it's an INSERT/UPDATE trying to reference non-existent parent
        if (lowerMessage.contains("not present") || lowerMessage.contains("does not exist")
                || lowerMessage.contains("no parent")) {
            return ForeignKeyViolationType.PARENT_NOT_FOUND;
        }

        // Infer from SQL statement
        if (lowerSql.startsWith("delete") || lowerSql.startsWith("update")) {
            return ForeignKeyViolationType.CHILD_EXISTS;
        }
        if (lowerSql.startsWith("insert")) {
            return ForeignKeyViolationType.PARENT_NOT_FOUND;
        }

        return ForeignKeyViolationType.UNKNOWN;
    }

    private static String extractReferencedTable(String message) {
        if (Objects.isNull(message)) {
            return null;
        }

        // PostgreSQL: "is not present in table \"users\""
        // MySQL: "REFERENCES `users`"
        Pattern[] patterns = {
                Pattern.compile("in table [\"'`]([^\"'`]+)[\"'`]", Pattern.CASE_INSENSITIVE),
                Pattern.compile("references [\"'`]([^\"'`]+)[\"'`]", Pattern.CASE_INSENSITIVE),
                Pattern.compile("referenced table [\"'`]([^\"'`]+)[\"'`]", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    private static String extractReferencedColumn(String message) {
        if (Objects.isNull(message)) {
            return null;
        }

        // PostgreSQL: "Key (user_id)=(123) is not present in table..."
        Pattern pattern = Pattern.compile("Key \\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Get the referenced (parent) table name.
     */
    public String getReferencedTable() {
        return referencedTable;
    }

    /**
     * Get the referenced (parent) column name.
     */
    public String getReferencedColumn() {
        return referencedColumn;
    }

    /**
     * Get the type of foreign key violation.
     */
    public ForeignKeyViolationType getViolationType() {
        return violationType;
    }

    /**
     * Check if violation is due to missing parent record.
     */
    public boolean isParentNotFound() {
        return violationType == ForeignKeyViolationType.PARENT_NOT_FOUND;
    }

    /**
     * Check if violation is due to existing child records.
     */
    public boolean hasChildRecords() {
        return violationType == ForeignKeyViolationType.CHILD_EXISTS;
    }

    /**
     * Types of foreign key violations.
     */
    public enum ForeignKeyViolationType {
        /** Trying to reference a parent that doesn't exist */
        PARENT_NOT_FOUND,
        /** Trying to delete/update a parent with existing children */
        CHILD_EXISTS,
        /** Unknown violation type */
        UNKNOWN
    }

    public static class Builder extends ConstraintViolationException.Builder {
        private String referencedTable;
        private String referencedColumn;
        private ForeignKeyViolationType violationType = ForeignKeyViolationType.UNKNOWN;

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

        public Builder referencedTable(String referencedTable) {
            this.referencedTable = referencedTable;
            return this;
        }

        public Builder referencedColumn(String referencedColumn) {
            this.referencedColumn = referencedColumn;
            return this;
        }

        public Builder violationType(ForeignKeyViolationType violationType) {
            this.violationType = violationType;
            return this;
        }

        @Override
        public ForeignKeyException build() {
            this.category(ErrorCategory.INTEGRITY_CONSTRAINT);
            this.constraintType(ConstraintType.FOREIGN_KEY);
            return new ForeignKeyException(this);
        }
    }
}
