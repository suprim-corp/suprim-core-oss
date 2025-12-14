package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.annotation.entity.Entity;

/**
 * Test entity with INVALID default eager load for coverage testing.
 * The "nonexistent" relation doesn't exist and will trigger exception handling.
 */
@Entity(table = "users_invalid", with = {"nonexistent_relation"})
public class TestUserWithInvalidDefaults {
    private Long id;
}
