package sant1ago.dev.suprim.core.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DialectRegistry.
 */
class DialectRegistryTest {

    @Test
    void testForNameFreeDialects() {
        assertTrue(DialectRegistry.forName("postgresql").isPresent());
        assertTrue(DialectRegistry.forName("postgres").isPresent());
        assertTrue(DialectRegistry.forName("mysql").isPresent());
        assertTrue(DialectRegistry.forName("mysql5").isPresent());
        assertTrue(DialectRegistry.forName("mysql8").isPresent());
        assertTrue(DialectRegistry.forName("mariadb").isPresent());
    }

    @Test
    void testForNameUnknown() {
        assertFalse(DialectRegistry.forName("unknown").isPresent());
    }

    @Test
    void testForNameCaseInsensitive() {
        assertTrue(DialectRegistry.forName("POSTGRESQL").isPresent());
        assertTrue(DialectRegistry.forName("PostgreSQL").isPresent());
        assertTrue(DialectRegistry.forName("MySQL").isPresent());
    }

    @Test
    void testMySqlVersions() {
        assertEquals(MySqlDialect.INSTANCE, DialectRegistry.forName("mysql").orElseThrow());
        assertEquals(MySqlDialect.INSTANCE, DialectRegistry.forName("mysql5").orElseThrow());
        assertEquals(MySql8Dialect.INSTANCE, DialectRegistry.forName("mysql8").orElseThrow());
    }

    @Test
    void testDetectFromUrlPostgreSQL() {
        assertEquals(PostgreSqlDialect.INSTANCE,
                DialectRegistry.detectFromUrl("jdbc:postgresql://localhost:5432/db"));
        assertEquals(PostgreSqlDialect.INSTANCE,
                DialectRegistry.detectFromUrl("jdbc:postgres://localhost:5432/db"));
    }

    @Test
    void testDetectFromUrlMySQL() {
        assertEquals(MySqlDialect.INSTANCE,
                DialectRegistry.detectFromUrl("jdbc:mysql://localhost:3306/db"));
    }

    @Test
    void testDetectFromUrlMariaDB() {
        assertEquals(MariaDbDialect.INSTANCE,
                DialectRegistry.detectFromUrl("jdbc:mariadb://localhost:3306/db"));
    }

    @Test
    void testDetectFromUrlH2FallsBackToPostgreSQL() {
        assertEquals(PostgreSqlDialect.INSTANCE,
                DialectRegistry.detectFromUrl("jdbc:h2:mem:testdb"));
    }

    @Test
    void testDetectFromUrlCommercialWithoutLicense() {
        // Commercial databases should throw without license
        assertThrows(UnsupportedOperationException.class,
                () -> DialectRegistry.detectFromUrl("jdbc:oracle:thin:@localhost:1521:XE"));

        assertThrows(UnsupportedOperationException.class,
                () -> DialectRegistry.detectFromUrl("jdbc:sqlserver://localhost:1433;database=db"));

        assertThrows(UnsupportedOperationException.class,
                () -> DialectRegistry.detectFromUrl("jdbc:db2://localhost:50000/db"));
    }

    @Test
    void testDetectFromUrlUnsupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> DialectRegistry.detectFromUrl("jdbc:sybase://localhost:5000/db"));
    }

    @Test
    void testDefaultDialect() {
        assertEquals(PostgreSqlDialect.INSTANCE, DialectRegistry.defaultDialect());
    }

    @Test
    void testIsSupported() {
        assertTrue(DialectRegistry.isSupported("postgresql"));
        assertTrue(DialectRegistry.isSupported("mysql"));
        assertTrue(DialectRegistry.isSupported("mariadb"));
        assertFalse(DialectRegistry.isSupported("sybase"));
    }

    @Test
    void testRegisterCustomDialect() {
        SqlDialect custom = new SqlDialect() {
            @Override
            public String quoteIdentifier(String identifier) {
                return "[" + identifier + "]";
            }

            @Override
            public String quoteString(String value) {
                return "'" + value + "'";
            }

            @Override
            public String formatBoolean(Boolean value) {
                return value != null && value ? "1" : "0";
            }

            @Override
            public String getName() {
                return "Custom";
            }
        };

        DialectRegistry.register("custom", custom);
        assertTrue(DialectRegistry.forName("custom").isPresent());
        assertEquals(custom, DialectRegistry.forName("custom").orElseThrow());
    }

    @Test
    void testForNameRequired() {
        SqlDialect dialect = DialectRegistry.forNameRequired("postgresql");
        assertEquals(PostgreSqlDialect.INSTANCE, dialect);
    }

    @Test
    void testForNameRequiredThrowsForUnknown() {
        assertThrows(UnsupportedOperationException.class,
                () -> DialectRegistry.forNameRequired("unknown"));
    }
}
