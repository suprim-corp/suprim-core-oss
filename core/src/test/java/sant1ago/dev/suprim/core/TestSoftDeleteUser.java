package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.annotation.entity.Column;
import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.annotation.entity.Id;
import sant1ago.dev.suprim.annotation.entity.SoftDeletes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test entity with soft delete support.
 */
@Entity(table = "soft_delete_users")
@SoftDeletes
public class TestSoftDeleteUser {

    @Id
    private UUID id;

    @Column
    private String email;

    @Column
    private String name;

    @Column
    private Boolean isActive;

    @Column
    private LocalDateTime deletedAt;

    @Column
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
