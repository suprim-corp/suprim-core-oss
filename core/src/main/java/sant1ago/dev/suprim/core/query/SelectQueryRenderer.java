package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.annotation.entity.SoftDeletes;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.type.Predicate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Renders SelectBuilder to SQL query.
 * Stateless utility class that handles SQL generation for SELECT queries.
 */
public final class SelectQueryRenderer {

    private SelectQueryRenderer() {
        // Utility class, prevent instantiation
    }

    /**
     * Render SelectBuilder to SQL query.
     *
     * @param builder the SelectBuilder with query configuration
     * @param dialect SQL dialect for rendering
     * @return QueryResult with SQL, parameters, and metadata
     */
    public static QueryResult render(SelectBuilder builder, SqlDialect dialect) {
        // Apply default eager loads from @Entity(with = {...}) annotation
        mergeDefaultEagerLoads(builder);

        // Apply soft delete filter based on entity's @SoftDeletes annotation
        applySoftDeleteFilter(builder, dialect);

        // Create parameter context for collecting WHERE/HAVING parameters
        ParameterContext paramContext = new ParameterContext();

        StringBuilder sql = new StringBuilder();

        // CTEs (WITH clause)
        renderCtes(sql, builder, dialect);

        // SELECT
        renderSelect(sql, builder, dialect);

        // FROM
        renderFrom(sql, builder, dialect);

        // JOINs
        renderJoins(sql, builder, dialect, paramContext);

        // WHERE
        renderWhere(sql, builder, dialect, paramContext);

        // GROUP BY
        renderGroupBy(sql, builder, dialect);

        // HAVING
        renderHaving(sql, builder, dialect, paramContext);

        // SET OPERATIONS
        renderSetOperations(sql, builder, dialect);

        // ORDER BY
        renderOrderBy(sql, builder, dialect);

        // LIMIT/OFFSET
        renderLimitOffset(sql, builder);

        // ROW LOCKING
        renderLocking(sql, builder, dialect);

        // Merge predicate parameters with existing parameters
        Map<String, Object> allParams = new LinkedHashMap<>(builder.parameters());
        allParams.putAll(paramContext.getParameters());

        return new QueryResult(
                sql.toString(),
                allParams,
                new ArrayList<>(builder.eagerLoads()),
                builder.getSoftDeleteScope()
        );
    }

    /**
     * Merge default eager loads from @Entity(with = {...}) annotation.
     * Only adds defaults that are NOT explicitly excluded via .without().
     */
    private static void mergeDefaultEagerLoads(SelectBuilder builder) {
        if (isNull(builder.fromTable()) || isNull(builder.fromTable().getEntityType())
                || builder.fromTable().getEntityType() == Object.class) {
            return;
        }

        // Get defaults from @Entity annotation
        String[] defaults = Suprim.defaultEagerLoads(builder.fromTable().getEntityType());
        if (defaults.length == 0) {
            return;
        }

        // Check which defaults are already explicitly loaded
        Set<String> explicitlyLoaded = builder.eagerLoads().stream()
                .map(spec -> spec.relation().getFieldName())
                .collect(Collectors.toSet());

        // Add defaults that are not excluded and not already loaded
        for (String relationName : defaults) {
            if (!builder.withoutRelations().contains(relationName) && !explicitlyLoaded.contains(relationName)) {
                try {
                    EagerLoadSpec spec = PathResolver.resolve(relationName, builder.fromTable().getEntityType());
                    builder.eagerLoads().add(spec);
                } catch (Exception e) {
                    // Silently skip invalid relation names - they'll be caught during validation
                }
            }
        }
    }

    /**
     * Apply soft delete filter based on entity's @SoftDeletes annotation and current scope.
     */
    private static void applySoftDeleteFilter(SelectBuilder builder, SqlDialect dialect) {
        // Skip if no from table or entity type
        if (isNull(builder.fromTable()) || isNull(builder.fromTable().getEntityType())) {
            return;
        }

        // Check if entity has @SoftDeletes annotation
        Class<?> entityClass = builder.fromTable().getEntityType();
        SoftDeletes softDeletes = entityClass.getAnnotation(SoftDeletes.class);
        if (isNull(softDeletes)) {
            return; // Entity doesn't support soft deletes
        }

        String columnName = softDeletes.column();
        SelectBuilder.SoftDeleteScope scope = builder.softDeleteScope();

        switch (scope) {
            case DEFAULT -> {
                // Exclude soft-deleted records: WHERE deleted_at IS NULL
                Predicate softDeletePredicate = new Predicate.RawPredicate(
                        dialect.quoteIdentifier(builder.fromTable().getName()) + "." +
                                dialect.quoteIdentifier(columnName) + " IS NULL"
                );
                if (isNull(builder.whereClause())) {
                    builder.whereClause(softDeletePredicate);
                } else {
                    builder.whereClause(builder.whereClause().and(softDeletePredicate));
                }
            }
            case WITH_TRASHED -> {
                // Include all records - no filter needed
            }
            case ONLY_TRASHED -> {
                // Only soft-deleted records: WHERE deleted_at IS NOT NULL
                Predicate onlyTrashedPredicate = new Predicate.RawPredicate(
                        dialect.quoteIdentifier(builder.fromTable().getName()) + "." +
                                dialect.quoteIdentifier(columnName) + " IS NOT NULL"
                );
                if (isNull(builder.whereClause())) {
                    builder.whereClause(onlyTrashedPredicate);
                } else {
                    builder.whereClause(builder.whereClause().and(onlyTrashedPredicate));
                }
            }
        }
    }

    private static void renderCtes(StringBuilder sql, SelectBuilder builder, SqlDialect dialect) {
        List<SelectBuilder.CteClause> ctes = builder.ctes();
        if (!ctes.isEmpty()) {
            sql.append("WITH ");
            if (builder.recursive()) {
                sql.append("RECURSIVE ");
            }
            sql.append(ctes.stream()
                    .map(cte -> cte.toSql(dialect))
                    .collect(Collectors.joining(", ")));
            sql.append(" ");
        }
    }

    private static void renderSelect(StringBuilder sql, SelectBuilder builder, SqlDialect dialect) {
        sql.append("SELECT ");
        if (builder.isDistinct()) {
            sql.append("DISTINCT ");
        }

        List<SelectItem> selectItems = builder.selectItems();
        if (selectItems.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(selectItems.stream()
                    .map(item -> item.toSql(dialect))
                    .collect(Collectors.joining(", ")));
        }
    }

    private static void renderFrom(StringBuilder sql, SelectBuilder builder, SqlDialect dialect) {
        if (nonNull(builder.fromTable())) {
            sql.append(" FROM ").append(builder.fromTable().toSql(dialect));
        }
    }

    private static void renderJoins(StringBuilder sql, SelectBuilder builder, SqlDialect dialect, ParameterContext paramContext) {
        for (SelectBuilder.JoinClause join : builder.joins()) {
            if (join.type() == SelectBuilder.JoinType.RAW) {
                // Raw join - the predicate contains the full join SQL
                sql.append(" ").append(join.on().toSql(dialect, paramContext));
            } else {
                sql.append(" ").append(join.type().getSql())
                        .append(" ").append(join.table().toSql(dialect))
                        .append(" ON ").append(join.on().toSql(dialect, paramContext));
            }
        }
    }

    private static void renderWhere(StringBuilder sql, SelectBuilder builder, SqlDialect dialect, ParameterContext paramContext) {
        if (nonNull(builder.whereClause())) {
            sql.append(" WHERE ").append(builder.whereClause().toSql(dialect, paramContext));
        }
    }

    private static void renderGroupBy(StringBuilder sql, SelectBuilder builder, SqlDialect dialect) {
        List<GroupByItem> groupByItems = builder.groupByItems();
        if (!groupByItems.isEmpty()) {
            sql.append(" GROUP BY ");
            sql.append(groupByItems.stream()
                    .map(item -> item.toSql(dialect))
                    .collect(Collectors.joining(", ")));
        }
    }

    private static void renderHaving(StringBuilder sql, SelectBuilder builder, SqlDialect dialect, ParameterContext paramContext) {
        if (nonNull(builder.havingClause())) {
            sql.append(" HAVING ").append(builder.havingClause().toSql(dialect, paramContext));
        }
    }

    private static void renderSetOperations(StringBuilder sql, SelectBuilder builder, SqlDialect dialect) {
        for (SelectBuilder.SetOperation setOp : builder.setOperations()) {
            sql.append(" ").append(setOp.operator()).append(" ").append(setOp.other().build(dialect).sql());
        }
    }

    private static void renderOrderBy(StringBuilder sql, SelectBuilder builder, SqlDialect dialect) {
        if (!builder.orderSpecs().isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(builder.orderSpecs().stream()
                    .map(o -> o.toSql(dialect))
                    .collect(Collectors.joining(", ")));
        }
    }

    private static void renderLimitOffset(StringBuilder sql, SelectBuilder builder) {
        Integer limit = builder.getLimit();
        Integer offset = builder.getOffset();

        if (nonNull(limit)) {
            sql.append(" LIMIT ").append(limit);
        }

        if (nonNull(offset) && offset > 0) {
            sql.append(" OFFSET ").append(offset);
        }
    }

    private static void renderLocking(StringBuilder sql, SelectBuilder builder, SqlDialect dialect) {
        String lockMode = builder.lockMode();
        if (nonNull(lockMode)) {
            // Check dialect support for NOWAIT and SKIP LOCKED
            if (lockMode.contains("NOWAIT") && !dialect.capabilities().supportsNowait()) {
                throw new UnsupportedDialectFeatureException("NOWAIT", dialect.getName(),
                        "FOR UPDATE NOWAIT requires MySQL 8.0+ or PostgreSQL.");
            }
            if (lockMode.contains("SKIP LOCKED") && !dialect.capabilities().supportsSkipLocked()) {
                throw new UnsupportedDialectFeatureException("SKIP LOCKED", dialect.getName(),
                        "FOR UPDATE SKIP LOCKED requires MySQL 8.0+ or PostgreSQL.");
            }
            sql.append(" ").append(lockMode);
        }
    }
}
