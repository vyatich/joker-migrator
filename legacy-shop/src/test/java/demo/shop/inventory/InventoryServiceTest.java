package demo.shop.inventory;

import demo.shop.product.Product;
import demo.shop.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(InventoryService.class)
class InventoryServiceTest {
    @Autowired
    InventoryService inventory;
    @Autowired
    ProductRepository products;

    long productId;

    @BeforeEach
    void setUp() {
        productId = products.save(new Product("Laptop", new BigDecimal("1499.00"))).getId();
        inventory.set(productId, 10);
    }

    @Test
    void reservationMovesStockFromAvailableToReserved() {
        inventory.reserve(productId, 3);

        StockItem item = inventory.get(productId);
        assertThat(item.getAvailable()).isEqualTo(7);
        assertThat(item.getReserved()).isEqualTo(3);
    }

    @Test
    void cannotReserveMoreThanAvailable() {
        assertThatThrownBy(() -> inventory.reserve(productId, 11))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("requested 11, available 10");
        assertThat(inventory.get(productId).getAvailable()).isEqualTo(10);
    }

    @Test
    void releaseReturnsReservedStock() {
        inventory.reserve(productId, 4);
        inventory.release(productId, 4);

        StockItem item = inventory.get(productId);
        assertThat(item.getAvailable()).isEqualTo(10);
        assertThat(item.getReserved()).isZero();
    }

    @Test
    void stockCanOnlyBeSetForExistingProducts() {
        assertThatThrownBy(() -> inventory.set(999_999L, 5)).isInstanceOf(ResponseStatusException.class);
    }
}
