package sant1ago.dev.suprim.annotation.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TimestampAction enum.
 */
class TimestampActionTest {

    @Test
    void now_exists() {
        assertEquals("NOW", TimestampAction.NOW.name());
    }

    @Test
    void ifNull_exists() {
        assertEquals("IF_NULL", TimestampAction.IF_NULL.name());
    }

    @Test
    void never_exists() {
        assertEquals("NEVER", TimestampAction.NEVER.name());
    }

    @Test
    void values_containsAllConstants() {
        TimestampAction[] values = TimestampAction.values();
        assertEquals(3, values.length);
        assertArrayEquals(
            new TimestampAction[]{TimestampAction.NOW, TimestampAction.IF_NULL, TimestampAction.NEVER},
            values
        );
    }

    @Test
    void valueOf_returnsCorrectConstant() {
        assertEquals(TimestampAction.NOW, TimestampAction.valueOf("NOW"));
        assertEquals(TimestampAction.IF_NULL, TimestampAction.valueOf("IF_NULL"));
        assertEquals(TimestampAction.NEVER, TimestampAction.valueOf("NEVER"));
    }
}
