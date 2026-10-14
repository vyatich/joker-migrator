package demo.shop;

import demo.shop.catalog.Category;
import demo.shop.catalog.CategoryRepository;
import demo.shop.inventory.StockItem;
import demo.shop.inventory.StockRepository;
import demo.shop.product.Product;
import demo.shop.product.ProductRepository;
import demo.shop.promo.PromoCode;
import demo.shop.promo.PromoCodeRepository;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fills the in-memory database with a small catalog so the shop can be tried right after start.
 */
@Component
public class DemoDataLoader {
    private final ProductRepository products;
    private final StockRepository stock;
    private final CategoryRepository categories;
    private final PromoCodeRepository promoCodes;

    public DemoDataLoader(ProductRepository products, StockRepository stock, CategoryRepository categories,
                          PromoCodeRepository promoCodes) {
        this.products = products;
        this.stock = stock;
        this.categories = categories;
        this.promoCodes = promoCodes;
    }

    @PostConstruct
    void load() {
        if (products.count() > 0) {
            return;
        }
        Category groceries = categories.save(new Category("Groceries"));
        Category electronics = categories.save(new Category("Electronics"));
        add("Coffee", "9.90", groceries, 100);
        add("Laptop", "1499.00", electronics, 5);
        add("Headphones", "199.00", electronics, 20);
        promoCodes.save(new PromoCode("WELCOME10", 10, LocalDate.of(2099, 12, 31)));
    }

    private void add(String name, String price, Category category, int available) {
        Product product = products.save(new Product(name, new BigDecimal(price), category));
        stock.save(new StockItem(product.getId(), available));
    }
}
