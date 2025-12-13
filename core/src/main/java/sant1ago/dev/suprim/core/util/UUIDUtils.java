package sant1ago.dev.suprim.core.util;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import java.util.UUID;

/**
 * UUID generation utilities using FasterXML java-uuid-generator.
 */
public final class UUIDUtils {

    private static final RandomBasedGenerator UUID_V4_GENERATOR = Generators.randomBasedGenerator();
    private static final TimeBasedEpochGenerator UUID_V7_GENERATOR = Generators.timeBasedEpochGenerator();

    private UUIDUtils() {
        // Utility class
    }

    /**
     * Generates a random UUID v4.
     *
     * @return UUID v4
     */
    public static UUID v4() {
        return UUID_V4_GENERATOR.generate();
    }

    /**
     * Generates a time-ordered UUID v7 (RFC 9562).
     * UUID v7 is sortable by creation time and provides better database performance
     * for indexed columns compared to random UUIDs.
     *
     * @return UUID v7
     */
    public static UUID v7() {
        return UUID_V7_GENERATOR.generate();
    }
}
