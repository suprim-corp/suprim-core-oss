package sant1ago.dev.suprim.core.type;

/**
 * SQL comparison and logical operators.
 */
public enum Operator {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUALS(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUALS("<="),
    LIKE("LIKE"),
    ILIKE("ILIKE"),
    IN("IN"),
    NOT_IN("NOT IN"),
    BETWEEN("BETWEEN"),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
    // JSONB operators (PostgreSQL)
    JSONB_CONTAINS("@>"),
    JSONB_CONTAINED_BY("<@"),
    JSONB_KEY_EXISTS("?"),
    JSONB_ARROW("->"),
    JSONB_ARROW_TEXT("->>"),
    // Array operators (PostgreSQL)
    ARRAY_CONTAINS("@>"),
    ARRAY_CONTAINED_BY("<@"),
    ARRAY_OVERLAP("&&"),
    // Subquery operators
    EXISTS("EXISTS"),
    NOT_EXISTS("NOT EXISTS");

    private final String sql;

    Operator(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
