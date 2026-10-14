package demo.shop.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
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
    private String promoCode;
    private BigDecimal promoDiscount = BigDecimal.ZERO;
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

    public void applyPromo(String code, BigDecimal discount) {
        this.promoCode = code;
        this.promoDiscount = discount;
    }

    public void markPaid() {
        moveTo(OrderStatus.PAID, OrderStatus.PLACED);
    }

    public void markShipped() {
        moveTo(OrderStatus.SHIPPED, OrderStatus.PAID);
    }

    public void expire() {
        moveTo(OrderStatus.EXPIRED, OrderStatus.PLACED);
    }

    private void moveTo(OrderStatus next, OrderStatus required) {
        if (status != required) {
            throw new IllegalStateException("Order " + id + " is " + status + ", expected " + required);
        }
        status = next;
    }

    public BigDecimal getItemsTotal() {
        return lines.stream().map(OrderLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotal() {
        return getItemsTotal().subtract(promoDiscount).add(delivery);
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

    public String getPromoCode() {
        return promoCode;
    }

    public BigDecimal getPromoDiscount() {
        return promoDiscount;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
