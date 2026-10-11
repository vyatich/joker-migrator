package demo.shop.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
    @Test
    void sumsLinesAndDelivery() {
        Order order = new Order(1L, Instant.now());
        order.addLine(1L, "Coffee", 2, new BigDecimal("17.82"));
        order.addLine(3L, "Headphones", 1, new BigDecimal("179.10"));
        order.setDelivery(new BigDecimal("4.99"));

        assertThat(order.getItemsTotal()).isEqualByComparingTo("196.92");
        assertThat(order.getTotal()).isEqualByComparingTo("201.91");
    }

    @Test
    void onlyPlacedOrdersExpire() {
        Order order = new Order(1L, Instant.now());
        order.expire();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThatThrownBy(order::expire).isInstanceOf(IllegalStateException.class);
    }
}
