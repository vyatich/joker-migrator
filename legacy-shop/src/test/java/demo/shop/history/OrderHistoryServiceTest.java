package demo.shop.history;

import demo.shop.order.OrderPaidEvent;
import demo.shop.order.OrderPlacedEvent;
import demo.shop.order.OrderShippedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OrderHistoryService.class)
class OrderHistoryServiceTest {
    @Autowired
    OrderHistoryService history;

    @Test
    void keepsEventsOfAnOrderInOrder() {
        history.onPlaced(new OrderPlacedEvent(42L, 5L, new BigDecimal("22.81")));
        history.onPaid(new OrderPaidEvent(42L, new BigDecimal("22.81")));
        history.onShipped(new OrderShippedEvent(42L, "TRK-00000042"));
        history.onPlaced(new OrderPlacedEvent(43L, 5L, BigDecimal.ONE));

        assertThat(history.forOrder(42L)).extracting(HistoryEntry::getAction).containsExactly("PLACED", "PAID", "SHIPPED");
        assertThat(history.forOrder(42L).get(2).getDetails()).isEqualTo("tracking TRK-00000042");
    }
}
