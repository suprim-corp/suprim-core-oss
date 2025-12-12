package sant1ago.dev.suprim.processor.model;

/**
 * Types of entity relationships.
 */
public enum RelationType {

    /**
     * One-to-one where FK is on the related table.
     * User hasOne Profile (profiles.user_id → users.id)
     */
    HAS_ONE,

    /**
     * One-to-many where FK is on the related table.
     * User hasMany Posts (posts.user_id → users.id)
     */
    HAS_MANY,

    /**
     * Inverse of hasOne/hasMany - FK is on THIS table.
     * Post belongsTo User (posts.user_id → users.id)
     */
    BELONGS_TO,

    /**
     * Many-to-many using a pivot table.
     * User belongsToMany Roles (role_user pivot table)
     */
    BELONGS_TO_MANY,

    /**
     * One-to-one through an intermediate table.
     * Mechanic hasOneThrough Owner via Car
     */
    HAS_ONE_THROUGH,

    /**
     * One-to-many through an intermediate table.
     * Country hasManyThrough Posts via Users
     */
    HAS_MANY_THROUGH,

    /**
     * Polymorphic one-to-one relationship.
     * User morphOne Image (images.imageable_type = 'User', images.imageable_id = users.id)
     */
    MORPH_ONE,

    /**
     * Polymorphic one-to-many relationship.
     * Post morphMany Comments (comments.commentable_type = 'Post', comments.commentable_id = posts.id)
     */
    MORPH_MANY,

    /**
     * Inverse polymorphic relationship.
     * Image morphTo imageable (imageable_type VARCHAR, imageable_id BIGINT)
     */
    MORPH_TO,

    /**
     * Polymorphic many-to-many relationship.
     * Post morphToMany Tags (taggables: taggable_type, taggable_id, tag_id)
     */
    MORPH_TO_MANY,

    /**
     * Inverse polymorphic many-to-many relationship.
     * Tag morphedByMany Posts (taggables: tag_id, taggable_type, taggable_id)
     */
    MORPHED_BY_MANY,

    /**
     * One-to-one returning latest record by order column.
     * User latestOfMany Order (ORDER BY created_at DESC LIMIT 1)
     */
    LATEST_OF_MANY,

    /**
     * One-to-one returning oldest record by order column.
     * User oldestOfMany Post (ORDER BY published_at ASC LIMIT 1)
     */
    OLDEST_OF_MANY,

    /**
     * One-to-one returning record based on aggregate function.
     * Auction ofMany Bid (MAX(amount))
     */
    OF_MANY;

    /**
     * Whether this relationship uses a pivot/join table.
     */
    public boolean usesPivotTable() {
        return this == BELONGS_TO_MANY || this == MORPH_TO_MANY || this == MORPHED_BY_MANY;
    }

    /**
     * Whether this is a through relationship.
     */
    public boolean isThrough() {
        return this == HAS_ONE_THROUGH || this == HAS_MANY_THROUGH;
    }

    /**
     * Whether FK is on the related table (not this table).
     */
    public boolean foreignKeyOnRelated() {
        return this == HAS_ONE || this == HAS_MANY || this == LATEST_OF_MANY || this == OLDEST_OF_MANY || this == OF_MANY;
    }

    /**
     * Whether this is an ofMany relationship.
     */
    public boolean isOfMany() {
        return this == LATEST_OF_MANY || this == OLDEST_OF_MANY || this == OF_MANY;
    }

    /**
     * Whether FK is on this table.
     */
    public boolean foreignKeyOnThis() {
        return this == BELONGS_TO;
    }

    /**
     * Whether this is a to-many relationship.
     */
    public boolean isToMany() {
        return this == HAS_MANY || this == BELONGS_TO_MANY || this == HAS_MANY_THROUGH || this == MORPH_MANY || this == MORPH_TO_MANY || this == MORPHED_BY_MANY;
    }

    /**
     * Whether this is a polymorphic relationship.
     */
    public boolean isMorphic() {
        return this == MORPH_ONE || this == MORPH_MANY || this == MORPH_TO || this == MORPH_TO_MANY || this == MORPHED_BY_MANY;
    }
}
