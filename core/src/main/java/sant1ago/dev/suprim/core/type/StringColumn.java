package sant1ago.dev.suprim.core.type;

/**
 * Column with string-specific operators (LIKE, ILIKE).
 *
 * @param <T> Table entity type
 */
public final class StringColumn<T> extends Column<T, String> {

    public StringColumn(Table<T> table, String name, String sqlType) {
        super(table, name, String.class, sqlType);
    }

    /**
     * LIKE pattern match: column LIKE pattern
     * Use % for wildcard matching.
     */
    public Predicate like(String pattern) {
        return new Predicate.SimplePredicate(this, Operator.LIKE, new Literal<>(pattern, String.class));
    }

    /**
     * Case-insensitive LIKE: column ILIKE pattern (PostgreSQL)
     */
    public Predicate ilike(String pattern) {
        return new Predicate.SimplePredicate(this, Operator.ILIKE, new Literal<>(pattern, String.class));
    }

    /**
     * Starts with: column LIKE 'prefix%'
     */
    public Predicate startsWith(String prefix) {
        return like(prefix + "%");
    }

    /**
     * Ends with: column LIKE '%suffix'
     */
    public Predicate endsWith(String suffix) {
        return like("%" + suffix);
    }

    /**
     * Contains: column LIKE '%substring%'
     */
    public Predicate contains(String substring) {
        return like("%" + substring + "%");
    }

    /**
     * Case-insensitive contains (PostgreSQL): column ILIKE '%substring%'
     */
    public Predicate containsIgnoreCase(String substring) {
        return ilike("%" + substring + "%");
    }
}
