package demo.shop.order;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity(name = "ShopOrder")
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue
    private Long id;
    private Long customerId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private Instant placedAt;
    private BigDecimal delivery = BigDecimal.ZERO;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "position")
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
    }

    public Order(Long customerId, Instant placedAt) {
        this.customerId = customerId;
        this.placedAt = placedAt;
        this.status = OrderStatus.PLACED;
    }

    public void addLine(Long productId, String productName, int quantity, BigDecimal amount) {
        lines.add(new OrderLine(this, productId, productName, quantity, amount));
    }

    public void expire() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("Order " + id + " is " + status + ", only placed orders expire");
        }
        status = OrderStatus.EXPIRED;
    }

    public BigDecimal getItemsTotal() {
        return lines.stream().map(OrderLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotal() {
        return getItemsTotal().add(delivery);
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public BigDecimal getDelivery() {
        return delivery;
    }

    public void setDelivery(BigDecimal delivery) {
        this.delivery = delivery;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
