package demo.shop;

import demo.shop.inventory.StockItem;
import demo.shop.inventory.StockRepository;
import demo.shop.product.Product;
import demo.shop.product.ProductRepository;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;

/**
 * Fills the in-memory database with a small catalog so the shop can be tried right after start.
 */
@Component
public class DemoDataLoader {
    private final ProductRepository products;
    private final StockRepository stock;

    public DemoDataLoader(ProductRepository products, StockRepository stock) {
        this.products = products;
        this.stock = stock;
    }

    @PostConstruct
    void load() {
        if (products.count() > 0) {
            return;
        }
        add("Coffee", "9.90", 100);
        add("Laptop", "1499.00", 5);
        add("Headphones", "199.00", 20);
    }

    private void add(String name, String price, int available) {
        Product product = products.save(new Product(name, new BigDecimal(price)));
        stock.save(new StockItem(product.getId(), available));
    }
}
