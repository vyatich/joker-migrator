package demo.shop.order;

import lombok.Value;

/**
 * One requested line of a new order: which product and how many.
 */
@Value
public class OrderItem {
    long productId;
    int quantity;
}
