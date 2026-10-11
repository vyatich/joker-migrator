package demo.shop.inventory;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {
    private final long productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(long productId, int requested, int available) {
        super("Product " + productId + ": requested " + requested + ", available " + available);
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }
}
