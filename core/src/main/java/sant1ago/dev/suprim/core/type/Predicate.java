package sant1ago.dev.suprim.core.type;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.query.SelectBuilder;

import java.util.Objects;
import java.util.function.Function;

/**
 * Boolean predicate for WHERE clauses.
 * Supports composition through AND/OR/NOT operations.
 */
public sealed interface Predicate permits Predicate.SimplePredicate, Predicate.CompositePredicate, Predicate.NotPredicate, Predicate.RawPredicate, Predicate.RelationExistsPredicate, Predicate.RelationCountPredicate, SubqueryExpression.ExistsPredicate {

    /**
     * Combine with AND: this AND other
     */
    default Predicate and(Predicate other) {
        return new CompositePredicate(this, LogicalOperator.AND, other);
    }

    /**
     * Combine with OR: this OR other
     */
    default Predicate or(Predicate other) {
        return new CompositePredicate(this, LogicalOperator.OR, other);
    }

    /**
     * Negate: NOT this
     */
    default Predicate not() {
        return new NotPredicate(this);
    }

    /**
     * Render as SQL.
     */
    String toSql(SqlDialect dialect);

    /**
     * Simple predicate: left operator right.
     *
     * @param left the left-hand expression
     * @param operator the comparison operator
     * @param right the right-hand expression
     */
    record SimplePredicate(
            Expression<?> left,
            Operator operator,
            Expression<?> right
    ) implements Predicate {

        @Override
        public String toSql(SqlDialect dialect) {
            String leftSql = left.toSql(dialect);

            return switch (operator) {
                case IS_NULL -> leftSql + " IS NULL";
                case IS_NOT_NULL -> leftSql + " IS NOT NULL";
                case IN, NOT_IN -> {
                    String rightSql = Objects.nonNull(right) ? right.toSql(dialect) : "";
                    yield leftSql + " " + operator.getSql() + " (" + rightSql + ")";
                }
                case BETWEEN -> {
                    if (right instanceof ListLiteral<?> list && list.values().size() == 2) {
                        var vals = list.values();
                        String min = new Literal<>(vals.get(0), Object.class).toSql(dialect);
                        String max = new Literal<>(vals.get(1), Object.class).toSql(dialect);
                        yield leftSql + " BETWEEN " + min + " AND " + max;
                    }
                    yield leftSql + " BETWEEN ?";
                }
                // JSONB operators - dialect-aware
                case JSONB_CONTAINS -> {
                    String jsonValue = extractJsonValue(right, dialect);
                    yield dialect.jsonContains(leftSql, jsonValue);
                }
                case JSONB_KEY_EXISTS -> {
                    String key = extractStringValue(right, dialect);
                    yield dialect.jsonKeyExists(leftSql, key);
                }
                case ILIKE -> {
                    // PostgreSQL: native ILIKE
                    // MySQL: LOWER(col) LIKE LOWER(pattern)
                    if (dialect.capabilities().supportsIlike()) {
                        String rightSql = Objects.nonNull(right) ? right.toSql(dialect) : "NULL";
                        yield leftSql + " ILIKE " + rightSql;
                    } else {
                        String rightSql = Objects.nonNull(right) ? right.toSql(dialect) : "NULL";
                        yield "LOWER(" + leftSql + ") LIKE LOWER(" + rightSql + ")";
                    }
                }
                // Array operators - PostgreSQL only
                case ARRAY_CONTAINS, ARRAY_CONTAINED_BY, ARRAY_OVERLAP -> {
                    if (!dialect.capabilities().supportsArrays()) {
                        throw new UnsupportedDialectFeatureException("Arrays", dialect.getName(),
                                "Consider using JSON arrays with JSON_CONTAINS() for MySQL.");
                    }
                    String rightSql = Objects.nonNull(right) ? right.toSql(dialect) : "NULL";
                    yield leftSql + " " + operator.getSql() + " " + rightSql;
                }
                default -> {
                    String rightSql = Objects.nonNull(right) ? right.toSql(dialect) : "NULL";
                    yield leftSql + " " + operator.getSql() + " " + rightSql;
                }
            };
        }

        private String extractJsonValue(Expression<?> expr, SqlDialect dialect) {
            if (expr instanceof JsonbColumn.JsonLiteral jsonLit) {
                return jsonLit.json();
            }
            if (expr instanceof Literal<?> lit) {
                return String.valueOf(lit.value());
            }
            return expr.toSql(dialect);
        }

        private String extractStringValue(Expression<?> expr, SqlDialect dialect) {
            if (expr instanceof Literal<?> lit) {
                return String.valueOf(lit.value());
            }
            return expr.toSql(dialect);
        }
    }

    /**
     * Composite predicate: left AND/OR right.
     *
     * @param left the left-hand predicate
     * @param operator the logical operator (AND/OR)
     * @param right the right-hand predicate
     */
    record CompositePredicate(
            Predicate left,
            LogicalOperator operator,
            Predicate right
    ) implements Predicate {

        @Override
        public String toSql(SqlDialect dialect) {
            return "(" + left.toSql(dialect) + " " + operator.name() + " " + right.toSql(dialect) + ")";
        }
    }

    /**
     * Negated predicate: NOT predicate.
     *
     * @param predicate the predicate to negate
     */
    record NotPredicate(Predicate predicate) implements Predicate {

        @Override
        public String toSql(SqlDialect dialect) {
            return "NOT (" + predicate.toSql(dialect) + ")";
        }
    }

    /**
     * Raw SQL predicate for whereRaw, andRaw, etc.
     *
     * @param sql the raw SQL string
     */
    record RawPredicate(String sql) implements Predicate {
        @Override
        public String toSql(SqlDialect dialect) {
            return sql;
        }
    }

    /**
     * Deferred EXISTS predicate for relation queries.
     * SQL generation happens at toSql() time with the correct dialect.
     *
     * @param relation the relationship to check
     * @param constraint optional additional constraints
     * @param negated true for NOT EXISTS
     * @param ownerTableName the owner table name for correlation
     */
    record RelationExistsPredicate(
            Relation<?, ?> relation,
            Function<SelectBuilder, SelectBuilder> constraint,
            boolean negated,
            String ownerTableName
    ) implements Predicate {

        @Override
        public String toSql(SqlDialect dialect) {
            StringBuilder sql = new StringBuilder();
            sql.append(negated ? "NOT EXISTS (SELECT 1" : "EXISTS (SELECT 1");
            sql.append(" FROM ").append(relation.getExistsFromTable());

            // For BelongsToMany, add pivot table join
            String pivotJoin = relation.getPivotJoinForExists();
            if (Objects.nonNull(pivotJoin)) {
                sql.append(" ").append(pivotJoin);
            }

            sql.append(" WHERE ").append(relation.getExistsCondition(ownerTableName));

            // Apply additional constraints with correct dialect
            if (Objects.nonNull(constraint)) {
                SelectBuilder subBuilder = new SelectBuilder(java.util.List.of());
                subBuilder = constraint.apply(subBuilder);
                Predicate whereClause = subBuilder.getWhereClause();
                if (Objects.nonNull(whereClause)) {
                    sql.append(" AND ").append(whereClause.toSql(dialect));
                }
            }

            sql.append(")");
            return sql.toString();
        }
    }

    /**
     * Deferred COUNT predicate for relation queries (e.g., has 5 posts).
     * SQL generation happens at toSql() time with the correct dialect.
     *
     * @param relation the relationship to count
     * @param operator comparison operator (=, >=, >, etc.)
     * @param count the count to compare against
     * @param constraint optional additional constraints
     * @param ownerTableName the owner table name for correlation
     */
    record RelationCountPredicate(
            Relation<?, ?> relation,
            String operator,
            int count,
            Function<SelectBuilder, SelectBuilder> constraint,
            String ownerTableName
    ) implements Predicate {

        @Override
        public String toSql(SqlDialect dialect) {
            StringBuilder sql = new StringBuilder();
            sql.append("(SELECT COUNT(*)");
            sql.append(" FROM ").append(relation.getExistsFromTable());

            // For BelongsToMany, add pivot table join
            String pivotJoin = relation.getPivotJoinForExists();
            if (Objects.nonNull(pivotJoin)) {
                sql.append(" ").append(pivotJoin);
            }

            sql.append(" WHERE ").append(relation.getExistsCondition(ownerTableName));

            // Apply additional constraints with correct dialect
            if (Objects.nonNull(constraint)) {
                SelectBuilder subBuilder = new SelectBuilder(java.util.List.of());
                subBuilder = constraint.apply(subBuilder);
                Predicate whereClause = subBuilder.getWhereClause();
                if (Objects.nonNull(whereClause)) {
                    sql.append(" AND ").append(whereClause.toSql(dialect));
                }
            }

            sql.append(") ").append(operator).append(" ").append(count);
            return sql.toString();
        }
    }
}
