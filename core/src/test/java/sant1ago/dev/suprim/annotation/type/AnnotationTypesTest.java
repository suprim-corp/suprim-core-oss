package sant1ago.dev.suprim.annotation.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for annotation types in sant1ago.dev.suprim.annotation.type package.
 */
class AnnotationTypesTest {

    // ==================== GenerationType Tests ====================

    @Test
    void testGenerationTypeIsNone() {
        assertTrue(GenerationType.NONE.isNone());
        assertFalse(GenerationType.IDENTITY.isNone());
        assertFalse(GenerationType.SEQUENCE.isNone());
        assertFalse(GenerationType.UUID_V4.isNone());
        assertFalse(GenerationType.UUID_V7.isNone());
        assertFalse(GenerationType.UUID_DB.isNone());
    }

    @Test
    void testGenerationTypeIsUuid() {
        assertFalse(GenerationType.NONE.isUuid());
        assertFalse(GenerationType.IDENTITY.isUuid());
        assertFalse(GenerationType.SEQUENCE.isUuid());
        assertTrue(GenerationType.UUID_V4.isUuid());
        assertTrue(GenerationType.UUID_V7.isUuid());
        assertTrue(GenerationType.UUID_DB.isUuid());
    }

    @Test
    void testGenerationTypeIsApplicationGenerated() {
        assertFalse(GenerationType.NONE.isApplicationGenerated());
        assertFalse(GenerationType.IDENTITY.isApplicationGenerated());
        assertFalse(GenerationType.SEQUENCE.isApplicationGenerated());
        assertTrue(GenerationType.UUID_V4.isApplicationGenerated());
        assertTrue(GenerationType.UUID_V7.isApplicationGenerated());
        assertFalse(GenerationType.UUID_DB.isApplicationGenerated());
    }

    @Test
    void testGenerationTypeIsDatabaseGenerated() {
        assertFalse(GenerationType.NONE.isDatabaseGenerated());
        assertTrue(GenerationType.IDENTITY.isDatabaseGenerated());
        assertTrue(GenerationType.SEQUENCE.isDatabaseGenerated());
        assertFalse(GenerationType.UUID_V4.isDatabaseGenerated());
        assertFalse(GenerationType.UUID_V7.isDatabaseGenerated());
        assertTrue(GenerationType.UUID_DB.isDatabaseGenerated());
    }

    // ==================== SqlType Tests ====================

    @Test
    void testSqlTypeGetSql() {
        assertEquals("", SqlType.AUTO.getSql());
        assertEquals("VARCHAR", SqlType.VARCHAR.getSql());
        assertEquals("TEXT", SqlType.TEXT.getSql());
        assertEquals("INTEGER", SqlType.INTEGER.getSql());
        assertEquals("BIGINT", SqlType.BIGINT.getSql());
        assertEquals("BOOLEAN", SqlType.BOOLEAN.getSql());
        assertEquals("TIMESTAMP", SqlType.TIMESTAMP.getSql());
        assertEquals("TIMESTAMPTZ", SqlType.TIMESTAMPTZ.getSql());
        assertEquals("UUID", SqlType.UUID.getSql());
        assertEquals("JSONB", SqlType.JSONB.getSql());
        assertEquals("DOUBLE PRECISION", SqlType.DOUBLE_PRECISION.getSql());
    }

    @Test
    void testSqlTypeIsAuto() {
        assertTrue(SqlType.AUTO.isAuto());
        assertFalse(SqlType.VARCHAR.isAuto());
        assertFalse(SqlType.INTEGER.isAuto());
        assertFalse(SqlType.JSONB.isAuto());
    }

    @Test
    void testSqlTypeSupportsLength() {
        assertTrue(SqlType.VARCHAR.supportsLength());
        assertTrue(SqlType.CHAR.supportsLength());
        assertTrue(SqlType.BIT.supportsLength());
        assertTrue(SqlType.VARBIT.supportsLength());
        assertFalse(SqlType.TEXT.supportsLength());
        assertFalse(SqlType.INTEGER.supportsLength());
        assertFalse(SqlType.JSONB.supportsLength());
        assertFalse(SqlType.AUTO.supportsLength());
    }

    @Test
    void testSqlTypeSupportsPrecision() {
        assertTrue(SqlType.NUMERIC.supportsPrecision());
        assertTrue(SqlType.DECIMAL.supportsPrecision());
        assertTrue(SqlType.TIME.supportsPrecision());
        assertTrue(SqlType.TIMESTAMP.supportsPrecision());
        assertTrue(SqlType.TIMESTAMPTZ.supportsPrecision());
        assertFalse(SqlType.VARCHAR.supportsPrecision());
        assertFalse(SqlType.INTEGER.supportsPrecision());
        assertFalse(SqlType.AUTO.supportsPrecision());
    }

    @Test
    void testSqlTypeToString() {
        assertEquals("VARCHAR", SqlType.VARCHAR.toString());
        assertEquals("INTEGER", SqlType.INTEGER.toString());
        assertEquals("JSONB", SqlType.JSONB.toString());
        assertEquals("", SqlType.AUTO.toString());
    }

    // ==================== FetchType Tests ====================

    @Test
    void testFetchTypeValues() {
        assertEquals(2, FetchType.values().length);
        assertNotNull(FetchType.LAZY);
        assertNotNull(FetchType.EAGER);
        assertEquals(FetchType.LAZY, FetchType.valueOf("LAZY"));
        assertEquals(FetchType.EAGER, FetchType.valueOf("EAGER"));
    }

    // ==================== CascadeType Tests ====================

    @Test
    void testCascadeTypeValues() {
        assertEquals(6, CascadeType.values().length);
        assertNotNull(CascadeType.NONE);
        assertNotNull(CascadeType.DB);
        assertNotNull(CascadeType.AUTO);
        assertNotNull(CascadeType.APPLICATION);
        assertNotNull(CascadeType.ALL);
        assertNotNull(CascadeType.DELETE_ORPHAN);
        assertEquals(CascadeType.NONE, CascadeType.valueOf("NONE"));
        assertEquals(CascadeType.AUTO, CascadeType.valueOf("AUTO"));
    }

    // ==================== IdGenerator.None Tests ====================

    @Test
    void testIdGeneratorNone() {
        IdGenerator.None generator = new IdGenerator.None();
        assertNull(generator.generate());
    }
}
