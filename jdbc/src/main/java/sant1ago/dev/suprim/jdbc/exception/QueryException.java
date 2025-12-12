package sant1ago.dev.suprim.jdbc.exception;

import sant1ago.dev.suprim.jdbc.SuprimException;

import java.sql.SQLException;

/**
 * Exception thrown when a SELECT query execution fails.
 *
 * <p>Common causes:
 * <ul>
 *   <li>SQL syntax errors</li>
 *   <li>Invalid column or table references</li>
 *   <li>Query timeout</li>
 *   <li>Permission denied</li>
 * </ul>
 */
public class QueryException extends SuprimException {

    protected QueryException(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static QueryException executionFailed(String sql, SQLException cause) {
        return builder()
                .message("Query execution failed")
                .sql(sql)
                .cause(cause)
                .build();
    }

    public static QueryException executionFailed(String sql, Object[] parameters, SQLException cause) {
        return builder()
                .message("Query execution failed")
                .sql(sql)
                .parameters(parameters)
                .cause(cause)
                .build();
    }

    public static QueryException timeout(String sql, SQLException cause) {
        return builder()
                .message("Query execution timed out")
                .sql(sql)
                .cause(cause)
                .build();
    }

    public static QueryException cancelled(String sql) {
        return builder()
                .message("Query was cancelled")
                .sql(sql)
                .category(ErrorCategory.QUERY_CANCELED)
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
        public Builder category(ErrorCategory category) {
            super.category(category);
            return this;
        }

        @Override
        public QueryException build() {
            return new QueryException(this);
        }
    }
}
