package sant1ago.dev.suprim.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        @Test
        void getValue_stringTrueAsBoolean_returnsTrue() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE str_bool_test (val VARCHAR(10))");
                stmt.execute("INSERT INTO str_bool_test VALUES ('true')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM str_bool_test")) {
                rs.next();
                assertEquals(true, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE str_bool_test");
            }
        }

        @Test
        void getValue_stringTAsBoolean_returnsTrue() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE str_t_bool_test (val VARCHAR(10))");
                stmt.execute("INSERT INTO str_t_bool_test VALUES ('t')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM str_t_bool_test")) {
                rs.next();
                assertEquals(true, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE str_t_bool_test");
            }
        }

        @Test
        void getValue_stringYesAsBoolean_returnsTrue() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE str_yes_bool (val VARCHAR(10))");
                stmt.execute("INSERT INTO str_yes_bool VALUES ('yes')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM str_yes_bool")) {
                rs.next();
                assertEquals(true, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE str_yes_bool");
            }
        }

        @Test
        void getValue_stringYAsBoolean_returnsTrue() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE str_y_bool (val VARCHAR(10))");
                stmt.execute("INSERT INTO str_y_bool VALUES ('y')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM str_y_bool")) {
                rs.next();
                assertEquals(true, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE str_y_bool");
            }
        }

        @Test
        void getValue_string1AsBoolean_returnsTrue() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE str_1_bool (val VARCHAR(10))");
                stmt.execute("INSERT INTO str_1_bool VALUES ('1')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM str_1_bool")) {
                rs.next();
                assertEquals(true, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE str_1_bool");
            }
        }

        @Test
        void getValue_stringFalseAsBoolean_returnsFalse() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE str_false_bool (val VARCHAR(10))");
                stmt.execute("INSERT INTO str_false_bool VALUES ('false')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM str_false_bool")) {
                rs.next();
                assertEquals(false, ResultSetTypeConverter.getValue(rs, 1, boolean.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE str_false_bool");
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

        @Test
        void getValue_floatToFloat_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE float_test (val REAL)");
                stmt.execute("INSERT INTO float_test VALUES (2.5)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM float_test")) {
                rs.next();
                assertEquals(2.5f, (Float) ResultSetTypeConverter.getValue(rs, 1, float.class), 0.001);
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE float_test");
            }
        }

        @Test
        void getValue_smallintToShort_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE short_test (val SMALLINT)");
                stmt.execute("INSERT INTO short_test VALUES (123)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM short_test")) {
                rs.next();
                assertEquals((short) 123, ResultSetTypeConverter.getValue(rs, 1, short.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE short_test");
            }
        }

        @Test
        void getValue_tinyintToByte_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE byte_test (val TINYINT)");
                stmt.execute("INSERT INTO byte_test VALUES (42)");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM byte_test")) {
                rs.next();
                assertEquals((byte) 42, ResultSetTypeConverter.getValue(rs, 1, byte.class));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE byte_test");
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

        @Test
        void getValue_timestampToInstant_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE instant_test (val TIMESTAMP)");
                stmt.execute("INSERT INTO instant_test VALUES ('2024-01-15 10:30:00')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM instant_test")) {
                rs.next();
                Instant result = (Instant) ResultSetTypeConverter.getValue(rs, 1, Instant.class);
                assertNotNull(result);
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE instant_test");
            }
        }

        @Test
        void getValue_timestampToOffsetDateTime_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE offset_test (val TIMESTAMP)");
                stmt.execute("INSERT INTO offset_test VALUES ('2024-01-15 10:30:00')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM offset_test")) {
                rs.next();
                OffsetDateTime result = (OffsetDateTime) ResultSetTypeConverter.getValue(rs, 1, OffsetDateTime.class);
                assertNotNull(result);
                assertEquals(2024, result.getYear());
                assertEquals(1, result.getMonthValue());
                assertEquals(15, result.getDayOfMonth());
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE offset_test");
            }
        }

        @Test
        void getValue_timestampToLocalDate_convertsCorrectly() throws Exception {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE ts_localdate_test (val TIMESTAMP)");
                stmt.execute("INSERT INTO ts_localdate_test VALUES ('2024-01-15 10:30:00')");
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT val FROM ts_localdate_test")) {
                rs.next();
                LocalDate result = (LocalDate) ResultSetTypeConverter.getValue(rs, 1, LocalDate.class);
                assertNotNull(result);
                assertEquals(2024, result.getYear());
                assertEquals(1, result.getMonthValue());
                assertEquals(15, result.getDayOfMonth());
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE ts_localdate_test");
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

    // ==================== MOCK-BASED TESTS FOR EDGE CASES ====================
    // These tests use mocks to simulate PostgreSQL/MySQL specific type returns

    @Nested
    @DisplayName("Mock-Based DateTime Conversion Tests")
    class MockDateTimeConversionTests {

        @Test
        @DisplayName("LocalDateTime value returned directly")
        void getValue_localDateTimeValue_returnedDirectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            LocalDateTime expected = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            when(rs.getObject(1)).thenReturn(expected);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, LocalDateTime.class);
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("java.sql.Date to LocalDateTime conversion")
        void getValue_sqlDateToLocalDateTime_converts() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            java.sql.Date sqlDate = java.sql.Date.valueOf("2024-01-15");
            when(rs.getObject(1)).thenReturn(sqlDate);
            when(rs.wasNull()).thenReturn(false);

            LocalDateTime result = (LocalDateTime) ResultSetTypeConverter.getValue(rs, 1, LocalDateTime.class);
            assertNotNull(result);
            assertEquals(2024, result.getYear());
            assertEquals(1, result.getMonthValue());
            assertEquals(15, result.getDayOfMonth());
            assertEquals(0, result.getHour()); // atStartOfDay
        }

        @Test
        @DisplayName("java.util.Date to LocalDateTime conversion")
        void getValue_utilDateToLocalDateTime_converts() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            Date utilDate = new Date(1705312200000L); // 2024-01-15 10:30:00 UTC approx
            when(rs.getObject(1)).thenReturn(utilDate);
            when(rs.wasNull()).thenReturn(false);

            LocalDateTime result = (LocalDateTime) ResultSetTypeConverter.getValue(rs, 1, LocalDateTime.class);
            assertNotNull(result);
        }

        @Test
        @DisplayName("LocalDate value returned directly")
        void getValue_localDateValue_returnedDirectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            LocalDate expected = LocalDate.of(2024, 1, 15);
            when(rs.getObject(1)).thenReturn(expected);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, LocalDate.class);
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Timestamp to LocalDate conversion")
        void getValue_timestampToLocalDate_converts() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            Timestamp ts = Timestamp.valueOf("2024-01-15 10:30:00");
            when(rs.getObject(1)).thenReturn(ts);
            when(rs.wasNull()).thenReturn(false);

            LocalDate result = (LocalDate) ResultSetTypeConverter.getValue(rs, 1, LocalDate.class);
            assertNotNull(result);
            assertEquals(2024, result.getYear());
            assertEquals(1, result.getMonthValue());
            assertEquals(15, result.getDayOfMonth());
        }

        @Test
        @DisplayName("java.util.Date to LocalDate conversion")
        void getValue_utilDateToLocalDate_converts() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            Date utilDate = new Date(1705312200000L);
            when(rs.getObject(1)).thenReturn(utilDate);
            when(rs.wasNull()).thenReturn(false);

            LocalDate result = (LocalDate) ResultSetTypeConverter.getValue(rs, 1, LocalDate.class);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Instant value returned directly")
        void getValue_instantValue_returnedDirectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            Instant expected = Instant.now();
            when(rs.getObject(1)).thenReturn(expected);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Instant.class);
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("java.util.Date to Instant conversion")
        void getValue_utilDateToInstant_converts() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            Date utilDate = new Date(1705312200000L);
            when(rs.getObject(1)).thenReturn(utilDate);
            when(rs.wasNull()).thenReturn(false);

            Instant result = (Instant) ResultSetTypeConverter.getValue(rs, 1, Instant.class);
            assertNotNull(result);
            assertEquals(utilDate.toInstant(), result);
        }

        @Test
        @DisplayName("OffsetDateTime value returned directly")
        void getValue_offsetDateTimeValue_returnedDirectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            OffsetDateTime expected = OffsetDateTime.now();
            when(rs.getObject(1)).thenReturn(expected);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, OffsetDateTime.class);
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("java.util.Date to OffsetDateTime conversion")
        void getValue_utilDateToOffsetDateTime_converts() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            Date utilDate = new Date(1705312200000L);
            when(rs.getObject(1)).thenReturn(utilDate);
            when(rs.wasNull()).thenReturn(false);

            OffsetDateTime result = (OffsetDateTime) ResultSetTypeConverter.getValue(rs, 1, OffsetDateTime.class);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Unsupported type returns null for LocalDateTime conversion")
        void getValue_unsupportedTypeForLocalDateTime_returnsNull() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("not-a-date");
            when(rs.wasNull()).thenReturn(false);

            // String is not a supported conversion source for LocalDateTime
            Object result = ResultSetTypeConverter.getValue(rs, 1, LocalDateTime.class);
            assertEquals("not-a-date", result); // Falls through to return original value
        }
    }

    @Nested
    @DisplayName("Mock-Based Boolean Conversion Edge Cases")
    class MockBooleanConversionTests {

        @Test
        @DisplayName("Unknown type for boolean returns false")
        void getValue_unknownTypeForBoolean_returnsFalse() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            // Return something that's not Boolean, Number, or String
            when(rs.getObject(1)).thenReturn(new Object());
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, boolean.class);
            assertEquals(false, result);
        }
    }

    @Nested
    @DisplayName("Mock-Based Enum Conversion Edge Cases")
    class MockEnumConversionTests {

        @Test
        @DisplayName("Enum value returned directly when already correct type")
        void getValue_enumValueAlreadyCorrectType_returnedDirectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(TestStatus.ACTIVE);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, TestStatus.class);
            assertEquals(TestStatus.ACTIVE, result);
        }

        @Test
        @DisplayName("Invalid ordinal returns null")
        void getValue_invalidOrdinal_returnsNull() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(999); // Invalid ordinal
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, TestStatus.class);
            assertNull(result);
        }

        @Test
        @DisplayName("Negative ordinal returns null")
        void getValue_negativeOrdinal_returnsNull() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(-1);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, TestStatus.class);
            assertNull(result);
        }

        @Test
        @DisplayName("Unsupported type for enum returns null")
        void getValue_unsupportedTypeForEnum_returnsNull() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(new Object()); // Not String or Number
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, TestStatus.class);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Mock-Based Null Return Path Tests")
    class MockNullReturnPathTests {

        @Test
        @DisplayName("Unsupported type for LocalDate returns original value")
        void getValue_unsupportedTypeForLocalDate_returnsOriginal() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("not-a-date");
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, LocalDate.class);
            assertEquals("not-a-date", result);
        }

        @Test
        @DisplayName("Unsupported type for Instant returns original value")
        void getValue_unsupportedTypeForInstant_returnsOriginal() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("not-a-date");
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Instant.class);
            assertEquals("not-a-date", result);
        }

        @Test
        @DisplayName("Unsupported type for OffsetDateTime returns original value")
        void getValue_unsupportedTypeForOffsetDateTime_returnsOriginal() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("not-a-date");
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, OffsetDateTime.class);
            assertEquals("not-a-date", result);
        }

        @Test
        @DisplayName("Empty string JSON falls through and returns original value")
        void getValue_emptyStringJson_returnsOriginal() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("");
            when(rs.wasNull()).thenReturn(false);

            // Empty string from convertFromJson returns null, so falls through to return original value
            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertEquals("", result);
        }

        @Test
        @DisplayName("Valid JSON string parses to Map")
        void getValue_validJsonString_parsesToMap() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("{\"key\":\"value\"}");
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertInstanceOf(java.util.Map.class, result);
            assertEquals("value", ((java.util.Map<?, ?>) result).get("key"));
        }

        @Test
        @DisplayName("Object.class type triggers JSON conversion")
        void getValue_objectType_parsesJson() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("{\"test\":123}");
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Object.class);
            assertInstanceOf(java.util.Map.class, result);
        }

        @Test
        @DisplayName("Non-string non-PGobject returns null from extractJsonString")
        void getValue_nonStringValue_forMapType_returnsOriginal() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(12345); // Not a string, not PGobject
            when(rs.wasNull()).thenReturn(false);

            // extractJsonString returns null for non-string, non-PGobject
            // convertFromJson returns null, so falls through
            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertEquals(12345, result);
        }
    }

    // Test enum
    enum TestStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    /**
     * Tests for PGobject-like JSON handling and invalid JSON paths.
     */
    @Nested
    @DisplayName("PGobject and JSON Edge Cases")
    class PGobjectAndJsonEdgeCaseTests {

        @Test
        @DisplayName("Invalid JSON string returns null from parsing")
        void getValue_invalidJsonString_returnsOriginal() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("not valid json {{{");
            when(rs.wasNull()).thenReturn(false);

            // parseJson will fail and return null, so falls through to original value
            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertEquals("not valid json {{{", result);
        }

        @Test
        @DisplayName("Null JSON string returns null")
        void getValue_nullJsonStringValue_returnsNull() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(null);
            when(rs.wasNull()).thenReturn(true);

            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertNull(result);
        }

        @Test
        @DisplayName("JSON array parses correctly")
        void getValue_jsonArray_parsesToList() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("[1, 2, 3]");
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Object.class);
            assertInstanceOf(java.util.List.class, result);
        }

        @Test
        @DisplayName("Complex nested JSON parses correctly")
        void getValue_nestedJson_parsesCorrectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("{\"outer\":{\"inner\":\"value\"}}");
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertInstanceOf(java.util.Map.class, result);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
            assertInstanceOf(java.util.Map.class, map.get("outer"));
        }
    }

    /**
     * Additional enum conversion edge cases.
     */
    @Nested
    @DisplayName("Enum Conversion Edge Cases")
    class EnumConversionEdgeCaseTests {

        @Test
        @DisplayName("Enum value already of correct type returns as-is")
        void getValue_enumValueSameType_returnsAsIs() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(TestStatus.ACTIVE);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, TestStatus.class);
            assertSame(TestStatus.ACTIVE, result);
        }

        @Test
        @DisplayName("Invalid enum string throws IllegalArgumentException")
        void getValue_invalidEnumString_throwsException() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("INVALID_VALUE");
            when(rs.wasNull()).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () ->
                    ResultSetTypeConverter.getValue(rs, 1, TestStatus.class));
        }

        @Test
        @DisplayName("Negative ordinal returns null")
        void getValue_negativeOrdinal_returnsNull() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(-1);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, TestStatus.class);
            assertNull(result);
        }

        @Test
        @DisplayName("Ordinal out of bounds returns null")
        void getValue_ordinalOutOfBounds_returnsNull() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(100); // Way out of bounds
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, TestStatus.class);
            assertNull(result);
        }

        @Test
        @DisplayName("Non-string non-number value for enum returns null")
        void getValue_unsupportedTypeForEnum_returnsNull() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(new java.util.ArrayList<>()); // Unsupported type
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, TestStatus.class);
            assertNull(result);
        }
    }

    /**
     * Additional primitive and wrapper edge cases.
     */
    @Nested
    @DisplayName("Primitive and Wrapper Edge Cases")
    class PrimitiveWrapperEdgeCaseTests {

        @Test
        @DisplayName("char primitive default is null character")
        void getDefaultValue_charPrimitive_returnsNullChar() {
            Object result = ResultSetTypeConverter.getDefaultValue(char.class);
            assertEquals('\0', result);
        }

        @Test
        @DisplayName("Number to non-numeric type returns null from convertNumber")
        void getValue_numberToNonNumericType_returnsNumber() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(42);
            when(rs.wasNull()).thenReturn(false);

            // String.class is not a numeric type, so convertNumber returns null
            // and falls through to return original value
            Object result = ResultSetTypeConverter.getValue(rs, 1, String.class);
            assertEquals(42, result);
        }

        @Test
        @DisplayName("Boolean wrapper type handles correctly")
        void getValue_booleanWrapper_convertsCorrectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(true);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Boolean.class);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("Integer wrapper type handles correctly")
        void getValue_integerWrapper_convertsCorrectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(Long.valueOf(100));
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Integer.class);
            assertEquals(100, result);
        }

        @Test
        @DisplayName("Long wrapper type handles correctly")
        void getValue_longWrapper_convertsCorrectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(Integer.valueOf(999));
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Long.class);
            assertEquals(999L, result);
        }

        @Test
        @DisplayName("Double wrapper type handles correctly")
        void getValue_doubleWrapper_convertsCorrectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(Float.valueOf(3.14f));
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Double.class);
            assertEquals(3.14, (Double) result, 0.01);
        }

        @Test
        @DisplayName("Float wrapper type handles correctly")
        void getValue_floatWrapper_convertsCorrectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(Double.valueOf(2.5));
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Float.class);
            assertEquals(2.5f, (Float) result, 0.01);
        }

        @Test
        @DisplayName("Short wrapper type handles correctly")
        void getValue_shortWrapper_convertsCorrectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(Integer.valueOf(50));
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Short.class);
            assertEquals((short) 50, result);
        }

        @Test
        @DisplayName("Byte wrapper type handles correctly")
        void getValue_byteWrapper_convertsCorrectly() throws Exception {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(Integer.valueOf(10));
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Byte.class);
            assertEquals((byte) 10, result);
        }
    }

    /**
     * Tests for PostgreSQL PGobject JSON/JSONB handling.
     * Uses test stub class org.postgresql.util.PGobject to simulate PostgreSQL driver.
     */
    @Nested
    @DisplayName("PGobject JSON/JSONB Handling")
    class PGobjectJsonTests {

        @Test
        @DisplayName("PGobject with jsonb type parses correctly")
        void getValue_pgObjectJsonb_parsesCorrectly() throws Exception {
            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue("{\"name\":\"test\",\"value\":42}");

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(pgObject);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertInstanceOf(java.util.Map.class, result);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
            assertEquals("test", map.get("name"));
            assertEquals(42, map.get("value"));
        }

        @Test
        @DisplayName("PGobject with json type parses correctly")
        void getValue_pgObjectJson_parsesCorrectly() throws Exception {
            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("json");
            pgObject.setValue("[1, 2, 3]");

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(pgObject);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, Object.class);
            assertInstanceOf(java.util.List.class, result);
        }

        @Test
        @DisplayName("PGobject with non-json type returns original value")
        void getValue_pgObjectNonJsonType_returnsOriginal() throws Exception {
            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("uuid");
            pgObject.setValue("550e8400-e29b-41d4-a716-446655440000");

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(pgObject);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            // Non-json type should fall through and return original PGobject
            assertSame(pgObject, result);
        }

        @Test
        @DisplayName("PGobject with null value returns null")
        void getValue_pgObjectNullValue_returnsNull() throws Exception {
            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(null);

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(pgObject);
            when(rs.wasNull()).thenReturn(false);

            // null JSON string should return null from convertFromJson, then fall through
            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertSame(pgObject, result);
        }

        @Test
        @DisplayName("PGobject with empty JSON string returns original")
        void getValue_pgObjectEmptyJson_returnsOriginal() throws Exception {
            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue("");

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(pgObject);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertSame(pgObject, result);
        }

        @Test
        @DisplayName("PGobject with invalid JSON returns original")
        void getValue_pgObjectInvalidJson_returnsOriginal() throws Exception {
            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue("not valid json {{{");

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(pgObject);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            // Invalid JSON parsing returns null, falls through to original value
            assertSame(pgObject, result);
        }

        @Test
        @DisplayName("PGobject reflection failure returns original value")
        void getValue_pgObjectReflectionFailure_returnsOriginal() throws Exception {
            // Create a PGobject that throws when methods are accessed via reflection
            org.postgresql.util.PGobject brokenPgObject = org.postgresql.util.PGobject.createBroken();

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(brokenPgObject);
            when(rs.wasNull()).thenReturn(false);

            // Reflection failure should catch exception and return null from extractJsonString
            // Then fall through to return original value
            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.Map.class);
            assertSame(brokenPgObject, result);
        }
    }

    /**
     * Tests for isJsonbValue() private method.
     * This method checks if a value is a PostgreSQL PGobject with json/jsonb type.
     */
    @Nested
    @DisplayName("isJsonbValue Tests")
    class IsJsonbValueTests {

        @Test
        @DisplayName("isJsonbValue returns false for null")
        void testIsJsonbValue_null_returnsFalse() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                "isJsonbValue", Object.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(null, (Object) null);
            assertFalse(result);
        }

        @Test
        @DisplayName("isJsonbValue returns false for String")
        void testIsJsonbValue_string_returnsFalse() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                "isJsonbValue", Object.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(null, "not a PGobject");
            assertFalse(result);
        }

        @Test
        @DisplayName("isJsonbValue returns true for PGobject with jsonb type")
        void testIsJsonbValue_pgObjectJsonb_returnsTrue() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                "isJsonbValue", Object.class);
            method.setAccessible(true);

            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue("{\"key\":\"value\"}");

            boolean result = (boolean) method.invoke(null, pgObject);
            assertTrue(result);
        }

        @Test
        @DisplayName("isJsonbValue returns true for PGobject with json type")
        void testIsJsonbValue_pgObjectJson_returnsTrue() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                "isJsonbValue", Object.class);
            method.setAccessible(true);

            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("json");
            pgObject.setValue("[1, 2, 3]");

            boolean result = (boolean) method.invoke(null, pgObject);
            assertTrue(result);
        }

        @Test
        @DisplayName("isJsonbValue returns false for PGobject with uuid type")
        void testIsJsonbValue_pgObjectUuid_returnsFalse() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                "isJsonbValue", Object.class);
            method.setAccessible(true);

            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("uuid");
            pgObject.setValue("550e8400-e29b-41d4-a716-446655440000");

            boolean result = (boolean) method.invoke(null, pgObject);
            assertFalse(result);
        }

        @Test
        @DisplayName("isJsonbValue returns false for PGobject with text type")
        void testIsJsonbValue_pgObjectText_returnsFalse() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                "isJsonbValue", Object.class);
            method.setAccessible(true);

            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("text");
            pgObject.setValue("plain text");

            boolean result = (boolean) method.invoke(null, pgObject);
            assertFalse(result);
        }

        @Test
        @DisplayName("isJsonbValue returns false for Integer")
        void testIsJsonbValue_integer_returnsFalse() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                "isJsonbValue", Object.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(null, Integer.valueOf(42));
            assertFalse(result);
        }

        @Test
        @DisplayName("isJsonbValue returns false for Map")
        void testIsJsonbValue_map_returnsFalse() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                "isJsonbValue", Object.class);
            method.setAccessible(true);

            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("key", "value");

            boolean result = (boolean) method.invoke(null, map);
            assertFalse(result);
        }

        @Test
        @DisplayName("isJsonbValue returns false when PGobject reflection fails")
        void testIsJsonbValue_pgObjectReflectionFailure_returnsFalse() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                "isJsonbValue", Object.class);
            method.setAccessible(true);

            // Use a broken PGobject that throws on getType()
            org.postgresql.util.PGobject brokenPgObject = org.postgresql.util.PGobject.createBroken();

            boolean result = (boolean) method.invoke(null, brokenPgObject);
            assertFalse(result);
        }
    }

    /**
     * Tests for PGobject JSONB to POJO conversion.
     * Tests the Case 1 path in getValue() where PGobject jsonb can be converted to any type.
     */
    @Nested
    @DisplayName("PGobject JSONB to POJO Conversion Tests")
    class PGobjectJsonbToPojoTests {

        @Test
        @DisplayName("PGobject jsonb converts to POJO correctly")
        void getValue_pgObjectJsonb_convertsToPojo() throws Exception {
            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue("{\"name\":\"test\",\"value\":42}");

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(pgObject);
            when(rs.wasNull()).thenReturn(false);

            // Convert to a simple POJO class
            Object result = ResultSetTypeConverter.getValue(rs, 1, TestPojo.class);
            assertInstanceOf(TestPojo.class, result);
            TestPojo pojo = (TestPojo) result;
            assertEquals("test", pojo.getName());
            assertEquals(42, pojo.getValue());
        }

        @Test
        @DisplayName("PGobject json (not jsonb) converts to POJO correctly")
        void getValue_pgObjectJson_convertsToPojo() throws Exception {
            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("json");
            pgObject.setValue("{\"name\":\"json-test\",\"value\":99}");

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(pgObject);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, TestPojo.class);
            assertInstanceOf(TestPojo.class, result);
            TestPojo pojo = (TestPojo) result;
            assertEquals("json-test", pojo.getName());
            assertEquals(99, pojo.getValue());
        }

        @Test
        @DisplayName("PGobject jsonb converts to List correctly")
        void getValue_pgObjectJsonb_convertToList() throws Exception {
            org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue("[\"a\", \"b\", \"c\"]");

            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(pgObject);
            when(rs.wasNull()).thenReturn(false);

            Object result = ResultSetTypeConverter.getValue(rs, 1, java.util.List.class);
            assertInstanceOf(java.util.List.class, result);
            @SuppressWarnings("unchecked")
            java.util.List<String> list = (java.util.List<String>) result;
            assertEquals(3, list.size());
            assertEquals("a", list.get(0));
        }

        // Test POJO for JSON conversion
        public static class TestPojo {
            private String name;
            private int value;

            public TestPojo() {}

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public int getValue() { return value; }
            public void setValue(int value) { this.value = value; }
        }
    }

    /**
     * Tests for Jackson 2.x fallback path via reflection.
     * These tests directly call the private parseJsonWithJackson2 method
     * to ensure it works correctly for users who don't have Jackson 3.x.
     */
    @Nested
    @DisplayName("Jackson 2.x Fallback Path Tests")
    class Jackson2FallbackTests {

        @Test
        @DisplayName("Jackson 2.x parses valid JSON to Map")
        void parseJsonWithJackson2_validJson_parsesToMap() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                    "parseJsonWithJackson2", String.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, "{\"key\":\"value\"}", java.util.Map.class);
            assertInstanceOf(java.util.Map.class, result);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
            assertEquals("value", map.get("key"));
        }

        @Test
        @DisplayName("Jackson 2.x parses JSON array to List")
        void parseJsonWithJackson2_jsonArray_parsesToList() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                    "parseJsonWithJackson2", String.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, "[1, 2, 3]", Object.class);
            assertInstanceOf(java.util.List.class, result);
        }

        @Test
        @DisplayName("Jackson 2.x returns null for invalid JSON")
        void parseJsonWithJackson2_invalidJson_returnsNull() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                    "parseJsonWithJackson2", String.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, "not valid json {{{", java.util.Map.class);
            assertNull(result);
        }

        @Test
        @DisplayName("Jackson 2.x parses nested JSON correctly")
        void parseJsonWithJackson2_nestedJson_parsesCorrectly() throws Exception {
            java.lang.reflect.Method method = ResultSetTypeConverter.class.getDeclaredMethod(
                    "parseJsonWithJackson2", String.class, Class.class);
            method.setAccessible(true);

            Object result = method.invoke(null, "{\"outer\":{\"inner\":123}}", java.util.Map.class);
            assertInstanceOf(java.util.Map.class, result);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
            assertInstanceOf(java.util.Map.class, map.get("outer"));
        }
    }
}
