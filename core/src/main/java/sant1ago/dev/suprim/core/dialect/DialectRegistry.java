package sant1ago.dev.suprim.core.dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Registry for SQL dialect instances.
 * Provides auto-detection from JDBC URLs and manual lookup.
 *
 * <p>This OSS version supports PostgreSQL, MySQL, and MariaDB.
 * For commercial databases (Oracle, SQL Server, DB2), add suprim-core dependency.</p>
 *
 * <pre>{@code
 * // Auto-detect from connection
 * SqlDialect dialect = DialectRegistry.detect(connection);
 *
 * // Manual lookup
 * SqlDialect dialect = DialectRegistry.forName("mysql").orElseThrow();
 *
 * // Register custom dialect
 * DialectRegistry.register("custom", MyCustomDialect.INSTANCE);
 * }</pre>
 */
public final class DialectRegistry {

    private static final Map<String, SqlDialect> DIALECTS = new ConcurrentHashMap<>();

    /**
     * License checker function for commercial dialects.
     * Set by suprim-core when loaded.
     */
    private static volatile Predicate<String> licenseChecker = db -> false;

    static {
        // Open-source dialects (free)
        register("postgresql", PostgreSqlDialect.INSTANCE);
        register("postgres", PostgreSqlDialect.INSTANCE);
        register("mysql", MySqlDialect.INSTANCE);
        register("mysql5", MySqlDialect.INSTANCE);
        register("mysql8", MySql8Dialect.INSTANCE);
        register("mariadb", MariaDbDialect.INSTANCE);
    }

    private DialectRegistry() {
    }

    /**
     * Set the license checker function.
     * Called by suprim-core module during initialization.
     *
     * @param checker the license checker predicate
     */
    public static void setLicenseChecker(Predicate<String> checker) {
        licenseChecker = Objects.requireNonNull(checker, "checker cannot be null");
    }

    /**
     * Register a dialect with a name.
     *
     * @param name the dialect name
     * @param dialect the dialect instance
     */
    public static void register(String name, SqlDialect dialect) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(dialect, "dialect cannot be null");
        DIALECTS.put(name.toLowerCase(), dialect);
    }

    /**
     * Get dialect by name.
     *
     * @param name the dialect name
     * @return optional containing the dialect if found and licensed
     */
    public static Optional<SqlDialect> forName(String name) {
        SqlDialect dialect = DIALECTS.get(name.toLowerCase());
        if (dialect == null) {
            return Optional.empty();
        }
        return checkLicenseAndReturn(dialect, name);
    }

    /**
     * Get dialect by name, throwing if not found or not licensed.
     *
     * @param name the dialect name
     * @return the dialect instance
     * @throws UnsupportedOperationException if dialect not found or not licensed
     */
    public static SqlDialect forNameRequired(String name) {
        return forName(name).orElseThrow(() -> {
            SqlDialect dialect = DIALECTS.get(name.toLowerCase());
            if (dialect != null && dialect.isCommercial()) {
                return new UnsupportedOperationException(
                        "Commercial database '" + name + "' requires suprim-core with valid license. " +
                                "Visit https://suprim.dev/pricing");
            }
            return new UnsupportedOperationException("Unknown dialect: " + name);
        });
    }

    /**
     * Auto-detect dialect from JDBC connection.
     *
     * @param connection the JDBC connection
     * @return the detected SQL dialect
     * @throws RuntimeException if dialect detection fails
     */
    public static SqlDialect detect(Connection connection) {
        try {
            String url = connection.getMetaData().getURL().toLowerCase();
            return detectFromUrl(url);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to detect dialect from connection", e);
        }
    }

    /**
     * Auto-detect dialect from JDBC URL.
     *
     * @param jdbcUrl the JDBC URL string
     * @return the detected SQL dialect
     * @throws UnsupportedOperationException if database is not supported or not licensed
     */
    public static SqlDialect detectFromUrl(String jdbcUrl) {
        String url = jdbcUrl.toLowerCase();

        // Open-source databases (free)
        if (url.contains("postgresql") || url.contains("postgres")) {
            return PostgreSqlDialect.INSTANCE;
        }
        if (url.contains("mariadb")) {
            return MariaDbDialect.INSTANCE;
        }
        if (url.contains("mysql")) {
            return MySqlDialect.INSTANCE;
        }
        if (url.contains("h2") || url.contains("hsqldb") || url.contains("sqlite")) {
            return PostgreSqlDialect.INSTANCE; // Compatible fallback
        }

        // Commercial databases - check if suprim-core registered them
        if (url.contains("oracle") || url.contains("sqlserver") || url.contains("mssql") || url.contains("db2")) {
            String dbName = url.contains("oracle") ? "oracle" :
                           url.contains("db2") ? "db2" : "sqlserver";
            SqlDialect dialect = DIALECTS.get(dbName);
            if (dialect != null) {
                return checkLicenseAndReturn(dialect, dbName)
                        .orElseThrow(() -> commercialNotLicensed(dbName));
            }
            throw new UnsupportedOperationException(
                    "Commercial database detected. Add suprim-core dependency with valid license. " +
                            "Visit https://suprim.dev/pricing");
        }

        throw new UnsupportedOperationException(
                "Unsupported database. Supported: PostgreSQL, MySQL, MariaDB (free). " +
                        "Oracle, SQL Server, DB2 require suprim-core. URL: " + jdbcUrl);
    }

    private static Optional<SqlDialect> checkLicenseAndReturn(SqlDialect dialect, String name) {
        if (!dialect.isCommercial()) {
            return Optional.of(dialect);
        }
        if (licenseChecker.test(name)) {
            return Optional.of(dialect);
        }
        return Optional.empty();
    }

    private static UnsupportedOperationException commercialNotLicensed(String dbName) {
        return new UnsupportedOperationException(
                dbName + " requires suprim-core with valid license. " +
                        "Visit https://suprim.dev/pricing");
    }

    /**
     * Get the default dialect (PostgreSQL).
     *
     * @return the default SQL dialect
     */
    public static SqlDialect defaultDialect() {
        return PostgreSqlDialect.INSTANCE;
    }

    /**
     * Check if a dialect is registered.
     *
     * @param name the dialect name
     * @return true if the dialect is registered
     */
    public static boolean isSupported(String name) {
        return DIALECTS.containsKey(name.toLowerCase());
    }
}
