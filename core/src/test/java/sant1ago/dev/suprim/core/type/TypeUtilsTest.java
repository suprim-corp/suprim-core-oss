package sant1ago.dev.suprim.core.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TypeUtils")
class TypeUtilsTest {

    // ==================== TEST FIXTURES ====================

    static class TestEntity {
        Long id;
        String name;
    }

    static final Table<TestEntity> TEST_TABLE = Table.of("test_entity", TestEntity.class);
    static final Column<TestEntity, String> NAME_COL = new Column<>(TEST_TABLE, "name", String.class, "VARCHAR(255)");
    static final Column<TestEntity, Long> ID_COL = new Column<>(TEST_TABLE, "id", Long.class, "BIGINT");

    // ==================== listClass() ====================

    @Nested
    @DisplayName("listClass()")
    class ListClassTests {

        @Test
        @DisplayName("returns Class assignable to List")
        void returnsListClass() {
            Class<List<String>> result = TypeUtils.listClass();
            assertNotNull(result);
            assertEquals(List.class, result);
        }

        @Test
        @DisplayName("works with different generic types")
        void worksWithDifferentTypes() {
            Class<List<Integer>> intList = TypeUtils.listClass();
            Class<List<String>> strList = TypeUtils.listClass();
            Class<List<UUID>> uuidList = TypeUtils.listClass();

            // All return same raw class
            assertEquals(intList, strList);
            assertEquals(strList, uuidList);
            assertEquals(List.class, intList);
        }

        @Test
        @DisplayName("can be used to check instanceof")
        void canCheckInstanceof() {
            Class<List<String>> listClass = TypeUtils.listClass();
            assertTrue(listClass.isInstance(List.of("a", "b")));
            assertTrue(listClass.isInstance(List.of(1, 2, 3)));
            assertFalse(listClass.isInstance("not a list"));
        }
    }

    // ==================== objectClass() ====================

    @Nested
    @DisplayName("objectClass()")
    class ObjectClassTests {

        @Test
        @DisplayName("returns Object.class")
        void returnsObjectClass() {
            Class<Object> result = TypeUtils.objectClass();
            assertEquals(Object.class, result);
        }

        @Test
        @DisplayName("works with any generic type")
        void worksWithAnyType() {
            Class<String> strClass = TypeUtils.objectClass();
            Class<Integer> intClass = TypeUtils.objectClass();

            assertEquals(Object.class, strClass);
            assertEquals(Object.class, intClass);
        }
    }

    // ==================== classOf() ====================

    @Nested
    @DisplayName("classOf()")
    class ClassOfTests {

        @Test
        @DisplayName("returns correct class for String")
        void returnsStringClass() {
            Class<String> result = TypeUtils.classOf("hello");
            assertEquals(String.class, result);
        }

        @Test
        @DisplayName("returns correct class for Integer")
        void returnsIntegerClass() {
            Class<Integer> result = TypeUtils.classOf(42);
            assertEquals(Integer.class, result);
        }

        @Test
        @DisplayName("returns correct class for Long")
        void returnsLongClass() {
            Class<Long> result = TypeUtils.classOf(100L);
            assertEquals(Long.class, result);
        }

        @Test
        @DisplayName("returns correct class for BigDecimal")
        void returnsBigDecimalClass() {
            Class<BigDecimal> result = TypeUtils.classOf(new BigDecimal("99.99"));
            assertEquals(BigDecimal.class, result);
        }

        @Test
        @DisplayName("returns correct class for UUID")
        void returnsUUIDClass() {
            Class<UUID> result = TypeUtils.classOf(UUID.randomUUID());
            assertEquals(UUID.class, result);
        }

        @Test
        @DisplayName("returns correct class for LocalDateTime")
        void returnsLocalDateTimeClass() {
            Class<LocalDateTime> result = TypeUtils.classOf(LocalDateTime.now());
            assertEquals(LocalDateTime.class, result);
        }

        @Test
        @DisplayName("returns correct class for custom entity")
        void returnsCustomEntityClass() {
            TestEntity entity = new TestEntity();
            Class<TestEntity> result = TypeUtils.classOf(entity);
            assertEquals(TestEntity.class, result);
        }
    }

    // ==================== valueTypeOf() ====================

    @Nested
    @DisplayName("valueTypeOf()")
    class ValueTypeOfTests {

        @Test
        @DisplayName("returns value type from Column")
        void returnsColumnValueType() {
            Class<String> result = TypeUtils.valueTypeOf(NAME_COL);
            assertEquals(String.class, result);
        }

        @Test
        @DisplayName("returns Long type from Column")
        void returnsLongColumnType() {
            Class<Long> result = TypeUtils.valueTypeOf(ID_COL);
            assertEquals(Long.class, result);
        }

        @Test
        @DisplayName("returns value type from Literal")
        void returnsLiteralValueType() {
            Literal<String> literal = new Literal<>("test", String.class);
            Class<String> result = TypeUtils.valueTypeOf(literal);
            assertEquals(String.class, result);
        }
    }

    // ==================== castExpression() ====================

    @Nested
    @DisplayName("castExpression()")
    class CastExpressionTests {

        @Test
        @DisplayName("casts Column to typed Expression")
        void castsColumnToExpression() {
            Expression<?> wildcard = NAME_COL;
            Expression<String> typed = TypeUtils.castExpression(wildcard);

            assertNotNull(typed);
            assertSame(wildcard, typed);
            assertEquals(String.class, typed.getValueType());
        }

        @Test
        @DisplayName("casts Literal to typed Expression")
        void castsLiteralToExpression() {
            Expression<?> wildcard = new Literal<>(42, Integer.class);
            Expression<Integer> typed = TypeUtils.castExpression(wildcard);

            assertNotNull(typed);
            assertSame(wildcard, typed);
        }

        @Test
        @DisplayName("preserves runtime type")
        void preservesRuntimeType() {
            Expression<?> original = NAME_COL;
            Expression<String> casted = TypeUtils.castExpression(original);

            assertTrue(casted instanceof Column);
            assertEquals("name", ((Column<?, ?>) casted).getName());
        }
    }

    // ==================== castRelation() ====================

    @Nested
    @DisplayName("castRelation()")
    class CastRelationTests {

        @Test
        @DisplayName("casts wildcard Relation")
        void castsWildcardRelation() {
            Relation<?, ?> wildcard = Relation.hasMany(TEST_TABLE, TEST_TABLE, "id", "parent_id", false, false);
            Relation<TestEntity, TestEntity> typed = TypeUtils.castRelation(wildcard);

            assertNotNull(typed);
            assertSame(wildcard, typed);
        }
    }

    // ==================== castTable() ====================

    @Nested
    @DisplayName("castTable()")
    class CastTableTests {

        @Test
        @DisplayName("casts wildcard Table")
        void castsWildcardTable() {
            Table<?> wildcard = TEST_TABLE;
            Table<TestEntity> typed = TypeUtils.castTable(wildcard);

            assertNotNull(typed);
            assertSame(wildcard, typed);
            assertEquals("test_entity", typed.getName());
        }

        @Test
        @DisplayName("preserves table properties")
        void preservesTableProperties() {
            Table<?> wildcard = TEST_TABLE;
            Table<Object> casted = TypeUtils.castTable(wildcard);

            assertEquals(TEST_TABLE.getName(), casted.getName());
            assertEquals(TEST_TABLE.getEntityType(), casted.getEntityType());
        }
    }

    // ==================== cast() ====================

    @Nested
    @DisplayName("cast()")
    class CastTests {

        @Test
        @DisplayName("casts Object to String")
        void castsToString() {
            Object obj = "hello";
            String result = TypeUtils.cast(obj);

            assertEquals("hello", result);
            assertSame(obj, result);
        }

        @Test
        @DisplayName("casts Object to Integer")
        void castsToInteger() {
            Object obj = 42;
            Integer result = TypeUtils.cast(obj);

            assertEquals(42, result);
        }

        @Test
        @DisplayName("casts Object to custom type")
        void castsToCustomType() {
            TestEntity entity = new TestEntity();
            entity.name = "test";

            Object obj = entity;
            TestEntity result = TypeUtils.cast(obj);

            assertSame(entity, result);
            assertEquals("test", result.name);
        }

        @Test
        @DisplayName("invalid cast throws ClassCastException")
        void throwsOnInvalidCast() {
            Object obj = "not a number";

            // ClassCastException may occur at cast or usage depending on JVM
            assertThrows(ClassCastException.class, () -> {
                Integer result = TypeUtils.cast(obj);
                result.intValue(); // Force usage
            });
        }

        @Test
        @DisplayName("handles null")
        void handlesNull() {
            String result = TypeUtils.cast(null);
            assertNull(result);
        }
    }

    // ==================== INTEGRATION TESTS ====================

    @Nested
    @DisplayName("Integration")
    class IntegrationTests {

        @Test
        @DisplayName("ArrayColumn constructor uses listClass() - ArrayColumn.java:26")
        void arrayColumnConstructorUsesListClass() {
            // Mirrors: super(table, name, TypeUtils.listClass(), sqlType)
            ArrayColumn<TestEntity, String> arrayCol =
                new ArrayColumn<>(TEST_TABLE, "tags", String.class, "TEXT[]");

            // Must return List.class for Column's valueType
            assertEquals(List.class, arrayCol.getValueType());

            // Must be usable in predicates
            Predicate contains = arrayCol.contains("tag1");
            assertNotNull(contains);
        }

        @Test
        @DisplayName("ArrayLiteral.getValueType uses listClass() - ArrayColumn.java:107")
        void arrayLiteralGetValueTypeUsesListClass() {
            // Mirrors: return TypeUtils.listClass()
            ArrayColumn.ArrayLiteral<Integer> literal =
                new ArrayColumn.ArrayLiteral<>(List.of(1, 2, 3), Integer.class);

            Class<List<Integer>> valueType = literal.getValueType();
            assertEquals(List.class, valueType);

            // Must work with instanceof checks
            assertTrue(valueType.isInstance(List.of("a", "b")));
        }

        @Test
        @DisplayName("Coalesce empty expressions uses objectClass() - Coalesce.java:37")
        void coalesceEmptyUsesObjectClass() {
            // Mirrors: expressions.isEmpty() ? TypeUtils.objectClass() : ...
            // When no expressions, fallback to Object.class
            Class<Object> objClass = TypeUtils.objectClass();
            assertEquals(Object.class, objClass);

            // Can be assigned to any Class<V>
            Class<String> strClass = TypeUtils.objectClass();
            Class<Integer> intClass = TypeUtils.objectClass();
            assertEquals(Object.class, strClass);
            assertEquals(Object.class, intClass);
        }

        @Test
        @DisplayName("Coalesce uses valueTypeOf for first expression - Coalesce.java:38")
        void coalesceUsesValueTypeOfForFirstExpression() {
            // Mirrors: TypeUtils.valueTypeOf(expressions.get(0))
            Expression<?> wildcardExpr = NAME_COL;
            Class<String> valueType = TypeUtils.valueTypeOf(wildcardExpr);

            assertEquals(String.class, valueType);

            // Used to set Coalesce's valueType field
            Coalesce<String> coalesce = Coalesce.of(NAME_COL, "default");
            assertEquals(String.class, coalesce.getValueType());
        }

        @Test
        @DisplayName("Coalesce.of uses classOf for fallback - Coalesce.java:69")
        void coalesceOfUsesClassOfForFallback() {
            // Mirrors: Class<V> fallbackType = TypeUtils.classOf(fallback)
            String fallback = "Anonymous";
            Class<String> fallbackType = TypeUtils.classOf(fallback);

            // Used to create Literal with correct type
            Literal<String> literal = new Literal<>(fallback, fallbackType);
            assertEquals(String.class, literal.getValueType());
            assertEquals("Anonymous", literal.value());
        }

        @Test
        @DisplayName("Coalesce.or uses classOf for chained fallback - Coalesce.java:93")
        void coalesceOrUsesClassOfForChainedFallback() {
            // Mirrors: Class<V> fallbackType = TypeUtils.classOf(fallback)
            Coalesce<String> coalesce = Coalesce.of(NAME_COL, "first")
                .or("second")
                .or("third");

            assertNotNull(coalesce);
            assertEquals(String.class, coalesce.getValueType());
        }

        @Test
        @DisplayName("Relation.morphTo uses castTable - Relation.java:482")
        void relationMorphToUsesCastTable() {
            // Mirrors: Table<Object> relatedTable = TypeUtils.castTable(ownerTable)
            Table<?> wildcardTable = TEST_TABLE;
            Table<Object> castedTable = TypeUtils.castTable(wildcardTable);

            // Must preserve table properties
            assertEquals(TEST_TABLE.getName(), castedTable.getName());
            assertEquals(TEST_TABLE.getEntityType(), castedTable.getEntityType());

            // Must be usable for creating relations
            assertNotNull(castedTable);
        }

        @Test
        @DisplayName("EagerLoader uses castRelation - EagerLoader.java:51")
        void eagerLoaderUsesCastRelation() {
            // Mirrors: Relation<T, R> relation = TypeUtils.castRelation(spec.relation())
            Relation<?, ?> wildcardRelation = Relation.hasMany(
                TEST_TABLE, TEST_TABLE, "id", "parent_id", false, false);

            Relation<TestEntity, TestEntity> typedRelation = TypeUtils.castRelation(wildcardRelation);

            // Must preserve relation properties
            assertNotNull(typedRelation);
            assertSame(wildcardRelation, typedRelation);
        }

        @Test
        @DisplayName("EntityMapper uses cast for cache - EntityMapper.java:92")
        void entityMapperUsesCastForCache() {
            // Mirrors: return TypeUtils.cast(METADATA_CACHE.computeIfAbsent(...))
            Object cachedValue = new TestEntity();
            TestEntity typed = TypeUtils.cast(cachedValue);

            assertNotNull(typed);
            assertSame(cachedValue, typed);
        }
    }

    // ==================== EDGE CASES & SAFETY TESTS ====================

    @Nested
    @DisplayName("Edge Cases & Safety")
    class EdgeCasesTests {

        @Test
        @DisplayName("classOf with subclass returns actual runtime class")
        void classOfWithSubclass() {
            class SubEntity extends TestEntity {}
            TestEntity entity = new SubEntity();

            Class<TestEntity> clazz = TypeUtils.classOf(entity);

            // Returns actual runtime class, not declared type
            assertEquals(SubEntity.class, clazz);
        }

        @Test
        @DisplayName("listClass result can check any List instance")
        void listClassCanCheckAnyList() {
            Class<List<String>> listClass = TypeUtils.listClass();

            // Due to type erasure, can check any List
            assertTrue(listClass.isInstance(List.of(1, 2, 3)));
            assertTrue(listClass.isInstance(List.of("a", "b")));
            assertTrue(listClass.isInstance(List.of()));
            assertFalse(listClass.isInstance("not a list"));
            assertFalse(listClass.isInstance(null));
        }

        @Test
        @DisplayName("cast preserves object identity")
        void castPreservesIdentity() {
            TestEntity original = new TestEntity();
            original.name = "test";

            Object asObject = original;
            TestEntity back = TypeUtils.cast(asObject);

            assertSame(original, back);
            assertEquals("test", back.name);
        }

        @Test
        @DisplayName("valueTypeOf works with all Expression implementations")
        void valueTypeOfWorksWithAllExpressions() {
            // Column
            assertEquals(String.class, TypeUtils.valueTypeOf(NAME_COL));
            assertEquals(Long.class, TypeUtils.valueTypeOf(ID_COL));

            // Literal
            assertEquals(Integer.class, TypeUtils.valueTypeOf(new Literal<>(1, Integer.class)));
            assertEquals(String.class, TypeUtils.valueTypeOf(new Literal<>("x", String.class)));

            // ListLiteral
            assertEquals(Long.class, TypeUtils.valueTypeOf(new ListLiteral<>(List.of(1L), Long.class)));

            // ArrayLiteral
            ArrayColumn.ArrayLiteral<String> arrLit = new ArrayColumn.ArrayLiteral<>(List.of("a"), String.class);
            assertEquals(List.class, TypeUtils.valueTypeOf(arrLit));
        }

        @Test
        @DisplayName("multiple casts of same object return same reference")
        void multipleCastsSameReference() {
            Table<?> original = TEST_TABLE;

            Table<TestEntity> cast1 = TypeUtils.castTable(original);
            Table<Object> cast2 = TypeUtils.castTable(original);
            Table<?> cast3 = TypeUtils.castTable(original);

            assertSame(original, cast1);
            assertSame(original, cast2);
            assertSame(original, cast3);
        }

        @Test
        @DisplayName("castExpression works with all Expression types")
        void castExpressionWorksWithAllTypes() {
            // Column
            Expression<?> col = NAME_COL;
            Expression<String> typedCol = TypeUtils.castExpression(col);
            assertSame(col, typedCol);

            // Literal
            Expression<?> lit = new Literal<>(42, Integer.class);
            Expression<Integer> typedLit = TypeUtils.castExpression(lit);
            assertSame(lit, typedLit);

            // Coalesce
            Expression<?> coal = Coalesce.of(NAME_COL, "default");
            Expression<String> typedCoal = TypeUtils.castExpression(coal);
            assertSame(coal, typedCoal);
        }

        @Test
        @DisplayName("methods are null-safe where applicable")
        void nullSafety() {
            // cast handles null
            assertNull(TypeUtils.cast(null));

            // castExpression handles null
            assertNull(TypeUtils.castExpression(null));

            // castRelation handles null
            assertNull(TypeUtils.castRelation(null));

            // castTable handles null
            assertNull(TypeUtils.castTable(null));
        }
    }

    // ==================== TYPE INFERENCE TESTS ====================

    @Nested
    @DisplayName("Type Inference")
    class TypeInferenceTests {

        @Test
        @DisplayName("listClass infers correct generic type")
        void listClassInfersType() {
            // Compiler should infer correct types
            Class<List<String>> strListClass = TypeUtils.listClass();
            Class<List<Integer>> intListClass = TypeUtils.listClass();
            Class<List<TestEntity>> entityListClass = TypeUtils.listClass();

            // All are same raw class
            assertEquals(strListClass, intListClass);
            assertEquals(intListClass, entityListClass);
        }

        @Test
        @DisplayName("objectClass infers correct generic type")
        void objectClassInfersType() {
            Class<String> strClass = TypeUtils.objectClass();
            Class<Integer> intClass = TypeUtils.objectClass();
            Class<TestEntity> entityClass = TypeUtils.objectClass();

            // All return Object.class
            assertEquals(Object.class, strClass);
            assertEquals(Object.class, intClass);
            assertEquals(Object.class, entityClass);
        }

        @Test
        @DisplayName("cast infers return type from context")
        void castInfersReturnType() {
            Object value = "hello";

            // Type inferred from assignment
            String str = TypeUtils.cast(value);
            assertEquals("hello", str);

            // Type inferred from method parameter
            acceptString(TypeUtils.cast(value));
        }

        private void acceptString(String s) {
            assertNotNull(s);
        }
    }
}
