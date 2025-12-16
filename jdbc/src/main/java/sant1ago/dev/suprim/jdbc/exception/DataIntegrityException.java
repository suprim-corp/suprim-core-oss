package sant1ago.dev.suprim.jdbc.exception;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Exception thrown when data format or type issues occur.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>Invalid date/time format</li>
 *   <li>Numeric overflow/underflow</li>
 *   <li>String too long for column</li>
 *   <li>Invalid JSON format</li>
 *   <li>Invalid enum value</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     executor.execute(insertQuery);
 * } catch (DataIntegrityException e) {
 *     log.warn("Data issue: type={}", e.getDataErrorType());
 *     throw new ValidationException(e.getMessage());
 * }
 * }</pre>
 */
public class DataIntegrityException extends ExecutionException {

    private final DataErrorType dataErrorType;

    protected DataIntegrityException(Builder builder) {
        super(builder);
        this.dataErrorType = builder.dataErrorType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DataIntegrityException fromSQLException(String sql, SQLException cause) {
        String message = cause.getMessage();
        DataErrorType errorType = detectErrorType(message, cause.getSQLState());

        return builder()
                .message("Data integrity error: " + errorType.getDescription())
                .sql(sql)
                .cause(cause)
                .dataErrorType(errorType)
                .build();
    }

    private static DataErrorType detectErrorType(String message, String sqlState) {
        if (Objects.isNull(message)) {
            return DataErrorType.UNKNOWN;
        }

        String lowerMessage = message.toLowerCase();

        // Check SQL state first
        if (Objects.nonNull(sqlState)) {
            return switch (sqlState) {
                case "22001" -> DataErrorType.STRING_TOO_LONG;
                case "22003" -> DataErrorType.NUMERIC_OVERFLOW;
                case "22007" -> DataErrorType.INVALID_DATETIME;
                case "22008" -> DataErrorType.DATETIME_OVERFLOW;
                case "22012" -> DataErrorType.DIVISION_BY_ZERO;
                case "22018" -> DataErrorType.INVALID_CHARACTER;
                case "22019" -> DataErrorType.INVALID_ESCAPE;
                case "22021" -> DataErrorType.CHARACTER_NOT_IN_REPERTOIRE;
                case "22023" -> DataErrorType.INVALID_PARAMETER;
                case "22P02" -> DataErrorType.INVALID_TEXT_REPRESENTATION;
                case "22P03" -> DataErrorType.INVALID_BINARY_REPRESENTATION;
                default -> detectFromMessage(lowerMessage);
            };
        }

        return detectFromMessage(lowerMessage);
    }

    private static DataErrorType detectFromMessage(String lowerMessage) {
        // String length issues
        if (lowerMessage.contains("too long")) {
            return DataErrorType.STRING_TOO_LONG;
        }
        // Numeric overflow
        if (lowerMessage.contains("overflow") || lowerMessage.contains("out of range")) {
            return DataErrorType.NUMERIC_OVERFLOW;
        }
        // Date/time issues - check specific terms
        if (lowerMessage.contains("timestamp")) {
            return DataErrorType.INVALID_DATETIME;
        }
        if (lowerMessage.contains("datetime")) {
            return DataErrorType.INVALID_DATETIME;
        }
        if (lowerMessage.contains("date") || lowerMessage.contains("time")) {
            return DataErrorType.INVALID_DATETIME;
        }
        // JSON issues - "json" matches both "json" and "jsonb"
        if (lowerMessage.contains("json")) {
            return DataErrorType.INVALID_JSON;
        }
        // Enum issues
        if (lowerMessage.contains("enum") || lowerMessage.contains("invalid input value")) {
            return DataErrorType.INVALID_ENUM;
        }
        // Division by zero
        if (lowerMessage.contains("division by zero")) {
            return DataErrorType.DIVISION_BY_ZERO;
        }
        // Invalid syntax
        if (lowerMessage.contains("invalid input syntax")) {
            return DataErrorType.INVALID_TEXT_REPRESENTATION;
        }

        return DataErrorType.UNKNOWN;
    }

    /**
     * Get the type of data error.
     */
    public DataErrorType getDataErrorType() {
        return dataErrorType;
    }

    /**
     * Types of data errors.
     */
    public enum DataErrorType {
        /** String value exceeds column length */
        STRING_TOO_LONG("String value exceeds maximum length"),
        /** Numeric value out of range */
        NUMERIC_OVERFLOW("Numeric value out of range"),
        /** Invalid date/time format */
        INVALID_DATETIME("Invalid date/time format"),
        /** Date/time value out of range */
        DATETIME_OVERFLOW("Date/time value out of range"),
        /** Division by zero */
        DIVISION_BY_ZERO("Division by zero"),
        /** Invalid character for encoding */
        INVALID_CHARACTER("Invalid character for encoding"),
        /** Invalid escape sequence */
        INVALID_ESCAPE("Invalid escape sequence"),
        /** Character not in character set */
        CHARACTER_NOT_IN_REPERTOIRE("Character not in repertoire"),
        /** Invalid parameter value */
        INVALID_PARAMETER("Invalid parameter value"),
        /** Invalid text representation */
        INVALID_TEXT_REPRESENTATION("Invalid text representation"),
        /** Invalid binary representation */
        INVALID_BINARY_REPRESENTATION("Invalid binary representation"),
        /** Invalid JSON format */
        INVALID_JSON("Invalid JSON format"),
        /** Invalid enum value */
        INVALID_ENUM("Invalid enum value"),
        /** Unknown data error */
        UNKNOWN("Data error");

        private final String description;

        DataErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class Builder extends ExecutionException.Builder {
        private DataErrorType dataErrorType = DataErrorType.UNKNOWN;

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

        public Builder dataErrorType(DataErrorType dataErrorType) {
            this.dataErrorType = dataErrorType;
            return this;
        }

        @Override
        public DataIntegrityException build() {
            this.category(ErrorCategory.DATA_EXCEPTION);
            return new DataIntegrityException(this);
        }
    }
}
