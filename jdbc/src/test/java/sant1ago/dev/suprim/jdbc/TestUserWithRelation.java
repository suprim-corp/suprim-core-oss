package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.annotation.entity.Entity;

/**
 * Test entity with relation - used to test with() method coverage.
 */
@Entity(table = "test_users_with_relation")
public class TestUserWithRelation {
    private Long id;
    private String name;
    private TestProfile profile;

    public TestUserWithRelation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TestProfile getProfile() { return profile; }
    public void setProfile(TestProfile profile) { this.profile = profile; }
}
