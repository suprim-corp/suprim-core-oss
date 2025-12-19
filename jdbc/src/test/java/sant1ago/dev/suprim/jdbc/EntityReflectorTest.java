package sant1ago.dev.suprim.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.entity.Table;
import sant1ago.dev.suprim.annotation.type.SqlType;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EntityReflector to cover edge cases and branches.
 */
@DisplayName("EntityReflector Tests")
class EntityReflectorTest {

    // ==================== TEST ENTITIES ====================

    /**
     * Entity with @Id but @Column has empty name (uses snake_case conversion).
     */
    @Entity(table = "empty_column_name_entities")
    static class EmptyColumnNameEntity {
        @Id
        @Column(name = "") // Empty name triggers snake_case conversion
        private Long myIdField;

        @Column(name = "name")
        private String name;

        public EmptyColumnNameEntity() {}

        public Long getMyIdField() { return myIdField; }
        public void setMyIdField(Long id) { this.myIdField = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /**
     * Entity with @Id field but NO @Column annotation (uses snake_case conversion).
     */
    @Entity(table = "no_column_annotation_entities")
    static class NoColumnAnnotationIdEntity {
        @Id
        private Long entityId; // No @Column, should convert to entity_id

        @Column(name = "title")
        private String title;

        public NoColumnAnnotationIdEntity() {}

        public Long getEntityId() { return entityId; }
        public void setEntityId(Long id) { this.entityId = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    /**
     * Entity WITHOUT @Entity but WITH @Table annotation.
     */
    @Table(name = "table_only_entities", schema = "test_schema")
    static class TableOnlyEntity {
        @Id
        @Column(name = "id")
        private Long id;

        @Column(name = "value")
        private String value;

        public TableOnlyEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    /**
     * Entity with @Table that has empty name (uses class name snake_case).
     */
    @Table(name = "", schema = "from_table")
    static class EmptyTableNameEntity {
        @Id
        @Column(name = "id")
        private Long id;

        public EmptyTableNameEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    /**
     * Entity with @Entity having schema, @Table should not override it.
     */
    @Entity(table = "entity_schema_test", schema = "entity_schema")
    @Table(name = "ignored", schema = "table_schema")
    static class EntityWithSchemaEntity {
        @Id
        @Column(name = "id")
        private Long id;

        public EntityWithSchemaEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    /**
     * Entity with @Column specifying SqlType.UUID.
     */
    @Entity(table = "uuid_entities")
    static class UuidColumnEntity {
        @Id
        @Column(name = "id", type = SqlType.UUID)
        private java.util.UUID id;

        @Column(name = "external_id", type = SqlType.UUID)
        private java.util.UUID externalId;

        @Column(name = "name")
        private String name;

        public UuidColumnEntity() {}

        public java.util.UUID getId() { return id; }
        public void setId(java.util.UUID id) { this.id = id; }
        public java.util.UUID getExternalId() { return externalId; }
        public void setExternalId(java.util.UUID externalId) { this.externalId = externalId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /**
     * Entity with fields but no @Column annotations (except @Id).
     */
    @Entity(table = "partial_column_entities")
    static class PartialColumnEntity {
        @Id
        @Column(name = "id")
        private Long id;

        private String unmappedField; // No @Column annotation

        @Column(name = "mapped_field")
        private String mappedField;

        public PartialColumnEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUnmappedField() { return unmappedField; }
        public void setUnmappedField(String f) { this.unmappedField = f; }
        public String getMappedField() { return mappedField; }
        public void setMappedField(String f) { this.mappedField = f; }
    }

    /**
     * Entity that cannot be instantiated (no no-arg constructor).
     */
    @Entity(table = "no_constructor_entities")
    static class NoConstructorEntity {
        @Id
        @Column(name = "id")
        private Long id;

        public NoConstructorEntity(Long id) {
            this.id = id;
        }

        public Long getId() { return id; }
    }

    /**
     * Entity without setter (for strict mode test).
     */
    @Entity(table = "no_setter_entities")
    static class NoSetterEntity {
        @Id
        @Column(name = "id")
        private Long id;

        @Column(name = "readonly_field")
        private final String readonlyField;

        public NoSetterEntity() {
            this.readonlyField = "default";
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getReadonlyField() { return readonlyField; }
        // No setter for readonlyField
    }

    /**
     * Base entity for inheritance testing.
     */
    @Entity(table = "base_entities")
    static class BaseEntity {
        @Column(name = "base_field")
        private String baseField;

        public BaseEntity() {}

        public String getBaseField() { return baseField; }
        public void setBaseField(String f) { this.baseField = f; }
    }

    /**
     * Child entity extending BaseEntity.
     */
    @Entity(table = "child_entities")
    static class ChildEntity extends BaseEntity {
        @Id
        @Column(name = "id")
        private Long id;

        @Column(name = "child_field")
        private String childField;

        public ChildEntity() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getChildField() { return childField; }
        public void setChildField(String f) { this.childField = f; }
    }

    // ==================== TESTS ====================

    @Nested
    @DisplayName("getIdOrNull Tests")
    class GetIdOrNullTests {

        @Test
        @DisplayName("Null entity returns null")
        void getIdOrNull_nullEntity_returnsNull() {
            Object result = EntityReflector.getIdOrNull(null);
            assertNull(result);
        }

        @Test
        @DisplayName("Entity with null ID returns null")
        void getIdOrNull_entityWithNullId_returnsNull() {
            UuidColumnEntity entity = new UuidColumnEntity();
            Object result = EntityReflector.getIdOrNull(entity);
            assertNull(result);
        }

        @Test
        @DisplayName("Entity with ID returns the ID")
        void getIdOrNull_entityWithId_returnsId() {
            UuidColumnEntity entity = new UuidColumnEntity();
            java.util.UUID uuid = java.util.UUID.randomUUID();
            entity.setId(uuid);

            Object result = EntityReflector.getIdOrNull(entity);
            assertEquals(uuid, result);
        }
    }

    @Nested
    @DisplayName("setId Tests")
    class SetIdTests {

        @Test
        @DisplayName("Null entity throws exception")
        void setId_nullEntity_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.setId(null, 1L));
        }

        @Test
        @DisplayName("Sets ID correctly")
        void setId_validEntity_setsId() {
            UuidColumnEntity entity = new UuidColumnEntity();
            java.util.UUID uuid = java.util.UUID.randomUUID();

            EntityReflector.setId(entity, uuid);

            assertEquals(uuid, entity.getId());
        }
    }

    @Nested
    @DisplayName("getEntityMeta Tests")
    class GetEntityMetaTests {

        @Test
        @DisplayName("Entity with @Entity annotation extracts correctly")
        void getEntityMeta_entityAnnotation_extractsCorrectly() {
            EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(UuidColumnEntity.class);

            assertEquals("uuid_entities", meta.tableName());
            assertNull(meta.schema());
            assertEquals("id", meta.idColumn());
        }

        @Test
        @DisplayName("Entity with only @Table annotation uses table name")
        void getEntityMeta_tableOnlyAnnotation_usesTableName() {
            EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(TableOnlyEntity.class);

            assertEquals("table_only_entities", meta.tableName());
            assertEquals("test_schema", meta.schema());
            assertEquals("id", meta.idColumn());
        }

        @Test
        @DisplayName("Entity with @Table but empty name uses snake_case class name")
        void getEntityMeta_emptyTableName_usesSnakeCaseClassName() {
            EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(EmptyTableNameEntity.class);

            assertEquals("empty_table_name_entity", meta.tableName());
            assertEquals("from_table", meta.schema());
        }

        @Test
        @DisplayName("Entity with @Entity schema takes precedence over @Table schema")
        void getEntityMeta_entitySchemaOverridesTableSchema() {
            EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(EntityWithSchemaEntity.class);

            assertEquals("entity_schema_test", meta.tableName());
            assertEquals("entity_schema", meta.schema());
        }

        @Test
        @DisplayName("ID column from @Column name")
        void getEntityMeta_idColumnFromColumnAnnotation() {
            EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(UuidColumnEntity.class);
            assertEquals("id", meta.idColumn());
        }

        @Test
        @DisplayName("ID column from snake_case when no @Column")
        void getEntityMeta_idColumnFromSnakeCase() {
            EntityReflector.EntityMeta meta = EntityReflector.getEntityMeta(NoColumnAnnotationIdEntity.class);
            assertEquals("entity_id", meta.idColumn());
        }

        @Test
        @DisplayName("Null entity class throws exception")
        void getEntityMeta_nullClass_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.getEntityMeta(null));
        }
    }

    @Nested
    @DisplayName("getIdMeta Tests")
    class GetIdMetaTests {

        @Test
        @DisplayName("@Column with empty name uses snake_case")
        void getIdMeta_emptyColumnName_usesSnakeCase() {
            EntityReflector.IdMeta meta = EntityReflector.getIdMeta(EmptyColumnNameEntity.class);

            assertEquals("myIdField", meta.fieldName());
            assertEquals("my_id_field", meta.columnName());
        }

        @Test
        @DisplayName("No @Column on ID field uses snake_case")
        void getIdMeta_noColumnAnnotation_usesSnakeCase() {
            EntityReflector.IdMeta meta = EntityReflector.getIdMeta(NoColumnAnnotationIdEntity.class);

            assertEquals("entityId", meta.fieldName());
            assertEquals("entity_id", meta.columnName());
        }

        @Test
        @DisplayName("@Column with SqlType extracts type")
        void getIdMeta_columnWithType_extractsType() {
            EntityReflector.IdMeta meta = EntityReflector.getIdMeta(UuidColumnEntity.class);

            assertEquals(SqlType.UUID, meta.columnType());
        }

        @Test
        @DisplayName("Null entity class throws exception")
        void getIdMeta_nullClass_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.getIdMeta(null));
        }
    }

    @Nested
    @DisplayName("getColumnType Tests")
    class GetColumnTypeTests {

        @Test
        @DisplayName("Null entity class returns AUTO")
        void getColumnType_nullClass_returnsAuto() {
            SqlType result = EntityReflector.getColumnType(null, "id");
            assertEquals(SqlType.AUTO, result);
        }

        @Test
        @DisplayName("Null column name returns AUTO")
        void getColumnType_nullColumnName_returnsAuto() {
            SqlType result = EntityReflector.getColumnType(UuidColumnEntity.class, null);
            assertEquals(SqlType.AUTO, result);
        }

        @Test
        @DisplayName("Non-existent column returns AUTO")
        void getColumnType_nonExistentColumn_returnsAuto() {
            SqlType result = EntityReflector.getColumnType(UuidColumnEntity.class, "does_not_exist");
            assertEquals(SqlType.AUTO, result);
        }

        @Test
        @DisplayName("Column with explicit type returns that type")
        void getColumnType_columnWithType_returnsType() {
            SqlType result = EntityReflector.getColumnType(UuidColumnEntity.class, "external_id");
            assertEquals(SqlType.UUID, result);
        }

        @Test
        @DisplayName("Column without type annotation returns AUTO")
        void getColumnType_columnWithoutType_returnsAuto() {
            SqlType result = EntityReflector.getColumnType(UuidColumnEntity.class, "name");
            assertEquals(SqlType.AUTO, result);
        }
    }

    @Nested
    @DisplayName("isUuidColumn Tests")
    class IsUuidColumnTests {

        @Test
        @DisplayName("UUID column returns true")
        void isUuidColumn_uuidColumn_returnsTrue() {
            boolean result = EntityReflector.isUuidColumn(UuidColumnEntity.class, "id");
            assertTrue(result);
        }

        @Test
        @DisplayName("Non-UUID column returns false")
        void isUuidColumn_nonUuidColumn_returnsFalse() {
            boolean result = EntityReflector.isUuidColumn(UuidColumnEntity.class, "name");
            assertFalse(result);
        }

        @Test
        @DisplayName("Null class returns false")
        void isUuidColumn_nullClass_returnsFalse() {
            boolean result = EntityReflector.isUuidColumn(null, "id");
            assertFalse(result);
        }

        @Test
        @DisplayName("Non-existent column returns false")
        void isUuidColumn_nonExistentColumn_returnsFalse() {
            boolean result = EntityReflector.isUuidColumn(UuidColumnEntity.class, "does_not_exist");
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("toColumnMap Tests")
    class ToColumnMapTests {

        @Test
        @DisplayName("Only fields with @Column are included")
        void toColumnMap_onlyColumnAnnotatedFields() {
            PartialColumnEntity entity = new PartialColumnEntity();
            entity.setId(1L);
            entity.setUnmappedField("unmapped");
            entity.setMappedField("mapped");

            Map<String, Object> result = EntityReflector.toColumnMap(entity);

            assertTrue(result.containsKey("id"));
            assertTrue(result.containsKey("mapped_field"));
            assertFalse(result.containsKey("unmappedField"));
            assertFalse(result.containsKey("unmapped_field"));
        }

        @Test
        @DisplayName("Null entity throws exception")
        void toColumnMap_nullEntity_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.toColumnMap(null));
        }

        @Test
        @DisplayName("Null values are not included")
        void toColumnMap_nullValuesExcluded() {
            UuidColumnEntity entity = new UuidColumnEntity();
            entity.setId(java.util.UUID.randomUUID());
            // name and externalId are null

            Map<String, Object> result = EntityReflector.toColumnMap(entity);

            assertTrue(result.containsKey("id"));
            assertFalse(result.containsKey("name"));
            assertFalse(result.containsKey("external_id"));
        }
    }

    @Nested
    @DisplayName("fromMap Tests")
    class FromMapTests {

        @Test
        @DisplayName("Creates entity and sets fields from map")
        void fromMap_validMap_createsEntity() {
            Map<String, Object> attrs = new LinkedHashMap<>();
            attrs.put("id", java.util.UUID.randomUUID());
            attrs.put("name", "test");

            UuidColumnEntity result = EntityReflector.fromMap(UuidColumnEntity.class, attrs);

            assertNotNull(result);
            assertEquals("test", result.getName());
        }

        @Test
        @DisplayName("Null entity class throws exception")
        void fromMap_nullClass_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.fromMap(null, Map.of()));
        }

        @Test
        @DisplayName("Null attributes map throws exception")
        void fromMap_nullAttributes_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.fromMap(UuidColumnEntity.class, null));
        }

        @Test
        @DisplayName("Entity without no-arg constructor throws exception")
        void fromMap_noConstructor_throwsException() {
            Map<String, Object> attrs = Map.of("id", 1L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.fromMap(NoConstructorEntity.class, attrs));

            assertTrue(ex.getMessage().contains("no-arg constructor"));
        }

        @Test
        @DisplayName("Unknown columns are ignored")
        void fromMap_unknownColumns_ignored() {
            Map<String, Object> attrs = new LinkedHashMap<>();
            attrs.put("id", java.util.UUID.randomUUID());
            attrs.put("unknown_column", "ignored");

            UuidColumnEntity result = EntityReflector.fromMap(UuidColumnEntity.class, attrs);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("findFieldByColumnName Tests")
    class FindFieldByColumnNameTests {

        @Test
        @DisplayName("Finds field by column name (case insensitive)")
        void getFieldByColumnName_findsField() {
            UuidColumnEntity entity = new UuidColumnEntity();
            entity.setName("test-value");

            Object result = EntityReflector.getFieldByColumnName(entity, "NAME"); // uppercase
            assertEquals("test-value", result);
        }

        @Test
        @DisplayName("Returns null for non-existent column")
        void getFieldByColumnName_nonExistent_returnsNull() {
            UuidColumnEntity entity = new UuidColumnEntity();
            Object result = EntityReflector.getFieldByColumnName(entity, "does_not_exist");
            assertNull(result);
        }

        @Test
        @DisplayName("Null entity returns null")
        void getFieldByColumnName_nullEntity_returnsNull() {
            Object result = EntityReflector.getFieldByColumnName(null, "name");
            assertNull(result);
        }

        @Test
        @DisplayName("Null column name returns null")
        void getFieldByColumnName_nullColumnName_returnsNull() {
            UuidColumnEntity entity = new UuidColumnEntity();
            Object result = EntityReflector.getFieldByColumnName(entity, null);
            assertNull(result);
        }

        @Test
        @DisplayName("Empty column name returns null")
        void getFieldByColumnName_emptyColumnName_returnsNull() {
            UuidColumnEntity entity = new UuidColumnEntity();
            Object result = EntityReflector.getFieldByColumnName(entity, "");
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Gets fields from parent class")
        void toColumnMap_includesParentFields() {
            ChildEntity entity = new ChildEntity();
            entity.setId(1L);
            entity.setChildField("child");
            entity.setBaseField("base");

            Map<String, Object> result = EntityReflector.toColumnMap(entity);

            assertTrue(result.containsKey("id"));
            assertTrue(result.containsKey("child_field"));
            assertTrue(result.containsKey("base_field"));
        }

        @Test
        @DisplayName("Sets fields on parent class")
        void setFieldByColumnName_setsParentField() {
            ChildEntity entity = new ChildEntity();
            EntityReflector.setFieldByColumnName(entity, "base_field", "from-parent");

            assertEquals("from-parent", entity.getBaseField());
        }
    }

    @Nested
    @DisplayName("setField Tests")
    class SetFieldTests {

        @Test
        @DisplayName("Null entity throws exception")
        void setField_nullEntity_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.setField(null, "name", "value"));
        }

        @Test
        @DisplayName("Null field name throws exception")
        void setField_nullFieldName_throwsException() {
            UuidColumnEntity entity = new UuidColumnEntity();
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.setField(entity, null, "value"));
        }

        @Test
        @DisplayName("Empty field name throws exception")
        void setField_emptyFieldName_throwsException() {
            UuidColumnEntity entity = new UuidColumnEntity();
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.setField(entity, "", "value"));
        }

        @Test
        @DisplayName("Non-existent field throws exception")
        void setField_nonExistentField_throwsException() {
            UuidColumnEntity entity = new UuidColumnEntity();
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.setField(entity, "nonExistent", "value"));
        }

        @Test
        @DisplayName("Sets field value correctly")
        void setField_validField_setsValue() {
            UuidColumnEntity entity = new UuidColumnEntity();
            EntityReflector.setField(entity, "name", "test-name");

            assertEquals("test-name", entity.getName());
        }
    }

    @Nested
    @DisplayName("setFieldByColumnName Tests")
    class SetFieldByColumnNameTests {

        @Test
        @DisplayName("Null entity throws exception")
        void setFieldByColumnName_nullEntity_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.setFieldByColumnName(null, "name", "value"));
        }

        @Test
        @DisplayName("Null column name throws exception")
        void setFieldByColumnName_nullColumnName_throwsException() {
            UuidColumnEntity entity = new UuidColumnEntity();
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.setFieldByColumnName(entity, null, "value"));
        }

        @Test
        @DisplayName("Empty column name throws exception")
        void setFieldByColumnName_emptyColumnName_throwsException() {
            UuidColumnEntity entity = new UuidColumnEntity();
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.setFieldByColumnName(entity, "", "value"));
        }

        @Test
        @DisplayName("Non-existent column throws exception")
        void setFieldByColumnName_nonExistentColumn_throwsException() {
            UuidColumnEntity entity = new UuidColumnEntity();
            assertThrows(IllegalArgumentException.class, () ->
                EntityReflector.setFieldByColumnName(entity, "non_existent", "value"));
        }

        @Test
        @DisplayName("Sets field by column name")
        void setFieldByColumnName_validColumn_setsValue() {
            UuidColumnEntity entity = new UuidColumnEntity();
            EntityReflector.setFieldByColumnName(entity, "name", "test-name");

            assertEquals("test-name", entity.getName());
        }
    }
}
