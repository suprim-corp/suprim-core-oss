package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.annotation.entity.Entity;

import java.time.LocalDateTime;

/**
 * Test entity class for query builder tests.
 * No external dependencies - self-contained for testing.
 */
@Entity(table = "users")
public class TestUser {
    private Long id;
    private String email;
    private String name;
    private Integer age;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public TestUser() {
    }

    public TestUser(Long id, String email, String name, Integer age) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
