package demo.shop.order;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class OrderPaidEvent {
    long orderId;
    BigDecimal amount;
}
