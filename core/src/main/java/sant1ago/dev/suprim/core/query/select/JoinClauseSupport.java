package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.Relation;
import sant1ago.dev.suprim.core.type.Table;

/**
 * Mixin interface providing JOIN clause operations.
 * Includes join, leftJoin, rightJoin, joinRaw, and relation-based joins.
 */
public interface JoinClauseSupport extends SelectBuilderCore {

    /**
     * INNER JOIN another table.
     */
    default SelectBuilder join(Table<?> table, Predicate on) {
        joins().add(new JoinClause(JoinType.INNER, table, on));
        return self();
    }

    /**
     * LEFT JOIN another table.
     */
    default SelectBuilder leftJoin(Table<?> table, Predicate on) {
        joins().add(new JoinClause(JoinType.LEFT, table, on));
        return self();
    }

    /**
     * RIGHT JOIN another table.
     */
    default SelectBuilder rightJoin(Table<?> table, Predicate on) {
        joins().add(new JoinClause(JoinType.RIGHT, table, on));
        return self();
    }

    /**
     * Add raw JOIN clause.
     */
    default SelectBuilder joinRaw(String rawJoinSql) {
        joins().add(new JoinClause(JoinType.RAW, null, new Predicate.RawPredicate(rawJoinSql)));
        return self();
    }

    // ==================== RELATIONSHIP JOINS ====================

    /**
     * LEFT JOIN using a Relation definition.
     * Automatically generates the proper JOIN clause based on relationship type.
     */
    default SelectBuilder leftJoin(Relation<?, ?> relation) {
        addRelationJoin(relation, JoinType.LEFT);
        return self();
    }

    /**
     * INNER JOIN using a Relation definition.
     */
    default SelectBuilder join(Relation<?, ?> relation) {
        addRelationJoin(relation, JoinType.INNER);
        return self();
    }

    /**
     * RIGHT JOIN using a Relation definition.
     */
    default SelectBuilder rightJoin(Relation<?, ?> relation) {
        addRelationJoin(relation, JoinType.RIGHT);
        return self();
    }

    /**
     * Add a JOIN clause based on relation type.
     */
    private void addRelationJoin(Relation<?, ?> relation, JoinType joinType) {
        Table<?> relatedTable = relation.getRelatedTable();
        Table<?> ownerTable = relation.getOwnerTable();

        String foreignKey = relation.getForeignKey();
        String localKey = relation.getLocalKey();
        String relatedKey = relation.getRelatedKey();
        String pivotTable = relation.getPivotTable();
        String relatedPivotKey = relation.getRelatedPivotKey();
        String foreignPivotKey = relation.getForeignPivotKey();

        switch (relation.getType()) {
            case HAS_ONE, HAS_MANY -> {
                // LEFT JOIN related ON related.fk = owner.localKey
                String onClause = relatedTable.getName() + "." + foreignKey + " = " + ownerTable.getName() + "." + localKey;
                joins().add(new JoinClause(joinType, relatedTable, new Predicate.RawPredicate(onClause)));
            }
            case BELONGS_TO -> {
                // LEFT JOIN related ON owner.fk = related.relatedKey
                String onClause = ownerTable.getName() + "." + foreignKey + " = " + relatedTable.getName() + "." + relatedKey;
                joins().add(new JoinClause(joinType, relatedTable, new Predicate.RawPredicate(onClause)));
            }
            case BELONGS_TO_MANY -> {
                // Two-part join through pivot table
                // 1. JOIN pivot ON pivot.foreignPivotKey = owner.localKey
                String pivotOnClause = pivotTable + "." + foreignPivotKey + " = " + ownerTable.getName() + "." + localKey;
                joins().add(new JoinClause(JoinType.RAW, null, new Predicate.RawPredicate(joinType.getSql() + " " + pivotTable + " ON " + pivotOnClause)));

                // 2. JOIN related ON related.relatedKey = pivot.relatedPivotKey
                String relatedOnClause = relatedTable.getName() + "." + relatedKey + " = " + pivotTable + "." + relatedPivotKey;
                joins().add(new JoinClause(joinType, relatedTable, new Predicate.RawPredicate(relatedOnClause)));
            }
            default -> {
                // Other relationship types not supported for direct joins
            }
        }
    }
}
