package sant1ago.dev.suprim.jdbc.exception;

import sant1ago.dev.suprim.jdbc.SuprimException;

import java.sql.SQLException;

/**
 * Exception thrown when an INSERT, UPDATE, or DELETE statement fails.
 *
 * <p>Common causes:
 * <ul>
 *   <li>Constraint violations (unique, foreign key, check)</li>
 *   <li>Data type mismatches</li>
 *   <li>Deadlocks</li>
 *   <li>Lock timeouts</li>
 * </ul>
 */
public class ExecutionException extends SuprimException {

    protected ExecutionException(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExecutionException failed(String sql, SQLException cause) {
        return builder()
                .message("Statement execution failed")
                .sql(sql)
                .cause(cause)
                .build();
    }

    public static ExecutionException failed(String sql, Object[] parameters, SQLException cause) {
        return builder()
                .message("Statement execution failed")
                .sql(sql)
                .parameters(parameters)
                .cause(cause)
                .build();
    }

    public static class Builder extends SuprimException.Builder {
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

        @Override
        public ExecutionException build() {
            return new ExecutionException(this);
        }
    }
}
