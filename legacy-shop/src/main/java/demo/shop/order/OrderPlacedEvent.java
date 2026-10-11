package demo.shop.order;

import java.math.BigDecimal;

/**
 * Published when an order is placed; listeners react after the transaction commits.
 */
public final class OrderPlacedEvent {
    private final long orderId;
    private final long customerId;
    private final BigDecimal total;

    public OrderPlacedEvent(long orderId, long customerId, BigDecimal total) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.total = total;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
