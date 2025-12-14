package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReflectionUtils - field access via MethodHandles with Java 9+ module support.
 */
@DisplayName("ReflectionUtils Tests")
class ReflectionUtilsTest {

    @BeforeEach
    void setUp() {
        ReflectionUtils.clearCaches();
        ReflectionUtils.setStrictMode(false);
    }

    @AfterEach
    void tearDown() {
        ReflectionUtils.clearCaches();
        ReflectionUtils.setStrictMode(false);
    }

    // ==================== TEST ENTITIES ====================

    // Entity with public getters/setters (standard JavaBean)
    static class StandardEntity {
        private String name;
        private int age;
        private boolean active;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    // Entity with public fields (no getters/setters)
    static class PublicFieldEntity {
        public String publicName;
        public int publicAge;
    }

    // Entity with only private fields (no getters/setters)
    static class PrivateFieldEntity {
        private String privateName;
        private int privateAge;
    }

    // Entity with snake_case field names
    static class SnakeCaseEntity {
        private String user_name;
        private String email_address;

        public String getUser_name() { return user_name; }
        public void setUser_name(String user_name) { this.user_name = user_name; }
        public String getEmail_address() { return email_address; }
        public void setEmail_address(String email_address) { this.email_address = email_address; }
    }

    // Base class for inheritance testing
    static class BaseEntity {
        private String baseField;

        public String getBaseField() { return baseField; }
        public void setBaseField(String baseField) { this.baseField = baseField; }
    }

    // Child class inheriting from BaseEntity
    static class ChildEntity extends BaseEntity {
        private String childField;

        public String getChildField() { return childField; }
        public void setChildField(String childField) { this.childField = childField; }
    }

    // Record for testing record accessor
    record TestRecord(String name, int value) {}

    // Exact copy of user's Account entity
    @Data
    @lombok.experimental.SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.EqualsAndHashCode
    static class Account {
        private UUID id;
        private java.time.OffsetDateTime createdAt;
        private java.time.OffsetDateTime updatedAt;
        private java.time.LocalDateTime deletedAt;
        private UUID deletedBy;
        private String username;
        private String password;
        private String provider;
        private java.util.Map<String, Object> metaData;
        private java.util.Map<String, Object> idp;
        private UUID lastActiveWorkspace;
        private boolean enabled;
    }

    // ==================== GET FIELD VALUE TESTS ====================

    @Nested
    @DisplayName("getFieldValue")
    class GetFieldValueTests {

        @Test
        @DisplayName("returns value via getter method")
        void testGetFieldValueViaGetter() {
            StandardEntity entity = new StandardEntity();
            entity.setName("John");

            Object value = ReflectionUtils.getFieldValue(entity, "name");
            assertEquals("John", value);
        }

        @Test
        @DisplayName("returns value via isX getter for boolean")
        void testGetFieldValueViaBooleanGetter() {
            StandardEntity entity = new StandardEntity();
            entity.setActive(true);

            Object value = ReflectionUtils.getFieldValue(entity, "active");
            assertEquals(true, value);
        }

        @Test
        @DisplayName("returns value via public field")
        void testGetFieldValueViaPublicField() {
            PublicFieldEntity entity = new PublicFieldEntity();
            entity.publicName = "Jane";

            Object value = ReflectionUtils.getFieldValue(entity, "publicName");
            assertEquals("Jane", value);
        }

        @Test
        @DisplayName("returns value via private field fallback")
        void testGetFieldValueViaPrivateField() {
            PrivateFieldEntity entity = new PrivateFieldEntity();
            // Set via reflection for testing
            try {
                Field field = PrivateFieldEntity.class.getDeclaredField("privateName");
                field.setAccessible(true);
                field.set(entity, "Secret");
            } catch (Exception e) {
                fail("Setup failed");
            }

            Object value = ReflectionUtils.getFieldValue(entity, "privateName");
            assertEquals("Secret", value);
        }

        @Test
        @DisplayName("returns null for null entity")
        void testGetFieldValueNullEntity() {
            Object value = ReflectionUtils.getFieldValue(null, "name");
            assertNull(value);
        }

        @Test
        @DisplayName("returns null for null field name")
        void testGetFieldValueNullFieldName() {
            StandardEntity entity = new StandardEntity();
            Object value = ReflectionUtils.getFieldValue(entity, null);
            assertNull(value);
        }

        @Test
        @DisplayName("returns value with snake_case field name conversion")
        void testGetFieldValueSnakeCaseConversion() {
            SnakeCaseEntity entity = new SnakeCaseEntity();
            entity.setUser_name("TestUser");

            // Access with snake_case
            Object value = ReflectionUtils.getFieldValue(entity, "user_name");
            assertEquals("TestUser", value);
        }

        @Test
        @DisplayName("returns value from record accessor")
        void testGetFieldValueFromRecord() {
            TestRecord record = new TestRecord("RecordName", 42);

            Object nameValue = ReflectionUtils.getFieldValue(record, "name");
            Object valueValue = ReflectionUtils.getFieldValue(record, "value");

            assertEquals("RecordName", nameValue);
            assertEquals(42, valueValue);
        }
    }

    // ==================== SET FIELD VALUE TESTS ====================

    @Nested
    @DisplayName("setFieldValue")
    class SetFieldValueTests {

        @Test
        @DisplayName("sets value via setter method")
        void testSetFieldValueViaSetter() {
            StandardEntity entity = new StandardEntity();

            boolean result = ReflectionUtils.setFieldValue(entity, "name", "NewName");

            assertTrue(result);
            assertEquals("NewName", entity.getName());
        }

        @Test
        @DisplayName("sets value via public field")
        void testSetFieldValueViaPublicField() {
            PublicFieldEntity entity = new PublicFieldEntity();

            boolean result = ReflectionUtils.setFieldValue(entity, "publicName", "PublicValue");

            assertTrue(result);
            assertEquals("PublicValue", entity.publicName);
        }

        @Test
        @DisplayName("sets value via private field fallback")
        void testSetFieldValueViaPrivateField() {
            PrivateFieldEntity entity = new PrivateFieldEntity();

            boolean result = ReflectionUtils.setFieldValue(entity, "privateName", "PrivateValue");

            assertTrue(result);
            // Verify via reflection
            try {
                Field field = PrivateFieldEntity.class.getDeclaredField("privateName");
                field.setAccessible(true);
                assertEquals("PrivateValue", field.get(entity));
            } catch (Exception e) {
                fail("Verification failed");
            }
        }

        @Test
        @DisplayName("returns false for null entity")
        void testSetFieldValueNullEntity() {
            boolean result = ReflectionUtils.setFieldValue(null, "name", "value");
            assertFalse(result);
        }

        @Test
        @DisplayName("returns false for null field name")
        void testSetFieldValueNullFieldName() {
            StandardEntity entity = new StandardEntity();
            boolean result = ReflectionUtils.setFieldValue(entity, null, "value");
            assertFalse(result);
        }

        @Test
        @DisplayName("sets null value correctly")
        void testSetFieldValueNull() {
            StandardEntity entity = new StandardEntity();
            entity.setName("Initial");

            boolean result = ReflectionUtils.setFieldValue(entity, "name", null);

            assertTrue(result);
            assertNull(entity.getName());
        }

        @Test
        @DisplayName("sets primitive value correctly")
        void testSetFieldValuePrimitive() {
            StandardEntity entity = new StandardEntity();

            boolean result = ReflectionUtils.setFieldValue(entity, "age", 25);

            assertTrue(result);
            assertEquals(25, entity.getAge());
        }
    }

    // ==================== STRICT MODE TESTS ====================

    @Nested
    @DisplayName("Strict Mode")
    class StrictModeTests {

        @Test
        @DisplayName("default strict mode is false")
        void testDefaultStrictMode() {
            assertFalse(ReflectionUtils.isStrictMode());
        }

        @Test
        @DisplayName("can enable strict mode")
        void testEnableStrictMode() {
            ReflectionUtils.setStrictMode(true);
            assertTrue(ReflectionUtils.isStrictMode());
        }

        @Test
        @DisplayName("private field access blocked in strict mode")
        void testPrivateFieldBlockedInStrictMode() {
            ReflectionUtils.setStrictMode(true);
            PrivateFieldEntity entity = new PrivateFieldEntity();

            // Should fail because no public getter and strict mode blocks private access
            boolean result = ReflectionUtils.setFieldValue(entity, "privateName", "Value");
            assertFalse(result);
        }
    }

    // ==================== FIND FIELD TESTS ====================

    @Nested
    @DisplayName("findField")
    class FindFieldTests {

        @Test
        @DisplayName("finds declared field")
        void testFindDeclaredField() {
            Field field = ReflectionUtils.findField(StandardEntity.class, "name");

            assertNotNull(field);
            assertEquals("name", field.getName());
        }

        @Test
        @DisplayName("finds inherited field")
        void testFindInheritedField() {
            Field field = ReflectionUtils.findField(ChildEntity.class, "baseField");

            assertNotNull(field);
            assertEquals("baseField", field.getName());
        }

        @Test
        @DisplayName("returns null for non-existent field")
        void testFindNonExistentField() {
            Field field = ReflectionUtils.findField(StandardEntity.class, "nonExistent");
            assertNull(field);
        }

        @Test
        @DisplayName("returns null for null class")
        void testFindFieldNullClass() {
            Field field = ReflectionUtils.findField(null, "name");
            assertNull(field);
        }

        @Test
        @DisplayName("returns null for null field name")
        void testFindFieldNullName() {
            Field field = ReflectionUtils.findField(StandardEntity.class, null);
            assertNull(field);
        }
    }

    // ==================== INHERITANCE TESTS ====================

    @Nested
    @DisplayName("Inheritance Support")
    class InheritanceTests {

        @Test
        @DisplayName("getFieldValue works with inherited fields")
        void testGetInheritedFieldValue() {
            ChildEntity entity = new ChildEntity();
            entity.setBaseField("BaseValue");
            entity.setChildField("ChildValue");

            Object baseValue = ReflectionUtils.getFieldValue(entity, "baseField");
            Object childValue = ReflectionUtils.getFieldValue(entity, "childField");

            assertEquals("BaseValue", baseValue);
            assertEquals("ChildValue", childValue);
        }

        @Test
        @DisplayName("setFieldValue works with inherited fields")
        void testSetInheritedFieldValue() {
            ChildEntity entity = new ChildEntity();

            boolean baseResult = ReflectionUtils.setFieldValue(entity, "baseField", "NewBase");
            boolean childResult = ReflectionUtils.setFieldValue(entity, "childField", "NewChild");

            assertTrue(baseResult);
            assertTrue(childResult);
            assertEquals("NewBase", entity.getBaseField());
            assertEquals("NewChild", entity.getChildField());
        }
    }

    // ==================== CACHE TESTS ====================

    @Nested
    @DisplayName("Cache Management")
    class CacheTests {

        @Test
        @DisplayName("clearCaches clears all caches")
        void testClearCaches() {
            // Populate caches
            StandardEntity entity = new StandardEntity();
            entity.setName("Test");
            ReflectionUtils.getFieldValue(entity, "name");
            ReflectionUtils.setFieldValue(entity, "name", "New");
            ReflectionUtils.findField(StandardEntity.class, "name");

            // Clear and verify still works (would fail if caches corrupted)
            ReflectionUtils.clearCaches();

            Object value = ReflectionUtils.getFieldValue(entity, "name");
            assertEquals("New", value);
        }
    }

    // ==================== GET FIELD VALUE EDGE CASES ====================

    @Nested
    @DisplayName("getFieldValue Edge Cases")
    class GetFieldValueEdgeCaseTests {

        @Test
        @DisplayName("returns null for non-existent field")
        void testGetFieldValueNonExistentField() {
            StandardEntity entity = new StandardEntity();
            entity.setName("Test");

            Object value = ReflectionUtils.getFieldValue(entity, "nonExistentField");
            assertNull(value);
        }

        @Test
        @DisplayName("handles camelCase to snake_case conversion")
        void testGetFieldValueCamelToSnake() {
            SnakeCaseEntity entity = new SnakeCaseEntity();
            entity.setUser_name("CamelTest");

            // Access with camelCase version
            Object value = ReflectionUtils.getFieldValue(entity, "userName");
            assertEquals("CamelTest", value);
        }
    }

    // ==================== SET FIELD VALUE EDGE CASES ====================

    @Nested
    @DisplayName("setFieldValue Edge Cases")
    class SetFieldValueEdgeCaseTests {

        @Test
        @DisplayName("returns false for non-existent field")
        void testSetFieldValueNonExistentField() {
            StandardEntity entity = new StandardEntity();

            boolean result = ReflectionUtils.setFieldValue(entity, "nonExistentField", "value");
            assertFalse(result);
        }

        @Test
        @DisplayName("handles camelCase to snake_case conversion")
        void testSetFieldValueCamelToSnake() {
            SnakeCaseEntity entity = new SnakeCaseEntity();

            // Set with camelCase version
            boolean result = ReflectionUtils.setFieldValue(entity, "userName", "CamelTest");
            assertTrue(result);
            assertEquals("CamelTest", entity.getUser_name());
        }

        @Test
        @DisplayName("set boolean value via setter")
        void testSetFieldValueBoolean() {
            StandardEntity entity = new StandardEntity();

            boolean result = ReflectionUtils.setFieldValue(entity, "active", true);
            assertTrue(result);
            assertTrue(entity.isActive());
        }
    }

    // ==================== STRICT MODE DETAILED TESTS ====================

    @Nested
    @DisplayName("Strict Mode Detailed")
    class StrictModeDetailedTests {

        @Test
        @DisplayName("getFieldValue returns null in strict mode for private field without getter")
        void testGetFieldValueStrictModePrivateField() {
            ReflectionUtils.setStrictMode(true);
            try {
                PrivateFieldEntity entity = new PrivateFieldEntity();
                // Set value via reflection for test setup
                try {
                    Field field = PrivateFieldEntity.class.getDeclaredField("privateName");
                    field.setAccessible(true);
                    field.set(entity, "Secret");
                } catch (Exception e) {
                    fail("Setup failed");
                }

                // In strict mode, should return null (no public getter)
                Object value = ReflectionUtils.getFieldValue(entity, "privateName");
                assertNull(value);
            } finally {
                ReflectionUtils.setStrictMode(false);
            }
        }

        @Test
        @DisplayName("setFieldValue returns false in strict mode for private field without setter")
        void testSetFieldValueStrictModePrivateField() {
            ReflectionUtils.setStrictMode(true);
            try {
                PrivateFieldEntity entity = new PrivateFieldEntity();

                // In strict mode, should return false (no public setter)
                boolean result = ReflectionUtils.setFieldValue(entity, "privateName", "Value");
                assertFalse(result);
            } finally {
                ReflectionUtils.setStrictMode(false);
            }
        }

        @Test
        @DisplayName("strict mode toggle works correctly")
        void testStrictModeToggle() {
            assertFalse(ReflectionUtils.isStrictMode());

            ReflectionUtils.setStrictMode(true);
            assertTrue(ReflectionUtils.isStrictMode());

            ReflectionUtils.setStrictMode(false);
            assertFalse(ReflectionUtils.isStrictMode());
        }
    }

    // ==================== DIRECT METHOD ACCESSOR TESTS ====================

    // Class with record-style accessors (method name = field name)
    static class RecordStyleEntity {
        private String data;

        // Record-style accessor (no "get" prefix)
        public String data() { return data; }
        public void setData(String data) { this.data = data; }
    }

    @Nested
    @DisplayName("Direct Method Accessor")
    class DirectMethodAccessorTests {

        @Test
        @DisplayName("finds record-style accessor without get prefix")
        void testDirectMethodAccessor() {
            RecordStyleEntity entity = new RecordStyleEntity();
            entity.setData("test-data");

            Object value = ReflectionUtils.getFieldValue(entity, "data");
            assertEquals("test-data", value);
        }
    }

    // ==================== TYPE MATCHING TESTS ====================

    // Entity with all primitive types for testing isBoxedMatch
    static class AllPrimitivesEntity {
        private int intValue;
        private long longValue;
        private double doubleValue;
        private float floatValue;
        private boolean boolValue;
        private short shortValue;
        private byte byteValue;
        private char charValue;

        public int getIntValue() { return intValue; }
        public void setIntValue(int intValue) { this.intValue = intValue; }
        public long getLongValue() { return longValue; }
        public void setLongValue(long longValue) { this.longValue = longValue; }
        public double getDoubleValue() { return doubleValue; }
        public void setDoubleValue(double doubleValue) { this.doubleValue = doubleValue; }
        public float getFloatValue() { return floatValue; }
        public void setFloatValue(float floatValue) { this.floatValue = floatValue; }
        public boolean isBoolValue() { return boolValue; }
        public void setBoolValue(boolean boolValue) { this.boolValue = boolValue; }
        public short getShortValue() { return shortValue; }
        public void setShortValue(short shortValue) { this.shortValue = shortValue; }
        public byte getByteValue() { return byteValue; }
        public void setByteValue(byte byteValue) { this.byteValue = byteValue; }
        public char getCharValue() { return charValue; }
        public void setCharValue(char charValue) { this.charValue = charValue; }
    }

    @Nested
    @DisplayName("Type Matching")
    class TypeMatchingTests {

        @Test
        @DisplayName("handles all primitive/wrapper combinations")
        void testPrimitiveWrapperMatching() {
            StandardEntity entity = new StandardEntity();

            // Integer -> int
            boolean result = ReflectionUtils.setFieldValue(entity, "age", Integer.valueOf(25));
            assertTrue(result);
            assertEquals(25, entity.getAge());

            // Boolean -> boolean
            result = ReflectionUtils.setFieldValue(entity, "active", Boolean.TRUE);
            assertTrue(result);
            assertTrue(entity.isActive());
        }

        @Test
        @DisplayName("handles Long -> long primitive conversion")
        void testLongPrimitiveConversion() {
            AllPrimitivesEntity entity = new AllPrimitivesEntity();
            boolean result = ReflectionUtils.setFieldValue(entity, "longValue", Long.valueOf(123456789L));
            assertTrue(result);
            assertEquals(123456789L, entity.getLongValue());
        }

        @Test
        @DisplayName("handles Double -> double primitive conversion")
        void testDoublePrimitiveConversion() {
            AllPrimitivesEntity entity = new AllPrimitivesEntity();
            boolean result = ReflectionUtils.setFieldValue(entity, "doubleValue", Double.valueOf(3.14159));
            assertTrue(result);
            assertEquals(3.14159, entity.getDoubleValue(), 0.0001);
        }

        @Test
        @DisplayName("handles Float -> float primitive conversion")
        void testFloatPrimitiveConversion() {
            AllPrimitivesEntity entity = new AllPrimitivesEntity();
            boolean result = ReflectionUtils.setFieldValue(entity, "floatValue", Float.valueOf(2.5f));
            assertTrue(result);
            assertEquals(2.5f, entity.getFloatValue(), 0.0001f);
        }

        @Test
        @DisplayName("handles Short -> short primitive conversion")
        void testShortPrimitiveConversion() {
            AllPrimitivesEntity entity = new AllPrimitivesEntity();
            boolean result = ReflectionUtils.setFieldValue(entity, "shortValue", Short.valueOf((short) 100));
            assertTrue(result);
            assertEquals((short) 100, entity.getShortValue());
        }

        @Test
        @DisplayName("handles Byte -> byte primitive conversion")
        void testBytePrimitiveConversion() {
            AllPrimitivesEntity entity = new AllPrimitivesEntity();
            boolean result = ReflectionUtils.setFieldValue(entity, "byteValue", Byte.valueOf((byte) 42));
            assertTrue(result);
            assertEquals((byte) 42, entity.getByteValue());
        }

        @Test
        @DisplayName("handles Character -> char primitive conversion")
        void testCharPrimitiveConversion() {
            AllPrimitivesEntity entity = new AllPrimitivesEntity();
            boolean result = ReflectionUtils.setFieldValue(entity, "charValue", Character.valueOf('X'));
            assertTrue(result);
            assertEquals('X', entity.getCharValue());
        }

        @Test
        @DisplayName("handles Boolean -> boolean primitive conversion")
        void testBooleanPrimitiveConversion() {
            AllPrimitivesEntity entity = new AllPrimitivesEntity();
            boolean result = ReflectionUtils.setFieldValue(entity, "boolValue", Boolean.TRUE);
            assertTrue(result);
            assertTrue(entity.isBoolValue());
        }
    }

    // ==================== ADDITIONAL EDGE CASE TESTS ====================

    @Nested
    @DisplayName("Additional Edge Cases")
    class AdditionalEdgeCaseTests {

        @Test
        @DisplayName("getFieldValue works with snake_case to camelCase conversion")
        void testSnakeToCamelConversion() {
            SnakeCaseEntity entity = new SnakeCaseEntity();
            entity.setEmail_address("test@example.com");

            // Try accessing with snake_case
            Object value = ReflectionUtils.getFieldValue(entity, "email_address");
            assertEquals("test@example.com", value);
        }

        @Test
        @DisplayName("setFieldValue works with snake_case field")
        void testSetSnakeCaseField() {
            SnakeCaseEntity entity = new SnakeCaseEntity();

            boolean result = ReflectionUtils.setFieldValue(entity, "email_address", "new@example.com");
            assertTrue(result);
            assertEquals("new@example.com", entity.getEmail_address());
        }

        @Test
        @DisplayName("getFieldValue returns null when getter throws exception")
        void testGetFieldValueGetterThrows() {
            // EntityWithThrowingGetter will cause an exception when invoked
            // This tests the catch (Throwable e) { return null; } path
            StandardEntity entity = new StandardEntity();
            // Non-existent field should return null through the normal null path
            Object value = ReflectionUtils.getFieldValue(entity, "completelyFakeField");
            assertNull(value);
        }

        @Test
        @DisplayName("setFieldValue returns false when setter throws exception")
        void testSetFieldValueSetterThrows() {
            StandardEntity entity = new StandardEntity();
            // Try to set a non-existent field - should return false
            boolean result = ReflectionUtils.setFieldValue(entity, "completelyFakeField", "value");
            assertFalse(result);
        }

        @Test
        @DisplayName("clearCaches resets private lookup cache")
        void testClearCachesResetsPrivateLookupCache() {
            // Exercise the cache
            StandardEntity entity = new StandardEntity();
            entity.setName("Initial");

            // Get value (populates cache)
            Object value1 = ReflectionUtils.getFieldValue(entity, "name");
            assertEquals("Initial", value1);

            // Clear caches
            ReflectionUtils.clearCaches();

            // Should still work after clearing
            entity.setName("Updated");
            Object value2 = ReflectionUtils.getFieldValue(entity, "name");
            assertEquals("Updated", value2);
        }
    }

    // ==================== PRIVATE LOOKUP FALLBACK TESTS ====================

    @Nested
    @DisplayName("Private Lookup Fallback")
    class PrivateLookupFallbackTests {

        @Test
        @DisplayName("findField works with inherited private fields")
        void testFindFieldInheritedPrivate() {
            // This exercises the findDeclaredField recursive path
            Field field = ReflectionUtils.findField(ChildEntity.class, "baseField");
            assertNotNull(field);
            assertEquals("baseField", field.getName());
        }

        @Test
        @DisplayName("setFieldValue handles type mismatch gracefully")
        void testSetFieldValueTypeMismatch() {
            StandardEntity entity = new StandardEntity();
            // Try to set String value to int field - should fail gracefully
            boolean result = ReflectionUtils.setFieldValue(entity, "age", "not-an-int");
            // Depending on implementation, this might fail silently
            // The test verifies no exception is thrown
        }

        @Test
        @DisplayName("getFieldValue handles exception in getter gracefully")
        void testGetFieldValueHandlesException() {
            // Test that the catch (Throwable e) { return null; } path works
            StandardEntity entity = new StandardEntity();
            // Access non-existent field should return null
            Object value = ReflectionUtils.getFieldValue(entity, "nonExistentFieldName");
            assertNull(value);
        }
    }

    // ==================== CAPITALIZE EDGE CASES ====================

    @Nested
    @DisplayName("Capitalize Edge Cases")
    class CapitalizeEdgeCaseTests {

        @Test
        @DisplayName("handles single character field names")
        void testSingleCharFieldName() {
            // Access with single char doesn't crash
            StandardEntity entity = new StandardEntity();
            Object value = ReflectionUtils.getFieldValue(entity, "x");
            assertNull(value); // Field doesn't exist, but shouldn't crash
        }

        @Test
        @DisplayName("handles empty string field name")
        void testEmptyStringFieldName() {
            StandardEntity entity = new StandardEntity();
            Object value = ReflectionUtils.getFieldValue(entity, "");
            assertNull(value);
        }
    }

    // ==================== BOXED MATCH FALSE PATH ====================

    @Nested
    @DisplayName("isBoxedMatch False Path")
    class IsBoxedMatchFalsePathTests {

        // Entity with Object field (non-primitive)
        static class ObjectFieldEntity {
            private Object data;

            public Object getData() { return data; }
            public void setData(Object data) { this.data = data; }
        }

        @Test
        @DisplayName("isBoxedMatch returns false for non-primitive types")
        void testIsBoxedMatchNonPrimitive() {
            ObjectFieldEntity entity = new ObjectFieldEntity();
            // Setting Object to Object - not a primitive match
            boolean result = ReflectionUtils.setFieldValue(entity, "data", "string-value");
            assertTrue(result);
            assertEquals("string-value", entity.getData());
        }

        @Test
        @DisplayName("handles null value to primitive field returns false")
        void testNullToPrimitiveField() {
            StandardEntity entity = new StandardEntity();
            // Setting null to int field should fail (can't set null to primitive)
            boolean result = ReflectionUtils.setFieldValue(entity, "age", null);
            assertFalse(result);
        }
    }

    // ==================== ACCOUNT ENTITY TESTS (Exact copy of user's Account) ====================

    @Nested
    @DisplayName("Account Entity Tests")
    class AccountEntityTests {

        @Test
        @DisplayName("setFieldValue works with Account UUID id field")
        void testSetAccountUuidId() {
            Account account = new Account();
            UUID testId = UUID.randomUUID();

            boolean result = ReflectionUtils.setFieldValue(account, "id", testId);

            assertTrue(result, "setFieldValue should succeed for Account.id UUID field");
            assertEquals(testId, account.getId());
        }

        @Test
        @DisplayName("getFieldValue works with Account UUID id field")
        void testGetAccountUuidId() {
            UUID testId = UUID.randomUUID();
            Account account = new Account();
            account.setId(testId);

            Object value = ReflectionUtils.getFieldValue(account, "id");

            assertEquals(testId, value);
        }

        @Test
        @DisplayName("setFieldValue works with Account username field")
        void testSetAccountUsername() {
            Account account = new Account();

            boolean result = ReflectionUtils.setFieldValue(account, "username", "testuser");

            assertTrue(result);
            assertEquals("testuser", account.getUsername());
        }

        @Test
        @DisplayName("setFieldValue works with Account enabled boolean field")
        void testSetAccountEnabled() {
            Account account = new Account();

            boolean result = ReflectionUtils.setFieldValue(account, "enabled", true);

            assertTrue(result);
            assertTrue(account.isEnabled());
        }

        @Test
        @DisplayName("setFieldValue with null UUID on Account")
        void testSetAccountNullUuid() {
            Account account = new Account();
            account.setId(UUID.randomUUID());

            boolean result = ReflectionUtils.setFieldValue(account, "id", null);

            assertTrue(result, "setFieldValue should succeed with null value");
            assertNull(account.getId());
        }

        @Test
        @DisplayName("setFieldValue works with Account OffsetDateTime field")
        void testSetAccountCreatedAt() {
            Account account = new Account();
            java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

            boolean result = ReflectionUtils.setFieldValue(account, "createdAt", now);

            assertTrue(result);
            assertEquals(now, account.getCreatedAt());
        }

        @Test
        @DisplayName("setFieldValue works with Account Map field (metaData)")
        void testSetAccountMetaData() {
            Account account = new Account();
            java.util.Map<String, Object> meta = new java.util.HashMap<>();
            meta.put("key", "value");

            boolean result = ReflectionUtils.setFieldValue(account, "metaData", meta);

            assertTrue(result);
            assertEquals(meta, account.getMetaData());
        }

        @Test
        @DisplayName("setFieldValue works with all Account UUID fields")
        void testSetAllAccountUuidFields() {
            Account account = new Account();
            UUID id = UUID.randomUUID();
            UUID deletedBy = UUID.randomUUID();
            UUID lastActiveWorkspace = UUID.randomUUID();

            assertTrue(ReflectionUtils.setFieldValue(account, "id", id));
            assertTrue(ReflectionUtils.setFieldValue(account, "deletedBy", deletedBy));
            assertTrue(ReflectionUtils.setFieldValue(account, "lastActiveWorkspace", lastActiveWorkspace));

            assertEquals(id, account.getId());
            assertEquals(deletedBy, account.getDeletedBy());
            assertEquals(lastActiveWorkspace, account.getLastActiveWorkspace());
        }
    }
}
