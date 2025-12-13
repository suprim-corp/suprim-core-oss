package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;

import java.util.List;

/**
 * Metamodel class with private static Relation field for testing IllegalAccessException handling.
 */
public class PrivateFieldEntity_ {
    public static final Table<PrivateFieldEntity> TABLE = Table.of("private_entities", PrivateFieldEntity.class);

    // Private field to trigger IllegalAccessException
    private static final Relation<PrivateFieldEntity, PrivateFieldEntity> PRIVATE_RELATION =
            Relation.hasOne(TABLE, TABLE, "parent_id", "id", false, false);
}
