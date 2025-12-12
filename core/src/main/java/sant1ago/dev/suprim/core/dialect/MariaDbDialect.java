package sant1ago.dev.suprim.core.dialect;

/**
 * MariaDB 10.5+ dialect implementation.
 * MariaDB is MySQL-compatible but has additional features:
 * <ul>
 *   <li>RETURNING clause (10.5+)</li>
 *   <li>SKIP LOCKED and NOWAIT (10.3+)</li>
 * </ul>
 */
public final class MariaDbDialect extends MySqlDialect {

    public static final MariaDbDialect INSTANCE = new MariaDbDialect();

    private MariaDbDialect() {
    }

    @Override
    public String getName() {
        return "MariaDB";
    }

    @Override
    public DialectCapabilities capabilities() {
        return DialectCapabilities.MARIADB;
    }
}
