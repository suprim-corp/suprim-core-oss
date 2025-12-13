package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.annotation.entity.Entity;

/**
 * Test entity with default eager loads for coverage testing.
 */
@Entity(table = "users_with_defaults", with = {"orders"})
public class TestUserWithDefaults {
    private Long id;
    private String email;
}
