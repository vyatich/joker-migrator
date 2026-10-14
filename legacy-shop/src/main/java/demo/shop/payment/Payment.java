package demo.shop.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Payment {
    @Id
    @GeneratedValue
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private Instant processedAt;

    protected Payment() {
    }

    public Payment(Long orderId, BigDecimal amount, PaymentStatus status, Instant processedAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.processedAt = processedAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
