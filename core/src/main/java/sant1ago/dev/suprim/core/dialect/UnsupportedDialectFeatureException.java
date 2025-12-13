package sant1ago.dev.suprim.core.dialect;

/**
 * Thrown when attempting to use a SQL feature not supported by the current dialect.
 *
 * <pre>{@code
 * if (!dialect.capabilities().supportsReturning()) {
 *     throw new UnsupportedDialectFeatureException("RETURNING", dialect.getName());
 * }
 * }</pre>
 */
public class UnsupportedDialectFeatureException extends RuntimeException {

    private final String feature;
    private final String dialectName;

    public UnsupportedDialectFeatureException(String feature, String dialectName) {
        super(String.format("Feature '%s' is not supported by %s dialect", feature, dialectName));
        this.feature = feature;
        this.dialectName = dialectName;
    }

    public UnsupportedDialectFeatureException(String feature, String dialectName, String workaround) {
        super(String.format("Feature '%s' is not supported by %s dialect. %s", feature, dialectName, workaround));
        this.feature = feature;
        this.dialectName = dialectName;
    }

    public String getFeature() {
        return feature;
    }

    public String getDialectName() {
        return dialectName;
    }
}
