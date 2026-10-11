package demo.shop;

import demo.shop.product.Product;
import demo.shop.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ShopApplicationTest {
    @Autowired
    ProductRepository repository;

    @Test
    void seedsCatalogAndQueriesIt() {
        assertThat(repository.findExpensive(new BigDecimal("100")))
            .extracting(Product::getName)
            .containsExactly("Laptop");
    }
}
