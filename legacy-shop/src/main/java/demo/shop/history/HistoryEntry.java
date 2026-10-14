package demo.shop.history;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(indexes = @Index(columnList = "orderId"))
public class HistoryEntry {
    @Id
    @GeneratedValue
    private Long id;
    private Long orderId;
    private String action;
    private String details;
    private Instant at;

    protected HistoryEntry() {
    }

    public HistoryEntry(Long orderId, String action, String details, Instant at) {
        this.orderId = orderId;
        this.action = action;
        this.details = details;
        this.at = at;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Instant getAt() {
        return at;
    }
}
