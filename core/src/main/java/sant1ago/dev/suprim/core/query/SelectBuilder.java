package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.annotation.entity.SoftDeletes;
import sant1ago.dev.suprim.core.dialect.PostgreSqlDialect;
import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.dialect.UnsupportedDialectFeatureException;
import sant1ago.dev.suprim.core.type.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Fluent builder for SELECT queries.
 *
 * <pre>{@code
 * QueryResult result = Suprim.select(User_.ID, User_.EMAIL)
 *     .from(User_.TABLE)
 *     .where(User_.EMAIL.eq("test@example.com"))
 *     .and(User_.AGE.gte(18))
 *     .orderBy(User_.CREATED_AT.desc())
 *     .limit(10)
 *     .build();
 * }</pre>
 */
public final class SelectBuilder {

    private final List<SelectItem> selectItems = new ArrayList<>();
    private Table<?> fromTable;
    private final List<JoinClause> joins = new ArrayList<>();
    private Predicate whereClause;
    private final List<OrderSpec> orderSpecs = new ArrayList<>();
    private final List<GroupByItem> groupByItems = new ArrayList<>();
    private Predicate havingClause;
    private Integer limit;
    private Integer offset;
    private boolean distinct = false;
    private final Map<String, Object> parameters = new LinkedHashMap<>();
    private int paramCounter = 0;
    // CTE support
    private final List<CteClause> ctes = new ArrayList<>();
    private boolean recursive = false;
    // Set operations
    private final List<SetOperation> setOperations = new ArrayList<>();
    // Row locking
    private String lockMode = null;
    // Eager loading
    private final List<EagerLoadSpec> eagerLoads = new ArrayList<>();
    private final Set<String> withoutRelations = new HashSet<>();
    // Soft delete scope
    private SoftDeleteScope softDeleteScope = SoftDeleteScope.DEFAULT;

    /**
     * Soft delete query scope.
     */
    public enum SoftDeleteScope {
        /** Default behavior - exclude soft deleted records (WHERE deleted_at IS NULL) */
        DEFAULT,
        /** Include soft deleted records - no filter applied */
        WITH_TRASHED,
        /** Only return soft deleted records (WHERE deleted_at IS NOT NULL) */
        ONLY_TRASHED
    }

    public SelectBuilder(List<? extends Expression<?>> expressions) {
        Objects.requireNonNull(expressions, "expressions cannot be null");
        for (Expression<?> expr : expressions) {
            this.selectItems.add(SelectItem.of(expr));
        }
    }

    /**
     * Add additional columns/expressions to SELECT clause.
     */
    public SelectBuilder select(Expression<?>... expressions) {
        for (Expression<?> expr : expressions) {
            this.selectItems.add(SelectItem.of(expr));
        }
        return this;
    }

    // ==================== SELECT VARIANTS ====================

    /**
     * Add raw SQL expression to SELECT clause.
     * <pre>{@code
     * .selectRaw("COUNT(*) as total")
     * .selectRaw("COALESCE(name, 'N/A') as display_name")
     * }</pre>
     */
    public SelectBuilder selectRaw(String rawSql) {
        this.selectItems.add(SelectItem.raw(rawSql));
        return this;
    }

    /**
     * Add column to SELECT clause conditionally.
     * <pre>{@code
     * .selectIf(includeEmail, User_.EMAIL)
     * }</pre>
     */
    public SelectBuilder selectIf(boolean condition, Column<?, ?> column) {
        if (condition) {
            this.selectItems.add(SelectItem.of(column));
        }
        return this;
    }

    /**
     * Add raw SQL expression to SELECT clause conditionally.
     * <pre>{@code
     * .selectRawIf(includeCount, "COUNT(*) as total")
     * }</pre>
     */
    public SelectBuilder selectRawIf(boolean condition, String rawSql) {
        if (condition) {
            this.selectItems.add(SelectItem.raw(rawSql));
        }
        return this;
    }

    /**
     * Add COUNT(*) to the SELECT clause.
     */
    public SelectBuilder selectCount() {
        this.selectItems.add(SelectItem.raw("COUNT(*)"));
        return this;
    }

    /**
     * Add COUNT(*) with alias to SELECT clause.
     */
    public SelectBuilder selectCount(String alias) {
        this.selectItems.add(SelectItem.raw("COUNT(*) AS " + alias));
        return this;
    }

    /**
     * Add COUNT(column) with optional filter to SELECT clause.
     * Uses PostgreSQL FILTER syntax - throws UnsupportedDialectFeatureException for other dialects.
     * <pre>{@code
     * .selectCountFilter(User_.ID, User_.IS_ACTIVE.eq(true), "active_count")
     * }</pre>
     */
    public SelectBuilder selectCountFilter(Column<?, ?> column, Predicate filter, String alias) {
        this.selectItems.add(SelectItem.countFilter(column, filter, alias));
        return this;
    }

    /**
     * Add column with alias to SELECT clause.
     * <pre>{@code
     * .selectAs(User_.EMAIL, "user_email")
     * .selectAs(User_.NAME, "display_name")
     * }</pre>
     */
    public SelectBuilder selectAs(Column<?, ?> column, String alias) {
        this.selectItems.add(SelectItem.of(column.as(alias)));
        return this;
    }

    /**
     * Specify the FROM table.
     */
    public SelectBuilder from(Table<?> table) {
        this.fromTable = table;
        return this;
    }

    /**
     * Add SELECT DISTINCT.
     */
    public SelectBuilder distinct() {
        this.distinct = true;
        return this;
    }

    // ==================== WHERE CLAUSE ====================

    /**
     * Add WHERE condition.
     */
    public SelectBuilder where(Predicate predicate) {
        this.whereClause = predicate;
        return this;
    }

    /**
     * Add grouped WHERE condition using closure (Laravel-style).
     * Conditions inside closure are grouped with parentheses.
     * <pre>{@code
     * // Simple grouping
     * .where(q -> q.where(User_.ROLE.eq("ADMIN")).or(User_.ROLE.eq("MOD")))
     * // SQL: WHERE (role = 'ADMIN' OR role = 'MOD')
     *
     * // Combined with EXISTS
     * .where(q -> q.where(App_.TYPE.ne("SIMPLE")).orExists(modelsSubquery))
     * // SQL: WHERE (type != 'SIMPLE' OR EXISTS (...))
     * }</pre>
     */
    public SelectBuilder where(Function<SelectBuilder, SelectBuilder> group) {
        SelectBuilder nested = new SelectBuilder(List.of());
        nested = group.apply(nested);
        Predicate groupedPredicate = nested.getWhereClause();
        if (nonNull(groupedPredicate)) {
            this.whereClause = groupedPredicate;
        }
        return this;
    }

    /**
     * Add WHERE condition only if value is present (not null).
     * <pre>{@code
     * String email = request.getEmail(); // may be null
     * .whereIfPresent(email, () -> User_.EMAIL.eq(email))
     * }</pre>
     */
    public <V> SelectBuilder whereIfPresent(V value, Supplier<Predicate> predicateSupplier) {
        if (nonNull(value)) {
            this.whereClause = predicateSupplier.get();
        }
        return this;
    }

    /**
     * Add raw SQL WHERE condition.
     * <pre>{@code
     * .whereRaw("created_at > NOW() - INTERVAL '7 days'")
     * }</pre>
     */
    public SelectBuilder whereRaw(String rawSql) {
        this.whereClause = new Predicate.RawPredicate(rawSql);
        return this;
    }

    /**
     * Add raw SQL WHERE condition with parameters.
     * <pre>{@code
     * .whereRaw("email = :email", Map.of("email", "test@example.com"))
     * }</pre>
     */
    public SelectBuilder whereRaw(String rawSql, java.util.Map<String, Object> params) {
        this.whereClause = new Predicate.ParameterizedRawPredicate(rawSql, params);
        return this;
    }

    /**
     * Add AND condition to WHERE clause.
     */
    public SelectBuilder and(Predicate predicate) {
        if (isNull(this.whereClause)) {
            this.whereClause = predicate;
        } else {
            this.whereClause = this.whereClause.and(predicate);
        }
        return this;
    }

    /**
     * Add grouped AND condition using closure (Laravel-style).
     * Conditions inside closure are grouped with parentheses.
     * <pre>{@code
     * .where(User_.IS_ACTIVE.eq(true))
     * .and(q -> q.where(User_.ROLE.eq("ADMIN")).or(User_.ROLE.eq("MOD")))
     * // SQL: WHERE is_active = true AND (role = 'ADMIN' OR role = 'MOD')
     *
     * // With EXISTS
     * .and(q -> q.where(App_.TYPE.ne("SIMPLE")).orExists(modelsSubquery))
     * // SQL: AND (type != 'SIMPLE' OR EXISTS (...))
     * }</pre>
     */
    public SelectBuilder and(Function<SelectBuilder, SelectBuilder> group) {
        SelectBuilder nested = new SelectBuilder(List.of());
        nested = group.apply(nested);
        Predicate groupedPredicate = nested.getWhereClause();
        if (nonNull(groupedPredicate)) {
            if (isNull(this.whereClause)) {
                this.whereClause = groupedPredicate;
            } else {
                this.whereClause = this.whereClause.and(groupedPredicate);
            }
        }
        return this;
    }

    /**
     * Add AND condition only if value is present (not null).
     * <pre>{@code
     * .where(User_.IS_ACTIVE.eq(true))
     * .andIfPresent(email, () -> User_.EMAIL.eq(email))
     * .andIfPresent(minAge, () -> User_.AGE.gte(minAge))
     * }</pre>
     */
    public <V> SelectBuilder andIfPresent(V value, Supplier<Predicate> predicateSupplier) {
        if (nonNull(value)) {
            and(predicateSupplier.get());
        }
        return this;
    }

    /**
     * Add raw SQL AND condition.
     */
    public SelectBuilder andRaw(String rawSql) {
        Predicate rawPredicate = new Predicate.RawPredicate(rawSql);
        if (isNull(this.whereClause)) {
            this.whereClause = rawPredicate;
        } else {
            this.whereClause = this.whereClause.and(rawPredicate);
        }
        return this;
    }

    /**
     * Add raw SQL AND condition with parameters.
     * <pre>{@code
     * .andRaw("status = :status", Map.of("status", "active"))
     * }</pre>
     */
    public SelectBuilder andRaw(String rawSql, java.util.Map<String, Object> params) {
        Predicate rawPredicate = new Predicate.ParameterizedRawPredicate(rawSql, params);
        if (isNull(this.whereClause)) {
            this.whereClause = rawPredicate;
        } else {
            this.whereClause = this.whereClause.and(rawPredicate);
        }
        return this;
    }

    /**
     * Add OR condition to WHERE clause.
     */
    public SelectBuilder or(Predicate predicate) {
        if (isNull(this.whereClause)) {
            this.whereClause = predicate;
        } else {
            this.whereClause = this.whereClause.or(predicate);
        }
        return this;
    }

    /**
     * Add grouped OR condition using closure (Laravel-style).
     * Conditions inside closure are grouped with parentheses.
     * <pre>{@code
     * .where(User_.TYPE.eq("PREMIUM"))
     * .or(q -> q.where(User_.CREDITS.gt(100)).and(User_.VERIFIED.eq(true)))
     * // SQL: WHERE type = 'PREMIUM' OR (credits > 100 AND verified = true)
     * }</pre>
     */
    public SelectBuilder or(Function<SelectBuilder, SelectBuilder> group) {
        SelectBuilder nested = new SelectBuilder(List.of());
        nested = group.apply(nested);
        Predicate groupedPredicate = nested.getWhereClause();
        if (nonNull(groupedPredicate)) {
            if (isNull(this.whereClause)) {
                this.whereClause = groupedPredicate;
            } else {
                this.whereClause = this.whereClause.or(groupedPredicate);
            }
        }
        return this;
    }

    /**
     * Add OR condition only if value is present (not null).
     */
    public <V> SelectBuilder orIfPresent(V value, Supplier<Predicate> predicateSupplier) {
        if (nonNull(value)) {
            or(predicateSupplier.get());
        }
        return this;
    }

    /**
     * Add raw SQL OR condition.
     * <pre>{@code
     * .orRaw("status = 'pending'")
     * }</pre>
     */
    public SelectBuilder orRaw(String rawSql) {
        Predicate rawPredicate = new Predicate.RawPredicate(rawSql);
        if (isNull(this.whereClause)) {
            this.whereClause = rawPredicate;
        } else {
            this.whereClause = this.whereClause.or(rawPredicate);
        }
        return this;
    }

    /**
     * Add raw SQL OR condition with parameters.
     * <pre>{@code
     * .orRaw("status = :status", Map.of("status", "pending"))
     * }</pre>
     */
    public SelectBuilder orRaw(String rawSql, Map<String, Object> params) {
        Predicate rawPredicate = new Predicate.ParameterizedRawPredicate(rawSql, params);
        if (isNull(this.whereClause)) {
            this.whereClause = rawPredicate;
        } else {
            this.whereClause = this.whereClause.or(rawPredicate);
        }
        return this;
    }

    // ==================== JOIN CLAUSE ====================

    /**
     * INNER JOIN another table.
     */
    public SelectBuilder join(Table<?> table, Predicate on) {
        joins.add(new JoinClause(JoinType.INNER, table, on));
        return this;
    }

    /**
     * LEFT JOIN another table.
     */
    public SelectBuilder leftJoin(Table<?> table, Predicate on) {
        joins.add(new JoinClause(JoinType.LEFT, table, on));
        return this;
    }

    /**
     * RIGHT JOIN another table.
     */
    public SelectBuilder rightJoin(Table<?> table, Predicate on) {
        joins.add(new JoinClause(JoinType.RIGHT, table, on));
        return this;
    }

    /**
     * Add raw JOIN clause.
     * <pre>{@code
     * .joinRaw("LEFT JOIN orders o ON o.user_id = users.id AND o.status = 'active'")
     * .joinRaw("CROSS JOIN LATERAL unnest(array_col) AS elem")
     * }</pre>
     */
    public SelectBuilder joinRaw(String rawJoinSql) {
        joins.add(new JoinClause(JoinType.RAW, null, new Predicate.RawPredicate(rawJoinSql)));
        return this;
    }

    // ==================== RELATIONSHIP JOINS ====================

    /**
     * LEFT JOIN using a Relation definition.
     * Automatically generates the proper JOIN clause based on relationship type.
     * <pre>{@code
     * // HasOne/HasMany: LEFT JOIN profiles ON profiles.user_id = users.id
     * .leftJoin(User_.PROFILE)
     *
     * // BelongsTo: LEFT JOIN users ON posts.user_id = users.id
     * .leftJoin(Post_.AUTHOR)
     *
     * // BelongsToMany: joins through pivot table
     * .leftJoin(User_.ROLES)
     * }</pre>
     */
    public SelectBuilder leftJoin(Relation<?, ?> relation) {
        addRelationJoin(relation, JoinType.LEFT);
        return this;
    }

    /**
     * INNER JOIN using a Relation definition.
     */
    public SelectBuilder join(Relation<?, ?> relation) {
        addRelationJoin(relation, JoinType.INNER);
        return this;
    }

    /**
     * RIGHT JOIN using a Relation definition.
     */
    public SelectBuilder rightJoin(Relation<?, ?> relation) {
        addRelationJoin(relation, JoinType.RIGHT);
        return this;
    }

    private void addRelationJoin(Relation<?, ?> relation, JoinType joinType) {
        switch (relation.getType()) {
            case HAS_ONE, HAS_MANY -> {
                // LEFT JOIN related ON related.fk = owner.localKey
                String onClause = relation.getRelatedTable().getName() + "." + relation.getForeignKey()
                        + " = " + relation.getOwnerTable().getName() + "." + relation.getLocalKey();
                joins.add(new JoinClause(joinType, relation.getRelatedTable(), new Predicate.RawPredicate(onClause)));
            }
            case BELONGS_TO -> {
                // LEFT JOIN related ON owner.fk = related.relatedKey
                String onClause = relation.getOwnerTable().getName() + "." + relation.getForeignKey()
                        + " = " + relation.getRelatedTable().getName() + "." + relation.getRelatedKey();
                joins.add(new JoinClause(joinType, relation.getRelatedTable(), new Predicate.RawPredicate(onClause)));
            }
            case BELONGS_TO_MANY -> {
                // Two-part join through pivot table
                // 1. JOIN pivot ON pivot.foreignPivotKey = owner.localKey
                String pivotOnClause = relation.getPivotTable() + "." + relation.getForeignPivotKey()
                        + " = " + relation.getOwnerTable().getName() + "." + relation.getLocalKey();
                joins.add(new JoinClause(JoinType.RAW, null,
                        new Predicate.RawPredicate(joinType.getSql() + " " + relation.getPivotTable() + " ON " + pivotOnClause)));

                // 2. JOIN related ON related.relatedKey = pivot.relatedPivotKey
                String relatedOnClause = relation.getRelatedTable().getName() + "." + relation.getRelatedKey()
                        + " = " + relation.getPivotTable() + "." + relation.getRelatedPivotKey();
                joins.add(new JoinClause(joinType, relation.getRelatedTable(), new Predicate.RawPredicate(relatedOnClause)));
            }
        }
    }

    // ==================== RELATIONSHIP QUERY METHODS ====================

    /**
     * Filter by existence of related records.
     * <pre>{@code
     * .whereHas(User_.POSTS)
     * // SQL: WHERE EXISTS (SELECT 1 FROM posts WHERE posts.user_id = users.id)
     * }</pre>
     */
    public SelectBuilder whereHas(Relation<?, ?> relation) {
        return whereHas(relation, null);
    }

    /**
     * Filter by existence of related records with additional constraints.
     * <pre>{@code
     * .whereHas(User_.POSTS, posts -> posts.where(Post_.IS_PUBLISHED.eq(true)))
     * // SQL: WHERE EXISTS (SELECT 1 FROM posts WHERE posts.user_id = users.id AND is_published = true)
     * }</pre>
     */
    public SelectBuilder whereHas(Relation<?, ?> relation, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        Predicate existsPredicate = new Predicate.RelationExistsPredicate(relation, constraint, false, ownerTable);

        if (isNull(this.whereClause)) {
            this.whereClause = existsPredicate;
        } else {
            this.whereClause = this.whereClause.and(existsPredicate);
        }
        return this;
    }

    /**
     * Filter by non-existence of related records.
     * <pre>{@code
     * .whereDoesntHave(User_.POSTS)
     * // SQL: WHERE NOT EXISTS (SELECT 1 FROM posts WHERE posts.user_id = users.id)
     * }</pre>
     */
    public SelectBuilder whereDoesntHave(Relation<?, ?> relation) {
        return whereDoesntHave(relation, null);
    }

    /**
     * Filter by non-existence of related records with additional constraints.
     */
    public SelectBuilder whereDoesntHave(Relation<?, ?> relation, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        Predicate notExistsPredicate = new Predicate.RelationExistsPredicate(relation, constraint, true, ownerTable);

        if (isNull(this.whereClause)) {
            this.whereClause = notExistsPredicate;
        } else {
            this.whereClause = this.whereClause.and(notExistsPredicate);
        }
        return this;
    }

    /**
     * Filter by count of related records.
     * <pre>{@code
     * .has(User_.POSTS, ">=", 5)
     * // SQL: WHERE (SELECT COUNT(*) FROM posts WHERE posts.user_id = users.id) >= 5
     * }</pre>
     */
    public SelectBuilder has(Relation<?, ?> relation, String operator, int count) {
        return has(relation, operator, count, null);
    }

    /**
     * Filter by count of related records with additional constraints.
     */
    public SelectBuilder has(Relation<?, ?> relation, String operator, int count, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        Predicate countPredicate = new Predicate.RelationCountPredicate(relation, operator, count, constraint, ownerTable);

        if (isNull(this.whereClause)) {
            this.whereClause = countPredicate;
        } else {
            this.whereClause = this.whereClause.and(countPredicate);
        }
        return this;
    }

    /**
     * Filter by having no related records.
     * <pre>{@code
     * .doesntHave(User_.POSTS)
     * // Equivalent to .has(User_.POSTS, "=", 0)
     * }</pre>
     */
    public SelectBuilder doesntHave(Relation<?, ?> relation) {
        return has(relation, "=", 0);
    }

    /**
     * Add a count subquery for a relationship to the SELECT clause.
     * <pre>{@code
     * .withCount(User_.POSTS)
     * // SQL: SELECT ..., (SELECT COUNT(*) FROM posts WHERE posts.user_id = users.id) AS posts_count
     * }</pre>
     */
    public SelectBuilder withCount(Relation<?, ?>... relations) {
        for (Relation<?, ?> relation : relations) {
            String alias = relation.getCountAlias(getRelationFieldName(relation));
            String ownerTable = getOwnerTableName(relation);
            this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.COUNT, relation, null, null, alias, ownerTable));
        }
        return this;
    }

    /**
     * Add a count subquery with constraint and custom alias.
     * <pre>{@code
     * .withCount(User_.POSTS, posts -> posts.where(Post_.IS_PUBLISHED.eq(true)), "published_count")
     * }</pre>
     */
    public SelectBuilder withCount(Relation<?, ?> relation, Function<SelectBuilder, SelectBuilder> constraint, String alias) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.COUNT, relation, null, constraint, alias, ownerTable));
        return this;
    }

    /**
     * Add a SUM subquery for a relationship column to the SELECT clause.
     * <pre>{@code
     * .withSum(User_.ORDERS, Order_.AMOUNT, "total_spent")
     * // SQL: SELECT ..., (SELECT SUM(amount) FROM orders WHERE orders.user_id = users.id) AS total_spent
     * }</pre>
     *
     * @param relation the relationship to aggregate
     * @param column   the column to sum
     * @param alias    the alias for the result
     */
    public SelectBuilder withSum(Relation<?, ?> relation, Column<?, ?> column, String alias) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.SUM, relation, column, null, alias, ownerTable));
        return this;
    }

    /**
     * Add a SUM subquery with constraint.
     */
    public SelectBuilder withSum(Relation<?, ?> relation, Column<?, ?> column, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.SUM, relation, column, constraint, alias, ownerTable));
        return this;
    }

    /**
     * Add an AVG subquery for a relationship column to the SELECT clause.
     * <pre>{@code
     * .withAvg(User_.RATINGS, Rating_.SCORE, "avg_rating")
     * // SQL: SELECT ..., (SELECT AVG(score) FROM ratings WHERE ratings.user_id = users.id) AS avg_rating
     * }</pre>
     */
    public SelectBuilder withAvg(Relation<?, ?> relation, Column<?, ?> column, String alias) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.AVG, relation, column, null, alias, ownerTable));
        return this;
    }

    /**
     * Add an AVG subquery with constraint.
     */
    public SelectBuilder withAvg(Relation<?, ?> relation, Column<?, ?> column, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.AVG, relation, column, constraint, alias, ownerTable));
        return this;
    }

    /**
     * Add a MIN subquery for a relationship column to the SELECT clause.
     * <pre>{@code
     * .withMin(User_.ORDERS, Order_.CREATED_AT, "first_order")
     * // SQL: SELECT ..., (SELECT MIN(created_at) FROM orders WHERE orders.user_id = users.id) AS first_order
     * }</pre>
     */
    public SelectBuilder withMin(Relation<?, ?> relation, Column<?, ?> column, String alias) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.MIN, relation, column, null, alias, ownerTable));
        return this;
    }

    /**
     * Add a MIN subquery with constraint.
     */
    public SelectBuilder withMin(Relation<?, ?> relation, Column<?, ?> column, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.MIN, relation, column, constraint, alias, ownerTable));
        return this;
    }

    /**
     * Add a MAX subquery for a relationship column to the SELECT clause.
     * <pre>{@code
     * .withMax(User_.ORDERS, Order_.AMOUNT, "largest_order")
     * // SQL: SELECT ..., (SELECT MAX(amount) FROM orders WHERE orders.user_id = users.id) AS largest_order
     * }</pre>
     */
    public SelectBuilder withMax(Relation<?, ?> relation, Column<?, ?> column, String alias) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.MAX, relation, column, null, alias, ownerTable));
        return this;
    }

    /**
     * Add a MAX subquery with constraint.
     */
    public SelectBuilder withMax(Relation<?, ?> relation, Column<?, ?> column, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.MAX, relation, column, constraint, alias, ownerTable));
        return this;
    }

    /**
     * Add an EXISTS subquery for a relationship to the SELECT clause.
     * <pre>{@code
     * .withExists(User_.POSTS, "has_posts")
     * // SQL: SELECT ..., EXISTS(SELECT 1 FROM posts WHERE posts.user_id = users.id) AS has_posts
     * }</pre>
     */
    public SelectBuilder withExists(Relation<?, ?> relation, String alias) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.EXISTS, relation, null, null, alias, ownerTable));
        return this;
    }

    /**
     * Add an EXISTS subquery with constraint.
     */
    public SelectBuilder withExists(Relation<?, ?> relation, String alias, Function<SelectBuilder, SelectBuilder> constraint) {
        String ownerTable = getOwnerTableName(relation);
        this.selectItems.add(SelectItem.subquery(SelectItem.SubqueryType.EXISTS, relation, null, constraint, alias, ownerTable));
        return this;
    }

    /**
     * Get a simple field name for a relation (used for alias generation).
     */
    private String getRelationFieldName(Relation<?, ?> relation) {
        // Use related table name as fallback
        return relation.getRelatedTable().getName();
    }

    /**
     * Get the owner table name for correlation in subqueries.
     */
    private String getOwnerTableName(Relation<?, ?> relation) {
        return nonNull(fromTable) ? fromTable.getName() : relation.getOwnerTable().getName();
    }

    // ==================== PIVOT OPERATIONS ====================

    /**
     * Filter by a pivot table column value.
     * <pre>{@code
     * .leftJoin(User_.ROLES)
     * .wherePivot(User_.ROLES, "is_primary", true)
     * // SQL: ... WHERE role_user.is_primary = ?
     * }</pre>
     *
     * @param relation BelongsToMany relation that has been joined
     * @param column   column name in the pivot table
     * @param value    value to filter by
     */
    public SelectBuilder wherePivot(Relation<?, ?> relation, String column, Object value) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivot can only be used with BelongsToMany relations");
        }
        String paramName = nextParamName();
        String pivotColumn = relation.getPivotTable() + "." + column;
        Predicate pivotPredicate = new Predicate.RawPredicate(pivotColumn + " = :" + paramName);
        parameters.put(paramName, value);

        if (isNull(this.whereClause)) {
            this.whereClause = pivotPredicate;
        } else {
            this.whereClause = this.whereClause.and(pivotPredicate);
        }
        return this;
    }

    /**
     * Filter by pivot column being IN a list of values.
     * <pre>{@code
     * .leftJoin(User_.ROLES)
     * .wherePivotIn(User_.ROLES, "role_type", List.of("admin", "moderator"))
     * // SQL: ... WHERE role_user.role_type IN (?, ?)
     * }</pre>
     */
    public SelectBuilder wherePivotIn(Relation<?, ?> relation, String column, List<?> values) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotIn can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        List<String> paramNames = new ArrayList<>();
        for (Object value : values) {
            String paramName = nextParamName();
            paramNames.add(":" + paramName);
            parameters.put(paramName, value);
        }
        String inClause = pivotColumn + " IN (" + String.join(", ", paramNames) + ")";
        Predicate pivotPredicate = new Predicate.RawPredicate(inClause);

        if (isNull(this.whereClause)) {
            this.whereClause = pivotPredicate;
        } else {
            this.whereClause = this.whereClause.and(pivotPredicate);
        }
        return this;
    }

    /**
     * Filter by pivot column being NOT IN a list of values.
     */
    public SelectBuilder wherePivotNotIn(Relation<?, ?> relation, String column, List<?> values) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotNotIn can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        List<String> paramNames = new ArrayList<>();
        for (Object value : values) {
            String paramName = nextParamName();
            paramNames.add(":" + paramName);
            parameters.put(paramName, value);
        }
        String inClause = pivotColumn + " NOT IN (" + String.join(", ", paramNames) + ")";
        Predicate pivotPredicate = new Predicate.RawPredicate(inClause);

        if (isNull(this.whereClause)) {
            this.whereClause = pivotPredicate;
        } else {
            this.whereClause = this.whereClause.and(pivotPredicate);
        }
        return this;
    }

    /**
     * Filter by pivot column being NULL.
     * <pre>{@code
     * .leftJoin(User_.ROLES)
     * .wherePivotNull(User_.ROLES, "revoked_at")
     * // SQL: ... WHERE role_user.revoked_at IS NULL
     * }</pre>
     */
    public SelectBuilder wherePivotNull(Relation<?, ?> relation, String column) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotNull can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        Predicate pivotPredicate = new Predicate.RawPredicate(pivotColumn + " IS NULL");

        if (isNull(this.whereClause)) {
            this.whereClause = pivotPredicate;
        } else {
            this.whereClause = this.whereClause.and(pivotPredicate);
        }
        return this;
    }

    /**
     * Filter by pivot column being NOT NULL.
     */
    public SelectBuilder wherePivotNotNull(Relation<?, ?> relation, String column) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotNotNull can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        Predicate pivotPredicate = new Predicate.RawPredicate(pivotColumn + " IS NOT NULL");

        if (isNull(this.whereClause)) {
            this.whereClause = pivotPredicate;
        } else {
            this.whereClause = this.whereClause.and(pivotPredicate);
        }
        return this;
    }

    /**
     * Filter by pivot column using comparison operators.
     * <pre>{@code
     * .leftJoin(User_.ROLES)
     * .wherePivotBetween(User_.ROLES, "assigned_at", startDate, endDate)
     * }</pre>
     */
    public SelectBuilder wherePivotBetween(Relation<?, ?> relation, String column, Object start, Object end) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("wherePivotBetween can only be used with BelongsToMany relations");
        }
        String startParam = nextParamName();
        String endParam = nextParamName();
        String pivotColumn = relation.getPivotTable() + "." + column;
        String betweenClause = pivotColumn + " BETWEEN :" + startParam + " AND :" + endParam;
        parameters.put(startParam, start);
        parameters.put(endParam, end);
        Predicate pivotPredicate = new Predicate.RawPredicate(betweenClause);

        if (isNull(this.whereClause)) {
            this.whereClause = pivotPredicate;
        } else {
            this.whereClause = this.whereClause.and(pivotPredicate);
        }
        return this;
    }

    /**
     * Order by a pivot table column.
     * <pre>{@code
     * .leftJoin(User_.ROLES)
     * .orderByPivot(User_.ROLES, "assigned_at", OrderDirection.DESC)
     * // SQL: ... ORDER BY role_user.assigned_at DESC
     * }</pre>
     */
    public SelectBuilder orderByPivot(Relation<?, ?> relation, String column, OrderDirection direction) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("orderByPivot can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        orderSpecs.add(OrderSpec.raw(pivotColumn + " " + direction.name()));
        return this;
    }

    /**
     * Select a pivot table column.
     * <pre>{@code
     * .leftJoin(User_.ROLES)
     * .selectPivot(User_.ROLES, "assigned_at", "assigned_date")
     * // SQL: SELECT ..., role_user.assigned_at AS assigned_date
     * }</pre>
     */
    public SelectBuilder selectPivot(Relation<?, ?> relation, String column, String alias) {
        if (!relation.usesPivotTable()) {
            throw new IllegalArgumentException("selectPivot can only be used with BelongsToMany relations");
        }
        String pivotColumn = relation.getPivotTable() + "." + column;
        this.selectItems.add(SelectItem.raw(pivotColumn + " AS " + alias));
        return this;
    }

    /**
     * Select a pivot table column without alias.
     */
    public SelectBuilder selectPivot(Relation<?, ?> relation, String column) {
        return selectPivot(relation, column, column);
    }

    // ==================== ORDER BY ====================

    /**
     * Add ORDER BY specification.
     */
    public SelectBuilder orderBy(OrderSpec... specs) {
        orderSpecs.addAll(Arrays.asList(specs));
        return this;
    }

    /**
     * Clear all ORDER BY clauses.
     */
    public SelectBuilder clearOrders() {
        orderSpecs.clear();
        return this;
    }

    // ==================== GROUP BY ====================

    /**
     * Add GROUP BY columns.
     */
    public SelectBuilder groupBy(Column<?, ?>... columns) {
        for (Column<?, ?> col : columns) {
            groupByItems.add(GroupByItem.of(col));
        }
        return this;
    }

    /**
     * Add GROUP BY expressions.
     * <pre>{@code
     * .groupByExpression(User_.META_DATA.jsonPath("feedback", "type"))
     * }</pre>
     */
    public SelectBuilder groupByExpression(Expression<?>... expressions) {
        for (Expression<?> expr : expressions) {
            groupByItems.add(GroupByItem.of(expr));
        }
        return this;
    }

    /**
     * Add raw GROUP BY expression.
     * <pre>{@code
     * .groupByRaw("DATE_TRUNC('month', created_at)")
     * .groupByRaw("ROLLUP(category, subcategory)")
     * }</pre>
     */
    public SelectBuilder groupByRaw(String rawSql) {
        groupByItems.add(GroupByItem.raw(rawSql));
        return this;
    }

    /**
     * Add HAVING clause.
     */
    public SelectBuilder having(Predicate predicate) {
        this.havingClause = predicate;
        return this;
    }

    /**
     * Add raw SQL HAVING condition.
     */
    public SelectBuilder havingRaw(String rawSql) {
        Predicate rawPredicate = new Predicate.RawPredicate(rawSql);
        if (isNull(this.havingClause)) {
            this.havingClause = rawPredicate;
        } else {
            this.havingClause = this.havingClause.and(rawPredicate);
        }
        return this;
    }

    /**
     * Add raw SQL HAVING condition with parameters.
     */
    public SelectBuilder havingRaw(String rawSql, Map<String, Object> params) {
        Predicate rawPredicate = new Predicate.ParameterizedRawPredicate(rawSql, params);
        if (isNull(this.havingClause)) {
            this.havingClause = rawPredicate;
        } else {
            this.havingClause = this.havingClause.and(rawPredicate);
        }
        return this;
    }

    /**
     * Add raw SQL OR HAVING condition.
     */
    public SelectBuilder orHavingRaw(String rawSql) {
        Predicate rawPredicate = new Predicate.RawPredicate(rawSql);
        if (isNull(this.havingClause)) {
            this.havingClause = rawPredicate;
        } else {
            this.havingClause = this.havingClause.or(rawPredicate);
        }
        return this;
    }

    /**
     * Add raw SQL OR HAVING condition with parameters.
     */
    public SelectBuilder orHavingRaw(String rawSql, Map<String, Object> params) {
        Predicate rawPredicate = new Predicate.ParameterizedRawPredicate(rawSql, params);
        if (isNull(this.havingClause)) {
            this.havingClause = rawPredicate;
        } else {
            this.havingClause = this.havingClause.or(rawPredicate);
        }
        return this;
    }

    // ==================== LIMIT / OFFSET ====================

    /**
     * Set LIMIT clause.
     */
    public SelectBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Set OFFSET clause.
     */
    public SelectBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Set pagination (page number and page size).
     * Page numbers are 1-based.
     * <pre>{@code
     * .paginate(1, 20)  // Page 1, 20 items per page -> LIMIT 20 OFFSET 0
     * .paginate(3, 20)  // Page 3, 20 items per page -> LIMIT 20 OFFSET 40
     * }</pre>
     */
    public SelectBuilder paginate(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        this.limit = pageSize;
        this.offset = (page - 1) * pageSize;
        return this;
    }

    // ==================== CONDITIONAL WHERE ====================

    /**
     * Add WHERE condition only if boolean condition is true.
     * <pre>{@code
     * .whereIf(includeActive, User_.IS_ACTIVE.eq(true))
     * }</pre>
     */
    public SelectBuilder whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return this;
    }

    /**
     * Add AND condition only if boolean condition is true.
     */
    public SelectBuilder andIf(boolean condition, Predicate predicate) {
        if (condition) {
            and(predicate);
        }
        return this;
    }

    // ==================== SUBQUERY SUPPORT ====================

    /**
     * Add WHERE EXISTS (subquery).
     * <pre>{@code
     * .whereExists(Suprim.select(Order_.ID).from(Order_.TABLE).where(Order_.USER_ID.eq(User_.ID)))
     * }</pre>
     */
    public SelectBuilder whereExists(SelectBuilder subquery) {
        this.whereClause = SubqueryExpression.exists(subquery);
        return this;
    }

    /**
     * Add WHERE NOT EXISTS (subquery).
     */
    public SelectBuilder whereNotExists(SelectBuilder subquery) {
        this.whereClause = SubqueryExpression.notExists(subquery);
        return this;
    }

    /**
     * Add AND EXISTS (subquery).
     */
    public SelectBuilder andExists(SelectBuilder subquery) {
        if (isNull(this.whereClause)) {
            this.whereClause = SubqueryExpression.exists(subquery);
        } else {
            this.whereClause = this.whereClause.and(SubqueryExpression.exists(subquery));
        }
        return this;
    }

    /**
     * Add AND NOT EXISTS (subquery).
     */
    public SelectBuilder andNotExists(SelectBuilder subquery) {
        if (isNull(this.whereClause)) {
            this.whereClause = SubqueryExpression.notExists(subquery);
        } else {
            this.whereClause = this.whereClause.and(SubqueryExpression.notExists(subquery));
        }
        return this;
    }

    /**
     * Add OR EXISTS (subquery) to WHERE clause.
     * <pre>{@code
     * .where(User_.IS_ACTIVE.eq(true))
     * .orExists(Suprim.selectRaw("1").from(Order_.TABLE).whereRaw("user_id = users.id"))
     * // SQL: WHERE is_active = true OR EXISTS (SELECT 1 FROM orders WHERE ...)
     * }</pre>
     */
    public SelectBuilder orExists(SelectBuilder subquery) {
        if (isNull(this.whereClause)) {
            this.whereClause = SubqueryExpression.exists(subquery);
        } else {
            this.whereClause = this.whereClause.or(SubqueryExpression.exists(subquery));
        }
        return this;
    }

    /**
     * Add OR NOT EXISTS (subquery) to WHERE clause.
     * <pre>{@code
     * .where(User_.IS_ADMIN.eq(true))
     * .orNotExists(Suprim.selectRaw("1").from(Ban_.TABLE).whereRaw("user_id = users.id"))
     * // SQL: WHERE is_admin = true OR NOT EXISTS (SELECT 1 FROM bans WHERE ...)
     * }</pre>
     */
    public SelectBuilder orNotExists(SelectBuilder subquery) {
        if (isNull(this.whereClause)) {
            this.whereClause = SubqueryExpression.notExists(subquery);
        } else {
            this.whereClause = this.whereClause.or(SubqueryExpression.notExists(subquery));
        }
        return this;
    }

    /**
     * Add WHERE column IN (subquery).
     * <pre>{@code
     * .whereInSubquery(User_.ID, Suprim.select(Order_.USER_ID).from(Order_.TABLE))
     * }</pre>
     */
    public SelectBuilder whereInSubquery(Column<?, ?> column, SelectBuilder subquery) {
        SubqueryExpression<?> subExpr = new SubqueryExpression<>(subquery, Object.class);
        this.whereClause = new Predicate.SimplePredicate(column, Operator.IN, subExpr);
        return this;
    }

    /**
     * Add WHERE column NOT IN (subquery).
     */
    public SelectBuilder whereNotInSubquery(Column<?, ?> column, SelectBuilder subquery) {
        SubqueryExpression<?> subExpr = new SubqueryExpression<>(subquery, Object.class);
        this.whereClause = new Predicate.SimplePredicate(column, Operator.NOT_IN, subExpr);
        return this;
    }

    // ==================== CTE SUPPORT ====================

    /**
     * Add WITH clause (Common Table Expression).
     * <pre>{@code
     * .with("active_users", Suprim.select(User_.ID).from(User_.TABLE).where(User_.IS_ACTIVE.eq(true)))
     * .from(Table.of("active_users"))
     * }</pre>
     */
    public SelectBuilder with(String name, SelectBuilder subquery) {
        ctes.add(new CteClause(name, subquery));
        return this;
    }

    /**
     * Add WITH clause using raw SQL.
     */
    public SelectBuilder with(String name, String rawSql) {
        ctes.add(new CteClause(name, rawSql));
        return this;
    }

    /**
     * Add WITH RECURSIVE clause.
     */
    public SelectBuilder withRecursive(String name, SelectBuilder subquery) {
        this.recursive = true;
        ctes.add(new CteClause(name, subquery));
        return this;
    }

    /**
     * Add WITH RECURSIVE clause using raw SQL.
     */
    public SelectBuilder withRecursive(String name, String rawSql) {
        this.recursive = true;
        ctes.add(new CteClause(name, rawSql));
        return this;
    }

    // ==================== SOFT DELETE SCOPE ====================

    /**
     * Include soft-deleted records in query results.
     * By default, queries on entities with @SoftDeletes exclude deleted records.
     * Use this to include them.
     *
     * <pre>{@code
     * // Include all users, even soft-deleted ones
     * Suprim.select(User_.ID, User_.EMAIL)
     *     .from(User_.TABLE)
     *     .withTrashed()
     *     .build();
     * }</pre>
     *
     * @return this builder for chaining
     */
    public SelectBuilder withTrashed() {
        this.softDeleteScope = SoftDeleteScope.WITH_TRASHED;
        return this;
    }

    /**
     * Only return soft-deleted records.
     * Queries only records where deleted_at IS NOT NULL.
     *
     * <pre>{@code
     * // Get only soft-deleted users
     * Suprim.select(User_.ID, User_.EMAIL)
     *     .from(User_.TABLE)
     *     .onlyTrashed()
     *     .build();
     * }</pre>
     *
     * @return this builder for chaining
     */
    public SelectBuilder onlyTrashed() {
        this.softDeleteScope = SoftDeleteScope.ONLY_TRASHED;
        return this;
    }

    /**
     * Get the current soft delete scope.
     * Used by executors to apply soft delete filtering.
     *
     * @return the soft delete scope
     */
    public SoftDeleteScope getSoftDeleteScope() {
        return softDeleteScope;
    }

    /**
     * Get the FROM table.
     * Used by executors to determine entity type for soft delete filtering.
     *
     * @return the from table, or null if not set
     */
    public Table<?> getFromTable() {
        return fromTable;
    }

    /**
     * Get the entity class from the FROM table.
     * Used by executors to check for @SoftDeletes annotation.
     *
     * @return the entity class, or null if from table not set
     */
    public Class<?> getEntityType() {
        return nonNull(fromTable) ? fromTable.getEntityType() : null;
    }

    // ==================== EAGER LOADING ====================

    /**
     * Eager load one or more relations to prevent N+1 queries.
     * <pre>{@code
     * .with(User_.PROFILE)
     * .with(User_.POSTS, User_.ROLES)
     * }</pre>
     */
    public SelectBuilder with(Relation<?, ?>... relations) {
        for (Relation<?, ?> relation : relations) {
            eagerLoads.add(EagerLoadSpec.of(relation));
        }
        return this;
    }

    /**
     * Eager load a relation with a constraint applied.
     * <pre>{@code
     * .with(User_.POSTS, posts -> posts
     *     .where(Post_.IS_PUBLISHED.eq(true))
     *     .orderBy(Post_.CREATED_AT.desc())
     *     .limit(5))
     * }</pre>
     */
    public SelectBuilder with(Relation<?, ?> relation, Function<SelectBuilder, SelectBuilder> constraint) {
        eagerLoads.add(EagerLoadSpec.of(relation, constraint));
        return this;
    }

    /**
     * Eager load relations using string path syntax for nested loading.
     * Supports dot-notation for deep nesting.
     * <pre>{@code
     * // Single level
     * .with("posts")
     *
     * // Nested 2 levels
     * .with("posts.comments")
     *
     * // Nested 3+ levels
     * .with("posts.comments.author")
     *
     * // Multiple paths
     * .with("posts.comments", "profile.settings")
     * }</pre>
     *
     * @param paths dot-notation paths to eager load
     * @return this builder
     * @throws IllegalArgumentException if path is invalid or relation not found
     */
    public SelectBuilder with(String... paths) {
        if (Objects.isNull(fromTable)) {
            throw new IllegalStateException("Cannot resolve string paths without FROM table. Call .from() first.");
        }

        Class<?> rootEntity = fromTable.getEntityType();
        for (String path : paths) {
            EagerLoadSpec spec = PathResolver.resolve(path, rootEntity);
            eagerLoads.add(spec);
        }
        return this;
    }

    /**
     * Eager load a typed relation with nested string path.
     * Combines type-safe parent relation with string-based nested path.
     * <pre>{@code
     * // Load posts, then their comments
     * .with(User_.POSTS, "comments")
     *
     * // Load posts, then comments and their authors
     * .with(User_.POSTS, "comments.author")
     * }</pre>
     *
     * @param relation   typed relation to load
     * @param nestedPath dot-notation path for nested relations
     * @return this builder
     */
    public SelectBuilder with(Relation<?, ?> relation, String nestedPath) {
        EagerLoadSpec spec = PathResolver.resolveNested(relation, nestedPath);
        eagerLoads.add(spec);
        return this;
    }

    /**
     * Eager load with string path and constraint on the final relation.
     * <pre>{@code
     * .with("posts.comments", comments -> comments
     *     .where(Comment_.IS_APPROVED.eq(true))
     *     .limit(10))
     * }</pre>
     *
     * @param path       dot-notation path
     * @param constraint constraint to apply to the final relation
     * @return this builder
     */
    public SelectBuilder with(String path, Function<SelectBuilder, SelectBuilder> constraint) {
        if (Objects.isNull(fromTable)) {
            throw new IllegalStateException("Cannot resolve string paths without FROM table. Call .from() first.");
        }

        Class<?> rootEntity = fromTable.getEntityType();
        EagerLoadSpec spec = PathResolver.resolve(path, rootEntity, constraint);
        eagerLoads.add(spec);
        return this;
    }

    /**
     * Eager load typed relation with nested path and constraint.
     * <pre>{@code
     * .with(User_.POSTS, "comments", comments -> comments
     *     .where(Comment_.IS_APPROVED.eq(true)))
     * }</pre>
     *
     * @param relation   typed relation
     * @param nestedPath nested path
     * @param constraint constraint to apply
     * @return this builder
     */
    public SelectBuilder with(Relation<?, ?> relation, String nestedPath, Function<SelectBuilder, SelectBuilder> constraint) {
        EagerLoadSpec spec = PathResolver.resolveNested(relation, nestedPath, constraint);
        eagerLoads.add(spec);
        return this;
    }

    /**
     * Exclude default eager loads specified in @Entity(with = {...}).
     * Use this to skip specific relations that would normally be auto-loaded.
     * <pre>{@code
     * // User entity has @Entity(with = {"profile", "settings"})
     * // This query excludes profile from default loads
     * .without(User_.PROFILE)
     * // Only settings will be loaded (from defaults), profile is excluded
     * }</pre>
     *
     * @param relations relations to exclude from default eager loading
     * @return this builder
     */
    public SelectBuilder without(Relation<?, ?>... relations) {
        for (Relation<?, ?> relation : relations) {
            withoutRelations.add(relation.getFieldName());
        }
        return this;
    }

    /**
     * Exclude default eager loads using string field names.
     * <pre>{@code
     * .without("profile", "settings")
     * }</pre>
     *
     * @param fieldNames field names to exclude from default eager loading
     * @return this builder
     */
    public SelectBuilder without(String... fieldNames) {
        withoutRelations.addAll(Arrays.asList(fieldNames));
        return this;
    }

    /**
     * Get the eager load specifications.
     * Used internally by the executor to load relations.
     */
    public List<EagerLoadSpec> getEagerLoads() {
        return new ArrayList<>(eagerLoads);
    }

    /**
     * Get the set of relations excluded from default eager loading.
     * Used internally by the executor.
     */
    public Set<String> getWithoutRelations() {
        return new HashSet<>(withoutRelations);
    }

    // ==================== SET OPERATIONS ====================

    /**
     * UNION with another query.
     */
    public SelectBuilder union(SelectBuilder other) {
        setOperations.add(new SetOperation("UNION", other));
        return this;
    }

    /**
     * UNION ALL with another query.
     */
    public SelectBuilder unionAll(SelectBuilder other) {
        setOperations.add(new SetOperation("UNION ALL", other));
        return this;
    }

    /**
     * INTERSECT with another query.
     */
    public SelectBuilder intersect(SelectBuilder other) {
        setOperations.add(new SetOperation("INTERSECT", other));
        return this;
    }

    /**
     * EXCEPT with another query.
     */
    public SelectBuilder except(SelectBuilder other) {
        setOperations.add(new SetOperation("EXCEPT", other));
        return this;
    }

    // ==================== ROW LOCKING ====================

    /**
     * Add FOR UPDATE lock (PostgreSQL).
     * Locks selected rows for update.
     */
    public SelectBuilder forUpdate() {
        this.lockMode = "FOR UPDATE";
        return this;
    }

    /**
     * Add FOR UPDATE NOWAIT lock.
     * Fails immediately if lock cannot be acquired.
     */
    public SelectBuilder forUpdateNowait() {
        this.lockMode = "FOR UPDATE NOWAIT";
        return this;
    }

    /**
     * Add FOR UPDATE SKIP LOCKED.
     * Skips rows that are already locked.
     */
    public SelectBuilder forUpdateSkipLocked() {
        this.lockMode = "FOR UPDATE SKIP LOCKED";
        return this;
    }

    /**
     * Add FOR SHARE lock (PostgreSQL).
     * Allows concurrent reads but prevents updates.
     */
    public SelectBuilder forShare() {
        this.lockMode = "FOR SHARE";
        return this;
    }

    /**
     * Add FOR SHARE NOWAIT lock.
     * Fails immediately if lock cannot be acquired.
     */
    public SelectBuilder forShareNowait() {
        this.lockMode = "FOR SHARE NOWAIT";
        return this;
    }

    /**
     * Add FOR SHARE SKIP LOCKED.
     * Skips rows that are already locked.
     */
    public SelectBuilder forShareSkipLocked() {
        this.lockMode = "FOR SHARE SKIP LOCKED";
        return this;
    }

    // ==================== BUILD ====================

    /**
     * Merge default eager loads from @Entity(with = {...}) annotation.
     * Called automatically by build() to apply entity-level default relations.
     * Only adds defaults that are NOT explicitly excluded via .without().
     */
    private void mergeDefaultEagerLoads() {
        if (Objects.isNull(fromTable) || Objects.isNull(fromTable.getEntityType())
                || fromTable.getEntityType() == Object.class) {
            return;
        }

        // Get defaults from @Entity annotation
        String[] defaults = Suprim.defaultEagerLoads(fromTable.getEntityType());
        if (defaults.length == 0) {
            return;
        }

        // Check which defaults are already explicitly loaded
        Set<String> explicitlyLoaded = eagerLoads.stream()
                .map(spec -> spec.relation().getFieldName())
                .collect(Collectors.toSet());

        // Add defaults that are not excluded and not already loaded
        for (String relationName : defaults) {
            if (!withoutRelations.contains(relationName) && !explicitlyLoaded.contains(relationName)) {
                try {
                    EagerLoadSpec spec = PathResolver.resolve(relationName, fromTable.getEntityType());
                    eagerLoads.add(spec);
                } catch (Exception e) {
                    // Silently skip invalid relation names - they'll be caught during validation
                }
            }
        }
    }

    /**
     * Get the current WHERE clause predicate.
     * Used by deferred predicate types for dialect-aware SQL generation.
     */
    public Predicate getWhereClause() {
        return whereClause;
    }

    /**
     * Apply soft delete filter based on entity's @SoftDeletes annotation and current scope.
     * Called automatically during build().
     *
     * - DEFAULT scope: Adds WHERE deleted_at IS NULL (if entity has @SoftDeletes)
     * - WITH_TRASHED scope: No filter applied
     * - ONLY_TRASHED scope: Adds WHERE deleted_at IS NOT NULL
     */
    private void applySoftDeleteFilter(SqlDialect dialect) {
        // Skip if no from table or entity type
        if (isNull(fromTable) || isNull(fromTable.getEntityType())) {
            return;
        }

        // Check if entity has @SoftDeletes annotation
        Class<?> entityClass = fromTable.getEntityType();
        SoftDeletes softDeletes = entityClass.getAnnotation(SoftDeletes.class);
        if (isNull(softDeletes)) {
            return; // Entity doesn't support soft deletes
        }

        String columnName = softDeletes.column();
        String qualifiedColumn = fromTable.getName() + "." + columnName;

        switch (softDeleteScope) {
            case DEFAULT -> {
                // Exclude soft-deleted records: WHERE deleted_at IS NULL
                Predicate softDeletePredicate = new Predicate.RawPredicate(
                        dialect.quoteIdentifier(fromTable.getName()) + "." +
                        dialect.quoteIdentifier(columnName) + " IS NULL"
                );
                if (isNull(this.whereClause)) {
                    this.whereClause = softDeletePredicate;
                } else {
                    this.whereClause = this.whereClause.and(softDeletePredicate);
                }
            }
            case WITH_TRASHED -> {
                // Include all records - no filter needed
            }
            case ONLY_TRASHED -> {
                // Only soft-deleted records: WHERE deleted_at IS NOT NULL
                Predicate onlyTrashedPredicate = new Predicate.RawPredicate(
                        dialect.quoteIdentifier(fromTable.getName()) + "." +
                        dialect.quoteIdentifier(columnName) + " IS NOT NULL"
                );
                if (isNull(this.whereClause)) {
                    this.whereClause = onlyTrashedPredicate;
                } else {
                    this.whereClause = this.whereClause.and(onlyTrashedPredicate);
                }
            }
        }
    }

    /**
     * Build the query using default PostgreSQL dialect.
     */
    public QueryResult build() {
        return build(PostgreSqlDialect.INSTANCE);
    }

    /**
     * Build the query using specified dialect.
     */
    public QueryResult build(SqlDialect dialect) {
        // Apply default eager loads from @Entity(with = {...}) annotation
        mergeDefaultEagerLoads();

        // Apply soft delete filter based on entity's @SoftDeletes annotation
        applySoftDeleteFilter(dialect);

        // Create parameter context for collecting WHERE/HAVING parameters
        ParameterContext paramContext = new ParameterContext();

        StringBuilder sql = new StringBuilder();

        // CTEs (WITH clause)
        if (!ctes.isEmpty()) {
            sql.append("WITH ");
            if (recursive) {
                sql.append("RECURSIVE ");
            }
            sql.append(ctes.stream()
                    .map(cte -> cte.toSql(dialect))
                    .collect(Collectors.joining(", ")));
            sql.append(" ");
        }

        // SELECT
        sql.append("SELECT ");
        if (distinct) {
            sql.append("DISTINCT ");
        }

        if (selectItems.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(selectItems.stream()
                    .map(item -> item.toSql(dialect))
                    .collect(Collectors.joining(", ")));
        }

        // FROM
        if (nonNull(fromTable)) {
            sql.append(" FROM ").append(fromTable.toSql(dialect));
        }

        // JOINs (use parameterized predicates)
        for (JoinClause join : joins) {
            if (join.type() == JoinType.RAW) {
                // Raw join - the predicate contains the full join SQL
                sql.append(" ").append(join.on().toSql(dialect, paramContext));
            } else {
                sql.append(" ").append(join.type().getSql())
                        .append(" ").append(join.table().toSql(dialect))
                        .append(" ON ").append(join.on().toSql(dialect, paramContext));
            }
        }

        // WHERE (use parameterized predicates)
        if (nonNull(whereClause)) {
            sql.append(" WHERE ").append(whereClause.toSql(dialect, paramContext));
        }

        // GROUP BY
        if (!groupByItems.isEmpty()) {
            sql.append(" GROUP BY ");
            sql.append(groupByItems.stream()
                    .map(item -> item.toSql(dialect))
                    .collect(Collectors.joining(", ")));
        }

        // HAVING (use parameterized predicates)
        if (nonNull(havingClause)) {
            sql.append(" HAVING ").append(havingClause.toSql(dialect, paramContext));
        }

        // SET OPERATIONS (UNION, INTERSECT, EXCEPT)
        for (SetOperation setOp : setOperations) {
            sql.append(" ").append(setOp.operator()).append(" ").append(setOp.other().build(dialect).sql());
        }

        // ORDER BY
        if (!orderSpecs.isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(orderSpecs.stream()
                    .map(o -> o.toSql(dialect))
                    .collect(Collectors.joining(", ")));
        }

        // LIMIT
        if (nonNull(limit)) {
            sql.append(" LIMIT ").append(limit);
        }

        // OFFSET (skip if 0)
        if (nonNull(offset) && offset > 0) {
            sql.append(" OFFSET ").append(offset);
        }

        // ROW LOCKING
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

        // Merge predicate parameters with existing parameters
        Map<String, Object> allParams = new LinkedHashMap<>(parameters);
        allParams.putAll(paramContext.getParameters());

        return new QueryResult(sql.toString(), allParams, new ArrayList<>(eagerLoads), softDeleteScope);
    }

    private String nextParamName() {
        return "p" + (++paramCounter);
    }

    // ==================== INNER TYPES ====================

    private enum JoinType {
        INNER("JOIN"),
        LEFT("LEFT JOIN"),
        RIGHT("RIGHT JOIN"),
        FULL("FULL JOIN"),
        CROSS("CROSS JOIN"),
        RAW("");

        private final String sql;

        JoinType(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    private record JoinClause(JoinType type, Table<?> table, Predicate on) {
    }

    private static final class CteClause {
        private final String name;
        private final SelectBuilder subquery;
        private final String rawSql;

        CteClause(String name, SelectBuilder subquery) {
            this.name = name;
            this.subquery = subquery;
            this.rawSql = null;
        }

        CteClause(String name, String rawSql) {
            this.name = name;
            this.subquery = null;
            this.rawSql = rawSql;
        }

        String toSql(SqlDialect dialect) {
            if (nonNull(rawSql)) {
                return name + " AS (" + rawSql + ")";
            }
            return name + " AS (" + subquery.build(dialect).sql() + ")";
        }
    }

    private record SetOperation(String operator, SelectBuilder other) {
    }
}
