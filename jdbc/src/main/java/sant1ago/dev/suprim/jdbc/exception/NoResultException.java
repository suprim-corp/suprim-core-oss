package sant1ago.dev.suprim.jdbc.exception;

/**
 * Exception thrown when a query expected to return exactly one result returns none.
 *
 * <p>Thrown by methods like {@code queryOneRequired()} when no rows match the query.
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     User user = executor.queryOneRequired(findByIdQuery, User.class);
 * } catch (NoResultException e) {
 *     // Handle missing record
 *     throw new UserNotFoundException(e.getSql());
 * }
 * }</pre>
 */
public class NoResultException extends QueryException {

    protected NoResultException(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static NoResultException forQuery(String sql) {
        return builder()
                .message("Query returned no results when exactly one was expected")
                .sql(sql)
                .build();
    }

    public static NoResultException forQuery(String sql, Object[] parameters) {
        return builder()
                .message("Query returned no results when exactly one was expected")
                .sql(sql)
                .parameters(parameters)
                .build();
    }

    public static class Builder extends QueryException.Builder {
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
        public NoResultException build() {
            this.category(ErrorCategory.NO_DATA);
            return new NoResultException(this);
        }
    }
}
