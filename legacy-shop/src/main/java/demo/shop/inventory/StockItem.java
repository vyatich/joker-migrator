package demo.shop.inventory;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * Stock of one product: what can still be ordered and what is held by placed orders.
 */
@Entity
public class StockItem {
    @Id
    private Long productId;
    private int available;
    private int reserved;
    @Version
    private Long version;

    protected StockItem() {
    }

    public StockItem(Long productId, int available) {
        this.productId = productId;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public int getAvailable() {
        return available;
    }

    public int getReserved() {
        return reserved;
    }

    void setAvailable(int available) {
        this.available = available;
    }

    void reserve(int quantity) {
        if (quantity > available) {
            throw new InsufficientStockException(productId, quantity, available);
        }
        available -= quantity;
        reserved += quantity;
    }

    /**
     * Reserved items have left the warehouse: they are neither reserved nor available any more.
     */
    void commit(int quantity) {
        reserved -= Math.min(quantity, reserved);
    }

    void release(int quantity) {
        int released = Math.min(quantity, reserved);
        reserved -= released;
        available += released;
    }
}
