package org.postgresql.util;

/**
 * Test stub for PostgreSQL's PGobject class.
 * This simulates the real PGobject to enable testing of JSON/JSONB handling
 * in ResultSetTypeConverter without requiring the actual PostgreSQL driver.
 */
public class PGobject {

    private String type;
    private String value;
    private boolean throwOnAccess = false;

    public PGobject() {
    }

    /**
     * Create a PGobject that throws RuntimeException when getType() or getValue() is called.
     * Used to test exception handling in reflection code.
     */
    public static PGobject createBroken() {
        PGobject obj = new PGobject();
        obj.throwOnAccess = true;
        return obj;
    }

    public String getType() {
        if (throwOnAccess) {
            throw new RuntimeException("Simulated reflection failure");
        }
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        if (throwOnAccess) {
            throw new RuntimeException("Simulated reflection failure");
        }
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
