package demo.shop.order;

import lombok.Value;

@Value
public class OrderShippedEvent {
    long orderId;
    String trackingNumber;
}
