package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.dialect.SqlDialect;
import sant1ago.dev.suprim.core.query.select.*;
import sant1ago.dev.suprim.core.type.Column;
import sant1ago.dev.suprim.core.type.Expression;
import sant1ago.dev.suprim.core.type.OrderDirection;
import sant1ago.dev.suprim.core.type.OrderSpec;
import sant1ago.dev.suprim.core.type.Predicate;
import sant1ago.dev.suprim.core.type.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Fluent builder for SELECT queries.
 * Implements mixin interfaces for modular query building capabilities.
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
public final class SelectBuilder implements
        SelectBuilderCore,
        WhereClauseSupport,
        JoinClauseSupport,
        RelationshipQuerySupport,
        PivotQuerySupport,
        EagerLoadSupport,
        GroupByHavingSupport,
        SubquerySupport,
        SetOperationSupport,
        LockingSupport,
        SoftDeleteSupport {

    // ==================== STATE ====================

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
    private final List<CteClause> ctes = new ArrayList<>();
    private boolean recursive = false;
    private final List<SetOperation> setOperations = new ArrayList<>();
    private String lockMode = null;
    private final List<EagerLoadSpec> eagerLoads = new ArrayList<>();
    private final Set<String> withoutRelations = new HashSet<>();
    private SoftDeleteScope softDeleteScope = SoftDeleteScope.DEFAULT;

    // ==================== CONSTRUCTOR ====================

    public SelectBuilder(List<? extends Expression<?>> expressions) {
        Objects.requireNonNull(expressions, "expressions cannot be null");
        for (Expression<?> expr : expressions) {
            this.selectItems.add(SelectItem.of(expr));
        }
    }

    // ==================== SELECT CLAUSE ====================

    /**
     * Add additional columns/expressions to SELECT clause.
     */
    public SelectBuilder select(Expression<?>... expressions) {
        for (Expression<?> expr : expressions) {
            this.selectItems.add(SelectItem.of(expr));
        }
        return this;
    }

    /**
     * Add raw SQL expression to SELECT clause.
     */
    public SelectBuilder selectRaw(String rawSql) {
        this.selectItems.add(SelectItem.raw(rawSql));
        return this;
    }

    /**
     * Select all columns (*).
     */
    public SelectBuilder selectAll() {
        this.selectItems.clear();
        return this;
    }

    /**
     * Apply DISTINCT modifier to SELECT.
     */
    public SelectBuilder distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * Add column conditionally to SELECT clause.
     */
    public SelectBuilder selectIf(boolean condition, Column<?, ?> column) {
        if (condition) {
            this.selectItems.add(SelectItem.of(column));
        }
        return this;
    }

    /**
     * Add raw SQL expression to SELECT clause conditionally.
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
     * Add COUNT(column) with filter to SELECT clause.
     */
    public SelectBuilder selectCountFilter(Column<?, ?> column, Predicate filter, String alias) {
        this.selectItems.add(SelectItem.countFilter(column, filter, alias));
        return this;
    }

    /**
     * Add column with alias to SELECT clause.
     */
    public SelectBuilder selectAs(Column<?, ?> column, String alias) {
        this.selectItems.add(SelectItem.of(column.as(alias)));
        return this;
    }

    // ==================== FROM CLAUSE ====================

    /**
     * Set the FROM table.
     */
    public SelectBuilder from(Table<?> table) {
        this.fromTable = table;
        return this;
    }

    // ==================== ORDER BY ====================

    /**
     * Add ORDER BY clause.
     */
    public SelectBuilder orderBy(OrderSpec... specs) {
        for (OrderSpec spec : specs) {
            this.orderSpecs.add(spec);
        }
        return this;
    }

    /**
     * Add ORDER BY with direction.
     */
    public SelectBuilder orderBy(Expression<?> expr, OrderDirection direction) {
        this.orderSpecs.add(OrderSpec.of(expr, direction));
        return this;
    }

    /**
     * Add raw ORDER BY expression.
     */
    public SelectBuilder orderByRaw(String rawSql) {
        this.orderSpecs.add(OrderSpec.raw(rawSql));
        return this;
    }

    /**
     * Clear all ORDER BY clauses.
     */
    public SelectBuilder clearOrders() {
        this.orderSpecs.clear();
        return this;
    }

    // ==================== LIMIT/OFFSET ====================

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
     */
    public SelectBuilder paginate(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        this.limit = pageSize;
        this.offset = (page - 1) * pageSize;
        return this;
    }

    // ==================== CTE SUPPORT ====================

    /**
     * Add a CTE (Common Table Expression).
     */
    public SelectBuilder with(String name, SelectBuilder subquery) {
        this.ctes.add(new CteClause(name, subquery));
        return this;
    }

    /**
     * Add a CTE using raw SQL.
     */
    public SelectBuilder with(String name, String rawSql) {
        this.ctes.add(new CteClause(name, rawSql));
        return this;
    }

    /**
     * Add a raw CTE.
     */
    public SelectBuilder withRaw(String name, String rawSql) {
        this.ctes.add(new CteClause(name, rawSql));
        return this;
    }

    /**
     * Mark CTEs as recursive.
     */
    public SelectBuilder withRecursive() {
        this.recursive = true;
        return this;
    }

    /**
     * Add a WITH RECURSIVE clause using a subquery.
     */
    public SelectBuilder withRecursive(String name, SelectBuilder subquery) {
        this.recursive = true;
        this.ctes.add(new CteClause(name, subquery));
        return this;
    }

    /**
     * Add a WITH RECURSIVE clause using raw SQL.
     */
    public SelectBuilder withRecursive(String name, String rawSql) {
        this.recursive = true;
        this.ctes.add(new CteClause(name, rawSql));
        return this;
    }

    // ==================== BUILD ====================

    /**
     * Build the query using default PostgreSQL dialect.
     */
    public QueryResult build() {
        return build(sant1ago.dev.suprim.core.dialect.PostgreSqlDialect.INSTANCE);
    }

    /**
     * Build the query for the specified SQL dialect.
     */
    public QueryResult build(SqlDialect dialect) {
        return SelectQueryRenderer.render(this, dialect);
    }

    // ==================== SelectBuilderCore IMPLEMENTATION ====================

    @Override
    public SelectBuilder self() {
        return this;
    }

    @Override
    public String nextParamName() {
        return "p" + (++paramCounter);
    }

    @Override
    public Predicate whereClause() {
        return whereClause;
    }

    @Override
    public void whereClause(Predicate predicate) {
        this.whereClause = predicate;
    }

    @Override
    public List<JoinClause> joins() {
        return joins;
    }

    @Override
    public List<SelectItem> selectItems() {
        return selectItems;
    }

    @Override
    public List<OrderSpec> orderSpecs() {
        return orderSpecs;
    }

    @Override
    public List<GroupByItem> groupByItems() {
        return groupByItems;
    }

    @Override
    public Predicate havingClause() {
        return havingClause;
    }

    @Override
    public void havingClause(Predicate predicate) {
        this.havingClause = predicate;
    }

    @Override
    public Table<?> fromTable() {
        return fromTable;
    }

    @Override
    public Map<String, Object> parameters() {
        return parameters;
    }

    @Override
    public List<EagerLoadSpec> eagerLoads() {
        return eagerLoads;
    }

    @Override
    public Set<String> withoutRelations() {
        return withoutRelations;
    }

    @Override
    public List<CteClause> ctes() {
        return ctes;
    }

    @Override
    public boolean recursive() {
        return recursive;
    }

    @Override
    public void recursive(boolean recursive) {
        this.recursive = recursive;
    }

    @Override
    public List<SetOperation> setOperations() {
        return setOperations;
    }

    @Override
    public String lockMode() {
        return lockMode;
    }

    @Override
    public void lockMode(String mode) {
        this.lockMode = mode;
    }

    @Override
    public SoftDeleteScope softDeleteScope() {
        return softDeleteScope;
    }

    @Override
    public void softDeleteScope(SoftDeleteScope scope) {
        this.softDeleteScope = scope;
    }

    // ==================== PUBLIC ACCESSORS ====================

    /**
     * Get the soft delete scope for query result metadata.
     */
    public SoftDeleteScope getSoftDeleteScope() {
        return softDeleteScope;
    }

    /**
     * Get the FROM table for entity type resolution.
     */
    public Table<?> getFromTable() {
        return fromTable;
    }

    /**
     * Get the entity class from the FROM table.
     */
    public Class<?> getEntityType() {
        return fromTable != null ? fromTable.getEntityType() : null;
    }

    /**
     * Get the current WHERE clause.
     */
    public Predicate getWhereClause() {
        return whereClause;
    }

    /**
     * Check if DISTINCT modifier is applied.
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Get the LIMIT value.
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Get the OFFSET value.
     */
    public Integer getOffset() {
        return offset;
    }

    /**
     * Get the eager load specifications.
     */
    public List<EagerLoadSpec> getEagerLoads() {
        return eagerLoads;
    }

    /**
     * Get the relations excluded from eager loading.
     */
    public Set<String> getWithoutRelations() {
        return withoutRelations;
    }

    /**
     * Soft delete scope for controlling query behavior.
     */
    public enum SoftDeleteScope {
        /** Default behavior - exclude soft deleted records (WHERE deleted_at IS NULL) */
        DEFAULT,
        /** Include soft deleted records - no filter applied */
        WITH_TRASHED,
        /** Only return soft deleted records (WHERE deleted_at IS NOT NULL) */
        ONLY_TRASHED
    }
}
