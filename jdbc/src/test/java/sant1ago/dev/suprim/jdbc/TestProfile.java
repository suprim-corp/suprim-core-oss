package sant1ago.dev.suprim.jdbc;

import sant1ago.dev.suprim.annotation.entity.Entity;

/**
 * Test entity for profile - used in relation tests.
 */
@Entity(table = "profiles")
public class TestProfile {
    private Long id;
    private Long userId;
    private String bio;

    public TestProfile() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
