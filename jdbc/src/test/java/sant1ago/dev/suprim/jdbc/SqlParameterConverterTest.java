package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.core.query.QueryResult;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SqlParameterConverterTest {

    @Test
    void convert_noParameters_returnsSqlUnchanged() {
        QueryResult query = new QueryResult(
                "SELECT * FROM users",
                Map.of()
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT * FROM users", result.sql());
        assertEquals(0, result.parameters().length);
    }

    @Test
    void convert_singleParameter_replacesWithQuestionMark() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "test@example.com");

        QueryResult query = new QueryResult(
                "SELECT * FROM users WHERE email = :p1",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT * FROM users WHERE email = ?", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals("test@example.com", result.parameters()[0]);
    }

    @Test
    void convert_multipleParameters_replacesInOrder() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "test@example.com");
        params.put("p2", "Test User");
        params.put("p3", true);

        QueryResult query = new QueryResult(
                "INSERT INTO users (email, name, is_active) VALUES (:p1, :p2, :p3)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO users (email, name, is_active) VALUES (?, ?, ?)", result.sql());
        assertEquals(3, result.parameters().length);
        assertEquals("test@example.com", result.parameters()[0]);
        assertEquals("Test User", result.parameters()[1]);
        assertEquals(true, result.parameters()[2]);
    }

    @Test
    void convert_parameterUsedMultipleTimes_replacesEachOccurrence() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "test@example.com");

        QueryResult query = new QueryResult(
                "SELECT * FROM users WHERE email = :p1 OR backup_email = :p1",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT * FROM users WHERE email = ? OR backup_email = ?", result.sql());
        assertEquals(2, result.parameters().length);
        assertEquals("test@example.com", result.parameters()[0]);
        assertEquals("test@example.com", result.parameters()[1]);
    }

    @Test
    void convert_parametersInWhereClause_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", 100);
        params.put("p2", 200);

        QueryResult query = new QueryResult(
                "SELECT * FROM products WHERE price >= :p1 AND price <= :p2",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT * FROM products WHERE price >= ? AND price <= ?", result.sql());
        assertEquals(2, result.parameters().length);
        assertEquals(100, result.parameters()[0]);
        assertEquals(200, result.parameters()[1]);
    }

    @Test
    void convert_updateQuery_replacesAllParameters() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "Updated Name");
        params.put("p2", false);
        params.put("p3", 42L);

        QueryResult query = new QueryResult(
                "UPDATE users SET name = :p1, is_active = :p2 WHERE id = :p3",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("UPDATE users SET name = ?, is_active = ? WHERE id = ?", result.sql());
        assertEquals(3, result.parameters().length);
        assertEquals("Updated Name", result.parameters()[0]);
        assertEquals(false, result.parameters()[1]);
        assertEquals(42L, result.parameters()[2]);
    }

    @Test
    void convert_nullParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", null);

        QueryResult query = new QueryResult(
                "UPDATE users SET name = :p1 WHERE id = 1",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("UPDATE users SET name = ? WHERE id = 1", result.sql());
        assertEquals(1, result.parameters().length);
        assertNull(result.parameters()[0]);
    }

    @Test
    void convert_parameterWithNumbers_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p10", "value10");
        params.put("p1", "value1");
        params.put("p2", "value2");

        QueryResult query = new QueryResult(
                "SELECT :p1, :p2, :p10",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT ?, ?, ?", result.sql());
        // Order follows SQL occurrence, not map order
        assertEquals(3, result.parameters().length);
    }

    // Basic Type Conversions

    @Test
    void convert_longParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", Long.MAX_VALUE);
        params.put("p2", 42L);
        params.put("p3", Long.MIN_VALUE);

        QueryResult query = new QueryResult(
                "SELECT * FROM data WHERE val1 = :p1 AND val2 = :p2 AND val3 = :p3",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT * FROM data WHERE val1 = ? AND val2 = ? AND val3 = ?", result.sql());
        assertEquals(3, result.parameters().length);
        assertEquals(Long.MAX_VALUE, result.parameters()[0]);
        assertEquals(42L, result.parameters()[1]);
        assertEquals(Long.MIN_VALUE, result.parameters()[2]);
    }

    @Test
    void convert_integerParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", Integer.MAX_VALUE);
        params.put("p2", 123);
        params.put("p3", Integer.MIN_VALUE);

        QueryResult query = new QueryResult(
                "INSERT INTO numbers (max_val, mid_val, min_val) VALUES (:p1, :p2, :p3)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO numbers (max_val, mid_val, min_val) VALUES (?, ?, ?)", result.sql());
        assertEquals(3, result.parameters().length);
        assertEquals(Integer.MAX_VALUE, result.parameters()[0]);
        assertEquals(123, result.parameters()[1]);
        assertEquals(Integer.MIN_VALUE, result.parameters()[2]);
    }

    @Test
    void convert_shortParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", Short.MAX_VALUE);
        params.put("p2", (short) 100);

        QueryResult query = new QueryResult(
                "UPDATE config SET max_value = :p1, default_value = :p2",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("UPDATE config SET max_value = ?, default_value = ?", result.sql());
        assertEquals(2, result.parameters().length);
        assertEquals(Short.MAX_VALUE, result.parameters()[0]);
        assertEquals((short) 100, result.parameters()[1]);
    }

    @Test
    void convert_byteParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", Byte.MAX_VALUE);
        params.put("p2", (byte) 42);

        QueryResult query = new QueryResult(
                "INSERT INTO bytes (max_byte, value) VALUES (:p1, :p2)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO bytes (max_byte, value) VALUES (?, ?)", result.sql());
        assertEquals(2, result.parameters().length);
        assertEquals(Byte.MAX_VALUE, result.parameters()[0]);
        assertEquals((byte) 42, result.parameters()[1]);
    }

    @Test
    void convert_doubleParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", 3.14159265359);
        params.put("p2", Double.MAX_VALUE);
        params.put("p3", Double.MIN_VALUE);

        QueryResult query = new QueryResult(
                "SELECT * FROM measurements WHERE pi = :p1 OR max_d = :p2 OR min_d = :p3",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT * FROM measurements WHERE pi = ? OR max_d = ? OR min_d = ?", result.sql());
        assertEquals(3, result.parameters().length);
        assertEquals(3.14159265359, result.parameters()[0]);
        assertEquals(Double.MAX_VALUE, result.parameters()[1]);
        assertEquals(Double.MIN_VALUE, result.parameters()[2]);
    }

    @Test
    void convert_floatParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", 3.14f);
        params.put("p2", Float.MAX_VALUE);

        QueryResult query = new QueryResult(
                "UPDATE calculations SET pi = :p1, max_float = :p2",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("UPDATE calculations SET pi = ?, max_float = ?", result.sql());
        assertEquals(2, result.parameters().length);
        assertEquals(3.14f, result.parameters()[0]);
        assertEquals(Float.MAX_VALUE, result.parameters()[1]);
    }

    @Test
    void convert_emptyString_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "");

        QueryResult query = new QueryResult(
                "UPDATE users SET description = :p1 WHERE id = 1",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("UPDATE users SET description = ? WHERE id = 1", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals("", result.parameters()[0]);
    }

    // Date/Time Type Conversions

    @Test
    void convert_localDateTimeParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.of(2025, 12, 10, 14, 30, 45);
        params.put("p1", now);

        QueryResult query = new QueryResult(
                "INSERT INTO events (created_at) VALUES (:p1)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO events (created_at) VALUES (?)", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(now, result.parameters()[0]);
    }

    @Test
    void convert_localDateParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        LocalDate date = LocalDate.of(2025, 12, 10);
        params.put("p1", date);

        QueryResult query = new QueryResult(
                "SELECT * FROM bookings WHERE booking_date = :p1",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT * FROM bookings WHERE booking_date = ?", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(date, result.parameters()[0]);
    }

    @Test
    void convert_localTimeParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        LocalTime time = LocalTime.of(14, 30, 45);
        params.put("p1", time);

        QueryResult query = new QueryResult(
                "UPDATE schedule SET meeting_time = :p1",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("UPDATE schedule SET meeting_time = ?", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(time, result.parameters()[0]);
    }

    @Test
    void convert_offsetDateTimeParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2025, 12, 10, 14, 30, 45, 0, ZoneOffset.ofHours(5));
        params.put("p1", offsetDateTime);

        QueryResult query = new QueryResult(
                "INSERT INTO logs (timestamp) VALUES (:p1)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO logs (timestamp) VALUES (?)", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(offsetDateTime, result.parameters()[0]);
    }

    @Test
    void convert_zonedDateTimeParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 12, 10, 14, 30, 45, 0, ZoneId.of("America/New_York"));
        params.put("p1", zonedDateTime);

        QueryResult query = new QueryResult(
                "INSERT INTO appointments (scheduled_at) VALUES (:p1)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO appointments (scheduled_at) VALUES (?)", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(zonedDateTime, result.parameters()[0]);
    }

    @Test
    void convert_instantParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        Instant instant = Instant.ofEpochSecond(1702220445L);
        params.put("p1", instant);

        QueryResult query = new QueryResult(
                "UPDATE events SET occurred_at = :p1 WHERE id = 1",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("UPDATE events SET occurred_at = ? WHERE id = 1", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(instant, result.parameters()[0]);
    }

    @Test
    void convert_sqlTimestampParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        params.put("p1", timestamp);

        QueryResult query = new QueryResult(
                "INSERT INTO audit (created_at) VALUES (:p1)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO audit (created_at) VALUES (?)", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(timestamp, result.parameters()[0]);
    }

    @Test
    void convert_sqlDateParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        java.sql.Date sqlDate = java.sql.Date.valueOf("2025-12-10");
        params.put("p1", sqlDate);

        QueryResult query = new QueryResult(
                "SELECT * FROM events WHERE event_date = :p1",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT * FROM events WHERE event_date = ?", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(sqlDate, result.parameters()[0]);
    }

    @Test
    void convert_sqlTimeParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        java.sql.Time sqlTime = java.sql.Time.valueOf("14:30:45");
        params.put("p1", sqlTime);

        QueryResult query = new QueryResult(
                "UPDATE schedule SET start_time = :p1",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("UPDATE schedule SET start_time = ?", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(sqlTime, result.parameters()[0]);
    }

    // Special Type Conversions

    @Test
    void convert_uuidParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        UUID uuid = UUID.randomUUID();
        params.put("p1", uuid);

        QueryResult query = new QueryResult(
                "INSERT INTO sessions (session_id) VALUES (:p1)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO sessions (session_id) VALUES (?)", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(uuid, result.parameters()[0]);
    }

    @Test
    void convert_bigDecimalParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        BigDecimal price = new BigDecimal("99999.99");
        BigDecimal discount = new BigDecimal("0.15");
        params.put("p1", price);
        params.put("p2", discount);

        QueryResult query = new QueryResult(
                "INSERT INTO products (price, discount_rate) VALUES (:p1, :p2)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO products (price, discount_rate) VALUES (?, ?)", result.sql());
        assertEquals(2, result.parameters().length);
        assertEquals(price, result.parameters()[0]);
        assertEquals(discount, result.parameters()[1]);
    }

    @Test
    void convert_bigIntegerParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        BigInteger largeNumber = new BigInteger("123456789012345678901234567890");
        params.put("p1", largeNumber);

        QueryResult query = new QueryResult(
                "INSERT INTO big_numbers (value) VALUES (:p1)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO big_numbers (value) VALUES (?)", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(largeNumber, result.parameters()[0]);
    }

    @Test
    void convert_enumParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", TestStatus.ACTIVE);
        params.put("p2", TestStatus.PENDING);

        QueryResult query = new QueryResult(
                "UPDATE users SET status = :p1 WHERE previous_status = :p2",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("UPDATE users SET status = ? WHERE previous_status = ?", result.sql());
        assertEquals(2, result.parameters().length);
        assertEquals(TestStatus.ACTIVE, result.parameters()[0]);
        assertEquals(TestStatus.PENDING, result.parameters()[1]);
    }

    @Test
    void convert_listParameter_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        List<Integer> ids = List.of(1, 2, 3, 4, 5);
        params.put("p1", ids);

        QueryResult query = new QueryResult(
                "SELECT * FROM users WHERE id IN (:p1)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("SELECT * FROM users WHERE id IN (?)", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(ids, result.parameters()[0]);
    }

    @Test
    void convert_multipleTimezones_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        ZonedDateTime nyTime = ZonedDateTime.of(2025, 12, 10, 14, 30, 0, 0, ZoneId.of("America/New_York"));
        ZonedDateTime tokyoTime = ZonedDateTime.of(2025, 12, 10, 14, 30, 0, 0, ZoneId.of("Asia/Tokyo"));
        params.put("p1", nyTime);
        params.put("p2", tokyoTime);

        QueryResult query = new QueryResult(
                "INSERT INTO meetings (ny_time, tokyo_time) VALUES (:p1, :p2)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO meetings (ny_time, tokyo_time) VALUES (?, ?)", result.sql());
        assertEquals(2, result.parameters().length);
        assertEquals(nyTime, result.parameters()[0]);
        assertEquals(tokyoTime, result.parameters()[1]);
    }

    @Test
    void convert_veryLargeNumber_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        BigDecimal veryLarge = new BigDecimal("9".repeat(100) + ".99");
        params.put("p1", veryLarge);

        QueryResult query = new QueryResult(
                "INSERT INTO financial_data (amount) VALUES (:p1)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO financial_data (amount) VALUES (?)", result.sql());
        assertEquals(1, result.parameters().length);
        assertEquals(veryLarge, result.parameters()[0]);
    }

    @Test
    void convert_mixedTypes_handlesCorrectly() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1", "John Doe");
        params.put("p2", 42);
        params.put("p3", 99.99);
        params.put("p4", true);
        params.put("p5", LocalDate.of(2025, 12, 10));
        params.put("p6", UUID.randomUUID());
        params.put("p7", new BigDecimal("1000.50"));

        QueryResult query = new QueryResult(
                "INSERT INTO records (name, age, price, active, created, uuid, amount) VALUES (:p1, :p2, :p3, :p4, :p5, :p6, :p7)",
                params
        );

        SqlParameterConverter.Result result = SqlParameterConverter.convert(query);

        assertEquals("INSERT INTO records (name, age, price, active, created, uuid, amount) VALUES (?, ?, ?, ?, ?, ?, ?)", result.sql());
        assertEquals(7, result.parameters().length);
        assertEquals("John Doe", result.parameters()[0]);
        assertEquals(42, result.parameters()[1]);
        assertEquals(99.99, result.parameters()[2]);
        assertEquals(true, result.parameters()[3]);
        assertEquals(LocalDate.of(2025, 12, 10), result.parameters()[4]);
        assertNotNull(result.parameters()[5]);
        assertEquals(new BigDecimal("1000.50"), result.parameters()[6]);
    }

    // Helper enum for testing
    private enum TestStatus {
        ACTIVE, PENDING, INACTIVE
    }
}
