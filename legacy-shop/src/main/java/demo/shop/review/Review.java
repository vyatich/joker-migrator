package demo.shop.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"productId", "customerId"}))
public class Review {
    @Id
    @GeneratedValue
    private Long id;
    private Long productId;
    private Long customerId;
    @Min(1)
    @Max(5)
    private int rating;
    @Size(max = 500)
    @Column(length = 500)
    private String text;
    private Instant createdAt;

    protected Review() {
    }

    public Review(Long productId, Long customerId, int rating, String text, Instant createdAt) {
        this.productId = productId;
        this.customerId = customerId;
        this.rating = rating;
        this.text = text;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public int getRating() {
        return rating;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
