package demo.shop.shipping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Shipment {
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true, nullable = false)
    private Long orderId;
    private String trackingNumber;
    private Instant shippedAt;

    protected Shipment() {
    }

    public Shipment(Long orderId, String trackingNumber, Instant shippedAt) {
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
        this.shippedAt = shippedAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }
}
