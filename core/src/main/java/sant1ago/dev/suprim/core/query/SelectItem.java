package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.type.AliasedColumn;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Expression;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.Relation;

import java.util.Objects;
import java.util.function.Function;

/**
 * Type-safe representation of items that can appear in a SELECT clause.
 * Supports Column, AliasedColumn, Aggregate, Coalesce, JSONB expressions, or raw SQL.
 */
public sealed interface SelectItem permits SelectItem.ColumnItem, SelectItem.AliasedItem, SelectItem.RawItem, SelectItem.ExpressionItem, SelectItem.CountFilterItem, SelectItem.SubqueryItem {

    /**
     * Render this select item as SQL.
     */
    String toSql(SqlDialect dialect);

    /**
     * Wrap a Column as a SelectItem.
     */
    static SelectItem of(Column<?, ?> column) {
        return new ColumnItem(column);
    }

    /**
     * Wrap an AliasedColumn as a SelectItem.
     */
    static SelectItem of(AliasedColumn<?, ?> aliased) {
        return new AliasedItem(aliased);
    }

    /**
     * Wrap any Expression as a SelectItem.
     * Handles Column, AliasedColumn, and generic expressions (Aggregate, Coalesce, etc.).
     */
    static SelectItem of(Expression<?> expr) {
        if (expr instanceof Column<?, ?> col) {
            return new ColumnItem(col);
        } else if (expr instanceof AliasedColumn<?, ?> aliased) {
            return new AliasedItem(aliased);
        }
        // For other expressions (Aggregate, Coalesce, JsonPathExpression, etc.)
        // Store the expression and render with dialect at build time
        return new ExpressionItem(expr);
    }

    /**
     * Create a raw SQL select item.
     */
    static SelectItem raw(String sql) {
        return new RawItem(sql);
    }

    /**
     * Column select item.
     *
     * @param column the column to select
     */
    record ColumnItem(Column<?, ?> column) implements SelectItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return column.toSql(dialect);
        }
    }

    /**
     * Aliased column select item.
     *
     * @param aliased the aliased column to select
     */
    record AliasedItem(AliasedColumn<?, ?> aliased) implements SelectItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return aliased.toSql(dialect);
        }
    }

    /**
     * Raw SQL select item.
     *
     * @param sql the raw SQL expression
     */
    record RawItem(String sql) implements SelectItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return sql;
        }
    }

    /**
     * Generic expression select item (for Aggregate, Coalesce, JsonPathExpression, etc.).
     *
     * @param expression the expression to select
     */
    record ExpressionItem(Expression<?> expression) implements SelectItem {
        @Override
        public String toSql(SqlDialect dialect) {
            return expression.toSql(dialect);
        }
    }

    /**
     * COUNT with FILTER clause (PostgreSQL-specific).
     * Defers SQL generation to build time for proper dialect handling.
     *
     * @param column the column to count
     * @param filter optional filter predicate (can be null)
     * @param alias the alias for the result
     */
    record CountFilterItem(Column<?, ?> column, Predicate filter, String alias) implements SelectItem {
        @Override
        public String toSql(SqlDialect dialect) {
            String columnSql = column.toSql(dialect);
            String countExpr = "COUNT(" + columnSql + ")";

            if (Objects.nonNull(filter)) {
                if (!dialect.capabilities().supportsFilterClause()) {
                    throw new UnsupportedDialectFeatureException("FILTER clause", dialect.getName(),
                            "Use CASE WHEN for conditional counting");
                }
                countExpr = dialect.aggregateFilter(countExpr, filter.toSql(dialect));
            }

            return countExpr + " AS " + alias;
        }
    }

    /**
     * Factory method for CountFilterItem.
     */
    static SelectItem countFilter(Column<?, ?> column, Predicate filter, String alias) {
        return new CountFilterItem(column, filter, alias);
    }

    /**
     * Deferred subquery select item for relation aggregates (COUNT, SUM, MAX, etc.).
     * Stores components and generates SQL at build time with correct dialect.
     *
     * @param subqueryType the type of subquery (COUNT, SUM, MAX, MIN, AVG, EXISTS)
     * @param relation the relationship to query
     * @param column the column for aggregation (null for EXISTS/COUNT(*))
     * @param constraint optional additional constraints
     * @param alias the alias for the result
     * @param ownerTableName the owner table name for correlation
     */
    record SubqueryItem(
            SubqueryType subqueryType,
            Relation<?, ?> relation,
            Column<?, ?> column,
            Function<SelectBuilder, SelectBuilder> constraint,
            String alias,
            String ownerTableName
    ) implements SelectItem {

        @Override
        public String toSql(SqlDialect dialect) {
            StringBuilder sql = new StringBuilder();

            // EXISTS wraps differently: EXISTS(SELECT 1 ...) AS alias
            boolean isExists = subqueryType == SubqueryType.EXISTS;
            if (isExists) {
                sql.append("EXISTS(SELECT 1");
            } else {
                sql.append("(SELECT ");
                switch (subqueryType) {
                    case COUNT -> sql.append("COUNT(*)");
                    case SUM -> sql.append("SUM(").append(column.getName()).append(")");
                    case MAX -> sql.append("MAX(").append(column.getName()).append(")");
                    case MIN -> sql.append("MIN(").append(column.getName()).append(")");
                    case AVG -> sql.append("AVG(").append(column.getName()).append(")");
                    default -> {}
                }
            }

            sql.append(" FROM ").append(relation.getExistsFromTable());

            // For BelongsToMany, add pivot table join
            String pivotJoin = relation.getPivotJoinForExists();
            if (Objects.nonNull(pivotJoin)) {
                sql.append(" ").append(pivotJoin);
            }

            sql.append(" WHERE ").append(relation.getExistsCondition(ownerTableName));

            // Apply additional constraints
            if (Objects.nonNull(constraint)) {
                SelectBuilder subBuilder = new SelectBuilder(java.util.List.of());
                subBuilder = constraint.apply(subBuilder);
                Predicate whereClause = subBuilder.getWhereClause();
                if (Objects.nonNull(whereClause)) {
                    sql.append(" AND ").append(whereClause.toSql(dialect));
                }
            }

            sql.append(") AS ").append(alias);
            return sql.toString();
        }
    }

    /**
     * Subquery types for relation aggregates.
     */
    enum SubqueryType {
        COUNT, EXISTS, SUM, MAX, MIN, AVG
    }

    /**
     * Factory method for SubqueryItem.
     */
    static SelectItem subquery(
            SubqueryType type,
            Relation<?, ?> relation,
            Column<?, ?> column,
            Function<SelectBuilder, SelectBuilder> constraint,
            String alias,
            String ownerTableName
    ) {
        return new SubqueryItem(type, relation, column, constraint, alias, ownerTableName);
    }
}
