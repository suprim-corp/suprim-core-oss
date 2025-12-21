package sant1ago.dev.suprim.core.query.select;

import sant1ago.dev.suprim.core.query.SelectBuilder;
import sant1ago.dev.suprim.core.query.SelectItem;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.Relation;

import java.util.function.Function;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Mixin interface providing relationship query operations.
 * Includes whereHas, whereDoesntHave, has, withCount, withSum, withAvg, withMin, withMax, withExists.
 */
public interface RelationshipQuerySupport extends SelectBuilderCore {

    // ==================== WHERE HAS / DOESN'T HAVE ====================

    /**
     * Filter by existence of related records.
     */
    default SelectBuilder whereHas(Relation<?, ?> relation) {
        return whereHas(relation, null);
    }

    /**
     * Filter by existence of related records with additional constraints.
     */
    default SelectBuilder whereHas(Relation<?, ?> relation, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        Predicate existsPredicate = new Predicate.RelationExistsPredicate(relation, constraint, false, ownerTable);

        if (isNull(whereClause())) {
            whereClause(existsPredicate);
        } else {
            whereClause(whereClause().and(existsPredicate));
        }
        return self();
    }

    /**
     * Filter by non-existence of related records.
     */
    default SelectBuilder whereDoesntHave(Relation<?, ?> relation) {
        return whereDoesntHave(relation, null);
    }

    /**
     * Filter by non-existence of related records with additional constraints.
     */
    default SelectBuilder whereDoesntHave(Relation<?, ?> relation, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        Predicate notExistsPredicate = new Predicate.RelationExistsPredicate(relation, constraint, true, ownerTable);

        if (isNull(whereClause())) {
            whereClause(notExistsPredicate);
        } else {
            whereClause(whereClause().and(notExistsPredicate));
        }
        return self();
    }

    // ==================== HAS / DOESN'T HAVE (COUNT) ====================

    /**
     * Filter by count of related records.
     */
    default SelectBuilder has(Relation<?, ?> relation, String operator, int count) {
        return has(relation, operator, count, null);
    }

    /**
     * Filter by count of related records with additional constraints.
     */
    default SelectBuilder has(Relation<?, ?> relation, String operator, int count, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        Predicate countPredicate = new Predicate.RelationCountPredicate(relation, operator, count, constraint, ownerTable);

        if (isNull(whereClause())) {
            whereClause(countPredicate);
        } else {
            whereClause(whereClause().and(countPredicate));
        }
        return self();
    }

    /**
     * Filter by having no related records.
     */
    default SelectBuilder doesntHave(Relation<?, ?> relation) {
        return has(relation, "=", 0);
    }

    // ==================== WITH AGGREGATES ====================

    /**
     * Add a count subquery for relationships to the SELECT clause.
     */
    default SelectBuilder withCount(Relation<?, ?>... relations) {
        for (Relation<?, ?> relation : relations) {
            String alias = relation.getCountAlias(getRelationFieldName(relation));
            String ownerTable = getOwnerTableName(relation);
            selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.COUNT, relation, null, null, alias, ownerTable));
        }
        return self();
    }

    /**
     * Add a count subquery with constraint and custom alias.
     */
    default SelectBuilder withCount(Relation<?, ?> relation, Function<SelectBuilder, SelectBuilder> constraint, String alias) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.COUNT, relation, null, constraint, alias, ownerTable));
        return self();
    }

    /**
     * Add a SUM subquery for a relationship column to the SELECT clause.
     */
    default SelectBuilder withSum(Relation<?, ?> relation, Column<?, ?> column, String alias) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.SUM, relation, column, null, alias, ownerTable));
        return self();
    }

    /**
     * Add a SUM subquery with constraint.
     */
    default SelectBuilder withSum(Relation<?, ?> relation, Column<?, ?> column, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.SUM, relation, column, constraint, alias, ownerTable));
        return self();
    }

    /**
     * Add an AVG subquery for a relationship column to the SELECT clause.
     */
    default SelectBuilder withAvg(Relation<?, ?> relation, Column<?, ?> column, String alias) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.AVG, relation, column, null, alias, ownerTable));
        return self();
    }

    /**
     * Add an AVG subquery with constraint.
     */
    default SelectBuilder withAvg(Relation<?, ?> relation, Column<?, ?> column, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.AVG, relation, column, constraint, alias, ownerTable));
        return self();
    }

    /**
     * Add a MIN subquery for a relationship column to the SELECT clause.
     */
    default SelectBuilder withMin(Relation<?, ?> relation, Column<?, ?> column, String alias) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.MIN, relation, column, null, alias, ownerTable));
        return self();
    }

    /**
     * Add a MIN subquery with constraint.
     */
    default SelectBuilder withMin(Relation<?, ?> relation, Column<?, ?> column, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.MIN, relation, column, constraint, alias, ownerTable));
        return self();
    }

    /**
     * Add a MAX subquery for a relationship column to the SELECT clause.
     */
    default SelectBuilder withMax(Relation<?, ?> relation, Column<?, ?> column, String alias) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.MAX, relation, column, null, alias, ownerTable));
        return self();
    }

    /**
     * Add a MAX subquery with constraint.
     */
    default SelectBuilder withMax(Relation<?, ?> relation, Column<?, ?> column, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.MAX, relation, column, constraint, alias, ownerTable));
        return self();
    }

    /**
     * Add an EXISTS subquery for a relationship to the SELECT clause.
     */
    default SelectBuilder withExists(Relation<?, ?> relation, String alias) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.EXISTS, relation, null, null, alias, ownerTable));
        return self();
    }

    /**
     * Add an EXISTS subquery with constraint.
     */
    default SelectBuilder withExists(Relation<?, ?> relation, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        selectItems().add(SelectItem.subquery(SelectItem.SubqueryType.EXISTS, relation, null, constraint, alias, ownerTable));
        return self();
    }

    // ==================== HELPERS ====================

    /**
     * Get a simple field name for a relation (used for alias generation).
     */
    private String getRelationFieldName(Relation<?, ?> relation) {
        return relation.getRelatedTable().getName();
    }

    /**
     * Get the owner table name for correlation in subqueries.
     */
    private String getOwnerTableName(Relation<?, ?> relation) {
        return nonNull(fromTable()) ? fromTable().getName() : relation.getOwnerTable().getName();
    }
}
