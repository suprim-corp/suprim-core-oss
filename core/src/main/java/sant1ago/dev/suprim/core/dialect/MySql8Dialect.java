package sant1ago.dev.suprim.core.dialect;

/**
 * MySQL 8.0+ dialect with additional features.
 * Adds support for SKIP LOCKED and NOWAIT.
 */
public final class MySql8Dialect extends MySqlDialect {

    public static final MySql8Dialect INSTANCE = new MySql8Dialect();

    private MySql8Dialect() {
    }

    @Override
    public String getName() {
        return "MySQL 8";
    }

    @Override
    public DialectCapabilities capabilities() {
        return DialectCapabilities.MYSQL_8;
    }
}
