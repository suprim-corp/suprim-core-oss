package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SuprimContext thread-local context holder.
 */
@DisplayName("SuprimContext Tests")
class SuprimContextTest {

    private static JdbcDataSource dataSource;

    @BeforeAll
    static void setupDataSource() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:context_test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
    }

    @AfterEach
    void cleanup() {
        // Ensure context is cleared after each test
        SuprimContext.clearContext();
        SuprimContext.clearGlobalExecutor();
    }

    @Nested
    @DisplayName("hasContext()")
    class HasContextTests {

        @Test
        @DisplayName("returns false when no context set")
        void hasContext_noContext_returnsFalse() {
            assertFalse(SuprimContext.hasContext());
        }

        @Test
        @DisplayName("returns true when context is set")
        void hasContext_withContext_returnsTrue() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                SqlDialect dialect = PostgreSqlDialect.INSTANCE;
                SuprimContext.setContext(conn, dialect);
                assertTrue(SuprimContext.hasContext());
            }
        }

        @Test
        @DisplayName("returns false after context cleared")
        void hasContext_afterClear_returnsFalse() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                SqlDialect dialect = PostgreSqlDialect.INSTANCE;
                SuprimContext.setContext(conn, dialect);
                SuprimContext.clearContext();
                assertFalse(SuprimContext.hasContext());
            }
        }
    }

    @Nested
    @DisplayName("getConnection()")
    class GetConnectionTests {

        @Test
        @DisplayName("throws when no context set")
        void getConnection_noContext_throws() {
            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                SuprimContext::getConnection
            );

            assertTrue(ex.getMessage().contains("No active transaction context"));
            assertTrue(ex.getMessage().contains("executor.transaction"));
        }

        @Test
        @DisplayName("returns connection when context set")
        void getConnection_withContext_returnsConnection() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                SqlDialect dialect = PostgreSqlDialect.INSTANCE;
                SuprimContext.setContext(conn, dialect);
                assertSame(conn, SuprimContext.getConnection());
            }
        }
    }

    @Nested
    @DisplayName("getDialect()")
    class GetDialectTests {

        @Test
        @DisplayName("throws when no context set")
        void getDialect_noContext_throws() {
            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                SuprimContext::getDialect
            );

            assertTrue(ex.getMessage().contains("No active transaction context"));
        }

        @Test
        @DisplayName("returns dialect when context set")
        void getDialect_withContext_returnsDialect() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                SqlDialect dialect = PostgreSqlDialect.INSTANCE;
                SuprimContext.setContext(conn, dialect);
                assertSame(dialect, SuprimContext.getDialect());
            }
        }
    }

    @Nested
    @DisplayName("setContext()")
    class SetContextTests {

        @Test
        @DisplayName("throws on null connection")
        void setContext_nullConnection_throws() {
            SqlDialect dialect = PostgreSqlDialect.INSTANCE;

            assertThrows(
                NullPointerException.class,
                () -> SuprimContext.setContext(null, dialect)
            );
        }

        @Test
        @DisplayName("throws on null dialect")
        void setContext_nullDialect_throws() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                assertThrows(
                    NullPointerException.class,
                    () -> SuprimContext.setContext(conn, null)
                );
            }
        }
    }

    @Nested
    @DisplayName("clearContext()")
    class ClearContextTests {

        @Test
        @DisplayName("clears existing context")
        void clearContext_withContext_clears() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                SqlDialect dialect = PostgreSqlDialect.INSTANCE;
                SuprimContext.setContext(conn, dialect);
                assertTrue(SuprimContext.hasContext());
                SuprimContext.clearContext();
                assertFalse(SuprimContext.hasContext());
            }
        }

        @Test
        @DisplayName("does not throw when no context")
        void clearContext_noContext_noOp() {
            assertDoesNotThrow(SuprimContext::clearContext);
        }
    }

    @Nested
    @DisplayName("Global Executor")
    class GlobalExecutorTests {

        @Test
        @DisplayName("setGlobalExecutor sets executor")
        void setGlobalExecutor_setsExecutor() {
            SuprimExecutor executor = SuprimExecutor.create(dataSource);
            SuprimContext.setGlobalExecutor(executor);
            assertTrue(SuprimContext.hasGlobalExecutor());
            assertSame(executor, SuprimContext.getGlobalExecutor());
        }

        @Test
        @DisplayName("setGlobalExecutor throws on null")
        void setGlobalExecutor_nullThrows() {
            assertThrows(NullPointerException.class, () -> SuprimContext.setGlobalExecutor(null));
        }

        @Test
        @DisplayName("hasGlobalExecutor returns false when not set")
        void hasGlobalExecutor_notSet_returnsFalse() {
            assertFalse(SuprimContext.hasGlobalExecutor());
        }

        @Test
        @DisplayName("getGlobalExecutor returns null when not set")
        void getGlobalExecutor_notSet_returnsNull() {
            assertNull(SuprimContext.getGlobalExecutor());
        }

        @Test
        @DisplayName("clearGlobalExecutor clears executor")
        void clearGlobalExecutor_clearsExecutor() {
            SuprimExecutor executor = SuprimExecutor.create(dataSource);
            SuprimContext.setGlobalExecutor(executor);
            assertTrue(SuprimContext.hasGlobalExecutor());
            SuprimContext.clearGlobalExecutor();
            assertFalse(SuprimContext.hasGlobalExecutor());
            assertNull(SuprimContext.getGlobalExecutor());
        }
    }
}
