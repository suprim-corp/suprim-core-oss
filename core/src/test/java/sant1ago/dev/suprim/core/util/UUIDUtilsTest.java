package sant1ago.dev.suprim.core.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UUIDUtils.
 */
class UUIDUtilsTest {

    @Test
    void testV4GeneratesValidUuid() {
        UUID uuid = UUIDUtils.v4();
        assertNotNull(uuid);
        // UUID v4 has version 4 in bits
        assertEquals(4, uuid.version());
    }

    @Test
    void testV4GeneratesUniqueUuids() {
        UUID uuid1 = UUIDUtils.v4();
        UUID uuid2 = UUIDUtils.v4();
        assertNotEquals(uuid1, uuid2);
    }

    @Test
    void testV7GeneratesValidUuid() {
        UUID uuid = UUIDUtils.v7();
        assertNotNull(uuid);
        // UUID v7 has version 7 in bits
        assertEquals(7, uuid.version());
    }

    @Test
    void testV7GeneratesUniqueUuids() {
        UUID uuid1 = UUIDUtils.v7();
        UUID uuid2 = UUIDUtils.v7();
        assertNotEquals(uuid1, uuid2);
    }

    @Test
    void testV7IsSortableByTime() throws InterruptedException {
        UUID uuid1 = UUIDUtils.v7();
        Thread.sleep(2); // Small delay to ensure different timestamps
        UUID uuid2 = UUIDUtils.v7();

        // UUID v7 should be sortable - later UUID should be "greater"
        assertTrue(uuid1.compareTo(uuid2) < 0);
    }
}
