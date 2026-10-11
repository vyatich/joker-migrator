package demo.shop.order;

import lombok.Value;

import java.math.BigDecimal;

/**
 * Published when an order is placed; listeners react after the transaction commits.
 */
@Value
public class OrderPlacedEvent {
    long orderId;
    long customerId;
    BigDecimal total;
}
