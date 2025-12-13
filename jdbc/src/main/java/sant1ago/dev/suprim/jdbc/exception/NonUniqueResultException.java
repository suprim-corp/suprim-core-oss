package sant1ago.dev.suprim.jdbc.exception;

/**
 * Exception thrown when a query expected to return at most one result returns multiple.
 *
 * <p>Thrown by methods like {@code queryOne()} when multiple rows match.
 *
 * <p>Usage:
 * <pre>{@code
 * try {
 *     Optional<User> user = executor.queryOne(findByEmailQuery, User.class);
 * } catch (NonUniqueResultException e) {
 *     // Handle unexpected multiple results
 *     log.error("Multiple users found: {} rows", e.getActualCount());
 * }
 * }</pre>
 */
public class NonUniqueResultException extends QueryException {

    private final int actualCount;

    protected NonUniqueResultException(Builder builder) {
        super(builder);
        this.actualCount = builder.actualCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static NonUniqueResultException forQuery(String sql) {
        return builder()
                .message("Query returned multiple results when at most one was expected")
                .sql(sql)
                .build();
    }

    public static NonUniqueResultException forQuery(String sql, int actualCount) {
        return builder()
                .message("Query returned multiple results when at most one was expected")
                .sql(sql)
                .actualCount(actualCount)
                .build();
    }

    public static NonUniqueResultException forQuery(String sql, Object[] parameters) {
        return builder()
                .message("Query returned multiple results when at most one was expected")
                .sql(sql)
                .parameters(parameters)
                .build();
    }

    /**
     * Get the actual number of results returned, if known.
     * Returns -1 if count is unknown.
     */
    public int getActualCount() {
        return actualCount;
    }

    public static class Builder extends QueryException.Builder {
        private int actualCount = -1;

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

        public Builder actualCount(int actualCount) {
            this.actualCount = actualCount;
            return this;
        }

        @Override
        public NonUniqueResultException build() {
            if (actualCount > 0) {
                message = message + " (found " + actualCount + " rows)";
            }
            return new NonUniqueResultException(this);
        }
    }
}
