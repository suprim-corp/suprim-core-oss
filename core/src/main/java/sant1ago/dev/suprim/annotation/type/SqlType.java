package sant1ago.dev.suprim.annotation.type;

/**
 * SQL column types for type-safe column definitions.
 * Use base types and specify length/precision via @Column.length() or @Column.precision().
 *
 * <pre>{@code
 * // Simple types
 * @Column(type = SqlType.TEXT)
 * private String description;
 *
 * // With length
 * @Column(type = SqlType.VARCHAR, length = 36)
 * private String uuid;
 *
 * // With precision/scale
 * @Column(type = SqlType.NUMERIC, precision = 10, scale = 2)
 * private BigDecimal amount;
 *
 * // PostgreSQL specific
 * @Column(type = SqlType.JSONB)
 * private Map<String, Object> metadata;
 * }</pre>
 */
public enum SqlType {

    // ==================== AUTO ====================
    /** Auto-detect type from Java field type. */
    AUTO(""),

    // ==================== STRING TYPES ====================
    /** Variable-length string. Use length attribute for size. */
    VARCHAR("VARCHAR"),

    /** Fixed-length string. Use length attribute for size. */
    CHAR("CHAR"),

    /** Unlimited text. */
    TEXT("TEXT"),

    // ==================== NUMERIC TYPES ====================
    /** Small integer (-32768 to 32767). */
    SMALLINT("SMALLINT"),

    /** Standard integer (-2B to 2B). */
    INTEGER("INTEGER"),

    /** Large integer. */
    BIGINT("BIGINT"),

    /** Auto-incrementing small integer. */
    SMALLSERIAL("SMALLSERIAL"),

    /** Auto-incrementing integer. */
    SERIAL("SERIAL"),

    /** Auto-incrementing large integer. */
    BIGSERIAL("BIGSERIAL"),

    /** Exact numeric. Use precision/scale attributes. */
    NUMERIC("NUMERIC"),

    /** Exact numeric (alias for NUMERIC). */
    DECIMAL("DECIMAL"),

    /** Single precision floating-point. */
    REAL("REAL"),

    /** Double precision floating-point. */
    DOUBLE_PRECISION("DOUBLE PRECISION"),

    /** Monetary amount. */
    MONEY("MONEY"),

    // ==================== BOOLEAN ====================
    /** Boolean true/false. */
    BOOLEAN("BOOLEAN"),

    // ==================== DATE/TIME TYPES ====================
    /** Date only (no time). */
    DATE("DATE"),

    /** Time only (no date). */
    TIME("TIME"),

    /** Time with timezone. */
    TIMETZ("TIMETZ"),

    /** Date and time without timezone. */
    TIMESTAMP("TIMESTAMP"),

    /** Date and time with timezone. */
    TIMESTAMPTZ("TIMESTAMPTZ"),

    /** Time interval. */
    INTERVAL("INTERVAL"),

    // ==================== UUID ====================
    /** UUID type. */
    UUID("UUID"),

    // ==================== JSON TYPES ====================
    /** JSON stored as text. */
    JSON("JSON"),

    /** Binary JSON (PostgreSQL). */
    JSONB("JSONB"),

    // ==================== BINARY TYPES ====================
    /** Binary data. */
    BYTEA("BYTEA"),

    // ==================== ARRAY TYPES ====================
    /** Text array. */
    TEXT_ARRAY("TEXT[]"),

    /** Integer array. */
    INTEGER_ARRAY("INTEGER[]"),

    /** Bigint array. */
    BIGINT_ARRAY("BIGINT[]"),

    /** UUID array. */
    UUID_ARRAY("UUID[]"),

    /** Boolean array. */
    BOOLEAN_ARRAY("BOOLEAN[]"),

    /** JSONB array. */
    JSONB_ARRAY("JSONB[]"),

    /** Varchar array. */
    VARCHAR_ARRAY("VARCHAR[]"),

    // ==================== NETWORK TYPES ====================
    /** IPv4 or IPv6 network address. */
    INET("INET"),

    /** IPv4 or IPv6 CIDR block. */
    CIDR("CIDR"),

    /** MAC address. */
    MACADDR("MACADDR"),

    // ==================== GEOMETRIC TYPES ====================
    /** Point on a plane. */
    POINT("POINT"),

    /** Geometric line. */
    LINE("LINE"),

    /** Geometric box. */
    BOX("BOX"),

    /** Geometric circle. */
    CIRCLE("CIRCLE"),

    /** Geometric polygon. */
    POLYGON("POLYGON"),

    // ==================== FULL TEXT SEARCH ====================
    /** Text search vector. */
    TSVECTOR("TSVECTOR"),

    /** Text search query. */
    TSQUERY("TSQUERY"),

    // ==================== RANGE TYPES (PostgreSQL) ====================
    /** Integer range. */
    INT4RANGE("INT4RANGE"),

    /** Bigint range. */
    INT8RANGE("INT8RANGE"),

    /** Numeric range. */
    NUMRANGE("NUMRANGE"),

    /** Timestamp range. */
    TSRANGE("TSRANGE"),

    /** Timestamp with timezone range. */
    TSTZRANGE("TSTZRANGE"),

    /** Date range. */
    DATERANGE("DATERANGE"),

    // ==================== OTHER ====================
    /** XML data. */
    XML("XML"),

    /** Object identifier. */
    OID("OID"),

    /** Bit string. Use length attribute. */
    BIT("BIT"),

    /** Variable-length bit string. Use length attribute. */
    VARBIT("VARBIT");

    private final String sql;

    SqlType(String sql) {
        this.sql = sql;
    }

    /**
     * Get the SQL type string.
     */
    public String getSql() {
        return sql;
    }

    /**
     * Check if this is AUTO (auto-detect).
     */
    public boolean isAuto() {
        return this == AUTO;
    }

    /**
     * Check if this type supports length parameter.
     */
    public boolean supportsLength() {
        return this == VARCHAR || this == CHAR || this == BIT || this == VARBIT;
    }

    /**
     * Check if this type supports precision/scale parameters.
     */
    public boolean supportsPrecision() {
        return this == NUMERIC || this == DECIMAL || this == TIME || this == TIMESTAMP || this == TIMESTAMPTZ;
    }

    @Override
    public String toString() {
        return sql;
    }
}
