package demo.shop.order;

import demo.shop.api.model.OrderDto;
import demo.shop.api.model.OrderStatusDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {
    @Test
    void mapsOrderToContract() {
        Order order = new Order(5L, Instant.parse("2026-10-01T10:15:30Z"));
        order.addLine(1L, "Coffee", 2, new BigDecimal("17.82"));
        order.setDelivery(new BigDecimal("4.99"));

        OrderDto dto = OrderMapper.toDto(order);

        assertThat(dto.getCustomerId()).isEqualTo(5L);
        assertThat(dto.getStatus()).isEqualTo(OrderStatusDto.PLACED);
        assertThat(dto.getLines()).hasSize(1);
        assertThat(dto.getItemsTotal()).isEqualByComparingTo("17.82");
        assertThat(dto.getTotal()).isEqualByComparingTo("22.81");
        assertThat(dto.getPlacedAt()).isEqualTo(OffsetDateTime.parse("2026-10-01T10:15:30Z"));
    }
}
