package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ResultSetTypeConverter using H2 database.
 */
class ResultSetTypeConverterTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:convertertest;DB_CLOSE_DELAY=-1");
        connection = dataSource.getConnection();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Nested
    class DefaultValueTests {

        @Test
        void getDefaultValue_booleanPrimitive_returnsFalse() {
            assertEquals(false, ResultSetTypeConverter.getDefaultValue(boolean.class));
        }

        @Test
        void getDefaultValue_intPrimitive_returnsZero() {
            assertEquals(0, ResultSetTypeConverter.getDefaultValue(int.class));
        }

        @Test
        void getDefaultValue_longPrimitive_returnsZeroL() {
            assertEquals(0L, ResultSetTypeConverter.getDefaultValue(long.class));
        }

        @Test
        void getDefaultValue_doublePrimitive_returnsZeroD() {
            assertEquals(0.0, ResultSetTypeConverter.getDefaultValue(double.class));
        }

        @Test
        void getDefaultValue_floatPrimitive_returnsZeroF() {
            assertEquals(0.0f, ResultSetTypeConverter.getDefaultValue(float.class));
        }

        @Test
        void getDefaultValue_shortPrimitive_returnsZero() {
            assertEquals((short) 0, ResultSetTypeConverter.getDefaultValue(short.class));
        }

        @Test
        void getDefaultValue_bytePrimitive_returnsZero() {
            assertEquals((byte) 0, ResultSetTypeConverter.getDefaultValue(byte.class));
        }

        @Test
        void getDefaultValue_charPrimitive_returnsNullChar() {
            assertEquals('\0', ResultSetTypeConverter.getDefaultValue(char.class));
        }

        @Test
        void getDefaultValue_objectType_returnsNull() {
            assertNull(ResultSetTypeConverter.getDefaultValue(String.class));
            assertNull(ResultSetTypeConverter.getDefaultValue(Integer.class));
            assertNull(ResultSetTypeConverter.getDefaultValue(Object.class));
        }
    }

    @Nested
    class BooleanConversionTests {

        @Test
        void getValue_booleanTrue_returnsTrue() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE bool_test (val BOOLEAN)");
                stmt.execute("INSERT INTO bool_test VALUES (TRUE)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM bool_test")) {
                rs.next();
                assertEquals(true, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE bool_test");
            }
        }

        @Test
        void getValue_booleanFalse_returnsFalse() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE bool_test2 (val BOOLEAN)");
                stmt.execute("INSERT INTO bool_test2 VALUES (FALSE)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM bool_test2")) {
                rs.next();
                assertEquals(false, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE bool_test2");
            }
        }

        @Test
        void getValue_integerOneAsBoolean_returnsTrue() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE int_bool_test (val INT)");
                stmt.execute("INSERT INTO int_bool_test VALUES (1)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM int_bool_test")) {
                rs.next();
                assertEquals(true, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE int_bool_test");
            }
        }

        @Test
        void getValue_integerZeroAsBoolean_returnsFalse() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE int_bool_test2 (val INT)");
                stmt.execute("INSERT INTO int_bool_test2 VALUES (0)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM int_bool_test2")) {
                rs.next();
                assertEquals(false, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE int_bool_test2");
            }
        }
    }

    @Nested
    class NumberConversionTests {

        @Test
        void getValue_bigintToLong_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE num_test (val BIGINT)");
                stmt.execute("INSERT INTO num_test VALUES (9999999999)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM num_test")) {
                rs.next();
                assertEquals(9999999999L, ResultSetTypeConverter.getValue(rs, 1, long.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE num_test");
            }
        }

        @Test
        void getValue_intToInteger_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE int_test (val INT)");
                stmt.execute("INSERT INTO int_test VALUES (42)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM int_test")) {
                rs.next();
                assertEquals(42, ResultSetTypeConverter.getValue(rs, 1, int.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE int_test");
            }
        }

        @Test
        void getValue_doubleToDouble_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE double_test (val DOUBLE)");
                stmt.execute("INSERT INTO double_test VALUES (3.14159)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM double_test")) {
                rs.next();
                assertEquals(3.14159, (Double) ResultSetTypeConverter.getValue(rs, 1, double.class), 0.00001);
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE double_test");
            }
        }
    }

    @Nested
    class DateTimeConversionTests {

        @Test
        void getValue_timestampToLocalDateTime_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE datetime_test (val TIMESTAMP)");
                stmt.execute("INSERT INTO datetime_test VALUES ('2024-01-15 10:30:00')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM datetime_test")) {
                rs.next();
                LocalDateTime result = (LocalDateTime) ResultSetTypeConverter.getValue(rs, 1, LocalDateTime.class);
                assertNotNull(result);
                assertEquals(2024, result.getYear());
                assertEquals(1, result.getMonthValue());
                assertEquals(15, result.getDayOfMonth());
                assertEquals(10, result.getHour());
                assertEquals(30, result.getMinute());
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE datetime_test");
            }
        }

        @Test
        void getValue_dateToLocalDate_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE date_test (val DATE)");
                stmt.execute("INSERT INTO date_test VALUES ('2024-01-15')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM date_test")) {
                rs.next();
                LocalDate result = (LocalDate) ResultSetTypeConverter.getValue(rs, 1, LocalDate.class);
                assertNotNull(result);
                assertEquals(2024, result.getYear());
                assertEquals(1, result.getMonthValue());
                assertEquals(15, result.getDayOfMonth());
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE date_test");
            }
        }
    }

    @Nested
    class EnumConversionTests {

        @Test
        void getValue_stringToEnum_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE enum_test (val VARCHAR(50))");
                stmt.execute("INSERT INTO enum_test VALUES ('ACTIVE')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM enum_test")) {
                rs.next();
                assertEquals(TestStatus.ACTIVE, ResultSetTypeConverter.getValue(rs, 1, TestStatus.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE enum_test");
            }
        }

        @Test
        void getValue_stringToEnumCaseInsensitive_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE enum_test2 (val VARCHAR(50))");
                stmt.execute("INSERT INTO enum_test2 VALUES ('inactive')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM enum_test2")) {
                rs.next();
                assertEquals(TestStatus.INACTIVE, ResultSetTypeConverter.getValue(rs, 1, TestStatus.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE enum_test2");
            }
        }

        @Test
        void getValue_ordinalToEnum_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE enum_test3 (val INT)");
                stmt.execute("INSERT INTO enum_test3 VALUES (2)"); // SUSPENDED is ordinal 2
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM enum_test3")) {
                rs.next();
                assertEquals(TestStatus.SUSPENDED, ResultSetTypeConverter.getValue(rs, 1, TestStatus.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE enum_test3");
            }
        }
    }

    @Nested
    class NullHandlingTests {

        @Test
        void getValue_nullForString_returnsNull() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE null_test (val VARCHAR(50))");
                stmt.execute("INSERT INTO null_test VALUES (NULL)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM null_test")) {
                rs.next();
                assertNull(ResultSetTypeConverter.getValue(rs, 1, String.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE null_test");
            }
        }

        @Test
        void getValue_nullForPrimitiveInt_returnsDefault() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE null_int_test (val INT)");
                stmt.execute("INSERT INTO null_int_test VALUES (NULL)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM null_int_test")) {
                rs.next();
                assertEquals(0, ResultSetTypeConverter.getValue(rs, 1, int.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE null_int_test");
            }
        }

        @Test
        void getValue_nullForPrimitiveBoolean_returnsDefault() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE null_bool_test (val BOOLEAN)");
                stmt.execute("INSERT INTO null_bool_test VALUES (NULL)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM null_bool_test")) {
                rs.next();
                assertEquals(false, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE null_bool_test");
            }
        }
    }

    @Nested
    class JsonConversionTests {

        @Test
        void getValue_jsonStringToMap_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE json_test (val VARCHAR(1000))");
                stmt.execute("INSERT INTO json_test VALUES ('{\"key\":\"value\",\"count\":42}')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM json_test")) {
                rs.next();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> result = (java.util.Map<String, Object>)
                        ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);

                assertNotNull(result);
                assertEquals("value", result.get("key"));
                assertEquals(42, result.get("count"));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE json_test");
            }
        }

        @Test
        void getValue_emptyJsonToMap_returnsEmptyMap() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE json_empty_test (val VARCHAR(1000))");
                stmt.execute("INSERT INTO json_empty_test VALUES ('{}')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM json_empty_test")) {
                rs.next();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> result = (java.util.Map<String, Object>)
                        ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);

                assertNotNull(result);
                assertTrue(result.isEmpty());
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE json_empty_test");
            }
        }
    }

    // Test enum
    enum TestStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
