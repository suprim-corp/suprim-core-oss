package sant1ago.dev.suprim.core.query;

import sant1ago.dev.suprim.core.type.Relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Specification for eager loading relations in a query.
 * Supports nested relations and query constraints.
 *
 * <pre>{@code
 * // Single relation
 * EagerLoadSpec spec = EagerLoadSpec.of(User_.POSTS);
 *
 * // With constraint
 * EagerLoadSpec spec = EagerLoadSpec.of(User_.POSTS, posts ->
 *     posts.where(Post_.IS_PUBLISHED.eq(true)).limit(5)
 * );
 *
 * // Nested relation
 * EagerLoadSpec spec = EagerLoadSpec.of(User_.POSTS)
 *     .with(Post_.COMMENTS);
 * }</pre>
 *
 * @param relation   the relation to an eager load
 * @param constraint optional query constraint to apply when loading
 * @param nested     nested eager load specs for relations on the loaded entities
 */
public record EagerLoadSpec(
        Relation<?, ?> relation,
        Function<SelectBuilder, SelectBuilder> constraint,
        List<EagerLoadSpec> nested
) {

    /**
     * Create an eager load spec for a relation.
     *
     * @param relation the relation to eager load
     * @return a new eager load spec
     */
    public static EagerLoadSpec of(Relation<?, ?> relation) {
        return new EagerLoadSpec(relation, null, new ArrayList<>());
    }

    /**
     * Create an eager load spec with a constraint.
     *
     * @param relation the relation to eager load
     * @param constraint the query constraint to apply
     * @return a new eager load spec with constraint
     */
    public static EagerLoadSpec of(Relation<?, ?> relation, Function<SelectBuilder, SelectBuilder> constraint) {
        return new EagerLoadSpec(relation, constraint, new ArrayList<>());
    }

    /**
     * Add a nested relation to this eager load spec.
     * <pre>{@code
     * EagerLoadSpec spec = EagerLoadSpec.of(User_.POSTS)
     *     .with(Post_.COMMENTS);
     * }</pre>
     *
     * @param nestedRelation the nested relation to eager load
     * @return this spec for chaining
     */
    public EagerLoadSpec with(Relation<?, ?> nestedRelation) {
        this.nested.add(EagerLoadSpec.of(nestedRelation));
        return this;
    }

    /**
     * Add a nested relation with constraint.
     *
     * @param nestedRelation the nested relation to eager load
     * @param nestedConstraint the query constraint for the nested relation
     * @return this spec for chaining
     */
    public EagerLoadSpec with(Relation<?, ?> nestedRelation, Function<SelectBuilder, SelectBuilder> nestedConstraint) {
        this.nested.add(EagerLoadSpec.of(nestedRelation, nestedConstraint));
        return this;
    }

    /**
     * Check if this spec has a constraint.
     *
     * @return true if a constraint is present
     */
    public boolean hasConstraint() {
        return Objects.nonNull(constraint);
    }

    /**
     * Check if this spec has nested relations.
     *
     * @return true if nested relations exist
     */
    public boolean hasNested() {
        return !nested.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EagerLoadSpec{");
        sb.append("relation=").append(relation.getRelatedTable().getName());
        if (hasConstraint()) {
            sb.append(", constrained=true");
        }
        if (hasNested()) {
            sb.append(", nested=").append(nested.size());
        }
        sb.append("}");
        return sb.toString();
    }
}
