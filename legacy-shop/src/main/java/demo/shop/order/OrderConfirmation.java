package demo.shop.order;

import java.math.BigDecimal;

public class OrderConfirmation {
    private final String product;
    private final int quantity;
    private final BigDecimal itemsTotal;
    private final BigDecimal delivery;

    public OrderConfirmation(String product, int quantity, BigDecimal itemsTotal, BigDecimal delivery) {
        this.product = product;
        this.quantity = quantity;
        this.itemsTotal = itemsTotal;
        this.delivery = delivery;
    }

    public String getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getItemsTotal() {
        return itemsTotal;
    }

    public BigDecimal getDelivery() {
        return delivery;
    }

    public BigDecimal getTotal() {
        return itemsTotal.add(delivery);
    }
}
