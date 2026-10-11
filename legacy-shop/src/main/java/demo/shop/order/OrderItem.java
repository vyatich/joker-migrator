package demo.shop.order;

/**
 * One requested line of a new order: which product and how many.
 */
public final class OrderItem {
    private final long productId;
    private final int quantity;

    public OrderItem(long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
