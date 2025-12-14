package sant1ago.dev.suprim.core.dialect;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DialectRegistry.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DialectRegistryTest {

    @Test
    @Order(1)
    void testDefaultLicenseCheckerRejectsCommercial() {
        // Must run FIRST to test the default lambda$static$0 (db -> false)
        SqlDialect commercial = new SqlDialect() {
            @Override public String quoteIdentifier(String id) { return id; }
            @Override public String quoteString(String v) { return v; }
            @Override public String formatBoolean(Boolean v) { return ""; }
            @Override public String getName() { return "DefaultTest"; }
            @Override public boolean isCommercial() { return true; }
        };

        DialectRegistry.register("defaulttest", commercial);

        // Uses default licenseChecker (db -> false), should reject
        assertFalse(DialectRegistry.forName("defaulttest").isPresent());
    }

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

    @Test
    void testSetLicenseCheckerAndCommercialDialect() {
        // Create a mock commercial dialect
        SqlDialect commercialDialect = new SqlDialect() {
            @Override
            public String quoteIdentifier(String identifier) { return "\"" + identifier + "\""; }
            @Override
            public String quoteString(String value) { return "'" + value + "'"; }
            @Override
            public String formatBoolean(Boolean value) { return value ? "TRUE" : "FALSE"; }
            @Override
            public String getName() { return "Commercial"; }
            @Override
            public boolean isCommercial() { return true; }
        };

        // Register commercial dialect
        DialectRegistry.register("testcommercial", commercialDialect);

        // Without license, forName returns empty
        assertFalse(DialectRegistry.forName("testcommercial").isPresent());

        // Set license checker that approves this dialect
        DialectRegistry.setLicenseChecker(name -> "testcommercial".equals(name));

        // Now it should be available
        assertTrue(DialectRegistry.forName("testcommercial").isPresent());
        assertEquals(commercialDialect, DialectRegistry.forName("testcommercial").orElseThrow());

        // Reset license checker
        DialectRegistry.setLicenseChecker(db -> false);
    }

    @Test
    void testForNameRequiredThrowsForUnlicensedCommercial() {
        // Create and register commercial dialect
        SqlDialect commercialDialect = new SqlDialect() {
            @Override
            public String quoteIdentifier(String identifier) { return "\"" + identifier + "\""; }
            @Override
            public String quoteString(String value) { return "'" + value + "'"; }
            @Override
            public String formatBoolean(Boolean value) { return value ? "TRUE" : "FALSE"; }
            @Override
            public String getName() { return "TestDB"; }
            @Override
            public boolean isCommercial() { return true; }
        };

        DialectRegistry.register("testdb", commercialDialect);

        // Ensure no license
        DialectRegistry.setLicenseChecker(db -> false);

        // Should throw with commercial message
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> DialectRegistry.forNameRequired("testdb")
        );
        assertTrue(ex.getMessage().contains("Commercial"));
        assertTrue(ex.getMessage().contains("suprim-core"));
    }

    @Test
    void testDetectFromUrlCommercialRegisteredButNotLicensed() {
        // Register a commercial Oracle dialect
        SqlDialect oracleDialect = new SqlDialect() {
            @Override
            public String quoteIdentifier(String identifier) { return "\"" + identifier + "\""; }
            @Override
            public String quoteString(String value) { return "'" + value + "'"; }
            @Override
            public String formatBoolean(Boolean value) { return value ? "1" : "0"; }
            @Override
            public String getName() { return "Oracle"; }
            @Override
            public boolean isCommercial() { return true; }
        };

        DialectRegistry.register("oracle", oracleDialect);
        DialectRegistry.setLicenseChecker(db -> false);

        // Should throw commercialNotLicensed exception
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> DialectRegistry.detectFromUrl("jdbc:oracle:thin:@localhost:1521:XE")
        );
        assertTrue(ex.getMessage().contains("oracle"));
        assertTrue(ex.getMessage().contains("suprim-core"));
    }

    @Test
    void testDetectFromUrlCommercialRegisteredAndLicensed() {
        // Register a commercial SQLServer dialect
        SqlDialect sqlServerDialect = new SqlDialect() {
            @Override
            public String quoteIdentifier(String identifier) { return "[" + identifier + "]"; }
            @Override
            public String quoteString(String value) { return "'" + value + "'"; }
            @Override
            public String formatBoolean(Boolean value) { return value ? "1" : "0"; }
            @Override
            public String getName() { return "SQLServer"; }
            @Override
            public boolean isCommercial() { return true; }
        };

        DialectRegistry.register("sqlserver", sqlServerDialect);
        DialectRegistry.setLicenseChecker(db -> "sqlserver".equals(db));

        // Should return the dialect when licensed
        SqlDialect detected = DialectRegistry.detectFromUrl("jdbc:sqlserver://localhost:1433;database=db");
        assertEquals(sqlServerDialect, detected);

        // Reset
        DialectRegistry.setLicenseChecker(db -> false);
    }

    @Test
    void testSetLicenseCheckerRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> DialectRegistry.setLicenseChecker(null));
    }

    @Test
    void testDetectFromUrlSqlite() {
        assertEquals(PostgreSqlDialect.INSTANCE,
                DialectRegistry.detectFromUrl("jdbc:sqlite:test.db"));
    }

    @Test
    void testDetectFromUrlHsqldb() {
        assertEquals(PostgreSqlDialect.INSTANCE,
                DialectRegistry.detectFromUrl("jdbc:hsqldb:mem:testdb"));
    }

    @Test
    void testDetectFromUrlMssql() {
        // mssql without registered dialect
        DialectRegistry.setLicenseChecker(db -> false);
        assertThrows(UnsupportedOperationException.class,
                () -> DialectRegistry.detectFromUrl("jdbc:mssql://localhost/db"));
    }

    @Test
    void testDetectFromConnection() throws SQLException {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/testdb");

        SqlDialect dialect = DialectRegistry.detect(connection);
        assertEquals(PostgreSqlDialect.INSTANCE, dialect);
    }

    @Test
    void testDetectFromConnectionThrowsOnSqlException() throws SQLException {
        Connection connection = mock(Connection.class);

        when(connection.getMetaData()).thenThrow(new SQLException("Connection failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DialectRegistry.detect(connection));
        assertTrue(ex.getMessage().contains("Failed to detect dialect"));
    }

    @Test
    void testForNameRequiredDialectExistsButNotCommercial() {
        // Tests branch: dialect != null && !dialect.isCommercial()
        // This occurs when dialect is swapped between forName lookup and lambda re-lookup

        SqlDialect nonCommercial = new SqlDialect() {
            @Override public String quoteIdentifier(String id) { return id; }
            @Override public String quoteString(String v) { return v; }
            @Override public String formatBoolean(Boolean v) { return ""; }
            @Override public String getName() { return "NonCommercial"; }
            @Override public boolean isCommercial() { return false; }
        };

        SqlDialect commercial = new SqlDialect() {
            @Override public String quoteIdentifier(String id) { return id; }
            @Override public String quoteString(String v) { return v; }
            @Override public String formatBoolean(Boolean v) { return ""; }
            @Override public String getName() { return "Commercial"; }
            @Override public boolean isCommercial() { return true; }
        };

        DialectRegistry.register("swaptest", commercial);

        // License checker swaps dialect before returning false
        DialectRegistry.setLicenseChecker(name -> {
            if ("swaptest".equals(name)) {
                DialectRegistry.register("swaptest", nonCommercial);
            }
            return false;
        });

        // Lambda re-fetches and finds non-commercial dialect
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> DialectRegistry.forNameRequired("swaptest")
        );
        assertTrue(ex.getMessage().contains("Unknown dialect"));

        DialectRegistry.setLicenseChecker(db -> false);
    }
}
