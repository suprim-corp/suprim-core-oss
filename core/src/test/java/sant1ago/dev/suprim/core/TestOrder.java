package sant1ago.dev.suprim.core;

import sant1ago.dev.suprim.annotation.entity.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test entity for JOIN and relationship tests.
 */
@Entity(table = "orders")
public class TestOrder {
    private Long id;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;

    public TestOrder() {
    }

    public TestOrder(Long id, Long userId, BigDecimal amount, String status) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
